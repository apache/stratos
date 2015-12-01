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
import paho.mqtt.publish as publish
import time
from Queue import Queue, Empty

import constants
import healthstats
from config import Config
from modules.event.instance.status.events import *
from modules.util import cartridgeagentutils
from modules.util.cartridgeagentutils import IncrementalCeilingListIterator
from modules.util.log import *

log = LogFactory().get_log(__name__)
publishers = {}
""" :type : dict[str, EventPublisher] """


def publish_instance_started_event():
    if not Config.started:
        log.info("Publishing instance started event...")

        application_id = Config.application_id
        service_name = Config.service_name
        cluster_id = Config.cluster_id
        member_id = Config.member_id
        instance_id = Config.instance_id
        cluster_instance_id = Config.cluster_instance_id
        network_partition_id = Config.network_partition_id
        partition_id = Config.partition_id

        instance_started_event = InstanceStartedEvent(
            application_id,
            service_name,
            cluster_id,
            cluster_instance_id,
            member_id,
            instance_id,
            network_partition_id,
            partition_id)

        publisher = get_publisher(constants.INSTANCE_STATUS_TOPIC + constants.INSTANCE_STARTED_EVENT)
        publisher.publish(instance_started_event)
        Config.started = True
    else:
        log.warn("Instance already started")


def publish_instance_activated_event():
    if not Config.activated:
        # Wait for all ports to be active
        listen_address = Config.listen_address
        configuration_ports = Config.ports
        ports_active = cartridgeagentutils.wait_until_ports_active(
            listen_address,
            configuration_ports,
            int(Config.port_check_timeout))

        if ports_active:
            log.info("Publishing instance activated event...")
            service_name = Config.service_name
            cluster_id = Config.cluster_id
            member_id = Config.member_id
            instance_id = Config.instance_id
            cluster_instance_id = Config.cluster_instance_id
            network_partition_id = Config.network_partition_id
            partition_id = Config.partition_id

            instance_activated_event = InstanceActivatedEvent(
                service_name,
                cluster_id,
                cluster_instance_id,
                member_id,
                instance_id,
                network_partition_id,
                partition_id)

            publisher = get_publisher(constants.INSTANCE_STATUS_TOPIC + constants.INSTANCE_ACTIVATED_EVENT)
            publisher.publish(instance_activated_event)

            log.info("Starting health statistics notifier")
            health_stat_publishing_enabled = Config.read_property(constants.CEP_PUBLISHER_ENABLED, True)

            if health_stat_publishing_enabled:
                interval_default = 15  # seconds
                interval = Config.read_property("stats.notifier.interval", False)
                if interval is not None and len(interval) > 0:
                    try:
                        interval = int(interval)
                    except ValueError:
                        interval = interval_default
                else:
                    interval = interval_default
                health_stats_publisher = healthstats.HealthStatisticsPublisherManager(interval)
                log.info("Starting Health statistics publisher with interval %r" % interval)
                health_stats_publisher.start()
            else:
                log.warn("Statistics publisher is disabled")

            Config.activated = True
            log.info("Health statistics notifier started")
        else:
            log.error(
                "Ports activation timed out. Aborting publishing instance activated event [IPAddress] %s [Ports] %s"
                % (listen_address, configuration_ports))
    else:
        log.warn("Instance already activated")


def publish_maintenance_mode_event():
    if not Config.maintenance:
        log.info("Publishing instance maintenance mode event...")

        service_name = Config.service_name
        cluster_id = Config.cluster_id
        member_id = Config.member_id
        instance_id = Config.instance_id
        cluster_instance_id = Config.cluster_instance_id
        network_partition_id = Config.network_partition_id
        partition_id = Config.partition_id

        instance_maintenance_mode_event = InstanceMaintenanceModeEvent(
            service_name,
            cluster_id,
            cluster_instance_id,
            member_id,
            instance_id,
            network_partition_id,
            partition_id)

        publisher = get_publisher(constants.INSTANCE_STATUS_TOPIC + constants.INSTANCE_MAINTENANCE_MODE_EVENT)
        publisher.publish(instance_maintenance_mode_event)

        Config.maintenance = True
    else:
        log.warn("Instance already in a maintenance mode")


