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
import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.payload.MetaDataHolder;
import org.apache.stratos.cloud.controller.publisher.CartridgeInstanceDataPublisher;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.domain.topology.util.CompositeApplicationBuilder;
import org.apache.stratos.messaging.event.application.status.*;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.util.Constants;

import java.util.*;
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
            log.warn(String.format("Cartridge list is empty"));
            return;
        }
        try {

            TopologyManager.acquireWriteLock();
            for (Cartridge cartridge : cartridgeList) {
                if (!topology.serviceExists(cartridge.getType())) {
                    service = new Service(cartridge.getType(), cartridge.isMultiTenant() ? ServiceType.MultiTenant : ServiceType.SingleTenant);
                    List<PortMapping> portMappings = cartridge.getPortMappings();
                    Properties properties = new Properties();
                    for (Map.Entry<String, String> entry : cartridge.getProperties().entrySet()) {
                        properties.setProperty(entry.getKey(), entry.getValue());
                    }
                    service.setProperties(properties);
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
                    log.warn(String.format("Service %s does not exist..", cartridge.getType()));
                }
            } else {
                log.warn("Subscription already exists. Hence not removing the service:" + cartridge.getType()
                        + " from the topology");
            }
        }
    }

    public static void handleClusterCreated(Registrant registrant, boolean isLb) {
        /*Topology topology = TopologyManager.getTopology();
        Service service;
        try {
            TopologyManager.acquireWriteLock();
            String cartridgeType = registrant.getCartridgeType();
            service = topology.getService(cartridgeType);
            Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());

            Cluster cluster;
            String clusterId = registrant.getClusterId();
            if (service.clusterExists(clusterId)) {
                // update the cluster
                cluster = service.getCluster(clusterId);
                cluster.addHostName(registrant.getHostName());
                if (service.getServiceType() == ServiceType.MultiTenant) {
                    cluster.setTenantRange(registrant.getTenantRange());
                }
                if (service.getProperties().getProperty(Constants.IS_PRIMARY) != null) {
                    props.setProperty(Constants.IS_PRIMARY, service.getProperties().getProperty(Constants.IS_PRIMARY));
                }
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
            } else {
                cluster = new Cluster(cartridgeType, clusterId,
                        registrant.getDeploymentPolicyName(), registrant.getAutoScalerPolicyName(), null);
                cluster.addHostName(registrant.getHostName());
                if (service.getServiceType() == ServiceType.MultiTenant) {
                    cluster.setTenantRange(registrant.getTenantRange());
                }
                if (service.getProperties().getProperty(Constants.IS_PRIMARY) != null) {
                    props.setProperty(Constants.IS_PRIMARY, service.getProperties().getProperty(Constants.IS_PRIMARY));
                }
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
                //cluster.setStatus(Status.Created);
                service.addCluster(cluster);
            }
            TopologyManager.updateTopology(topology);
            TopologyEventPublisher.sendClusterCreatedEvent(cartridgeType, clusterId, cluster);

        } finally {
            TopologyManager.releaseWriteLock();
        }*/
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

    /*public static void handleClusterMaintenanceMode(ClusterContext ctxt) {

        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(ctxt.getCartridgeType());
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
            Cluster cluster = service.getCluster(ctxt.getClusterId());
            if (!cluster.isStateTransitionValid(ClusterStatus.Inactive)) {
                log.error("Invalid State Transition from " + cluster.getStatus() + " to " + ClusterStatus.Inactive);
            }
            cluster.setStatus(ClusterStatus.Inactive);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendClusterMaintenanceModeEvent(ctxt);
    }*/


    public static void handleMemberSpawned(String serviceName,
                                           String clusterId, String partitionId,
                                           String privateIp, String publicIp, MemberContext context) {
        // adding the new member to the cluster after it is successfully started
        // in IaaS.
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        Cluster cluster = service.getCluster(clusterId);
        String memberId = context.getMemberId();
        String networkPartitionId = context.getNetworkPartitionId();
        String lbClusterId = context.getLbClusterId();

        if (cluster.memberExists(memberId)) {
            log.warn(String.format("Member %s already exists", memberId));
            return;
        }

        try {
            TopologyManager.acquireWriteLock();
            Member member = new Member(serviceName, clusterId,
                    networkPartitionId, partitionId, memberId);
            //member.setStatus(MemberStatus.Created);
            member.setMemberIp(privateIp);
            member.setLbClusterId(lbClusterId);
            member.setMemberPublicIp(publicIp);
            member.setProperties(CloudControllerUtil.toJavaUtilProperties(context.getProperties()));
            cluster.addMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendInstanceSpawnedEvent(serviceName, clusterId,
                networkPartitionId, partitionId, memberId, lbClusterId,
                publicIp, privateIp, context);
    }

    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
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

        Member member = service.getCluster(instanceStartedEvent.getClusterId()).
                getMember(instanceStartedEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceStartedEvent.getMemberId()));
            return;
        }

        //grouping

        if (log.isDebugEnabled()) {
            log.debug("checking group id in ToplogyBuilder for member started event");
        }

        CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
        String appAlias = "compositeApplicationAlias";
        CompositeApplication app = builder.buildCompositeApplication(topology, appAlias);
        if (app != null) {
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder found composite app for " + appAlias);
            }
            String clusterId = instanceStartedEvent.getClusterId();
            String groupAlias = app.extractClusterGroupFromClusterId(clusterId);
            instanceStartedEvent.setGroupId(groupAlias);
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder  setting groupAlias " + groupAlias + " for instance started event for cluster " + clusterId);
            }
        }

        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Starting)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " + MemberStatus.Starting);
            }
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

        // grouping

        CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
        String appAlias = "compositeApplicationAlias";
        CompositeApplication app = builder.buildCompositeApplication(topology, appAlias);
        if (app != null) {
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder found composite app for member activated " + appAlias);
            }
            String clusterId = instanceActivatedEvent.getClusterId();
            String groupAlias = app.extractClusterGroupFromClusterId(clusterId);
            instanceActivatedEvent.setGroupId(groupAlias);
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder  setting groupAlias " + groupAlias + " for instance activated event for cluster " + clusterId);
            }
        }


        Member member = cluster.getMember(instanceActivatedEvent.getMemberId());

        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceActivatedEvent.getMemberId()));
            return;
        }

        MemberActivatedEvent memberActivatedEvent = new MemberActivatedEvent(instanceActivatedEvent.getServiceName(),
                instanceActivatedEvent.getClusterId(), instanceActivatedEvent.getNetworkPartitionId(), instanceActivatedEvent.getPartitionId(), instanceActivatedEvent.getMemberId());

        // grouping - set grouid
        memberActivatedEvent.setGroupId(instanceActivatedEvent.getGroupId());
        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Activated)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " + MemberStatus.Activated);
            }
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
            memberActivatedEvent.setMemberPublicIp(member.getMemberPublicIp());
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

        //grouping

        if (log.isDebugEnabled()) {
            log.debug("checking group id in ToplogyBuilder for member started event");
        }

        CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
        String appAlias = "compositeApplicationAlias";
        CompositeApplication app = builder.buildCompositeApplication(topology, appAlias);
        if (app != null) {
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder found composite app for " + appAlias);
            }
            String clusterId = instanceReadyToShutdownEvent.getClusterId();
            String groupAlias = app.extractClusterGroupFromClusterId(clusterId);
            instanceReadyToShutdownEvent.setGroupId(groupAlias);
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder  setting groupAlias " + groupAlias + " for instance ready shutdown event for cluster " + clusterId);
            }
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
                instanceReadyToShutdownEvent.getNetworkPartitionId(),
                instanceReadyToShutdownEvent.getPartitionId(),
                instanceReadyToShutdownEvent.getMemberId());
        try {
            TopologyManager.acquireWriteLock();

            if (!member.isStateTransitionValid(MemberStatus.ReadyToShutDown)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " + MemberStatus.ReadyToShutDown);
            }
            member.setStatus(MemberStatus.ReadyToShutDown);
            log.info("Member Ready to shut down event adding status started");

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


        //grouping

        if (log.isDebugEnabled()) {
            log.debug("checking group id in ToplogyBuilder for member started event");
        }

        CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
        String appAlias = "compositeApplicationAlias";
        CompositeApplication app = builder.buildCompositeApplication(topology, appAlias);
        if (app != null) {
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder found composite app for " + appAlias);
            }
            String clusterId = instanceMaintenanceModeEvent.getClusterId();

            String groupAlias = app.extractClusterGroupFromClusterId(clusterId);
            instanceMaintenanceModeEvent.setGroupId(groupAlias);
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder  setting groupAlias " + groupAlias + " for instance ready shutdown event for cluster " + clusterId);
            }
        }

        MemberMaintenanceModeEvent memberMaintenanceModeEvent = new MemberMaintenanceModeEvent(
                instanceMaintenanceModeEvent.getServiceName(),
                instanceMaintenanceModeEvent.getClusterId(),
                instanceMaintenanceModeEvent.getNetworkPartitionId(),
                instanceMaintenanceModeEvent.getPartitionId(),
                instanceMaintenanceModeEvent.getMemberId());
        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.In_Maintenance)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " + MemberStatus.In_Maintenance);
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

    public static void handleMemberTerminated(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId) {
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
            log.warn(String.format("Member with nodeID %s does not exist",
                    memberId));
            return;
        }

        //grouping

        if (log.isDebugEnabled()) {
            log.debug("checking group id in ToplogyBuilder for member started event");
        }

        CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
        String appAlias = "compositeApplicationAlias";
        CompositeApplication app = builder.buildCompositeApplication(topology, appAlias);

        String groupAlias = null;
        if (app != null) {
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder found composite app for " + appAlias);
            }
            groupAlias = app.extractClusterGroupFromClusterId(clusterId);
            if (log.isDebugEnabled()) {
                log.debug("TopologyBuilder  setting groupAlias " + groupAlias + " for member terminated event for cluster " + clusterId);
            }
        }

        try {
            TopologyManager.acquireWriteLock();
            properties = member.getProperties();
            cluster.removeMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberTerminatedEvent(serviceName, clusterId, networkPartitionId, partitionId, memberId, properties, groupAlias);
    }

    public static void handleMemberSuspended() {
        //TODO
        try {
            TopologyManager.acquireWriteLock();
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static synchronized void handleApplicationDeployed(Application application,
                                                 Set<ApplicationClusterContext> applicationClusterContexts,
                                                 Set<MetaDataHolder> metaDataHolders) {


        Topology topology = TopologyManager.getTopology();
        try {
            TopologyManager.acquireWriteLock();

            if (topology.applicationExists(application.getUniqueIdentifier())) {
                log.warn("Application with id [ " + application.getUniqueIdentifier() + " ] already exists in Topology");
                return;
            }
            List<Cluster> clusters = new ArrayList<Cluster>();
            for (ApplicationClusterContext applicationClusterContext : applicationClusterContexts) {
                Cluster cluster = new Cluster(applicationClusterContext.getCartridgeType(),
                        applicationClusterContext.getClusterId(), applicationClusterContext.getDeploymentPolicyName(),
                        applicationClusterContext.getAutoscalePolicyName(), application.getUniqueIdentifier());
                //cluster.setStatus(Status.Created);
                cluster.addHostName(applicationClusterContext.getHostName());
                cluster.setTenantRange(applicationClusterContext.getTenantRange());
                clusters.add(cluster);

                Service service = topology.getService(applicationClusterContext.getCartridgeType());
                if (service != null) {
                    service.addCluster(cluster);
                    log.info("Added Cluster " + cluster.toString() + " to Topology for Application with id: " + application.getUniqueIdentifier());
                } else {
                    log.error("Service " + applicationClusterContext.getCartridgeType() + " not found");
                    return;
                }
            }

            // add to Topology and update
            topology.addApplication(application);
            TopologyManager.updateTopology(topology);

            log.info("Application with id [ " + application.getUniqueIdentifier() + " ] added to Topology successfully");
            org.apache.stratos.messaging.event.topology.ApplicationCreatedEvent applicationCreatedEvent = new org.apache.stratos.messaging.event.topology.ApplicationCreatedEvent(application, clusters);
            TopologyEventPublisher.sendApplicationCreatedEvent(applicationCreatedEvent);

        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static synchronized void handleApplicationUndeployed(FasterLookUpDataHolder dataHolder,
                                                                String applicationId, int tenantId, String tenantDomain) {

        Set<ClusterDataHolder> clusterData;

        // update the Application and Cluster Statuses as 'Terminating'
        TopologyManager.acquireWriteLock();

        try {

            Topology topology = TopologyManager.getTopology();

            if (!topology.applicationExists(applicationId)) {
                log.warn("Application with id [ " + applicationId + " ] doesn't exist in Topology");
                return;
            }

            Application application = topology.getApplication(applicationId);
            // check and update application status to 'Terminating'
            if (!application.isStateTransitionValid(ApplicationStatus.Terminating)) {
                log.error("Invalid state transfer from " + application.getStatus() + " to " + ApplicationStatus.Terminating);
            }
            // for now anyway update the status forcefully
            application.setStatus(ApplicationStatus.Terminating);
            log.info("Application " + applicationId + "'s status updated to " + ApplicationStatus.Terminating);

            // update all the Clusters' statuses to 'Terminating'
            clusterData = application.getClusterDataRecursively();
            for (ClusterDataHolder clusterDataHolder : clusterData) {
                Service service = topology.getService(clusterDataHolder.getServiceType());
                if (service != null) {
                    Cluster aCluster = service.getCluster(clusterDataHolder.getClusterId());
                    if (aCluster != null) {
                        // validate state transition
                        if (aCluster.isStateTransitionValid(ClusterStatus.Terminating)) {
                            log.error("Invalid state transfer from " + aCluster.getStatus() + " to "
                                    + ClusterStatus.Terminating + " successfully");
                        }
                        // for now anyway update the status forcefully
                        aCluster.setStatus(ClusterStatus.Terminating);
                        log.info("Cluster " + clusterDataHolder.getClusterId() + "'s status updated to "
                                + ClusterStatus.Terminating + " successfully");

                    } else {
                        log.warn("Unable to find Cluster with cluster id " + clusterDataHolder.getClusterId() +
                        " in Topology");
                    }

                } else {
                    log.warn("Unable to remove cluster with cluster id: " + clusterDataHolder.getClusterId() + " from Topology, " +
                            " associated Service [ " + clusterDataHolder.getServiceType() + " ] npt found");
                }
            }

        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendApplicationUndeployedEvent(applicationId, clusterData);


//        Topology topology = TopologyManager.getTopology();
//
//        try {
//            TopologyManager.acquireWriteLock();
//
//            if (!topology.applicationExists(applicationId)) {
//                log.warn("Application with id [ " + applicationId + " ] doesn't exist in Topology");
//                //TopologyEventPublisher.sendApplicationRemovedEvent(applicationId, tenantId, tenantDomain);
//
//            } else {
//                Application application = topology.getApplication(applicationId);
//                Set<ClusterDataHolder> clusterData = application.getClusterDataRecursively();
//                // remove clusters
//                for (ClusterDataHolder clusterDataHolder : clusterData) {
//                    Service service = topology.getService(clusterDataHolder.getServiceType());
//                    if (service != null) {
//                        // remove Cluster
//                        service.removeCluster(clusterDataHolder.getClusterId());
//
//                        if (log.isDebugEnabled()) {
//                            log.debug("Removed cluster with id " + clusterDataHolder.getClusterId());
//                        }
//                    } else {
//                        log.warn("Unable to remove cluster with cluster id: " + clusterDataHolder.getClusterId() + " from Topology, " +
//                                " associated Service [ " + clusterDataHolder.getServiceType() + " ] npt found");
//                    }
//
//                    // remove runtime data
//                    dataHolder.removeClusterContext(clusterDataHolder.getClusterId());
//                    if(log.isDebugEnabled()) {
//                        log.debug("Removed Cluster Context for Cluster id: " + clusterDataHolder.getClusterId());
//                    }
//                }
//
//                // remove application
//                topology.removeApplication(applicationId);
//                TopologyManager.updateTopology(topology);
//
//                log.info("Removed application [ " + applicationId + " ] from Topology");
//
//                TopologyEventPublisher.sendApplicationRemovedEvent(applicationId, clusterData, tenantId, tenantDomain);
//            }
//
//        } finally {
//            TopologyManager.releaseWriteLock();
//        }
    }

    public static void handleCompositeApplicationCreated(ConfigCompositeApplication messConfigApp) {
        Topology topology = TopologyManager.getTopology();

        //ConfigCompositeApplication messConfigApp;
        try {

            TopologyManager.acquireWriteLock();
            String key = "compositeApplicationAlias"; //app.getAlias()
            topology.addConfigCompositeApplication(key, messConfigApp);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendConfigApplicationCreatedEventEvent(messConfigApp);
        log.info("TopolgyBuilder: sending sendConfigApplicationCreatedEventEvent ");

    }

    public static void handleCompositeApplicationRemoved(String alias) {
        log.info("TopolgyBuilder: sending sendConfigApplicationRemovedEventEvent ");
        TopologyEventPublisher.sendConfigApplicationRemovedEventEvent(alias);
    }

    public static void handleClusterActivatedEvent(ClusterActivatedEvent clusterActivatedEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(clusterActivatedEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    clusterActivatedEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    clusterActivatedEvent.getClusterId()));
            return;
        }

        org.apache.stratos.messaging.event.topology.ClusterActivatedEvent clusterActivatedEvent1 =
                new org.apache.stratos.messaging.event.topology.ClusterActivatedEvent(
                        clusterActivatedEvent.getAppId(),
                        clusterActivatedEvent.getServiceName(),
                        clusterActivatedEvent.getClusterId());
        try {
            TopologyManager.acquireWriteLock();
            //cluster.setStatus(Status.Activated);
            cluster.setStatus(ClusterStatus.Active);

            log.info("Cluster activated adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendClusterActivatedEvent(clusterActivatedEvent1);
    }

    public static void handleGroupActivatedEvent(GroupActivatedEvent groupActivatedEvent) {
        Topology topology = TopologyManager.getTopology();
        Application application = topology.getApplication(groupActivatedEvent.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    groupActivatedEvent.getAppId()));
            return;
        }

        Group group = application.getGroupRecursively(groupActivatedEvent.getGroupId());
        if (group == null) {
            log.warn(String.format("Group %s does not exist",
                    groupActivatedEvent.getGroupId()));
            return;
        }

        org.apache.stratos.messaging.event.topology.GroupActivatedEvent groupActivatedEvent1 =
                new org.apache.stratos.messaging.event.topology.GroupActivatedEvent(
                        groupActivatedEvent.getAppId(),
                        groupActivatedEvent.getGroupId());
        try {
            TopologyManager.acquireWriteLock();
            group.setStatus(GroupStatus.Active);
            log.info("Group activated adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendGroupActivatedEvent(groupActivatedEvent1);
    }

    public static void handleApplicationActivatedEvent(ApplicationActivatedEvent applicationActivatedEvent) {
        Topology topology = TopologyManager.getTopology();
        Application application = topology.getApplication(applicationActivatedEvent.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    applicationActivatedEvent.getAppId()));
            return;
        }

        org.apache.stratos.messaging.event.topology.ApplicationActivatedEvent applicationActivatedEvent1 =
                new org.apache.stratos.messaging.event.topology.ApplicationActivatedEvent(
                        applicationActivatedEvent.getAppId());
        try {
            TopologyManager.acquireWriteLock();
            application.setStatus(ApplicationStatus.Active);
            log.info("Application activated adding status started for Topology");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendApplicationActivatedEvent(applicationActivatedEvent1);
    }

    public static void handleApplicationInActivatedEvent(ApplicationInactivatedEvent event) {
        Topology topology = TopologyManager.getTopology();
        Application application = topology.getApplication(event.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    event.getAppId()));
            return;
        }

        org.apache.stratos.messaging.event.topology.ApplicationInactivatedEvent applicationActivatedEvent =
                new org.apache.stratos.messaging.event.topology.ApplicationInactivatedEvent(
                        event.getAppId());
        try {
            TopologyManager.acquireWriteLock();
            application.setStatus(ApplicationStatus.Inactive);
            log.info("Application inactivated adding status started for Topology");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendApplicationInactivatedEvent(applicationActivatedEvent);
    }

    public static void handleApplicationCreatedEvent(ApplicationCreatedEvent event) {
        Topology topology = TopologyManager.getTopology();
        Application application = topology.getApplication(event.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    event.getAppId()));
            return;
        }
        List<Cluster> clusters = new ArrayList<Cluster>();
        Set<ClusterDataHolder> allClusters  = application.getClusterDataRecursively();

        for(ClusterDataHolder clusterDataHolder : allClusters){
            String clusterId = clusterDataHolder.getClusterId();
            String serviceName = clusterDataHolder.getServiceType();
            clusters.add(TopologyManager.getTopology().getService(serviceName).getCluster(clusterId));
        }
        org.apache.stratos.messaging.event.topology.ApplicationCreatedEvent applicationActivatedEvent =
                new org.apache.stratos.messaging.event.topology.ApplicationCreatedEvent(
                        application, clusters);
        try {
            TopologyManager.acquireWriteLock();
            application.setStatus(ApplicationStatus.Created);
            log.info("Application created adding status started for Topology");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendApplicationCreatedEvent(applicationActivatedEvent);
    }

    public static void handleApplicationTerminatingEvent(ApplicationTerminatingEvent event) {
        Topology topology = TopologyManager.getTopology();
        Application application = topology.getApplication(event.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    event.getAppId()));
            return;
        }

        org.apache.stratos.messaging.event.topology.ApplicationTerminatingEvent applicationTerminatingEvent =
                new org.apache.stratos.messaging.event.topology.ApplicationTerminatingEvent(
                        event.getAppId());
        try {
            TopologyManager.acquireWriteLock();
            application.setStatus(ApplicationStatus.Terminating);
            log.info("Application terminating adding status started for Topology");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendApplicationTerminatingEvent(applicationTerminatingEvent);
    }

    public static void handleApplicationTerminatedEvent(ApplicationTerminatedEvent event) {
        Topology topology = TopologyManager.getTopology();
        Application application = topology.getApplication(event.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    event.getAppId()));
            return;
        }

        org.apache.stratos.messaging.event.topology.ApplicationTerminatedEvent applicationTerminatedEvent =
                new org.apache.stratos.messaging.event.topology.ApplicationTerminatedEvent(
                        event.getAppId());
        try {
            TopologyManager.acquireWriteLock();
            application.setStatus(ApplicationStatus.Terminated);
            log.info("Application terminated adding status started for Topology");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendApplicationTerminatedEvent(applicationTerminatedEvent);
    }
}
