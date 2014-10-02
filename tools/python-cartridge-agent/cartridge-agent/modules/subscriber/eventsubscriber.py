import logging
import threading
import paho.mqtt.client as mqtt

from .. util import cartridgeagentconstants
from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from .. util.log import LogFactory


class EventSubscriber(threading.Thread):
    """
    Provides functionality to subscribe to a given topic on the stratos MB and
    register event handlers for various events.
    """

    def __init__(self, topic):
        threading.Thread.__init__(self)

        #{"ArtifactUpdateEvent" : onArtifactUpdateEvent()}
        self.__event_handlers = {}

        self.log = LogFactory().get_log(__name__)

        self.__mb_client = None

        self.__topic = topic

        self.__subscribed = False

    def run(self):
        self.__mb_client = mqtt.Client()
        self.__mb_client.on_connect = self.on_connect
        self.__mb_client.on_message = self.on_message

        mb_ip = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.MB_IP)
        mb_port = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.MB_PORT)

        self.log.debug("Connecting to the message broker with address %r:%r" % (mb_ip, mb_port))
        self.__mb_client.connect(mb_ip, mb_port, 60)
        self.__subscribed = True
        self.__mb_client.loop_forever()

    def register_handler(self, event, handler):
        """
        Adds an event handler function mapped to the provided event.
        :param str event: Name of the event to attach the provided handler
        :param (str)->void handler: The handler function
        :return: void
        :rtype: void
        """
        self.__event_handlers[event] = handler
        self.log.debug("Registered handler for event %r" % event)

    def on_connect(self, client, userdata, flags, rc):
        self.log.debug("Connected to message broker.")
        self.__mb_client.subscribe(self.__topic)
        self.log.debug("Subscribed to %r" % self.__topic)

    def on_message(self, client, userdata, msg):
        self.log.debug("Message received: %r:\n%r" % (msg.topic, msg.payload))

        event = msg.topic.rpartition('/')[2]

        if event in self.__event_handlers:
            handler = self.__event_handlers[event]

            try:
                self.log.debug("Executing handler for event %r" % event)
                handler(msg)
            except:
                self.log.exception("Error processing %r event" % event)
        else:
            self.log.debug("Event handler not found for event : %r" % event)

    def is_subscribed(self):
        """
        Checks if this event subscriber is successfully subscribed to the provided topic
        :return: True if subscribed, False if otherwise
        :rtype: bool
        """
        return self.__subscribed
