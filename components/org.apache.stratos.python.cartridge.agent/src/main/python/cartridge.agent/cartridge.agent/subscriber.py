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

from Queue import Queue

import threading
import paho.mqtt.client as mqtt

from config import Config

from modules.util.log import LogFactory
from modules.util.asyncscheduledtask import *
from modules.util.cartridgeagentutils import IncrementalCeilingListIterator


class EventSubscriber(threading.Thread):
    """
    Provides functionality to subscribe to a given topic on the Stratos MB and
    register event handlers for various events.
    """

    log = LogFactory().get_log(__name__)

    def __init__(self, topic, urls, username, password):
        threading.Thread.__init__(self)
        self.setDaemon(True)

        self.__event_queue = Queue(maxsize=0)
        self.__event_executor = EventExecutor(self.__event_queue)

        self.__mb_client = None
        self.__topic = topic
        self.__subscribed = False
        self.__urls = urls
        self.__username = username
        self.__password = password
        self.setName("MBSubscriberThreadForTopic%s" % topic)
        EventSubscriber.log.debug("Created a subscriber thread for %s" % topic)

    def run(self):
        EventSubscriber.log.debug("Starting the subscriber thread for %s" % self.__topic)
        #  Start the event executor thread
        self.__event_executor.start()

        """
        The following loop will iterate forever.

        When a successful connection is made, the failover() method returns. Then the
        blocking method loop_forever() will be called on the connected mqtt client. This will only
        return if disconnect() is called on the same client. If the connected message broker goes
        down, the callback method on_disconnect() will call disconnect() on the connected client and the
        loop_forever() method will return. The parent loop will be called again and this repeats
        every time the message brokers are disconnected.

        This behavior guarantees that the subscriber is always subscribed to an available message
        broker.

        """
        while True:
            self.__mb_client = mqtt.Client()
            self.__mb_client.on_connect = self.on_connect
            self.__mb_client.on_message = self.on_message
            self.__mb_client.on_disconnect = self.on_disconnect
            if self.__username is not None:
                EventSubscriber.log.info("Message broker credentials are provided.")
                self.__mb_client.username_pw_set(self.__username, self.__password)

            # Select an online message broker and connect
            self.__mb_client, connected_mb_ip, connected_mb_port = \
                EventSubscriber.failover(self.__urls, self.__mb_client)

            # update connected MB details in the config for the plugins to use
            Config.mb_ip = connected_mb_ip
            Config.mb_port = connected_mb_port

            EventSubscriber.log.info(
                "Connected to the message broker with address %s:%s" % (connected_mb_ip, connected_mb_port))

            self.__subscribed = True

            # Start blocking loop method
            self.__mb_client.loop_forever()

            # Disconnected when the on_disconnect calls disconnect() on the client
            self.__subscribed = False
            EventSubscriber.log.debug("Disconnected from the message broker %s:%s. Reconnecting..."
                                      % (connected_mb_ip, connected_mb_port))

    def register_handler(self, event, handler):
        """
        Adds an event handler function mapped to the provided event.
        :param str event: Name of the event to attach the provided handler
        :param handler: The handler function
        :return: void
        :rtype: void
        """
        self.__event_executor.register_event_handler(event, handler)
        EventSubscriber.log.debug("Registered handler for event %r" % event)

    def on_connect(self, client, userdata, flags, rc):
        if rc != 0:
            EventSubscriber.log.debug("Connection to the message broker didn't succeed. Disconnecting client.")
            client.disconnect()
            return

        EventSubscriber.log.debug("Connected to message broker %s:%s successfully." % (client._host, client._port))
        self.__mb_client.subscribe(self.__topic)
        EventSubscriber.log.debug("Subscribed to %r" % self.__topic)

    def on_message(self, client, userdata, msg):
        EventSubscriber.log.debug("Message received: %s:\n%s" % (msg.topic, msg.payload))
        self.__event_queue.put(msg)

    def on_disconnect(self, client, userdata, rc):
        EventSubscriber.log.debug("Message broker client disconnected. %s:%s" % (client._host, client._port))
        if rc != 0:
            client.disconnect()

    def is_subscribed(self):
        """
        Checks if this event subscriber is successfully subscribed to the provided topic
        :return: True if subscribed, False if otherwise
        :rtype: bool
        """
        return self.__subscribed

    @staticmethod
    def failover(mb_urls, mb_client):
        """
        Iterate through the list of message brokers provided and connect to the first available server. This will not
        return until a message broker connection is established.

        :param mb_urls: the list of message broker URLS of format [host:port, host:port]
        :param mb_client: the initialized message broker client object
        :return: a tuple of the connected message broker client, connected message broker IP address and connected
        message broker port

        """
        # Connection retry interval incrementer
        message_broker_retry_timer = IncrementalCeilingListIterator(
                                                            [2, 2, 5, 5, 10, 10, 20, 20, 30, 30, 40, 40, 50, 50, 60],
                                                            False)

        # Cycling through the provided mb urls until forever
        while True:
            retry_interval = message_broker_retry_timer.get_next_retry_interval()

            for mb_url in mb_urls:
                mb_ip, mb_port = mb_url.split(":")
                EventSubscriber.log.debug(
                    "Trying to connect to the message broker with address %r:%r" % (mb_ip, mb_port))
                try:
                    mb_client.connect(mb_ip, mb_port, 60)
                    return mb_client, mb_ip, mb_port
                except:
                    # The message broker didn't respond well
                    EventSubscriber.log.info("Could not connect to the message broker at %s:%s." % (mb_ip, mb_port))

            EventSubscriber.log.error(
                "Could not connect to any of the message brokers provided. Retrying in %s seconds." % retry_interval)

            time.sleep(retry_interval)


class EventExecutor(threading.Thread):
    """
    Polls the event queue and executes event handlers for each event
    """
    def __init__(self, event_queue):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.__event_queue = event_queue
        self.__event_handlers = {}
        EventSubscriber.log = LogFactory().get_log(__name__)
        self.setName("MBEventExecutorThread")
        EventSubscriber.log.debug("Created an EventExecutor")

    def run(self):
        EventSubscriber.log.debug("Starting an EventExecutor")
        while True:
            event_msg = self.__event_queue.get()
            event = event_msg.topic.rpartition('/')[2]
            if event in self.__event_handlers:
                handler = self.__event_handlers[event]
                try:
                    EventSubscriber.log.debug("Executing handler for event %r" % event)
                    handler(event_msg)
                except Exception as err:
                    EventSubscriber.log.exception("Error processing %r event: %s" % (event, err))
            else:
                EventSubscriber.log.debug("Event handler not found for event : %r" % event)

    def register_event_handler(self, event, handler):
        self.__event_handlers[event] = handler

    def terminate(self):
        self.terminate()
