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

publishers = {}


def publish_instance_started_event():
    global started, log
    if not started:
        log.info("Publishing instance started event")
        service_name = CartridgeAgentConfiguration.service_name
        cluster_id = CartridgeAgentConfiguration.cluster_id
        network_partition_id = CartridgeAgentConfiguration.network_partition_id
        parition_id = CartridgeAgentConfiguration.partition_id
        member_id = CartridgeAgentConfiguration.member_id

        instance_started_event = InstanceStartedEvent(service_name, cluster_id, network_partition_id, parition_id,
                                                      member_id)
        publisher = get_publisher(cartridgeagentconstants.INSTANCE_STATUS_TOPIC)
        publisher.publish(instance_started_event)
        started = True
        log.info("Instance started event published")
    else:
        log.warn("Instance already started")


def publish_instance_activated_event():
    global activated, log
    if not activated:
        log.info("Publishing instance activated event")
        service_name = CartridgeAgentConfiguration.service_name
        cluster_id = CartridgeAgentConfiguration.cluster_id
        network_partition_id = CartridgeAgentConfiguration.network_partition_id
        parition_id = CartridgeAgentConfiguration.partition_id
        member_id = CartridgeAgentConfiguration.member_id

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


def publish_maintenance_mode_event():
    global maintenance, log
    if not maintenance:
        log.info("Publishing instance maintenance mode event")

        service_name = CartridgeAgentConfiguration.service_name
        cluster_id = CartridgeAgentConfiguration.cluster_id
        network_partition_id = CartridgeAgentConfiguration.network_partition_id
        parition_id = CartridgeAgentConfiguration.partition_id
        member_id = CartridgeAgentConfiguration.member_id

        instance_maintenance_mode_event = InstanceMaintenanceModeEvent(service_name, cluster_id, network_partition_id, parition_id,
                                                          member_id)

        publisher = get_publisher(cartridgeagentconstants.INSTANCE_STATUS_TOPIC)
        publisher.publish(instance_maintenance_mode_event)

        maintenance = True
        log.info("Instance Maintenance mode event published")
    else:
        log.warn("Instance already in a Maintenance mode....")


def publish_instance_ready_to_shutdown_event():
    global ready_to_shutdown, log
    if not ready_to_shutdown:
        log.info("Publishing instance activated event")

        service_name = CartridgeAgentConfiguration.service_name
        cluster_id = CartridgeAgentConfiguration.cluster_id
        network_partition_id = CartridgeAgentConfiguration.network_partition_id
        parition_id = CartridgeAgentConfiguration.partition_id
        member_id = CartridgeAgentConfiguration.member_id

        instance_shutdown_event = InstanceReadyToShutdownEvent(service_name, cluster_id, network_partition_id, parition_id,
                                                          member_id)

        publisher = get_publisher(cartridgeagentconstants.INSTANCE_STATUS_TOPIC)
        publisher.publish(instance_shutdown_event)

        ready_to_shutdown = True
        log.info("Instance ReadyToShutDown event published")
    else:
        log.warn("Instance already in a ReadyToShutDown event....")


def get_publisher(topic):
    if topic not in publishers:
        publishers[topic] = EventPublisher(topic)

    return publishers[topic]


class EventPublisher:
    def __init__(self, topic):
        self.__topic = topic

    """
    msgs = [{'topic': "instance/status/InstanceStartedEvent", 'payload': instance_started_event.to_JSON()}]
    #publish.single("instance", instance_started_event.to_JSON(), hostname="localhost", port=1883)
    publish.multiple(msgs, "localhost", 1883)
    """

    def publish(self, event):
        mb_ip = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.MB_IP)
        mb_port = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.MB_PORT)
        payload = event.to_json()
        publish.single(self.__topic, payload, hostname=mb_ip, port=mb_port)