def publish_instance_ready_to_shutdown_event():
    if not Config.ready_to_shutdown:
        log.info("Publishing instance ready to shutdown event...")

        service_name = Config.service_name
        cluster_id = Config.cluster_id
        member_id = Config.member_id
        instance_id = Config.instance_id
        cluster_instance_id = Config.cluster_instance_id
        network_partition_id = Config.network_partition_id
        partition_id = Config.partition_id

        instance_shutdown_event = InstanceReadyToShutdownEvent(
            service_name,
            cluster_id,
            cluster_instance_id,
            member_id,
            instance_id,
            network_partition_id,
            partition_id)

        publisher = get_publisher(constants.INSTANCE_STATUS_TOPIC + constants.INSTANCE_READY_TO_SHUTDOWN_EVENT)
        publisher.publish(instance_shutdown_event)

        Config.ready_to_shutdown = True
    else:
        log.warn("Instance already in a ReadyToShutDown event...")


def publish_complete_topology_request_event():
    complete_topology_request_event = CompleteTopologyRequestEvent()
    publisher = get_publisher(constants.INITIALIZER_TOPIC + constants.COMPLETE_TOPOLOGY_REQUEST_EVENT)
    publisher.publish(complete_topology_request_event)


def publish_complete_tenant_request_event():
    complete_tenant_request_event = CompleteTenantRequestEvent()
    publisher = get_publisher(constants.INITIALIZER_TOPIC + constants.COMPLETE_TENANT_REQUEST_EVENT)
    publisher.publish(complete_tenant_request_event)


def get_publisher(topic):
    if topic not in publishers:
        publishers[topic] = EventPublisher(topic)

    return publishers[topic]


class EventPublisher(object):
    """
    Handles publishing events to topics to the provided message broker
    """

    def __init__(self, topic):
        self.__topic = topic
        self.__log = LogFactory().get_log(__name__)
        self.__start_time = int(time.time())
        self.__msg_queue = Queue()

    def publish(self, event):
        if Config.mb_username is None:
            auth = None
        else:
            auth = {"username": Config.mb_username, "password": Config.mb_password}

        payload = event.to_json()

        retry_iterator = IncrementalCeilingListIterator([2, 2, 5, 5, 10, 10, 20, 20, 30, 30, 40, 40, 50, 50, 60], False)

        # Retry to publish the event until the timeout exceeds
        while int(time.time()) - self.__start_time < (Config.mb_publisher_timeout * 1000):
            retry_interval = retry_iterator.get_next_retry_interval()

            for mb_url in Config.mb_urls.split(","):
                mb_ip, mb_port = mb_url.split(":")

                # start a thread to execute publish event
                publisher_thread = Thread(target=self.__publish_event, args=(event, mb_ip, mb_port, auth, payload))
                publisher_thread.start()

                # give sometime for the thread to complete
                time.sleep(5)

                # check if thread is still running and notify
                if publisher_thread.isAlive():
                    self.__log.debug(
                        "Event publishing timed out before succeeding. The message broker could be offline.")

                # check if publish.single() succeeded
                try:
                    published = self.__msg_queue.get(block=False)
                except Empty:
                    published = False

                if published:
                    return True

            # All the brokers on the list were offline
            self.__log.debug(
                "Could not publish event to any of the provided message brokers. Retrying in %s seconds."
                % retry_interval)

            time.sleep(retry_interval)

        # Even publisher timeout exceeded
        self.__log.warn("Could not publish event to any of the provided message brokers before "
                        "the timeout [%s] exceeded. The event will be dropped." % Config.mb_publisher_timeout)
        return False

    def __publish_event(self, event, mb_ip, mb_port, auth, payload):
        """
        Publishes the given event to the message broker.

        When a list of message brokers are given the event is published to the first message broker
        available. Therefore the message brokers should share the data (ex: Sharing the KahaDB in ActiveMQ).

        When the event cannot be published, it will be retried until the mb_publisher_timeout is exceeded.
        This value is set in the agent.conf.

        :param event:
        :return: True if the event was published.
        """
        try:
            self.__log.debug("Publishing [event] %s to %s:%s" % (event.__class__.__name__, mb_ip, mb_port))
            publish.single(self.__topic, payload, hostname=mb_ip, port=mb_port, auth=auth)
            self.__log.debug("[Event] %s published to MB: %s:%s" % (str(event.__class__.__name__), mb_ip, mb_port))
            self.__msg_queue.put(True)
        except Exception as err:
            self.__log.debug(
                "Could not publish [event] %s to message broker %s:%s. : %s"
                % (str(event.__class__.__name__), mb_ip, mb_port, err))
            self.__msg_queue.put(False)
