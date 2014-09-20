import logging
import threading
import paho.mqtt.client as mqtt

from ..util import cartridgeagentconstants,extensionutils
from ..config import cartridgeagentconfiguration
from ..extensions import defaultextensionhandler
from ..event.tenant import *


class TenantEventSubscriber(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)

        self.cartridge_agent_config = cartridgeagentconfiguration.CartridgeAgentConfiguration()
        #{"ArtifactUpdateEvent" : onArtifactUpdateEvent()}
        self.__event_handlers = {}
        self.register_handler("SubscriptionDomainAddedEvent", self.on_subscription_domain_added)
        self.register_handler("SubscriptionDomainsRemovedEventListener", self.on_subscription_domain_removed)

        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)
        self.__mb_client = None

        self.extension_handler = defaultextensionhandler.DefaultExtensionHandler()

        self.__subscribed = False

    def run(self):
        self.__mb_client = mqtt.Client()
        self.__mb_client.on_connect = self.on_connect
        self.__mb_client.on_message = self.on_message

        mb_ip = self.cartridge_agent_config.read_property(cartridgeagentconstants.MB_IP)
        mb_port = self.cartridge_agent_config.read_property(cartridgeagentconstants.MB_PORT)

        self.log.debug("Connecting to the message broker with address %r:%r" % (mb_ip, mb_port))
        self.__mb_client.connect(mb_ip, mb_port, 60)
        self.__subscribed = True
        self.__mb_client.loop_forever()

    def register_handler(self, event, handler):
        self.__event_handlers[event] = handler
        self.log.debug("Registered handler for event %r" % event)

    def on_connect(self, client, userdata, flags, rc):
        self.log.debug("Connected to message broker.")
        self.__mb_client.subscribe(cartridgeagentconstants.TENANT_TOPIC)
        self.log.debug("Subscribed to %r" % cartridgeagentconstants.TENANT_TOPIC)

    def on_message(self, client, userdata, msg):
        self.log.debug("Message received: %r:\n%r" % (msg.topic, msg.payload))

        event = msg.topic.rpartition('/')[2]
        handler = self.__event_handlers[event]

        try:
            self.log.debug("Executing handler for event %r" % event)
            handler(msg)
        except:
            self.log.exception("Error processing %r event" % event)

    def on_subscription_domain_added(self, msg):
        event_obj = subscriptiondomainaddedevent.create_from_json(msg.payload)
        extensionutils.execute_subscription_domain_added_extension(
            event_obj.tenant_id,
            self.find_tenant_domain(event_obj.tenant_id),
            event_obj.domain_name,
            event_obj.application_context
        )

    def on_subscription_domain_removed(self, msg):
        event_obj = subscriptiondomainremovedevent.create_from_json(msg.payload)
        extensionutils.execute_subscription_domain_removed_extension(
            event_obj.tenant_id,
            self.find_tenant_domain(event_obj.tenant_id),
            event_obj.domain_name
        )

    def find_tenant_domain(self, tenant_id):
        #TODO: call to REST Api and get tenant information
        raise NotImplementedError