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

import publisher
from logpublisher import *
from modules.event.application.signup.events import *
from modules.event.domain.mapping.events import *
import modules.event.eventhandler as event_handler
from modules.event.instance.notifier.events import *
from modules.event.tenant.events import *
from modules.event.topology.events import *
from subscriber import EventSubscriber


class CartridgeAgent(object):
    def __init__(self):
        Config.initialize_config()
        self.__terminated = False
        self.__log = LogFactory().get_log(__name__)

        mb_urls = Config.mb_urls.split(",")
        mb_uname = Config.mb_username
        mb_pwd = Config.mb_password

        self.__inst_topic_subscriber = EventSubscriber(constants.INSTANCE_NOTIFIER_TOPIC, mb_urls, mb_uname, mb_pwd)
        self.__tenant_topic_subscriber = EventSubscriber(constants.TENANT_TOPIC, mb_urls, mb_uname, mb_pwd)
        self.__app_topic_subscriber = EventSubscriber(constants.APPLICATION_SIGNUP, mb_urls, mb_uname, mb_pwd)
        self.__topology_event_subscriber = EventSubscriber(constants.TOPOLOGY_TOPIC, mb_urls, mb_uname, mb_pwd)

    def run_agent(self):
        self.__log.info("Starting Cartridge Agent...")

        # Start topology event receiver thread
        self.register_topology_event_listeners()

        if Config.lvs_virtual_ip is None or str(Config.lvs_virtual_ip).strip() == "":
            self.__log.debug("LVS Virtual IP is not defined")
        else:
            event_handler.create_dummy_interface()

        # request complete topology event from CC by publishing CompleteTopologyRequestEvent
        publisher.publish_complete_topology_request_event()

        # wait until complete topology message is received to get LB IP
        self.wait_for_complete_topology()

        # wait for member initialized event
        while not Config.initialized:
            self.__log.debug("Waiting for cartridge agent to be initialized...")
            time.sleep(1)

        # Start instance notifier listener thread
        self.register_instance_topic_listeners()

        # Start tenant event receiver thread
        self.register_tenant_event_listeners()

        # start application signup event listener
        self.register_application_signup_event_listeners()

        # request complete tenant event from CC by publishing CompleteTenantRequestEvent
        publisher.publish_complete_tenant_request_event()

        # Execute instance started shell script
        event_handler.on_instance_started_event()

        # Publish instance started event
        publisher.publish_instance_started_event()

        # Execute start servers extension
        try:
            event_handler.start_server_extension()
        except Exception as ex:
            self.__log.exception("Error processing start servers event: %s" % ex)

        # check if artifact management is required before publishing instance activated event
        repo_url = Config.repo_url
        if repo_url is None or str(repo_url).strip() == "":
            self.__log.info("No artifact repository found")
            publisher.publish_instance_activated_event()
            event_handler.on_instance_activated_event()
        else:
            # instance activated event will be published in artifact updated event handler
            self.__log.info(
                "Artifact repository found, waiting for artifact updated event to checkout artifacts: [repo_url] %s",
                repo_url)

        persistence_mapping_payload = Config.persistence_mappings
        if persistence_mapping_payload is not None:
            event_handler.volume_mount_extension(persistence_mapping_payload)

        # start log publishing thread
        log_publish_manager = None
        if DataPublisherConfiguration.get_instance().enabled:
            log_file_paths = Config.log_file_paths
            if log_file_paths is None:
                self.__log.exception("No valid log file paths found, no logs will be published")
            else:
                self.__log.debug("Starting Log Publisher Manager: [Log file paths] %s" % ", ".join(log_file_paths))
                log_publish_manager = LogPublisherManager(log_file_paths)
                log_publish_manager.start()

        # run until terminated
        while not self.__terminated:
            time.sleep(5)

        if DataPublisherConfiguration.get_instance().enabled:
            log_publish_manager.terminate_all_publishers()

    def terminate(self):
        """
        Allows the CartridgeAgent thread to be terminated

        :return: void
        """
        self.__terminated = True

    def register_instance_topic_listeners(self):
        self.__log.debug("Starting instance notifier event message receiver thread")

        self.__inst_topic_subscriber.register_handler("ArtifactUpdatedEvent", Handlers.on_artifact_updated)
        self.__inst_topic_subscriber.register_handler("InstanceCleanupMemberEvent", Handlers.on_instance_cleanup_member)
        self.__inst_topic_subscriber.register_handler(
            "InstanceCleanupClusterEvent", Handlers.on_instance_cleanup_cluster)

        self.__inst_topic_subscriber.start()
        self.__log.info("Instance notifier event message receiver thread started")

        # wait till subscribed to continue
        while not self.__inst_topic_subscriber.is_subscribed():
            time.sleep(1)

    def register_topology_event_listeners(self):
        self.__log.debug("Starting topology event message receiver thread")

        self.__topology_event_subscriber.register_handler("MemberActivatedEvent", Handlers.on_member_activated)
        self.__topology_event_subscriber.register_handler("MemberTerminatedEvent", Handlers.on_member_terminated)
        self.__topology_event_subscriber.register_handler("MemberSuspendedEvent", Handlers.on_member_suspended)
        self.__topology_event_subscriber.register_handler("CompleteTopologyEvent", Handlers.on_complete_topology)
        self.__topology_event_subscriber.register_handler("MemberStartedEvent", Handlers.on_member_started)
        self.__topology_event_subscriber.register_handler("MemberCreatedEvent", Handlers.on_member_created)
        self.__topology_event_subscriber.register_handler("MemberInitializedEvent", Handlers.on_member_initialized)

        self.__topology_event_subscriber.start()
        self.__log.info("Cartridge agent topology receiver thread started")

        # wait till subscribed to continue
        while not self.__topology_event_subscriber.is_subscribed():
            time.sleep(1)

    def register_tenant_event_listeners(self):
        self.__log.debug("Starting tenant event message receiver thread")
        self.__tenant_topic_subscriber.register_handler("DomainMappingAddedEvent",
                                                        Handlers.on_domain_mapping_added)
        self.__tenant_topic_subscriber.register_handler("DomainsMappingRemovedEvent",
                                                        Handlers.on_domain_mapping_removed)
        self.__tenant_topic_subscriber.register_handler("CompleteTenantEvent", Handlers.on_complete_tenant)
        self.__tenant_topic_subscriber.register_handler("TenantSubscribedEvent", Handlers.on_tenant_subscribed)

        self.__tenant_topic_subscriber.start()
        self.__log.info("Tenant event message receiver thread started")

        # wait till subscribed to continue
        while not self.__tenant_topic_subscriber.is_subscribed():
            time.sleep(1)

    def register_application_signup_event_listeners(self):
        self.__log.debug("Starting application signup event message receiver thread")
        self.__app_topic_subscriber.register_handler("ApplicationSignUpRemovedEvent",
                                                     Handlers.on_application_signup_removed)

        self.__app_topic_subscriber.start()
        self.__log.info("Application signup event message receiver thread started")

        # wait till subscribed to continue
        while not self.__app_topic_subscriber.is_subscribed():
            time.sleep(1)

    def wait_for_complete_topology(self):
        while not TopologyContext.topology.initialized:
            self.__log.info("Waiting for complete topology event...")
            time.sleep(5)
        self.__log.info("Complete topology event received")


