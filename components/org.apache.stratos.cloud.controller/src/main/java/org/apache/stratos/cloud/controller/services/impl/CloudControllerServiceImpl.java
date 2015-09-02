/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.services.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.concurrent.PartitionValidatorCallable;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyManager;
import org.apache.stratos.cloud.controller.services.CloudControllerService;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.topology.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

/**
 * Cloud Controller Service is responsible for starting up new server instances,
 * terminating already started instances, providing pending instance count etc.
 */
public class CloudControllerServiceImpl implements CloudControllerService {

    private static final Log log = LogFactory.getLog(CloudControllerServiceImpl.class);

    private static final String PERSISTENCE_MAPPING = "PERSISTENCE_MAPPING";
    public static final String PAYLOAD_PARAMETER = "payload_parameter.";

    private CloudControllerContext cloudControllerContext = CloudControllerContext.getInstance();
    private ExecutorService executorService;
    public CloudControllerServiceImpl() {
        executorService = StratosThreadPool.getExecutorService("cloud.controller.instance.manager.thread.pool", 50);

    }

    public boolean addCartridge(Cartridge cartridgeConfig) throws InvalidCartridgeDefinitionException,
            InvalidIaasProviderException,
            CartridgeAlreadyExistsException {

        handleNullObject(cartridgeConfig, "Cartridge definition is null");
        handleNullObject(cartridgeConfig.getUuid(), "Cartridge definition UUID is null");

        if (log.isInfoEnabled()) {
            log.info(String.format("Adding cartridge: [tenant id] %d [cartridge-uuid] %s [cartridge-type] %s ",
                    cartridgeConfig.getTenantId(), cartridgeConfig.getUuid(), cartridgeConfig.getType()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Cartridge definition: " + cartridgeConfig.toString());
        }

        try {
            CloudControllerUtil.extractIaaSProvidersFromCartridge(cartridgeConfig);
        } catch (Exception e) {
            String message = String.format("Invalid cartridge definition: " +
                            "[cartridge-type] %s", cartridgeConfig.getType());
            log.error(message, e);
            throw new InvalidCartridgeDefinitionException(message, e);
        }

        String cartridgeUuid = cartridgeConfig.getUuid();
        Cartridge existingCartridge = cloudControllerContext.getCartridge(cartridgeUuid);
        if (existingCartridge != null) {
            String message = String.format("Cartridge already exists: [tenant id] %d [cartridge-uuid] %s " +
                            "[cartridge-type] %s ", existingCartridge.getTenantId(), cartridgeUuid,
                    existingCartridge.getType());
            log.error(message);
            throw new CartridgeAlreadyExistsException(message);
        }

        // Add cartridge to the cloud controller context and persist
        CloudControllerContext.getInstance().addCartridge(cartridgeConfig);
        CloudControllerContext.getInstance().persist();

        List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
        cartridgeList.add(cartridgeConfig);

        TopologyBuilder.handleServiceCreated(cartridgeList);

        if (log.isInfoEnabled()) {
            log.info(String.format("Successfully added cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type]" +
                            " %s", cartridgeConfig.getTenantId(), cartridgeUuid, cartridgeConfig.getType()));
        }
        return true;
    }

    @Override
    public boolean updateCartridge(Cartridge cartridge) throws InvalidCartridgeDefinitionException,
            InvalidIaasProviderException,
            CartridgeDefinitionNotExistsException {

        handleNullObject(cartridge, "Cartridge definition is null");

        if (log.isInfoEnabled()) {
            log.info(String.format("Updating cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type]" +
                    " %s", cartridge.getTenantId(), cartridge.getUuid(), cartridge.getType()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Cartridge definition: " + cartridge.toString());
        }

        try {
            CloudControllerUtil.extractIaaSProvidersFromCartridge(cartridge);
        } catch (Exception e) {
            String msg = "Invalid cartridge definition: [cartridge-type] " + cartridge.getType();
            log.error(msg, e);
            throw new InvalidCartridgeDefinitionException(msg, e);
        }

        String cartridgeUuid = cartridge.getUuid();
        if (cloudControllerContext.getCartridge(cartridgeUuid) != null) {
            Cartridge cartridgeToBeRemoved = cloudControllerContext.getCartridge(cartridgeUuid);
            try {
                removeCartridgeFromCC(cartridgeToBeRemoved.getUuid());
            } catch (InvalidCartridgeTypeException ignore) {
            }
            copyIaasProviders(cartridge, cartridgeToBeRemoved);
        } else {
            throw new CartridgeDefinitionNotExistsException("Cartridge definition not exists: [cartridge-type] " +
                    cartridge.getType());
        }

        // Add cartridge to the cloud controller context and persist
        CloudControllerContext.getInstance().addCartridge(cartridge);
        CloudControllerContext.getInstance().persist();
        // transaction ends

        if (log.isInfoEnabled()) {
            log.info(String.format("Successfully updated cartridge: tenant-id] %d [cartridge-uuid] %s " +
                    "[cartridge-type] %s", cartridge.getTenantId(), cartridge.getUuid(), cartridge.getType()));
        }
        return true;
    }

    private void copyIaasProviders(Cartridge destCartridge,
                                   Cartridge sourceCartridge) {

        List<IaasProvider> newIaasProviders = CloudControllerContext.getInstance().getIaasProviders(destCartridge.getUuid());

        Map<String, IaasProvider> iaasProviderMap = CloudControllerContext.getInstance().getPartitionToIaasProvider(sourceCartridge.getUuid());

        if (iaasProviderMap != null) {
            for (Entry<String, IaasProvider> entry : iaasProviderMap.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String partitionId = entry.getKey();
                IaasProvider iaasProvider = entry.getValue();
                if (newIaasProviders.contains(iaasProvider)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Copying partition from the cartridge that is undeployed, to the new cartridge: "
                                + "[partition-id] " + partitionId + " [cartridge-type] " + destCartridge.getType());
                    }
                    CloudControllerContext.getInstance().addIaasProvider(destCartridge.getType(), partitionId,
                            newIaasProviders.get(newIaasProviders.indexOf(iaasProvider)));
                }
            }
        }

    }

    public boolean removeCartridge(String cartridgeUuid) throws InvalidCartridgeTypeException {
        //Removing the cartridge from CC
        Cartridge cartridge = removeCartridgeFromCC(cartridgeUuid);
        //removing the cartridge from Topology
        removeCartridgeFromTopology(cartridge);

        if (log.isInfoEnabled()) {
            log.info(String.format("Successfully removed cartridge: [tenant-id] %d [cartridge-uuid] %s " +
                    "[cartridge-type] %s", cartridge.getTenantId(), cartridge.getUuid(), cartridge.getType()));
        }
        return true;
    }

    private Cartridge removeCartridgeFromCC(String cartridgeUuid) throws InvalidCartridgeTypeException {
        Cartridge cartridge = null;
        if ((cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeUuid)) != null) {
            if (CloudControllerContext.getInstance().removeCartridge(cartridgeUuid)) {
                // invalidate partition validation cache
                CloudControllerContext.getInstance().removeFromCartridgeTypeToPartitionIds(cartridgeUuid);

                if (log.isDebugEnabled()) {
                    log.debug("Partition cache invalidated for cartridge " + cartridgeUuid);
                }

                CloudControllerContext.getInstance().persist();

                if (log.isInfoEnabled()) {
                    log.info(String.format("Successfully removed cartridge: [tenant-id] %d [cartridge-uuid] %s " +
                            "[cartridge-type] %s", cartridge.getTenantId(), cartridge.getUuid(), cartridge.getType()));
                }
                return cartridge;
            }
        }
        String msg = "Cartridge not found: [cartridge-uuid] " + cartridgeUuid;
        log.error(msg);
        throw new InvalidCartridgeTypeException(msg);
    }

    private void removeCartridgeFromTopology(Cartridge cartridge) throws InvalidCartridgeTypeException {
        // sends the service removed event
        List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
        cartridgeList.add(cartridge);
        TopologyBuilder.handleServiceRemoved(cartridgeList);
    }

    public boolean addServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException {

        if (servicegroup == null) {
            String msg = "Invalid ServiceGroup Definition: Definition is null.";
            log.error(msg);
            throw new IllegalArgumentException(msg);

        }
        CloudControllerContext.getInstance().addServiceGroup(servicegroup);
        CloudControllerContext.getInstance().persist();
        return true;
    }

    public boolean removeServiceGroup(String name) throws InvalidServiceGroupException {
        if (log.isDebugEnabled()) {
            log.debug("Removing service group: [service-group-name] " + name);
        }

        ServiceGroup serviceGroup = null;

        serviceGroup = CloudControllerContext.getInstance().getServiceGroup(name);

        if (serviceGroup != null) {
            if (CloudControllerContext.getInstance().getServiceGroups().remove(serviceGroup)) {
                CloudControllerContext.getInstance().persist();
                if (log.isInfoEnabled()) {
                    log.info(String.format("Successfully removed the cartridge group:  [service-group-name] %s", name));
                }
                return true;
            }
        }

        String msg = "Cartridge group not found: [group-name] " + name;
        log.error(msg);
        throw new InvalidServiceGroupException(msg);

    }

    @Override
    public ServiceGroup getServiceGroup(String name) throws InvalidServiceGroupException {

        if (log.isDebugEnabled()) {
            log.debug("getServiceGroupDefinition:" + name);
        }

        ServiceGroup serviceGroup = CloudControllerContext.getInstance().getServiceGroup(name);

        if (serviceGroup == null) {
            String message = "Cartridge group not found: [group-name] " + name;
            if (log.isDebugEnabled()) {
                log.debug(message);
            }
            throw new InvalidServiceGroupException(message);
        }

        return serviceGroup;
    }

    public String[] getServiceGroupSubGroups(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid cartridge group: [group-name] " + name);
        }

        return serviceGroup.getSubGroups();
    }

