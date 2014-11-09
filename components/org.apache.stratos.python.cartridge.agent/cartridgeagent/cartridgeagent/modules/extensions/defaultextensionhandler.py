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

import time
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
        try:
            self.log.debug("Processing instance started event...")
            if self.cartridge_agent_config.is_multitenant:
                artifact_source = "%r/repository/deployment/server/" % self.cartridge_agent_config.app_path
                artifact_dest = cartridgeagentconstants.SUPERTENANT_TEMP_PATH
                extensionutils.execute_copy_artifact_extension(artifact_source, artifact_dest)

            env_params = {}
            extensionutils.execute_instance_started_extension(env_params)
        except:
            self.log.exception("Error processing instance started event")

    def on_instance_activated_event(self):
        extensionutils.execute_instance_activated_extension()

    def on_artifact_updated_event(self, artifacts_updated_event):
        self.log.info("Artifact update event received: [tenant] %r [cluster] %r [status] %r" %
                      (artifacts_updated_event.tenant_id, artifacts_updated_event.cluster_id,
                       artifacts_updated_event.status))

        cluster_id_event = str(artifacts_updated_event.cluster_id).strip()
        cluster_id_payload = self.cartridge_agent_config.cluster_id
        repo_url = str(artifacts_updated_event.repo_url).strip()

        if (repo_url != "") and (cluster_id_payload is not None) and (cluster_id_payload == cluster_id_event):
            local_repo_path = self.cartridge_agent_config.app_path

            secret = self.cartridge_agent_config.cartridge_key
            #repoPassword = https://mb_ip:9443/stratosmetadataservice/app_id/alias/repoPassword
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

            update_artifacts = self.cartridge_agent_config.read_property(cartridgeagentconstants.ENABLE_ARTIFACT_UPDATE, False)
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
        env_params = {"STRATOS_ARTIFACT_UPDATED_TENANT_ID": str(tenant_id), "STRATOS_ARTIFACT_UPDATED_SCHEDULER": str(True)}

        extensionutils.execute_artifacts_updated_extension(env_params)

    def on_instance_cleanup_cluster_event(self, instance_cleanup_cluster_event):
        self.cleanup()

    def on_instance_cleanup_member_event(self, instance_cleanup_member_event):
        self.cleanup()

    def on_member_activated_event(self, member_activated_event):
        self.log.info("Member activated event received: [service] %r [cluster] %r [member] %r"
            % (member_activated_event.service_name, member_activated_event.cluster_id, member_activated_event.member_id))

        topology_consistent = extensionutils.check_topology_consistency(
            member_activated_event.service_name,
            member_activated_event.cluster_id,
            member_activated_event.member_id)

        if not topology_consistent:
            self.log.error("Topology is inconsistent...failed to execute member activated event")
            return

        topology = TopologyContext.get_topology()
        service = topology.get_service(member_activated_event.service_name)
        cluster = service.get_cluster(member_activated_event.cluster_id)
        member = cluster.get_member(member_activated_event.member_id)
        lb_cluster_id = member.lb_cluster_id

        if extensionutils.is_relevant_member_event(member_activated_event.service_name,
                                                   member_activated_event.cluster_id, lb_cluster_id):

            env_params = {"STRATOS_MEMBER_ACTIVATED_MEMBER_IP": str(member_activated_event.member_ip),
                          "STRATOS_MEMBER_ACTIVATED_MEMBER_ID": str(member_activated_event.member_id),
                          "STRATOS_MEMBER_ACTIVATED_CLUSTER_ID": str(member_activated_event.cluster_id),
                          "STRATOS_MEMBER_ACTIVATED_LB_CLUSTER_ID": str(lb_cluster_id),
                          "STRATOS_MEMBER_ACTIVATED_NETWORK_PARTITION_ID": str(member_activated_event.network_partition_id),
                          "STRATOS_MEMBER_ACTIVATED_SERVICE_NAME": str(member_activated_event.service_name)}

            ports = member_activated_event.port_map.values()
            ports_str = ""
            for port in ports:
                ports_str += port.protocol + "," + str(port.value) + "," + str(port.proxy) + "|"

            env_params["STRATOS_MEMBER_ACTIVATED_PORTS"] = ports_str

            env_params["STRATOS_MEMBER_ACTIVATED_MEMBER_LIST_JSON"] = json.dumps(cluster.member_list_json)

            member_ips = extensionutils.get_lb_member_ip(lb_cluster_id)
            if member_ips is not None and len(member_ips) > 1:
                env_params["STRATOS_MEMBER_ACTIVATED_LB_IP"] = str(member_ips[0])
                env_params["STRATOS_MEMBER_ACTIVATED_LB_PUBLIC_IP"] = str(member_ips[1])

            env_params["STRATOS_TOPOLOGY_JSON"] = json.dumps(topology.json_str)

            extensionutils.add_properties(service.properties, env_params, "MEMBER_ACTIVATED_SERVICE_PROPERTY")
            extensionutils.add_properties(cluster.properties, env_params, "MEMBER_ACTIVATED_CLUSTER_PROPERTY")
            extensionutils.add_properties(member.properties, env_params, "MEMBER_ACTIVATED_MEMBER_PROPERTY")

            clustered = self.cartridge_agent_config.is_clustered

            if member.properties is not None and cartridgeagentconstants.CLUSTERING_PRIMARY_KEY in member.properties \
                    and member.properties[cartridgeagentconstants.CLUSTERING_PRIMARY_KEY] == "true" \
                    and clustered is not None and clustered:

                self.log.debug(" If WK member is re-spawned, update axis2.xml ")

                has_wk_ip_changed = True
                for wk_member in self.wk_members:
                    if wk_member.member_ip == member_activated_event.member_ip:
                        has_wk_ip_changed = False

                self.log.debug(" hasWKIpChanged %r" + has_wk_ip_changed)

                min_count = int(self.cartridge_agent_config.min_count)
                is_wk_member_grp_ready = self.is_wk_member_group_ready(env_params, min_count)
                self.log.debug("MinCount: %r" % min_count)
                self.log.debug("is_wk_member_grp_ready : %r" % is_wk_member_grp_ready)

                if has_wk_ip_changed and is_wk_member_grp_ready:
                    self.log.debug("Setting env var STRATOS_UPDATE_WK_IP to true")
                    env_params["STRATOS_UPDATE_WK_IP"] = "true"

            self.log.debug("Setting env var STRATOS_CLUSTERING to %r" % clustered)
            env_params["STRATOS_CLUSTERING"] = str(clustered)
            env_params["STRATOS_WK_MEMBER_COUNT"] = str(self.cartridge_agent_config.min_count)

            extensionutils.execute_member_activated_extension(env_params)
        else:
            self.log.debug("Member activated event is not relevant...skipping agent extension")

    def on_complete_topology_event(self, complete_topology_event):
        self.log.debug("Complete topology event received")

        service_name_in_payload = self.cartridge_agent_config.service_name
        cluster_id_in_payload = self.cartridge_agent_config.cluster_id
        member_id_in_payload = self.cartridge_agent_config.member_id

        consistant = extensionutils.check_topology_consistency(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        if not consistant:
            return
        else:
            self.cartridge_agent_config.initialized = True

        topology = complete_topology_event.get_topology()
        service = topology.get_service(service_name_in_payload)
        cluster = service.get_cluster(cluster_id_in_payload)

        env_params = {"STRATOS_TOPOLOGY_JSON": json.dumps(topology.json_str), "STRATOS_MEMBER_LIST_JSON": json.dumps(cluster.member_list_json)}

        extensionutils.execute_complete_topology_extension(env_params)

    def on_instance_spawned_event(self, instance_spawned_event):
        self.log.debug("Instance Spawned event received")

        service_name_in_payload = self.cartridge_agent_config.service_name
        cluster_id_in_payload = self.cartridge_agent_config.cluster_id
        member_id_in_payload = self.cartridge_agent_config.member_id

        consistant = extensionutils.check_topology_consistency(
            service_name_in_payload,
            cluster_id_in_payload,
            member_id_in_payload)

        if not consistant:
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

        topology_consistent = extensionutils.check_topology_consistency(
            member_terminated_event.service_name,
            member_terminated_event.cluster_id,
            member_terminated_event.member_id
        )

        if not topology_consistent:
            self.log.error("Topology is inconsistent...failed to execute member terminated event")
            return

        topology = TopologyContext.get_topology()
        service = topology.get_service(member_terminated_event.service_name)
        cluster = service.get_cluster(member_terminated_event.cluster_id)
        terminated_member = cluster.get_member(member_terminated_event.member_id)
        lb_cluster_id = cluster.get_member(member_terminated_event.member_id).lb_cluster_id

        #check whether terminated member is within the same cluster, LB cluster or service group
        if extensionutils.is_relevant_member_event(
                member_terminated_event.service_name,
                member_terminated_event.cluster_id,
                lb_cluster_id):

            env_params = {"STRATOS_MEMBER_TERMINATED_MEMBER_IP": terminated_member.member_ip,
                          "STRATOS_MEMBER_TERMINATED_MEMBER_ID": member_terminated_event.member_id,
                          "STRATOS_MEMBER_TERMINATED_CLUSTER_ID": member_terminated_event.cluster_id,
                          "STRATOS_MEMBER_TERMINATED_LB_CLUSTER_ID": lb_cluster_id,
                          "STRATOS_MEMBER_TERMINATED_NETWORK_PARTITION_ID": member_terminated_event.network_partition_id,
                          "STRATOS_MEMBER_TERMINATED_SERVICE_NAME": member_terminated_event.service_name,
                          "STRATOS_MEMBER_TERMINATED_MEMBER_LIST_JSON": json.dumps(cluster.member_list_json),
                          "STRATOS_TOPOLOGY_JSON": json.dumps(topology.json_str)}

            member_ips = extensionutils.get_lb_member_ip(lb_cluster_id)
            if member_ips is not None and len(member_ips) > 1:
                env_params["STRATOS_MEMBER_TERMINATED_LB_IP"] = member_ips[0]
                env_params["STRATOS_MEMBER_TERMINATED_LB_PUBLIC_IP"] = member_ips[1]

            extensionutils.add_properties(service.properties, env_params, "MEMBER_TERMINATED_SERVICE_PROPERTY")
            extensionutils.add_properties(cluster.properties, env_params, "MEMBER_TERMINATED_CLUSTER_PROPERTY")
            extensionutils.add_properties(terminated_member.properties, env_params, "MEMBER_TERMINATED_MEMBER_PROPERTY")

            extensionutils.execute_member_terminated_extension(env_params)

        else:
            self.log.debug("Member terminated event is not relevant...skipping agent extension")

    def on_member_suspended_event(self, member_suspended_event):
        self.log.info("Member suspended event received: [service] " + member_suspended_event.service_name +
                      " [cluster] " + member_suspended_event.cluster_id + " [member] " + member_suspended_event.member_id)

        topology_consistent = extensionutils.check_topology_consistency(
            member_suspended_event.service_name,
            member_suspended_event.cluster_id,
            member_suspended_event.member_id
        )

        if not topology_consistent:
            self.log.error("Topology is inconsistent...failed to execute member suspended event")
            return

        topology = TopologyContext.get_topology()
        service = topology.get_service(member_suspended_event.service_name)
        cluster = service.get_cluster(member_suspended_event.cluster_id)
        suspended_member = cluster.get_member(member_suspended_event.member_id)
        lb_cluster_id = cluster.get_member(member_suspended_event.member_id).lb_cluster_id

        #check whether suspended member is within the same cluster, LB cluster or service group
        if extensionutils.is_relevant_member_event(
                member_suspended_event.service_name,
                member_suspended_event.cluster_id,
                lb_cluster_id):

            env_params = {"STRATOS_MEMBER_SUSPENDED_MEMBER_IP": member_suspended_event.member_ip,
                          "STRATOS_MEMBER_SUSPENDED_MEMBER_ID": member_suspended_event.member_id,
                          "STRATOS_MEMBER_SUSPENDED_CLUSTER_ID": member_suspended_event.cluster_id,
                          "STRATOS_MEMBER_SUSPENDED_LB_CLUSTER_ID": lb_cluster_id,
                          "STRATOS_MEMBER_SUSPENDED_NETWORK_PARTITION_ID": member_suspended_event.network_partition_id,
                          "STRATOS_MEMBER_SUSPENDED_SERVICE_NAME": member_suspended_event.service_name,
                          "STRATOS_MEMBER_SUSPENDED_MEMBER_LIST_JSON": json.dumps(cluster.member_list_json),
                          "STRATOS_TOPOLOGY_JSON": json.dumps(topology.json_str)}

            member_ips = extensionutils.get_lb_member_ip(lb_cluster_id)
            if member_ips is not None and len(member_ips) > 1:
                env_params["STRATOS_MEMBER_SUSPENDED_LB_IP"] = member_ips[0]
                env_params["STRATOS_MEMBER_SUSPENDED_LB_PUBLIC_IP"] = member_ips[1]

            extensionutils.add_properties(service.properties, env_params, "MEMBER_SUSPENDED_SERVICE_PROPERTY")
            extensionutils.add_properties(cluster.properties, env_params, "MEMBER_SUSPENDED_CLUSTER_PROPERTY")
            extensionutils.add_properties(suspended_member.properties, env_params, "MEMBER_SUSPENDED_MEMBER_PROPERTY")

            extensionutils.execute_member_suspended_extension(env_params)

        else:
            self.log.debug("Member suspended event is not relevant...skipping agent extension")

    def on_member_started_event(self, member_started_event):
        self.log.info("Member started event received: [service] " + member_started_event.service_name +
                      " [cluster] " + member_started_event.cluster_id + " [member] " + member_started_event.member_id)

        topology_consistent = extensionutils.check_topology_consistency(
            member_started_event.service_name,
            member_started_event.cluster_id,
            member_started_event.member_id
        )

        if not topology_consistent:
            self.log.error("Topology is inconsistent...failed to execute member started event")
            return

        topology = TopologyContext.get_topology()
        service = topology.get_service(member_started_event.service_name)
        cluster = service.get_cluster(member_started_event.cluster_id)
        started_member = cluster.get_member(member_started_event.member_id)
        lb_cluster_id = cluster.get_member(member_started_event.member_id).lb_cluster_id

        #check whether started member is within the same cluster, LB cluster or service group
        if extensionutils.is_relevant_member_event(
                member_started_event.service_name,
                member_started_event.cluster_id,
                lb_cluster_id):

            env_params = {"STRATOS_MEMBER_STARTED_MEMBER_IP": started_member.member_ip,
                          "STRATOS_MEMBER_STARTED_MEMBER_ID": member_started_event.member_id,
                          "STRATOS_MEMBER_STARTED_CLUSTER_ID": member_started_event.cluster_id,
                          "STRATOS_MEMBER_STARTED_LB_CLUSTER_ID": lb_cluster_id,
                          "STRATOS_MEMBER_STARTED_NETWORK_PARTITION_ID": member_started_event.network_partition_id,
                          "STRATOS_MEMBER_STARTED_SERVICE_NAME": member_started_event.service_name,
                          "STRATOS_MEMBER_STARTED_MEMBER_LIST_JSON": json.dumps(cluster.member_list_json),
                          "STRATOS_TOPOLOGY_JSON": json.dumps(topology.json_str)}

            member_ips = extensionutils.get_lb_member_ip(lb_cluster_id)
            if member_ips is not None and len(member_ips) > 1:
                env_params["STRATOS_MEMBER_STARTED_LB_IP"] = member_ips[0]
                env_params["STRATOS_MEMBER_STARTED_LB_PUBLIC_IP"] = member_ips[1]

            extensionutils.add_properties(service.properties, env_params, "MEMBER_STARTED_SERVICE_PROPERTY")
            extensionutils.add_properties(cluster.properties, env_params, "MEMBER_STARTED_CLUSTER_PROPERTY")
            extensionutils.add_properties(started_member.properties, env_params, "MEMBER_STARTED_MEMBER_PROPERTY")

            extensionutils.execute_member_started_extension(env_params)

        else:
            self.log.debug("Member started event is not relevant...skipping agent extension")

    def start_server_extension(self):
        #wait until complete topology message is received to get LB IP
        extensionutils.wait_for_complete_topology()
        self.log.info("[start server extension] complete topology event received")

        service_name_in_payload = self.cartridge_agent_config.service_name
        cluster_id_in_payload = self.cartridge_agent_config.cluster_id
        member_id_in_payload = self.cartridge_agent_config.member_id

        topology_consistant = extensionutils.check_topology_consistency(service_name_in_payload, cluster_id_in_payload, member_id_in_payload)

        try:
            if not topology_consistant:
                self.log.error("Topology is inconsistent...failed to execute start server event")
                return

            topology = TopologyContext.get_topology()
            service = topology.get_service(service_name_in_payload)
            cluster = service.get_cluster(cluster_id_in_payload)

            # store environment variable parameters to be passed to extension shell script
            env_params = {}

            # if clustering is enabled wait until all well known members have started
            clustering_enabled = self.cartridge_agent_config.is_clustered
            if clustering_enabled:
                env_params["STRATOS_CLUSTERING"] = "true"
                env_params["STRATOS_WK_MEMBER_COUNT"] = self.cartridge_agent_config.min_count

                env_params["STRATOS_PRIMARY"] = "true" if self.cartridge_agent_config.is_primary else "false"

                self.wait_for_wk_members(env_params)
                self.log.info("All well known members have started! Resuming start server extension...")

            env_params["STRATOS_TOPOLOGY_JSON"] = json.dumps(topology.json_str)
            env_params["STRATOS_MEMBER_LIST_JSON"] = json.dumps(cluster.member_list_json)

            extensionutils.execute_start_servers_extension(env_params)

        except:
            self.log.exception("Error processing start servers event")

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

    def on_tenant_unsubscribed_event(self, tenant_unsubscribed_event):
        self.log.info(
            "Tenant unsubscribed event received: [tenant] " + tenant_unsubscribed_event.tenant_id +
            " [service] " + tenant_unsubscribed_event.service_name +
            " [cluster] " + tenant_unsubscribed_event.cluster_ids
        )

        try:
            if self.cartridge_agent_config.service_name == tenant_unsubscribed_event.service_name:
                agentgithandler.AgentGitHandler.remove_repo(tenant_unsubscribed_event.tenant_id)
        except:
            self.log.exception("Removing git repository failed: ")
        extensionutils.execute_tenant_unsubscribed_extension({})

    def cleanup(self):
        self.log.info("Executing cleaning up the data in the cartridge instance...")

        cartridgeagentpublisher.publish_maintenance_mode_event()

        extensionutils.execute_cleanup_extension()
        self.log.info("cleaning up finished in the cartridge instance...")

        self.log.info("publishing ready to shutdown event...")
        cartridgeagentpublisher.publish_instance_ready_to_shutdown_event()

    def is_wk_member_group_ready(self, env_params, min_count):
        topology = TopologyContext.get_topology()
        if topology is None or not topology.initialized:
            return False

        service_group_in_payload = self.cartridge_agent_config.service_group
        if service_group_in_payload is not None:
            env_params["STRATOS_SERVICE_GROUP"] = service_group_in_payload

        # clustering logic for apimanager
        if service_group_in_payload is not None and service_group_in_payload == "apim":
            # handle apistore and publisher case
            if self.cartridge_agent_config.service_name == cartridgeagentconstants.APIMANAGER_SERVICE_NAME or \
                    self.cartridge_agent_config.service_name == cartridgeagentconstants.PUBLISHER_SERVICE_NAME:

                apistore_cluster_collection = topology.get_service(cartridgeagentconstants.APIMANAGER_SERVICE_NAME)\
                    .get_clusters()
                publisher_cluster_collection = topology.get_service(cartridgeagentconstants.PUBLISHER_SERVICE_NAME)\
                    .get_clusters()

                apistore_member_list = []
                for member in apistore_cluster_collection[0].get_members():
                    if member.status == MemberStatus.Starting or member.status == MemberStatus.Activated:
                        apistore_member_list.append(member)
                        self.wk_members.append(member)

                if len(apistore_member_list) == 0:
                    self.log.debug("API Store members not yet created")
                    return False

                apistore_member = apistore_member_list[0]
                env_params["STRATOS_WK_APISTORE_MEMBER_IP"] = apistore_member.member_ip
                self.log.debug("STRATOS_WK_APISTORE_MEMBER_IP: %r" % apistore_member.member_ip)

                publisher_member_list = []
                for member in publisher_cluster_collection[0].get_members():
                    if member.status == MemberStatus.Starting or member.status == MemberStatus.Activated:
                        publisher_member_list.append(member)
                        self.wk_members.append(member)

                if len(publisher_member_list) == 0:
                    self.log.debug("API Publisher members not yet created")

                publisher_member = publisher_member_list[0]
                env_params["STRATOS_WK_PUBLISHER_MEMBER_IP"] = publisher_member.member_ip
                self.log.debug("STRATOS_WK_PUBLISHER_MEMBER_IP: %r" % publisher_member.member_ip)

                return True

            elif self.cartridge_agent_config.service_name == cartridgeagentconstants.GATEWAY_MGT_SERVICE_NAME or \
                    self.cartridge_agent_config.service_name == cartridgeagentconstants.GATEWAY_SERVICE_NAME:

                if self.cartridge_agent_config.deployment is not None:
                    # check if deployment is Manager Worker separated
                    if self.cartridge_agent_config.deployment.lower() == cartridgeagentconstants.DEPLOYMENT_MANAGER.lower() or \
                            self.cartridge_agent_config.deployment.lower() == cartridgeagentconstants.DEPLOYMENT_WORKER.lower():

                        self.log.info("Deployment pattern for the node: %r" % self.cartridge_agent_config.deployment)
                        env_params["DEPLOYMENT"] = self.cartridge_agent_config.deployment
                        # check if WKA members of Manager Worker separated deployment is ready
                        return self.is_manager_worker_WKA_group_ready(env_params)

            elif self.cartridge_agent_config.service_name == cartridgeagentconstants.KEY_MANAGER_SERVICE_NAME:
                return True

        else:
            if self.cartridge_agent_config.deployment is not None:
                # check if deployment is Manager Worker separated
                if self.cartridge_agent_config.deployment.lower() == cartridgeagentconstants.DEPLOYMENT_MANAGER.lower() or \
                        self.cartridge_agent_config.deployment.lower() == cartridgeagentconstants.DEPLOYMENT_WORKER.lower():

                    self.log.info("Deployment pattern for the node: %r" % self.cartridge_agent_config.deployment)
                    env_params["DEPLOYMENT"] = self.cartridge_agent_config.deployment
                    # check if WKA members of Manager Worker separated deployment is ready
                    return self.is_manager_worker_WKA_group_ready(env_params)

            service_name_in_payload = self.cartridge_agent_config.service_name
            cluster_id_in_payload = self.cartridge_agent_config.cluster_id
            service = topology.get_service(service_name_in_payload)
            cluster = service.get_cluster(cluster_id_in_payload)

            wk_members = []
            for member in cluster.get_members():
                if member.properties is not None and \
                        cartridgeagentconstants.PRIMARY in member.properties \
                        and member.properties[cartridgeagentconstants.PRIMARY].lower() == "true" and \
                        (member.status == MemberStatus.Starting or member.status == MemberStatus.Activated):

                    wk_members.append(member)
                    self.wk_members.append(member)
                    self.log.debug("Found WKA: STRATOS_WK_MEMBER_IP: " + member.member_ip)

            if len(wk_members) >= min_count:
                idx = 0
                for member in wk_members:
                    env_params["STRATOS_WK_MEMBER_" + idx + "_IP"] = member.member_ip
                    self.log.debug("STRATOS_WK_MEMBER_" + idx + "_IP:" + member.member_ip)

                    idx += 1

                return True

        return False

    # generic worker manager separated clustering logic
    def is_manager_worker_WKA_group_ready(self, env_params):

        # for this, we need both manager cluster service name and worker cluster service name
        manager_service_name = self.cartridge_agent_config.manager_service_name
        worker_service_name = self.cartridge_agent_config.worker_service_name

        # managerServiceName and workerServiceName both should not be null /empty
        if manager_service_name is None or manager_service_name.strip() == "":
            self.log.error("Manager service name [ " + manager_service_name + " ] is invalid")
            return False

        if worker_service_name is None or worker_service_name.strip() == "":
            self.log.error("Worker service name [ " + worker_service_name + " ] is invalid")
            return False

        min_manager_instances_available = False
        min_worker_instances_available = False

        topology = TopologyContext.get_topology()
        manager_service = topology.get_service(manager_service_name)
        worker_service = topology.get_service(worker_service_name)

        if manager_service is None:
            self.log.warn("Service [ " + manager_service_name + " ] is not found")
            return False

        if worker_service is None:
            self.log.warn("Service [ " + worker_service_name + " ] is not found")
            return False

        # manager clusters
        manager_clusters = manager_service.get_clusters()
        if manager_clusters is None or len(manager_clusters) == 0:
            self.log.warn("No clusters found for service [ " + manager_service_name + " ]")
            return False

        manager_min_instance_count = 1
        manager_min_instance_count_found = False

        manager_wka_members = []
        for member in manager_clusters[0].get_members():
            if member.properties is not None and \
                    cartridgeagentconstants.PRIMARY in member.properties \
                    and member.properties[cartridgeagentconstants.PRIMARY].lower() == "true" and \
                    (member.status == MemberStatus.Starting or member.status == MemberStatus.Activated):

                manager_wka_members.append(member)
                self.wk_members.append(member)

                # get the min instance count
                if not manager_min_instance_count_found:
                    manager_min_instance_count = self.get_min_instance_count_from_member(member)
                    manager_min_instance_count_found = True
                    self.log.info("Manager min instance count: " + manager_min_instance_count)

        if len(manager_wka_members) >= manager_min_instance_count:
            min_manager_instances_available = True
            idx = 0
            for member in manager_wka_members:
                env_params["STRATOS_WK_MANAGER_MEMBER_" + idx + "_IP"] = member.member_ip
                self.log.debug("STRATOS_WK_MANAGER_MEMBER_" + idx + "_IP: " + member.member_ip)
                idx += 1

            env_params["STRATOS_WK_MANAGER_MEMBER_COUNT"] = int(manager_min_instance_count)

        # If all the manager members are non primary and is greate than or equal to mincount,
        # minManagerInstancesAvailable should be true
        all_managers_non_primary = True
        for member in manager_clusters[0].get_members():
            # get the min instance count
            if not manager_min_instance_count_found:
                manager_min_instance_count = self.get_min_instance_count_from_member(member)
                manager_min_instance_count_found = True
                self.log.info(
                    "Manager min instance count when allManagersNonPrimary true : " + manager_min_instance_count)

            if member.properties is not None and cartridgeagentconstants.PRIMARY in member.properties and \
                    member.properties[cartridgeagentconstants.PRIMARY].lower() == "true":
                all_managers_non_primary = False
                break

        self.log.debug(
            " allManagerNonPrimary & managerMinInstanceCount [" + all_managers_non_primary +
            "], [" + manager_min_instance_count + "] ")

        if all_managers_non_primary and len(manager_clusters) >= manager_min_instance_count:
            min_manager_instances_available = True

        # worker cluster
        worker_clusters = worker_service.get_clusters()
        if worker_clusters is None or len(worker_clusters) == 0:
            self.log.warn("No clusters found for service [ " + worker_service_name + " ]")
            return False

        worker_min_instance_count = 1
        worker_min_instance_count_found = False

        worker_wka_members = []
        for member in worker_clusters[0].get_members():
            self.log.debug("Checking member : " + member.member_id)

            if member.properties is not None and cartridgeagentconstants.PRIMARY in member.properties and \
                    member.properties[cartridgeagentconstants.PRIMARY].lower() == "true" and \
                    (member.status == MemberStatus.Starting or member.status == MemberStatus.Activated):

                self.log.debug("Added worker member " + member.member_id)

                worker_wka_members.append(member)
                self.wk_members.append(member)

                # get the min instance count
                if not worker_min_instance_count_found:
                    worker_min_instance_count = self.get_min_instance_count_from_member(member)
                    worker_min_instance_count_found = True
                    self.log.debug("Worker min instance count: " + worker_min_instance_count)

        if len(worker_wka_members) >= worker_min_instance_count:
            min_worker_instances_available = True
            idx = 0
            for member in worker_wka_members:
                env_params["STRATOS_WK_WORKER_MEMBER_" + idx + "_IP"] = member.member_ip
                self.log.debug("STRATOS_WK_WORKER_MEMBER_" + idx + "_IP: " + member.member_ip)
                idx += 1

            env_params["STRATOS_WK_WORKER_MEMBER_COUNT"] = int(worker_min_instance_count)

        self.log.debug(
            " Returnning values minManagerInstancesAvailable && minWorkerInstancesAvailable [" +
            min_manager_instances_available + "],  [" + min_worker_instances_available + "] ")

        return min_manager_instances_available and min_worker_instances_available

    def get_min_instance_count_from_member(self, member):
        if cartridgeagentconstants.MIN_COUNT in member.properties:
            return int(member.properties[cartridgeagentconstants.MIN_COUNT])

        return 1

    def find_tenant_domain(self, tenant_id):
        tenant = TenantContext.get_tenant(tenant_id)
        if tenant is None:
            raise RuntimeError("Tenant could not be found: [tenant-id] %r" % tenant_id)

        return tenant.tenant_domain

    def wait_for_wk_members(self, env_params):
        min_count = int(self.cartridge_agent_config.min_count)
        is_wk_member_group_ready = False
        while not is_wk_member_group_ready:
            self.log.info("Waiting for %r well known members..." % min_count)

            time.sleep(5)

            is_wk_member_group_ready = self.is_wk_member_group_ready(env_params, min_count)

from ..artifactmgt.git import agentgithandler
from ..artifactmgt.repositoryinformation import RepositoryInformation
from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ..publisher import cartridgeagentpublisher
from ..exception.parameternotfoundexception import ParameterNotFoundException
from ..topology.topologycontext import *
from ..tenant.tenantcontext import *
from ..util.log import LogFactory