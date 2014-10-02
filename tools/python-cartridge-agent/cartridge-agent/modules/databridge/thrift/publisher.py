import time
import sys

sys.path.append("gen")

from gen.ThriftSecureEventTransmissionService import ThriftSecureEventTransmissionService
from gen.ThriftSecureEventTransmissionService.ttypes import *

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
        #event = EventBundle()
        event.setSessionId(self.sessionId)
        event.setEventNum(1)
        event.addLongAttribute(time.time() * 1000)
        event.addStringAttribute(self.streamId)
        #event.addStringAttribute(msg)
        # Publish
        Publisher.client.publish(event.getEventBundle())

    def disconnect(self):
        # Disconnect
        Publisher.client.disconnect(self.sessionId)
        self.transport.close()
        self.socket.close()


class EventBundle:
    __sessionId = ""
    __eventNum = 0
    __intAttributeList = []
    __longAttributeList = []
    __doubleAttributeList = []
    __boolAttributeList = []
    __stringAttributeList = []
    __arbitraryDataMapMap = None

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
        return Data.ttypes.ThriftEventBundle(self.__sessionId, self.__eventNum, self.__intAttributeList,
                                             self.__longAttributeList, self.__doubleAttributeList,
                                             self.__boolAttributeList, self.__stringAttributeList,
                                             self.__arbitraryDataMapMap)
