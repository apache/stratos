from threading import Thread
import time
import logging

from abstracthealthstatisticspublisher import *
from ..databridge.agent import *
from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ..util import cartridgeagentutils, cartridgeagentconstants


class HealthStatisticsPublisherManager(Thread):
    """
    Read from an implementation of AbstractHealthStatisticsPublisher the value for memory usage and
    load average and publishes them as ThriftEvents to a CEP server
    """
    def __init__(self, publish_interval):
        """
        Initializes a new HealthStatistsPublisherManager with a given number of seconds as the interval
        :param int publish_interval: Number of seconds as the interval
        :return: void
        """
        Thread.__init__(self)

        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)

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


class HealthStatisticsPublisher:
    """
    Publishes memory usage and load average to thrift server
    """
    def __init__(self):
        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)
        self.ports = []
        self.ports.append(CEPPublisherConfiguration.get_instance().server_port)
        cartridgeagentutils.wait_until_ports_active(CEPPublisherConfiguration.get_instance().server_ip, self.ports)
        cep_active = cartridgeagentutils.check_ports_active(CEPPublisherConfiguration.get_instance().server_ip, self.ports)
        if not cep_active:
            raise CEPPublisherException("CEP server not active. Health statistics publishing aborted.")

        self.stream_definition = HealthStatisticsPublisher.create_stream_definition()
        self.publisher = ThriftPublisher(
            CEPPublisherConfiguration.get_instance().server_ip,
            CEPPublisherConfiguration.get_instance().server_port,
            CEPPublisherConfiguration.get_instance().admin_username,
            CEPPublisherConfiguration.get_instance().admin_password,
            self.stream_definition)


    @staticmethod
    def create_stream_definition():
        """
        Create a StreamDefinition for publishing to CEP
        """
        stream_def = StreamDefinition()
        stream_def.name = "cartridge_agent_health_stats"
        stream_def.version = "1.0.0"
        stream_def.nickname = "agent health stats"
        stream_def.description = "agent health stats"

        stream_def.add_payloaddata_attribute("cluster_id", "STRING")
        stream_def.add_payloaddata_attribute("network_partition_id", "STRING")
        stream_def.add_payloaddata_attribute("member_id", "STRING")
        stream_def.add_payloaddata_attribute("partition_id", "STRING")
        stream_def.add_payloaddata_attribute("health_description", "STRING")
        stream_def.add_payloaddata_attribute("value", "DOUBLE")

        return stream_def

    def publish_memory_usage(self, memory_usage):
        """
        Publishes the given memory usage value to the thrift server as a ThriftEvent
        :param float memory_usage: memory usage
        """

        event = ThriftEvent()
        event.payloadData.append(CartridgeAgentConfiguration.cluster_id)
        event.payloadData.append(CartridgeAgentConfiguration.network_partition_id)
        event.payloadData.append(CartridgeAgentConfiguration.member_id)
        event.payloadData.append(CartridgeAgentConfiguration.partition_id)
        event.payloadData.append(cartridgeagentconstants.MEMORY_CONSUMPTION)
        event.payloadData.append(memory_usage)

        self.log.debug("Publishing cep event: [stream] %r [version] %r" % (self.stream_definition.name, self.stream_definition.version))
        self.publisher.publish(event)

    def publish_load_average(self, load_avg):
        """
        Publishes the given load average value to the thrift server as a ThriftEvent
        :param float load_avg: load average value
        """

        event = ThriftEvent()
        event.payloadData.append(CartridgeAgentConfiguration.cluster_id)
        event.payloadData.append(CartridgeAgentConfiguration.network_partition_id)
        event.payloadData.append(CartridgeAgentConfiguration.member_id)
        event.payloadData.append(CartridgeAgentConfiguration.partition_id)
        event.payloadData.append(cartridgeagentconstants.LOAD_AVERAGE)
        event.payloadData.append(load_avg)

        self.log.debug("Publishing cep event: [stream] %r [version] %r" % (self.stream_definition.name, self.stream_definition.version))
        self.publisher.publish(event)


class DefaultHealthStatisticsReader(AbstractHealthStatisticsReader):
    """
    Default implementation of the AbstractHealthStatisticsReader
    """

    def stat_cartridge_health(self):
        cartridge_stats = CartridgeHealthStatistics()
        cartridge_stats.memory_usage = DefaultHealthStatisticsReader.__read_mem_usage()
        cartridge_stats.load_avg = DefaultHealthStatisticsReader.__read_load_avg()

        return cartridge_stats

    @staticmethod
    def __read_mem_usage():
        raise NotImplementedError

    @staticmethod
    def __read_load_avg():
        raise NotImplementedError


class CEPPublisherConfiguration:
    """
    A singleton implementation to access configuration information for data publishing to BAM/CEP
    TODO: perfect singleton impl ex: Borg
    """

    __instance = None
    logging.basicConfig(level=logging.DEBUG)
    log = logging.getLogger(__name__)

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
        self.read_config()

    def read_config(self):
        self.enabled = True if CartridgeAgentConfiguration.read_property("cep.stats.publisher.enabled", False).strip().lower() == "true" else False
        if not self.enabled:
            CEPPublisherConfiguration.log.info("CEP Publisher disabled")
            return

        CEPPublisherConfiguration.log.info("CEP Publisher enabled")

        self.server_ip = CartridgeAgentConfiguration.read_property("thrift.receiver.ip", False)
        if self.server_ip.strip() == "":
            raise RuntimeError("System property not found: thrift.receiver.ip")

        self.server_port = CartridgeAgentConfiguration.read_property("thrift.receiver.port", False)
        if self.server_port.strip() == "":
            raise RuntimeError("System property not found: thrift.receiver.port")

        self.admin_username = CartridgeAgentConfiguration.read_property("thrift.server.admin.username", False)
        if self.admin_username.strip() == "":
            raise RuntimeError("System property not found: thrift.server.admin.username")

        self.admin_password = CartridgeAgentConfiguration.read_property("thrift.server.admin.password", False)
        if self.admin_password.strip() == "":
            raise RuntimeError("System property not found: thrift.server.admin.password")

        CEPPublisherConfiguration.log.info("CEP Publisher configuration initialized")
