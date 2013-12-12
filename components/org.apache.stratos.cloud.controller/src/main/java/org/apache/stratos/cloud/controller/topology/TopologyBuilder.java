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
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
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
            Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
            
            String property = props.getProperty(Constants.IS_LOAD_BALANCER);
            boolean isLb = property != null ? Boolean.parseBoolean(property) : false;
            
            Cluster cluster;
            if (service.clusterExists(registrant.getClusterId())) {
                // update the cluster
                cluster = service.getCluster(registrant.getClusterId());
                cluster.addHostName(registrant.getHostName());
                cluster.setTenantRange(registrant.getTenantRange());
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
            } else {
                cluster = new Cluster(registrant.getCartridgeType(), registrant.getClusterId(),
                                      registrant.getDeploymentPolicyName(), registrant.getAutoScalerPolicyName());
                cluster.addHostName(registrant.getHostName());
                cluster.setTenantRange(registrant.getTenantRange());
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
                service.addCluster(cluster);
            }
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
            Member member = new Member(serviceName, clusterId, partitionId, memberId);
            member.setStatus(MemberStatus.Created);
            member.setMemberIp(privateIp);
            cluster.addMember(member);
            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendInstanceSpawnedEvent(serviceName, clusterId, partitionId, memberId);

    }

    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
        Topology topology = TopologyManager.getInstance().getTopology();
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
            TopologyManager.getInstance().acquireWriteLock();
            member.setStatus(MemberStatus.Starting);
            log.info("member started event adding status started");

            TopologyManager.getInstance().updateTopology(topology);
        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        //memberStartedEvent.
        TopologyEventSender.sendMemberStartedEvent(instanceStartedEvent);
    }

    public static void handleMemberActivated(InstanceActivatedEvent instanceActivatedEvent) {
        Topology topology = TopologyManager.getInstance().getTopology();
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

        org.apache.stratos.messaging.event.topology.MemberActivatedEvent memberActivatedEvent =
                new org.apache.stratos.messaging.event.topology.MemberActivatedEvent(instanceActivatedEvent.getServiceName(),
                        instanceActivatedEvent.getClusterId(), instanceActivatedEvent.getPartitionId(), instanceActivatedEvent.getMemberId());

        try {
            TopologyManager.getInstance().acquireWriteLock();
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
            TopologyManager.getInstance().updateTopology(topology);

        } finally {
            TopologyManager.getInstance().releaseWriteLock();
        }
        TopologyEventSender.sendMemberActivatedEvent(memberActivatedEvent);
    }

    public static void handleMemberTerminated(String serviceName, String clusterId, String partitionId, String memberId) {
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
        TopologyEventSender.sendMemberTerminatedEvent(serviceName, clusterId, partitionId, memberId);
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
