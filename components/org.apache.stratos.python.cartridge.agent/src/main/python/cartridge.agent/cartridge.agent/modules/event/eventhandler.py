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

import publisher
from entity import *

from ..artifactmgt.git.agentgithandler import *
from ..artifactmgt.repository import Repository
from ..util import cartridgeagentutils
from ..util.log import LogFactory

SUPER_TENANT_ID = "-1234"
SUPER_TENANT_REPO_PATH = "/repository/deployment/server/"
TENANT_REPO_PATH = "/repository/tenants/"
log = LogFactory().get_log(__name__)

"""
Event execution related logic
"""


def on_instance_started_event():
    log.debug("Processing instance started event...")
    # TODO: copy artifacts extension
    execute_event_extendables(constants.INSTANCE_STARTED_EVENT, {})


def create_dummy_interface():
    log.debug("Processing lvs dummy interface creation...")
    lvs_vip = Config.lvs_virtual_ip.split("|")
    log.debug("LVS dummy interface creation values %s %s " % (lvs_vip[0], lvs_vip[1]))
    execute_event_extendables(
        constants.CREATE_LVS_DUMMY_INTERFACE,
        {"EVENT": constants.CREATE_LVS_DUMMY_INTERFACE,
         "LVS_DUMMY_VIRTUAL_IP": lvs_vip[0],
         "LVS_SUBNET_MASK": lvs_vip[1]}
    )


def on_instance_activated_event():
    log.debug("Processing instance activated event...")
    execute_event_extendables(constants.INSTANCE_ACTIVATED_EVENT, {})


def on_artifact_updated_event(artifacts_updated_event):
    log.debug(
        "Processing artifact updated event for [tenant] %s [cluster] %s [status] %s"
        % (str(artifacts_updated_event.tenant_id), artifacts_updated_event.cluster_id, artifacts_updated_event.status))

    cluster_id_event = str(artifacts_updated_event.cluster_id).strip()
    cluster_id_payload = Config.cluster_id
    repo_url = str(artifacts_updated_event.repo_url).strip()

    if repo_url == "":
        log.error("Repository URL is empty. Failed to process artifact updated event.")
        return

    if cluster_id_payload is None or cluster_id_payload == "":
        log.error("Cluster ID in payload is empty. Failed to process artifact updated event.")
        return

    if cluster_id_payload != cluster_id_event:
        log.debug("Cluster ID in artifact updated event does not match. Skipping event handler.")
        return

    repo_password = None
    if artifacts_updated_event.repo_password is not None:
        secret = Config.cartridge_key
        repo_password = cartridgeagentutils.decrypt_password(artifacts_updated_event.repo_password, secret)

    if Config.app_path is None:
        log.error("Repository path is empty. Failed to process artifact updated event.")
        return

    if not validate_repo_path(Config.app_path):
        log.error(
            "Repository path cannot be accessed, or is invalid. Failed to process artifact updated event. [App Path] %s"
            % Config.app_path)

        return

    repo_username = artifacts_updated_event.repo_username
    tenant_id = artifacts_updated_event.tenant_id
    is_multitenant = Config.is_multiTenant
    commit_enabled = artifacts_updated_event.commit_enabled

    # create repo object
    local_repo_path = get_repo_path_for_tenant(str(tenant_id), Config.app_path, is_multitenant)
    repo_info = Repository(repo_url, repo_username, repo_password, local_repo_path, tenant_id, commit_enabled)
    log.info("Executing checkout job on artifact updated event...")

    try:
        Config.artifact_checkout_plugin.plugin_object.checkout(repo_info)
    except Exception as e:
        log.exception(
            "Checkout job on artifact updated event failed for tenant: %s %s" % (repo_info.tenant_id, e))

    # execute artifact updated extension
    plugin_values = {"ARTIFACT_UPDATED_CLUSTER_ID": artifacts_updated_event.cluster_id,
                     "ARTIFACT_UPDATED_TENANT_ID": artifacts_updated_event.tenant_id,
                     "ARTIFACT_UPDATED_REPO_URL": artifacts_updated_event.repo_url,
                     "ARTIFACT_UPDATED_REPO_PASSWORD": artifacts_updated_event.repo_password,
                     "ARTIFACT_UPDATED_REPO_USERNAME": artifacts_updated_event.repo_username,
                     "ARTIFACT_UPDATED_STATUS": artifacts_updated_event.status}

    try:
        execute_event_extendables(constants.ARTIFACT_UPDATED_EVENT, plugin_values)
    except Exception as e:
        log.exception("Could not execute plugins for artifact updated event: %s" % e)

    if not Config.activated:
        # publish instance activated event if not yet activated
        publisher.publish_instance_activated_event()
        on_instance_activated_event()

    update_artifacts = Config.read_property(constants.ENABLE_ARTIFACT_UPDATE, True)
    auto_commit = Config.is_commits_enabled
    auto_checkout = Config.is_checkout_enabled

    if update_artifacts:
        try:
            update_interval = int(Config.artifact_update_interval)
        except ValueError:
            log.debug("Invalid artifact sync interval specified: %s, defaulting to 10 seconds" % ValueError)
            update_interval = 10

        AgentGitHandler.schedule_artifact_update_task(
            repo_info,
            auto_checkout,
            auto_commit,
            update_interval)


