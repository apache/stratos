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

from threading import Thread
import time
import psutil
import os
import multiprocessing

from abstracthealthstatisticspublisher import *
from ..databridge.agent import *
from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ..util import cartridgeagentutils, cartridgeagentconstants


class HealthStatisticsPublisherManager(Thread):
    """
    Read from an implementation of AbstractHealthStatisticsPublisher the value for memory usage and
    load average and publishes them as ThriftEvents to a CEP server
    """
    STREAM_NAME = "cartridge_agent_health_stats"
    STREAM_VERSION = "1.0.0"
    STREAM_NICKNAME = "agent health stats"
    STREAM_DESCRIPTION = "agent health stats"

    def __init__(self, publish_interval):
        """
        Initializes a new HealthStatistsPublisherManager with a given number of seconds as the interval
        :param int publish_interval: Number of seconds as the interval
        :return: void
        """
        Thread.__init__(self)

        self.log = LogFactory().get_log(__name__)

        self.publish_interval = publish_interval
        """:type : int"""

        self.terminated = False

        self.publisher = HealthStatisticsPublisher()
        """:type : HealthStatisticsPublisher"""
        # TODO: load plugins for the reader
        self.stats_reader = DefaultHealthStatisticsReader()
        """:type : AbstractHealthStatisticsReader"""

    def run(self):
        while not self.terminated:
            time.sleep(self.publish_interval)

            cartridge_stats = self.stats_reader.stat_cartridge_health()
            self.log.debug("Publishing memory consumption: %r" % cartridge_stats.memory_usage)
            self.publisher.publish_memory_usage(cartridge_stats.memory_usage)

            self.log.debug("Publishing load average: %r" % cartridge_stats.load_avg)
            self.publisher.publish_load_average(cartridge_stats.load_avg)

        self.publisher.publisher.disconnect()


class HealthStatisticsPublisher:
    """
    Publishes memory usage and load average to thrift server
    """
    log = LogFactory().get_log(__name__)

    def __init__(self):

        self.ports = []
        self.ports.append(CEPPublisherConfiguration.get_instance().server_port)

        self.cartridge_agent_config = CartridgeAgentConfiguration()

        cartridgeagentutils.wait_until_ports_active(
            CEPPublisherConfiguration.get_instance().server_ip,
            self.ports,
            int(self.cartridge_agent_config.read_property("port.check.timeout", critical=False)))
        cep_active = cartridgeagentutils.check_ports_active(CEPPublisherConfiguration.get_instance().server_ip, self.ports)
        if not cep_active:
            raise CEPPublisherException("CEP server not active. Health statistics publishing aborted.")

        self.stream_definition = HealthStatisticsPublisher.create_stream_definition()
        HealthStatisticsPublisher.log.debug("Stream definition created: %r" % str(self.stream_definition))
        self.publisher = ThriftPublisher(
            CEPPublisherConfiguration.get_instance().server_ip,
            CEPPublisherConfiguration.get_instance().server_port,
            CEPPublisherConfiguration.get_instance().admin_username,
            CEPPublisherConfiguration.get_instance().admin_password,
            self.stream_definition)

        HealthStatisticsPublisher.log.debug("HealthStatisticsPublisher initialized")

    @staticmethod
    def create_stream_definition():
        """
        Create a StreamDefinition for publishing to CEP
        """
        stream_def = StreamDefinition()
        stream_def.name = HealthStatisticsPublisherManager.STREAM_NAME
        stream_def.version = HealthStatisticsPublisherManager.STREAM_VERSION
        stream_def.nickname = HealthStatisticsPublisherManager.STREAM_NICKNAME
        stream_def.description = HealthStatisticsPublisherManager.STREAM_DESCRIPTION

        stream_def.add_payloaddata_attribute("cluster_id", StreamDefinition.STRING)
        stream_def.add_payloaddata_attribute("network_partition_id", StreamDefinition.STRING)
        stream_def.add_payloaddata_attribute("member_id", StreamDefinition.STRING)
        stream_def.add_payloaddata_attribute("partition_id", StreamDefinition.STRING)
        stream_def.add_payloaddata_attribute("health_description", StreamDefinition.STRING)
        stream_def.add_payloaddata_attribute("value", StreamDefinition.DOUBLE)

        return stream_def

    def publish_memory_usage(self, memory_usage):
        """
        Publishes the given memory usage value to the thrift server as a ThriftEvent
        :param float memory_usage: memory usage
        """

        event = ThriftEvent()
        event.payloadData.append(self.cartridge_agent_config.cluster_id)
        event.payloadData.append(self.cartridge_agent_config.network_partition_id)
        event.payloadData.append(self.cartridge_agent_config.member_id)
        event.payloadData.append(self.cartridge_agent_config.partition_id)
        event.payloadData.append(cartridgeagentconstants.MEMORY_CONSUMPTION)
        event.payloadData.append(memory_usage)

        HealthStatisticsPublisher.log.debug("Publishing cep event: [stream] %r [version] %r" % (self.stream_definition.name, self.stream_definition.version))
        self.publisher.publish(event)

    def publish_load_average(self, load_avg):
        """
        Publishes the given load average value to the thrift server as a ThriftEvent
        :param float load_avg: load average value
        """

        event = ThriftEvent()
        event.payloadData.append(self.cartridge_agent_config.cluster_id)
        event.payloadData.append(self.cartridge_agent_config.network_partition_id)
        event.payloadData.append(self.cartridge_agent_config.member_id)
        event.payloadData.append(self.cartridge_agent_config.partition_id)
        event.payloadData.append(cartridgeagentconstants.LOAD_AVERAGE)
        event.payloadData.append(load_avg)

        HealthStatisticsPublisher.log.debug("Publishing cep event: [stream] %r [version] %r" % (self.stream_definition.name, self.stream_definition.version))
        self.publisher.publish(event)


