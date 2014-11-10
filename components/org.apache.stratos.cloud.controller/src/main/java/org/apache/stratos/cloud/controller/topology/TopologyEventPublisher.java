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

package org.apache.stratos.cloud.controller.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.applications.*;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.util.Util;

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
            Port port;
            List<PortMapping> portMappings = cartridge.getPortMappings();
            for (PortMapping portMapping : portMappings) {
                port = new Port(portMapping.getProtocol(),
                        Integer.parseInt(portMapping.getPort()),
                        Integer.parseInt(portMapping.getProxyPort()));
                serviceCreatedEvent.addPort(port);
            }

            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Publishing service created event: [service] %s",
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
                        "Publishing service removed event: [service] %s",
                        serviceRemovedEvent.getServiceName()));
            }
            publishEvent(serviceRemovedEvent);
        }
    }

    public static void sendClusterResetEvent(String appId, String serviceName, String clusterId) {
        ClusterResetEvent clusterResetEvent = new ClusterResetEvent(appId, serviceName, clusterId);

        if (log.isInfoEnabled()) {
            log.info("Publishing cluster reset event: " + clusterId);
        }
        publishEvent(clusterResetEvent);
    }

    public static void sendClusterCreatedEvent(Cluster cluster) {
        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(cluster);

        if (log.isInfoEnabled()) {
            log.info("Publishing cluster created event: " + cluster.getClusterId());
        }
        publishEvent(clusterCreatedEvent);
    }

    public static void sendApplicationClustersCreated(String appId, List<Cluster> clusters) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Application Clusters registered event for Application: " + appId);
        }

        publishEvent(new ApplicationClustersCreatedEvent(clusters, appId));
    }

    public static void sendApplicationClustersRemoved(String appId, Set<ClusterDataHolder> clusters) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Application Clusters removed event for Application: " + appId);
        }

        publishEvent(new ApplicationClustersRemovedEvent(clusters, appId));
    }

