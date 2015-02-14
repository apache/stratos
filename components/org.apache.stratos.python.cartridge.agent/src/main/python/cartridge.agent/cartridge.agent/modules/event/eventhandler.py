# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import json
from threading import Thread

from ..util import cartridgeagentutils
from yapsy.PluginManager import PluginManager
from ...plugins.contracts import ICartridgeAgentPlugin, IArtifactManagementPlugin
from ..artifactmgt.git.agentgithandler import *
from ..artifactmgt.repository import Repository
from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ..publisher import cartridgeagentpublisher
from ..topology.topologycontext import *
from ..tenant.tenantcontext import *
from ..util.log import LogFactory

from ... exception import ParameterNotFoundException

ARTIFACT_MGT_PLUGIN = "ArtifactManagementPlugin"

AGENT_PLUGIN_EXT = "agent-plugin"
CARTRIDGE_AGENT_PLUGIN = "CartridgeAgentPlugin"
SUPER_TENANT_ID = -1234
SUPER_TENANT_REPO_PATH = "/repository/deployment/server/"
TENANT_REPO_PATH = "/repository/tenants/"


class EventHandler:
    """
    Event execution related logic
    """

    def __init__(self):
        self.__log = LogFactory().get_log(__name__)
        self.__config = CartridgeAgentConfiguration()
        self.__plugin_manager = None
        self.__plugins = {}
        """ :type dict{str: [PluginInfo]} : """
        self.__artifact_mgt_plugins = []
        self.__plugin_manager, self.__plugins, self.__artifact_mgt_plugins = self.initialize_plugins()

    def initialize_plugins(self):
        self.__log.info("Collecting and loading plugins")

        try:
            plugin_manager = PluginManager()
            plugin_manager.setPluginInfoExtension(AGENT_PLUGIN_EXT)
            plugin_manager.setCategoriesFilter({
                CARTRIDGE_AGENT_PLUGIN: ICartridgeAgentPlugin,
                ARTIFACT_MGT_PLUGIN: IArtifactManagementPlugin
            })

            plugin_manager.setPluginPlaces([self.__config.read_property(cartridgeagentconstants.PLUGINS_DIR)])

            plugin_manager.collectPlugins()

            # activate cartridge agent plugins
            plugins = plugin_manager.getPluginsOfCategory(CARTRIDGE_AGENT_PLUGIN)
            grouped_plugins = {}
            for plugin_info in plugins:
                print "Found plugin %s at %s" % (plugin_info.name, plugin_info.path)
                plugin_manager.activatePluginByName(plugin_info.name)
                print "Activated plugin %s" % plugin_info.name

                if grouped_plugins[plugin_info.description] is None:
                    grouped_plugins[plugin_info.description] = []

                grouped_plugins[plugin_info.description].add(plugin_info)

            # activate artifact management plugins
            artifact_mgt_plugins = plugin_manager.getPluginsOfCategory(ARTIFACT_MGT_PLUGIN)
            for plugin_info in artifact_mgt_plugins:
                print "Found artifact management plugin %s at %s" % (plugin_info.name, plugin_info.path)
                plugin_manager.activatePluginByName(plugin_info.name)
                print "Activated artifact management plugin %s" % plugin_info.name

            return plugin_manager, grouped_plugins, artifact_mgt_plugins
        except ParameterNotFoundException as e:
            self.__log.exception("Could not load plugins. Plugins directory not set: %s" % e)
            return None, None, None
        except Exception as e:
            self.__log.exception("Error while loading plugin: %s" % e)
            return None, None, None

    def execute_plugins_for_event(self, event, plugin_values):
        """ For each plugin registered for the specified event, start a plugin execution thread
        :param str event: The event name string
        :param dict plugin_values: the values to be passed to the plugin
        :return:
        """
        plugin_values = self.get_values_for_plugins(plugin_values)
        for plugin_info in self.__plugins[event]:
            PluginExecutor(plugin_info, plugin_values).start()

    def on_instance_started_event(self):
        self.__log.debug("Processing instance started event...")
        self.execute_plugins_for_event("InstanceStartedEvent", {})

    def on_instance_activated_event(self):
        self.__log.debug("Processing instance activated event...")
        self.execute_plugins_for_event("InstanceActivatedEvent", {})

    def get_repo_path_for_tenant(self, tenant_id, git_local_repo_path, is_multitenant):
        repo_path = ""

        if is_multitenant:
            if tenant_id == SUPER_TENANT_ID:
                # super tenant, /repository/deploy/server/
                super_tenant_repo_path = self.__config.super_tenant_repository_path
                # "app_path"
                repo_path += git_local_repo_path

                if super_tenant_repo_path is not None and super_tenant_repo_path != "":
                    super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.startswith("/") \
                        else "/" + super_tenant_repo_path
                    super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.endswith("/") \
                        else super_tenant_repo_path + "/"
                    # "app_path/repository/deploy/server/"
                    repo_path += super_tenant_repo_path
                else:
                    # "app_path/repository/deploy/server/"
                    repo_path += SUPER_TENANT_REPO_PATH

            else:
                # normal tenant, /repository/tenants/tenant_id
                tenant_repo_path = self.__config.tenant_repository_path
                # "app_path"
                repo_path += git_local_repo_path

                if tenant_repo_path is not None and tenant_repo_path != "":
                    tenant_repo_path = tenant_repo_path if tenant_repo_path.startswith("/") else "/" + tenant_repo_path
                    tenant_repo_path = tenant_repo_path if tenant_repo_path.endswith("/") else tenant_repo_path + "/"
                    # "app_path/repository/tenants/244653444"
                    repo_path += tenant_repo_path + tenant_id
                else:
                    # "app_path/repository/tenants/244653444"
                    repo_path += TENANT_REPO_PATH + tenant_id

                # tenant_dir_path = git_local_repo_path + AgentGitHandler.TENANT_REPO_PATH + tenant_id
                # GitUtils.create_dir(repo_path)
        else:
            # not multi tenant, app_path
            repo_path = git_local_repo_path

        self.__log.debug("Repo path returned : %r" % repo_path)
        return repo_path

    def on_artifact_updated_event(self, artifacts_updated_event):
        self.__log.info("Processing Artifact update event: [tenant] %s [cluster] %s [status] %s" %
                        (artifacts_updated_event.tenant_id,
                         artifacts_updated_event.cluster_id,
                         artifacts_updated_event.status))

        cluster_id_event = str(artifacts_updated_event.cluster_id).strip()
        cluster_id_payload = self.__config.cluster_id
        repo_url = str(artifacts_updated_event.repo_url).strip()

        if (repo_url != "") and (cluster_id_payload is not None) and (cluster_id_payload == cluster_id_event):
            local_repo_path = self.__config.app_path

            repo_password = None
            if artifacts_updated_event.repo_password is not None:
                secret = self.__config.cartridge_key
                repo_password = cartridgeagentutils.decrypt_password(artifacts_updated_event.repo_password, secret)

            repo_username = artifacts_updated_event.repo_username
            tenant_id = artifacts_updated_event.tenant_id
            is_multitenant = self.__config.is_multitenant
            commit_enabled = artifacts_updated_event.commit_enabled

            self.__log.info("Executing git checkout")

            # create repo object
            local_repo_path = self.get_repo_path_for_tenant(tenant_id, local_repo_path, is_multitenant)
            repo_info = Repository(repo_url, repo_username, repo_password, local_repo_path, tenant_id, is_multitenant,
                                   commit_enabled)

            # checkout code
            subscribe_run, updated = AgentGitHandler.checkout(repo_info)
            # execute artifact updated extension
            plugin_values = {"STRATOS_ARTIFACT_UPDATED_CLUSTER_ID": artifacts_updated_event.cluster_id,
                             "STRATOS_ARTIFACT_UPDATED_TENANT_ID": artifacts_updated_event.tenant_id,
                             "STRATOS_ARTIFACT_UPDATED_REPO_URL": artifacts_updated_event.repo_url,
                             "STRATOS_ARTIFACT_UPDATED_REPO_PASSWORD": artifacts_updated_event.repo_password,
                             "STRATOS_ARTIFACT_UPDATED_REPO_USERNAME": artifacts_updated_event.repo_username,
                             "STRATOS_ARTIFACT_UPDATED_STATUS": artifacts_updated_event.status}

            self.execute_plugins_for_event("ArtifactUpdatedEvent", plugin_values)

            if subscribe_run:
                # publish instanceActivated
                cartridgeagentpublisher.publish_instance_activated_event()
            elif updated:
                # updated on pull
                self.on_artifact_update_scheduler_event(tenant_id)

            update_artifacts = self.__config.read_property(cartridgeagentconstants.ENABLE_ARTIFACT_UPDATE, False)
            update_artifacts = True if str(update_artifacts).strip().lower() == "true" else False
            if update_artifacts:
                auto_commit = self.__config.is_commits_enabled
                auto_checkout = self.__config.is_checkout_enabled

                try:
                    update_interval = int(self.__config.artifact_update_interval)
                except ValueError:
                    self.__log.exception("Invalid artifact sync interval specified.")
                    update_interval = 10

                self.__log.info("Artifact updating task enabled, update interval: %s seconds" % update_interval)

                self.__log.info("Auto Commit is turned %s " % ("on" if auto_commit else "off"))
                self.__log.info("Auto Checkout is turned %s " % ("on" if auto_checkout else "off"))

                AgentGitHandler.schedule_artifact_update_task(
                    repo_info,
                    auto_checkout,
                    auto_commit,
                    update_interval)

    def on_artifact_update_scheduler_event(self, tenant_id):
        self.__log.info("Processing Artifact update scheduler event...")
        plugin_values = {"STRATOS_ARTIFACT_UPDATED_TENANT_ID": str(tenant_id),
                         "STRATOS_ARTIFACT_UPDATED_SCHEDULER": str(True)}

        self.execute_plugins_for_event("ArtifactUpdateSchedulerEvent", plugin_values)

    def on_instance_cleanup_cluster_event(self):
        self.__log.info("Processing instance cleanup cluster event...")
        self.cleanup("InstanceCleanupClusterEvent")

    def on_instance_cleanup_member_event(self):
        self.__log.info("Processing instance cleanup member event...")
        self.cleanup("InstanceCleanupMemberEvent")

    def on_member_activated_event(self, member_activated_event):
        self.__log.info("Processing Member activated event: [service] %r [cluster] %r [member] %r"
                        % (member_activated_event.service_name,
                           member_activated_event.cluster_id,
                           member_activated_event.member_id))

        member_initialized = self.check_member_state_in_topology(
            member_activated_event.service_name,
            member_activated_event.cluster_id,
            member_activated_event.member_id)

        if not member_initialized:
            self.__log.error("Member has not initialized, failed to execute member activated event")
            return

        self.execute_plugins_for_event("MemberActivatedEvent", {})

    def on_complete_topology_event(self, complete_topology_event):
        self.__log.debug("Processing Complete topology event...")

        service_name_in_payload = self.__config.service_name
        cluster_id_in_payload = self.__config.cluster_id
        member_id_in_payload = self.__config.member_id

        member_initialized = self.check_member_state_in_topology(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        self.__log.debug("Member initialized %s", member_initialized)
        if member_initialized:
            # Set cartridge agent as initialized since member is available and it is in initialized state
            self.__config.initialized = True

        topology = complete_topology_event.get_topology()
        service = topology.get_service(service_name_in_payload)
        cluster = service.get_cluster(cluster_id_in_payload)

        plugin_values = {"STRATOS_TOPOLOGY_JSON": json.dumps(topology.json_str),
                         "STRATOS_MEMBER_LIST_JSON": json.dumps(cluster.member_list_json)}

        self.execute_plugins_for_event("CompleteTopologyEvent", plugin_values)

    def on_member_initialized_event(self):
        """
         Member initialized event is sent by cloud controller once volume attachment and
         ip address allocation is completed successfully
        :return:
        """
        self.__log.debug("Processing Member initialized event...")

        service_name_in_payload = self.__config.service_name
        cluster_id_in_payload = self.__config.cluster_id
        member_id_in_payload = self.__config.member_id

        member_exists = self.member_exists_in_topology(service_name_in_payload, cluster_id_in_payload,
                                                       member_id_in_payload)

        self.__log.debug("Member exists: %s" % member_exists)

        if member_exists:
            self.__config.initialized = True

    def on_complete_tenant_event(self, complete_tenant_event):
        self.__log.debug("Processing Complete tenant event...")

        tenant_list_json = complete_tenant_event.tenant_list_json
        self.__log.debug("Complete tenants:" + json.dumps(tenant_list_json))

        plugin_values = {"STRATOS_TENANT_LIST_JSON": json.dumps(tenant_list_json)}

        self.execute_plugins_for_event("CompleteTenantEvent", plugin_values)

    def on_member_terminated_event(self, member_terminated_event):
        self.__log.info("Processing Member terminated event: [service] %s [cluster] %s [member] %s" %
                        (member_terminated_event.service_name, member_terminated_event.cluster_id,
                         member_terminated_event.member_id))

        member_initialized = self.check_member_state_in_topology(
            member_terminated_event.service_name,
            member_terminated_event.cluster_id,
            member_terminated_event.member_id
        )

        if not member_initialized:
            self.__log.error("Member has not initialized, failed to execute member terminated event")
            return

        self.execute_plugins_for_event("MemberTerminatedEvent", {})

    def on_member_suspended_event(self, member_suspended_event):
        self.__log.info("Processing Member suspended event: [service] %s [cluster] %s [member] %s" %
                        (member_suspended_event.service_name, member_suspended_event.cluster_id,
                         member_suspended_event.member_id))

        member_initialized = self.check_member_state_in_topology(
            member_suspended_event.service_name,
            member_suspended_event.cluster_id,
            member_suspended_event.member_id
        )

        if not member_initialized:
            self.__log.error("Member has not initialized, failed to execute member suspended event")
            return

        self.execute_plugins_for_event("MembeSuspendedEvent", {})

    def on_member_started_event(self, member_started_event):
        self.__log.info("Processing Member started event: [service] %s [cluster] %s [member] %s" %
                        (member_started_event.service_name, member_started_event.cluster_id,
                         member_started_event.member_id))

        member_initialized = self.check_member_state_in_topology(
            member_started_event.service_name,
            member_started_event.cluster_id,
            member_started_event.member_id
        )

        if not member_initialized:
            self.__log.error("Member has not initialized, failed to execute member started event")
            return

        self.execute_plugins_for_event("MemberStartedEvent", {})

    def start_server_extension(self):
        self.__log.info("Processing start server extension...")
        service_name_in_payload = self.__config.service_name
        cluster_id_in_payload = self.__config.cluster_id
        member_id_in_payload = self.__config.member_id

        topology_consistant = self.check_member_state_in_topology(service_name_in_payload, cluster_id_in_payload,
                                                                  member_id_in_payload)

        if not topology_consistant:
            self.__log.error("Member has not initialized, failed to execute start server event")
            return

        self.execute_plugins_for_event("StartServers", {})

    def volume_mount_extension(self, persistence_mappings_payload):
        self.__log.info("Processing volume mount extension...")
        self.execute_plugins_for_event("VolumeMount", persistence_mappings_payload)

    def on_subscription_domain_added_event(self, subscription_domain_added_event):
        tenant_domain = EventHandler.find_tenant_domain(subscription_domain_added_event.tenant_id)
        self.__log.info(
            "Processing Subscription domain added event: [tenant-id] " + subscription_domain_added_event.tenant_id +
            " [tenant-domain] " + tenant_domain + " [domain-name] " + subscription_domain_added_event.domain_name +
            " [application-context] " + subscription_domain_added_event.application_context
        )

        plugin_values = {"STRATOS_SUBSCRIPTION_SERVICE_NAME": subscription_domain_added_event.service_name,
                         "STRATOS_SUBSCRIPTION_DOMAIN_NAME": subscription_domain_added_event.domain_name,
                         "STRATOS_SUBSCRIPTION_TENANT_ID": int(subscription_domain_added_event.tenant_id),
                         "STRATOS_SUBSCRIPTION_TENANT_DOMAIN": tenant_domain,
                         "STRATOS_SUBSCRIPTION_APPLICATION_CONTEXT":
                             subscription_domain_added_event.application_context}

        self.execute_plugins_for_event("SubscriptionDomainAddedEvent", plugin_values)

    def on_subscription_domain_removed_event(self, subscription_domain_removed_event):
        tenant_domain = EventHandler.find_tenant_domain(subscription_domain_removed_event.tenant_id)
        self.__log.info(
            "Subscription domain removed event received: [tenant-id] " + subscription_domain_removed_event.tenant_id +
            " [tenant-domain] " + tenant_domain + " [domain-name] " + subscription_domain_removed_event.domain_name
        )

        plugin_values = {"STRATOS_SUBSCRIPTION_SERVICE_NAME": subscription_domain_removed_event.service_name,
                         "STRATOS_SUBSCRIPTION_DOMAIN_NAME": subscription_domain_removed_event.domain_name,
                         "STRATOS_SUBSCRIPTION_TENANT_ID": int(subscription_domain_removed_event.tenant_id),
                         "STRATOS_SUBSCRIPTION_TENANT_DOMAIN": tenant_domain}

        self.execute_plugins_for_event("SubscriptionDomainRemovedEvent", plugin_values)

    def on_copy_artifacts_extension(self, src, dest):
        self.__log.info("Processing Copy artifacts extension...")
        plugin_values = {"SOURCE": src, "DEST": dest}
        self.execute_plugins_for_event("CopyArtifacts", plugin_values)

    def on_tenant_subscribed_event(self, tenant_subscribed_event):
        self.__log.info(
            "Processing Tenant subscribed event: [tenant] " + tenant_subscribed_event.tenant_id +
            " [service] " + tenant_subscribed_event.service_name + " [cluster] " + tenant_subscribed_event.cluster_ids
        )

        self.execute_plugins_for_event("TenantSubscribedEvent", {})

    def on_application_signup_removed_event(self, application_signup_removal_event):
        self.__log.info(
            "Processing Tenant unsubscribed event: [tenant] " + application_signup_removal_event.tenantId +
            " [application ID] " + application_signup_removal_event.applicationId
        )

        if self.__config.application_id == application_signup_removal_event.applicationId:
            AgentGitHandler.remove_repo(application_signup_removal_event.tenant_id)

        self.execute_plugins_for_event("ApplicationSignUpRemovedEvent", {})

    def cleanup(self, event):
        self.__log.info("Executing cleaning up the data in the cartridge instance...")

        cartridgeagentpublisher.publish_maintenance_mode_event()

        self.execute_plugins_for_event(event, {})
        self.__log.info("cleaning up finished in the cartridge instance...")

        self.__log.info("publishing ready to shutdown event...")
        cartridgeagentpublisher.publish_instance_ready_to_shutdown_event()

    def check_member_state_in_topology(self, service_name, cluster_id, member_id):
        topology = TopologyContext.get_topology()
        service = topology.get_service(service_name)
        if service is None:
            self.__log.error("Service not found in topology [service] %s" % service_name)
            return False

        cluster = service.get_cluster(cluster_id)
        if cluster is None:
            self.__log.error("Cluster id not found in topology [cluster] %s" % cluster_id)
            return False

        activated_member = cluster.get_member(member_id)
        if activated_member is None:
            self.__log.error("Member id not found in topology [member] %s" % member_id)
            return False

        if activated_member.status != MemberStatus.Initialized:
            return False

        return True

    def member_exists_in_topology(self, service_name, cluster_id, member_id):
        topology = TopologyContext.get_topology()
        service = topology.get_service(service_name)
        if service is None:
            self.__log.error("Service not found in topology [service] %s" % service_name)
            return False

        cluster = service.get_cluster(cluster_id)
        if cluster is None:
            self.__log.error("Cluster id not found in topology [cluster] %s" % cluster_id)
            return False

        activated_member = cluster.get_member(member_id)
        if activated_member is None:
            self.__log.error("Member id not found in topology [member] %s" % member_id)
            return False

        return True

    def get_values_for_plugins(self, env_params):
        """
        Adds the common parameters to be used by the extension scripts
        :param dict[str, str] env_params: Dictionary to be added
        :return: Dictionary with updated parameters
        :rtype: dict[str, str]
        """
        env_params["APPLICATION_PATH"] = self.__config.app_path
        env_params["PARAM_FILE_PATH"] = self.__config.read_property(cartridgeagentconstants.PARAM_FILE_PATH, False)
        env_params["SERVICE_NAME"] = self.__config.service_name
        env_params["TENANT_ID"] = self.__config.tenant_id
        env_params["CARTRIDGE_KEY"] = self.__config.cartridge_key
        env_params["LB_CLUSTER_ID"] = self.__config.lb_cluster_id
        env_params["CLUSTER_ID"] = self.__config.cluster_id
        env_params["NETWORK_PARTITION_ID"] = self.__config.network_partition_id
        env_params["PARTITION_ID"] = self.__config.partition_id
        env_params["PERSISTENCE_MAPPINGS"] = self.__config.persistence_mappings
        env_params["REPO_URL"] = self.__config.repo_url
        env_params["DEPENDANT_CLUSTER_ID"] = self.__config.dependant_cluster_id
        env_params["EXPORT_METADATA_KEYS"] = self.__config.export_metadata_keys
        env_params["IMPORT_METADATA_KEYS"] = self.__config.import_metadata_keys

        lb_cluster_id_in_payload = self.__config.lb_cluster_id
        member_ips = EventHandler.get_lb_member_ip(lb_cluster_id_in_payload)
        if member_ips is not None:
            env_params["LB_IP"] = member_ips[0]
            env_params["LB_PUBLIC_IP"] = member_ips[1]
        else:
            env_params["LB_IP"] = self.__config.lb_private_ip
            env_params["LB_PUBLIC_IP"] = self.__config.lb_public_ip

        topology = TopologyContext.get_topology()
        if topology.initialized:
            service = topology.get_service(self.__config.service_name)
            cluster = service.get_cluster(self.__config.cluster_id)
            member_id_in_payload = self.__config.member_id
            member = cluster.get_member(member_id_in_payload)
            self.add_properties(service.properties, env_params, "SERVICE_PROPERTY")
            self.add_properties(cluster.properties, env_params, "CLUSTER_PROPERTY")
            self.add_properties(member.properties, env_params, "MEMBER_PROPERTY")

        # TODO: env_params.update(self.__config.get_payload_params())

        return EventHandler.clean_process_parameters(env_params)

    def add_properties(self, properties, params, prefix):
        """
        Adds the given property list to the parameters list with given prefix in the parameter name
        :param dict[str, str] properties: service properties
        :param dict[str, str] params:
        :param str prefix:
        :return: dict[str, str]
        """
        if properties is None or properties.items() is None:
            return

        for key in properties:
            params[prefix + "_" + key] = str(properties[key])
            self.__log.debug("Property added: " + prefix + "_" + key + " => " + properties[key])

    @staticmethod
    def get_lb_member_ip(lb_cluster_id):
        topology = TopologyContext.get_topology()
        services = topology.get_services()

        for service in services:
            clusters = service.get_clusters()
            for cluster in clusters:
                members = cluster.get_members()
                for member in members:
                    if member.cluster_id == lb_cluster_id:
                        return [member.member_default_private_ip, member.member_default_public_ip]

        return None

    @staticmethod
    def clean_process_parameters(params):
        """
        Removes any null valued parameters before passing them to the extension scripts
        :param dict params:
        :return: cleaned parameters
        :rtype: dict
        """
        for key, value in params.items():
            if value is None:
                del params[key]

        return params

    @staticmethod
    def find_tenant_domain(tenant_id):
        tenant = TenantContext.get_tenant(tenant_id)
        if tenant is None:
            raise RuntimeError("Tenant could not be found: [tenant-id] %s" % tenant_id)

        return tenant.tenant_domain


class PluginExecutor(Thread):
    """ Executes a given plugin on a separate thread, passing the given dictionary of values to the plugin entry mehtod
    """

    def __init__(self, plugin_info, values):
        Thread.__init__(self)
        self.__plugin_info = plugin_info
        self.__values = values
        self.__log = LogFactory().get_log(__name__)

    def run(self):
        try:
            self.__plugin_info.run_plugin(self.__values)
        except Exception as e:
            self.__log.exception("Error while executing plugin %s: %s" % (self.__plugin_info.name, e))
