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

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.iaases.kubernetes.KubernetesIaas;
import org.apache.stratos.cloud.controller.messaging.publisher.TopologyEventPublisher;
import org.apache.stratos.cloud.controller.statistics.publisher.CloudControllerPublisherFactory;
import org.apache.stratos.cloud.controller.statistics.publisher.MemberInformationPublisher;
import org.apache.stratos.cloud.controller.statistics.publisher.MemberStatusPublisher;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.statistics.publisher.StatisticsPublisherType;
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
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * this is to manipulate the received events by cloud controller
 * and build the complete topology with the events received
 */
public class TopologyBuilder {
    private static final Log log = LogFactory.getLog(TopologyBuilder.class);
    private static MemberInformationPublisher memInfoPublisher = CloudControllerPublisherFactory.
            createMemberInformationPublisher(StatisticsPublisherType.WSO2DAS);

    private static MemberStatusPublisher memStatusPublisher = CloudControllerPublisherFactory.
            createMemberStatusPublisher(StatisticsPublisherType.WSO2DAS);

    public static void handleServiceCreated(List<Cartridge> cartridgeList) throws RegistryException {
        Service service;
        Topology topology = TopologyHolder.getTopology();
        if (cartridgeList == null) {
            throw new RuntimeException("Cartridge list is empty");
        }
        TopologyHolder.acquireWriteLock();
        try {
            for (Cartridge cartridge : cartridgeList) {
                if (!topology.serviceExists(cartridge.getType())) {
                    ServiceType serviceType = cartridge.isMultiTenant() ?
                            ServiceType.MultiTenant :
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
                        List<Property> propertyList = new ArrayList<Property>();
                        if (propertyArray != null) {
                            propertyList = Arrays.asList(propertyArray);
                            for (Property property : propertyList) {
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
                            port = new Port(portMapping.getProtocol(), portMapping.getPort(),
                                    portMapping.getProxyPort());
                            service.addPort(port);
                        }
                    }
                    topology.addService(service);
                    TopologyHolder.updateTopology(topology);
                }
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }
        TopologyEventPublisher.sendServiceCreateEvent(cartridgeList);
    }

    public static void handleServiceRemoved(List<Cartridge> cartridgeList) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        for (Cartridge cartridge : cartridgeList) {
            Service service = topology.getService(cartridge.getType());
            if (service == null) {
                throw new RuntimeException(String.format("Service %s does not exist", cartridge.getType()));
            }
            if (service.getClusters().size() == 0) {
                TopologyHolder.acquireWriteLock();
                try {
                    topology.removeService(cartridge.getType());
                    TopologyHolder.updateTopology(topology);
                } finally {
                    TopologyHolder.releaseWriteLock();
                }
                TopologyEventPublisher.sendServiceRemovedEvent(cartridgeList);
            } else {
                log.warn("Subscription already exists. Hence not removing the service:" + cartridge.getType()
                        + " from the topology");
            }
        }
    }

    public static void handleApplicationClustersCreated(String appId, List<Cluster> appClusters)
            throws RegistryException {
        TopologyHolder.acquireWriteLock();
        try {
            Topology topology = TopologyHolder.getTopology();
            for (Cluster cluster : appClusters) {
                Service service = topology.getService(cluster.getServiceName());
                if (service == null) {
                    throw new RuntimeException(
                            "Service " + cluster.getServiceName() + " not found in topology, unable to create cluster");
                }
                service.addCluster(cluster);
                log.info("Cluster created: [cluster] " + cluster.getClusterId());
            }
            TopologyHolder.updateTopology(topology);
        } finally {
            TopologyHolder.releaseWriteLock();
        }

        log.debug("Creating cluster port mappings: [application-id] " + appId);
        for (Cluster cluster : appClusters) {
            String cartridgeType = cluster.getServiceName();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                throw new CloudControllerException("Cartridge not found: [cartridge-type] " + cartridgeType);
            }

            for (PortMapping portMapping : cartridge.getPortMappings()) {
                ClusterPortMapping clusterPortMapping = new ClusterPortMapping(appId, cluster.getClusterId(),
                        portMapping.getName(), portMapping.getProtocol(), portMapping.getPort(),
                        portMapping.getProxyPort());
                if (portMapping.getKubernetesPortType() != null) {
                    clusterPortMapping.setKubernetesPortType(portMapping.getKubernetesPortType());
                }
                CloudControllerContext.getInstance().addClusterPortMapping(clusterPortMapping);
                log.debug("Cluster port mapping created: " + clusterPortMapping.toString());
            }
        }

        // Persist cluster port mappings
        CloudControllerContext.getInstance().persist();

