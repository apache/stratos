import logging

from ..artifactmgt.git.agentgithandler import AgentGitHandler
from ..artifactmgt.repositoryinformation import RepositoryInformation
from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from .. util import extensionutils, cartridgeagentconstants, cartridgeagentutils
from .. publisher import cartridgeagentpublisher


class DefaultExtensionHandler:
    log = None
    cartridge_agent_config = None

    def __init__(self):
        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)

        self.cartridge_agent_config = CartridgeAgentConfiguration()

        pass

    def on_instance_started_event(self):
        try:
            self.log.debug("Processing instance started event...")
            if self.cartridge_agent_config.is_multitenant():
                artifact_source = "%r/repository/deployment/server/" % self.cartridge_agent_config.get_app_path()
                artifact_dest = cartridgeagentconstants.SUPERTENANT_TEMP_PATH
                extensionutils.execute_copy_artifact_extension(artifact_source, artifact_dest)

            env_params = {}
            extensionutils.execute_instance_started_extention(env_params)

        except Exception:
            self.log.exception("Error processing instance started event")

    def on_instance_activated_event(self):
        extensionutils.execute_instance_activated_extension()

    def on_artifact_updated_event(self, event):
        self.log.info("Artifact update event received: [tenant] %r [cluster] %r [status] %r" %
                      (event.tenant_id, event.cluster_id, event.status))

        cluster_id_event = str(event.cluster_id).strip()
        cluster_id_payload = self.cartridge_agent_config.get_cluster_id()
        repo_url = str(event.repo_url).strip()

        if (repo_url != "") and (cluster_id_payload is not None) and (cluster_id_payload == cluster_id_event):
            local_repo_path = self.cartridge_agent_config.get_app_path()

            secret = self.cartridge_agent_config.get_cartridge_key()
            repo_password = cartridgeagentutils.decrypt_password(event.repo_password, secret)

            repo_username = event.repo_username
            tenant_id = event.tenant_id
            is_multitenant = self.cartridge_agent_config.is_multitenant()
            commit_enabled = event.commit_enabled

            self.log.info("Executing git checkout")

            # create repo object
            repo_info = RepositoryInformation(repo_url, repo_username, repo_password, local_repo_path, tenant_id,
                                              is_multitenant, commit_enabled)

            #checkout code
            checkout_result = AgentGitHandler.checkout(repo_info)
            #execute artifact updated extension
            env_params = {"STRATOS_ARTIFACT_UPDATED_CLUSTER_ID": event.cluster_id,
                          "STRATOS_ARTIFACT_UPDATED_TENANT_ID": event.tenant_id,
                          "STRATOS_ARTIFACT_UPDATED_REPO_URL": event.repo_url,
                          "STRATOS_ARTIFACT_UPDATED_REPO_PASSWORD": event.repo_password,
                          "STRATOS_ARTIFACT_UPDATED_REPO_USERNAME": event.repo_username,
                          "STRATOS_ARTIFACT_UPDATED_STATUS": event.status}

            extensionutils.execute_artifacts_updated_extension(env_params)

            #if !cloneExists publish instanceActivatedEvent
            if not checkout_result["cloned"]:
                #publish instanceActivated
                cartridgeagentpublisher.publish_instance_activated_event()

            #TODO: set artifact update task

    def on_artifact_update_scheduler_event(self, tenantId):
        pass

    def on_instance_cleanup_cluster_event(self, instanceCleanupClusterEvent):
        pass

    def onInstanceCleanupMemberEvent(self, instanceCleanupMemberEvent):
        pass

    def onMemberActivatedEvent(self, memberActivatedEvent):
        pass

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
        # service_name_in_payload = self.cartridge_agent_config.get_service_name()
        # cluster_id_in_payload = self.cartridge_agent_config.get_cluster_id()
        # member_id_in_payload = self.cartridge_agent_config.get_member_id()
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