//    public static void sendApplicationRemovedEvent(String applicationId, Set<ClusterDataHolder> clusterData,
//                                                   int tenantId, String tenantDomain) {
//
//        if(log.isInfoEnabled() || log.isDebugEnabled()) {
//            log.info("Publishing Application removed event: " + applicationId + " tenantId: " + tenantId);
//        }
//
//        publishEvent(new ApplicationRemovedEvent(applicationId, clusterData, tenantId, tenantDomain));
//    }

    public static void sendClusterRemovedEvent(ClusterContext ctxt, String deploymentPolicy) {
        ClusterRemovedEvent clusterRemovedEvent = new ClusterRemovedEvent(
                ctxt.getCartridgeType(), ctxt.getClusterId(), deploymentPolicy, ctxt.isLbCluster());
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing cluster removed event: [service] %s [cluster] %s",
                            ctxt.getCartridgeType(), ctxt.getClusterId()));
        }
        publishEvent(clusterRemovedEvent);

    }

    public static void sendInstanceSpawnedEvent(String serviceName,
                                                String clusterId, String networkPartitionId, String partitionId,
                                                String memberId, String lbClusterId, String publicIp,
                                                String privateIp, MemberContext context) {

        long initTime = context.getInitTime();
        InstanceSpawnedEvent instanceSpawnedEvent = new InstanceSpawnedEvent(
                serviceName, clusterId, networkPartitionId, partitionId,
                memberId, initTime);
        instanceSpawnedEvent.setLbClusterId(lbClusterId);
        instanceSpawnedEvent.setMemberIp(privateIp);
        instanceSpawnedEvent.setMemberPublicIp(publicIp);
        instanceSpawnedEvent.setProperties(CloudControllerUtil
                .toJavaUtilProperties(context.getProperties()));
        log.info(String.format("Publishing instance spawned event: [service] %s [cluster] %s [network-partition] %s  [partition] %s [member]%s[lb-cluster-id] %s", serviceName, clusterId, networkPartitionId, partitionId, memberId, lbClusterId));
        publishEvent(instanceSpawnedEvent);
    }

    public static void sendMemberStartedEvent(InstanceStartedEvent instanceStartedEvent) {
        MemberStartedEvent memberStartedEventTopology = new MemberStartedEvent(instanceStartedEvent.getServiceName(),
                instanceStartedEvent.getClusterId(), instanceStartedEvent.getNetworkPartitionId(), instanceStartedEvent.getPartitionId(), instanceStartedEvent.getMemberId());
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing member started event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s",
                            instanceStartedEvent.getServiceName(),
                            instanceStartedEvent.getClusterId(),
                            instanceStartedEvent.getNetworkPartitionId(),
                            instanceStartedEvent.getPartitionId(),
                            instanceStartedEvent.getMemberId()));
        }
        publishEvent(memberStartedEventTopology);
    }

    public static void sendMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing member activated event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s",
                            memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId(),
                            memberActivatedEvent.getNetworkPartitionId(),
                            memberActivatedEvent.getPartitionId(),
                            memberActivatedEvent.getMemberId()));
        }
        publishEvent(memberActivatedEvent);
    }

    public static void sendMemberReadyToShutdownEvent(MemberReadyToShutdownEvent memberReadyToShutdownEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member Ready to shut down event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s",
                    memberReadyToShutdownEvent.getServiceName(), memberReadyToShutdownEvent.getClusterId(), memberReadyToShutdownEvent.getNetworkPartitionId(),
                    memberReadyToShutdownEvent.getPartitionId(), memberReadyToShutdownEvent.getMemberId(), memberReadyToShutdownEvent.getGroupId()));
        }
        // grouping
        memberReadyToShutdownEvent.setGroupId(memberReadyToShutdownEvent.getGroupId());
        publishEvent(memberReadyToShutdownEvent);
    }

    public static void sendMemberMaintenanceModeEvent(MemberMaintenanceModeEvent memberMaintenanceModeEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Maintenance mode event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s",
                    memberMaintenanceModeEvent.getServiceName(), memberMaintenanceModeEvent.getClusterId(), memberMaintenanceModeEvent.getNetworkPartitionId(),
                    memberMaintenanceModeEvent.getPartitionId(), memberMaintenanceModeEvent.getMemberId(), memberMaintenanceModeEvent.getGroupId()));
        }

        publishEvent(memberMaintenanceModeEvent);
    }

    public static void sendGroupActivatedEvent(GroupActivatedEvent groupActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing group activated event: [appId] %s [group] %s",
                    groupActivatedEvent.getAppId(), groupActivatedEvent.getGroupId()));
        }
        publishEvent(groupActivatedEvent);
    }

    public static void sendClusterActivatedEvent(ClusterActivatedEvent clusterActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster activated event: [service] %s [cluster] %s [appId] %s",
                    clusterActivatedEvent.getServiceName(), clusterActivatedEvent.getClusterId(), clusterActivatedEvent.getAppId()));
        }
        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInactivateEvent(ClusterInactivateEvent clusterInactiveEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster in-active event: [service] %s [cluster] %s [appId] %s",
                    clusterInactiveEvent.getServiceName(), clusterInactiveEvent.getClusterId(), clusterInactiveEvent.getAppId()));
        }
        publishEvent(clusterInactiveEvent);
    }


    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String networkPartitionId,
                                                 String partitionId, String memberId, Properties properties, String groupId) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId);
        memberTerminatedEvent.setProperties(properties);
        memberTerminatedEvent.setGroupId(groupId);

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member terminated event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s", serviceName, clusterId, networkPartitionId,
                    partitionId, memberId, groupId));
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

    public static void sendApplicationActivatedEvent(ApplicationActivatedEvent applicationActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing application activated event: [appId] %s",
                    applicationActivatedEvent.getAppId()));
        }
        publishEvent(applicationActivatedEvent);
    }

    public static void sendApplicationInactivatedEvent(ApplicationInactivatedEvent applicationActivatedEvent1) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing application in activated event: [appId] %s",
                    applicationActivatedEvent1.getAppId()));
        }
        publishEvent(applicationActivatedEvent1);
    }

    public static void sendApplicationTerminatingEvent(ApplicationTerminatingEvent applicationTerminatingEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing application terminating event: [appId] %s",
                    applicationTerminatingEvent.getAppId()));
        }
        publishEvent(applicationTerminatingEvent);
    }

    public static void sendApplicationTerminatedEvent(ApplicationTerminatedEvent applicationTerminatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing application terminated event: [appId] %s",
                    applicationTerminatedEvent.getAppId()));
        }
        publishEvent(applicationTerminatedEvent);
    }


    public static void sendGroupTerminatedEvent(GroupTerminatedEvent groupTerminatedTopologyEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing group terminated event: [appId] %s",
                    groupTerminatedTopologyEvent.getAppId()));
        }
        publishEvent(groupTerminatedTopologyEvent);
    }

    public static void sendGroupTerminatingEvent(GroupTerminatingEvent groupTerminatingTopologyEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing group terminating event: [appId] %s",
                    groupTerminatingTopologyEvent.getAppId()));
        }
        publishEvent(groupTerminatingTopologyEvent);
    }

    public static void sendClusterTerminatingEvent(ClusterTerminatingEvent clusterTerminatingEvent) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Cluster terminating event: [appId] %s [cluster id] %s",
                    clusterTerminatingEvent.getAppId(), clusterTerminatingEvent.getClusterId()));
        }

        publishEvent(clusterTerminatingEvent);
    }

    public static void sendClusterTerminatedEvent(ClusterTerminatedEvent clusterTerminatedEvent) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Cluster terminated event: [appId] %s [cluster id] %s",
                    clusterTerminatedEvent.getAppId(), clusterTerminatedEvent.getClusterId()));
        }

        publishEvent(clusterTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        String topic = Util.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }
}
