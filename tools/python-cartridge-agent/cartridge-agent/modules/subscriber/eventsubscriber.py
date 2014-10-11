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

import threading
import paho.mqtt.client as mqtt


class EventSubscriber(threading.Thread):
    """
    Provides functionality to subscribe to a given topic on the stratos MB and
    register event handlers for various events.
    """

    def __init__(self, topic, ip, port):
        threading.Thread.__init__(self)

        #{"ArtifactUpdateEvent" : onArtifactUpdateEvent()}
        self.__event_handlers = {}

        self.log = LogFactory().get_log(__name__)

        self.__mb_client = None

        self.__topic = topic

        self.__subscribed = False

        self.__ip = ip
        self.__port = port

    def run(self):
        self.__mb_client = mqtt.Client()
        self.__mb_client.on_connect = self.on_connect
        self.__mb_client.on_message = self.on_message

        self.log.debug("Connecting to the message broker with address %r:%r" % (self.__ip, self.__port))
        self.__mb_client.connect(self.__ip, self.__port, 60)
        self.__subscribed = True
        self.__mb_client.loop_forever()

    def register_handler(self, event, handler):
        """
        Adds an event handler function mapped to the provided event.
        :param str event: Name of the event to attach the provided handler
        :param handler: The handler function
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


from .. util.log import LogFactory