import logging
import threading
import paho.mqtt.client as mqtt

from ..util import cartridgeagentconstants
from ..config import cartridgeagentconfiguration
from ..extensions import defaultextensionhandler
from ..event.instance.notifier import *


class InstanceEventSubscriber(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)

        self.cartridge_agent_config = cartridgeagentconfiguration.CartridgeAgentConfiguration()
        #{"ArtifactUpdateEvent" : onArtifactUpdateEvent()}
        self.__event_handlers = {}
        self.register_handler("ArtifactUpdatedEvent", self.on_artifact_updated)
        self.register_handler("InstanceCleanupMemberEvent", self.on_instance_cleanup_member)
        self.register_handler("InstanceCleanupClusterEvent", self.on_instance_cleanup_cluster)

        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)
        self.__mb_client = None

        self.extension_handler = defaultextensionhandler.DefaultExtensionHandler()

    def run(self):
        self.__mb_client = mqtt.Client()
        self.__mb_client.on_connect = self.on_connect
        self.__mb_client.on_message = self.on_message

        mb_ip = self.cartridge_agent_config.read_property(cartridgeagentconstants.MB_IP)
        mb_port = self.cartridge_agent_config.read_property(cartridgeagentconstants.MB_PORT)

        self.log.debug("Connecting to the message broker with address %r:%r" % (mb_ip, mb_port))
        self.__mb_client.connect(mb_ip, mb_port, 60)
        self.__mb_client.loop_forever()

    def register_handler(self, event, handler):
        self.__event_handlers[event] = handler
        self.log.debug("Registered handler for event %r" % event)

    def on_connect(self, client, userdata, flags, rc):
        self.log.debug("Connected to message broker.")
        self.__mb_client.subscribe(cartridgeagentconstants.INSTANCE_NOTIFIER_TOPIC)
        self.log.debug("Subscribed to %r" % cartridgeagentconstants.INSTANCE_NOTIFIER_TOPIC)

    def on_message(self, client, userdata, msg):
        self.log.debug("Message received: %r:\n%r" % (msg.topic, msg.payload))

        event = msg.topic.rpartition('/')[2]
        handler = self.__event_handlers[event]

        try:
            self.log.debug("Executing handler for event %r" % event)
            handler(msg)
        except Exception:
            self.log.exception("Error processing %r event" % event)

    def on_artifact_updated(self, msg):
        event_obj = artifactupdatedevent.create_from_json(msg.payload)
        self.extension_handler.onArtifactUpdatedEvent(event_obj)

    def on_instance_cleanup_member(self, msg):
        member_in_payload = self.cartridge_agent_config.get_member_id()
        event_obj = instancecleanupmemberevent.InstanceCleanupMemberEvent.create_from_json(msg.payload)
        member_in_event = event_obj.member_id
        if member_in_payload == member_in_event:
            self.extension_handler.onInstanceCleanupMemberEvent(event_obj)

    def on_instance_cleanup_cluster(self, msg):
        event_obj = instancecleanupclusterevent.create_from_json(msg.payload)
        cluster_in_payload = self.cartridge_agent_config.get_cluster_id()
        cluster_in_event = event_obj.cluster_id

        if cluster_in_event == cluster_in_payload:
            self.extension_handler.onInstanceCleanupClusterEvent(event_obj)