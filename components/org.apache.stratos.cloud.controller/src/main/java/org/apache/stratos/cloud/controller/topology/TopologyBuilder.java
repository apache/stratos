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
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.impl.CloudControllerServiceImpl;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.pojo.Registrant;
import org.apache.stratos.cloud.controller.publisher.CartridgeInstanceDataPublisher;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.util.Constants;

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
        Topology topology = TopologyManager.getTopology();
        if (cartridgeList == null) {
            throw new RuntimeException(String.format("Cartridge list is empty"));
        }
        try {

            TopologyManager.acquireWriteLock();
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
                    TopologyManager.updateTopology(topology);
                }
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendServiceCreateEvent(cartridgeList);

    }

    public static void handleServiceRemoved(List<Cartridge> cartridgeList) {
        Topology topology = TopologyManager.getTopology();

        for (Cartridge cartridge : cartridgeList) {
            if (topology.getService(cartridge.getType()).getClusters().size() == 0) {
                if (topology.serviceExists(cartridge.getType())) {
                    try {
                        TopologyManager.acquireWriteLock();
                        topology.removeService(cartridge.getType());
                        TopologyManager.updateTopology(topology);
                    } finally {
                        TopologyManager.releaseWriteLock();
                    }
                    TopologyEventPublisher.sendServiceRemovedEvent(cartridgeList);
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
        Topology topology = TopologyManager.getTopology();
        Service service;
        try {
            TopologyManager.acquireWriteLock();
            String cartridgeType = registrant.getCartridgeType();
            service = topology.getService(cartridgeType);
            Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
            
            String property = props.getProperty(Constants.IS_LOAD_BALANCER);
            boolean isLb = property != null ? Boolean.parseBoolean(property) : false;
            
            Cluster cluster;
            String clusterId = registrant.getClusterId();
            if (service.clusterExists(clusterId)) {
                // update the cluster
                cluster = service.getCluster(clusterId);
                cluster.addHostName(registrant.getHostName());
                if(service.getServiceType() == ServiceType.MultiTenant) {
                    cluster.setTenantRange(registrant.getTenantRange());
                }
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
            } else {
                cluster = new Cluster(cartridgeType, clusterId,
                                      registrant.getDeploymentPolicyName(), registrant.getAutoScalerPolicyName());
                cluster.addHostName(registrant.getHostName());
                if(service.getServiceType() == ServiceType.MultiTenant) {
                    cluster.setTenantRange(registrant.getTenantRange());
                }
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
                service.addCluster(cluster);
            }
            TopologyManager.updateTopology(topology);
            TopologyEventPublisher.sendClusterCreatedEvent(cartridgeType, clusterId, cluster);

        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static void handleClusterRemoved(ClusterContext ctxt) {
        Topology topology = TopologyManager.getTopology();
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
            TopologyManager.acquireWriteLock();
            service.removeCluster(ctxt.getClusterId());
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendClusterRemovedEvent(ctxt);
    }

    public static void handleMemberSpawned(String memberId, String serviceName, String clusterId,
                                           String networkPartitionId, String partitionId, String privateIp, String lbClusterId) {
        //adding the new member to the cluster after it is successfully started in IaaS.
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);

        if (cluster.memberExists(memberId)) {
            throw new RuntimeException(String.format("Member %s already exists", memberId));
        }

        try {
            TopologyManager.acquireWriteLock();
            Member member = new Member(serviceName, clusterId, networkPartitionId, partitionId, memberId);
            member.setStatus(MemberStatus.Created);
            member.setMemberIp(privateIp);
            member.setLbClusterId(lbClusterId);
            cluster.addMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendInstanceSpawnedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId, lbClusterId);
    }

    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceStartedEvent.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                    instanceStartedEvent.getServiceName()));
        }
        if (!service.clusterExists(instanceStartedEvent.getClusterId())) {
            throw new RuntimeException(String.format("Cluster %s does not exist in service %s",
                    instanceStartedEvent.getClusterId(),
                    instanceStartedEvent.getServiceName()));
        }

        Member member = service.getCluster(instanceStartedEvent.getClusterId()).
                getMember(instanceStartedEvent.getMemberId());
        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist",
                    instanceStartedEvent.getMemberId()));
        }
        try {
            TopologyManager.acquireWriteLock();
            member.setStatus(MemberStatus.Starting);
            log.info("member started event adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //memberStartedEvent.
        TopologyEventPublisher.sendMemberStartedEvent(instanceStartedEvent);
        //publishing data
        CartridgeInstanceDataPublisher.publish(instanceStartedEvent.getMemberId(),
                                            instanceStartedEvent.getPartitionId(),
                                            instanceStartedEvent.getNetworkPartitionId(),
                                            instanceStartedEvent.getClusterId(),
                                            instanceStartedEvent.getServiceName(),
                                            MemberStatus.Starting.toString(),
                                            null);
    }

    public static void handleMemberActivated(InstanceActivatedEvent instanceActivatedEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceActivatedEvent.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                                                     instanceActivatedEvent.getServiceName()));
        }
        
        Cluster cluster = service.getCluster(instanceActivatedEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist",
                                                     instanceActivatedEvent.getClusterId()));
        }
        Member member = cluster.getMember(instanceActivatedEvent.getMemberId());

        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist",
                    instanceActivatedEvent.getMemberId()));
        }

        MemberActivatedEvent memberActivatedEvent = new MemberActivatedEvent(instanceActivatedEvent.getServiceName(),
                        instanceActivatedEvent.getClusterId(), instanceActivatedEvent.getNetworkPartitionId(), instanceActivatedEvent.getPartitionId(), instanceActivatedEvent.getMemberId());

        try {
            TopologyManager.acquireWriteLock();
            member.setStatus(MemberStatus.Activated);
            log.info("member started event adding status activated");
            Cartridge cartridge = FasterLookUpDataHolder.getInstance().
                    getCartridge(instanceActivatedEvent.getServiceName());

            List<PortMapping> portMappings = cartridge.getPortMappings();
            Port port;
            //adding ports to the event
            for (PortMapping portMapping : portMappings) {
                port = new Port(portMapping.getProtocol(),
                        Integer.parseInt(portMapping.getPort()),
                        Integer.parseInt(portMapping.getProxyPort()));
                member.addPort(port);
                memberActivatedEvent.addPort(port);
            }

            memberActivatedEvent.setMemberIp(member.getMemberIp());
            TopologyManager.updateTopology(topology);

        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberActivatedEvent(memberActivatedEvent);
        //publishing data
        CartridgeInstanceDataPublisher.publish(memberActivatedEvent.getMemberId(),
                                            memberActivatedEvent.getPartitionId(),
                                            memberActivatedEvent.getNetworkPartitionId(),
                                            memberActivatedEvent.getClusterId(),
                                            memberActivatedEvent.getServiceName(),
                                            MemberStatus.Activated.toString(),
                                            null);
    }

    public static void handleMemberReadyToShutdown(InstanceReadyToShutdownEvent instanceReadyToShutdownEvent)
                            throws InvalidMemberException, InvalidCartridgeTypeException {
        String memberId = instanceReadyToShutdownEvent.getMemberId();
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceReadyToShutdownEvent.getServiceName());
        //update the status of the member
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist",
                                                     instanceReadyToShutdownEvent.getServiceName()));
        }

        Cluster cluster = service.getCluster(instanceReadyToShutdownEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist",
                                                     instanceReadyToShutdownEvent.getClusterId()));
        }
        Member member = cluster.getMember(instanceReadyToShutdownEvent.getMemberId());
        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist",
                    instanceReadyToShutdownEvent.getMemberId()));
        }
        MemberReadyToShutdownEvent memberReadyToShutdownEvent = new MemberReadyToShutdownEvent(
                                                                instanceReadyToShutdownEvent.getServiceName(),
                                                                instanceReadyToShutdownEvent.getClusterId(),
                                                                instanceReadyToShutdownEvent.getNetworkPartitionId(),
                                                                instanceReadyToShutdownEvent.getPartitionId(),
                                                                instanceReadyToShutdownEvent.getMemberId());
        try {
            TopologyManager.acquireWriteLock();
            member.setStatus(MemberStatus.ReadyToShutDown);
            log.info("member started event adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberReadyToShutdownEvent(memberReadyToShutdownEvent);
        //publishing data
        CartridgeInstanceDataPublisher.publish(instanceReadyToShutdownEvent.getMemberId(),
                                            instanceReadyToShutdownEvent.getPartitionId(),
                                            instanceReadyToShutdownEvent.getNetworkPartitionId(),
                                            instanceReadyToShutdownEvent.getClusterId(),
                                            instanceReadyToShutdownEvent.getServiceName(),
                                            MemberStatus.ReadyToShutDown.toString(),
                                            null);
        //calling the actual termination of the instance
        new CloudControllerServiceImpl().terminateInstance(memberId);

    }

    public static void handleMemberTerminated(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);
        Member member = cluster.getMember(memberId);

        if (member == null) {
            throw new RuntimeException(String.format("Member with nodeID %s does not exist",
                    memberId));
        }

        try {
            TopologyManager.acquireWriteLock();
            cluster.removeMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberTerminatedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId);
    }

    public static void handleMemberSuspended() {
        //TODO
        try {
            TopologyManager.acquireWriteLock();
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }


}
