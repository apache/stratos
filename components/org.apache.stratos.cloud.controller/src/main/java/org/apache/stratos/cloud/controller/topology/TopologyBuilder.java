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
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.Cartridge;
import org.apache.stratos.cloud.controller.util.LocationScope;
import org.apache.stratos.cloud.controller.util.PortMapping;
import org.apache.stratos.cloud.controller.util.ServiceContext;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.status.MemberActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.MemberStartedEvent;

import java.util.List;

/**
 * this is to manipulate the received events by cloud controller
 * and build the complete topology with the events received
 */
public class TopologyBuilder {
    private static final Log log = LogFactory.getLog(TopologyBuilder.class);


    public static void handleServiceCreated(List<Cartridge> cartridgeList) {
        Service service;
        Topology topology = TopologyManager.getInstance().getTopology();
        if (cartridgeList == null) {
            throw new RuntimeException(String.format("Cartridge list is empty"));
        }
        try {
            TopologyManager.getInstance().acquireWriteLock();
            for (Cartridge cartridge : cartridgeList) {
                if (!topology.serviceExists(cartridge.getType())) {
                    service = new Service(cartridge.getType());
                    topology.addService(service);
                    TopologyManager.getInstance().updateTopology(topology);
                }
            }
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendServiceCreateEvent(cartridgeList);

    }

    public static void handlePartitionCreated(Partition partition) {

        Topology topology = TopologyManager.getInstance().getTopology();
        if (partition == null) {
            throw new RuntimeException(String.format("Partition is empty"));
        }
        try {
            TopologyManager.getInstance().acquireWriteLock();
            topology.addPartition(partition);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendPartitionCreatedEvent(partition);

    }

    public static void handlePartitionUpdated(Partition newPartition, Partition oldPartition) {

        Topology topology = TopologyManager.getInstance().getTopology();
        if (newPartition == null || oldPartition == null) {
            throw new RuntimeException(String.format("Partition is empty"));
        }
        try {
            TopologyManager.getInstance().acquireWriteLock();
            topology.removePartition(oldPartition);
            topology.addPartition(newPartition);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendPartitionUpdatedEvent(newPartition, oldPartition.getId());

    }

    public static void handlePartitionRemoved(Partition partition) {

        Topology topology = TopologyManager.getInstance().getTopology();
        if (partition == null) {
            throw new RuntimeException(String.format("Partition is empty"));
        }
        try {
            TopologyManager.getInstance().acquireWriteLock();
            topology.removePartition(partition);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendPartitionRemovedEvent(partition);
    }


    public static void handleServiceRemoved(List<Cartridge> cartridgeList) {
        Topology topology = TopologyManager.getInstance().getTopology();

        for (Cartridge cartridge : cartridgeList) {
            if (topology.getService(cartridge.getType()).getClusters().size() == 0) {
                if (topology.serviceExists(cartridge.getType())) {
                    try {
                        TopologyManager.getInstance().acquireWriteLock();
                        topology.removeService(cartridge.getType());
                        TopologyManager.getInstance().updateTopology(topology);
                    } finally {
                        TopologyManager.getInstance().releaseWriteLock();
                    }
                    TopologyEventSender.sendServiceRemovedEvent(cartridgeList);
                } else {
                    throw new RuntimeException(String.format("Service %s does not exist..", cartridge.getType()));
                }
            } else {
                log.warn("Subscription already exists. Hence not removing the service:" + cartridge.getType()
                        + " from the topology");
            }
        }
    }

    public static void handleClusterCreated(ServiceContext serviceContext) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service;
        try {
            TopologyManager.getInstance().acquireWriteLock();
            service = topology.getService(serviceContext.getCartridgeType());
            if (service == null) {
                service = new Service(serviceContext.getClusterId());
                Cluster cluster = new Cluster(serviceContext.getCartridgeType(),
                        serviceContext.getClusterId(),
                        serviceContext.getAutoScalerPolicyName());
                cluster.setHostName(serviceContext.getHostName());
                cluster.setTenantRange(serviceContext.getTenantRange());
                cluster.setAutoscalePolicyName(serviceContext.getAutoScalerPolicyName());
                service.addCluster(cluster);
                topology.addService(service);
            } else {
                if (service.clusterExists(serviceContext.getClusterId())) {
                    //update the cluster
                    service.getCluster(serviceContext.getClusterId()).
                            setHostName(serviceContext.getHostName());
                    service.getCluster(serviceContext.getClusterId()).
                            setAutoscalePolicyName(serviceContext.getAutoScalerPolicyName());
                    service.getCluster(serviceContext.getClusterId()).
                            setTenantRange(serviceContext.getTenantRange());
                } else {
                    Cluster cluster = new Cluster(serviceContext.getCartridgeType(),
                            serviceContext.getClusterId(),
                            serviceContext.getAutoScalerPolicyName());
                    cluster.setHostName(serviceContext.getHostName());
                    cluster.setTenantRange(serviceContext.getTenantRange());
                    cluster.setAutoscalePolicyName(serviceContext.getAutoScalerPolicyName());
                    service.addCluster(cluster);
                }
            }
            TopologyManager.getInstance().updateTopology(topology);
            TopologyEventSender.sendClusterCreatedEvent(serviceContext);

        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
    }

    public static void handleClusterRemoved(ServiceContext serviceContext) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(serviceContext.getCartridgeType());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                    serviceContext.getCartridgeType()));
        }

        if (!service.clusterExists(serviceContext.getClusterId())) {
            throw new RuntimeException(String.format("Cluster %s does not exist for service %s",
                    serviceContext.getClusterId(),
                    serviceContext.getCartridgeType()));
        }

        try {
            TopologyManager.getInstance().acquireWriteLock();
            service.removeCluster(serviceContext.getClusterId());
            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendClusterRemovedEvent(serviceContext);
    }

    public static void handleMemberSpawned(String memberId, String serviceName, String clusterId,
                                           String iaasNodeId, LocationScope locationScope, String privateIp) {
        //adding the new member to the cluster after it is successfully started in IaaS.
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);
        //TODO adding the IaaS scope to the member

        if (cluster.memberExists(memberId)) {
            throw new RuntimeException(String.format("Member %s already exists", memberId));
        }

        try {
            TopologyManager.getInstance().acquireWriteLock();
            Member member = new Member(serviceName, clusterId, memberId);
            member.setIaasNodeId(iaasNodeId);
            member.setStatus(MemberStatus.Created);
            member.setMemberIp(privateIp);
            cluster.addMember(member);
            cluster.addMemberToIaasNodeId(member);
            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendInstanceSpawnedEvent(serviceName, clusterId, memberId, iaasNodeId);

    }

    public static void handleMemberStarted(MemberStartedEvent memberStartedEvent) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(memberStartedEvent.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                    memberStartedEvent.getServiceName()));
        }
        if (!service.clusterExists(memberStartedEvent.getClusterId())) {
            throw new RuntimeException(String.format("Cluster %s does not exist in service %s",
                    memberStartedEvent.getClusterId(),
                    memberStartedEvent.getServiceName()));
        }

        Member member = service.getCluster(memberStartedEvent.getClusterId()).
                getMember(memberStartedEvent.getMemberId());
        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist",
                    memberStartedEvent.getMemberId()));
        }
        try {
            TopologyManager.getInstance().acquireWriteLock();
            member.setStatus(MemberStatus.Starting);
            log.info("member started event adding status started");

            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendMemberStartedEvent(memberStartedEvent);
    }

    public static void handleMemberActivated(MemberActivatedEvent memberActivatedEvent) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(memberActivatedEvent.getServiceName());
        Cluster cluster = service.getCluster(memberActivatedEvent.getClusterId());
        Member member = cluster.getMember(memberActivatedEvent.getMemberId());

        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                    memberActivatedEvent.getServiceName()));
        }

        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist",
                    memberActivatedEvent.getClusterId()));
        }

        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist",
                    memberActivatedEvent.getMemberId()));
        }

        org.apache.stratos.messaging.event.topology.MemberActivatedEvent memberActivatedEventTopology =
                new org.apache.stratos.messaging.event.topology.MemberActivatedEvent(memberActivatedEvent.getServiceName(),
                        memberActivatedEvent.getClusterId(), memberActivatedEvent.getMemberId());

        try {
            TopologyManager.getInstance().acquireWriteLock();
            member.setStatus(MemberStatus.Activated);
            log.info("member started event adding status activated");
            Cartridge cartridge = FasterLookUpDataHolder.getInstance().
                    getCartridge(memberActivatedEvent.getServiceName());

            List<PortMapping> portMappings = cartridge.getPortMappings();
            Port port;
            //adding ports to the event
            for (PortMapping portMapping : portMappings) {
                port = new Port(portMapping.getProtocol(),
                        Integer.parseInt(portMapping.getPort()),
                        Integer.parseInt(portMapping.getProxyPort()));
                member.addPort(port);
                memberActivatedEventTopology.addPort(port);
            }
            memberActivatedEventTopology.setMemberIp(member.getMemberIp());
            TopologyManager.getInstance().updateTopology(topology);

        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendMemberActivatedEvent(memberActivatedEventTopology);
    }

    public static void handleMemberTerminated(String serviceName, String clusterId, String nodeId) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);
        Member member = cluster.getMemberFromIaasNodeId(nodeId);
        String memberId = member.getMemberId();

        if (member == null) {
            throw new RuntimeException(String.format("Member with nodeID %s does not exist",
                    nodeId));
        }

        try {
            TopologyManager.getInstance().acquireWriteLock();
            cluster.removeMember(member);
            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendMemberTerminatedEvent(serviceName, clusterId, memberId);
    }

    public static void handleMemberSuspended() {
        //TODO
        try {
            TopologyManager.getInstance().acquireWriteLock();
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
    }


}