def on_instance_cleanup_cluster_event():
    log.debug("Processing instance cleanup cluster event...")
    cleanup(constants.INSTANCE_CLEANUP_CLUSTER_EVENT)


def on_instance_cleanup_member_event():
    log.debug("Processing instance cleanup member event...")
    cleanup(constants.INSTANCE_CLEANUP_MEMBER_EVENT)


def on_member_activated_event(member_activated_event):
    log.debug(
        "Processing Member activated event: [service] %r [cluster] %r [member] %r"
        % (member_activated_event.service_name,
           member_activated_event.cluster_id,
           member_activated_event.member_id))

    member_initialized = is_member_initialized_in_topology(
        member_activated_event.service_name,
        member_activated_event.cluster_id,
        member_activated_event.member_id)

    if not member_initialized:
        log.debug("Member has not initialized, failed to execute member activated event")
        return

    execute_event_extendables(constants.MEMBER_ACTIVATED_EVENT, {})


def on_complete_topology_event(complete_topology_event):
    log.debug("Processing Complete topology event...")

    service_name_in_payload = Config.service_name
    cluster_id_in_payload = Config.cluster_id
    member_id_in_payload = Config.member_id

    if not Config.initialized:
        member_initialized = is_member_initialized_in_topology(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        if member_initialized:
            # Set cartridge agent as initialized since member is available and it is in initialized state
            Config.initialized = True
            log.info(
                "Member initialized [member id] %s, [cluster-id] %s, [service] %s"
                % (member_id_in_payload, cluster_id_in_payload, service_name_in_payload))
        else:
            log.info("Member not initialized in topology.")

    topology = complete_topology_event.get_topology()
    service = topology.get_service(service_name_in_payload)
    if service is None:
        raise Exception("Service not found in topology [service] %s" % service_name_in_payload)

    cluster = service.get_cluster(cluster_id_in_payload)
    if cluster is None:
        raise Exception("Cluster id not found in topology [cluster] %s" % cluster_id_in_payload)

    plugin_values = {"TOPOLOGY_JSON": json.dumps(topology.json_str),
                     "MEMBER_LIST_JSON": json.dumps(cluster.member_list_json)}

    execute_event_extendables(constants.COMPLETE_TOPOLOGY_EVENT, plugin_values)


def on_member_initialized_event(member_initialized_event):
    """
     Member initialized event is sent by cloud controller once volume attachment and
     ip address allocation is completed successfully
    :param member_initialized_event:
    :return:
    """
    log.debug("Processing Member initialized event...")
    service_name_in_payload = Config.service_name
    cluster_id_in_payload = Config.cluster_id
    member_id_in_payload = Config.member_id

    if not Config.initialized and member_id_in_payload == member_initialized_event.member_id:
        member_exists = member_exists_in_topology(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        log.debug("Member exists: %s" % member_exists)

        if member_exists:
            Config.initialized = True
            mark_member_as_initialized(service_name_in_payload, cluster_id_in_payload, member_id_in_payload)
            log.info("Instance marked as initialized on member initialized event")
        else:
            raise Exception("Member [member-id] %s not found in topology while processing member initialized "
                            "event. [Topology] %s" % (member_id_in_payload, TopologyContext.get_topology()))

    execute_event_extendables(constants.MEMBER_INITIALIZED_EVENT, {})


def on_complete_tenant_event(complete_tenant_event):
    log.debug("Processing Complete tenant event...")

    tenant_list_json = complete_tenant_event.tenant_list_json
    log.debug("Complete tenants:" + json.dumps(tenant_list_json))

    plugin_values = {"TENANT_LIST_JSON": json.dumps(tenant_list_json)}

    execute_event_extendables(constants.COMPLETE_TENANT_EVENT, plugin_values)


def on_member_terminated_event(member_terminated_event):
    log.debug(
        "Processing Member terminated event: [service] %s [cluster] %s [member] %s"
        % (member_terminated_event.service_name, member_terminated_event.cluster_id, member_terminated_event.member_id))

    member_initialized = is_member_initialized_in_topology(
        member_terminated_event.service_name,
        member_terminated_event.cluster_id,
        member_terminated_event.member_id
    )

    if not member_initialized:
        log.debug("Member has not initialized, failed to execute member terminated event")
        return

    execute_event_extendables(constants.MEMBER_TERMINATED_EVENT, {})


def on_member_suspended_event(member_suspended_event):
    log.debug(
        "Processing Member suspended event: [service] %s [cluster] %s [member] %s"
        % (member_suspended_event.service_name, member_suspended_event.cluster_id, member_suspended_event.member_id))

    member_initialized = is_member_initialized_in_topology(
        member_suspended_event.service_name,
        member_suspended_event.cluster_id,
        member_suspended_event.member_id
    )

    if not member_initialized:
        log.debug("Member has not initialized, failed to execute member suspended event")
        return

    execute_event_extendables(constants.MEMBER_SUSPENDED_EVENT, {})


def on_member_started_event(member_started_event):
    log.debug(
        "Processing Member started event: [service] %s [cluster] %s [member] %s"
        % (member_started_event.service_name, member_started_event.cluster_id, member_started_event.member_id))

    member_initialized = is_member_initialized_in_topology(
        member_started_event.service_name,
        member_started_event.cluster_id,
        member_started_event.member_id
    )

    if not member_initialized:
        log.debug("Member has not initialized, failed to execute member started event")
        return

    execute_event_extendables(constants.MEMBER_STARTED_EVENT, {})


def start_server_extension():
    log.debug("Processing start server extension...")
    service_name_in_payload = Config.service_name
    cluster_id_in_payload = Config.cluster_id
    member_id_in_payload = Config.member_id
    member_initialized = is_member_initialized_in_topology(
        service_name_in_payload, cluster_id_in_payload, member_id_in_payload)

    if not member_initialized:
        log.debug("Member has not initialized, failed to execute start server event")
        return

    execute_event_extendables("StartServers", {})


def volume_mount_extension(persistence_mappings_payload):
    log.debug("Processing volume mount extension...")
    execute_event_extendables("VolumeMount", persistence_mappings_payload)


def on_domain_mapping_added_event(domain_mapping_added_event):
    tenant_domain = find_tenant_domain(domain_mapping_added_event.tenant_id)
    log.debug(
        "Processing Domain mapping added event: [tenant-id] " + str(domain_mapping_added_event.tenant_id) +
        " [tenant-domain] " + tenant_domain + " [domain-name] " + domain_mapping_added_event.domain_name +
        " [application-context] " + domain_mapping_added_event.application_context
    )

    plugin_values = {"SUBSCRIPTION_APPLICATION_ID": domain_mapping_added_event.application_id,
                     "SUBSCRIPTION_SERVICE_NAME": domain_mapping_added_event.service_name,
                     "SUBSCRIPTION_DOMAIN_NAME": domain_mapping_added_event.domain_name,
                     "SUBSCRIPTION_CLUSTER_ID": domain_mapping_added_event.cluster_id,
                     "SUBSCRIPTION_TENANT_ID": int(domain_mapping_added_event.tenant_id),
                     "SUBSCRIPTION_TENANT_DOMAIN": tenant_domain,
                     "SUBSCRIPTION_CONTEXT_PATH":
                         domain_mapping_added_event.context_path}

    execute_event_extendables(constants.DOMAIN_MAPPING_ADDED_EVENT, plugin_values)


def on_domain_mapping_removed_event(domain_mapping_removed_event):
    tenant_domain = find_tenant_domain(domain_mapping_removed_event.tenant_id)
    log.info(
        "Domain mapping removed event received: [tenant-id] " + str(domain_mapping_removed_event.tenant_id) +
        " [tenant-domain] " + tenant_domain + " [domain-name] " + domain_mapping_removed_event.domain_name
    )

    plugin_values = {"SUBSCRIPTION_APPLICATION_ID": domain_mapping_removed_event.application_id,
                     "SUBSCRIPTION_SERVICE_NAME": domain_mapping_removed_event.service_name,
                     "SUBSCRIPTION_DOMAIN_NAME": domain_mapping_removed_event.domain_name,
                     "SUBSCRIPTION_CLUSTER_ID": domain_mapping_removed_event.cluster_id,
                     "SUBSCRIPTION_TENANT_ID": int(domain_mapping_removed_event.tenant_id),
                     "SUBSCRIPTION_TENANT_DOMAIN": tenant_domain}

    execute_event_extendables(constants.DOMAIN_MAPPING_REMOVED_EVENT, plugin_values)


def on_copy_artifacts_extension(src, dest):
    log.debug("Processing Copy artifacts extension...")
    plugin_values = {"SOURCE": src, "DEST": dest}
    execute_event_extendables("CopyArtifacts", plugin_values)


def on_tenant_subscribed_event(tenant_subscribed_event):
    log.debug(
        "Processing Tenant subscribed event: [tenant] " + str(tenant_subscribed_event.tenant_id) +
        " [service] " + tenant_subscribed_event.service_name + " [cluster] " + tenant_subscribed_event.cluster_ids
    )

    execute_event_extendables(constants.TENANT_SUBSCRIBED_EVENT, {})


def on_application_signup_removed_event(application_signup_removal_event):
    log.debug(
        "Processing Tenant unsubscribed event: [tenant] " + str(application_signup_removal_event.tenantId) +
        " [application ID] " + str(application_signup_removal_event.applicationId)
    )

    if Config.application_id == application_signup_removal_event.applicationId:
        AgentGitHandler.remove_repo(application_signup_removal_event.tenantId)

    execute_event_extendables(constants.APPLICATION_SIGNUP_REMOVAL_EVENT, {})


def cleanup(event):
    log.debug("Executing cleanup extension for event %s..." % event)
    publisher.publish_maintenance_mode_event()
    execute_event_extendables("clean", {})
    publisher.publish_instance_ready_to_shutdown_event()


def execute_event_extendables(event, input_values):
    """ Execute the extensions and plugins related to the event
    :param event: The event name string
    :param input_values: the values to be passed to the plugin
    :return:
    """
    try:
        input_values = add_common_input_values(input_values)
    except Exception as e:
        log.error("Error while adding common input values for event extendables: %s" % e)

    input_values["EVENT"] = event
    log.debug("Executing extensions for [event] %s with [input values] %s" % (event, input_values))
    # Execute the extension
    execute_extension_for_event(event, input_values)
    # Execute the plugins
    execute_plugins_for_event(event, input_values)


def execute_plugins_for_event(event, input_values):
    """ For each plugin registered for the specified event, start a plugin execution thread
   :param str event: The event name string
   :param dict input_values: the values to be passed to the plugin
   :return:
   """
    try:
        plugins_for_event = Config.plugins.get(event)
        if plugins_for_event is not None:
            for plugin_info in plugins_for_event:
                log.debug("Executing plugin %s for event %s" % (plugin_info.name, event))
                plugin_thread = PluginExecutor(plugin_info, input_values)
                plugin_thread.setName("PluginExecutorThreadForPlugin%s" % plugin_info.name)
                log.debug("Starting a PluginExecutor Thread for event %s" % event.__class__.__name__)
                plugin_thread.start()

                # block till plugin run completes.
                plugin_thread.join()
        else:
            log.debug("No plugins registered for event %s" % event)
    except Exception as e:
        log.exception("Error while executing plugin for event %s: %s" % (event, e))


def execute_extension_for_event(event, extension_values):
    """ Execute the extension related to the event
    :param event: The event name string
    :param extension_values: the values to be passed to the plugin
    :return:
    """
    try:
        if Config.extension_executor is not None:
            log.debug("Executing extension for event [%s]" % event)
            extension_thread = PluginExecutor(Config.extension_executor, extension_values)
            extension_thread.setName("ExtensionExecutorThreadForExtension%s" % event.__class__.__name__)
            log.debug("Starting a PluginExecutor Thread for event extension %s" % event.__class__.__name__)
            extension_thread.start()

            # block till plugin run completes.
            extension_thread.join()
        else:
            log.debug("No extensions registered for event %s" % event)
    except OSError as e:
        log.warn("No extension was found for event %s: %s" % (event, e))
    except Exception as e:
        log.exception("Error while executing extension for event %s: %s" % (event, e))


def get_repo_path_for_tenant(tenant_id, git_local_repo_path, is_multitenant):
    """ Finds the repository path for tenant to clone from the remote repository
    :param tenant_id:
    :param git_local_repo_path:
    :param is_multitenant:
    :return:
    """
    repo_path = ""

    if is_multitenant:
        if tenant_id == SUPER_TENANT_ID:
            # super tenant, /repository/deploy/server/
            super_tenant_repo_path = Config.super_tenant_repository_path
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
            tenant_repo_path = Config.tenant_repository_path
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

    log.debug("Repo path returned : %r" % repo_path)
    return repo_path


def is_member_initialized_in_topology(service_name, cluster_id, member_id):
    if member_exists_in_topology(service_name, cluster_id, member_id):
        topology = TopologyContext.get_topology()
        service = topology.get_service(service_name)
        if service is None:
            raise Exception("Service not found in topology [service] %s" % service_name)

        cluster = service.get_cluster(cluster_id)
        if cluster is None:
            raise Exception("Cluster id not found in topology [cluster] %s" % cluster_id)

        member = cluster.get_member(member_id)
        if member is None:
            raise Exception("Member id not found in topology [member] %s" % member_id)

        log.debug("Found member: " + member.to_json())
        if member.status == MemberStatus.Initialized:
            return True

    log.debug("Member doesn't exist in topology")
    return False


def member_exists_in_topology(service_name, cluster_id, member_id):
    log.debug("Checking if member exists in topology : %s, %s, %s, " % (service_name, cluster_id, member_id))
    topology = TopologyContext.get_topology()
    service = topology.get_service(service_name)
    if service is None:
        raise Exception("Service not found in topology [service] %s" % service_name)

    cluster = service.get_cluster(cluster_id)
    if cluster is None:
        raise Exception("Cluster id not found in topology [cluster] %s" % cluster_id)

    member = cluster.get_member(member_id)
    if member is None:
        log.debug("Member id not found in topology [member] %s" % member_id)
        return False

    return True


def mark_member_as_initialized(service_name, cluster_id, member_id):
    topology = TopologyContext.get_topology()
    service = topology.get_service(service_name)
    if service is None:
        raise Exception("Service not found in topology [service] %s" % service_name)

    cluster = service.get_cluster(cluster_id)
    if cluster is None:
        raise Exception("Cluster id not found in topology [cluster] %s" % cluster_id)

    member = cluster.get_member(member_id)
    if member is None:
        raise Exception("Member id not found in topology [member] %s" % member_id)
    member.status = MemberStatus.Initialized


def add_common_input_values(plugin_values):
    """
    Adds the common parameters to be used by the extension scripts
    :param dict[str, str] plugin_values: Dictionary to be added
    :return: Dictionary with updated parameters
    :rtype: dict[str, str]
    """
    if plugin_values is None:
        plugin_values = {}
    elif type(plugin_values) != dict:
        plugin_values = {"VALUE1": str(plugin_values)}

    # Values for the plugins to use in case they want to connect to the MB.
    plugin_values["MB_IP"] = Config.mb_ip

    plugin_values["APPLICATION_PATH"] = Config.app_path
    plugin_values["PARAM_FILE_PATH"] = Config.read_property(constants.PARAM_FILE_PATH, False)
    plugin_values["PERSISTENCE_MAPPINGS"] = Config.persistence_mappings

    lb_cluster_id_in_payload = Config.lb_cluster_id
    lb_private_ip, lb_public_ip = get_lb_member_ip(lb_cluster_id_in_payload)
    plugin_values["LB_IP"] = lb_private_ip if lb_private_ip is not None else Config.lb_private_ip
    plugin_values["LB_PUBLIC_IP"] = lb_public_ip if lb_public_ip is not None else Config.lb_public_ip

    topology = TopologyContext.get_topology()
    if topology.initialized:
        service = topology.get_service(Config.service_name)
        if service is None:
            raise Exception("Service not found in topology [service] %s" % Config.service_name)

        cluster = service.get_cluster(Config.cluster_id)
        if cluster is None:
            raise Exception("Cluster id not found in topology [cluster] %s" % Config.cluster_id)

        member = cluster.get_member(Config.member_id)
        if member is None:
            raise Exception("Member id not found in topology [member] %s" % Config.member_id)

        add_properties(service.properties, plugin_values, "SERVICE_PROPERTY")
        add_properties(cluster.properties, plugin_values, "CLUSTER_PROPERTY")
        add_properties(member.properties, plugin_values, "MEMBER_PROPERTY")

    plugin_values.update(Config.get_payload_params())

    return clean_process_parameters(plugin_values)


def add_properties(properties, params, prefix):
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


def get_lb_member_ip(lb_cluster_id):
    topology = TopologyContext.get_topology()
    services = topology.get_services()

    for service in services:
        clusters = service.get_clusters()
        for cluster in clusters:
            members = cluster.get_members()
            for member in members:
                if member.cluster_id == lb_cluster_id:
                    return member.member_default_private_ip, member.member_default_public_ip

    return None, None


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


def find_tenant_domain(tenant_id):
    tenant = TenantContext.get_tenant(tenant_id)
    if tenant is None:
        raise RuntimeError("Tenant could not be found: [tenant-id] %s" % str(tenant_id))

    return tenant.tenant_domain


def validate_repo_path(app_path):
    # app path would be ex: /var/www, or /opt/server/data
    return os.path.isabs(app_path)


class PluginExecutor(Thread):
    """ Executes a given plugin on a separate thread, passing the given dictionary of values to the plugin entry method
    """

    def __init__(self, plugin_info, values):
        Thread.__init__(self)
        self.__plugin_info = plugin_info
        self.__values = values
        self.__log = LogFactory().get_log(__name__)
        self.setDaemon(True)

    def run(self):
        self.__log.debug("Starting the PluginExecutor thread")
        try:
            self.__plugin_info.plugin_object.run_plugin(self.__values)
        except Exception as e:
            self.__log.exception("Error while executing plugin %s: %s" % (self.__plugin_info.name, e))
