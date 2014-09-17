class DefaultExceptionHandler:
    def __init__(self):
        pass

    def onInstanceStartedEvent(self):
        pass

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