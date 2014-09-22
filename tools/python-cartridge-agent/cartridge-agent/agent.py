#!/usr/bin/env python
import logging
import threading
import time

from config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from util import *
from exception.parameternotfoundexception import ParameterNotFoundException
from subscriber.eventsubscriber import EventSubscriber
from extensions.defaultextensionhandler import DefaultExtensionHandler
from publisher import cartridgeagentpublisher
from event.instance.notifier.events import *
from event.tenant.events import *


class CartridgeAgent(threading.Thread):
    logging.basicConfig(level=logging.DEBUG)
    log = logging.getLogger(__name__)

    cart_config = CartridgeAgentConfiguration()

    def __init__(self):
        threading.Thread.__init__(self)
        self.__instance_event_subscriber = EventSubscriber(cartridgeagentconstants.INSTANCE_NOTIFIER_TOPIC)
        self.__tenant_event_subscriber = EventSubscriber(cartridgeagentconstants.TENANT_TOPIC)
        self.__topology_event_subscriber = EventSubscriber(cartridgeagentconstants.TOPOLOGY_TOPIC)

        self.extension_handler = DefaultExtensionHandler()

        self.__complete_tenant_initialized = False

    def run(self):
        self.log.info("Starting Cartridge Agent...")

        self.validate_required_properties()

        self.subscribe_to_topics_and_register_listeners()

        self.register_topology_event_listeners()

        self.register_tenant_event_listeners()

        self.extension_handler.on_instance_started_event()

        cartridgeagentpublisher.publish_instance_started_event()

        try:
            self.extension_handler.start_server_extension()
        except:
            self.log.exception("Error processing start servers event")

        cartridgeagentutils.wait_until_ports_active(
            self.cart_config.get_listen_address(),
            self.cart_config.get_ports()
        )

        repo_url = self.cart_config.get_repo_url()

        if repo_url is None or str(repo_url).strip() == "":
            self.log.info("No artifact repository found")
            self.extension_handler.on_instance_activated_event()

            cartridgeagentpublisher.publish_instance_activated_event()
        else:
            pass

        persistence_mappping_payload = self.cart_config.get_persistance_mappings()
        if persistence_mappping_payload is not None:
            self.extension_handler.volume_mount_extension(persistence_mappping_payload)

            # TODO: logpublisher shceduled event

            #TODO: wait until terminated is true

    def validate_required_properties(self):
        # JNDI_PROPERTIES_DIR
        try:
            self.cart_config.read_property(cartridgeagentconstants.JNDI_PROPERTIES_DIR)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.JNDI_PROPERTIES_DIR)

        #PARAM_FILE_PATH
        try:
            self.cart_config.read_property(cartridgeagentconstants.PARAM_FILE_PATH)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.PARAM_FILE_PATH)

        #EXTENSIONS_DIR
        try:
            self.cart_config.read_property(cartridgeagentconstants.EXTENSIONS_DIR)
        except ParameterNotFoundException:
            self.log.error("System property not found: %r" % cartridgeagentconstants.EXTENSIONS_DIR)

    def subscribe_to_topics_and_register_listeners(self):
        self.log.debug("Starting instance notifier event message receiver thread")

        self.__instance_event_subscriber.register_handler("ArtifactUpdatedEvent", self.on_artifact_updated)
        self.__instance_event_subscriber.register_handler("InstanceCleanupMemberEvent", self.on_instance_cleanup_member)
        self.__instance_event_subscriber.register_handler("InstanceCleanupClusterEvent",
                                                          self.on_instance_cleanup_cluster)
        self.__instance_event_subscriber.start()
        self.log.info("Instance notifier event message receiver thread started")

        # wait till subscribed to continue
        while not self.__instance_event_subscriber.is_subscribed():
            time.sleep(2)

    def on_artifact_updated(self, msg):
        event_obj = ArtifactUpdatedEvent.create_from_json(msg.payload)
        self.extension_handler.on_artifact_updated_event(event_obj)

    def on_instance_cleanup_member(self, msg):
        member_in_payload = self.cart_config.get_member_id()
        event_obj = InstanceCleanupMemberEvent.create_from_json(msg.payload)
        member_in_event = event_obj.member_id
        if member_in_payload == member_in_event:
            self.extension_handler.onInstanceCleanupMemberEvent(event_obj)

    def on_instance_cleanup_cluster(self, msg):
        event_obj = InstanceCleanupClusterEvent.create_from_json(msg.payload)
        cluster_in_payload = self.cart_config.get_cluster_id()
        cluster_in_event = event_obj.cluster_id

        if cluster_in_event == cluster_in_payload:
            self.extension_handler.on_instance_cleanup_cluster_event(event_obj)

    def register_topology_event_listeners(self):
        self.log.debug("Starting topology event message receiver thread")

        self.__topology_event_subscriber.register_handler("MemberActivatedEvent", self.on_member_activated)
        self.__topology_event_subscriber.register_handler("MemberTerminatedEvent", self.on_member_terminated)
        self.__topology_event_subscriber.register_handler("MemberSuspendedEvent", self.on_member_suspended)
        self.__topology_event_subscriber.register_handler("CompleteTopologyEvent", self.on_complete_topology)
        self.__topology_event_subscriber.register_handler("MemberStartedEvent", self.on_member_started)

        self.__topology_event_subscriber.start()
        self.log.info("Cartridge Agent topology receiver thread started")

    def on_member_activated(self, msg):
        raise NotImplementedError

    def on_member_terminated(self, msg):
        raise NotImplementedError

    def on_member_suspended(self, msg):
        raise NotImplementedError

    def on_complete_topology(self, msg):
        raise NotImplementedError

    def on_member_started(self, msg):
        raise NotImplementedError

    def register_tenant_event_listeners(self):
        self.log.debug("Starting tenant event message receiver thread")
        self.__tenant_event_subscriber.register_handler("SubscriptionDomainAddedEvent",self.on_subscription_domain_added)
        self.__tenant_event_subscriber.register_handler("SubscriptionDomainsRemovedEvent",self.on_subscription_domain_removed)
        self.__tenant_event_subscriber.register_handler("CompleteTenantEvent", self.on_complete_tenant)
        self.__tenant_event_subscriber.register_handler("TenantSubscribedEvent", self.on_tenant_subscribed)
        self.__tenant_event_subscriber.register_handler("TenantUnSubscribedEvent", self.on_tenant_unsubscribed)

        self.__tenant_event_subscriber.start()
        self.log.info("Tenant event message receiver thread started")

    def on_subscription_domain_added(self, msg):
        self.log.debug("Subscription domain added event received")
        event_obj = SubscriptionDomainAddedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onSubscriptionDomainAddedEvent(event_obj)
        except:
            self.log.exception("Error processing subscription domains added event")
        # extensionutils.execute_subscription_domain_added_extension(
        #     event_obj.tenant_id,
        #     self.find_tenant_domain(event_obj.tenant_id),
        #     event_obj.domain_name,
        #     event_obj.application_context
        # )

    def on_subscription_domain_removed(self, msg):
        self.log.debug("Subscription domain removed event received")
        event_obj = SubscriptionDomainRemovedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onSubscriptionDomainRemovedEvent(event_obj)
        except:
            self.log.exception("Error processing subscription domains removed event")
        # extensionutils.execute_subscription_domain_removed_extension(
        #     event_obj.tenant_id,
        #     self.find_tenant_domain(event_obj.tenant_id),
        #     event_obj.domain_name
        # )

    def on_complete_tenant(self, msg):
        if not self.__complete_tenant_initialized:
            self.log.debug("Complete tenant event received")
            event_obj = CompleteTenantEvent.create_from_json(msg.payload)

            try:
                self.extension_handler.onCompleteTenantEvent(event_obj)
                self.__complete_tenant_initialized = True
            except:
                self.log.exception("Error processing complete tenant event")
        else:
            self.log.info("Complete tenant event updating task disabled")

    def on_tenant_subscribed(self, msg):
        self.log.debug("Tenant subscribed event received")
        event_obj = TenantSubscribedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onTenantSubscribedEvent(event_obj)
        except:
            self.log.exception("Error processing tenant subscribed event")

    def on_tenant_unsubscribed(self, msg):
        self.log.debug("Tenant unSubscribed event received")
        event_obj = TenantUnsubscribedEvent.create_from_json(msg.payload)
        try:
            self.extension_handler.onTenantUnSubscribedEvent(event_obj)
        except:
            self.log.exception("Error processing tenant unSubscribed event")

    def find_tenant_domain(self, tenant_id):
        # TODO: call to REST Api and get tenant information
        raise NotImplementedError