class Handlers(object):
    """
    Handler methods for message broker events
    """

    __log = LogFactory().get_log(__name__)
    __tenant_context_initialized = False

    @staticmethod
    def on_artifact_updated(msg):
        event_obj = ArtifactUpdatedEvent.create_from_json(msg.payload)
        event_handler.on_artifact_updated_event(event_obj)

    @staticmethod
    def on_instance_cleanup_member(msg):
        member_in_payload = Config.member_id
        event_obj = InstanceCleanupMemberEvent.create_from_json(msg.payload)
        member_in_event = event_obj.member_id
        if member_in_payload == member_in_event:
            event_handler.on_instance_cleanup_member_event()

    @staticmethod
    def on_instance_cleanup_cluster(msg):
        event_obj = InstanceCleanupClusterEvent.create_from_json(msg.payload)
        cluster_in_payload = Config.cluster_id
        cluster_in_event = event_obj.cluster_id
        instance_in_payload = Config.cluster_instance_id
        instance_in_event = event_obj.cluster_instance_id

        if cluster_in_event == cluster_in_payload and instance_in_payload == instance_in_event:
            event_handler.on_instance_cleanup_cluster_event()

    @staticmethod
    def on_member_created(msg):
        Handlers.__log.debug("Member created event received: %r" % msg.payload)

    @staticmethod
    def on_member_initialized(msg):
        Handlers.__log.debug("Member initialized event received: %r" % msg.payload)
        event_obj = MemberInitializedEvent.create_from_json(msg.payload)

        if not TopologyContext.topology.initialized:
            return

        event_handler.on_member_initialized_event(event_obj)

    @staticmethod
    def on_member_activated(msg):
        Handlers.__log.debug("Member activated event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberActivatedEvent.create_from_json(msg.payload)
        event_handler.on_member_activated_event(event_obj)

    @staticmethod
    def on_member_terminated(msg):
        Handlers.__log.debug("Member terminated event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberTerminatedEvent.create_from_json(msg.payload)
        event_handler.on_member_terminated_event(event_obj)

    @staticmethod
    def on_member_suspended(msg):
        Handlers.__log.debug("Member suspended event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberSuspendedEvent.create_from_json(msg.payload)
        event_handler.on_member_suspended_event(event_obj)

    @staticmethod
    def on_complete_topology(msg):
        event_obj = CompleteTopologyEvent.create_from_json(msg.payload)
        TopologyContext.update(event_obj.topology)
        if not TopologyContext.topology.initialized:
            Handlers.__log.info("Topology initialized from complete topology event")
            TopologyContext.topology.initialized = True
            event_handler.on_complete_topology_event(event_obj)

        Handlers.__log.debug("Topology context updated with [topology] %r" % event_obj.topology.json_str)

    @staticmethod
    def on_member_started(msg):
        Handlers.__log.debug("Member started event received: %r" % msg.payload)
        if not TopologyContext.topology.initialized:
            return

        event_obj = MemberStartedEvent.create_from_json(msg.payload)
        event_handler.on_member_started_event(event_obj)

    @staticmethod
    def on_domain_mapping_added(msg):
        Handlers.__log.debug("Subscription domain added event received : %r" % msg.payload)
        event_obj = DomainMappingAddedEvent.create_from_json(msg.payload)
        event_handler.on_domain_mapping_added_event(event_obj)

    @staticmethod
    def on_domain_mapping_removed(msg):
        Handlers.__log.debug("Subscription domain removed event received : %r" % msg.payload)
        event_obj = DomainMappingRemovedEvent.create_from_json(msg.payload)
        event_handler.on_domain_mapping_removed_event(event_obj)

    @staticmethod
    def on_complete_tenant(msg):
        event_obj = CompleteTenantEvent.create_from_json(msg.payload)
        TenantContext.update(event_obj.tenants)
        if not Handlers.__tenant_context_initialized:
            Handlers.__log.info("Tenant context initialized from complete tenant event")
            Handlers.__tenant_context_initialized = True
            event_handler.on_complete_tenant_event(event_obj)

        Handlers.__log.debug("Tenant context updated with [tenant list] %r" % event_obj.tenant_list_json)

    @staticmethod
    def on_tenant_subscribed(msg):
        Handlers.__log.debug("Tenant subscribed event received: %r" % msg.payload)
        event_obj = TenantSubscribedEvent.create_from_json(msg.payload)
        event_handler.on_tenant_subscribed_event(event_obj)

    @staticmethod
    def on_application_signup_removed(msg):
        Handlers.__log.debug("Application signup removed event received: %r" % msg.payload)
        event_obj = ApplicationSignUpRemovedEvent.create_from_json(msg.payload)
        event_handler.on_application_signup_removed_event(event_obj)


if __name__ == "__main__":
    log = LogFactory().get_log(__name__)
    try:
        log.info("Starting Stratos cartridge agent...")
        cartridge_agent = CartridgeAgent()
        cartridge_agent.run_agent()
    except Exception as e:
        log.exception("Cartridge Agent Exception: %r" % e)
        # cartridge_agent.terminate()
