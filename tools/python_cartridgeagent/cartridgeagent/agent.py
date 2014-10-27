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
import sys

from modules.exception.parameternotfoundexception import ParameterNotFoundException
from modules.subscriber import eventsubscriber
from modules.publisher import cartridgeagentpublisher
from modules.event.instance.notifier.events import *
from modules.event.tenant.events import *
from modules.event.topology.events import *
from modules.tenant.tenantcontext import *
from modules.topology.topologycontext import *
from modules.datapublisher.logpublisher import *
from modules.config import cartridgeagentconfiguration
from modules.extensions import defaultextensionhandler


class CartridgeAgent(threading.Thread):
    extension_handler = defaultextensionhandler.DefaultExtensionHandler()

    def __init__(self):
        threading.Thread.__init__(self)

        mb_ip = cartridgeagentconfiguration.CartridgeAgentConfiguration().read_property(cartridgeagentconstants.MB_IP)
        mb_port = cartridgeagentconfiguration.CartridgeAgentConfiguration().read_property(cartridgeagentconstants.MB_PORT)

        self.__instance_event_subscriber = eventsubscriber.EventSubscriber(
            cartridgeagentconstants.INSTANCE_NOTIFIER_TOPIC,
            mb_ip,
            mb_port)
        self.__tenant_event_subscriber = eventsubscriber.EventSubscriber(
            cartridgeagentconstants.TENANT_TOPIC,
            mb_ip,
            mb_port)
        self.__topology_event_subscriber = eventsubscriber.EventSubscriber(
            cartridgeagentconstants.TOPOLOGY_TOPIC,
            mb_ip,
            mb_port)

        self.__tenant_context_initialized = False

        self.log_publish_manager = None

        self.terminated = False

        self.log = LogFactory().get_log(__name__)

        self.cartridge_agent_config = CartridgeAgentConfiguration()

    def run(self):
        self.log.info("Starting Cartridge Agent...")

        #Check if required prpoerties are set
        self.validate_required_properties()

        #Start instance notifier listener thread
        self.subscribe_to_topics_and_register_listeners()

        #Start topology event receiver thread
        self.register_topology_event_listeners()

        #Start tenant event receiver thread
        self.register_tenant_event_listeners()

        #wait for intance spawned event
        while not self.cartridge_agent_config.initialized:
            self.log.debug("Waiting for Cartridge Agent to be initialized...")
            time.sleep(1)

        #Execute instance started shell script
        CartridgeAgent.extension_handler.on_instance_started_event()

        #Publish instance started event
        cartridgeagentpublisher.publish_instance_started_event()

        #Execute start servers extension
        try:
            CartridgeAgent.extension_handler.start_server_extension()
        except:
            self.log.exception("Error processing start servers event")

        #Wait for all ports to be active
        cartridgeagentutils.wait_until_ports_active(
            self.cartridge_agent_config.listen_address,
            self.cartridge_agent_config.ports,
            int(self.cartridge_agent_config.read_property("port.check.timeout", critical=False))
        )

        # check if artifact management is required before publishing instance activated event
        repo_url = self.cartridge_agent_config.repo_url
        if repo_url is None or str(repo_url).strip() == "":
            self.log.info("No artifact repository found")
            CartridgeAgent.extension_handler.on_instance_activated_event()

            cartridgeagentpublisher.publish_instance_activated_event()

        persistence_mappping_payload = self.cartridge_agent_config.persistence_mappings
        if persistence_mappping_payload is not None:
            CartridgeAgent.extension_handler.volume_mount_extension(persistence_mappping_payload)

        # start log publishing thread
        if DataPublisherConfiguration.get_instance().enabled:
            log_file_paths = self.cartridge_agent_config.log_file_paths
            if log_file_paths is None:
                self.log.exception("No valid log file paths found, no logs will be published")
            else:
                self.log_publish_manager = LogPublisherManager(log_file_paths)
                self.log_publish_manager.start()

        while not self.terminated:
            time.sleep(1)

        if DataPublisherConfiguration.get_instance().enabled:
            self.log_publish_manager.terminate_all_publishers()

    def terminate(self):
        """
        Allows the CartridgeAgent thread to be terminated

        :return: void
        """
        self.terminated = True

    def validate_required_properties(self):
        """
        Checks if required properties are set
        :return: void
        """
        #PARAM_FILE_PATH
        try:
            self.cartridge_agent_config.read_property(cartridgeagentconstants.PARAM_FILE_PATH)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.PARAM_FILE_PATH)
            return

        #EXTENSIONS_DIR
        try:
            self.cartridge_agent_config.read_property(cartridgeagentconstants.EXTENSIONS_DIR)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.EXTENSIONS_DIR)
            return
        
    def subscribe_to_topics_and_register_listeners(self):
        self.log.debug("Starting instance notifier event message receiver thread")

        self.__instance_event_subscriber.register_handler("ArtifactUpdatedEvent", self.on_artifact_updated)
        self.__instance_event_subscriber.register_handler("InstanceCleanupMemberEvent", self.on_instance_cleanup_member)
        self.__instance_event_subscriber.register_handler("InstanceCleanupClusterEvent", self.on_instance_cleanup_cluster)

        self.__instance_event_subscriber.start()
        self.log.info("Instance notifier event message receiver thread started")

        # wait till subscribed to continue
        while not self.__instance_event_subscriber.is_subscribed():
            time.sleep(2)

    def on_artifact_updated(self, msg):
        event_obj = ArtifactUpdatedEvent.create_from_json(msg.payload)
        CartridgeAgent.extension_handler.on_artifact_updated_event(event_obj)

    def on_instance_cleanup_member(self, msg):
        member_in_payload = self.cartridge_agent_config.member_id
        event_obj = InstanceCleanupMemberEvent.create_from_json(msg.payload)
        member_in_event = event_obj.member_id
        if member_in_payload == member_in_event:
            CartridgeAgent.extension_handler.on_instance_cleanup_member_event(event_obj)

    def on_instance_cleanup_cluster(self, msg):
        event_obj = InstanceCleanupClusterEvent.create_from_json(msg.payload)
        cluster_in_payload = self.cartridge_agent_config.cluster_id
        cluster_in_event = event_obj.cluster_id

        if cluster_in_event == cluster_in_payload:
            CartridgeAgent.extension_handler.on_instance_cleanup_cluster_event(event_obj)

    def register_topology_event_listeners(self):
        self.log.debug("Starting topology event message receiver thread")

        self.__topology_event_subscriber.register_handler("MemberActivatedEvent", self.on_member_activated)
        self.__topology_event_subscriber.register_handler("MemberTerminatedEvent", self.on_member_terminated)
        self.__topology_event_subscriber.register_handler("MemberSuspendedEvent", self.on_member_suspended)
        self.__topology_event_subscriber.register_handler("CompleteTopologyEvent", self.on_complete_topology)
        self.__topology_event_subscriber.register_handler("MemberStartedEvent", self.on_member_started)
        self.__topology_event_subscriber.register_handler("InstanceSpawnedEvent", self.on_instance_spawned)

        self.__topology_event_subscriber.start()
        self.log.info("Cartridge Agent topology receiver thread started")

    def on_instance_spawned(self, msg):
        self.log.debug("Instance spawned event received: %r" % msg.payload)
        if self.cartridge_agent_config.initialized:
            return

        event_obj = InstanceSpawnedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_instance_spawned_event(event_obj)
        except:
            self.log.exception("Error processing instance spawned event")

    def on_member_activated(self, msg):
        self.log.debug("Member activated event received: %r" % msg.payload)
        if not self.cartridge_agent_config.initialized:
            return

        event_obj = MemberActivatedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_member_activated_event(event_obj)
        except:
            self.log.exception("Error processing member activated event")

    def on_member_terminated(self, msg):
        self.log.debug("Member terminated event received: %r" % msg.payload)
        if not self.cartridge_agent_config.initialized:
            return

        event_obj = MemberTerminatedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_member_terminated_event(event_obj)
        except:
            self.log.exception("Error processing member terminated event")

    def on_member_suspended(self, msg):
        self.log.debug("Member suspended event received: %r" % msg.payload)
        if not self.cartridge_agent_config.initialized:
            return

        event_obj = MemberSuspendedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_member_suspended_event(event_obj)
        except:
            self.log.exception("Error processing member suspended event")

    def on_complete_topology(self, msg):
        if not self.cartridge_agent_config.initialized:
            self.log.debug("Complete topology event received")
            event_obj = CompleteTopologyEvent.create_from_json(msg.payload)
            TopologyContext.update(event_obj.topology)
            try:
                CartridgeAgent.extension_handler.on_complete_topology_event(event_obj)
            except:
                self.log.exception("Error processing complete topology event")
        else:
            self.log.info("Complete topology event updating task disabled")

    def on_member_started(self, msg):
        self.log.debug("Member started event received: %r" % msg.payload)
        if not self.cartridge_agent_config.initialized:
            return

        event_obj = MemberStartedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_member_started_event(event_obj)
        except:
            self.log.exception("Error processing member started event")

    def register_tenant_event_listeners(self):
        self.log.debug("Starting tenant event message receiver thread")
        self.__tenant_event_subscriber.register_handler("SubscriptionDomainAddedEvent", self.on_subscription_domain_added)
        self.__tenant_event_subscriber.register_handler("SubscriptionDomainsRemovedEvent", self.on_subscription_domain_removed)
        self.__tenant_event_subscriber.register_handler("CompleteTenantEvent", self.on_complete_tenant)
        self.__tenant_event_subscriber.register_handler("TenantSubscribedEvent", self.on_tenant_subscribed)
        self.__tenant_event_subscriber.register_handler("TenantUnSubscribedEvent", self.on_tenant_unsubscribed)

        self.__tenant_event_subscriber.start()
        self.log.info("Tenant event message receiver thread started")

    def on_subscription_domain_added(self, msg):
        self.log.debug("Subscription domain added event received : %r" % msg.payload)
        event_obj = SubscriptionDomainAddedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_subscription_domain_added_event(event_obj)
        except:
            self.log.exception("Error processing subscription domains added event")

    def on_subscription_domain_removed(self, msg):
        self.log.debug("Subscription domain removed event received : %r" % msg.payload)
        event_obj = SubscriptionDomainRemovedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_subscription_domain_removed_event(event_obj)
        except:
            self.log.exception("Error processing subscription domains removed event")

    def on_complete_tenant(self, msg):
        if not self.__tenant_context_initialized:
            self.log.debug("Complete tenant event received")
            event_obj = CompleteTenantEvent.create_from_json(msg.payload)
            TenantContext.update(event_obj.tenants)

            try:
                CartridgeAgent.extension_handler.on_complete_tenant_event(event_obj)
                self.__tenant_context_initialized = True
            except:
                self.log.exception("Error processing complete tenant event")
        else:
            self.log.info("Complete tenant event updating task disabled")

    def on_tenant_subscribed(self, msg):
        self.log.debug("Tenant subscribed event received: %r" % msg.payload)
        event_obj = TenantSubscribedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_tenant_subscribed_event(event_obj)
        except:
            self.log.exception("Error processing tenant subscribed event")

    def on_tenant_unsubscribed(self, msg):
        self.log.debug("Tenant unSubscribed event received: %r" % msg.payload)
        event_obj = TenantUnsubscribedEvent.create_from_json(msg.payload)
        try:
            CartridgeAgent.extension_handler.on_tenant_unsubscribed_event(event_obj)
        except:
            self.log.exception("Error processing tenant unSubscribed event")


def uncaught_exception_mg(exctype, value, tb):
    log = LogFactory().get_log(__name__)
    log.exception("UNCAUGHT EXCEPTION:", value)

def main():
    sys.excepthook = uncaught_exception_mg
    cartridge_agent = CartridgeAgent()
    log = LogFactory().get_log(__name__)

    try:
        log.debug("Starting cartridge agent")
        cartridge_agent.start()
    except:
        log.exception("Cartridge Agent Exception")
        cartridge_agent.terminate()


if __name__ == "__main__":
    main()
