import logging

from .. artifactmgt.git.agentgithandler import AgentGitHandler
from .. artifactmgt.repositoryinformation import RepositoryInformation
from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from .. util import extensionutils, cartridgeagentconstants, cartridgeagentutils
from .. publisher import cartridgeagentpublisher
from .. exception.parameternotfoundexception import ParameterNotFoundException
from .. topology.topologycontext import *


class DefaultExtensionHandler:
    log = None

    def __init__(self):
        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)
        self.wk_members = []

    def on_instance_started_event(self):
        try:
            self.log.debug("Processing instance started event...")
            if CartridgeAgentConfiguration.is_multitenant:
                artifact_source = "%r/repository/deployment/server/" % CartridgeAgentConfiguration.app_path
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
                      (artifacts_updated_event.tenant_id, artifacts_updated_event.cluster_id, artifacts_updated_event.status))

        cluster_id_event = str(artifacts_updated_event.cluster_id).strip()
        cluster_id_payload = CartridgeAgentConfiguration.cluster_id
        repo_url = str(artifacts_updated_event.repo_url).strip()

        if (repo_url != "") and (cluster_id_payload is not None) and (cluster_id_payload == cluster_id_event):
            local_repo_path = CartridgeAgentConfiguration.app_path

            secret = CartridgeAgentConfiguration.cartridge_key
            repo_password = cartridgeagentutils.decrypt_password(artifacts_updated_event.repo_password, secret)

            repo_username = artifacts_updated_event.repo_username
            tenant_id = artifacts_updated_event.tenant_id
            is_multitenant = CartridgeAgentConfiguration.is_multitenant()
            commit_enabled = artifacts_updated_event.commit_enabled

            self.log.info("Executing git checkout")

            # create repo object
            repo_info = RepositoryInformation(repo_url, repo_username, repo_password, local_repo_path, tenant_id,
                                              is_multitenant, commit_enabled)

            #checkout code
            checkout_result = AgentGitHandler.checkout(repo_info)
            #repo_context = checkout_result["repo_context"]
            #execute artifact updated extension
            env_params = {"STRATOS_ARTIFACT_UPDATED_CLUSTER_ID": artifacts_updated_event.cluster_id,
                          "STRATOS_ARTIFACT_UPDATED_TENANT_ID": artifacts_updated_event.tenant_id,
                          "STRATOS_ARTIFACT_UPDATED_REPO_URL": artifacts_updated_event.repo_url,
                          "STRATOS_ARTIFACT_UPDATED_REPO_PASSWORD": artifacts_updated_event.repo_password,
                          "STRATOS_ARTIFACT_UPDATED_REPO_USERNAME": artifacts_updated_event.repo_username,
                          "STRATOS_ARTIFACT_UPDATED_STATUS": artifacts_updated_event.status}

            extensionutils.execute_artifacts_updated_extension(env_params)

            if checkout_result["subscribe_run"]:
                #publish instanceActivated
                cartridgeagentpublisher.publish_instance_activated_event()

            update_artifacts = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.ENABLE_ARTIFACT_UPDATE)
            update_artifacts = True if str(update_artifacts).strip().lower() == "true" else False
            if update_artifacts:
                auto_commit = CartridgeAgentConfiguration.is_commits_enabled()
                auto_checkout = CartridgeAgentConfiguration.is_checkout_enabled()

                try:
                    update_interval = len(CartridgeAgentConfiguration.read_property(cartridgeagentconstants.ARTIFACT_UPDATE_INTERVAL))
                except ParameterNotFoundException:
                    self.log.exception("Invalid artifact sync interval specified ")
                    update_interval = 10
                except ValueError:
                    self.log.exception("Invalid artifact sync interval specified ")
                    update_interval = 10

                self.log.info("Artifact updating task enabled, update interval: %r seconds" % update_interval)

                self.log.info("Auto Commit is turned %r " % "on" if auto_commit else "off")
                self.log.info("Auto Checkout is turned %r " % "on" if auto_checkout else "off")

                AgentGitHandler.schedule_artifact_update_scheduled_task(repo_info, auto_checkout, auto_commit, update_interval)

    def on_artifact_update_scheduler_event(self, tenant_id):
        env_params = {}
        env_params["STRATOS_ARTIFACT_UPDATED_TENANT_ID"] = tenant_id
        env_params["STRATOS_ARTIFACT_UPDATED_SCHEDULER"] = True
        extensionutils.execute_artifacts_updated_extension(env_params)

    def on_instance_cleanup_cluster_event(self, instanceCleanupClusterEvent):
        self.cleanup()

    def on_instance_cleanup_member_event(self, instanceCleanupMemberEvent):
        self.cleanup()

    def on_member_activated_event(self, member_activated_event):
        self.log.info("Member activated event received: [service] %r [cluster] %r [member] %r"
                      % (member_activated_event.service_name, member_activated_event.cluster_id, member_activated_event.member_id))

        consistant = extensionutils.check_topology_consistency(member_activated_event.service_name, member_activated_event.cluster_id, member_activated_event.member_id)
        if not consistant:
            self.log.error("Topology is inconsistent...failed to execute member activated event")
            return

        topology = TopologyContext.get_topology()
        service = topology.service_map[member_activated_event.service_name]
        cluster = service.get_cluster(member_activated_event.cluster_id)
        member = cluster.get_member(member_activated_event.member_id)
        lb_cluster_id = member.lb_cluster_id

        if extensionutils.is_relevant_member_event(member_activated_event.service_name, member_activated_event.cluster_id, lb_cluster_id)
            env_params = {}
            env_params["STRATOS_MEMBER_ACTIVATED_MEMBER_IP"] = member_activated_event.member_ip
            env_params["STRATOS_MEMBER_ACTIVATED_MEMBER_ID"] = member_activated_event.member_id
            env_params["STRATOS_MEMBER_ACTIVATED_CLUSTER_ID"] = member_activated_event.cluster_id
            env_params["STRATOS_MEMBER_ACTIVATED_LB_CLUSTER_ID"] = lb_cluster_id
            env_params["STRATOS_MEMBER_ACTIVATED_NETWORK_PARTITION_ID"] = member_activated_event.network_partition_id
            env_params["STRATOS_MEMBER_ACTIVATED_SERVICE_NAME"] = member_activated_event.service_name

            ports = member_activated_event.port_map.values()
            ports_str = ""
            for port in ports:
                ports_str += port.protocol + "," + port.value + "," + port.proxy + "|"

            env_params["STRATOS_MEMBER_ACTIVATED_PORTS"] = ports_str

            members = cluster.get_members()
            member_list_json = ""
            for member in members:
                member_list_json += member.json_str + ","
            env_params["STRATOS_MEMBER_ACTIVATED_MEMBER_LIST_JSON"] = member_list_json[:-1] # removing last comma

            member_ips = extensionutils.get_lb_member_ip(lb_cluster_id)
            if member_ips is not None and len(member_ips) > 1:
                env_params["STRATOS_MEMBER_ACTIVATED_LB_IP"] = member_ips[0]
                env_params["STRATOS_MEMBER_ACTIVATED_LB_PUBLIC_IP"] = member_ips[1]

            env_params["STRATOS_TOPOLOGY_JSON"] = topology.json_str

            extensionutils.add_properties(service.properties, env_params,  "MEMBER_ACTIVATED_SERVICE_PROPERTY")
            extensionutils.add_properties(cluster.properties, env_params,  "MEMBER_ACTIVATED_CLUSTER_PROPERTY")
            extensionutils.add_properties(member.properties, env_params,  "MEMBER_ACTIVATED_MEMBER_PROPERTY")

            clustered = CartridgeAgentConfiguration.is_clustered

            if member.properties is not None and member.properties[cartridgeagentconstants.CLUSTERING_PRIMARY_KEY] == "true" and clustered is not None and clustered:
                self.log.debug(" If WK member is re-spawned, update axis2.xml ")

                has_wk_ip_changed = True
                for wk_member in self.wk_members:
                    if wk_member.member_ip == member_activated_event.member_ip:
                        has_wk_ip_changed = False

                self.log.debug(" hasWKIpChanged %r" + has_wk_ip_changed)

                min_count = int(CartridgeAgentConfiguration.min_count)
                is_wk_member_grp_ready = self.is_wk_member_group_ready(env_params, min_count)
                self.log.debug("MinCount: %r" % min_count)
                self.log.debug("is_wk_member_grp_ready : %r" % is_wk_member_grp_ready)

                if has_wk_ip_changed and is_wk_member_grp_ready:
                    self.log.debug("Setting env var STRATOS_UPDATE_WK_IP to true")
                    env_params["STRATOS_UPDATE_WK_IP"] = "true"

            self.log.debug("Setting env var STRATOS_CLUSTERING to %r" % clustered)
            env_params["STRATOS_CLUSTERING"] = clustered
            env_params["STRATOS_WK_MEMBER_COUNT"] = CartridgeAgentConfiguration.min_count

            extensionutils.execute_member_activated_extension(env_params)
        else:
            self.log.debug("Member activated event is not relevant...skipping agent extension")

    def onCompleteTopologyEvent(self, completeTopologyEvent):
        pass

    def onCompleteTenantEvent(self, completeTenantEvent):
        pass

    def onMemberTerminatedEvent(self, memberTerminatedEvent):
        pass

    def onMemberSuspendedEvent(self, memberSuspendedEvent):
        pass

    def onMemberStartedEvent(self, memberStartedEvent):
        pass

    def start_server_extension(self):
        raise NotImplementedError
        # extensionutils.wait_for_complete_topology()
        # self.log.info("[start server extension] complete topology event received")
        #
        # service_name_in_payload = CartridgeAgentConfiguration.service_name()
        # cluster_id_in_payload = CartridgeAgentConfiguration.cluster_id()
        # member_id_in_payload = CartridgeAgentConfiguration.member_id()
        #
        # try:
        #     consistant = extensionutils.check_topology_consistency(service_name_in_payload, cluster_id_in_payload, member_id_in_payload)
        #
        #     if not consistant:
        #         self.log.error("Topology is inconsistent...failed to execute start server event")
        #         return
        #
        #
        # except:
        #     self.log.exception("Error processing start servers event")
        # finally:
        #     pass

    def volume_mount_extension(self, persistence_mappings_payload):
        extensionutils.execute_volume_mount_extension(persistence_mappings_payload)

    def onSubscriptionDomainAddedEvent(self, subscriptionDomainAddedEvent):
        pass

    def onSubscriptionDomainRemovedEvent(self, subscriptionDomainRemovedEvent):
        pass

    def onCopyArtifactsExtension(self, src, des):
        pass

    def onTenantSubscribedEvent(self, tenantSubscribedEvent):
        pass

    def onTenantUnSubscribedEvent(self, tenantUnSubscribedEvent):
        pass

    def cleanup(self):
        self.log.info("Executing cleaning up the data in the cartridge instance...")

        cartridgeagentpublisher.publish_maintenance_mode_event()

        extensionutils.execute_cleanup_extension()
        self.log.info("cleaning up finished in the cartridge instance...")

        self.log.info("publishing ready to shutdown event...")
        cartridgeagentpublisher.publish_instance_ready_to_shutdown_event()

    def is_wk_member_group_ready(self, env_params, min_count):
        raise NotImplementedError