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

from thrift.publisher import *
from thrift.gen.Exception.ttypes import ThriftSessionExpiredException
from ..util.log import *
from exception import ThriftReceiverOfflineException

import time


class StreamDefinition:
    """
    Represents a BAM/CEP stream definition
    """

    STRING = 'STRING'
    DOUBLE = 'DOUBLE'
    INT = 'INT'
    LONG = 'LONG'
    BOOL = 'BOOL'

    def __init__(self):
        self.name = None
        """:type : str"""
        self.version = None
        """:type : str"""
        self.nickname = None
        """:type : str"""
        self.description = None
        """:type : str"""
        self.meta_data = []
        """:type : list[str]"""
        self.correlation_data = []
        """:type : list[str]"""
        self.payload_data = []
        """:type : list[str]"""
        self.stream_id = None
        """ :type : str """

    def add_metadata_attribute(self, attr_name, attr_type):
        self.meta_data.append({"name": attr_name, "type": attr_type})

    def add_payloaddata_attribute(self, attr_name, attr_type):
        self.payload_data.append({"name": attr_name, "type": attr_type})

    def add_correlationdata_attribute(self, attr_name, attr_type):
        self.correlation_data.append({"name": attr_name, "type": attr_type})

    def __str__(self):
        """
        To string override
        """

        json_str = "{"
        json_str += "\"name\":\"" + self.name + "\","
        json_str += "\"version\":\"" + self.version + "\","
        json_str += "\"nickName\":\"" + self.nickname + "\","
        json_str += "\"description\":\"" + self.description + "\","

        # add metadata attributes if exists
        if len(self.meta_data) > 0:
            json_str += "\"metaData\":["
            for metadatum in self.meta_data:
                json_str += "{\"name\":\"" + metadatum["name"] + "\", \"type\": \"" + metadatum["type"] + "\"},"

            json_str = json_str[:-1] + "],"

        # add correlationdata attributes if exists
        if len(self.correlation_data) > 0:
            json_str += "\"correlationData\":["
            for coredatum in self.correlation_data:
                json_str += "{\"name\":\"" + coredatum["name"] + "\", \"type\": \"" + coredatum["type"] + "\"},"

            json_str = json_str[:-1] + "],"

        # add payloaddata attributes if exists
        if len(self.payload_data) > 0:
            json_str += "\"payloadData\":["
            for payloaddatum in self.payload_data:
                json_str += "{\"name\":\"" + payloaddatum["name"] + "\", \"type\": \"" + payloaddatum["type"] + "\"},"

            json_str = json_str[:-1] + "],"

        json_str = json_str[:-1] + "}"

        return json_str


class ThriftEvent:
    """
    Represents an event to be published to a BAM/CEP monitoring server
    """
    def __init__(self):
        self.metaData = []
        """:type : list[str]"""
        self.correlationData = []
        """:type : list[str]"""
        self.payloadData = []
        """:type : list[T]"""


class ThriftPublisher:
    """
    Handles publishing events to BAM/CEP through thrift using the provided address and credentials
    """
    log = LogFactory().get_log(__name__)

    def __init__(self, ip, port, username, password, stream_definition):
        """
        Initializes a ThriftPublisher object.

        At initialization a ThriftPublisher connects and defines a stream definition. A connection
        should be disconnected after all the publishing has been done.

        :param str ip: IP address of the monitoring server
        :param str port: Port of the monitoring server
        :param str username: Username
        :param str password: Password
        :param StreamDefinition stream_definition: StreamDefinition object for this particular connection
        :return: ThriftPublisher object
        :rtype: ThriftPublisher
        """
        try:
            port_number = int(port)
        except ValueError:
            raise RuntimeError("Port number for Thrift Publisher is invalid: %r" % port)

        self.__publisher = Publisher(ip, port_number)
        self.__publisher.connect(username, password)
        self.__publisher.defineStream(str(stream_definition))

        self.stream_definition = stream_definition
        self.stream_id = self.__publisher.streamId
        self.ip = ip
        self.port = port
        self.username = username
        self.password = password

    def publish(self, event):
        """
        Publishes the given event by creating the event bundle from the log event

        :param ThriftEvent event: The log event to be published
        :return: void
        """

        event_bundle = self.create_event_bundle(event)

        try:
            self.__publisher.publish(event_bundle)
            self.log.debug("Published event to thrift stream [%r]" % self.stream_id)
        except ThriftSessionExpiredException:
            self.log.debug("ThriftSession expired. Reconnecting")
            self.__publisher.connect(self.username, self.password)
            self.log.debug("connected! stream ID: %r" % self.stream_id)

            self.publish(event)
        except Exception as ex:
            raise ThriftReceiverOfflineException(ex)

    def create_event_bundle(self, event):
        """
        Creates an EventBundle object to be published to the Thrift stream

        :param ThriftEvent event:
        :return: EventBundle event bundle object
        """

        event_bundle = EventBundle()
        event_bundle.addStringAttribute(self.stream_id)
        event_bundle.addLongAttribute(time.time() * 1000)
        ThriftPublisher.assign_attributes(event.metaData, event_bundle)
        ThriftPublisher.assign_attributes(event.correlationData, event_bundle)
        ThriftPublisher.assign_attributes(event.payloadData, event_bundle)

        return event_bundle

    def disconnect(self):
        """
        Disconnect the thrift publisher
        :return: void
        """
        self.__publisher.disconnect()

    @staticmethod
    def assign_attributes(attributes, event_bundler):
        """
        Adds the given attributes to the given event bundler according to type of each attribute
        :param list attributes: attributes to be assigned
        :param EventBundle event_bundler: Event bundle to assign attributes to
        :return: void
        """

        # __intAttributeList = []
        # __longAttributeList = []
        # __doubleAttributeList = []
        # __boolAttributeList = []
        # __stringAttributeList = []

        if attributes is not None and len(attributes) > 0:
            for attrib in attributes:
                if isinstance(attrib, int):
                    event_bundler.addIntAttribute(attrib)
                elif isinstance(attrib, long):
                    event_bundler.addLongAttribute(attrib)
                elif isinstance(attrib, float):
                    event_bundler.addDoubleAttribute(attrib)
                elif isinstance(attrib, bool):
                    event_bundler.addBoolAttribute(attrib)
                elif isinstance(attrib, str):
                    event_bundler.addStringAttribute(attrib)
                else:
                    ThriftPublisher.log.error("Undefined attribute type: %r" % attrib)