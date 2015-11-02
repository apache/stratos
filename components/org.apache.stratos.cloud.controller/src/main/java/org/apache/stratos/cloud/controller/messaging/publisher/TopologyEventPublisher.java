/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.stratos.cloud.controller.messaging.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.PortMapping;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * this is to send the relevant events from cloud controller to topology topic
 */
public class TopologyEventPublisher {
    private static final Log log = LogFactory.getLog(TopologyEventPublisher.class);

    public static void sendServiceCreateEvent(List<Cartridge> cartridgeList) {
        ServiceCreatedEvent serviceCreatedEvent;
        for (Cartridge cartridge : cartridgeList) {
            serviceCreatedEvent = new ServiceCreatedEvent(cartridge.getType(),
                    (cartridge.isMultiTenant() ? ServiceType.MultiTenant
                            : ServiceType.SingleTenant));

            // Add ports to the event
            if (cartridge.getPortMappings() != null) {
                Port port;
                List<PortMapping> portMappings = Arrays.asList(cartridge.getPortMappings());
                for (PortMapping portMapping : portMappings) {
                    port = new Port(portMapping.getProtocol(), portMapping.getPort(), portMapping.getProxyPort());
                    serviceCreatedEvent.addPort(port);
                }
            }

            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Publishing service created event: [service-name] %s",
                        cartridge.getType()));
            }
            publishEvent(serviceCreatedEvent);
        }
    }

    public static void sendServiceRemovedEvent(List<Cartridge> cartridgeList) {
        ServiceRemovedEvent serviceRemovedEvent;
        for (Cartridge cartridge : cartridgeList) {
            serviceRemovedEvent = new ServiceRemovedEvent(cartridge.getType());
            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Publishing service removed event: [service-name] %s",
                        serviceRemovedEvent.getServiceName()));
            }
            publishEvent(serviceRemovedEvent);
        }
    }

    public static void sendClusterResetEvent(String appId, String serviceName, String clusterId,
                                             String instanceId) {
        ClusterResetEvent clusterResetEvent = new ClusterResetEvent(appId, serviceName,
                clusterId, instanceId);

        if (log.isInfoEnabled()) {
            log.info("Publishing cluster reset event: [cluster-id] " + clusterId);
        }
        publishEvent(clusterResetEvent);
    }

    public static void sendClusterCreatedEvent(Cluster cluster) {
        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(cluster);

        if (log.isInfoEnabled()) {
            log.info("Publishing cluster created event: [cluster-id] " + cluster.getClusterId());
        }
        publishEvent(clusterCreatedEvent);
    }

    public static void sendApplicationClustersCreated(String appId, List<Cluster> clusters) {

        if (log.isInfoEnabled()) {
            log.info("Publishing application clusters created event: [application-id] " + appId);
        }

        publishEvent(new ApplicationClustersCreatedEvent(clusters, appId));
    }

    public static void sendApplicationClustersRemoved(String appId, Set<ClusterDataHolder> clusters) {

        if (log.isInfoEnabled()) {
            log.info("Publishing application clusters removed event: [application-id] " + appId);
        }

        publishEvent(new ApplicationClustersRemovedEvent(clusters, appId));
    }

    public static void sendClusterRemovedEvent(ClusterContext ctxt, String deploymentPolicy) {
        ClusterRemovedEvent clusterRemovedEvent = new ClusterRemovedEvent(
                ctxt.getCartridgeType(), ctxt.getClusterId(), deploymentPolicy, ctxt.isLbCluster());
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing cluster removed event: [service-name] %s [cluster-id] %s",
                            ctxt.getCartridgeType(), ctxt.getClusterId()));
        }
        publishEvent(clusterRemovedEvent);

    }

    public static void sendMemberCreatedEvent(MemberContext memberContext) {

        MemberCreatedEvent memberCreatedEvent = new MemberCreatedEvent(
                memberContext.getCartridgeType(),
                memberContext.getClusterId(),
                memberContext.getClusterInstanceId(),
                memberContext.getMemberId(),
                memberContext.getNetworkPartitionId(),
                memberContext.getPartition().getId(),
                memberContext.getLoadBalancingIPType(),
                memberContext.getInitTime());

        memberCreatedEvent.setProperties(CloudControllerUtil
                .toJavaUtilProperties(memberContext.getProperties()));

        log.info(String.format("Publishing member created event: [service-name] %s [cluster-id] %s " +
                        "[cluster-instance-id] %s [member-id] %s [instance-id] %s [network-partition-id] %s " +
                        "[partition-id] %s [lb-cluster-id] %s",
                memberContext.getCartridgeType(), memberContext.getClusterId(), memberContext.getClusterInstanceId(),
                memberContext.getMemberId(), memberContext.getClusterInstanceId(), memberContext.getNetworkPartitionId(),
                memberContext.getPartition().getId(), memberContext.getLbClusterId()));
        publishEvent(memberCreatedEvent);
    }


    public static void sendMemberInitializedEvent(MemberContext memberContext) {

        MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(
                memberContext.getCartridgeType(),
                memberContext.getClusterId(),
                memberContext.getClusterInstanceId(),
                memberContext.getMemberId(),
                memberContext.getNetworkPartitionId(),
                memberContext.getPartition().getId());

        memberInitializedEvent.setDefaultPrivateIP(memberContext.getDefaultPrivateIP());
        if (memberContext.getPrivateIPs() != null) {
            memberInitializedEvent.setMemberPrivateIPs(Arrays.asList(memberContext.getPrivateIPs()));
        }
        memberInitializedEvent.setDefaultPublicIP(memberContext.getDefaultPublicIP());
        if (memberContext.getPublicIPs() != null) {
            memberInitializedEvent.setMemberPublicIPs(Arrays.asList(memberContext.getPublicIPs()));
        }
        memberInitializedEvent.setProperties(CloudControllerUtil
                .toJavaUtilProperties(memberContext.getProperties()));

        log.info(String.format("Publishing member initialized event: [service-name] %s [cluster-id] %s " +
                        "[cluster-instance-id] %s [member-id] %s [instance-id] %s [network-partition-id] %s " +
                        "[partition-id] %s [lb-cluster-id] %s",
                memberContext.getCartridgeType(), memberContext.getClusterId(), memberContext.getClusterInstanceId(),
                memberContext.getMemberId(), memberContext.getInstanceId(), memberContext.getNetworkPartitionId(),
                memberContext.getPartition().getId(), memberContext.getLbClusterId()));
        publishEvent(memberInitializedEvent);
    }

    public static void sendMemberStartedEvent(InstanceStartedEvent instanceStartedEvent) {
        MemberStartedEvent memberStartedEventTopology = new MemberStartedEvent(instanceStartedEvent.getServiceName(),
                instanceStartedEvent.getClusterId(), instanceStartedEvent.getClusterInstanceId(),
                instanceStartedEvent.getMemberId(), instanceStartedEvent.getNetworkPartitionId(), instanceStartedEvent.getPartitionId()
        );
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing member started event: [service-name] %s [cluster-id] %s [cluster-instance-id] %s " +
                                    "[member-id] %s [network-partition-id] %s [partition-id] %s",
                            instanceStartedEvent.getServiceName(),
                            instanceStartedEvent.getClusterId(),
                            instanceStartedEvent.getClusterInstanceId(),
                            instanceStartedEvent.getMemberId(),
                            instanceStartedEvent.getNetworkPartitionId(),
                            instanceStartedEvent.getPartitionId()));
        }
        publishEvent(memberStartedEventTopology);
    }

    public static void sendMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing member activated event: [service-name] %s [cluster-id] %s [cluster-instance-id] %s " +
                                    "[member-id] %s [network-partition-id] %s [partition-id] %s",
                            memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId(),
                            memberActivatedEvent.getClusterInstanceId(),
                            memberActivatedEvent.getMemberId(),
                            memberActivatedEvent.getNetworkPartitionId(),
                            memberActivatedEvent.getPartitionId()));
        }
        publishEvent(memberActivatedEvent);
    }

    public static void sendMemberReadyToShutdownEvent(MemberReadyToShutdownEvent memberReadyToShutdownEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member ready to shut down event: [service-name] %s [cluster-id] %s " +
                            "[cluster-instance-id] %s [member-id] %s [network-partition-id] %s " +
                            "[partition-id] %s",
                    memberReadyToShutdownEvent.getServiceName(),
                    memberReadyToShutdownEvent.getClusterId(),
                    memberReadyToShutdownEvent.getClusterInstanceId(),
                    memberReadyToShutdownEvent.getMemberId(),
                    memberReadyToShutdownEvent.getNetworkPartitionId(),
                    memberReadyToShutdownEvent.getPartitionId()));
        }
        // grouping
        memberReadyToShutdownEvent.setGroupId(memberReadyToShutdownEvent.getGroupId());
        publishEvent(memberReadyToShutdownEvent);
    }

    public static void sendMemberMaintenanceModeEvent(MemberMaintenanceModeEvent memberMaintenanceModeEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member in maintenance mode event: [service-name] %s [cluster-id] %s [cluster-instance-id] %s " +
                            "[member-id] %s [network-partition-id] %s [partition-id] %s",
                    memberMaintenanceModeEvent.getServiceName(),
                    memberMaintenanceModeEvent.getClusterId(),
                    memberMaintenanceModeEvent.getClusterInstanceId(),
                    memberMaintenanceModeEvent.getMemberId(),
                    memberMaintenanceModeEvent.getNetworkPartitionId(),
                    memberMaintenanceModeEvent.getPartitionId()));
        }

        publishEvent(memberMaintenanceModeEvent);
    }

    public static void sendClusterActivatedEvent(ClusterInstanceActivatedEvent clusterActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster activated event: [service-name] %s [cluster-id] %s " +
                            " [instance-id] %s [application-id] %s",
                    clusterActivatedEvent.getServiceName(),
                    clusterActivatedEvent.getClusterId(),
                    clusterActivatedEvent.getInstanceId(),
                    clusterActivatedEvent.getAppId()));
        }
        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInactivateEvent(ClusterInstanceInactivateEvent clusterInactiveEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster inactive event: [service-name] %s [cluster-id] %s " +
                            "[instance-id] %s [application-id] %s",
                    clusterInactiveEvent.getServiceName(), clusterInactiveEvent.getClusterId(),
                    clusterInactiveEvent.getInstanceId(), clusterInactiveEvent.getAppId()));
        }
        publishEvent(clusterInactiveEvent);
    }

    public static void sendClusterInstanceCreatedEvent(ClusterInstanceCreatedEvent clusterInstanceCreatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster instance created event: [service-name] %s [cluster-id] %s " +
                            " in [network-partition-id] %s [instance-id] %s",
                    clusterInstanceCreatedEvent.getServiceName(), clusterInstanceCreatedEvent.getClusterId(),
                    clusterInstanceCreatedEvent.getNetworkPartitionId(),
                    clusterInstanceCreatedEvent.getClusterInstance().getInstanceId()));
        }
        publishEvent(clusterInstanceCreatedEvent);
    }


    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String memberId,
                                                 String clusterInstanceId,
                                                 String networkPartitionId, String partitionId, Properties properties,
                                                 String groupId) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName, clusterId,
                memberId, clusterInstanceId, networkPartitionId, partitionId);
        memberTerminatedEvent.setProperties(properties);
        memberTerminatedEvent.setGroupId(groupId);

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member terminated event: [service-name] %s [cluster-id] %s " +
                            "[cluster-instance-id] %s [member-id] %s [network-partition-id] %s " +
                            "[partition-id] %s [group-id] %s", serviceName, clusterId, clusterInstanceId, memberId,
                    networkPartitionId, partitionId, groupId));
        }
        publishEvent(memberTerminatedEvent);
    }

    public static void sendCompleteTopologyEvent(Topology topology) {
        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Publishing complete topology event"));
        }
        publishEvent(completeTopologyEvent);
    }

    public static void sendClusterTerminatingEvent(ClusterInstanceTerminatingEvent clusterTerminatingEvent) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Cluster terminating event: [application-id] %s [cluster id] %s" +
                            " [instance-id] %s ",
                    clusterTerminatingEvent.getAppId(), clusterTerminatingEvent.getClusterId(),
                    clusterTerminatingEvent.getInstanceId()));
        }

        publishEvent(clusterTerminatingEvent);
    }

    public static void sendClusterTerminatedEvent(ClusterInstanceTerminatedEvent clusterTerminatedEvent) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Cluster terminated event: [application-id] %s [cluster id] %s" +
                            " [instance-id] %s ",
                    clusterTerminatedEvent.getAppId(), clusterTerminatedEvent.getClusterId(),
                    clusterTerminatedEvent.getInstanceId()));
        }

        publishEvent(clusterTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }
}
