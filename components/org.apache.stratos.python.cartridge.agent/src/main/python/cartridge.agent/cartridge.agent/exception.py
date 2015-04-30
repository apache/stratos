# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


class CartridgeAgentException(Exception):
    """
    Exception super class to be used by specific exceptions thrown in the cartridge agent
    """

    def __init__(self, message):
        super(CartridgeAgentException, self).__init__(message)
        self.__message = message

    def get_message(self):
        """
        The message provided when the exception is raised
        :return: message
        :rtype: str
        """
        return self.__message


class DataPublisherException(CartridgeAgentException):
    """
    Exception to be used during log publishing operations
    """

    def __init__(self, message):
        super(DataPublisherException, self).__init__(message)


class PluginExecutionException(CartridgeAgentException):
    """
    Exception raised when a runtime error is met while executing an plugin script
    """

    def __init__(self, message):
        super(PluginExecutionException, self).__init__(message)


class GitRepositorySynchronizationException(CartridgeAgentException):
    """
    Exception raised during a git repository related task
    """

    def __init__(self, message):
        super(GitRepositorySynchronizationException, self).__init__(message)


class ParameterNotFoundException(CartridgeAgentException):
    """
    Exception raised when a property is not present in the configuration or the payload
    of the cartridge agent
    """

    def __init__(self, message):
        super(ParameterNotFoundException, self).__init__(message)


class ThriftReceiverOfflineException(CartridgeAgentException):
    """
    Exception raised when the connection to the Thrift receiver is dropped when publishing events
    """

    def __init__(self, message):
        super(ThriftReceiverOfflineException, self).__init__(message)


class CEPPublisherException(CartridgeAgentException):
    """
    Exception to be used during CEP publishing operations
    """

    def __init__(self, message):
        super(CEPPublisherException, self).__init__(message)
