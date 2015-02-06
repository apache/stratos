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

from abstractextensionhandler import AbstractExtensionHandler
from ..util import extensionutils, cartridgeagentutils


class DefaultExtensionHandler(AbstractExtensionHandler):
    """
    Default implementation of the AbstractExtensionHandler
    """
    log = None

    def __init__(self):
        self.log = LogFactory().get_log(__name__)
        self.wk_members = []
        self.cartridge_agent_config = CartridgeAgentConfiguration()

    def on_instance_started_event(self):
        self.log.debug("Processing instance started event...")
        env_params = {}
        extensionutils.execute_instance_started_extension(env_params)

    def on_instance_activated_event(self):
        self.log.debug("Processing instance activated event...")
        env_params = {}
        extensionutils.execute_instance_activated_extension(env_params)

    def on_artifact_updated_event(self, artifacts_updated_event):
        self.log.info("Artifact update event received: [tenant] %r [cluster] %r [status] %r" %
                      (artifacts_updated_event.tenant_id, artifacts_updated_event.cluster_id,
                       artifacts_updated_event.status))

        cluster_id_event = str(artifacts_updated_event.cluster_id).strip()
        cluster_id_payload = self.cartridge_agent_config.cluster_id
        repo_url = str(artifacts_updated_event.repo_url).strip()

        if (repo_url != "") and (cluster_id_payload is not None) and (cluster_id_payload == cluster_id_event):
            local_repo_path = self.cartridge_agent_config.app_path

            repo_password = None
            if artifacts_updated_event.repo_password is not None:
                secret = self.cartridge_agent_config.cartridge_key
                repo_password = cartridgeagentutils.decrypt_password(artifacts_updated_event.repo_password, secret)

            repo_username = artifacts_updated_event.repo_username
            tenant_id = artifacts_updated_event.tenant_id
            is_multitenant = self.cartridge_agent_config.is_multitenant
            commit_enabled = artifacts_updated_event.commit_enabled

            self.log.info("Executing git checkout")

            # create repo object
            repo_info = RepositoryInformation(repo_url, repo_username, repo_password, local_repo_path, tenant_id,
                                              is_multitenant, commit_enabled)

            # checkout code
            subscribe_run, repo_context = agentgithandler.AgentGitHandler.checkout(repo_info)
            # repo_context = checkout_result["repo_context"]
            # execute artifact updated extension
            env_params = {"STRATOS_ARTIFACT_UPDATED_CLUSTER_ID": artifacts_updated_event.cluster_id,
                          "STRATOS_ARTIFACT_UPDATED_TENANT_ID": artifacts_updated_event.tenant_id,
                          "STRATOS_ARTIFACT_UPDATED_REPO_URL": artifacts_updated_event.repo_url,
                          "STRATOS_ARTIFACT_UPDATED_REPO_PASSWORD": artifacts_updated_event.repo_password,
                          "STRATOS_ARTIFACT_UPDATED_REPO_USERNAME": artifacts_updated_event.repo_username,
                          "STRATOS_ARTIFACT_UPDATED_STATUS": artifacts_updated_event.status}

            extensionutils.execute_artifacts_updated_extension(env_params)

            if subscribe_run:
                # publish instanceActivated
                cartridgeagentpublisher.publish_instance_activated_event()

            update_artifacts = self.cartridge_agent_config.read_property(cartridgeagentconstants.ENABLE_ARTIFACT_UPDATE,
                                                                         False)
            update_artifacts = True if str(update_artifacts).strip().lower() == "true" else False
            if update_artifacts:
                auto_commit = self.cartridge_agent_config.is_commits_enabled
                auto_checkout = self.cartridge_agent_config.is_checkout_enabled

                try:
                    update_interval = int(
                        self.cartridge_agent_config.read_property(cartridgeagentconstants.ARTIFACT_UPDATE_INTERVAL))
                except ParameterNotFoundException:
                    self.log.exception("Invalid artifact sync interval specified ")
                    update_interval = 10
                except ValueError:
                    self.log.exception("Invalid artifact sync interval specified ")
                    update_interval = 10

                self.log.info("Artifact updating task enabled, update interval: %r seconds" % update_interval)

                self.log.info("Auto Commit is turned %r " % ("on" if auto_commit else "off"))
                self.log.info("Auto Checkout is turned %r " % ("on" if auto_checkout else "off"))

                agentgithandler.AgentGitHandler.schedule_artifact_update_scheduled_task(
                    repo_info,
                    auto_checkout,
                    auto_commit,
                    update_interval)

    def on_artifact_update_scheduler_event(self, tenant_id):
        env_params = {"STRATOS_ARTIFACT_UPDATED_TENANT_ID": str(tenant_id),
                      "STRATOS_ARTIFACT_UPDATED_SCHEDULER": str(True)}

        extensionutils.execute_artifacts_updated_extension(env_params)

    def on_instance_cleanup_cluster_event(self, instance_cleanup_cluster_event):
        self.cleanup()

    def on_instance_cleanup_member_event(self, instance_cleanup_member_event):
        self.cleanup()

    def on_member_activated_event(self, member_activated_event):
        self.log.info("Member activated event received: [service] %r [cluster] %r [member] %r"
                      % (member_activated_event.service_name,
                         member_activated_event.cluster_id,
                         member_activated_event.member_id))

        member_initialized = extensionutils.check_member_state_in_topology(
            member_activated_event.service_name,
            member_activated_event.cluster_id,
            member_activated_event.member_id)

        if not member_initialized:
            self.log.error("Member has not initialized, failed to execute member activated event")
            return

        extensionutils.execute_member_activated_extension({})

    def on_complete_topology_event(self, complete_topology_event):
        self.log.debug("Complete topology event received")

        service_name_in_payload = self.cartridge_agent_config.service_name
        cluster_id_in_payload = self.cartridge_agent_config.cluster_id
        member_id_in_payload = self.cartridge_agent_config.member_id

        member_initialized = extensionutils.check_member_state_in_topology(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        if member_initialized:
            # Set cartridge agent as initialized since member is available and it is in initialized state
            self.cartridge_agent_config.initialized = True

        topology = complete_topology_event.get_topology()
        service = topology.get_service(service_name_in_payload)
        cluster = service.get_cluster(cluster_id_in_payload)

        env_params = {"STRATOS_TOPOLOGY_JSON": json.dumps(topology.json_str),
                      "STRATOS_MEMBER_LIST_JSON": json.dumps(cluster.member_list_json)}

        extensionutils.execute_complete_topology_extension(env_params)

    # Member initialized event is sent by cloud controller once volume attachment
    # and ip address allocation is completed successfully
    def on_member_initialized_event(self, member_initialized_event):
        self.log.debug("Member initialized event received")

        service_name_in_payload = self.cartridge_agent_config.service_name
        cluster_id_in_payload = self.cartridge_agent_config.cluster_id
        member_id_in_payload = self.cartridge_agent_config.member_id

        member_initialized = extensionutils.check_member_state_in_topology(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        if not member_initialized:
            return
        else:
            self.cartridge_agent_config.initialized = True

    def on_complete_tenant_event(self, complete_tenant_event):
        self.log.debug("Complete tenant event received")

        tenant_list_json = complete_tenant_event.tenant_list_json
        self.log.debug("Complete tenants:" + json.dumps(tenant_list_json))

        env_params = {"STRATOS_TENANT_LIST_JSON": json.dumps(tenant_list_json)}

        extensionutils.execute_complete_tenant_extension(env_params)

    def on_member_terminated_event(self, member_terminated_event):
        self.log.info("Member terminated event received: [service] " + member_terminated_event.service_name +
                      " [cluster] " + member_terminated_event.cluster_id
                      + " [member] " + member_terminated_event.member_id)

        member_initialized = extensionutils.check_member_state_in_topology(
            member_terminated_event.service_name,
            member_terminated_event.cluster_id,
            member_terminated_event.member_id
        )

        if not member_initialized:
            self.log.error("Member has not initialized, failed to execute member terminated event")
            return

        extensionutils.execute_member_terminated_extension({})

    def on_member_suspended_event(self, member_suspended_event):
        self.log.info("Member suspended event received: [service] " + member_suspended_event.service_name +
                      " [cluster] " + member_suspended_event.cluster_id +
                      " [member] " + member_suspended_event.member_id)

        member_initialized = extensionutils.check_member_state_in_topology(
            member_suspended_event.service_name,
            member_suspended_event.cluster_id,
            member_suspended_event.member_id
        )

        if not member_initialized:
            self.log.error("Member has not initialized, failed to execute member suspended event")
            return

        extensionutils.execute_member_suspended_extension({})

    def on_member_started_event(self, member_started_event):
        self.log.info("Member started event received: [service] " + member_started_event.service_name +
                      " [cluster] " + member_started_event.cluster_id + " [member] " + member_started_event.member_id)

        member_initialized = extensionutils.check_member_state_in_topology(
            member_started_event.service_name,
            member_started_event.cluster_id,
            member_started_event.member_id
        )

        if not member_initialized:
            self.log.error("Member has not initialized, failed to execute member started event")
            return

        extensionutils.execute_member_started_extension({})

    def start_server_extension(self):
        service_name_in_payload = self.cartridge_agent_config.service_name
        cluster_id_in_payload = self.cartridge_agent_config.cluster_id
        member_id_in_payload = self.cartridge_agent_config.member_id

        topology_consistant = extensionutils.check_member_state_in_topology(service_name_in_payload, cluster_id_in_payload,
                                                                        member_id_in_payload)

        if not topology_consistant:
            self.log.error("Member has not initialized, failed to execute start server event")
            return

        extensionutils.execute_start_servers_extension({})

    def volume_mount_extension(self, persistence_mappings_payload):
        extensionutils.execute_volume_mount_extension(persistence_mappings_payload)

    def on_subscription_domain_added_event(self, subscription_domain_added_event):
        tenant_domain = self.find_tenant_domain(subscription_domain_added_event.tenant_id)
        self.log.info(
            "Subscription domain added event received: [tenant-id] " + subscription_domain_added_event.tenant_id +
            " [tenant-domain] " + tenant_domain + " [domain-name] " + subscription_domain_added_event.domain_name +
            " [application-context] " + subscription_domain_added_event.application_context
        )

        env_params = {"STRATOS_SUBSCRIPTION_SERVICE_NAME": subscription_domain_added_event.service_name,
                      "STRATOS_SUBSCRIPTION_DOMAIN_NAME": subscription_domain_added_event.domain_name,
                      "STRATOS_SUBSCRIPTION_TENANT_ID": int(subscription_domain_added_event.tenant_id),
                      "STRATOS_SUBSCRIPTION_TENANT_DOMAIN": tenant_domain,
                      "STRATOS_SUBSCRIPTION_APPLICATION_CONTEXT": subscription_domain_added_event.application_context}

        extensionutils.execute_subscription_domain_added_extension(env_params)

    def on_subscription_domain_removed_event(self, subscription_domain_removed_event):
        tenant_domain = self.find_tenant_domain(subscription_domain_removed_event.tenant_id)
        self.log.info(
            "Subscription domain removed event received: [tenant-id] " + subscription_domain_removed_event.tenant_id +
            " [tenant-domain] " + tenant_domain + " [domain-name] " + subscription_domain_removed_event.domain_name
        )

        env_params = {"STRATOS_SUBSCRIPTION_SERVICE_NAME": subscription_domain_removed_event.service_name,
                      "STRATOS_SUBSCRIPTION_DOMAIN_NAME": subscription_domain_removed_event.domain_name,
                      "STRATOS_SUBSCRIPTION_TENANT_ID": int(subscription_domain_removed_event.tenant_id),
                      "STRATOS_SUBSCRIPTION_TENANT_DOMAIN": tenant_domain}

        extensionutils.execute_subscription_domain_removed_extension(env_params)

    def on_copy_artifacts_extension(self, src, des):
        extensionutils.execute_copy_artifact_extension(src, des)

    def on_tenant_subscribed_event(self, tenant_subscribed_event):
        self.log.info(
            "Tenant subscribed event received: [tenant] " + tenant_subscribed_event.tenant_id +
            " [service] " + tenant_subscribed_event.service_name + " [cluster] " + tenant_subscribed_event.cluster_ids
        )

        extensionutils.execute_tenant_subscribed_extension({})

    def on_application_signup_removal_event(self, application_signup_removal_event):
        self.log.info(
            "Tenant unsubscribed event received: [tenant] " + application_signup_removal_event.tenantId +
            " [application ID] " + application_signup_removal_event.applicationId
        )

        try:
            if self.cartridge_agent_config.service_name == application_signup_removal_event.applicationId:
                agentgithandler.AgentGitHandler.remove_repo(application_signup_removal_event.tenant_id)
        except:
            self.log.exception("Removing git repository failed: ")
        extensionutils.execute_application_signup_removal_extension({})

    def cleanup(self):
        self.log.info("Executing cleaning up the data in the cartridge instance...")

        cartridgeagentpublisher.publish_maintenance_mode_event()

        extensionutils.execute_cleanup_extension()
        self.log.info("cleaning up finished in the cartridge instance...")

        self.log.info("publishing ready to shutdown event...")
        cartridgeagentpublisher.publish_instance_ready_to_shutdown_event()

    def find_tenant_domain(self, tenant_id):
        tenant = TenantContext.get_tenant(tenant_id)
        if tenant is None:
            raise RuntimeError("Tenant could not be found: [tenant-id] %r" % tenant_id)

        return tenant.tenant_domain

from ..artifactmgt.git import agentgithandler
from ..artifactmgt.repositoryinformation import RepositoryInformation
from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ..publisher import cartridgeagentpublisher
from ..exception.parameternotfoundexception import ParameterNotFoundException
from ..topology.topologycontext import *
from ..tenant.tenantcontext import *
from ..util.log import LogFactory
