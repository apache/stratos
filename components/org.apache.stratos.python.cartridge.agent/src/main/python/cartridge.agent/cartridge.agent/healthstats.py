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

    def __init__(self, publish_interval):
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

        """:type : IHealthStatReaderPlugin"""
        self.stats_reader = Config.health_stat_plugin

    def run(self):
        while not self.terminated:
            time.sleep(self.publish_interval)

            try:
                ca_health_stat = CartridgeHealthStatistics()
                cartridge_stats = self.stats_reader.plugin_object.stat_cartridge_health(ca_health_stat)
                self.log.debug("Publishing memory consumption: %r" % cartridge_stats.memory_usage)
                self.publisher.publish_memory_usage(cartridge_stats.memory_usage)

                self.log.debug("Publishing load average: %r" % cartridge_stats.load_avg)
                self.publisher.publish_load_average(cartridge_stats.load_avg)
            except ThriftReceiverOfflineException:
                self.log.exception(
                    "Couldn't publish health statistics to CEP. Thrift Receiver offline. Reconnecting...")
                self.publisher = HealthStatisticsPublisher()

        self.publisher.disconnect_publisher()


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

        self.publishers = []
        self.deactive_publishers = []
        self.cep_admin_username = HealthStatisticsPublisher.read_config(constants.CEP_SERVER_ADMIN_USERNAME)
        self.cep_admin_password = HealthStatisticsPublisher.read_config(constants.CEP_SERVER_ADMIN_PASSWORD)
        self.stream_definition = HealthStatisticsPublisher.create_stream_definition()
        HealthStatisticsPublisher.log.debug("Stream definition created: %r" % str(self.stream_definition))

        # 1.1.1.1:1883,2.2.2.2:1883
        cep_urls = HealthStatisticsPublisher.read_config(constants.CEP_RECEIVER_URLS)
        cep_urls = cep_urls.split(',')
        for cep_url in cep_urls:

            cep_active = self.is_cep_active(cep_url)

            if cep_active:
                self.add_publishers(cep_url)
            else:
                HealthStatisticsPublisher.log.warn("CEP server is not active... %r" % cep_url)
                self.deactive_publishers.append(cep_url)

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

        self.publish_event(event)

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

        self.publish_event(event)

    def add_publishers(self, cep_url):
        """
        Add publishers to the publisher list for publishing
        """
        cep_ip = cep_url.split(':')[0]
        cep_port = cep_url.split(':')[1]

        publisher = ThriftPublisher(
            cep_ip,
            cep_port,
            self.cep_admin_username,
            self.cep_admin_password,
            self.stream_definition)

        self.publishers.append(publisher)


    def is_cep_active(self, cep_url):
        """
        Check if the cep node is active
        return true if active
        """
        self.ports = []
        cep_ip = cep_url.split(':')[0]
        cep_port = cep_url.split(':')[1]
        self.ports.append(cep_port)

        cep_active = cartridgeagentutils.check_ports_active(
            cep_ip,
            self.ports)

        return cep_active


    def publish_event(self, event):
        """
        Publish events to cep nodes
        """
        for publisher in self.publishers:
            try:
                publisher.publish(event)
            except Exception as ex:
                raise ThriftReceiverOfflineException(ex)

        deactive_ceps = self.deactive_publishers
        for cep_url in deactive_ceps:
            cep_active = self.is_cep_active(cep_url)
            if cep_active:
                self.add_publishers(cep_url)
                self.deactive_publishers.remove(cep_url)

    def disconnect_publisher(self):
        """
        Disconnect publishers
        """
        for publisher in self.publishers:
            publisher.disconnect()


class CartridgeHealthStatistics:
    """
    Holds the memory usage and load average reading
    """

    def __init__(self):
        self.memory_usage = None
        """:type : float"""
        self.load_avg = None
        """:type : float"""
