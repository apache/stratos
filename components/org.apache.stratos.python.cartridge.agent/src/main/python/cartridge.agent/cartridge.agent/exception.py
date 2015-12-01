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

    def __init__(self, *args, **kwargs):
        Exception.__init__(self, *args, **kwargs)


class DataPublisherException(CartridgeAgentException):
    """
    Exception to be used during log publishing operations
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)


class PluginExecutionException(CartridgeAgentException):
    """
    Exception raised when a runtime error is met while executing an plugin script
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)


class GitRepositorySynchronizationException(CartridgeAgentException):
    """
    Exception raised during a git repository related task
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)


class ParameterNotFoundException(CartridgeAgentException):
    """
    Exception raised when a property is not present in the configuration or the payload
    of the cartridge agent
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)


class ThriftReceiverOfflineException(CartridgeAgentException):
    """
    Exception raised when the connection to the Thrift receiver is dropped when publishing events
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)


class CEPPublisherException(CartridgeAgentException):
    """
    Exception to be used during CEP publishing operations
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)


class InvalidConfigValueException(CartridgeAgentException):
    """
    Exception to be used when validating agent configuration
    """

    def __init__(self, *args, **kwargs):
        CartridgeAgentException.__init__(self, *args, **kwargs)