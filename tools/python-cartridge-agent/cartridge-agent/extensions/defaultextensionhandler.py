import logging

from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ..util import extensionutils, cartridgeagentconstants


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

        except Exception as ex:
            self.log.exception("Error processing instance started event")


    def onInstanceActivatedEvent(self):
        pass

    def onArtifactUpdatedEvent(self, artifact_updated_event):
        pass

    def onArtifactUpdateSchedulerEvent(self, tenantId):
        pass

    def onInstanceCleanupClusterEvent(self, instanceCleanupClusterEvent):
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

    def startServerExtension(self):
        pass

    def volumeMountExtension(self, persistenceMappingsPayload):
        pass

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