    /**
     *
     */
    public String[] getServiceGroupCartridges(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid cartridge group: [group-name] " + name);
        }
        String[] cs = serviceGroup.getCartridges();
        return cs;

    }

    public Dependencies getServiceGroupDependencies(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid cartridge group: [group-name] " + name);
        }
        return serviceGroup.getDependencies();
    }

    @Override
    public MemberContext[] startInstances(InstanceContext[] instanceContexts)
            throws CartridgeNotFoundException, InvalidIaasProviderException, CloudControllerException {

        handleNullObject(instanceContexts, "Instance start-up failed, member contexts is null");

        List<MemberContext> memberContextList = new ArrayList<MemberContext>();
        for (InstanceContext instanceContext : instanceContexts) {
            if (instanceContext != null) {
                MemberContext memberContext = startInstance(instanceContext);
                memberContextList.add(memberContext);
            }
        }
        MemberContext[] memberContextsArray = memberContextList.toArray(new MemberContext[memberContextList.size()]);
        return memberContextsArray;
    }

    public MemberContext startInstance(InstanceContext instanceContext) throws
            CartridgeNotFoundException, InvalidIaasProviderException, CloudControllerException {

        try {
            // Validate instance context
            handleNullObject(instanceContext, "Could not start instance, instance context is null");
            if (log.isDebugEnabled()) {
                log.debug("Starting up instance: " + instanceContext);
            }

            // Validate partition
            Partition partition = instanceContext.getPartition();
            handleNullObject(partition, "Could not start instance, partition is null");

            // Validate cluster
            String partitionId = partition.getId();
            String clusterId = instanceContext.getClusterId();
            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, "Could not start instance, cluster context not found: [cluster-id] " + clusterId);

            // Validate cartridge
            String cartridgeType = clusterContext.getCartridgeUuid();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = "Could not startup instance, cartridge not found: [cartridge-type] " + cartridgeType;
                log.error(msg);
                throw new CartridgeNotFoundException(msg);
            }

            // Validate iaas provider
            IaasProvider iaasProvider = CloudControllerContext.getInstance().getIaasProviderOfPartition(cartridge.getUuid(), partitionId);
            if (iaasProvider == null) {
                String msg = String.format("Could not start instance, " +
                                "IaaS provider not found in cartridge %s for partition %s, " +
                                "partitions found: %s ", cartridgeType, partitionId,
                        CloudControllerContext.getInstance().getPartitionToIaasProvider(cartridge.getType()).keySet().toString());
                log.error(msg);
                throw new InvalidIaasProviderException(msg);
            }

            // Generate member ID
            String memberId = generateMemberId(clusterId);

            // Create member context
            String applicationId = clusterContext.getApplicationUuid();
            MemberContext memberContext = createMemberContext(applicationId, cartridgeType, memberId,
                    CloudControllerUtil.getLoadBalancingIPTypeEnumFromString(cartridge.getLoadBalancingIPType()), instanceContext);

            // Prepare payload
            StringBuilder payload = new StringBuilder(clusterContext.getPayload());
            addToPayload(payload, "MEMBER_ID", memberId);
            addToPayload(payload, "INSTANCE_ID", memberContext.getInstanceId());
            addToPayload(payload, "CLUSTER_INSTANCE_ID", memberContext.getClusterInstanceId());
            addToPayload(payload, "LB_CLUSTER_ID", memberContext.getLbClusterId());
            addToPayload(payload, "NETWORK_PARTITION_ID", memberContext.getNetworkPartitionId());
            addToPayload(payload, "PARTITION_ID", partitionId);
            addToPayload(payload, "INTERNAL", "false");

            if (memberContext.getProperties() != null) {
                org.apache.stratos.common.Properties properties = memberContext.getProperties();
                if (properties != null) {
                    for (Property prop : properties.getProperties()) {
                        addToPayload(payload, prop.getName(), String.valueOf(prop.getValue()));
                    }
                }
            }

            NetworkPartition networkPartition =
                    CloudControllerContext.getInstance().getNetworkPartition(memberContext.getNetworkPartitionId());


            if (networkPartition.getProperties() != null) {
                if (networkPartition.getProperties().getProperties() != null) {
                    for (Property property : networkPartition.getProperties().getProperties()) {
                        // check if a property is related to the payload. Currently
                        // this is done by checking if the
                        // property name starts with 'payload_parameter.' suffix. If
                        // so the payload param name will
                        // be taken as the substring from the index of '.' to the
                        // end of the property name.
                        if (property.getName().startsWith(PAYLOAD_PARAMETER)) {
                            String propertyName = property.getName();
                            String payloadParamName = propertyName.substring(propertyName.indexOf(".") + 1);
                            if (payload.toString().contains(payloadParamName)) {
                                replaceInPayload(payloadParamName, payload, payloadParamName, property.getValue());
                            } else {
                                addToPayload(payload, payloadParamName, property.getValue());
                            }
                        }
                    }
                }
            }

            Iaas iaas = iaasProvider.getIaas();
            if (clusterContext.isVolumeRequired()) {
                addToPayload(payload, PERSISTENCE_MAPPING, getPersistencePayload(clusterContext, iaas).toString());
            }

            if (log.isDebugEnabled()) {
                log.debug("Payload: " + payload.toString());
            }

            if (clusterContext.isVolumeRequired()) {

                Volume[] volumes = clusterContext.getVolumes();
                if (volumes != null) {
                    for (int i = 0; i < volumes.length; i++) {

                        if (volumes[i].getId() == null) {
                            // Create a new volume
                            volumes[i] = createVolumeAndSetInClusterContext(volumes[i], iaasProvider);
                        }
                    }
                }
                clusterContext.setVolumes(volumes);
            }

            // Persist member context
            CloudControllerContext.getInstance().addMemberContext(memberContext);
            CloudControllerContext.getInstance().persist();

            // Handle member created event
            TopologyBuilder.handleMemberCreatedEvent(memberContext);

            // Start instance in a new thread
            if (log.isDebugEnabled()) {
                log.debug(String.format("Starting instance creator thread: [cluster] %s [cluster-instance] %s " +
                                "[member] %s [application-id] %s", instanceContext.getClusterId(),
                        instanceContext.getClusterInstanceId(), memberId, applicationId));
            }
            executorService.execute(new InstanceCreator(memberContext, iaasProvider, payload.toString().getBytes()));

            return memberContext;
        } catch (Exception e) {
            String msg = String.format("Could not start instance: [cluster] %s [cluster-instance] %s",
                    instanceContext.getClusterId(), instanceContext.getClusterInstanceId());
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }

    private MemberContext createMemberContext(String applicationId, String cartridgeType, String memberId,
                                              LoadBalancingIPType loadBalancingIPType, InstanceContext instanceContext) {
        MemberContext memberContext = new MemberContext(
                applicationId, cartridgeType, instanceContext.getClusterId(), memberId);


        memberContext.setClusterInstanceId(instanceContext.getClusterInstanceId());
        memberContext.setNetworkPartitionId(instanceContext.getNetworkPartitionId());
        memberContext.setPartition(cloudControllerContext.getNetworkPartition(instanceContext.getNetworkPartitionId()).
                getPartition(instanceContext.getPartition().getId()));
        memberContext.setInitTime(instanceContext.getInitTime());
        memberContext.setProperties(instanceContext.getProperties());
        memberContext.setLoadBalancingIPType(loadBalancingIPType);
        memberContext.setInitTime(System.currentTimeMillis());
        memberContext.setObsoleteExpiryTime(instanceContext.getObsoleteExpiryTime());

        return memberContext;
    }

    private Volume createVolumeAndSetInClusterContext(Volume volume,
                                                      IaasProvider iaasProvider) {
        // iaas cannot be null at this state #startInstance method
        Iaas iaas = iaasProvider.getIaas();
        int sizeGB = volume.getSize();
        String snapshotId = volume.getSnapshotId();
        if (StringUtils.isNotEmpty(volume.getVolumeId())) {
            // volumeID is specified, so not creating additional volumes
            if (log.isDebugEnabled()) {
                log.debug("Volume creation is skipping since a volume ID is specified. [Volume ID] " + volume.getVolumeId());
            }
            volume.setId(volume.getVolumeId());
        } else {
            String volumeId = iaas.createVolume(sizeGB, snapshotId);
            volume.setId(volumeId);
        }

        volume.setIaasType(iaasProvider.getType());

        return volume;
    }


    private StringBuilder getPersistencePayload(ClusterContext ctx, Iaas iaas) {
        StringBuilder persistencePayload = new StringBuilder();
        if (isPersistenceMappingAvailable(ctx)) {
            for (Volume volume : ctx.getVolumes()) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding persistence mapping " + volume.toString());
                }
                if (persistencePayload.length() != 0) {
                    persistencePayload.append("|");
                }

                persistencePayload.append(iaas.getIaasDevice(volume.getDevice()));
                persistencePayload.append("|");
                persistencePayload.append(volume.getId());
                persistencePayload.append("|");
                persistencePayload.append(volume.getMappingPath());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Persistence payload: " + persistencePayload.toString());
        }
        return persistencePayload;
    }

    private boolean isPersistenceMappingAvailable(ClusterContext ctx) {
        return ctx.getVolumes() != null && ctx.isVolumeRequired();
    }

    private void addToPayload(StringBuilder payload, String name, String value) {
        payload.append(",");
        payload.append(name + "=" + value);
    }

    private void replaceInPayload(String payloadParamName, StringBuilder payload, String name, String value) {

        payload.replace(payload.indexOf(payloadParamName), payload.indexOf(",", payload.indexOf(payloadParamName)),
                "," + name + "=" + value);
    }

    private String generateMemberId(String clusterId) {
        UUID memberId = UUID.randomUUID();
        return clusterId + memberId.toString();
    }

    public boolean terminateInstanceForcefully(String memberId) {

        log.info(String.format("Starting to forcefully terminate the member " + memberId));
        boolean memberTerminated = true;
        try {
            this.terminateInstance(memberId);
        } catch (InvalidMemberException e) {
            memberTerminated = false;
        } catch (CloudControllerException e) {
            memberTerminated = false;
        } catch (InvalidCartridgeTypeException e) {
            memberTerminated = false;
        }

        if (memberTerminated) {
            log.info(String.format("Member terminated [member-id] %s ", memberId));
        } else {
            log.warn(String.format("Stratos could not terminate the member [member-id] %s. This may due to a issue " +
                    "in the underlying IaaS, Please terminate the member manually if it is available", memberId));
            MemberContext memberContext = CloudControllerContext.getInstance().getMemberContextOfMemberId(memberId);
            CloudControllerServiceUtil.executeMemberTerminationPostProcess(memberContext);
        }
        return true;
    }

    @Override
    public boolean terminateInstance(String memberId) throws InvalidMemberException,
            InvalidCartridgeTypeException, CloudControllerException {

        try {
            handleNullObject(memberId, "Could not terminate instance, member id is null.");

            MemberContext memberContext = CloudControllerContext.getInstance().getMemberContextOfMemberId(memberId);
            if (memberContext == null) {
                String msg = "Could not terminate instance, member context not found: [member-id] " + memberId;
                log.error(msg);
                throw new InvalidMemberException(msg);
            }

            if (StringUtils.isBlank(memberContext.getInstanceId())) {
                if (log.isErrorEnabled()) {
                    log.error(String.format(
                            "Could not terminate instance, instance id is blank: [member-id] %s " +
                                    ", removing member from topology...",
                            memberContext.getMemberId()));
                }
                CloudControllerServiceUtil.executeMemberTerminationPostProcess(memberContext);
            }

            // check if status == active, if true, then this is a termination on member faulty
            TopologyManager.acquireWriteLock();
            Topology topology = TopologyManager.getTopology();
            org.apache.stratos.messaging.domain.topology.Service service = topology.getService(memberContext.getCartridgeType());

            if (service != null) {
                Cluster cluster = service.getCluster(memberContext.getClusterId());
                if (cluster != null) {
                    Member member = cluster.getMember(memberId);
                    if (member != null) {

                        // check if ready to shutdown member is expired and send
                        // member terminated if it is.
                        if (isMemberExpired(member, memberContext.getObsoleteInitTime(), memberContext.getObsoleteExpiryTime())) {
                            if (log.isInfoEnabled()) {
                                log.info(String.format(
                                        "Member pending termination in ReadyToShutdown state exceeded expiry time. " +
                                                "This member has to be manually deleted: %s", memberContext.getMemberId()));
                            }

                            CloudControllerServiceUtil.executeMemberTerminationPostProcess(memberContext);
                            return false;
                        }
                    }
                }
            }

            executorService.execute(new InstanceTerminator(memberContext));
        } catch (InvalidMemberException e) {
            String message = "Could not terminate instance: [member-id] " + memberId;
            log.error(message, e);
            throw e;
        } catch (Exception e) {
            String message = "Could not terminate instance: [member-id] " + memberId;
            log.error(message, e);
            throw new CloudControllerException(message, e);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        return true;
    }

    /**
     * Check if a member has been in the ReadyToShutdown status for a specified expiry time
     *
     * @param member
     * @param initTime
     * @param expiryTime
     * @return
     */
    private boolean isMemberExpired(Member member, long initTime, long expiryTime) {
        if (member.getStatus() == MemberStatus.ReadyToShutDown) {
            if (initTime == 0) {
                // obsolete init time hasn't been set, i.e. not a member detected faulty.
                // this is a graceful shutdown
                return false;
            }

            // member detected faulty, calculate ready to shutdown waiting period
            long timeInReadyToShutdownStatus = System.currentTimeMillis() - initTime;
            return timeInReadyToShutdownStatus >= expiryTime;
        }

        return false;
    }

    @Override
    public boolean terminateInstances(String clusterId) throws InvalidClusterException {

        log.info("Starting to terminate all instances of cluster : "
                + clusterId);

        handleNullObject(clusterId, "Instance termination failed. Cluster id is null.");

        List<MemberContext> memberContexts = CloudControllerContext.getInstance().getMemberContextsOfClusterId(clusterId);
        if (memberContexts == null) {
            String msg = "Instance termination failed. No members found for cluster id: " + clusterId;
            log.warn(msg);
            return false;
        }

        for (MemberContext memberContext : memberContexts) {
            executorService.execute(new InstanceTerminator(memberContext));
        }
        return true;
    }

    @Override
    public boolean registerService(Registrant registrant)
            throws CartridgeNotFoundException {

        String cartridgeType = registrant.getCartridgeType();
        handleNullObject(cartridgeType, "Service registration failed, cartridge Type is null.");

        String clusterId = registrant.getClusterId();
        handleNullObject(clusterId, "Service registration failed, cluster id is null.");

        String payload = registrant.getPayload();
        handleNullObject(payload, "Service registration failed, payload is null.");

        String hostName = registrant.getHostName();
        handleNullObject(hostName, "Service registration failed, hostname is null.");

        Cartridge cartridge = null;
        if ((cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType)) == null) {
            String msg = "Registration of cluster: " + clusterId +
                    " failed, cartridge not found: [cartridge-type] " + cartridgeType;
            log.error(msg);
            throw new CartridgeNotFoundException(msg);
        }

        CloudControllerContext.getInstance().persist();

        log.info("Successfully registered service: " + registrant);
        return true;
    }

    @Override
    public String[] getCartridges() {
        // get the list of cartridges registered
        Collection<Cartridge> cartridges = CloudControllerContext.getInstance().getCartridges();

        if (cartridges == null) {
            log.info("No registered Cartridge found.");
            return new String[0];
        }

        String[] cartridgeTypes = new String[cartridges.size()];
        int i = 0;

        if (log.isDebugEnabled()) {
            log.debug("Registered Cartridges : \n");
        }
        for (Cartridge cartridge : cartridges) {
            if (log.isDebugEnabled()) {
                log.debug(cartridge);
            }
            cartridgeTypes[i] = cartridge.getType();
            i++;
        }

        return cartridgeTypes;
    }

    @Override
    public Cartridge getCartridge(String cartridgeUuid) throws CartridgeNotFoundException {
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeUuid);
        if (cartridge != null) {
            return cartridge;
        }

        String msg = "Could not find cartridge: [cartridge-uuid] " + cartridgeUuid;
        throw new CartridgeNotFoundException(msg);
    }

    @Override
    public Cartridge getCartridgeByTenant(String cartridgeType, int tenantId) throws CartridgeNotFoundException {
        // get the list of cartridges registered
        Collection<Cartridge> cartridges = CloudControllerContext.getInstance().getCartridges();
        if (cartridges == null || cartridges.size() == 0) {
            log.info("No registered Cartridge found.");
            return null;
        }
        for (Cartridge cartridge : cartridges) {
            if (log.isDebugEnabled()) {
                log.debug(cartridge);
            }
            if (cartridge.getType().equals(cartridgeType) && (cartridge.getTenantId() == tenantId)) {
                return cartridge;
            }
        }

        String msg = "Could not find cartridge: [cartridge-type]" + cartridgeType;
        throw new CartridgeNotFoundException(msg);
    }

    @Override
    public Cartridge[] getCartridgesByTenant(int tenantId) {

        Collection<Cartridge> allCartridges = CloudControllerContext.getInstance().getCartridges();
        List<Cartridge> cartridges = new ArrayList<Cartridge>();
        if (allCartridges == null || allCartridges.size() == 0) {
            log.info("No registered Cartridge found for [tenant-id]" + tenantId);
            return null;
        }
        for (Cartridge cartridge : allCartridges) {
            if (log.isDebugEnabled()) {
                log.debug(cartridge);
            }
            if (cartridge.getTenantId() == tenantId) {
                cartridges.add(cartridge);
            }
        }

        return cartridges.toArray(new Cartridge[cartridges.size()]);
    }

    @Override
    public boolean unregisterService(String clusterId) throws UnregisteredClusterException {
        final String clusterId_ = clusterId;

        ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId_);
        handleNullObject(ctxt, "Service unregistration failed. Invalid cluster id: " + clusterId);

        final String cartridgeUuid = ctxt.getCartridgeUuid();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeUuid);

        if (cartridge == null) {
            String msg =
                    String.format("Service unregistration failed. No matching cartridge found: [cartridge-uuid] %s " +
                            "[application-id] %s", cartridgeUuid, ctxt.getApplicationUuid());
            log.error(msg);
            throw new UnregisteredClusterException(msg);
        }

        Runnable terminateInTimeout = new Runnable() {
            @Override
            public void run() {
                ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId_);
                if (ctxt == null) {
                    String msg = String.format("Service unregistration failed. Cluster not found: [cluster-id] %s " +
                            "[application-id] %s", clusterId_, ctxt.getApplicationUuid());
                    log.error(msg);
                    return;
                }
                Collection<Member> members = TopologyManager.getTopology().
                        getService(ctxt.getCartridgeUuid()).getCluster(clusterId_).getMembers();
                //finding the responding members from the existing members in the topology.
                int sizeOfRespondingMembers = 0;
                for (Member member : members) {
                    if (member.getStatus().getCode() >= MemberStatus.Active.getCode()) {
                        sizeOfRespondingMembers++;
                    }
                }

                long endTime = System.currentTimeMillis() + ctxt.getTimeoutInMillis() * sizeOfRespondingMembers;
                while (System.currentTimeMillis() < endTime) {
                    CloudControllerUtil.sleep(1000);

                }

                // if there are still alive members
                if (members.size() > 0) {
                    //forcefully terminate them
                    for (Member member : members) {

                        try {
                            terminateInstance(member.getMemberId());
                        } catch (Exception e) {
                            // we are not gonna stop the execution due to errors.
                            log.warn((String.format("Instance termination failed of member [member-id] %s " +
                                    "[application-id] %s", member.getMemberId(), ctxt.getApplicationUuid())), e);

                        }
                    }
                }
            }
        };
        Runnable unregister = new Runnable() {
            public void run() {
                Lock lock = null;
                try {
                    lock = CloudControllerContext.getInstance().acquireClusterContextWriteLock();
                    ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId_);
                    if (ctxt == null) {
                        String msg = String.format("Service unregistration failed. Cluster not found: [cluster-id] %s " +
                                "[application-id] %s ", clusterId_, ctxt.getApplicationUuid());
                        log.error(msg);
                        return;
                    }
                    Collection<Member> members = TopologyManager.getTopology().
                            getService(ctxt.getCartridgeUuid()).getCluster(clusterId_).getMembers();

                    while (members.size() > 0) {
                        //waiting until all the members got removed from the Topology/ timed out
                        CloudControllerUtil.sleep(1000);
                    }

                    log.info(String.format("Unregistration of service cluster: [cluster-id] %s [application-id] %s",
                            clusterId_, ctxt.getApplicationUuid()));
                    deleteVolumes(ctxt);
                    onClusterRemoval(clusterId_);
                } finally {
                    if (lock != null) {
                        CloudControllerContext.getInstance().releaseWriteLock(lock);
                    }
                }
            }

            private void deleteVolumes(ClusterContext ctxt) {
                if (ctxt.isVolumeRequired()) {
                    Lock lock = null;
                    try {
                        lock = CloudControllerContext.getInstance().acquireCartridgesWriteLock();

                        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(ctxt.getCartridgeUuid());
                        if (cartridge != null && CloudControllerContext.getInstance().getIaasProviders(cartridge.getType()) != null && ctxt.getVolumes() != null) {
                            for (Volume volume : ctxt.getVolumes()) {
                                if (volume.getId() != null) {
                                    String iaasType = volume.getIaasType();
                                    Iaas iaas = CloudControllerContext.getInstance().getIaasProvider(cartridge.getType(), iaasType).getIaas();
                                    if (iaas != null) {
                                        try {
                                            // delete the volumes if remove on unsubscription is true.
                                            if (volume.isRemoveOntermination()) {
                                                iaas.deleteVolume(volume.getId());
                                                volume.setId(null);
                                            }
                                        } catch (Exception ignore) {
                                            if (log.isErrorEnabled()) {
                                                log.error((String.format("Error while deleting volume [id] %s " +
                                                                "[application-id] %s", volume.getId(),
                                                                ctxt.getApplicationUuid())), ignore);
                                            }
                                        }
                                    }
                                }
                            }
                            CloudControllerContext.getInstance().updateCartridge(cartridge);
                        }
                    } finally {
                        if (lock != null) {
                            CloudControllerContext.getInstance().releaseWriteLock(lock);
                        }
                    }
                }
            }
        };
        new Thread(terminateInTimeout).start();
        new Thread(unregister).start();
        return true;
    }

    /**
     * FIXME: A validate method shouldn't persist data
     */
    @Override
    public boolean validateDeploymentPolicyNetworkPartition(String cartridgeUuid, String networkPartitionUuid)
            throws InvalidPartitionException, InvalidCartridgeTypeException {

        NetworkPartition networkPartition = CloudControllerContext.getInstance().getNetworkPartition(networkPartitionUuid);
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireCartridgesWriteLock();

            List<String> validatedPartitions = CloudControllerContext.getInstance().getPartitionIds(cartridgeUuid);
            if (validatedPartitions != null) {
                // cache hit for this cartridge
                // get list of partitions
                if (log.isDebugEnabled()) {
                    log.debug("Partition validation cache hit for cartridge uuid: " + cartridgeUuid);
                }
            }

            Map<String, IaasProvider> partitionToIaasProviders = new ConcurrentHashMap<String, IaasProvider>();

            if (log.isDebugEnabled()) {
                log.debug("Deployment policy validation started for cartridge uuid: " + cartridgeUuid);
            }

            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeUuid);
            if (cartridge == null) {
                String msg = "Cartridge not found: [cartridge-uuid] " + cartridgeUuid;
                log.error(msg);
                throw new InvalidCartridgeTypeException(msg);
            }

            Map<String, Future<IaasProvider>> jobList = new HashMap<String, Future<IaasProvider>>();
            for (Partition partition : networkPartition.getPartitions()) {
                if (validatedPartitions != null && validatedPartitions.contains(partition.getUuid())) {
                    // partition cache hit
                    String provider = partition.getProvider();
                    IaasProvider iaasProvider = CloudControllerContext.getInstance()
                            .getIaasProvider(cartridge.getUuid(), provider);
                    partitionToIaasProviders.put(partition.getUuid(), iaasProvider);
                    continue;
                }

                Callable<IaasProvider> worker = new PartitionValidatorCallable(partition, cartridge);
                Future<IaasProvider> job = CloudControllerContext.getInstance()
                        .getExecutorService().submit(worker);
                jobList.put(partition.getUuid(), job);
            }

            // Retrieve the results of the concurrently performed sanity checks.
            for (Entry<String, Future<IaasProvider>> entry : jobList.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String partitionId = entry.getKey();
                Future<IaasProvider> job = entry.getValue();
                try {
                    // add to a temporary Map
                    IaasProvider iaasProvider = job.get();
                    if (iaasProvider != null) {
                        partitionToIaasProviders.put(partitionId, iaasProvider);
                    }

                    // add to cache
                    CloudControllerContext.getInstance().addToCartridgeTypeToPartitionIdMap(cartridgeUuid, partitionId);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Partition %s added to the cache against " +
                                "cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type] %s", partitionId,
                                cartridge.getTenantId(), cartridgeUuid, cartridge.getType()));
                    }
                } catch (Exception e) {
                    String message = "Could not cache partitions against the cartridge: [cartridge-uuid] " + cartridgeUuid;
                    log.error(message, e);
                    throw new InvalidPartitionException(message, e);
                }
            }

            // if and only if the deployment policy valid
            CloudControllerContext.getInstance().addIaasProviders(cartridgeUuid, partitionToIaasProviders);
            CloudControllerContext.getInstance().updateCartridge(cartridge);

            // persist data
            CloudControllerContext.getInstance().persist();

            log.info(String.format("All partitions [%s] were validated successfully, " +
                            "against the cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type] %s",
                    CloudControllerUtil.getPartitionIds(networkPartition.getPartitions()), cartridge.getTenantId(),
                    cartridgeUuid, cartridge.getType()));

            return true;
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private void onClusterRemoval(final String clusterId) {
        ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId);
        TopologyBuilder.handleClusterRemoved(ctxt);
        CloudControllerContext.getInstance().removeClusterContext(clusterId);
        CloudControllerContext.getInstance().removeMemberContextsOfCluster(clusterId);
        CloudControllerContext.getInstance().persist();
    }

    @Override
    public boolean validatePartition(Partition partition) throws InvalidPartitionException {
        handleNullObject(partition, "Partition validation failed. Partition is null.");

        String provider = partition.getProvider();
        String partitionUuid = partition.getUuid();

        handleNullObject(provider, String.format("Partition validation failed. Partition provider is null. " +
                "[partition-uuid] %s [partition-id] %s ", partitionUuid, partition.getId()));
        IaasProvider iaasProvider = CloudControllerConfig.getInstance().getIaasProvider(provider);

        return CloudControllerServiceUtil.validatePartition(partition, iaasProvider);
    }

    public ClusterContext getClusterContext(String clusterId) {
        return CloudControllerContext.getInstance().getClusterContext(clusterId);
    }

    @Override
    public boolean updateClusterStatus(String serviceName, String clusterId, String instanceId, ClusterStatus status) {
        //TODO
        return true;
    }

    private void handleNullObject(Object obj, String errorMsg) {
        if (obj == null) {
            log.error(errorMsg);
            throw new CloudControllerException(errorMsg);
        }
    }

    @Override
    public boolean createApplicationClusters(String appUuid, ApplicationClusterContext[] appClustersContexts) throws
            ApplicationClusterRegistrationException {
        if (appClustersContexts == null || appClustersContexts.length == 0) {
            String errorMsg = "No application cluster information found, unable to create clusters: " +
                    "[application-uuid] " + appUuid;
            log.error(errorMsg);
            throw new ApplicationClusterRegistrationException(errorMsg);
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireClusterContextWriteLock();

            // Create a Cluster Context obj. for each of the Clusters in the Application
            List<Cluster> clusters = new ArrayList<Cluster>();
            Map<String, List<String>> accessUrls = new HashMap<String, List<String>>();

            for (ApplicationClusterContext appClusterCtxt : appClustersContexts) {
                String clusterId = appClusterCtxt.getClusterId();
                if (appClusterCtxt.isLbCluster()) {
                    String[] dependencyClusterIDs = appClusterCtxt.getDependencyClusterIds();
                    if (dependencyClusterIDs != null) {
                        for (int i = 0; i < dependencyClusterIDs.length; i++) {

                            List<String> accessUrlPerCluster = new ArrayList();
                            Collection<ClusterPortMapping> clusterPortMappings =
                                    CloudControllerContext.getInstance().getClusterPortMappings(appUuid, clusterId);

                            for (ClusterPortMapping clusterPortMapping : clusterPortMappings) {
                                try {
                                    if (clusterPortMapping.isKubernetes()) {
                                        // Using type URI since only http, https, ftp, file, jar protocols are supported in URL
                                        URI accessUrl = new URI(clusterPortMapping.getProtocol(), null, appClusterCtxt.getHostName(),
                                                clusterPortMapping.getKubernetesServicePort(), null, null, null);
                                        accessUrlPerCluster.add(accessUrl.toString());
                                    } else {
                                        URI accessUrl = new URI(clusterPortMapping.getProtocol(), null, appClusterCtxt.getHostName(),
                                                clusterPortMapping.getProxyPort(), null, null, null);
                                        accessUrlPerCluster.add(accessUrl.toString());
                                    }
                                } catch (URISyntaxException e) {
                                    String message = "Could not generate access URL";
                                    log.error(message, e);
                                }
                            }
                            accessUrls.put(dependencyClusterIDs[i], accessUrlPerCluster);
                        }
                    }
                }
            }

            for (ApplicationClusterContext appClusterCtxt : appClustersContexts) {
                ClusterContext clusterContext = new ClusterContext(
                        appUuid, appClusterCtxt.getCartridgeUuid(), appClusterCtxt.getClusterId(),
                        appClusterCtxt.getTextPayload(), appClusterCtxt.getHostName(),
                        appClusterCtxt.isLbCluster(), appClusterCtxt.getProperties());

                if (appClusterCtxt.isVolumeRequired()) {
                    clusterContext.setVolumeRequired(true);
                    clusterContext.setVolumes(appClusterCtxt.getVolumes());
                }
                CloudControllerContext.getInstance().addClusterContext(clusterContext);

                // Create cluster object
                Cluster cluster = new Cluster(appClusterCtxt.getCartridgeUuid(), appClusterCtxt.getClusterId(),
                        appClusterCtxt.getDeploymentPolicyName(), appClusterCtxt.getAutoscalePolicyName(), appUuid);
                cluster.setLbCluster(false);
                cluster.setTenantRange(appClusterCtxt.getTenantRange());
                cluster.setHostNames(Arrays.asList(appClusterCtxt.getHostName()));
                cluster.setAccessUrls(accessUrls.get(appClusterCtxt.getClusterId()));

                if (appClusterCtxt.getProperties() != null) {
                    Properties properties = CloudControllerUtil.toJavaUtilProperties(appClusterCtxt.getProperties());
                    cluster.setProperties(properties);
                }

                clusters.add(cluster);
            }
            TopologyBuilder.handleApplicationClustersCreated(appUuid, clusters);
            CloudControllerContext.getInstance().persist();
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
        return true;
    }

    public boolean createClusterInstance(String serviceUuid, String clusterId,
                                         String alias, String instanceId, String partitionId,
                                         String networkPartitionUuid) throws ClusterInstanceCreationException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireClusterContextWriteLock();
            TopologyBuilder.handleClusterInstanceCreated(serviceUuid, clusterId, alias,
                    instanceId, partitionId, networkPartitionUuid);

            CloudControllerContext.getInstance().persist();
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
        return true;
    }

    @Override
    public KubernetesCluster[] getKubernetesClusters(int tenantId) {
        KubernetesCluster[] kubernetesClusters=CloudControllerContext.getInstance().getKubernetesClusters();
        List<KubernetesCluster> kubernetesClusterList = new ArrayList<KubernetesCluster>();
        for(int i=0;i<kubernetesClusters.length;i++){
            if(kubernetesClusters[i].getTenantId()==tenantId){
                kubernetesClusterList.add(kubernetesClusters[i]);
            }
        }
        return kubernetesClusterList.toArray(new KubernetesCluster[kubernetesClusterList.size()]);

    }

    @Override
    public KubernetesCluster getKubernetesCluster(String kubernetesClusterUuid) throws
            NonExistingKubernetesClusterException {
        return CloudControllerContext.getInstance().getKubernetesCluster(kubernetesClusterUuid);
    }

    @Override
    public KubernetesCluster getKubernetesClusterByTenant(String kubernetesClusterId,int tenantId) throws
            NonExistingKubernetesClusterException {
        for(KubernetesCluster kubernetesCluster:getKubernetesClusters(tenantId)){
            if(kubernetesCluster.getClusterId().equals(kubernetesClusterId)&&kubernetesCluster.getTenantId()==tenantId){
                return kubernetesCluster;
            }
        }
        return null;
    }

    @Override
    public KubernetesMaster getMasterForKubernetesCluster(String kubernetesClusterId) throws
            NonExistingKubernetesClusterException {
        return CloudControllerContext.getInstance().getKubernetesMasterInGroup(kubernetesClusterId);
    }

    @Override
    public KubernetesHost[] getHostsForKubernetesCluster(String kubernetesClusterId) throws
            NonExistingKubernetesClusterException {
        return CloudControllerContext.getInstance().getKubernetesHostsInGroup(kubernetesClusterId);
    }


    @Override
    public boolean addKubernetesCluster(KubernetesCluster kubernetesCluster) throws InvalidKubernetesClusterException,
            KubernetesClusterAlreadyExistsException {
        if (kubernetesCluster == null) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster cannot be null");
        }

        try {
            if (CloudControllerContext.getInstance().getKubernetesCluster(kubernetesCluster.getClusterUuid()) != null) {
                throw new KubernetesClusterAlreadyExistsException("Kubernetes cluster already exists: " +
                        "[kubernetes-cluster] " + kubernetesCluster.getClusterId());
            }
        } catch (NonExistingKubernetesClusterException ignore) {
        }
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info(String.format("Adding kubernetes cluster: [tenant-id] %d [kubernetes-cluster-uuid] %s " +
                                "[kubernetes-cluster-id] %s", kubernetesCluster.getTenantId(),
                        kubernetesCluster.getClusterUuid(), kubernetesCluster.getClusterId()));
            }
            CloudControllerUtil.validateKubernetesCluster(kubernetesCluster);

            // Add to information model
            CloudControllerContext.getInstance().addKubernetesCluster(kubernetesCluster);
            CloudControllerContext.getInstance().persist();

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes cluster added successfully: [tenant-id] %d " +
                                "[kubernetes-cluster-uuid] %s [kubernetes-cluster-id] %s",
                        kubernetesCluster.getTenantId(), kubernetesCluster.getClusterUuid(),
                        kubernetesCluster.getClusterId()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesClusterException(e.getMessage(), e);
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public boolean updateKubernetesCluster(KubernetesCluster kubernetesCluster) throws InvalidKubernetesClusterException {
        if (kubernetesCluster == null) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster cannot be null");
        }
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info(String.format("Updating kubernetes cluster: [tenant-id] %d [kubernetes-cluster-uuid] %s " +
                                "[kubernetes-cluster-id] %s", kubernetesCluster.getTenantId(),
                        kubernetesCluster.getClusterUuid(), kubernetesCluster.getClusterId()));
            }
            CloudControllerUtil.validateKubernetesCluster(kubernetesCluster);

            // Updating the information model
            CloudControllerContext.getInstance().updateKubernetesCluster(kubernetesCluster);
            CloudControllerContext.getInstance().persist();

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes cluster updated successfully: [tenant-id] %d " +
                                "[kubernetes-cluster-uuid] %s [kubernetes-cluster-id] %s",
                        kubernetesCluster.getTenantId(), kubernetesCluster.getClusterUuid(),
                        kubernetesCluster.getClusterId()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesClusterException(e.getMessage(), e);
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public boolean addKubernetesHost(String groupUuid, KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesClusterException {
        if (kubernetesHost == null) {
            throw new InvalidKubernetesHostException("Kubernetes host cannot be null");
        }
        if (StringUtils.isEmpty(groupUuid)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id cannot be null");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info(String.format("Adding kubernetes host for kubernetes cluster: [kubernetes-cluster-id] %s " +
                        "[hostname] %s", groupUuid, kubernetesHost.getHostname()));
            }
            CloudControllerUtil.validateKubernetesHost(kubernetesHost);

            KubernetesCluster kubernetesCluster = getKubernetesCluster(groupUuid);
            ArrayList<KubernetesHost> kubernetesHostArrayList;

            if (kubernetesCluster.getKubernetesHosts() == null) {
                kubernetesHostArrayList = new ArrayList<KubernetesHost>();
            } else {
                if (CloudControllerContext.getInstance().kubernetesHostExists(kubernetesHost.getHostId())) {
                    throw new InvalidKubernetesHostException("Kubernetes host already exists: [kubernetes-host-id] " +
                            kubernetesHost.getHostId());
                }
                kubernetesHostArrayList = new
                        ArrayList<KubernetesHost>(Arrays.asList(kubernetesCluster.getKubernetesHosts()));
            }
            kubernetesHostArrayList.add(kubernetesHost);

            // Update information model
            kubernetesCluster.setKubernetesHosts(kubernetesHostArrayList.toArray(new KubernetesHost[kubernetesHostArrayList.size()]));
            CloudControllerContext.getInstance().updateKubernetesCluster(kubernetesCluster);
            CloudControllerContext.getInstance().persist();

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes host added successfully: [kubernetes-cluster-id] %s " +
                                "[kubernetes-host-id] %s", kubernetesCluster.getClusterUuid(), kubernetesHost.getHostId()));
            }

            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesHostException(e.getMessage(), e);
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public boolean removeKubernetesCluster(String groupUuid) throws NonExistingKubernetesClusterException {
        if (StringUtils.isEmpty(groupUuid)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id cannot be empty");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info("Removing Kubernetes cluster: " + groupUuid);
            }
            // Remove entry from information model
            CloudControllerContext.getInstance().removeKubernetesCluster(groupUuid);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes cluster removed successfully: [kubernetes-cluster-uuid] %s",
                        groupUuid));
            }

            CloudControllerContext.getInstance().persist();

        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
        return true;
    }

    @Override
    public boolean removeKubernetesHost(String kubernetesHostId) throws NonExistingKubernetesHostException {
        if (kubernetesHostId == null) {
            throw new NonExistingKubernetesHostException("Kubernetes host id cannot be null");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info("Removing Kubernetes Host: " + kubernetesHostId);
            }
            try {
                KubernetesCluster kubernetesClusterStored = CloudControllerContext.getInstance().getKubernetesClusterContainingHost(kubernetesHostId);

                // Kubernetes master cannot be removed
                if (kubernetesClusterStored.getKubernetesMaster().getHostId().equals(kubernetesHostId)) {
                    throw new NonExistingKubernetesHostException("Kubernetes master is not allowed to be removed " +
                            "[kubernetes-host-id] " + kubernetesHostId);
                }

                List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
                for (KubernetesHost kubernetesHost : kubernetesClusterStored.getKubernetesHosts()) {
                    if (!kubernetesHost.getHostId().equals(kubernetesHostId)) {
                        kubernetesHostList.add(kubernetesHost);
                    }
                }
                // member count will be equal only when host object was not found
                if (kubernetesHostList.size() == kubernetesClusterStored.getKubernetesHosts().length) {
                    throw new NonExistingKubernetesHostException("Kubernetes host not found for [kubernetes-host-id] " +
                            "" + kubernetesHostId);
                }
                KubernetesHost[] kubernetesHostsArray = new KubernetesHost[kubernetesHostList.size()];
                kubernetesHostList.toArray(kubernetesHostsArray);

                // Update information model
                kubernetesClusterStored.setKubernetesHosts(kubernetesHostsArray);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Kubernetes host removed successfully: [kubernetes-host-id] %s", kubernetesHostId));
                }

                CloudControllerContext.getInstance().persist();

                return true;
            } catch (Exception e) {
                throw new NonExistingKubernetesHostException(e.getMessage(), e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException, NonExistingKubernetesMasterException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();
            CloudControllerUtil.validateKubernetesMaster(kubernetesMaster);
            if (log.isInfoEnabled()) {
                log.info("Updating Kubernetes master: " + kubernetesMaster);
            }
            try {
                KubernetesCluster kubernetesClusterStored = CloudControllerContext.getInstance().getKubernetesClusterContainingHost(kubernetesMaster.getHostId());

                // Update information model
                kubernetesClusterStored.setKubernetesMaster(kubernetesMaster);

                CloudControllerContext.getInstance().persist();

                if (log.isInfoEnabled()) {
                    log.info(String.format("Kubernetes master updated successfully: [id] %s", kubernetesMaster.getHostId()));
                }

                return true;
            } catch (Exception e) {
                throw new InvalidKubernetesMasterException(e.getMessage(), e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public boolean updateKubernetesHost(KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesHostException {

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();
            CloudControllerUtil.validateKubernetesHost(kubernetesHost);
            if (log.isInfoEnabled()) {
                log.info("Updating Kubernetes Host: [kubernetes-host-id] " + kubernetesHost);
            }

            try {
                KubernetesCluster kubernetesClusterStored = CloudControllerContext.getInstance().getKubernetesClusterContainingHost(kubernetesHost.getHostId());
                KubernetesHost[] kubernetesHosts = kubernetesClusterStored.getKubernetesHosts();
                for (int i = 0; i < kubernetesHosts.length; i++) {
                    if (kubernetesHosts[i].getHostId().equals(kubernetesHost.getHostId())) {
                        // Update the information model
                        kubernetesHosts[i] = kubernetesHost;

                        if (log.isInfoEnabled()) {
                            log.info(String.format("Kubernetes host updated successfully: [kubernetes-host-id] %s", kubernetesHost.getHostId()));
                        }

                        CloudControllerContext.getInstance().updateKubernetesCluster(kubernetesClusterStored);
                        CloudControllerContext.getInstance().persist();
                        return true;
                    }
                }
            } catch (Exception e) {
                throw new InvalidKubernetesHostException(e.getMessage(), e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
        throw new NonExistingKubernetesHostException("Kubernetes host not found [kubernetes-host-id] " + kubernetesHost.getHostId());
    }

    @Override
    public boolean addNetworkPartition(NetworkPartition networkPartition) throws
            NetworkPartitionAlreadyExistsException,
            InvalidNetworkPartitionException {

        handleNullObject(networkPartition, "Network Partition is null");
        handleNullObject(networkPartition.getUuid(), "Network Partition Id is null");

        if (log.isInfoEnabled()) {
            log.info(String.format("Adding network partition: [tenant-id] %d [network-partition-uuid] %s " +
                            "[network-partition-id] %s", networkPartition.getTenantId(), networkPartition.getUuid(),
                    networkPartition.getId()));
        }

        String networkPartitionUuid = networkPartition.getUuid();
        if (cloudControllerContext.getNetworkPartition(networkPartitionUuid) != null) {
            String logMessage = String.format("Network partition already exists: [tenant-id] %d " +
                            "[network-partition-uuid]" +
                            " %s [network-partition-id] %s", networkPartition.getTenantId(), networkPartition.getUuid(),
                    networkPartition.getId());
            String message = String.format("Network partition already exists: [network-partition-id] %s",
                    networkPartition.getId());

            log.error(logMessage);
            throw new NetworkPartitionAlreadyExistsException(message);
        }

        String networkPartitionId = networkPartition.getId();

        if (cloudControllerContext.getNetworkPartitionForTenant(networkPartitionId,
                networkPartition.getTenantId()) != null) {
            String logMessage = String.format("Network partition already exists: [tenant-id] %d " +
                            "[network-partition-uuid]" +
                            " %s [network-partition-id] %s", networkPartition.getTenantId(), networkPartition.getUuid(),
                    networkPartition.getId());
            String message = String.format("Network partition already exists: [network-partition-id] %s",
                    networkPartition.getId());
            log.error(logMessage);
            throw new NetworkPartitionAlreadyExistsException(message);
        }

        if (networkPartition.getPartitions() != null && networkPartition.getPartitions().length != 0) {
            for (Partition partition : networkPartition.getPartitions()) {
                if (partition != null) {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Validating partition: [tenant-id] %d[network-partition-uuid] %s " +
                                        "[network-partition-id] %s [partition-id] %s",
                                networkPartition.getTenantId(), networkPartitionUuid, networkPartition.getId(),
                                partition.getUuid()));
                    }
                    // Overwrites partition provider with network partition provider
                    partition.setProvider(networkPartition.getProvider());
                    try {
                        validatePartition(partition);
                    } catch (InvalidPartitionException e) {
                        //Following message is shown to the end user in all the the API clients(GUI/CLI/Rest API)
                        throw new InvalidNetworkPartitionException(String.format("Network partition " +
                                        "[tenant-id] %d [network-partition-uuid] %s [network-partition-id] %s , " +
                                        "is invalid since the partition %s is invalid",
                                networkPartition.getTenantId(), networkPartition.getUuid(), networkPartition.getId(),
                                partition.getUuid()), e);
                    }
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Partition validated successfully: [tenant-id] %d " +
                                        "[network-partition-uuid] %s [network-partition-id] %s [partition-id] %s",
                                networkPartition.getTenantId(), networkPartition.getUuid(), networkPartition.getId(),
                                partition.getUuid()));
                    }
                }
            }
        } else {
            //Following message is shown to the end user in all the the API clients(GUI/CLI/Rest API)
            throw new InvalidNetworkPartitionException(String.format("Network partition [tenant-id] %d " +
                            "[network-partition-uuid] %s [network-partition-id] %s, doesn't not have any partitions ",
                    networkPartition.getTenantId(), networkPartition.getUuid(), networkPartition.getId()));
        }

        // adding network partition to CC-Context
        CloudControllerContext.getInstance().addNetworkPartition(networkPartition);
        // persisting CC-Context
        CloudControllerContext.getInstance().persist();
        if (log.isInfoEnabled()) {
            log.info(String.format("Network partition added successfully: [tenant-id] %d [network-partition-uuid] %s " +
                    "[network-partition-id] %s", networkPartition.getTenantId(), networkPartition.getUuid(),
                    networkPartition.getId()));
        }
        return true;
    }

    @Override
    public boolean removeNetworkPartition(String networkPartitionId,
                                          int tenantId) throws NetworkPartitionNotExistsException {

        try {
            NetworkPartition networkPartition = cloudControllerContext.getNetworkPartitionForTenant
                    (networkPartitionId, tenantId);

            if (networkPartition != null) {
                String networkPartitionUuid = networkPartition.getUuid();

                if (log.isInfoEnabled()) {
                    log.info(String.format("Removing network partition: [tenant-id] %d [network-partition-uuid] %s " +
                            "[network-partition-id] %s", networkPartition.getTenantId(), networkPartitionUuid,
                            networkPartitionId));
                }
                handleNullObject(networkPartitionId, "Network Partition Id is null");

                if (networkPartitionUuid == null) {
                    String message = String.format("Network partition not found: [network-partition-id] %s",
                            networkPartitionId);
                    log.error(message);
                    throw new NetworkPartitionNotExistsException(message);
                }
                // removing from CC-Context
                CloudControllerContext.getInstance().removeNetworkPartition(networkPartitionUuid);
                // persisting CC-Context
                CloudControllerContext.getInstance().persist();
                if (log.isInfoEnabled()) {
                    log.info(String.format("Network partition removed successfully: [tenant-id] %d " +
                            "[network-partition-uuid] %s [network-partition-id] %s", networkPartition.getTenantId(),
                            networkPartitionUuid, networkPartitionId));
                }
            } else {
                String message = String.format("Network partition not found: [network-partition-id] %s",
                        networkPartitionId);
                log.error(message);
                throw new NetworkPartitionNotExistsException(message);
            }




        } catch (Exception e) {
            String message = e.getMessage();
            log.error(message);
            throw new CloudControllerException(message, e);
        }
        return true;
    }

    @Override
    public boolean updateNetworkPartition(NetworkPartition networkPartition) throws NetworkPartitionNotExistsException {
        try {
            handleNullObject(networkPartition, "Network Partition is null");
            handleNullObject(networkPartition.getUuid(), "Network Partition Id is null");

            if (log.isInfoEnabled()) {
                log.info(String.format("Updating network partition: [tenant-id] %d [network-partition-uuid] %s " +
                                "[network-partition-id] %s", networkPartition.getTenantId(),
                        networkPartition.getUuid(), networkPartition.getId()));
            }

            String networkPartitionId = networkPartition.getId();
            String networkPartitionUuid = networkPartition.getUuid();
            if (cloudControllerContext.getNetworkPartition(networkPartitionUuid) == null) {
                String message = String.format("Network partition not found: [tenant-id] %d [network-partition-uuid] %s " +
                                "[network-partition-id] %s", networkPartition.getTenantId(),
                        networkPartition.getUuid(), networkPartition.getId());
                log.error(message);
                throw new NetworkPartitionNotExistsException(message);
            }

            if (networkPartition.getPartitions() != null) {
                for (Partition partition : networkPartition.getPartitions()) {
                    if (partition != null) {
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Validating partition: [tenant-id] %d [network-partition-uuid] %s" +
                                            "[network-partition-id] %s [partition-uuid] %s [partition-id] %s ",
                                    networkPartition.getTenantId(), networkPartitionUuid, networkPartitionId,
                                    partition.getUuid(), partition.getId()));
                        }
                        // Overwrites partition provider with network partition provider
                        partition.setProvider(networkPartition.getProvider());
                        validatePartition(partition);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Partition validated successfully: [tenant-id] %d " +
                                            "[network-partition-uuid] %s [network-partition-id] %s [partition-uuid] " +
                                            "%s [partition-id] %s ", networkPartition.getTenantId(),
                                    networkPartitionUuid, networkPartitionId, partition.getUuid(), partition.getId()));
                        }
                    }
                }
            }

            // overriding network partition to CC-Context
            CloudControllerContext.getInstance().addNetworkPartition(networkPartition);
            // persisting CC-Context
            CloudControllerContext.getInstance().persist();
            if (log.isInfoEnabled()) {
                log.info(String.format("Network partition updated successfully: [tenant-id] %d [network-partition-uuid] %s " +
                                "[network-partition-id] %s", networkPartition.getTenantId(),
                        networkPartition.getUuid(), networkPartition.getId()));
            }
            return true;
        } catch (Exception e) {
            String message = e.getMessage();
            log.error(message);
            throw new CloudControllerException(message, e);
        }
    }

    @Override
    public NetworkPartition[] getNetworkPartitions() {
        try {
            Collection<NetworkPartition> networkPartitionList = cloudControllerContext.getNetworkPartitions();
            return networkPartitionList.toArray(new NetworkPartition[networkPartitionList.size()]);
        } catch (Exception e) {
            String message = "Could not get network partitions";
            log.error(message);
            throw new CloudControllerException(message, e);
        }
    }

    @Override
    public NetworkPartition[] getNetworkPartitionsByTenant(int tenantId) {
        NetworkPartition[] allNetworkPartitions = getNetworkPartitions();
        List<NetworkPartition> networkPartitions = new ArrayList<NetworkPartition>();

        if (allNetworkPartitions != null) {
            for (NetworkPartition networkPartition : allNetworkPartitions) {
                if (networkPartition.getTenantId() == tenantId) {
                    networkPartitions.add(networkPartition);
                }
            }
        }
        return networkPartitions.toArray(new NetworkPartition[networkPartitions.size()]);
    }

    @Override
    public NetworkPartition getNetworkPartitionByTenant(String networkPartitionId, int tenantId) {
        NetworkPartition[] allNetworkPartitions = getNetworkPartitions();
        NetworkPartition networkPartition = null;

        if (allNetworkPartitions != null) {
            for (NetworkPartition networkPartition1 : allNetworkPartitions) {
                if (networkPartition1.getTenantId() == tenantId && networkPartition1.getId().equals
                        (networkPartitionId)) {
                    networkPartition = networkPartition1;
                }
            }
        }
        return networkPartition;
    }

    @Override
    public NetworkPartition getNetworkPartition(String networkPartitionUuid) {
        try {
            return CloudControllerContext.getInstance().getNetworkPartition(networkPartitionUuid);
        } catch (Exception e) {
            String message = String.format("Could not get network partition: [network-partition-uuid] %s " +
                            "[network-partition-id] %s", getNetworkPartition(networkPartitionUuid).getUuid(),
                    networkPartitionUuid);
            log.error(message);
            throw new CloudControllerException(message, e);
        }
    }

    public String getNetworkPartitionUuid(String networkPartitionId, int tenantId) {
        NetworkPartition networkPartition = getNetworkPartitionByTenant(networkPartitionId, tenantId);
        return networkPartition.getUuid();
    }

    public Partition[] getPartitionsByNetworkPartition(String networkPartitionId, int tenantId) {
        NetworkPartition networkPartition = getNetworkPartitionByTenant(networkPartitionId, tenantId);
        return networkPartition.getPartitions();
    }

    @Override
    public String[] getIaasProviders() {

        try {
            Collection<IaasProvider> iaasProviders = CloudControllerConfig.getInstance().getIaasProviders();
            List<String> iaases = new ArrayList<String>();

            for (IaasProvider iaas : iaasProviders) {
                iaases.add(iaas.getType());
            }

            return iaases.toArray(new String[iaases.size()]);
        } catch (Exception e) {
            String message = String.format("Could not get Iaas Providers");
            log.error(message);
            throw new CloudControllerException(message, e);
        }

    }

}
