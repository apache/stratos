package org.apache.stratos.cloud.controller.topology;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
<<<<<<< HEAD
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ConfigCompositeApplication;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.domain.topology.Topology;
=======
import org.apache.stratos.messaging.domain.topology.*;
>>>>>>> master
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.util.Constants;

import java.util.List;
import java.util.Properties;

/**
 * this is to send the relevant events from cloud controller to topology topic
 */
public class TopologyEventPublisher {
    private static final Log log = LogFactory.getLog(TopologyEventPublisher.class);


    public static void sendServiceCreateEvent(List<Cartridge> cartridgeList) {
        ServiceCreatedEvent serviceCreatedEvent;
        for(Cartridge cartridge : cartridgeList) {
            serviceCreatedEvent = new ServiceCreatedEvent(cartridge.getType(), (cartridge.isMultiTenant() ? ServiceType.MultiTenant : ServiceType.SingleTenant));

            // Add ports to the event
            Port port;
            List<PortMapping> portMappings = cartridge.getPortMappings();
            for(PortMapping portMapping : portMappings) {
                port = new Port(portMapping.getProtocol(),
                                Integer.parseInt(portMapping.getPort()),
                                Integer.parseInt(portMapping.getProxyPort()));
                serviceCreatedEvent.addPort(port);
            }

            if(log.isInfoEnabled()) {
                log.info(String.format("Publishing service created event: [service] %s", cartridge.getType()));
            }
            publishEvent(serviceCreatedEvent);
        }
    }

    public static void sendServiceRemovedEvent(List<Cartridge> cartridgeList) {
        ServiceRemovedEvent serviceRemovedEvent;
        for(Cartridge cartridge : cartridgeList) {
            serviceRemovedEvent = new ServiceRemovedEvent(cartridge.getType());
            if(log.isInfoEnabled()) {
                log.info(String.format("Publishing service removed event: [service] %s", serviceRemovedEvent.getServiceName()));
            }
            publishEvent(serviceRemovedEvent);
        }
    }

    public static void sendClusterCreatedEvent(String serviceName, String clusterId, Cluster cluster) {
        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(serviceName, clusterId, cluster);

        if(log.isInfoEnabled()) {
            log.info("Publishing cluster created event: " +cluster.toString());
        }
        publishEvent(clusterCreatedEvent);

    }

