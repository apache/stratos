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

import sys

sys.path.append("gen")

from gen.ThriftSecureEventTransmissionService import ThriftSecureEventTransmissionService
from gen.Data.ttypes import ThriftEventBundle

from thrift.transport import TSSLSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol


# Define publisher class
class Publisher:
    client = None

    def __init__(self, ip, port):
        # Make SSL socket
        self.socket = TSSLSocket.TSSLSocket(ip, port, False)
        # Buffering is critical. Raw sockets are very slow
        self.transport = TTransport.TBufferedTransport(self.socket)
        # Wrap in a protocol
        self.protocol = TBinaryProtocol.TBinaryProtocol(self.transport)
        self.sessionId = None
        self.streamId = None

        # self.event_num = 0

    def connect(self, username, password):
        # Create a client to use the protocol encoder
        Publisher.client = ThriftSecureEventTransmissionService.Client(self.protocol)

        # Make connection
        self.socket.open()
        self.transport.open()
        self.sessionId = Publisher.client.connect(username, password)

    def defineStream(self, streamDef):
        # Create Stream Definition
        self.streamId = Publisher.client.defineStream(self.sessionId, streamDef)

    def publish(self, event):
        # Build thrift event bundle
        event.setSessionId(self.sessionId)
        event.setEventNum(1)
        # self.event_num += 1

        # Publish
        Publisher.client.publish(event.getEventBundle())

    def disconnect(self):
        # Disconnect
        Publisher.client.disconnect(self.sessionId)
        self.transport.close()
        self.socket.close()


class EventBundle:

    def __init__(self):
        self.__sessionId = ""
        self.__eventNum = 0
        self.__intAttributeList = []
        self.__longAttributeList = []
        self.__doubleAttributeList = []
        self.__boolAttributeList = []
        self.__stringAttributeList = []
        self.__arbitraryDataMapMap = None

    def setSessionId(self, sessionId):
        self.__sessionId = sessionId

    def setEventNum(self, num):
        self.__eventNum = num

    def addIntAttribute(self, attr):
        self.__intAttributeList.append(attr)

    def addLongAttribute(self, attr):
        self.__longAttributeList.append(attr)

    def addDoubleAttribute(self, attr):
        self.__doubleAttributeList.append(attr)

    def addBoolAttribute(self, attr):
        self.__boolAttributeList.append(attr)

    def addStringAttribute(self, attr):
        self.__stringAttributeList.append(attr)

    def getEventBundle(self):
        return ThriftEventBundle(self.__sessionId, self.__eventNum, self.__intAttributeList,
                                 self.__longAttributeList, self.__doubleAttributeList,
                                 self.__boolAttributeList, self.__stringAttributeList,
                                 self.__arbitraryDataMapMap)
