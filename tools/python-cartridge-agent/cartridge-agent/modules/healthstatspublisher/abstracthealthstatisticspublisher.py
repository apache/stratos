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

class AbstractHealthStatisticsReader:
    """
    Abstract class to implement to create a custom health stat reader
    """

    def stat_cartridge_health(self):
        """
        Abstract method that when implemented reads the memory usage and the load average
        of the instance running the agent and returns a CartridgeHealthStatistics object
        with the information

        :return: CartridgeHealthStatistics object with memory usage and load average values
        :rtype : CartridgeHealthStatistics
        """
        raise NotImplementedError


class CartridgeHealthStatistics:
    """
    Holds the memory usage and load average reading
    """

    def __init__(self):
        self.memory_usage = None
        """:type : float"""
        self.load_avg = None
        """:type : float"""


class CEPPublisherException(Exception):
    """
    Exception to be used during CEP publishing operations
    """

    def __init__(self, msg):
        super(self,  msg)
        self.message = msg

    def get_message(self):
        """
        The message provided when the exception is raised
        :return: message
        :rtype: str
        """
        return self.message