def main():
    cartridge_agent = CartridgeAgent()
    cartridge_agent.start()


if __name__ == "__main__":
    main()
# ========================================================
#
#
# def runningSuspendScript():
#     print "inside thread"
#     os.system('./script.sh')
#
#
# def MyThread2():
#     pass
#
#
# def listeningTopology():
#     class MyListener(stomp.ConnectionListener):
#         def on_error(self, headers, message):
#             print('received an error %s' % message)
#
#         def on_message(self, headers, message):
#             # print('received message\n %s'% message)
#             for k, v in headers.iteritems():
#                 print('header: key %s , value %s' % (k, v))
#
#                 if k == 'event-class-name':
#                     print('event class name found')
#                     if v == 'org.apache.stratos.messaging.event.topology.CompleteTopologyEvent':
#                         print('CompleteTopologyEvent triggered')
#                         print('received message\n %s' % message)
#                     if v == 'org.apache.stratos.messaging.event.topology.MemberTerminatedEvent':
#                         print('MemberTerminatedEvent triggered')
#                     if v == 'org.apache.stratos.messaging.event.topology.ServiceCreatedEvent':
#                         print('MemberTerminatedEvent triggered')
#                     if v == 'org.apache.stratos.messaging.event.topology.InstanceSpawnedEvent':
#                         print('MemberTerminatedEvent triggered')
#                         print('received message\n %s' % message)
#                     if v == 'org.apache.stratos.messaging.event.topology.ClusterCreatedEvent':
#                         print('MemberTerminatedEvent triggered')
#                     if v == 'org.apache.stratos.messaging.event.topology.InstanceSpawnedEvent':
#                         print('MemberTerminatedEvent triggered')
#                     else:
#                         print('something else')
#
#
#     dest = '/topic/topology'
#     conn = stomp.Connection([('localhost', 61613)])
#     print('set up Connection')
#     conn.set_listener('somename', MyListener())
#     print('Set up listener')
#
#     conn.start()
#     print('started connection')
#
#     conn.connect(wait=True)
#     print('connected')
#     conn.subscribe(destination=dest, ack='auto')
#     print('subscribed')
#
#
# def listeningInstanceNotifier():
#     instance_topic_client.on_connect = instance_notifier_connect
#     instance_topic_client.on_message = instance_notifier_message
#
#     # mb_client.connect(readProperty("mb.ip"), properties.get("agent", "mb.port"), 60)
#     instance_topic_client.connect("127.0.0.1", 1883, 60)
#     instance_topic_client.loop_forever()
#
#
# def instance_notifier_connect(client, userdata, flags, rc):
#     print "Connected! Subscribing to instance/# topics"
#     instance_topic_client.subscribe("instance/#")
#
#
# def artifact_updated(msg):
#     extensionhandler.onArtifactUpdatedEvent(extensionsDir, 'artifacts-updated.sh')
#
#
# def instance_cleanup_member(msg):
#     # if sd['MEMBER_ID'] == member_id_from_event:
#     extensionhandler.onInstanceCleanupMemberEvent(extensionsDir, 'clean.sh')
#
#
# def instance_cleanup_cluster(msg):
#     # if cluster_id == cluster_id_from_event:
#     extensionhandler.onInstanceCleanupMemberEvent(extensionsDir, 'clean.sh')
#
#
# def instance_notifier_message(client, userdata, msg):
#     print "Topic: %r\nContent:%r" % (msg.topic, msg.payload)
#     event = msg.topic.rpartition('/')[2]
#     print "Event: %r" % event
#     if event == "ArtifactUpdatedEvent":
#         # TODO: event details to be passed to the script
#         print "ArtifactUpdatedEvent received"
#         artifact_updated(msg)
#     elif event == "InstanceCleanupMemberEvent":
#         print "InstanceCleanupMemberEvent received"
#         # TODO: event details to be passed to the script
#         instance_cleanup_member(msg)
#     elif event == "InstanceCleanupClusterEvent":
#         print "InstanceCleanupClusterEvent received"
#         # TODO: event details to be passed to the script
#         instance_cleanup_cluster(msg)
#     else:
#         print "Unidentified event: %r" % event
#
#
# def publishInstanceStartedEvent():
#     instance_started_event = InstanceStartedEvent(service_name, cluster_id, sd['NETWORK_PARTITION_ID'],
#                                                   sd['PARTITION_ID'], sd['MEMBER_ID'])
#     msgs = [{'topic': "instance/status/InstanceStartedEvent", 'payload': instance_started_event.to_JSON()}]
#     #publish.single("instance", instance_started_event.to_JSON(), hostname="localhost", port=1883)
#     publish.multiple(msgs, "localhost", 1883)
#
#
# def checkPortsActive():
#     sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#     result = sock.connect_ex(('127.0.0.1', 80))
#     if result == 0:
#         print "Port is open"
#     else:
#         print "Port is not open"
#
#
# class InstanceStartedEvent:
#     serviceName = ''
#
#     def __init__(self, serviceName, clusterId, networkPartitionId, partitionId, memberId):
#         self.serviceName = serviceName
#         self.clusterId = clusterId
#         self.networkPartitionId = networkPartitionId
#         self.partitionId = partitionId
#         self.memberId = memberId
#
#     def to_JSON(self):
#         return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)
#
#
# def onInstanceStartedEvent():
#     print('on instance start up event')
#     event = InstanceStartedEvent(service_name, cluster_id, '', '', tenant_id)
#     print(event.to_JSON())
#
#
# def onArtifactUpdatedEvent():
#     print('on srtifcats update event')
#
# # Parse properties file
# properties = ConfigParser.SafeConfigParser()
# properties.read('agent.properties')
#
# # TODO: check from properties file
# util.validateRequiredSystemProperties()
#
# payloadPath = sys.argv[1]
# extensionsDir = sys.argv[2]
# extensionhandler.onArtifactUpdatedEvent(extensionsDir, 'artifacts-updated.sh')
#
# fo = open(payloadPath, "r+")
# str = fo.read(1000);
#
# print "Read String is : ", str
#
# sd = dict(u.split("=") for u in str.split(","))
#
# print [i for i in sd.keys()]
#
# print "HOST_NAME   ", sd['HOST_NAME']
#
# hostname = sd['HOST_NAME']
# service_name = sd['SERVICE_NAME']
# multitenant = sd['MULTITENANT']
# tenant_id = sd['TENANT_ID']
# tenantrange = sd['TENANT_RANGE']
# cartridealies = sd['CARTRIDGE_ALIAS']
# cluster_id = sd['CLUSTER_ID']
# cartridge_key = sd['CARTRIDGE_KEY']
# deployement = sd['DEPLOYMENT']
# repourl = sd['REPO_URL']
# ports = sd['PORTS']
# puppetip = sd['PUPPET_IP']
# puppethostname = sd['PUPPET_HOSTNAME']
# puppetenv = sd['PUPPET_ENV']
# persistance_mapping = sd['PERSISTENCE_MAPPING'] if 'PERSISTENCE_MAPPING' in sd else None
#
# if 'COMMIT_ENABLED' in sd:
#     commitenabled = sd['COMMIT_ENABLED']
#
# if 'DB_HOST' in sd:
#     dbhost = sd['DB_HOST']
#
# if multitenant == "true":
#     app_path = sd['APP_PATH']
# else:
#     app_path = ""
#
# instance_topic_client = mqtt.Client()
#
# env_params = {}
# env_params['STRATOS_APP_PATH'] = app_path
# env_params['STRATOS_PARAM_FILE_PATH'] = readProperty("param.file.path")
# env_params['STRATOS_SERVICE_NAME'] = service_name
# env_params['STRATOS_TENANT_ID'] = tenant_id
# env_params['STRATOS_CARTRIDGE_KEY'] = cartridge_key
# env_params['STRATOS_LB_CLUSTER_ID'] = sd['LB_CLUSTER_ID']
# env_params['STRATOS_CLUSTER_ID'] = cluster_id
# env_params['STRATOS_NETWORK_PARTITION_ID'] = sd['NETWORK_PARTITION_ID']
# env_params['STRATOS_PARTITION_ID'] = sd['PARTITION_ID']
# env_params['STRATOS_PERSISTENCE_MAPPINGS'] = persistance_mapping
# env_params['STRATOS_REPO_URL'] = sd['REPO_URL']
# # envParams['STRATOS_LB_IP']=
# # envParams['STRATOS_LB_PUBLIC_IP']=
# # envParams['']=
# # envParams['']=
# # envParams['']=
# # envParams['']=
#
# extensionhandler.onInstanceStartedEvent(extensionsDir, 'instance-started.sh', multitenant, 'artifacts-copy.sh.erb',
#                                         app_path, env_params)
#
# t1 = threading.Thread(target=runningSuspendScript, args=[])
#
# t1.start()
#
# t2 = threading.Thread(target=listeningInstanceNotifier, args=[])
#
# t2.start()
#
# t3 = threading.Thread(target=listeningTopology, args=[])
#
# t3.start()
#
# onInstanceStartedEvent()
#
# checkPortsActive()
#
# publishInstanceStartedEvent()
#
# extensionhandler.startServerExtension()
#
#
# def git(*args):
#     return subprocess.check_call(['git'] + list(args))
#
# # examples
# git("status")
# git("clone", "git://git.xyz.com/platform/manifest.git", "-b", "jb_2.5")
#
#
#
#
#