class DefaultHealthStatisticsReader(AbstractHealthStatisticsReader):
    """
    Default implementation of the AbstractHealthStatisticsReader
    """

    def __init__(self):
        self.log = LogFactory().get_log(__name__)

    def stat_cartridge_health(self):
        cartridge_stats = CartridgeHealthStatistics()
        cartridge_stats.memory_usage = DefaultHealthStatisticsReader.__read_mem_usage()
        cartridge_stats.load_avg = DefaultHealthStatisticsReader.__read_load_avg()

        self.log.debug("Memory read: %r, CPU read: %r" % (cartridge_stats.memory_usage, cartridge_stats.load_avg))
        return cartridge_stats

    @staticmethod
    def __read_mem_usage():
        return psutil.virtual_memory().percent

    @staticmethod
    def __read_load_avg():
        (one, five, fifteen) = os.getloadavg()
        cores = multiprocessing.cpu_count()

        return (one/cores) * 100


class CEPPublisherConfiguration:
    """
    TODO: Extract common functionality
    """

    __instance = None
    log = LogFactory().get_log(__name__)

    @staticmethod
    def get_instance():
        """
        Singleton instance retriever
        :return: Instance
        :rtype : CEPPublisherConfiguration
        """
        if CEPPublisherConfiguration.__instance is None:
            CEPPublisherConfiguration.__instance = CEPPublisherConfiguration()

        return CEPPublisherConfiguration.__instance

    def __init__(self):
        self.enabled = False
        self.server_ip = None
        self.server_port = None
        self.admin_username = None
        self.admin_password = None
        self.cartridge_agent_config = CartridgeAgentConfiguration()

        self.read_config()

    def read_config(self):
        self.enabled = True if self.cartridge_agent_config.read_property(
           cartridgeagentconstants.CEP_PUBLISHER_ENABLED, False).strip().lower() == "true" else False
        if not self.enabled:
            CEPPublisherConfiguration.log.info("CEP Publisher disabled")
            return

        CEPPublisherConfiguration.log.info("CEP Publisher enabled")

        self.server_ip = self.cartridge_agent_config.read_property(
            cartridgeagentconstants.CEP_RECEIVER_IP, False)
        if self.server_ip is None or self.server_ip.strip() == "":
            raise RuntimeError("System property not found: " + cartridgeagentconstants.CEP_RECEIVER_IP)

        self.server_port = self.cartridge_agent_config.read_property(
            cartridgeagentconstants.CEP_RECEIVER_PORT, False)
        if self.server_port is None or self.server_port.strip() == "":
            raise RuntimeError("System property not found: " + cartridgeagentconstants.CEP_RECEIVER_PORT)

        self.admin_username = self.cartridge_agent_config.read_property(
            cartridgeagentconstants.CEP_SERVER_ADMIN_USERNAME, False)
        if self.admin_username is None or self.admin_username.strip() == "":
            raise RuntimeError("System property not found: " + cartridgeagentconstants.CEP_SERVER_ADMIN_USERNAME)

        self.admin_password = self.cartridge_agent_config.read_property(
            cartridgeagentconstants.CEP_SERVER_ADMIN_PASSWORD, False)
        if self.admin_password is None or self.admin_password.strip() == "":
            raise RuntimeError("System property not found: " + cartridgeagentconstants.CEP_SERVER_ADMIN_PASSWORD)

        CEPPublisherConfiguration.log.info("CEP Publisher configuration initialized")