        // Send application clusters created event
        TopologyEventPublisher.sendApplicationClustersCreated(appId, appClusters);
    }

    public static void handleApplicationClustersRemoved(String appId, Set<ClusterDataHolder> clusterData)
            throws RegistryException {
        TopologyHolder.acquireWriteLock();
        CloudControllerContext context = CloudControllerContext.getInstance();
        try {
            Topology topology = TopologyHolder.getTopology();

            if (clusterData != null) {
                // remove clusters from CC topology model and remove runtime information
                for (ClusterDataHolder aClusterData : clusterData) {
                    Service aService = topology.getService(aClusterData.getServiceType());
                    if (aService == null) {
                        log.warn("Service " + aClusterData.getServiceType() + " not found, " +
                                "unable to remove Cluster " + aClusterData.getClusterId());
                    }
                    // remove runtime data
                    context.removeClusterContext(aClusterData.getClusterId());

                    log.info("Removed application [ " + appId + " ]'s Cluster " +
                            "[ " + aClusterData.getClusterId() + " ] from the topology");
                }
                // persist runtime data changes
                CloudControllerContext.getInstance().persist();
            } else {
                log.info("No cluster data found for application " + appId + " to remove");
            }
            TopologyHolder.updateTopology(topology);
        } finally {
            TopologyHolder.releaseWriteLock();
        }

        // Remove cluster port mappings of application
        CloudControllerContext.getInstance().removeClusterPortMappings(appId);
        CloudControllerContext.getInstance().persist();
        TopologyEventPublisher.sendApplicationClustersRemoved(appId, clusterData);
    }

    public static void handleClusterReset(ClusterStatusClusterResetEvent event) throws RegistryException {
        TopologyHolder.acquireWriteLock();
        try {
            Topology topology = TopologyHolder.getTopology();
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                throw new RuntimeException("Service " + event.getServiceName() +
                        " not found in Topology, unable to update the cluster status to Created");
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                throw new RuntimeException(
                        "Cluster " + event.getClusterId() + " not found in Topology, unable to update " +
                                "status to Created");
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                throw new RuntimeException("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
            }
            ClusterStatus status = ClusterStatus.Created;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Created adding status started for" + cluster.getClusterId());
                TopologyHolder.updateTopology(topology);
                //publishing data
                TopologyEventPublisher
                        .sendClusterResetEvent(event.getAppId(), event.getServiceName(), event.getClusterId(),
                                event.getInstanceId());
            } else {
                log.warn(String.format("Cluster state transition is not valid: [cluster-id] %s "
                                + " [instance-id] %s [current-status] %s [status-requested] %s", event.getClusterId(),
                        event.getInstanceId(), context.getStatus(), status));
            }

        } finally {
            TopologyHolder.releaseWriteLock();
        }

    }

    public static void handleClusterInstanceCreated(String serviceType, String clusterId, String alias,
            String instanceId, String partitionId, String networkPartitionId) throws RegistryException {
        TopologyHolder.acquireWriteLock();
        try {
            Topology topology = TopologyHolder.getTopology();
            Service service = topology.getService(serviceType);
            if (service == null) {
                throw new RuntimeException("Service " + serviceType +
                        " not found in Topology, unable to update the cluster status to Created");
            }
            Cluster cluster = service.getCluster(clusterId);
            if (cluster == null) {
                throw new RuntimeException("Cluster " + clusterId + " not found in Topology, unable to update " +
                        "status to Created");
            }
            if (cluster.getInstanceContexts(instanceId) != null) {
                throw new RuntimeException("The Instance context for the cluster already exists for [cluster] " +
                        clusterId + " [instance-id] " + instanceId);
            }
            ClusterInstance clusterInstance = new ClusterInstance(alias, clusterId, instanceId);
            clusterInstance.setNetworkPartitionId(networkPartitionId);
            clusterInstance.setPartitionId(partitionId);
            cluster.addInstanceContext(instanceId, clusterInstance);
            TopologyHolder.updateTopology(topology);
            ClusterInstanceCreatedEvent clusterInstanceCreatedEvent = new ClusterInstanceCreatedEvent(serviceType,
                    clusterId, clusterInstance);
            clusterInstanceCreatedEvent.setPartitionId(partitionId);
            TopologyEventPublisher.sendClusterInstanceCreatedEvent(clusterInstanceCreatedEvent);
        } finally {
            TopologyHolder.releaseWriteLock();
        }
    }

    public static void handleClusterRemoved(ClusterContext ctxt) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(ctxt.getCartridgeType());
        String deploymentPolicy;
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist", ctxt.getCartridgeType()));
        }
        if (!service.clusterExists(ctxt.getClusterId())) {
            throw new RuntimeException(String.format("Cluster %s does not exist for service %s", ctxt.getClusterId(),
                    ctxt.getCartridgeType()));
        }
        TopologyHolder.acquireWriteLock();
        try {
            Cluster cluster = service.removeCluster(ctxt.getClusterId());
            deploymentPolicy = cluster.getDeploymentPolicyName();
            TopologyHolder.updateTopology(topology);
        } finally {
            TopologyHolder.releaseWriteLock();
        }
        TopologyEventPublisher.sendClusterRemovedEvent(ctxt, deploymentPolicy);
    }

    /**
     * Add member object to the topology and publish member created event
     *
     * @param memberContext
     */
    public static void handleMemberCreatedEvent(MemberContext memberContext) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(memberContext.getCartridgeType());
        String clusterId = memberContext.getClusterId();
        Cluster cluster = service.getCluster(clusterId);
        String applicationId = service.getCluster(memberContext.getClusterId()).getAppId();
        String memberId = memberContext.getMemberId();
        String clusterInstanceId = memberContext.getClusterInstanceId();
        String networkPartitionId = memberContext.getNetworkPartitionId();
        String partitionId = memberContext.getPartition().getId();
        String clusterAlias = CloudControllerUtil.getAliasFromClusterId(memberContext.getClusterId());
        String lbClusterId = memberContext.getLbClusterId();
        long initTime = memberContext.getInitTime();
        if (cluster.memberExists(memberId)) {
            throw new RuntimeException(String.format("Member %s already exists", memberId));
        }
        TopologyHolder.acquireWriteLock();
        try {
            Member member = new Member(service.getServiceName(), clusterId, memberId, clusterInstanceId,
                    networkPartitionId, partitionId, memberContext.getLoadBalancingIPType(), initTime);
            member.setStatus(MemberStatus.Created);
            member.setLbClusterId(lbClusterId);
            member.setProperties(CloudControllerUtil.toJavaUtilProperties(memberContext.getProperties()));
            cluster.addMember(member);
            TopologyHolder.updateTopology(topology);

            //member created time
            Long timestamp = System.currentTimeMillis();
            //publishing member status to DAS
            if (memStatusPublisher.isEnabled()) {
                if (log.isDebugEnabled()) {
                    log.debug("Publishing Member Status to DAS");
                }
                memStatusPublisher.publish(timestamp, applicationId, memberContext.getClusterId(), clusterAlias,
                        memberContext.getClusterInstanceId(), memberContext.getCartridgeType(),
                        memberContext.getNetworkPartitionId(), memberContext.getPartition().getId(),
                        memberContext.getMemberId(), MemberStatus.Created.toString());
            }

        } finally {
            TopologyHolder.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberCreatedEvent(memberContext);
    }

    /**
     * Update member status to initialized and publish member initialized event
     *
     * @param memberContext
     */
    public static void handleMemberInitializedEvent(MemberContext memberContext) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(memberContext.getCartridgeType());
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist", memberContext.getCartridgeType()));
        }
        if (!service.clusterExists(memberContext.getClusterId())) {
            throw new RuntimeException(
                    String.format("Cluster %s does not exist for service %s", memberContext.getClusterId(),
                            memberContext.getCartridgeType()));
        }
        String applicationId = service.getCluster(memberContext.getClusterId()).getAppId();
        String clusterAlias = CloudControllerUtil.getAliasFromClusterId(memberContext.getClusterId());
        Member member = service.getCluster(memberContext.getClusterId()).
                getMember(memberContext.getMemberId());
        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist", memberContext.getMemberId()));
        }
        TopologyHolder.acquireWriteLock();
        try {
            // Set instance id returned by the IaaS
            member.setInstanceId(memberContext.getInstanceId());
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
            } else {
                Cluster cluster = service.getCluster(memberContext.getClusterId());
                String clusterId = cluster.getClusterId();
                ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
                List<KubernetesService> kubernetesServices = Lists
                        .newArrayList(clusterContext.getKubernetesServices(memberContext.getClusterInstanceId()));

                if (!kubernetesServices.isEmpty()) {
                    cluster.setKubernetesServices(kubernetesServices);
                }

                member.setStatus(MemberStatus.Initialized);
                log.info("Member status updated to initialized");

                TopologyHolder.updateTopology(topology);
                //member intialized time
                Long timestamp = System.currentTimeMillis();
                TopologyEventPublisher.sendMemberInitializedEvent(memberContext);
                //publishing member information and status to DAS

                if (memInfoPublisher.isEnabled()) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing member information to DAS");
                    }
                    String scalingDecisionId = memberContext.getProperties()
                            .getProperty(StratosConstants.SCALING_DECISION_ID).getValue();
                    memInfoPublisher.publish(memberContext.getMemberId(), scalingDecisionId,
                            memberContext.getInstanceMetadata());
                }
                if (memStatusPublisher.isEnabled()) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing member status to DAS");
                    }
                    memStatusPublisher.publish(timestamp, applicationId, memberContext.getClusterId(), clusterAlias,
                            memberContext.getClusterInstanceId(), memberContext.getCartridgeType(),
                            memberContext.getNetworkPartitionId(), memberContext.getPartition().getId(),
                            memberContext.getMemberId(), MemberStatus.Initialized.toString());
                }
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }
    }

    private static int findKubernetesServicePort(String clusterId, Collection<KubernetesService> kubernetesServices,
            PortMapping portMapping) {
        for (KubernetesService kubernetesService : kubernetesServices) {
            if (kubernetesService.getProtocol().equals(portMapping.getProtocol())) {
                return kubernetesService.getPort();
            }
        }
        throw new RuntimeException(
                "Kubernetes service port not found: [cluster-id] " + clusterId + " [port] " + portMapping.getPort());
    }

    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
        try {
            Topology topology = TopologyHolder.getTopology();
            Service service = topology.getService(instanceStartedEvent.getServiceName());
            if (service == null) {
                throw new RuntimeException(
                        String.format("Service %s does not exist", instanceStartedEvent.getServiceName()));
            }
            if (!service.clusterExists(instanceStartedEvent.getClusterId())) {
                throw new RuntimeException(
                        String.format("Cluster %s does not exist for service %s", instanceStartedEvent.getClusterId(),
                                instanceStartedEvent.getServiceName()));
            }

            String applicationId = service.getCluster(instanceStartedEvent.getClusterId()).getAppId();
            String clusterAlias = CloudControllerUtil.getAliasFromClusterId(instanceStartedEvent.getClusterId());
            Cluster cluster = service.getCluster(instanceStartedEvent.getClusterId());
            Member member = cluster.getMember(instanceStartedEvent.getMemberId());
            if (member == null) {
                throw new RuntimeException(
                        String.format("Member %s does not exist", instanceStartedEvent.getMemberId()));
            }
            TopologyHolder.acquireWriteLock();
            try {
                // try update lifecycle state
                if (!member.isStateTransitionValid(MemberStatus.Starting)) {
                    log.error("Invalid State Transition from " + member.getStatus() + " to " +
                            MemberStatus.Starting);
                } else {
                    member.setStatus(MemberStatus.Starting);
                    log.info("member started event adding status started");

                    TopologyHolder.updateTopology(topology);
                    //member started time
                    Long timestamp = System.currentTimeMillis();
                    //memberStartedEvent.
                    TopologyEventPublisher.sendMemberStartedEvent(instanceStartedEvent);
                    //publishing member status to DAS
                    if (memStatusPublisher.isEnabled()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Publishing Member Status to DAS");
                        }
                        memStatusPublisher
                                .publish(timestamp, applicationId, instanceStartedEvent.getClusterId(), clusterAlias,
                                        instanceStartedEvent.getClusterInstanceId(),
                                        instanceStartedEvent.getServiceName(),
                                        instanceStartedEvent.getNetworkPartitionId(),
                                        instanceStartedEvent.getPartitionId(), instanceStartedEvent.getMemberId(),
                                        MemberStatus.Starting.toString());
                    }
                }
            } finally {
                TopologyHolder.releaseWriteLock();
            }
        } catch (Exception e) {
            String message = String.format("Could not handle member started event: [application-id] %s "
                            + "[service-name] %s [member-id] %s", instanceStartedEvent.getApplicationId(),
                    instanceStartedEvent.getServiceName(), instanceStartedEvent.getMemberId());
            log.warn(message, e);
        }
    }

    public static void handleMemberActivated(InstanceActivatedEvent instanceActivatedEvent) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(instanceActivatedEvent.getServiceName());
        if (service == null) {
            throw new RuntimeException(
                    String.format("Service %s does not exist", instanceActivatedEvent.getServiceName()));
        }
        Cluster cluster = service.getCluster(instanceActivatedEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(
                    String.format("Cluster %s does not exist for service %s", instanceActivatedEvent.getClusterId(),
                            instanceActivatedEvent.getServiceName()));
        }

        String applicationId = service.getCluster(instanceActivatedEvent.getClusterId()).getAppId();
        String clusterAlias = CloudControllerUtil.getAliasFromClusterId(instanceActivatedEvent.getClusterId());

        Member member = cluster.getMember(instanceActivatedEvent.getMemberId());
        if (member == null) {
            throw new RuntimeException(String.format("Member %s does not exist", instanceActivatedEvent.getMemberId()));
        }

        MemberActivatedEvent memberActivatedEvent = new MemberActivatedEvent(instanceActivatedEvent.getServiceName(),
                instanceActivatedEvent.getClusterId(), instanceActivatedEvent.getClusterInstanceId(),
                instanceActivatedEvent.getMemberId(), instanceActivatedEvent.getNetworkPartitionId(),
                instanceActivatedEvent.getPartitionId());

        // grouping - set grouid
        //TODO
        memberActivatedEvent.setApplicationId(null);
        TopologyHolder.acquireWriteLock();
        try {
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Active)) {
                log.error("Invalid state transition from [" + member.getStatus() + "] to [" +
                        MemberStatus.Active + "]");
            } else {
                member.setStatus(MemberStatus.Active);

                // Set member ports
                try {
                    Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(service.getServiceName());
                    if (cartridge == null) {
                        throw new RuntimeException(
                                String.format("Cartridge not found: [cartridge-type] %s", service.getServiceName()));
                    }

                    Port port;
                    int portValue;
                    List<PortMapping> portMappings = Arrays.asList(cartridge.getPortMappings());
                    String clusterId = cluster.getClusterId();
                    ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
                    Collection<KubernetesService> kubernetesServices = clusterContext
                            .getKubernetesServices(instanceActivatedEvent.getClusterInstanceId());

                    for (PortMapping portMapping : portMappings) {
                        if (!kubernetesServices.isEmpty()) {
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
                TopologyHolder.updateTopology(topology);

                //member activated time
                Long timestamp = System.currentTimeMillis();
                // Publish member activated event
                TopologyEventPublisher.sendMemberActivatedEvent(memberActivatedEvent);

                //publishing member status to DAS
                if (memStatusPublisher.isEnabled()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Publishing Member Status to DAS");
                    }
                    memStatusPublisher
                            .publish(timestamp, applicationId, memberActivatedEvent.getClusterId(), clusterAlias,
                                    memberActivatedEvent.getClusterInstanceId(), memberActivatedEvent.getServiceName(),
                                    memberActivatedEvent.getNetworkPartitionId(), memberActivatedEvent.getPartitionId(),
                                    memberActivatedEvent.getMemberId(), MemberStatus.Active.toString());
                }
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }
    }

    public static void handleMemberReadyToShutdown(InstanceReadyToShutdownEvent instanceReadyToShutdownEvent)
            throws InvalidMemberException, InvalidCartridgeTypeException, RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(instanceReadyToShutdownEvent.getServiceName());

        //update the status of the member
        if (service == null) {
            throw new RuntimeException(
                    String.format("Service %s does not exist", instanceReadyToShutdownEvent.getServiceName()));
        }

        Cluster cluster = service.getCluster(instanceReadyToShutdownEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist for service %s",
                    instanceReadyToShutdownEvent.getClusterId(), instanceReadyToShutdownEvent.getServiceName()));
        }

        String applicationId = service.getCluster(instanceReadyToShutdownEvent.getClusterId()).getAppId();
        String clusterAlias = CloudControllerUtil.getAliasFromClusterId(instanceReadyToShutdownEvent.getClusterId());

        Member member = cluster.getMember(instanceReadyToShutdownEvent.getMemberId());
        if (member == null) {
            throw new RuntimeException(
                    String.format("Member %s does not exist", instanceReadyToShutdownEvent.getMemberId()));
        }
        MemberReadyToShutdownEvent memberReadyToShutdownEvent = new MemberReadyToShutdownEvent(
                instanceReadyToShutdownEvent.getServiceName(), instanceReadyToShutdownEvent.getClusterId(),
                instanceReadyToShutdownEvent.getClusterInstanceId(), instanceReadyToShutdownEvent.getMemberId(),
                instanceReadyToShutdownEvent.getNetworkPartitionId(), instanceReadyToShutdownEvent.getPartitionId());
        //member ReadyToShutDown state change time
        Long timestamp = null;
        TopologyHolder.acquireWriteLock();
        try {
            if (!member.isStateTransitionValid(MemberStatus.ReadyToShutDown)) {
                throw new RuntimeException("Invalid State Transition from " + member.getStatus() + " to " +
                        MemberStatus.ReadyToShutDown);
            }
            member.setStatus(MemberStatus.ReadyToShutDown);
            log.info("Member Ready to shut down event adding status started");

            TopologyHolder.updateTopology(topology);
            timestamp = System.currentTimeMillis();
        } finally {
            TopologyHolder.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberReadyToShutdownEvent(memberReadyToShutdownEvent);
        //publishing member status to DAS.
        if (memStatusPublisher.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Publishing Member Status to DAS");
            }
            memStatusPublisher
                    .publish(timestamp, applicationId, instanceReadyToShutdownEvent.getClusterId(), clusterAlias,
                            instanceReadyToShutdownEvent.getClusterInstanceId(),
                            instanceReadyToShutdownEvent.getServiceName(),
                            instanceReadyToShutdownEvent.getNetworkPartitionId(),
                            instanceReadyToShutdownEvent.getPartitionId(), instanceReadyToShutdownEvent.getMemberId(),
                            MemberStatus.ReadyToShutDown.toString());
        }
        //termination of particular instance will be handled by autoscaler
    }

    public static void handleMemberMaintenance(InstanceMaintenanceModeEvent instanceMaintenanceModeEvent)
            throws InvalidMemberException, InvalidCartridgeTypeException, RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(instanceMaintenanceModeEvent.getServiceName());
        //update the status of the member
        if (service == null) {
            throw new RuntimeException(
                    String.format("Service %s does not exist", instanceMaintenanceModeEvent.getServiceName()));
        }

        Cluster cluster = service.getCluster(instanceMaintenanceModeEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist for service %s",
                    instanceMaintenanceModeEvent.getClusterId(), instanceMaintenanceModeEvent.getServiceName()));
        }

        Member member = cluster.getMember(instanceMaintenanceModeEvent.getMemberId());
        if (member == null) {
            throw new RuntimeException(
                    String.format("Member %s does not exist", instanceMaintenanceModeEvent.getMemberId()));
        }

        MemberMaintenanceModeEvent memberMaintenanceModeEvent = new MemberMaintenanceModeEvent(
                instanceMaintenanceModeEvent.getServiceName(), instanceMaintenanceModeEvent.getClusterId(),
                instanceMaintenanceModeEvent.getClusterInstanceId(), instanceMaintenanceModeEvent.getMemberId(),
                instanceMaintenanceModeEvent.getNetworkPartitionId(), instanceMaintenanceModeEvent.getPartitionId());
        TopologyHolder.acquireWriteLock();
        try {
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.In_Maintenance)) {
                throw new RuntimeException(
                        "Invalid State Transition from " + member.getStatus() + " to " + MemberStatus.In_Maintenance);
            }
            member.setStatus(MemberStatus.In_Maintenance);
            log.info("member maintenance mode event adding status started");

            TopologyHolder.updateTopology(topology);
        } finally {
            TopologyHolder.releaseWriteLock();
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
    public static void handleMemberTerminated(String serviceName, String clusterId, String networkPartitionId,
            String partitionId, String memberId) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(serviceName);
        Properties properties;
        if (service == null) {
            throw new RuntimeException(String.format("Service %s does not exist", serviceName));
        }
        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            throw new RuntimeException(
                    String.format("Cluster %s does not exist for service %s", clusterId, serviceName));
        }

        String applicationId = service.getCluster(clusterId).getAppId();
        String clusterAlias = CloudControllerUtil.getAliasFromClusterId(clusterId);
        Member member = cluster.getMember(memberId);
        if (member == null) {
            throw new RuntimeException((String.format("Member [member-id] %s does not exist", memberId)));
        }

        String clusterInstanceId = member.getClusterInstanceId();

        //member terminated time
        Long timestamp = null;
        TopologyHolder.acquireWriteLock();
        try {
            properties = member.getProperties();
            cluster.removeMember(member);
            TopologyHolder.updateTopology(topology);
        } finally {
            TopologyHolder.releaseWriteLock();
            timestamp = System.currentTimeMillis();
        }
        /* @TODO leftover from grouping_poc*/
        String groupAlias = null;
        TopologyEventPublisher
                .sendMemberTerminatedEvent(serviceName, clusterId, memberId, clusterInstanceId, networkPartitionId,
                        partitionId, properties, groupAlias);

        //publishing member status to DAS.
        if (memStatusPublisher.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Publishing Member Status to DAS");
            }
            memStatusPublisher.publish(timestamp, applicationId, member.getClusterId(), clusterAlias,
                    member.getClusterInstanceId(), member.getServiceName(), member.getNetworkPartitionId(),
                    member.getPartitionId(), member.getMemberId(), MemberStatus.Terminated.toString());
        }
    }

    public static void handleClusterActivatedEvent(
            ClusterStatusClusterActivatedEvent clusterStatusClusterActivatedEvent) throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(clusterStatusClusterActivatedEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            throw new RuntimeException(
                    String.format("Service %s does not exist", clusterStatusClusterActivatedEvent.getServiceName()));
        }

        Cluster cluster = service.getCluster(clusterStatusClusterActivatedEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(String.format("Cluster %s does not exist for service %s",
                    clusterStatusClusterActivatedEvent.getClusterId(),
                    clusterStatusClusterActivatedEvent.getServiceName()));
        }

        String applicationId = cluster.getAppId();
        String clusterId = cluster.getClusterId();
        ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
        if (clusterContext == null) {
            throw new RuntimeException(String.format("Cluster context not found [cluster-id] %s", clusterId));
        }

        ClusterInstanceActivatedEvent clusterInstanceActivatedEvent = new ClusterInstanceActivatedEvent(
                clusterStatusClusterActivatedEvent.getAppId(), clusterStatusClusterActivatedEvent.getServiceName(),
                clusterStatusClusterActivatedEvent.getClusterId(), clusterStatusClusterActivatedEvent.getInstanceId());
        TopologyHolder.acquireWriteLock();
        try {
            Collection<KubernetesService> kubernetesServices = clusterContext
                    .getKubernetesServices(clusterStatusClusterActivatedEvent.getInstanceId());
            if ((kubernetesServices != null) && (kubernetesServices.size() > 0)) {
                try {
                    // Generate access URLs for kubernetes services
                    Set<String> nodePublicIps = new HashSet<>();
                    for (KubernetesService kubernetesService : kubernetesServices) {
                        // Add node ips as load balancer ips
                        nodePublicIps.addAll(Arrays.asList(kubernetesService.getPublicIPs()));

                        // Only expose services of type node port
                        if (kubernetesService.getServiceType().equals(KubernetesConstants.NODE_PORT)) {
                            for (String hostname : cluster.getHostNames()) {
                                // Using type URI since only http, https, ftp, file, jar protocols are
                                // supported in URL
                                int port = kubernetesService.getPort();
                                if (cluster.getLoadBalancerIps().size() > 0) {
                                    // Load balancer ips have been provided, need to use proxy port
                                    port = findProxyPort(applicationId, clusterId, kubernetesService.getPortName());
                                }
                                URI accessURL = new URI(kubernetesService.getProtocol(), null, hostname, port, null,
                                        null, null);
                                cluster.addAccessUrl(clusterStatusClusterActivatedEvent.getInstanceId(),
                                        accessURL.toString());
                                clusterInstanceActivatedEvent.addAccessUrl(accessURL.toString());
                            }
                        }
                    }
                    if (cluster.getLoadBalancerIps().size() == 0) {
                        // Load balancer ips not given, use node public ips as load balancer ips
                        List<String> nodePublicIpsList = new ArrayList<>();
                        nodePublicIpsList.addAll(nodePublicIps);
                        cluster.setLoadBalancerIps(nodePublicIpsList);
                        clusterInstanceActivatedEvent.setLoadBalancerIps(nodePublicIpsList);
                    }
                    log.info(String.format("Access URLs generated for kubernetes services: [application] %s "
                                    + "[cluster] %s [access-urls] %s", applicationId, clusterId,
                            clusterInstanceActivatedEvent.getAccessUrls()));
                } catch (URISyntaxException e) {
                    log.error("Could not generate access URLs for Kubernetes services", e);
                }
            } else {
                try {
                    List<ClusterPortMapping> portMappings = CloudControllerContext.getInstance().
                            getClusterPortMappings(applicationId, clusterId);
                    for (ClusterPortMapping portMapping : portMappings) {
                        for (String hostname : cluster.getHostNames()) {
                            URI accessURL = new URI(portMapping.getProtocol(), null, hostname, portMapping.getPort(),
                                    null, null, null);
                            cluster.addAccessUrl(clusterStatusClusterActivatedEvent.getInstanceId(),
                                    accessURL.toString());
                            clusterInstanceActivatedEvent.addAccessUrl(accessURL.toString());
                        }
                    }

                    log.info(String.format("Access URLs generated: [application] %s [cluster] %s [access-urls] %s",
                            applicationId, clusterId, clusterInstanceActivatedEvent.getAccessUrls()));
                } catch (URISyntaxException e) {
                    log.error("Could not generate access URLs", e);
                }
            }

            ClusterInstance context = cluster.getInstanceContexts(clusterStatusClusterActivatedEvent.getInstanceId());
            if (context == null) {
                throw new RuntimeException("Cluster instance context is not found for [cluster] " +
                        clusterStatusClusterActivatedEvent.getClusterId() + " [instance-id] " +
                        clusterStatusClusterActivatedEvent.getInstanceId());
            }
            ClusterStatus status = ClusterStatus.Active;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster activated adding status started for " + cluster.getClusterId());
                TopologyHolder.updateTopology(topology);
                // publish event
                TopologyEventPublisher.sendClusterActivatedEvent(clusterInstanceActivatedEvent);
            } else {
                throw new RuntimeException(String.format("Cluster state transition is not valid: [cluster-id] %s "
                                + " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterStatusClusterActivatedEvent.getClusterId(),
                        clusterStatusClusterActivatedEvent.getInstanceId(), context.getStatus(), status));
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }

    }

    private static int findProxyPort(String applicationId, String clusterId, String portName) {
        List<ClusterPortMapping> portMappings = CloudControllerContext.getInstance().
                getClusterPortMappings(applicationId, clusterId);
        for (ClusterPortMapping portMapping : portMappings) {
            if (portMapping.getName().equals(portName)) {
                return portMapping.getProxyPort();
            }
        }
        throw new RuntimeException(
                String.format("Port mapping not found: [application] %s [cluster] %s " + "[port-name] %s",
                        applicationId, clusterId, portName));
    }

    public static void handleClusterInactivateEvent(ClusterStatusClusterInactivateEvent clusterInactivateEvent)
            throws RegistryException {
        Topology topology = TopologyHolder.getTopology();
        Service service = topology.getService(clusterInactivateEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            throw new RuntimeException(
                    String.format("Service %s does not exist", clusterInactivateEvent.getServiceName()));
        }

        Cluster cluster = service.getCluster(clusterInactivateEvent.getClusterId());
        if (cluster == null) {
            throw new RuntimeException(
                    String.format("Cluster %s does not exist for service %s", clusterInactivateEvent.getClusterId(),
                            clusterInactivateEvent.getServiceName()));
        }

        ClusterInstanceInactivateEvent clusterInactivatedEvent1 = new ClusterInstanceInactivateEvent(
                clusterInactivateEvent.getAppId(), clusterInactivateEvent.getServiceName(),
                clusterInactivateEvent.getClusterId(), clusterInactivateEvent.getInstanceId());
        TopologyHolder.acquireWriteLock();
        try {
            ClusterInstance context = cluster.getInstanceContexts(clusterInactivateEvent.getInstanceId());
            if (context == null) {
                throw new RuntimeException("Cluster Instance Context is not found for [cluster] " +
                        clusterInactivateEvent.getClusterId() + " [instance-id] " +
                        clusterInactivateEvent.getInstanceId());
            }
            ClusterStatus status = ClusterStatus.Inactive;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Inactive adding status started for" + cluster.getClusterId());
                TopologyHolder.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterInactivateEvent(clusterInactivatedEvent1);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s "
                                + " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterInactivateEvent.getClusterId(), clusterInactivateEvent.getInstanceId(),
                        context.getStatus(), status));
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }
    }

    public static void handleClusterTerminatedEvent(ClusterStatusClusterTerminatedEvent event)
            throws RegistryException {
        TopologyHolder.acquireWriteLock();
        try {
            Topology topology = TopologyHolder.getTopology();
            Service service = topology.getService(event.getServiceName());

            //update the status of the cluster
            if (service == null) {
                throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                throw new RuntimeException(
                        String.format("Cluster %s does not exist for service %s", event.getClusterId(),
                                event.getServiceName()));
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                throw new RuntimeException("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
            }
            ClusterStatus status = ClusterStatus.Terminated;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Terminated adding status started for and removing the cluster instance" + cluster
                        .getClusterId());
                cluster.removeInstanceContext(event.getInstanceId());
                TopologyHolder.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatedEvent clusterTerminatedEvent = new ClusterInstanceTerminatedEvent(
                        event.getAppId(), event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatedEvent(clusterTerminatedEvent);
            } else {
                throw new RuntimeException(String.format("Cluster state transition is not valid: [cluster-id] %s "
                                + " [instance-id] %s [current-status] %s [status-requested] %s", event.getClusterId(),
                        event.getInstanceId(), context.getStatus(), status));
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }
    }

    public static void handleClusterTerminatingEvent(ClusterStatusClusterTerminatingEvent event)
            throws RegistryException {
        TopologyHolder.acquireWriteLock();
        try {
            Topology topology = TopologyHolder.getTopology();
            Cluster cluster = topology.getService(event.getServiceName()).
                    getCluster(event.getClusterId());

            if (!cluster.isStateTransitionValid(ClusterStatus.Terminating, event.getInstanceId())) {
                log.error("Invalid state transfer from " + cluster.getStatus(event.getInstanceId()) + " to " +
                        ClusterStatus.Terminating);
            }
            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                throw new RuntimeException("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
            }
            ClusterStatus status = ClusterStatus.Terminating;
            if (context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Terminating started for " + cluster.getClusterId());
                TopologyHolder.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatingEvent clusterTerminaingEvent = new ClusterInstanceTerminatingEvent(
                        event.getAppId(), event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatingEvent(clusterTerminaingEvent);

                // Remove kubernetes services if available
                ClusterContext clusterContext = CloudControllerContext.getInstance()
                        .getClusterContext(event.getClusterId());

                KubernetesIaas.removeKubernetesServices(clusterContext, context.getInstanceId());

            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s "
                                + " [instance-id] %s [current-status] %s [status-requested] %s", event.getClusterId(),
                        event.getInstanceId(), context.getStatus(), status));
            }
        } finally {
            TopologyHolder.releaseWriteLock();
        }
    }
}
