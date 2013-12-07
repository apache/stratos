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
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.pojo.Registrant;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.status.MemberActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.MemberStartedEvent;

import java.util.List;
import java.util.Properties;

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
                    service = new Service(cartridge.getType(), cartridge.isMultiTenant() ? ServiceType.MultiTenant : ServiceType.SingleTenant);
                     List<PortMapping> portMappings = cartridge.getPortMappings();
                    Port port;
                    //adding ports to the event
                    for (PortMapping portMapping : portMappings) {
                        port = new Port(portMapping.getProtocol(),
                                Integer.parseInt(portMapping.getPort()),
                                Integer.parseInt(portMapping.getProxyPort()));
                        service.addPort(port);
                    }
                    topology.addService(service);
                    TopologyManager.getInstance().updateTopology(topology);
                }
            }
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendServiceCreateEvent(cartridgeList);

    }

//    public static void handlePartitionCreated(Partition partition) {
//
//        Topology topology = TopologyManager.getInstance().getTopology();
//        if (partition == null) {
//            throw new RuntimeException(String.format("Partition is empty"));
//        }
//        try {
//            TopologyManager.getInstance().acquireWriteLock();
//            topology.addPartition(partition);
//            TopologyManager.getInstance().updateTopology(topology);
//        } finally {
//            TopologyManager.getInstance().releaseWriteLock();
//        }
//        TopologyEventSender.sendPartitionCreatedEvent(partition);
//
//    }

//    public static void handlePartitionUpdated(Partition newPartition, Partition oldPartition) {
//
//        Topology topology = TopologyManager.getInstance().getTopology();
//        if (newPartition == null || oldPartition == null) {
//            throw new RuntimeException(String.format("Partition is empty"));
//        }
//        try {
//            TopologyManager.getInstance().acquireWriteLock();
//            topology.removePartition(oldPartition);
//            topology.addPartition(newPartition);
//            TopologyManager.getInstance().updateTopology(topology);
//        } finally {
//            TopologyManager.getInstance().releaseWriteLock();
//        }
//        TopologyEventSender.sendPartitionUpdatedEvent(newPartition, oldPartition.getId());
//
//    }
//
//    public static void handlePartitionRemoved(Partition partition) {
//
//        Topology topology = TopologyManager.getInstance().getTopology();
//        if (partition == null) {
//            throw new RuntimeException(String.format("Partition is empty"));
//        }
//        try {
//            TopologyManager.getInstance().acquireWriteLock();
//            topology.removePartition(partition);
//            TopologyManager.getInstance().updateTopology(topology);
//        } finally {
//            TopologyManager.getInstance().releaseWriteLock();
//        }
//        TopologyEventSender.sendPartitionRemovedEvent(partition);
//    }


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

    public static void handleClusterCreated(Registrant registrant) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service;
        try {
            TopologyManager.getInstance().acquireWriteLock();
            service = topology.getService(registrant.getCartridgeType());
//            if (service == null) {
//                service = new Service(registrant.getClusterId());
//                Cluster cluster = new Cluster(registrant.getCartridgeType(),
//                                              registrant.getClusterId(),
//                                              registrant.getAutoScalerPolicyName());
//                cluster.setHostName(registrant.getHostName());
//                cluster.setTenantRange(registrant.getTenantRange());
//                cluster.setAutoscalePolicyName(registrant.getAutoScalerPolicyName());
//                service.addCluster(cluster);
//                topology.addService(service);
//            } else {
            Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
            
            Cluster cluster;
                if (service.clusterExists(registrant.getClusterId())) {
                    //update the cluster
                    cluster = service.getCluster(registrant.getClusterId());
                    cluster.addHostName(registrant.getHostName());
                    cluster.setAutoscalePolicyName(registrant.getAutoScalerPolicyName());
                    cluster.setTenantRange(registrant.getTenantRange());
                    cluster.setProperties(props);
                } else {
                    cluster = new Cluster(registrant.getCartridgeType(),
                            registrant.getClusterId(),
                            registrant.getAutoScalerPolicyName());
                    cluster.addHostName(registrant.getHostName());
                    cluster.setTenantRange(registrant.getTenantRange());
                    cluster.setAutoscalePolicyName(registrant.getAutoScalerPolicyName());
                    cluster.setProperties(props);
                    service.addCluster(cluster);
                }
//            }
            TopologyManager.getInstance().updateTopology(topology);
            TopologyEventSender.sendClusterCreatedEvent(registrant);

        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
    }

    public static void handleClusterRemoved(ClusterContext ctxt) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(ctxt.getCartridgeType());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                    ctxt.getCartridgeType()));
        }

        if (!service.clusterExists(ctxt.getClusterId())) {
            throw new RuntimeException(String.format("Cluster %s does not exist for service %s",
                    ctxt.getClusterId(),
                    ctxt.getCartridgeType()));
        }

        try {
            TopologyManager.getInstance().acquireWriteLock();
            service.removeCluster(ctxt.getClusterId());
            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendClusterRemovedEvent(ctxt);
    }

    public static void handleMemberSpawned(String memberId, String serviceName, String clusterId,
                                           Partition partition, String privateIp) {
        //adding the new member to the cluster after it is successfully started in IaaS.
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);
        String partitionId = partition.getId();

        if (cluster.memberExists(memberId)) {
            throw new RuntimeException(String.format("Member %s already exists", memberId));
        }

        try {
            TopologyManager.getInstance().acquireWriteLock();
            Member member = new Member(serviceName, clusterId, memberId);
            member.setPartitionId(partitionId);
            member.setStatus(MemberStatus.Created);
            member.setMemberIp(privateIp);
            cluster.addMember(member);
            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendInstanceSpawnedEvent(serviceName, clusterId, memberId, partitionId);

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
        //memberStartedEvent.
        TopologyEventSender.sendMemberStartedEvent(memberStartedEvent);
    }

    public static void handleMemberActivated(MemberActivatedEvent memberActivatedEvent) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(memberActivatedEvent.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                                                     memberActivatedEvent.getServiceName()));
        }
        
        Cluster cluster = service.getCluster(memberActivatedEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist",
                                                     memberActivatedEvent.getClusterId()));
        }
        Member member = cluster.getMember(memberActivatedEvent.getMemberId());

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
            
            memberActivatedEventTopology.setPartitionId(member.getPartitionId());
            memberActivatedEventTopology.setMemberIp(member.getMemberIp());
            TopologyManager.getInstance().updateTopology(topology);

        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendMemberActivatedEvent(memberActivatedEventTopology);
    }

    public static void handleMemberTerminated(String serviceName, String clusterId, String memberId) {
        Topology topology = TopologyManager.getInstance().getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);
        Member member = cluster.getMember(memberId);

        if (member == null) {
            throw new RuntimeException(String.format("Member with nodeID %s does not exist",
                    memberId));
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
