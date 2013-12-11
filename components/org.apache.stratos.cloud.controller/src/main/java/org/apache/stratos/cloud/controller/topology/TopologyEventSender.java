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
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.pojo.Registrant;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;

import java.util.List;
import java.util.Properties;

/**
 * this is to send the relevant events from cloud controller to topology topic
 */
public class TopologyEventSender {
    private static final Log log = LogFactory.getLog(TopologyBuilder.class);


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

    public static void sendClusterCreatedEvent(Registrant registrant) {
        Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(registrant.getCartridgeType(),
                                                                          registrant.getClusterId(),
                                                                          registrant.getHostName());
        clusterCreatedEvent.setTenantRange(registrant.getTenantRange());
        clusterCreatedEvent.setAutoscalingPolicyName(registrant.getAutoScalerPolicyName());
        clusterCreatedEvent.setProperties(props);
        clusterCreatedEvent.setDeploymentPolicyName(registrant.getDeploymentPolicyName());

        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster created event: " +
                    "[service] %s [cluster] %s [host] %s [tenant-range] %s [autoscaling-policy] %s [deployment-policy] %s ",
                                   registrant.getCartridgeType(), registrant.getClusterId(), 
                                   registrant.getHostName(), registrant.getTenantRange(), registrant.getAutoScalerPolicyName(),
                                   registrant.getDeploymentPolicyName()));
        }
        publishEvent(clusterCreatedEvent);

    }

    public static void sendClusterRemovedEvent(ClusterContext ctxt) {
        ClusterRemovedEvent clusterRemovedEvent = new ClusterRemovedEvent(ctxt.getCartridgeType(), ctxt.getClusterId());

        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster removed event: [service] %s [cluster] %s", ctxt.getCartridgeType(), ctxt.getClusterId()));
        }
        publishEvent(clusterRemovedEvent);

    }

    public static void sendInstanceSpawnedEvent(String serviceName, String clusterId, String partitionId, String memberId) {
        InstanceSpawnedEvent instanceSpawnedEvent = new InstanceSpawnedEvent(serviceName, clusterId, partitionId, memberId);
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing instance spawned event: [service] %s [cluster] %s [partition] %s [member] %s", serviceName, clusterId, partitionId, memberId));
        }
        publishEvent(instanceSpawnedEvent);
    }

    public static void sendMemberStartedEvent(InstanceStartedEvent instanceStartedEvent) {
        MemberStartedEvent memberStartedEventTopology = new MemberStartedEvent(instanceStartedEvent.getServiceName(),
                           instanceStartedEvent.getClusterId(), instanceStartedEvent.getMemberId());

        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member started event: [service] %s [cluster] %s [member] %s", instanceStartedEvent.getServiceName(), instanceStartedEvent.getClusterId(), instanceStartedEvent.getMemberId()));
        }
        publishEvent(memberStartedEventTopology);
    }

     public static void sendMemberActivatedEvent(MemberActivatedEvent memberActivatedEvent) {
         if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member activated event: [service] %s [cluster] %s [member] %s", memberActivatedEvent.getServiceName(), memberActivatedEvent.getClusterId(), memberActivatedEvent.getMemberId()));
         }
         publishEvent(memberActivatedEvent);
    }

    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String partitionId, String memberId) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName, clusterId, partitionId, memberId);
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing member terminated event: [service] %s [cluster] %s [partition] %s [member] %s", serviceName, clusterId, partitionId, memberId));
        }
        publishEvent(memberTerminatedEvent);
    }

    public static void sendCompleteTopologyEvent(Topology topology) {
        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);

        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing complete topology event"));
        }
        publishEvent(completeTopologyEvent);
    }

    public static void publishEvent(Event topologyEvent) {
        List<EventPublisher> topicPublishers = FasterLookUpDataHolder.getInstance().getAllEventPublishers();

        for(EventPublisher topicPublisher : topicPublishers) {
            topicPublisher.publish(topologyEvent);
        }

    }
}
