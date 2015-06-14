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
package org.apache.stratos.cloud.controller.messaging.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.PortMapping;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.messaging.publisher.TopologyEventPublisher;
import org.apache.stratos.cloud.controller.statistics.publisher.BAMUsageDataPublisher;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.application.ApplicationInstanceTerminatedEvent;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.metadata.client.defaults.DefaultMetaDataServiceClient;
import org.apache.stratos.metadata.client.defaults.MetaDataServiceClient;

import java.util.*;

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
            log.warn(String.format("Cartridge list is empty"));
            return;
        }

        try {

            TopologyManager.acquireWriteLock();
            for (Cartridge cartridge : cartridgeList) {
                if (!topology.serviceExists(cartridge.getType())) {
                    ServiceType serviceType = cartridge.isMultiTenant() ? ServiceType.MultiTenant : ServiceType.SingleTenant;
                    service = new Service(cartridge.getType(), serviceType);
                    Properties properties = new Properties();

                    try {
                        Property[] propertyArray = null;

                        if (cartridge.getProperties() != null) {
                            if (cartridge.getProperties().getProperties() != null) {
                                propertyArray = cartridge.getProperties().getProperties();
                            }
                        }

                        List<Property> propertyList = new ArrayList<Property>();
                        if (propertyArray != null) {
                            propertyList = Arrays.asList(propertyArray);
                            if (propertyList != null) {
                                for (Property property : propertyList) {
                                    properties.setProperty(property.getName(), property.getValue());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }

                    service.setProperties(properties);
                    if (cartridge.getPortMappings() != null) {
                        List<PortMapping> portMappings = Arrays.asList(cartridge.getPortMappings());
                        Port port;
                        //adding ports to the event
                        for (PortMapping portMapping : portMappings) {
                            port = new Port(portMapping.getProtocol(),
                                    portMapping.getPort(), portMapping.getProxyPort());
                            service.addPort(port);
                        }
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
            Service service = topology.getService(cartridge.getType());
            if (service == null) {
                log.warn("Cartridge does not exist [cartidge] " + cartridge);
                return;
            }
            if (service.getClusters().size() == 0) {
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
                    log.warn(String.format("Service %s does not exist..", cartridge.getType()));
                }
            } else {
                log.warn("Subscription already exists. Hence not removing the service:" + cartridge.getType()
                        + " from the topology");
            }
        }
    }

    public static void handleClusterCreated(ClusterStatusClusterCreatedEvent event) {
        TopologyManager.acquireWriteLock();
        Cluster cluster;

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                log.error("Service " + event.getServiceName() +
                        " not found in Topology, unable to update the cluster status to Created");
                return;
            }

            if (service.clusterExists(event.getClusterId())) {
                log.warn("Cluster " + event.getClusterId() + " is already in the Topology ");
                return;
            } else {
                cluster = new Cluster(event.getServiceName(),
                        event.getClusterId(), event.getDeploymentPolicyName(),
                        event.getAutosScalePolicyName(), event.getAppId());
                //cluster.setStatus(Status.Created);
                cluster.setHostNames(event.getHostNames());
                cluster.setTenantRange(event.getTenantRange());
                service.addCluster(cluster);
                TopologyManager.updateTopology(topology);
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendClusterCreatedEvent(cluster);
    }

    public static void handleApplicationClustersCreated(String appId, List<Cluster> appClusters) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            for (Cluster cluster : appClusters) {
                Service service = topology.getService(cluster.getServiceName());
                if (service == null) {
                    log.error("Service " + cluster.getServiceName()
                            + " not found in Topology, unable to create Application cluster");
                } else {
                    service.addCluster(cluster);
                    log.info("Application Cluster " + cluster.getClusterId() + " created in CC topology");
                }
            }

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendApplicationClustersCreated(appId, appClusters);
    }

    public static void handleApplicationClustersRemoved(String appId, Set<ClusterDataHolder> clusterData) {
        TopologyManager.acquireWriteLock();

        List<Cluster> removedClusters = new ArrayList<Cluster>();
        CloudControllerContext context = CloudControllerContext.getInstance();
        try {
            Topology topology = TopologyManager.getTopology();

            if (clusterData != null) {
                // remove clusters from CC topology model and remove runtime information
                for (ClusterDataHolder aClusterData : clusterData) {
                    Service aService = topology.getService(aClusterData.getServiceType());
                    if (aService != null) {
                        removedClusters.add(aService.removeCluster(aClusterData.getClusterId()));
                    } else {
                        log.warn("Service " + aClusterData.getServiceType() + " not found, unable to remove Cluster " + aClusterData.getClusterId());
                    }
                    // remove runtime data
                    context.removeClusterContext(aClusterData.getClusterId());

                    log.info("Removed application [ " + appId + " ]'s Cluster [ " + aClusterData.getClusterId() + " ] from the topology");
                }
                // persist runtime data changes
                CloudControllerContext.getInstance().persist();
            } else {
                log.info("No cluster data found for application " + appId + " to remove");
            }

            TopologyManager.updateTopology(topology);

        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendApplicationClustersRemoved(appId, clusterData);

    }

    public static void handleClusterReset(ClusterStatusClusterResetEvent event) {
        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                log.error("Service " + event.getServiceName() +
                        " not found in Topology, unable to update the cluster status to Created");
                return;
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                log.error("Cluster " + event.getClusterId() + " not found in Topology, unable to update " +
                        "status to Created");
                return;
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Created;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Created adding status started for" + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterResetEvent(event.getAppId(), event.getServiceName(),
                        event.getClusterId(), event.getInstanceId());
            } else {
                log.warn(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        event.getClusterId(), event.getInstanceId(),
                        context.getStatus(), status));
            }

        } finally {
            TopologyManager.releaseWriteLock();
        }


    }

    public static void handleClusterInstanceCreated(String serviceType, String clusterId,
                                                    String alias, String instanceId, String partitionId,
                                                    String networkPartitionId) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(serviceType);
            if (service == null) {
                log.error("Service " + serviceType +
                        " not found in Topology, unable to update the cluster status to Created");
                return;
            }

            Cluster cluster = service.getCluster(clusterId);
            if (cluster == null) {
                log.error("Cluster " + clusterId + " not found in Topology, unable to update " +
                        "status to Created");
                return;
            }

            if (cluster.getInstanceContexts(instanceId) != null) {
                log.warn("The Instance context for the cluster already exists for [cluster] " +
                        clusterId + " [instance-id] " + instanceId);
                return;
            }

            ClusterInstance clusterInstance = new ClusterInstance(alias, clusterId, instanceId);
            clusterInstance.setNetworkPartitionId(networkPartitionId);
            clusterInstance.setPartitionId(partitionId);
            cluster.addInstanceContext(instanceId, clusterInstance);
            TopologyManager.updateTopology(topology);

            ClusterInstanceCreatedEvent clusterInstanceCreatedEvent =
                    new ClusterInstanceCreatedEvent(serviceType, clusterId,
                            clusterInstance);
            clusterInstanceCreatedEvent.setPartitionId(partitionId);
            TopologyEventPublisher.sendClusterInstanceCreatedEvent(clusterInstanceCreatedEvent);

        } finally {
            TopologyManager.releaseWriteLock();
        }
    }


    public static void handleClusterRemoved(ClusterContext ctxt) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(ctxt.getCartridgeType());
        String deploymentPolicy;
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    ctxt.getCartridgeType()));
            return;
        }

        if (!service.clusterExists(ctxt.getClusterId())) {
            log.warn(String.format("Cluster %s does not exist for service %s",
                    ctxt.getClusterId(),
                    ctxt.getCartridgeType()));
            return;
        }

        try {
            TopologyManager.acquireWriteLock();
            Cluster cluster = service.removeCluster(ctxt.getClusterId());
            deploymentPolicy = cluster.getDeploymentPolicyName();
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendClusterRemovedEvent(ctxt, deploymentPolicy);
    }

    /**
     * Add member object to the topology and publish member created event
     *
     * @param memberContext
     */
    public static void handleMemberCreatedEvent(MemberContext memberContext) {
        Topology topology = TopologyManager.getTopology();

        Service service = topology.getService(memberContext.getCartridgeType());
        String clusterId = memberContext.getClusterId();
        Cluster cluster = service.getCluster(clusterId);
        String memberId = memberContext.getMemberId();
        String clusterInstanceId = memberContext.getClusterInstanceId();
        String networkPartitionId = memberContext.getNetworkPartitionId();
        String partitionId = memberContext.getPartition().getId();
        String lbClusterId = memberContext.getLbClusterId();
        long initTime = memberContext.getInitTime();

        if (cluster.memberExists(memberId)) {
            log.warn(String.format("Member %s already exists", memberId));
            return;
        }

        try {
            TopologyManager.acquireWriteLock();
            Member member = new Member(service.getServiceName(), clusterId, memberId, clusterInstanceId,
                    networkPartitionId, partitionId, memberContext.getLoadBalancingIPType(), initTime);
            member.setStatus(MemberStatus.Created);
            member.setLbClusterId(lbClusterId);
            member.setProperties(CloudControllerUtil.toJavaUtilProperties(memberContext.getProperties()));
            cluster.addMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendMemberCreatedEvent(memberContext);
    }

    /**
     * Update member status to initialized and publish member initialized event
     *
     * @param memberContext
     */
    public static void handleMemberInitializedEvent(MemberContext memberContext) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberContext.getCartridgeType());
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    memberContext.getCartridgeType()));
            return;
        }
        if (!service.clusterExists(memberContext.getClusterId())) {
            log.warn(String.format("Cluster %s does not exist in service %s",
                    memberContext.getClusterId(),
                    memberContext.getCartridgeType()));
            return;
        }

        Member member = service.getCluster(memberContext.getClusterId()).
                getMember(memberContext.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    memberContext.getMemberId()));
            return;
        }

        try {
            TopologyManager.acquireWriteLock();

            // Set ip addresses
            member.setDefaultPrivateIP(memberContext.getDefaultPrivateIP());
            if (memberContext.getPrivateIPs() != null) {
                member.setMemberPrivateIPs(Arrays.asList(memberContext.getPrivateIPs()));
            }
            member.setDefaultPublicIP(memberContext.getDefaultPublicIP());
            if (memberContext.getPublicIPs() != null) {
                member.setMemberPublicIPs(Arrays.asList(memberContext.getPublicIPs()));
            }

            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Initialized)) {
                log.error("Invalid state transition from " + member.getStatus() + " to " +
                        MemberStatus.Initialized);
                return;
            } else {
                member.setStatus(MemberStatus.Initialized);
                log.info("Member status updated to initialized");

                TopologyManager.updateTopology(topology);

                TopologyEventPublisher.sendMemberInitializedEvent(memberContext);
                //publishing data
                BAMUsageDataPublisher.publish(memberContext.getMemberId(),
                        memberContext.getPartition().getId(),
                        memberContext.getNetworkPartitionId(),
                        memberContext.getClusterId(),
                        memberContext.getCartridgeType(),
                        MemberStatus.Initialized.toString(),
                        null);
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    private static int findKubernetesServicePort(String clusterId, List<KubernetesService> kubernetesServices,
                                                 PortMapping portMapping) {
        for (KubernetesService kubernetesService : kubernetesServices) {
            if (kubernetesService.getProtocol().equals(portMapping.getProtocol())) {
                return kubernetesService.getPort();
            }
        }
        throw new RuntimeException("Kubernetes service port not found: [cluster-id] " + clusterId + " [port] "
                + portMapping.getPort());
    }

    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(instanceStartedEvent.getServiceName());
            if (service == null) {
                log.warn(String.format("Service %s does not exist",
                        instanceStartedEvent.getServiceName()));
                return;
            }
            if (!service.clusterExists(instanceStartedEvent.getClusterId())) {
                log.warn(String.format("Cluster %s does not exist in service %s",
                        instanceStartedEvent.getClusterId(),
                        instanceStartedEvent.getServiceName()));
                return;
            }

            Cluster cluster = service.getCluster(instanceStartedEvent.getClusterId());
            Member member = cluster.getMember(instanceStartedEvent.getMemberId());
            if (member == null) {
                log.warn(String.format("Member %s does not exist",
                        instanceStartedEvent.getMemberId()));
                return;
            }

            try {
                TopologyManager.acquireWriteLock();
                // try update lifecycle state
                if (!member.isStateTransitionValid(MemberStatus.Starting)) {
                    log.error("Invalid State Transition from " + member.getStatus() + " to " +
                            MemberStatus.Starting);
                    return;
                } else {
                    member.setStatus(MemberStatus.Starting);
                    log.info("member started event adding status started");

                    TopologyManager.updateTopology(topology);
                    //memberStartedEvent.
                    TopologyEventPublisher.sendMemberStartedEvent(instanceStartedEvent);
                    //publishing data
                    BAMUsageDataPublisher.publish(instanceStartedEvent.getMemberId(),
                            instanceStartedEvent.getPartitionId(),
                            instanceStartedEvent.getNetworkPartitionId(),
                            instanceStartedEvent.getClusterId(),
                            instanceStartedEvent.getServiceName(),
                            MemberStatus.Starting.toString(),
                            null);
                }
            } finally {
                TopologyManager.releaseWriteLock();
            }
        } catch (Exception e) {
            String message = String.format("Could not handle member started event: [application-id] %s " +
                            "[service-name] %s [member-id] %s", instanceStartedEvent.getApplicationId(),
                    instanceStartedEvent.getServiceName(), instanceStartedEvent.getMemberId());
            log.warn(message, e);
        }
    }

    public static void handleMemberActivated(InstanceActivatedEvent instanceActivatedEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceActivatedEvent.getServiceName());
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    instanceActivatedEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    instanceActivatedEvent.getClusterId()));
            return;
        }

        Member member = cluster.getMember(instanceActivatedEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceActivatedEvent.getMemberId()));
            return;
        }

        MemberActivatedEvent memberActivatedEvent = new MemberActivatedEvent(
                instanceActivatedEvent.getServiceName(),
                instanceActivatedEvent.getClusterId(),
                instanceActivatedEvent.getClusterInstanceId(),
                instanceActivatedEvent.getMemberId(),
                instanceActivatedEvent.getNetworkPartitionId(),
                instanceActivatedEvent.getPartitionId());

        // grouping - set grouid
        //TODO
        memberActivatedEvent.setApplicationId(null);
        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Active)) {
                log.error("Invalid state transition from [" + member.getStatus() + "] to [" + MemberStatus.Active + "]");
                return;
            } else {
                member.setStatus(MemberStatus.Active);

                // Set member ports
                try {
                    Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(service.getServiceName());
                    if (cartridge == null) {
                        throw new RuntimeException(String.format("Cartridge not found: [cartridge-type] %s",
                                service.getServiceName()));
                    }

                    Port port;
                    int portValue;
                    List<PortMapping> portMappings = Arrays.asList(cartridge.getPortMappings());
                    String clusterId = cluster.getClusterId();
                    ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
                    List<KubernetesService> kubernetesServices = clusterContext.getKubernetesServices();

                    for (PortMapping portMapping : portMappings) {
                        if (kubernetesServices != null) {
                            portValue = findKubernetesServicePort(clusterId, kubernetesServices, portMapping);
                        } else {
                            portValue = portMapping.getPort();
                        }
                        port = new Port(portMapping.getProtocol(), portValue, portMapping.getProxyPort());
                        member.addPort(port);
                        memberActivatedEvent.addPort(port);
                    }
                } catch (Exception e) {
                    String message = String.format("Could not add member ports: [service-name] %s [member-id] %s",
                            memberActivatedEvent.getServiceName(), memberActivatedEvent.getMemberId());
                    log.error(message, e);
                }

                // Set member ip addresses
                memberActivatedEvent.setDefaultPrivateIP(member.getDefaultPrivateIP());
                memberActivatedEvent.setMemberPrivateIPs(member.getMemberPrivateIPs());
                memberActivatedEvent.setDefaultPublicIP(member.getDefaultPublicIP());
                memberActivatedEvent.setMemberPublicIPs(member.getMemberPublicIPs());
                TopologyManager.updateTopology(topology);

                // Publish member activated event
                TopologyEventPublisher.sendMemberActivatedEvent(memberActivatedEvent);

                // Publish statistics data
                BAMUsageDataPublisher.publish(memberActivatedEvent.getMemberId(),
                        memberActivatedEvent.getPartitionId(),
                        memberActivatedEvent.getNetworkPartitionId(),
                        memberActivatedEvent.getClusterId(),
                        memberActivatedEvent.getServiceName(),
                        MemberStatus.Active.toString(),
                        null);
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static void handleMemberReadyToShutdown(InstanceReadyToShutdownEvent instanceReadyToShutdownEvent)
            throws InvalidMemberException, InvalidCartridgeTypeException {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceReadyToShutdownEvent.getServiceName());
        //update the status of the member
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    instanceReadyToShutdownEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceReadyToShutdownEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    instanceReadyToShutdownEvent.getClusterId()));
            return;
        }


        Member member = cluster.getMember(instanceReadyToShutdownEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceReadyToShutdownEvent.getMemberId()));
            return;
        }
        MemberReadyToShutdownEvent memberReadyToShutdownEvent = new MemberReadyToShutdownEvent(
                instanceReadyToShutdownEvent.getServiceName(),
                instanceReadyToShutdownEvent.getClusterId(),
                instanceReadyToShutdownEvent.getClusterInstanceId(),
                instanceReadyToShutdownEvent.getMemberId(),
                instanceReadyToShutdownEvent.getNetworkPartitionId(),
                instanceReadyToShutdownEvent.getPartitionId());
        try {
            TopologyManager.acquireWriteLock();

            if (!member.isStateTransitionValid(MemberStatus.ReadyToShutDown)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " +
                        MemberStatus.ReadyToShutDown);
                return;
            }
            member.setStatus(MemberStatus.ReadyToShutDown);
            log.info("Member Ready to shut down event adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberReadyToShutdownEvent(memberReadyToShutdownEvent);
        //publishing data
        BAMUsageDataPublisher.publish(instanceReadyToShutdownEvent.getMemberId(),
                instanceReadyToShutdownEvent.getPartitionId(),
                instanceReadyToShutdownEvent.getNetworkPartitionId(),
                instanceReadyToShutdownEvent.getClusterId(),
                instanceReadyToShutdownEvent.getServiceName(),
                MemberStatus.ReadyToShutDown.toString(),
                null);
        //termination of particular instance will be handled by autoscaler
    }

    public static void handleMemberMaintenance(InstanceMaintenanceModeEvent instanceMaintenanceModeEvent)
            throws InvalidMemberException, InvalidCartridgeTypeException {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceMaintenanceModeEvent.getServiceName());
        //update the status of the member
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    instanceMaintenanceModeEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceMaintenanceModeEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    instanceMaintenanceModeEvent.getClusterId()));
            return;
        }

        Member member = cluster.getMember(instanceMaintenanceModeEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceMaintenanceModeEvent.getMemberId()));
            return;
        }


        MemberMaintenanceModeEvent memberMaintenanceModeEvent = new MemberMaintenanceModeEvent(
                instanceMaintenanceModeEvent.getServiceName(),
                instanceMaintenanceModeEvent.getClusterId(),
                instanceMaintenanceModeEvent.getClusterInstanceId(),
                instanceMaintenanceModeEvent.getMemberId(),
                instanceMaintenanceModeEvent.getNetworkPartitionId(),
                instanceMaintenanceModeEvent.getPartitionId());
        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.In_Maintenance)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to "
                        + MemberStatus.In_Maintenance);
                return;
            }
            member.setStatus(MemberStatus.In_Maintenance);
            log.info("member maintenance mode event adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendMemberMaintenanceModeEvent(memberMaintenanceModeEvent);

    }

    /**
     * Remove member from topology and send member terminated event.
     *
     * @param serviceName
     * @param clusterId
     * @param networkPartitionId
     * @param partitionId
     * @param memberId
     */
    public static void handleMemberTerminated(String serviceName, String clusterId,
                                              String networkPartitionId, String partitionId,
                                              String memberId) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        Properties properties;
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    serviceName));
            return;
        }
        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    clusterId));
            return;
        }

        Member member = cluster.getMember(memberId);
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    memberId));
            return;
        }

        String clusterInstanceId = member.getClusterInstanceId();

        try {
            TopologyManager.acquireWriteLock();
            properties = member.getProperties();
            cluster.removeMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        /* @TODO leftover from grouping_poc*/
        String groupAlias = null;
        TopologyEventPublisher.sendMemberTerminatedEvent(serviceName, clusterId, memberId,
                clusterInstanceId, networkPartitionId,
                partitionId, properties, groupAlias);
    }

    public static void handleMemberSuspended() {
        //TODO
        try {
            TopologyManager.acquireWriteLock();
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static void handleClusterActivatedEvent(ClusterStatusClusterActivatedEvent clusterStatusClusterActivatedEvent) {

        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(clusterStatusClusterActivatedEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    clusterStatusClusterActivatedEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterStatusClusterActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    clusterStatusClusterActivatedEvent.getClusterId()));
            return;
        }

        String clusterId = cluster.getClusterId();
        ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
        if (clusterContext == null) {
            log.warn("Cluster context not found: [cluster-id] " + clusterId);
            return;
        }

        ClusterInstanceActivatedEvent clusterInstanceActivatedEvent =
                new ClusterInstanceActivatedEvent(
                        clusterStatusClusterActivatedEvent.getAppId(),
                        clusterStatusClusterActivatedEvent.getServiceName(),
                        clusterStatusClusterActivatedEvent.getClusterId(),
                        clusterStatusClusterActivatedEvent.getInstanceId());
        try {
            TopologyManager.acquireWriteLock();
            List<KubernetesService> kubernetesServices = clusterContext.getKubernetesServices();
            cluster.setKubernetesServices(kubernetesServices);
            clusterInstanceActivatedEvent.setKubernetesServices(kubernetesServices);

            ClusterInstance context = cluster.getInstanceContexts(clusterStatusClusterActivatedEvent.getInstanceId());
            if (context == null) {
                log.warn("Cluster instance context is not found for [cluster] " +
                        clusterStatusClusterActivatedEvent.getClusterId() + " [instance-id] " +
                        clusterStatusClusterActivatedEvent.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Active;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster activated adding status started for " + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                // publish event
                TopologyEventPublisher.sendClusterActivatedEvent(clusterInstanceActivatedEvent);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterStatusClusterActivatedEvent.getClusterId(), clusterStatusClusterActivatedEvent.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }

    }

    public static void handleClusterInactivateEvent(
            ClusterStatusClusterInactivateEvent clusterInactivateEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(clusterInactivateEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    clusterInactivateEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterInactivateEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    clusterInactivateEvent.getClusterId()));
            return;
        }

        ClusterInstanceInactivateEvent clusterInactivatedEvent1 =
                new ClusterInstanceInactivateEvent(
                        clusterInactivateEvent.getAppId(),
                        clusterInactivateEvent.getServiceName(),
                        clusterInactivateEvent.getClusterId(),
                        clusterInactivateEvent.getInstanceId());
        try {
            TopologyManager.acquireWriteLock();
            ClusterInstance context = cluster.getInstanceContexts(clusterInactivateEvent.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        clusterInactivateEvent.getClusterId() + " [instance-id] " +
                        clusterInactivateEvent.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Inactive;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Inactive adding status started for" + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterInactivateEvent(clusterInactivatedEvent1);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterInactivateEvent.getClusterId(), clusterInactivateEvent.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }


    private static void deleteAppResourcesFromMetadataService(ApplicationInstanceTerminatedEvent event) {
        try {
            MetaDataServiceClient metadataClient = new DefaultMetaDataServiceClient();
            metadataClient.deleteApplicationProperties(event.getAppId());
        } catch (Exception e) {
            log.error("Error occurred while deleting the application resources frm metadata service ", e);
        }
    }

    public static void handleClusterTerminatedEvent(ClusterStatusClusterTerminatedEvent event) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());

            //update the status of the cluster
            if (service == null) {
                log.warn(String.format("Service %s does not exist",
                        event.getServiceName()));
                return;
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                log.warn(String.format("Cluster %s does not exist",
                        event.getClusterId()));
                return;
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Terminated;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Terminated adding status started for and removing the cluster instance"
                        + cluster.getClusterId());
                cluster.removeInstanceContext(event.getInstanceId());
                TopologyManager.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatedEvent clusterTerminatedEvent = new ClusterInstanceTerminatedEvent(event.getAppId(),
                        event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatedEvent(clusterTerminatedEvent);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        event.getClusterId(), event.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }


    }

    public static void handleClusterTerminatingEvent(ClusterStatusClusterTerminatingEvent event) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Cluster cluster = topology.getService(event.getServiceName()).
                    getCluster(event.getClusterId());

            if (!cluster.isStateTransitionValid(ClusterStatus.Terminating, event.getInstanceId())) {
                log.error("Invalid state transfer from " + cluster.getStatus(event.getInstanceId()) + " to " +
                        ClusterStatus.Terminating);
            }
            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Terminating;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Terminating started for " + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatingEvent clusterTerminaingEvent = new ClusterInstanceTerminatingEvent(event.getAppId(),
                        event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatingEvent(clusterTerminaingEvent);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        event.getClusterId(), event.getInstanceId(),
                        context.getStatus(), status));
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }
}
