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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.iaases.kubernetes.KubernetesIaas;
import org.apache.stratos.cloud.controller.messaging.publisher.TopologyEventPublisher;
import org.apache.stratos.cloud.controller.statistics.publisher.BAMUsageDataPublisher;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.kubernetes.client.KubernetesConstants;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
            log.warn("Cartridge list is empty");
            return;
        }

        try {

            TopologyManager.acquireWriteLock();
            for (Cartridge cartridge : cartridgeList) {
                if (!topology.serviceExists(cartridge.getType())) {
                    ServiceType serviceType = cartridge.isMultiTenant() ? ServiceType.MultiTenant :
                            ServiceType.SingleTenant;
                    service = new Service(cartridge.getType(), serviceType);
                    Properties properties = new Properties();

                    try {
                        Property[] propertyArray = null;

                        if (cartridge.getProperties() != null) {
                            if (cartridge.getProperties().getProperties() != null) {
                                propertyArray = cartridge.getProperties().getProperties();
                            }
                        }

                        if (propertyArray != null) {
                            for (Property property : Arrays.asList(propertyArray)) {
                                properties.setProperty(property.getName(), property.getValue());
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
                log.warn(String.format("Cartridge does not exist: [cartridge] %s", cartridge));
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
                    log.warn(String.format("Service does not exist: [service] %s", cartridge.getType()));
                }
            } else {
                log.warn(String.format("Subscription already exists. Hence not removing the service from the " +
                        "topology: [service] %s ", cartridge.getType()));
            }
        }
    }


    public static void handleApplicationClustersCreated(String appId, List<Cluster> appClusters) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            for (Cluster cluster : appClusters) {
                Service service = topology.getService(cluster.getServiceName());
                if (service == null) {
                    log.error(String.format("Service not found in Topology, unable to create Application cluster: " +
                            "[service] %s ", cluster.getServiceName()));
                } else {
                    service.addCluster(cluster);
                    log.info(String.format("Application cluster created in CC topology: [cluster] %s",
                            cluster.getClusterId()));
                }
            }
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }

        log.debug(String.format("Creating cluster port mappings: [application-id] %s", appId));
        for (Cluster cluster : appClusters) {
            String cartridgeType = cluster.getServiceName();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                throw new CloudControllerException(String.format("Cartridge not found: [cartridge-type] %s",
                        cartridgeType));
            }

            for (PortMapping portMapping : cartridge.getPortMappings()) {
                ClusterPortMapping clusterPortMapping = new ClusterPortMapping(appId,
                        cluster.getClusterId(), portMapping.getName(), portMapping.getProtocol(), portMapping.getPort(),
                        portMapping.getProxyPort());
                if (portMapping.getKubernetesPortType() != null) {
                    clusterPortMapping.setKubernetesServiceType(portMapping.getKubernetesPortType());
                }
                CloudControllerContext.getInstance().addClusterPortMapping(clusterPortMapping);
                log.debug(String.format("Cluster port mapping created: [cluster-port-mapping] %s",
                        clusterPortMapping.toString()));
            }
        }

        // Persist cluster port mappings
        CloudControllerContext.getInstance().persist();

        // Send application clusters created event
        TopologyEventPublisher.sendApplicationClustersCreated(appId, appClusters);
    }

    public static void handleApplicationClustersRemoved(String appId,
                                                        Set<ClusterDataHolder> clusterData) {
        TopologyManager.acquireWriteLock();

        CloudControllerContext context = CloudControllerContext.getInstance();
        try {
            Topology topology = TopologyManager.getTopology();

            if (clusterData != null) {
                // remove clusters from CC topology model and remove runtime information
                for (ClusterDataHolder aClusterData : clusterData) {
                    Service aService = topology.getService(aClusterData.getServiceType());
                    if (aService == null) {
                        log.warn(String.format("Service not found, unable to remove cluster: [service] %s [cluster] %s",
                                aClusterData.getServiceType(), aClusterData.getClusterId()));
                    }
                    // remove runtime data
                    context.removeClusterContext(aClusterData.getClusterId());

                    log.info(String.format("Removed application's cluster: [application] %s [cluster] %s from the " +
                            "topology", appId, aClusterData.getClusterId()));
                }
                // persist runtime data changes
                CloudControllerContext.getInstance().persist();
            } else {
                log.info(String.format("No cluster data found for application to remove: [application] %s", appId));
            }

            TopologyManager.updateTopology(topology);

        } finally {
            TopologyManager.releaseWriteLock();
        }

        // Remove cluster port mappings of application
        CloudControllerContext.getInstance().removeClusterPortMappings(appId);
        CloudControllerContext.getInstance().persist();

        TopologyEventPublisher.sendApplicationClustersRemoved(appId, clusterData);

    }

    public static void handleClusterReset(ClusterStatusClusterResetEvent event) {
        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                log.error(String.format("Service not found in Topology, unable to update the cluster status to " +
                        "Created: [service] %s", event.getServiceName()));
                return;
            }
            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                log.error(String.format("Cluster not found in Topology, unable to update status to Created: " +
                        "Created: [service] %s", event.getServiceName()));
                return;
            }
            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn(String.format("Cluster Instance Context is not found for [cluster] %s [instance-id] %s",
                        event.getClusterId(), event.getInstanceId()));
                return;
            }
            ClusterStatus status = ClusterStatus.Created;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info(String.format("Cluster Created adding status started for [cluster] %s",
                        cluster.getClusterId()));
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
                log.error(String.format("Service not found in Topology, unable to update the cluster status to " +
                        "Created: [service] %s", serviceType));
                return;
            }

            Cluster cluster = service.getCluster(clusterId);
            if (cluster == null) {
                log.error(String.format("Cluster not found in Topology, unable to update status to Created: " +
                        "[cluster] %s", clusterId));
                return;
            }

            if (cluster.getInstanceContexts(instanceId) != null) {
                log.warn(String.format("The Instance context for the cluster already exists for [cluster] %s " +
                                "[instance-id] %s",
                        clusterId, instanceId));
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
            log.warn(String.format("Service does not exist: [service] %s",
                    ctxt.getCartridgeType()));
            return;
        }

        if (!service.clusterExists(ctxt.getClusterId())) {
            log.warn(String.format("Cluster does not exist for service: [cluster] %s [service] %s",
                    ctxt.getClusterId(),
                    ctxt.getCartridgeType()));
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
     * @param memberContext MemberContext
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
            log.warn(String.format("Member already exists: [member] %s", memberId));
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
     * @param memberContext MemberContext
     */
    public static void handleMemberInitializedEvent(MemberContext memberContext) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberContext.getCartridgeType());
        if (service == null) {
            log.warn(String.format("Service does not exist: [service] %s",
                    memberContext.getCartridgeType()));
            return;
        }
        if (!service.clusterExists(memberContext.getClusterId())) {
            log.warn(String.format("Cluster does not exist in service: [cluster] %s [service] %s",
                    memberContext.getClusterId(), memberContext.getCartridgeType()));
        }

        Member member = service.getCluster(memberContext.getClusterId()).
                getMember(memberContext.getMemberId());
        if (member == null) {
            log.warn(String.format("Member does not exist: [member] %s", memberContext.getMemberId()));
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
                log.error(String.format("Invalid state transition: [from] %s [to] %s",
                        member.getStatus(), MemberStatus.Initialized));
            } else {

                Cluster cluster = service.getCluster(memberContext.getClusterId());
                String clusterId = cluster.getClusterId();
                ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
                List<KubernetesService> kubernetesServices = clusterContext.getKubernetesServices();

                if (kubernetesServices != null) {
                    cluster.setKubernetesServices(kubernetesServices);
                }

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
        throw new RuntimeException(String.format("Kubernetes service port not found: [cluster-id] %s [port] %s",
                clusterId, portMapping.getPort()));
    }

    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(instanceStartedEvent.getServiceName());
            if (service == null) {
                log.warn(String.format("Service does not exist: [service] %s",
                        instanceStartedEvent.getServiceName()));
                return;
            }
            if (!service.clusterExists(instanceStartedEvent.getClusterId())) {
                log.warn(String.format("Cluster does not exist in service: [cluster] %s [service] %s",
                        instanceStartedEvent.getClusterId(), instanceStartedEvent.getServiceName()));
            }

            Cluster cluster = service.getCluster(instanceStartedEvent.getClusterId());
            Member member = cluster.getMember(instanceStartedEvent.getMemberId());
            if (member == null) {
                log.warn(String.format("Member does not exist: [member] %s",
                        instanceStartedEvent.getMemberId()));
                return;
            }

            try {
                TopologyManager.acquireWriteLock();
                // try update lifecycle state
                if (!member.isStateTransitionValid(MemberStatus.Starting)) {
                    log.error(String.format("Invalid State Transition: [from] %s [to] %s",
                            member.getStatus(), MemberStatus.Starting));
                } else {
                    member.setStatus(MemberStatus.Starting);
                    log.info("Member started event adding status started");

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
            log.warn(String.format("Service does not exist: [service] %s",
                    instanceActivatedEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster does not exist: [cluster] %s",
                    instanceActivatedEvent.getClusterId()));
            return;
        }

        Member member = cluster.getMember(instanceActivatedEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member does not exist: [member] %s",
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
                log.error(String.format("Invalid state transition: [from] %s [to] %s",
                        member.getStatus(), MemberStatus.Active));
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
            log.warn(String.format("Service does not exist: [service] %s",
                    instanceReadyToShutdownEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceReadyToShutdownEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster does not exist: [cluster] %s",
                    instanceReadyToShutdownEvent.getClusterId()));
            return;
        }


        Member member = cluster.getMember(instanceReadyToShutdownEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member does not exist: [member] %s",
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
                log.error(String.format("Invalid State Transition: [from] %s [to] %s ", member.getStatus(),
                        MemberStatus.ReadyToShutDown));
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
            log.warn(String.format("Service does not exist: [service] %s",
                    instanceMaintenanceModeEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceMaintenanceModeEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster does not exist: [cluster] %s",
                    instanceMaintenanceModeEvent.getClusterId()));
            return;
        }

        Member member = cluster.getMember(instanceMaintenanceModeEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member does not exist: [member] %s",
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
                log.error(String.format("Invalid State Transition: [from] %s [to] %s", member.getStatus(),
                        MemberStatus.In_Maintenance));
                return;
            }
            member.setStatus(MemberStatus.In_Maintenance);
            log.info("Member maintenance mode event adding status started");

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
     * @param serviceName        Service Name
     * @param clusterId          Cluster Id
     * @param networkPartitionId Network Partition Id
     * @param partitionId        Partition Id
     * @param memberId           Member Id
     */
    public static void handleMemberTerminated(String serviceName, String clusterId,
                                              String networkPartitionId, String partitionId,
                                              String memberId) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        Properties properties;
        if (service == null) {
            log.warn(String.format("Service does not exist: [service] %s",
                    serviceName));
            return;
        }
        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            log.warn(String.format("Cluster does not exist: [cluster] %s",
                    clusterId));
            return;
        }

        Member member = cluster.getMember(memberId);
        if (member == null) {
            log.warn(String.format("Member does not exist: [member] %s",
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

    public static void handleClusterActivatedEvent(ClusterStatusClusterActivatedEvent
                                                           clusterStatusClusterActivatedEvent) {

        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(clusterStatusClusterActivatedEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            log.warn(String.format("Service does not exist: [service] %s",
                    clusterStatusClusterActivatedEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterStatusClusterActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster does not exist: [cluster] %s",
                    clusterStatusClusterActivatedEvent.getClusterId()));
            return;
        }

        String clusterId = cluster.getClusterId();
        ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
        if (clusterContext == null) {
            log.warn(String.format("Cluster context not found: [cluster-id] %s", clusterId));
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

            if (kubernetesServices != null) {

                try {
                    // Generate access URLs for kubernetes services
                    for (KubernetesService kubernetesService : kubernetesServices) {

                        if (kubernetesService.getServiceType().equals(KubernetesConstants.NODE_PORT)) {
                            // Public IP = Kubernetes minion public IP
                            String[] publicIPs = kubernetesService.getPublicIPs();
                            if ((publicIPs != null) && (publicIPs.length > 0)) {
                                for (String publicIP : publicIPs) {
                                    // There can be a String array with null values
                                    if (publicIP != null) {
                                        // Using type URI since only http, https, ftp, file, jar protocols are supported in URL
                                        URI accessURL = new URI(kubernetesService.getProtocol(), null, publicIP,
                                                kubernetesService.getPort(), null, null, null);
                                        cluster.addAccessUrl(accessURL.toString());
                                        clusterInstanceActivatedEvent.addAccessUrl(accessURL.toString());
                                    } else {
                                        log.error(String.format("Could not create access URL for " +
                                                        "[Kubernetes-service] %s , since Public IP is not available",
                                                kubernetesService.getId()));
                                    }
                                }
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    log.error("Could not create access URLs for Kubernetes services", e);
                }
            }

            ClusterInstance context = cluster.getInstanceContexts(clusterStatusClusterActivatedEvent.getInstanceId());

            if (context == null) {
                log.warn(String.format("Cluster instance context is not found for [cluster] %s [instance-id] %s",
                        clusterStatusClusterActivatedEvent.getClusterId(),
                        clusterStatusClusterActivatedEvent.getInstanceId()));
                return;
            }
            ClusterStatus status = ClusterStatus.Active;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info(String.format("Cluster activated adding status started for [cluster] %s",
                        cluster.getClusterId()));
                TopologyManager.updateTopology(topology);
                // publish event
                TopologyEventPublisher.sendClusterActivatedEvent(clusterInstanceActivatedEvent);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterStatusClusterActivatedEvent.getClusterId(),
                        clusterStatusClusterActivatedEvent.getInstanceId(),
                        context.getStatus(), status));
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
            log.warn(String.format("Service does not exist: [service] %s",
                    clusterInactivateEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterInactivateEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster does not exist: [cluster] %s",
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
                log.warn(String.format("Cluster Instance Context is not found for [cluster] %s [instance-id] %s",
                        clusterInactivateEvent.getClusterId(), clusterInactivateEvent.getInstanceId()));
                return;
            }
            ClusterStatus status = ClusterStatus.Inactive;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info(String.format("Cluster Inactive adding status started for [cluster] %s",
                        cluster.getClusterId()));
                TopologyManager.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterInactivateEvent(clusterInactivatedEvent1);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterInactivateEvent.getClusterId(), clusterInactivateEvent.getInstanceId(),
                        context.getStatus(), status));
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static void handleClusterTerminatedEvent(ClusterStatusClusterTerminatedEvent event) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());

            //update the status of the cluster
            if (service == null) {
                log.warn(String.format("Service does not exist: [service] %s",
                        event.getServiceName()));
                return;
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                log.warn(String.format("Cluster does not exist: [cluster] %s",
                        event.getClusterId()));
                return;
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn(String.format("Cluster Instance Context is not found for [cluster] %s [instance-id] %s",
                        event.getClusterId(), event.getInstanceId()));
                return;
            }
            ClusterStatus status = ClusterStatus.Terminated;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info(String.format("Cluster Terminated adding status started for and removing the cluster " +
                        "instance [cluster] %s", cluster.getClusterId()));
                cluster.removeInstanceContext(event.getInstanceId());
                TopologyManager.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatedEvent clusterTerminatedEvent = new ClusterInstanceTerminatedEvent(
                        event.getAppId(), event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatedEvent(clusterTerminatedEvent);
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

    public static void handleClusterTerminatingEvent(ClusterStatusClusterTerminatingEvent event) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Cluster cluster = topology.getService(event.getServiceName()).
                    getCluster(event.getClusterId());

            if (!cluster.isStateTransitionValid(ClusterStatus.Terminating, event.getInstanceId())) {
                log.error(String.format("Invalid state transfer: [from] %s [to] %s",
                        cluster.getStatus(event.getInstanceId()), ClusterStatus.Terminating));
            }
            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn(String.format("Cluster Instance Context is not found for [cluster] %s [instance-id] %s",
                        event.getClusterId(), event.getInstanceId()));
                return;
            }
            ClusterStatus status = ClusterStatus.Terminating;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info(String.format("Cluster Terminating started for [cluster] %s", cluster.getClusterId()));
                TopologyManager.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatingEvent clusterTerminaingEvent = new ClusterInstanceTerminatingEvent(event.getAppId(),
                        event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatingEvent(clusterTerminaingEvent);

                // Remove kubernetes services if available
                ClusterContext clusterContext =
                        CloudControllerContext.getInstance().getClusterContext(event.getClusterId());
                if (StringUtils.isNotBlank(clusterContext.getKubernetesClusterId())) {
                    KubernetesIaas.removeKubernetesServices(event.getAppId(), event.getClusterId());
                }
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
