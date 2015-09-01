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
import multiprocessing

import psutil

from modules.databridge.agent import *
from config import Config
from modules.util import cartridgeagentutils
from exception import ThriftReceiverOfflineException, CEPPublisherException
import constants


class HealthStatisticsPublisherManager(Thread):
    """
    Read from provided health stat reader plugin or the default health stat reader, the value for memory usage and
    load average and publishes them as ThriftEvents to a CEP server
    """
    STREAM_NAME = "cartridge_agent_health_stats"
    STREAM_VERSION = "1.0.0"
    STREAM_NICKNAME = "agent health stats"
    STREAM_DESCRIPTION = "agent health stats"

    def __init__(self, publish_interval, health_stat_plugin):
        """
        Initializes a new HealthStatisticsPublisherManager with a given number of seconds as the interval
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
        # If there are no health stat reader plugins, create the default reader instance
        self.stats_reader = health_stat_plugin if health_stat_plugin is not None else DefaultHealthStatisticsReader()

    def run(self):
        while not self.terminated:
            time.sleep(self.publish_interval)

            try:
                ca_health_stat = CartridgeHealthStatistics()
                cartridge_stats = self.stats_reader.stat_cartridge_health(ca_health_stat)
                self.log.debug("Publishing memory consumption: %r" % cartridge_stats.memory_usage)
                self.publisher.publish_memory_usage(cartridge_stats.memory_usage)

                self.log.debug("Publishing load average: %r" % cartridge_stats.load_avg)
                self.publisher.publish_load_average(cartridge_stats.load_avg)
            except ThriftReceiverOfflineException:
                self.log.exception("Couldn't publish health statistics to CEP. Thrift Receiver offline. Reconnecting...")
                self.publisher = HealthStatisticsPublisher()

        self.publisher.publisher.disconnect()


class HealthStatisticsPublisher:
    """
    Publishes memory usage and load average to thrift server
    """
    log = LogFactory().get_log(__name__)

    @staticmethod
    def read_config(conf_key):
        """
        Read a given key from the cartridge agent configuration
        :param conf_key: The key to look for in the CA config
        :return: The value for the key from the CA config
        :raise: RuntimeError if the given key is not found in the CA config
        """
        conf_value = Config.read_property(conf_key, False)

        if conf_value is None or conf_value.strip() == "":
            raise RuntimeError("System property not found: " + conf_key)

        return conf_value

    def __init__(self):
        self.ports = []
        cep_port = HealthStatisticsPublisher.read_config(constants.CEP_RECEIVER_PORT)
        self.ports.append(cep_port)

        cep_ip = HealthStatisticsPublisher.read_config(constants.CEP_RECEIVER_IP)

        cartridgeagentutils.wait_until_ports_active(
            cep_ip,
            self.ports,
            int(Config.read_property("port.check.timeout", critical=False)))

        cep_active = cartridgeagentutils.check_ports_active(
            cep_ip,
            self.ports)

        if not cep_active:
            raise CEPPublisherException("CEP server not active. Health statistics publishing aborted.")

        cep_admin_username = HealthStatisticsPublisher.read_config(constants.CEP_SERVER_ADMIN_USERNAME)
        cep_admin_password = HealthStatisticsPublisher.read_config(constants.CEP_SERVER_ADMIN_PASSWORD)

        self.stream_definition = HealthStatisticsPublisher.create_stream_definition()
        HealthStatisticsPublisher.log.debug("Stream definition created: %r" % str(self.stream_definition))

        self.publisher = ThriftPublisher(
            cep_ip,
            cep_port,
            cep_admin_username,
            cep_admin_password,
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

        # stream_def.add_payloaddata_attribute()
        stream_def.add_payloaddata_attribute("cluster_id", StreamDefinition.STRING)
        stream_def.add_payloaddata_attribute("cluster_instance_id", StreamDefinition.STRING)
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
        event.payloadData.append(Config.cluster_id)
        event.payloadData.append(Config.cluster_instance_id)
        event.payloadData.append(Config.network_partition_id)
        event.payloadData.append(Config.member_id)
        event.payloadData.append(Config.partition_id)
        event.payloadData.append(constants.MEMORY_CONSUMPTION)
        event.payloadData.append(float(memory_usage))
        # event.payloadData.append(str(memory_usage))

        HealthStatisticsPublisher.log.debug("Publishing cep event: [stream] %r [payload_data] %r [version] %r"
                                            % (
                                                self.stream_definition.name,
                                                event.payloadData,
                                                self.stream_definition.version))

        self.publisher.publish(event)

    def publish_load_average(self, load_avg):
        """
        Publishes the given load average value to the thrift server as a ThriftEvent
        :param float load_avg: load average value
        """

        event = ThriftEvent()
        event.payloadData.append(Config.cluster_id)
        event.payloadData.append(Config.cluster_instance_id)
        event.payloadData.append(Config.network_partition_id)
        event.payloadData.append(Config.member_id)
        event.payloadData.append(Config.partition_id)
        event.payloadData.append(constants.LOAD_AVERAGE)
        event.payloadData.append(float(load_avg))
        # event.payloadData.append(str(load_avg))

        HealthStatisticsPublisher.log.debug("Publishing cep event: [stream] %r [payload_data] %r [version] %r"
                                            % (
                                                self.stream_definition.name,
                                                event.payloadData,
                                                self.stream_definition.version))

        self.publisher.publish(event)


class DefaultHealthStatisticsReader:
    """
    Default implementation for the health statistics reader. If no Health Statistics Reader plugins are provided,
    this will be used to read health stats from the instance.
    """

    def __init__(self):
        self.log = LogFactory().get_log(__name__)

    def stat_cartridge_health(self, ca_health_stat):
        ca_health_stat.memory_usage = DefaultHealthStatisticsReader.__read_mem_usage()
        ca_health_stat.load_avg = DefaultHealthStatisticsReader.__read_load_avg()

        self.log.debug("Memory read: %r, CPU read: %r" % (ca_health_stat.memory_usage, ca_health_stat.load_avg))
        return ca_health_stat

    @staticmethod
    def __read_mem_usage():
        return psutil.virtual_memory().percent

    @staticmethod
    def __read_load_avg():
        (one, five, fifteen) = os.getloadavg()
        cores = multiprocessing.cpu_count()

        return (one/cores) * 100


class CartridgeHealthStatistics:
    """
    Holds the memory usage and load average reading
    """

    def __init__(self):
        self.memory_usage = None
        """:type : float"""
        self.load_avg = None
        """:type : float"""
