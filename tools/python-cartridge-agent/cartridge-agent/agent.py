#!/usr/bin/env python
import logging
import threading
import time

from modules.config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from modules.exception.parameternotfoundexception import ParameterNotFoundException
from modules.subscriber.eventsubscriber import EventSubscriber
from modules.extensions.defaultextensionhandler import DefaultExtensionHandler
from modules.publisher import cartridgeagentpublisher
from modules.event.instance.notifier.events import *
from modules.event.tenant.events import *
from modules.event.topology.events import *
from modules.tenant.tenantcontext import *
from modules.topology.topologycontext import *


class CartridgeAgent(threading.Thread):
    logging.basicConfig(level=logging.DEBUG)
    log = logging.getLogger(__name__)

    def __init__(self):
        threading.Thread.__init__(self)
        CartridgeAgentConfiguration.initialize_configuration()
        self.__instance_event_subscriber = EventSubscriber(cartridgeagentconstants.INSTANCE_NOTIFIER_TOPIC)
        self.__tenant_event_subscriber = EventSubscriber(cartridgeagentconstants.TENANT_TOPIC)
        self.__topology_event_subscriber = EventSubscriber(cartridgeagentconstants.TOPOLOGY_TOPIC)

        self.extension_handler = DefaultExtensionHandler()

        self.__tenant_context_initialized = False
        self.__topology_context_initialized = False

    def run(self):
        self.log.info("Starting Cartridge Agent...")

        self.validate_required_properties()

        self.subscribe_to_topics_and_register_listeners()

        self.register_topology_event_listeners()

        self.register_tenant_event_listeners()

        self.extension_handler.on_instance_started_event()

        cartridgeagentpublisher.publish_instance_started_event()

        try:
            self.extension_handler.start_server_extension()
        except:
            self.log.exception("Error processing start servers event")

        cartridgeagentutils.wait_until_ports_active(
            CartridgeAgentConfiguration.listen_address,
            CartridgeAgentConfiguration.ports
        )

        repo_url = CartridgeAgentConfiguration.repo_url

        if repo_url is None or str(repo_url).strip() == "":
            self.log.info("No artifact repository found")
            self.extension_handler.on_instance_activated_event()

            cartridgeagentpublisher.publish_instance_activated_event()
        else:
            pass

        persistence_mappping_payload = CartridgeAgentConfiguration.persistence_mappings
        if persistence_mappping_payload is not None:
            self.extension_handler.volume_mount_extension(persistence_mappping_payload)

            # TODO: logpublisher shceduled event

            #TODO: wait until terminated is true

    def validate_required_properties(self):
        # JNDI_PROPERTIES_DIR
        try:
            CartridgeAgentConfiguration.read_property(cartridgeagentconstants.JNDI_PROPERTIES_DIR)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.JNDI_PROPERTIES_DIR)

        #PARAM_FILE_PATH
        try:
            CartridgeAgentConfiguration.read_property(cartridgeagentconstants.PARAM_FILE_PATH)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.PARAM_FILE_PATH)

        #EXTENSIONS_DIR
        try:
            CartridgeAgentConfiguration.read_property(cartridgeagentconstants.EXTENSIONS_DIR)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.EXTENSIONS_DIR)

    def subscribe_to_topics_and_register_listeners(self):
        self.log.debug("Starting instance notifier event message receiver thread")

        self.__instance_event_subscriber.register_handler("ArtifactUpdatedEvent", self.on_artifact_updated)
        self.__instance_event_subscriber.register_handler("InstanceCleanupMemberEvent", self.on_instance_cleanup_member)
        self.__instance_event_subscriber.register_handler("InstanceCleanupClusterEvent", self.on_instance_cleanup_cluster)

        self.__instance_event_subscriber.start()
        self.log.info("Instance notifier event message receiver thread started")

        # wait till subscribed to continue
        while not self.__instance_event_subscriber.is_subscribed():
            time.sleep(2)

    def on_artifact_updated(self, msg):
        event_obj = ArtifactUpdatedEvent.create_from_json(msg.payload)
        self.extension_handler.on_artifact_updated_event(event_obj)

    def on_instance_cleanup_member(self, msg):
        member_in_payload = CartridgeAgentConfiguration.member_id
        event_obj = InstanceCleanupMemberEvent.create_from_json(msg.payload)
        member_in_event = event_obj.member_id
        if member_in_payload == member_in_event:
            self.extension_handler.on_instance_cleanup_member_event(event_obj)

    def on_instance_cleanup_cluster(self, msg):
        event_obj = InstanceCleanupClusterEvent.create_from_json(msg.payload)
        cluster_in_payload = CartridgeAgentConfiguration.cluster_id
        cluster_in_event = event_obj.cluster_id

        if cluster_in_event == cluster_in_payload:
            self.extension_handler.on_instance_cleanup_cluster_event(event_obj)

    def register_topology_event_listeners(self):
        self.log.debug("Starting topology event message receiver thread")

        self.__topology_event_subscriber.register_handler("MemberActivatedEvent", self.on_member_activated)
        self.__topology_event_subscriber.register_handler("MemberTerminatedEvent", self.on_member_terminated)
        self.__topology_event_subscriber.register_handler("MemberSuspendedEvent", self.on_member_suspended)
        self.__topology_event_subscriber.register_handler("CompleteTopologyEvent", self.on_complete_topology)
        self.__topology_event_subscriber.register_handler("MemberStartedEvent", self.on_member_started)

        self.__topology_event_subscriber.start()
        self.log.info("Cartridge Agent topology receiver thread started")

    def on_member_activated(self, msg):
        raise NotImplementedError

    def on_member_terminated(self, msg):
        raise NotImplementedError

    def on_member_suspended(self, msg):
        raise NotImplementedError

    def on_complete_topology(self, msg):
        if not self.__topology_context_initialized:
            self.log.debug("Complete topology event received")
            event_obj = CompleteTopologyEvent.create_from_json(msg.payload)
            TopologyContext.update(event_obj.topology)
            self.__topology_context_initialized = True
            try:
                self.extension_handler.onCompleteTopologyEvent(event_obj)
            except:
                self.log.exception("Error processing complete topology event")
        else:
            self.log.info("Complete topology event updating task disabled")

    def on_member_started(self, msg):
        raise NotImplementedError

    def register_tenant_event_listeners(self):
        self.log.debug("Starting tenant event message receiver thread")
        self.__tenant_event_subscriber.register_handler("SubscriptionDomainAddedEvent", self.on_subscription_domain_added)
        self.__tenant_event_subscriber.register_handler("SubscriptionDomainsRemovedEvent", self.on_subscription_domain_removed)
        self.__tenant_event_subscriber.register_handler("CompleteTenantEvent", self.on_complete_tenant)
        self.__tenant_event_subscriber.register_handler("TenantSubscribedEvent", self.on_tenant_subscribed)
        self.__tenant_event_subscriber.register_handler("TenantUnSubscribedEvent", self.on_tenant_unsubscribed)

        self.__tenant_event_subscriber.start()
        self.log.info("Tenant event message receiver thread started")

    def on_subscription_domain_added(self, msg):
        self.log.debug("Subscription domain added event received")
        event_obj = SubscriptionDomainAddedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onSubscriptionDomainAddedEvent(event_obj)
        except:
            self.log.exception("Error processing subscription domains added event")
        # extensionutils.execute_subscription_domain_added_extension(
        #     event_obj.tenant_id,
        #     self.find_tenant_domain(event_obj.tenant_id),
        #     event_obj.domain_name,
        #     event_obj.application_context
        # )

    def on_subscription_domain_removed(self, msg):
        self.log.debug("Subscription domain removed event received")
        event_obj = SubscriptionDomainRemovedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onSubscriptionDomainRemovedEvent(event_obj)
        except:
            self.log.exception("Error processing subscription domains removed event")
        # extensionutils.execute_subscription_domain_removed_extension(
        #     event_obj.tenant_id,
        #     self.find_tenant_domain(event_obj.tenant_id),
        #     event_obj.domain_name
        # )

    def on_complete_tenant(self, msg):
        if not self.__tenant_context_initialized:
            self.log.debug("Complete tenant event received")
            event_obj = CompleteTenantEvent.create_from_json(msg.payload)
            TenantContext.update(event_obj.tenants)

            try:
                self.extension_handler.onCompleteTenantEvent(event_obj)
                self.__tenant_context_initialized = True
            except:
                self.log.exception("Error processing complete tenant event")
        else:
            self.log.info("Complete tenant event updating task disabled")

    def on_tenant_subscribed(self, msg):
        self.log.debug("Tenant subscribed event received")
        event_obj = TenantSubscribedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onTenantSubscribedEvent(event_obj)
        except:
            self.log.exception("Error processing tenant subscribed event")

    def on_tenant_unsubscribed(self, msg):
        self.log.debug("Tenant unSubscribed event received")
        event_obj = TenantUnsubscribedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onTenantUnSubscribedEvent(event_obj)
        except:
            self.log.exception("Error processing tenant unSubscribed event")


def main():
    cartridge_agent = CartridgeAgent()
    cartridge_agent.start()


if __name__ == "__main__":
    main()
