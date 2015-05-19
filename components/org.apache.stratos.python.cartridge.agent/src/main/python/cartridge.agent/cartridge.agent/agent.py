#!/usr/bin/env python
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

import threading

from modules.subscriber.eventsubscriber import EventSubscriber
from modules.publisher import cartridgeagentpublisher
from modules.event.instance.notifier.events import *
from modules.event.tenant.events import *
from modules.event.topology.events import *
from modules.event.application.signup.events import *
from modules.event.domain.mapping.events import *
from modules.tenant.tenantcontext import *
from modules.topology.topologycontext import *
from modules.datapublisher.logpublisher import *
from config import CartridgeAgentConfiguration
from modules.event.eventhandler import EventHandler
import constants


class CartridgeAgent(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)

        self.__tenant_context_initialized = False
        self.__log_publish_manager = None
        self.__terminated = False
        self.__log = LogFactory().get_log(__name__)
        self.__config = CartridgeAgentConfiguration()

        mb_ip = self.__config.read_property(constants.MB_IP)
        mb_port = self.__config.read_property(constants.MB_PORT)

        self.__inst_topic_subscriber = EventSubscriber(constants.INSTANCE_NOTIFIER_TOPIC, mb_ip, mb_port)
        self.__tenant_topic_subscriber = EventSubscriber(constants.TENANT_TOPIC, mb_ip, mb_port)
        self.__app_topic_subscriber = EventSubscriber(constants.APPLICATION_SIGNUP, mb_ip, mb_port)
        self.__topology_event_subscriber = EventSubscriber(constants.TOPOLOGY_TOPIC, mb_ip, mb_port)

        self.__event_handler = EventHandler()

    def run(self):
        self.__log.info("Starting Cartridge Agent...")

        # Start topology event receiver thread
        self.register_topology_event_listeners()

        if self.__config.lvs_virtual_ip is None or str(self.__config.lvs_virtual_ip).strip() == "":
            self.__log.debug("LVS Virtual IP is not defined")
        else:
            self.__event_handler.create_dummy_interface()

        # wait until complete topology message is received to get LB IP
        self.wait_for_complete_topology()

        # wait for member initialized event
        while not self.__config.initialized:
            self.__log.debug("Waiting for cartridge agent to be initialized...")
            time.sleep(1)

        # Start instance notifier listener thread
        self.register_instance_topic_listeners()

        # Start tenant event receiver thread
        self.register_tenant_event_listeners()

        # start application signup event listener
        self.register_application_signup_event_listeners()

        # Execute instance started shell script
        self.__event_handler.on_instance_started_event()

        # Publish instance started event
        cartridgeagentpublisher.publish_instance_started_event()

        # Execute start servers extension
        try:
            self.__event_handler.start_server_extension()
        except Exception as e:
            self.__log.exception("Error processing start servers event: %s" % e)

        # check if artifact management is required before publishing instance activated event
        repo_url = self.__config.repo_url
        if repo_url is None or str(repo_url).strip() == "":
            self.__log.info("No artifact repository found")
            self.__event_handler.on_instance_activated_event()
            cartridgeagentpublisher.publish_instance_activated_event()
        else:
            self.__log.info(
                "Artifact repository found, waiting for artifact updated event to checkout artifacts: [repo_url] %s",
                repo_url)

        persistence_mapping_payload = self.__config.persistence_mappings
        if persistence_mapping_payload is not None:
            self.__event_handler.volume_mount_extension(persistence_mapping_payload)

        # start log publishing thread
        if DataPublisherConfiguration.get_instance().enabled:
            log_file_paths = self.__config.log_file_paths
            if log_file_paths is None:
                self.__log.exception("No valid log file paths found, no logs will be published")
            else:
                self.__log.debug("Starting Log Publisher Manager: [Log file paths] %s" % ", ".join(log_file_paths))
                self.__log_publish_manager = LogPublisherManager(log_file_paths)
                self.__log_publish_manager.start()

        # run until terminated
        while not self.__terminated:
            time.sleep(1)

        if DataPublisherConfiguration.get_instance().enabled:
            self.__log_publish_manager.terminate_all_publishers()

    def terminate(self):
        """
        Allows the CartridgeAgent thread to be terminated

        :return: void
        """
        self.__terminated = True

    def register_instance_topic_listeners(self):
        self.__log.debug("Starting instance notifier event message receiver thread")

        self.__inst_topic_subscriber.register_handler("ArtifactUpdatedEvent", self.on_artifact_updated)
        self.__inst_topic_subscriber.register_handler("InstanceCleanupMemberEvent", self.on_instance_cleanup_member)
        self.__inst_topic_subscriber.register_handler("InstanceCleanupClusterEvent", self.on_instance_cleanup_cluster)

        self.__inst_topic_subscriber.start()
        self.__log.info("Instance notifier event message receiver thread started")

        # wait till subscribed to continue
        while not self.__inst_topic_subscriber.is_subscribed():
            time.sleep(1)

    def register_topology_event_listeners(self):
        self.__log.debug("Starting topology event message receiver thread")

        self.__topology_event_subscriber.register_handler("MemberActivatedEvent", self.on_member_activated)
        self.__topology_event_subscriber.register_handler("MemberTerminatedEvent", self.on_member_terminated)
        self.__topology_event_subscriber.register_handler("MemberSuspendedEvent", self.on_member_suspended)
        self.__topology_event_subscriber.register_handler("CompleteTopologyEvent", self.on_complete_topology)
        self.__topology_event_subscriber.register_handler("MemberStartedEvent", self.on_member_started)
        self.__topology_event_subscriber.register_handler("MemberCreatedEvent", self.on_member_created)
        self.__topology_event_subscriber.register_handler("MemberInitializedEvent", self.on_member_initialized)

        self.__topology_event_subscriber.start()
        self.__log.info("Cartridge agent topology receiver thread started")

        # wait till subscribed to continue
        while not self.__topology_event_subscriber.is_subscribed():
            time.sleep(1)

    def register_tenant_event_listeners(self):
        self.__log.debug("Starting tenant event message receiver thread")
        self.__tenant_topic_subscriber.register_handler("DomainMappingAddedEvent",
                                                        self.on_domain_mapping_added)
        self.__tenant_topic_subscriber.register_handler("DomainsMappingRemovedEvent",
                                                        self.on_domain_mapping_removed)
        self.__tenant_topic_subscriber.register_handler("CompleteTenantEvent", self.on_complete_tenant)
        self.__tenant_topic_subscriber.register_handler("TenantSubscribedEvent", self.on_tenant_subscribed)

        self.__tenant_topic_subscriber.start()
        self.__log.info("Tenant event message receiver thread started")

        # wait till subscribed to continue
        while not self.__tenant_topic_subscriber.is_subscribed():
            time.sleep(1)

    def register_application_signup_event_listeners(self):
        self.__log.debug("Starting application signup event message receiver thread")
        self.__app_topic_subscriber.register_handler("ApplicationSignUpRemovedEvent",
                                                     self.on_application_signup_removed)

        self.__app_topic_subscriber.start()
        self.__log.info("Application signup event message receiver thread started")

        # wait till subscribed to continue
        while not self.__app_topic_subscriber.is_subscribed():
            time.sleep(1)

    def on_artifact_updated(self, msg):
        event_obj = ArtifactUpdatedEvent.create_from_json(msg.payload)
        self.__event_handler.on_artifact_updated_event(event_obj)

    def on_instance_cleanup_member(self, msg):
        member_in_payload = self.__config.member_id
        event_obj = InstanceCleanupMemberEvent.create_from_json(msg.payload)
        member_in_event = event_obj.member_id
        if member_in_payload == member_in_event:
            self.__event_handler.on_instance_cleanup_member_event()

    def on_instance_cleanup_cluster(self, msg):
        event_obj = InstanceCleanupClusterEvent.create_from_json(msg.payload)
        cluster_in_payload = self.__config.cluster_id
        cluster_in_event = event_obj.cluster_id
        instance_in_payload = self.__config.cluster_instance_id
        instance_in_event = event_obj.cluster_instance_id

        if cluster_in_event == cluster_in_payload and instance_in_payload == instance_in_event:
            self.__event_handler.on_instance_cleanup_cluster_event()

    def on_member_created(self, msg):
        self.__log.debug("Member created event received: %r" % msg.payload)

    def on_member_initialized(self, msg):
        self.__log.debug("Member initialized event received: %r" % msg.payload)

        if not TopologyContext.topology.initialized:
            return

        self.__event_handler.on_member_initialized_event()

    def on_member_activated(self, msg):
        self.__log.debug("Member activated event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberActivatedEvent.create_from_json(msg.payload)
        self.__event_handler.on_member_activated_event(event_obj)

    def on_member_terminated(self, msg):
        self.__log.debug("Member terminated event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberTerminatedEvent.create_from_json(msg.payload)
        self.__event_handler.on_member_terminated_event(event_obj)

    def on_member_suspended(self, msg):
        self.__log.debug("Member suspended event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberSuspendedEvent.create_from_json(msg.payload)
        self.__event_handler.on_member_suspended_event(event_obj)

    def on_complete_topology(self, msg):
        if not TopologyContext.topology.initialized:
            self.__log.debug("Complete topology event received")
            event_obj = CompleteTopologyEvent.create_from_json(msg.payload)
            TopologyContext.update(event_obj.topology)
            self.__event_handler.on_complete_topology_event(event_obj)
        else:
            self.__log.debug("Complete topology event updating task disabled")

    def on_member_started(self, msg):
        self.__log.debug("Member started event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberStartedEvent.create_from_json(msg.payload)
        self.__event_handler.on_member_started_event(event_obj)

    def on_domain_mapping_added(self, msg):
        self.__log.debug("Subscription domain added event received : %r" % msg.payload)
        event_obj = DomainMappingAddedEvent.create_from_json(msg.payload)
        self.__event_handler.on_domain_mapping_added_event(event_obj)

    def on_domain_mapping_removed(self, msg):
        self.__log.debug("Subscription domain removed event received : %r" % msg.payload)
        event_obj = DomainMappingRemovedEvent.create_from_json(msg.payload)
        self.__event_handler.on_domain_mapping_removed_event(event_obj)

    def on_complete_tenant(self, msg):
        if not self.__tenant_context_initialized:
            self.__log.debug("Complete tenant event received")
            event_obj = CompleteTenantEvent.create_from_json(msg.payload)
            TenantContext.update(event_obj.tenants)

            self.__event_handler.on_complete_tenant_event(event_obj)
            self.__tenant_context_initialized = True
        else:
            self.__log.debug("Complete tenant event updating task disabled")

    def on_tenant_subscribed(self, msg):
        self.__log.debug("Tenant subscribed event received: %r" % msg.payload)
        event_obj = TenantSubscribedEvent.create_from_json(msg.payload)
        self.__event_handler.on_tenant_subscribed_event(event_obj)

    def on_application_signup_removed(self, msg):
        self.__log.debug("Application signup removed event received: %r" % msg.payload)
        event_obj = ApplicationSignUpRemovedEvent.create_from_json(msg.payload)
        self.__event_handler.on_application_signup_removed_event(event_obj)

    def wait_for_complete_topology(self):
        while not TopologyContext.topology.initialized:
            self.__log.info("Waiting for complete topology event...")
            time.sleep(5)
        self.__log.info("Complete topology event received")


def main():
    cartridge_agent = CartridgeAgent()
    log = LogFactory().get_log(__name__)

    try:
        log.debug("Starting cartridge agent")
        cartridge_agent.start()
    except Exception as e:
        log.exception("Cartridge Agent Exception: %r" % e)
        cartridge_agent.terminate()


if __name__ == "__main__":
    main()
