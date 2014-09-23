import logging

import paho.mqtt.publish as publish

from .. event.instance.status.events import *
from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from .. util import cartridgeagentconstants


logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)

started = False
activated = False
ready_to_shutdown = False
maintenance = False

cartridge_agent_config = CartridgeAgentConfiguration()

publishers = {}


def publish_instance_started_event():
    global started, log, cartridge_agent_config
    if not started:
        log.info("Publishing instance started event")
        service_name = cartridge_agent_config.get_service_name()
        cluster_id = cartridge_agent_config.get_cluster_id()
        network_partition_id = cartridge_agent_config.get_network_partition_id()
        parition_id = cartridge_agent_config.get_partition_id()
        member_id = cartridge_agent_config.get_member_id()

        instance_started_event = InstanceStartedEvent(service_name, cluster_id, network_partition_id, parition_id,
                                                      member_id)
        publisher = get_publisher(cartridgeagentconstants.INSTANCE_STATUS_TOPIC)
        publisher.publish(instance_started_event)
        started = True
        log.info("Instance started event published")
    else:
        log.warn("Instance already started")


def publish_instance_activated_event():
    global activated, log, cartridge_agent_config
    if not activated:
        log.info("Publishing instance activated event")
        service_name = cartridge_agent_config.get_service_name()
        cluster_id = cartridge_agent_config.get_cluster_id()
        network_partition_id = cartridge_agent_config.get_network_partition_id()
        parition_id = cartridge_agent_config.get_partition_id()
        member_id = cartridge_agent_config.get_member_id()

        instance_activated_event = InstanceActivatedEvent(service_name, cluster_id, network_partition_id, parition_id,
                                                          member_id)
        publisher = get_publisher(cartridgeagentconstants.INSTANCE_STATUS_TOPIC)
        publisher.publish(instance_activated_event)

        log.info("Instance activated event published")
        log.info("Starting health statistics notifier")

        # TODO: health stat publisher start()
        activated = True
        log.info("Health statistics notifier started")
    else:
        log.warn("Instance already activated")


def get_publisher(topic):
    if topic not in publishers:
        publishers[topic] = EventPublisher(topic)

    return publishers[topic]


class EventPublisher:
    def __init__(self, topic):
        self.__topic = topic
        self.cartridge_agent_config = CartridgeAgentConfiguration()

    """
    msgs = [{'topic': "instance/status/InstanceStartedEvent", 'payload': instance_started_event.to_JSON()}]
    #publish.single("instance", instance_started_event.to_JSON(), hostname="localhost", port=1883)
    publish.multiple(msgs, "localhost", 1883)
    """

    def publish(self, event):
        mb_ip = self.cartridge_agent_config.read_property(cartridgeagentconstants.MB_IP)
        mb_port = self.cartridge_agent_config.read_property(cartridgeagentconstants.MB_PORT)
        payload = event.to_json()
        publish.single(self.__topic, payload, hostname=mb_ip, port=mb_port)