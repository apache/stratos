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
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.Cartridge;
import org.apache.stratos.cloud.controller.util.PortMapping;
import org.apache.stratos.cloud.controller.util.ServiceContext;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;

import java.util.List;

/**
 * this is to send the relevant events from cloud controller to topology topic
 */
public class TopologyEventSender {
    private static final Log log = LogFactory.getLog(TopologyBuilder.class);


    public static void sendServiceCreateEvent(List<Cartridge> cartridgeList) {
        ServiceCreatedEvent serviceCreatedEvent;
        for(Cartridge cartridge : cartridgeList) {
            serviceCreatedEvent = new ServiceCreatedEvent(cartridge.getType());

            // Add ports to the event
            Port port;
            List<PortMapping> portMappings = cartridge.getPortMappings();
            for(PortMapping portMapping : portMappings) {
                port = new Port(portMapping.getProtocol(),
                                Integer.parseInt(portMapping.getPort()),
                                Integer.parseInt(portMapping.getProxyPort()));
                serviceCreatedEvent.addPort(port);
            }
            publishEvent(serviceCreatedEvent);
        }
    }

    public static void sendServiceRemovedEvent(List<Cartridge> cartridgeList) {
        ServiceRemovedEvent serviceRemovedEvent;
        for(Cartridge cartridge : cartridgeList) {
            serviceRemovedEvent = new ServiceRemovedEvent();
            serviceRemovedEvent.setServiceName(cartridge.getType());
            publishEvent(serviceRemovedEvent);
        }
    }

    public static void sendClusterCreatedEvent(ServiceContext serviceContext) {
        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(serviceContext.getCartridgeType(),
                                                                          serviceContext.getClusterId());
        clusterCreatedEvent.setHostName(serviceContext.getHostName());
        clusterCreatedEvent.setTenantRange(serviceContext.getTenantRange());
        clusterCreatedEvent.setAutoscalingPolicyName(serviceContext.getAutoScalerPolicyName());
        publishEvent(clusterCreatedEvent);

    }

    public static void sendClusterRemovedEvent(ServiceContext serviceContext) {
        ClusterRemovedEvent clusterRemovedEvent = new ClusterRemovedEvent();
        clusterRemovedEvent.setClusterId(serviceContext.getClusterId());
        clusterRemovedEvent.setServiceName(serviceContext.getCartridgeType());
        publishEvent(clusterRemovedEvent);

    }

    public static void sendInstanceSpawnedEvent(String serviceName, String clusterId, String memberId, String nodeId) {
        InstanceSpawnedEvent instanceSpawnedEvent = new InstanceSpawnedEvent(serviceName,
                                                                             clusterId,
                                                                             memberId,
                                                                             nodeId);
        publishEvent(instanceSpawnedEvent);
    }

    public static void sendMemberStartedEvent(org.apache.stratos.messaging.event.instance.status.MemberStartedEvent memberStartedEvent) {
        log.info("member started event" + MemberStartedEvent.class);
        MemberStartedEvent memberStartedEventTopology = new MemberStartedEvent(memberStartedEvent.getServiceName(),
                            memberStartedEvent.getClusterId(), memberStartedEvent.getMemberId());
        publishEvent(memberStartedEventTopology);
    }

     public static void sendMemberActivatedEvent(MemberActivatedEvent memberActivatedEventTopology) {
         log.info("member activated event" + MemberActivatedEvent.class);
         publishEvent(memberActivatedEventTopology);
    }

    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String memberId) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName,
                                                                                clusterId,
                                                                                memberId);
        publishEvent(memberTerminatedEvent);
    }

    public static void sendCompleteTopologyEvent(Topology topology) {
        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent();
        completeTopologyEvent.setTopology(topology);
        publishEvent(completeTopologyEvent);
    }

    public static void publishEvent(Event topologyEvent) {
        List<EventPublisher> topicPublishers = FasterLookUpDataHolder.getInstance().getAllEventPublishers();

        for(EventPublisher topicPublisher : topicPublishers) {
            topicPublisher.publish(topologyEvent);
        }

    }
}