    public static void sendClusterRemovedEvent(ClusterContext ctxt, String deploymentPolicy) {

        ClusterRemovedEvent clusterRemovedEvent = new ClusterRemovedEvent(ctxt.getCartridgeType(), ctxt.getClusterId(), deploymentPolicy, ctxt.isLbCluster());

        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster removed event: [service] %s [cluster] %s", ctxt.getCartridgeType(), ctxt.getClusterId()));
        }
        publishEvent(clusterRemovedEvent);

    }

    public static void sendClusterMaintenanceModeEvent(ClusterContext ctxt) {

        ClusterMaintenanceModeEvent clusterMaintenanceModeEvent = new ClusterMaintenanceModeEvent(ctxt.getCartridgeType(), ctxt.getClusterId());
        clusterMaintenanceModeEvent.setStatus(ClusterStatus.In_Maintenance);
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster maintenance mode event: [service] %s [cluster] %s",
                    clusterMaintenanceModeEvent.getServiceName(), clusterMaintenanceModeEvent.getClusterId()));
        }
        publishEvent(clusterMaintenanceModeEvent);

    }

    public static void sendInstanceSpawnedEvent(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId,
    		String lbClusterId, String publicIp, String privateIp, MemberContext context) {
        InstanceSpawnedEvent instanceSpawnedEvent = new InstanceSpawnedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId);
        instanceSpawnedEvent.setLbClusterId(lbClusterId);
        instanceSpawnedEvent.setMemberIp(privateIp);
        instanceSpawnedEvent.setMemberPublicIp(publicIp);
        instanceSpawnedEvent.setProperties(CloudControllerUtil.toJavaUtilProperties(context.getProperties()));
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing instance spawned event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [lb-cluster-id] %s",
                    serviceName, clusterId, networkPartitionId, partitionId, memberId, lbClusterId));
        }
        publishEvent(instanceSpawnedEvent);
    }

    public static void sendMemberStartedEvent(InstanceStartedEvent instanceStartedEvent) {
        MemberStartedEvent memberStartedEventTopology = new MemberStartedEvent(instanceStartedEvent.getServiceName(),
                           instanceStartedEvent.getClusterId(), instanceStartedEvent.getNetworkPartitionId(), instanceStartedEvent.getPartitionId(), instanceStartedEvent.getMemberId());

     // grouping
        memberStartedEventTopology.setGroupId(instanceStartedEvent.getGroupId());
        if(log.isInfoEnabled()) {
            log.info(" Grouping member started event - adding groupID " + instanceStartedEvent.getGroupId() + " for cluster " +
            		instanceStartedEvent.getClusterId());
        }
        
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member started event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s",
            		memberStartedEventTopology.getServiceName(), memberStartedEventTopology.getClusterId(), memberStartedEventTopology.getNetworkPartitionId(), 
            		memberStartedEventTopology.getPartitionId(), memberStartedEventTopology.getMemberId(), memberStartedEventTopology.getGroupId()));
        }
        
        publishEvent(memberStartedEventTopology);
    }

     public static void sendMemberActivatedEvent(MemberActivatedEvent memberActivatedEvent) {
         if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member activated event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s",
                    memberActivatedEvent.getServiceName(), memberActivatedEvent.getClusterId(), memberActivatedEvent.getNetworkPartitionId(), 
                    memberActivatedEvent.getPartitionId(), memberActivatedEvent.getMemberId(), memberActivatedEvent.getGroupId()));
         }
         publishEvent(memberActivatedEvent);
    }

    public static void sendMemberReadyToShutdownEvent(MemberReadyToShutdownEvent memberReadyToShutdownEvent) {
         if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member Ready to shut down event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s",
                    memberReadyToShutdownEvent.getServiceName(), memberReadyToShutdownEvent.getClusterId(), memberReadyToShutdownEvent.getNetworkPartitionId(), 
                    memberReadyToShutdownEvent.getPartitionId(), memberReadyToShutdownEvent.getMemberId(), memberReadyToShutdownEvent.getGroupId()));
         }
         // grouping
         memberReadyToShutdownEvent.setGroupId(memberReadyToShutdownEvent.getGroupId());
         publishEvent(memberReadyToShutdownEvent);
    }

    public static void sendMemberMaintenanceModeEvent(MemberMaintenanceModeEvent memberMaintenanceModeEvent) {
            if(log.isInfoEnabled()) {
               log.info(String.format("Publishing Maintenance mode event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s",
                       memberMaintenanceModeEvent.getServiceName(), memberMaintenanceModeEvent.getClusterId(), memberMaintenanceModeEvent.getNetworkPartitionId(), 
                       memberMaintenanceModeEvent.getPartitionId(), memberMaintenanceModeEvent.getMemberId(), memberMaintenanceModeEvent.getGroupId()));
            }

            publishEvent(memberMaintenanceModeEvent);
       }


<<<<<<< HEAD
    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId, String groupId) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId);
        // grouping
        memberTerminatedEvent.setGroupId(groupId);
=======
    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String networkPartitionId,
                                                 String partitionId, String memberId, Properties properties) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId);
        memberTerminatedEvent.setProperties(properties);
>>>>>>> master
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member terminated event: [service] %s [cluster] %s [network-partition] %s [partition] %s [member] %s [groupId] %s", serviceName, clusterId, networkPartitionId, 
            		partitionId, memberId, groupId));
        }

        publishEvent(memberTerminatedEvent);
    }

    public static void sendCompleteTopologyEvent(Topology topology) {
        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);

        if(log.isDebugEnabled()) {
            log.debug(String.format("Publishing complete topology event"));
        }
        publishEvent(completeTopologyEvent);
    }
    
    // Grouping
    public static void sendConfigApplicationCreatedEventEvent(ConfigCompositeApplication configCompositeApplication) {
        
    	CompositeApplicationCreatedEvent compositeApplicationCreatedEvent = new CompositeApplicationCreatedEvent(configCompositeApplication);

        if(log.isDebugEnabled()) {
            log.debug(String.format("Publishing compositeApplicationCreatedEvent: " + compositeApplicationCreatedEvent));
        }
        publishEvent(compositeApplicationCreatedEvent);
    }
    
	public static void sendConfigApplicationRemovedEventEvent(String alias) {
	        
    	CompositeApplicationRemovedEvent compositeApplicationCreatedEvent = new CompositeApplicationRemovedEvent(alias);

        if(log.isDebugEnabled()) {
            log.debug(String.format("Publishing compositeApplicationRemovedEvent: " + compositeApplicationCreatedEvent));
        }
        publishEvent(compositeApplicationCreatedEvent);
	}

    public static void publishEvent(Event event) {
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.TOPOLOGY_TOPIC);
        eventPublisher.publish(event);
    }
}
