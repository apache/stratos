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
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.Dependencies;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.messaging.publisher.StatisticsDataPublisher;
import org.apache.stratos.cloud.controller.messaging.publisher.TopologyEventPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyManager;
import org.apache.stratos.cloud.controller.services.CloudControllerService;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;

import java.net.MalformedURLException;
import java.net.URL;
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

    private CloudControllerContext cloudControllerContext = CloudControllerContext.getInstance();
    private ExecutorService executorService;

    public CloudControllerServiceImpl() {
        executorService = StratosThreadPool.getExecutorService("cloud.controller.instance.manager.thread.pool", 50);

    }

    public void addCartridge(CartridgeConfig cartridgeConfig) throws InvalidCartridgeDefinitionException,
            InvalidIaasProviderException, CartridgeAlreadyExistsException {

        handleNullObject(cartridgeConfig, "Cartridge definition is null");

        if(log.isInfoEnabled()) {
            log.info("Adding cartridge: [cartridge-type] " + cartridgeConfig.getType());
        }
        if (log.isDebugEnabled()) {
            log.debug("Cartridge definition: " + cartridgeConfig.toString());
        }

        Cartridge cartridge = null;
        try {
            cartridge = CloudControllerUtil.toCartridge(cartridgeConfig);
        } catch (Exception e) {
            String message = "Invalid cartridge definition: [cartridge-type] " + cartridgeConfig.getType();
            log.error(message, e);
            throw new InvalidCartridgeDefinitionException(message, e);
        }

        String cartridgeType = cartridge.getType();
        if (cloudControllerContext.getCartridge(cartridgeType) != null) {
            String message = "Cartridge already exists: [cartridge-type] " + cartridgeType;
            log.error(message);
            throw new CartridgeAlreadyExistsException(message);
        }

        // Add cartridge to the cloud controller context and persist
        CloudControllerContext.getInstance().addCartridge(cartridge);
        CloudControllerContext.getInstance().persist();

        List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
        cartridgeList.add(cartridge);

        TopologyBuilder.handleServiceCreated(cartridgeList);

        if(log.isInfoEnabled()) {
            log.info("Successfully added cartridge: [cartridge-type] " + cartridgeType);
        }
    }

	@Override
	public void updateCartridge(CartridgeConfig cartridgeConfig) throws InvalidCartridgeDefinitionException,
	                                                                    InvalidIaasProviderException,
	                                                                    CartridgeDefinitionNotExistsException {

		handleNullObject(cartridgeConfig, "Cartridge definition is null");

		if (log.isInfoEnabled()) {
			log.info("Adding cartridge: [cartridge-type] " + cartridgeConfig.getType());
		}
		if (log.isDebugEnabled()) {
			log.debug("Cartridge definition: " + cartridgeConfig.toString());
		}

		Cartridge cartridge = null;
		try {
			cartridge = CloudControllerUtil.toCartridge(cartridgeConfig);
		} catch (Exception e) {
			String msg = "Invalid cartridge definition: [cartridge-type] " + cartridgeConfig.getType();
			log.error(msg, e);
			throw new InvalidCartridgeDefinitionException(msg, e);
		}

		// TODO transaction begins
		String cartridgeType = cartridge.getType();
		// Undeploy if already deployed
		if (cloudControllerContext.getCartridge(cartridgeType) != null) {
			Cartridge cartridgeToBeRemoved = cloudControllerContext.getCartridge(cartridgeType);
			// undeploy
			try {
				removeCartridge(cartridgeToBeRemoved.getType());
			} catch (InvalidCartridgeTypeException ignore) {
			}
			copyIaasProviders(cartridge, cartridgeToBeRemoved);
		}
		else{
			throw new CartridgeDefinitionNotExistsException("This cartridge definition not exists");
		}

		// Add cartridge to the cloud controller context and persist
		CloudControllerContext.getInstance().addCartridge(cartridge);
		CloudControllerContext.getInstance().persist();

		List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
		cartridgeList.add(cartridge);

		TopologyBuilder.handleServiceCreated(cartridgeList);
		// transaction ends

		if (log.isInfoEnabled()) {
			log.info("Successfully added cartridge: [cartridge-type] " + cartridgeType);
		}
	}

    private void copyIaasProviders(Cartridge destCartridge,
                                   Cartridge sourceCartridge) {

        List<IaasProvider> newIaasProviders = destCartridge.getIaases();
        Map<String, IaasProvider> iaasProviderMap = sourceCartridge.getPartitionToIaasProvider();

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
                destCartridge.addIaasProvider(partitionId, newIaasProviders.get(newIaasProviders.indexOf(iaasProvider)));
            }
        }
    }

    public void removeCartridge(String cartridgeType) throws InvalidCartridgeTypeException {

        Cartridge cartridge = null;
        if ((cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType)) != null) {
            if (CloudControllerContext.getInstance().getCartridges().remove(cartridge)) {
                // invalidate partition validation cache
                CloudControllerContext.getInstance().removeFromCartridgeTypeToPartitionIds(cartridgeType);

                if (log.isDebugEnabled()) {
                    log.debug("Partition cache invalidated for cartridge " + cartridgeType);
                }

                CloudControllerContext.getInstance().persist();

                // sends the service removed event
                List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
                cartridgeList.add(cartridge);
                TopologyBuilder.handleServiceRemoved(cartridgeList);

                if (log.isInfoEnabled()) {
                    log.info("Successfully removed cartridge: [cartridge-type] " + cartridgeType);
                }
                return;
            }
        }
        String msg = "Cartridge not found: [cartridge-type] " + cartridgeType;
        log.error(msg);
        throw new InvalidCartridgeTypeException(msg);
    }

    public void addServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException {

        if (servicegroup == null) {
            String msg = "Invalid ServiceGroup Definition: Definition is null.";
            log.error(msg);
            throw new IllegalArgumentException(msg);

        }

        if (log.isDebugEnabled()) {
            log.debug("CloudControllerServiceImpl:addServiceGroup:" + servicegroup.getName());
        }

        String[] subGroups = servicegroup.getCartridges();


        if (log.isDebugEnabled()) {
            log.debug("CloudControllerServiceImpl:addServiceGroup:subGroups" + subGroups);
            if (subGroups != null) {
                log.debug("CloudControllerServiceImpl:addServiceGroup:subGroups:size" + subGroups.length);
            } else {
                log.debug("CloudControllerServiceImpl:addServiceGroup:subGroups: is null");
            }
        }


        Dependencies dependencies = servicegroup.getDependencies();

        if (log.isDebugEnabled()) {
            log.debug("CloudControllerServiceImpl:addServiceGroup:dependencies" + dependencies);
        }

        if (dependencies != null) {
            String[] startupOrders = dependencies.getStartupOrders();

            if (log.isDebugEnabled()) {
                log.debug("CloudControllerServiceImpl:addServiceGroup:startupOrders" + startupOrders);

                if (startupOrders != null) {
                    log.debug("CloudControllerServiceImpl:addServiceGroup:startupOrder:size" + startupOrders.length);
                } else {
                    log.debug("CloudControllerServiceImpl:addServiceGroup:startupOrder: is null");
                }
            }
        }

        CloudControllerContext.getInstance().addServiceGroup(servicegroup);
        CloudControllerContext.getInstance().persist();
    }

    public void removeServiceGroup(String name) throws InvalidServiceGroupException {
        if (log.isDebugEnabled()) {
            log.debug("CloudControllerServiceImpl:removeServiceGroup: " + name);
        }

        ServiceGroup serviceGroup = null;

        serviceGroup = CloudControllerContext.getInstance().getServiceGroup(name);

        if (serviceGroup != null) {
            if (CloudControllerContext.getInstance().getServiceGroups().remove(serviceGroup)) {
                CloudControllerContext.getInstance().persist();
                if (log.isInfoEnabled()) {
                    log.info("Successfully removed the cartridge group: [group-name] " + serviceGroup);
                }
                return;
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
            throw new InvalidServiceGroupException("Invalid cartridge group: [group-name] " + serviceGroup);
        }

        return serviceGroup.getSubGroups();
    }

    /**
     *
     */
    public String[] getServiceGroupCartridges(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid cartridge group: [group-name] " + serviceGroup);
        }
        String[] cs = serviceGroup.getCartridges();
        return cs;

    }

    public Dependencies getServiceGroupDependencies(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid cartridge group: [group-name] " + serviceGroup);
        }
        return serviceGroup.getDependencies();
    }

    @Override
    public MemberContext[] startInstances(InstanceContext[] instanceContexts)
            throws CartridgeNotFoundException, InvalidIaasProviderException, CloudControllerException {

        handleNullObject(instanceContexts, "Instance start-up failed, member contexts is null");

        List<MemberContext> memberContextList = new ArrayList<MemberContext>();
        for(InstanceContext instanceContext : instanceContexts) {
            if(instanceContext != null) {
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
            String cartridgeType = clusterContext.getCartridgeType();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = "Could not startup instance, cartridge not found: [cartridge-type] " + cartridgeType;
                log.error(msg);
                throw new CartridgeNotFoundException(msg);
            }

            // Validate iaas provider
            IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);
            if (iaasProvider == null) {
                String msg = String.format("Could not start instance, " +
                                "IaaS provider not found in cartridge %s for partition %s, " +
                                "partitions found: %s ", cartridgeType, partitionId,
                        cartridge.getPartitionToIaasProvider().keySet().toString());
                log.error(msg);
                throw new InvalidIaasProviderException(msg);
            }

            // Generate member ID
            String memberId = generateMemberId(clusterId);

            // Create member context
            String applicationId = clusterContext.getApplicationId();
            MemberContext memberContext = createMemberContext(applicationId, cartridgeType, memberId, instanceContext);

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

            Iaas iaas = iaasProvider.getIaas();
            if (clusterContext.isVolumeRequired()) {
                addToPayload(payload, PERSISTENCE_MAPPING, getPersistencePayload(clusterContext, iaas).toString());
            }

            if (log.isDebugEnabled()) {
                log.debug("Payload: " + payload.toString());
            }

            iaasProvider.setPayload(payload.toString().getBytes());
            iaas.setDynamicPayload(iaasProvider.getPayload());

            if (clusterContext.isVolumeRequired()) {
                if (clusterContext.getVolumes() != null) {
                    for (Volume volume : clusterContext.getVolumes()) {
                        if (volume.getId() == null) {
                            // Create a new volume
                            createVolumeAndSetInClusterContext(volume, iaasProvider);
                        }
                    }
                }
            }

            // Handle member created event
            TopologyBuilder.handleMemberCreatedEvent(memberContext);

            // Persist member context
            CloudControllerContext.getInstance().addMemberContext(memberContext);
            CloudControllerContext.getInstance().persist();

            // Start instance in a new thread
            if (log.isDebugEnabled()) {
                log.debug(String.format("Starting instance creator thread: [cluster] %s [cluster-instance] %s " +
                        "[member] %s", instanceContext.getClusterId(), instanceContext.getClusterInstanceId(),
                        memberId));
            }
            executorService.execute(new InstanceCreator(memberContext, iaasProvider));

            return memberContext;
        } catch (Exception e) {
            String msg = String.format("Could not start instance: [cluster] %s [cluster-instance] %s",
                    instanceContext.getClusterId(), instanceContext.getClusterInstanceId());
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }

    private MemberContext createMemberContext(String applicationId, String cartridgeType, String memberId,
                                              InstanceContext instanceContext) {
        MemberContext memberContext = new MemberContext(
                applicationId, cartridgeType, instanceContext.getClusterId(), memberId);

        memberContext.setClusterInstanceId(instanceContext.getClusterInstanceId());
        memberContext.setNetworkPartitionId(instanceContext.getNetworkPartitionId());
        memberContext.setPartition(instanceContext.getPartition());
        memberContext.setInitTime(instanceContext.getInitTime());
        memberContext.setProperties(instanceContext.getProperties());
        memberContext.setInitTime(System.currentTimeMillis());

        return memberContext;
    }

    private void createVolumeAndSetInClusterContext(Volume volume,
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

    private String generateMemberId(String clusterId) {
        UUID memberId = UUID.randomUUID();
        return clusterId + memberId.toString();
    }

    @Override
    public void terminateInstance(String memberId) throws InvalidMemberException,
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
                                        "Member pending termination in ReadyToShutdown state exceeded expiry time. This member has to be manually deleted: %s",
                                        memberContext.getMemberId()));
                            }

                            CloudControllerServiceUtil.executeMemberTerminationPostProcess(memberContext);
                            return;
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
    public void terminateInstances(String clusterId) throws InvalidClusterException {

        log.info("Starting to terminate all instances of cluster : "
                + clusterId);

        handleNullObject(clusterId, "Instance termination failed. Cluster id is null.");

        List<MemberContext> memberContexts = CloudControllerContext.getInstance().getMemberContextsOfClusterId(clusterId);
        if (memberContexts == null) {
            String msg = "Instance termination failed. No members found for cluster id: " + clusterId;
            log.warn(msg);
            return;
        }

        for (MemberContext memberContext : memberContexts) {
            executorService.execute(new InstanceTerminator(memberContext));
        }
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

        Properties properties = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
        String property = properties.getProperty(CloudControllerConstants.IS_LOAD_BALANCER);
        boolean isLb = property != null ? Boolean.parseBoolean(property) : false;
        TopologyBuilder.handleClusterCreated(registrant, isLb);
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
    public CartridgeInfo getCartridgeInfo(String cartridgeType) throws CartridgeNotFoundException {
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
        if (cartridge != null) {
            return CloudControllerUtil.toCartridgeInfo(cartridge);
        }

        String msg = "Could not find cartridge: [cartridge-type] " + cartridgeType;
        throw new CartridgeNotFoundException(msg);
    }

    @Override
    public void unregisterService(String clusterId) throws UnregisteredClusterException {
        final String clusterId_ = clusterId;

        ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId_);
        handleNullObject(ctxt, "Service unregistration failed. Invalid cluster id: " + clusterId);

        String cartridgeType = ctxt.getCartridgeType();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg =
                    "Service unregistration failed. No matching cartridge found: [cartridge-type] " + cartridgeType;
            log.error(msg);
            throw new UnregisteredClusterException(msg);
        }

        // TODO: Fix kubernetes config
//        if (StratosConstants.KUBERNETES_DEPLOYER_TYPE.equals(cartridge.getDeployerType())) {
//            unregisterDockerService(clusterId_);
//        } else {
            Runnable terminateInTimeout = new Runnable() {
                @Override
                public void run() {
                    ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId_);
                    if (ctxt == null) {
                        String msg = "Service unregistration failed. Cluster not found: [cluster-id] " + clusterId_;
                        log.error(msg);
                        return;
                    }
                    Collection<Member> members = TopologyManager.getTopology().
                            getService(ctxt.getCartridgeType()).getCluster(clusterId_).getMembers();
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

                    // if there're still alive members
                    if (members.size() > 0) {
                        //forcefully terminate them
                        for (Member member : members) {

                            try {
                                terminateInstance(member.getMemberId());
                            } catch (Exception e) {
                                // we are not gonna stop the execution due to errors.
                                log.warn("Instance termination failed of member [id] " + member.getMemberId(), e);
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
                            String msg = "Service unregistration failed. Cluster not found: [cluster-id] " + clusterId_;
                            log.error(msg);
                            return;
                        }
                        Collection<Member> members = TopologyManager.getTopology().
                                getService(ctxt.getCartridgeType()).getCluster(clusterId_).getMembers();

                        while (members.size() > 0) {
                            //waiting until all the members got removed from the Topology/ timed out
                            CloudControllerUtil.sleep(1000);
                        }

                        log.info("Unregistration of service cluster: " + clusterId_);
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

                            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(ctxt.getCartridgeType());
                            if (cartridge != null && cartridge.getIaases() != null && ctxt.getVolumes() != null) {
                                for (Volume volume : ctxt.getVolumes()) {
                                    if (volume.getId() != null) {
                                        String iaasType = volume.getIaasType();
                                        Iaas iaas = cartridge.getIaasProvider(iaasType).getIaas();
                                        if (iaas != null) {
                                            try {
                                                // delete the volumes if remove on unsubscription is true.
                                                if (volume.isRemoveOntermination()) {
                                                    iaas.deleteVolume(volume.getId());
                                                    volume.setId(null);
                                                }
                                            } catch (Exception ignore) {
                                                if (log.isErrorEnabled()) {
                                                    log.error("Error while deleting volume [id] " + volume.getId(), ignore);
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
   //     }
    }

    /**
     * FIXME: A validate method shouldn't persist data
     */
    @Override
    public boolean validateDeploymentPolicy(String cartridgeType, Partition[] partitions)
            throws InvalidPartitionException, InvalidCartridgeTypeException {

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireCartridgesWriteLock();

            List<String> validatedPartitions = CloudControllerContext.getInstance().getPartitionIds(cartridgeType);
            if (validatedPartitions != null) {
                // cache hit for this cartridge
                // get list of partitions
                if (log.isDebugEnabled()) {
                    log.debug("Partition validation cache hit for cartridge type: " + cartridgeType);
                }
            }

            Map<String, IaasProvider> partitionToIaasProviders =
                    new ConcurrentHashMap<String, IaasProvider>();

            if (log.isDebugEnabled()) {
                log.debug("Deployment policy validation started for cartridge type: " + cartridgeType);
            }

            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = "Invalid Cartridge Type: " + cartridgeType;
                log.error(msg);
                throw new InvalidCartridgeTypeException(msg);
            }

            Map<String, Future<IaasProvider>> jobList = new HashMap<String, Future<IaasProvider>>();
            for (Partition partition : partitions) {
                if (validatedPartitions != null && validatedPartitions.contains(partition.getId())) {
                    // partition cache hit
                    continue;
                }

                Callable<IaasProvider> worker = new PartitionValidatorCallable(
                        partition, cartridge);
                Future<IaasProvider> job = CloudControllerContext.getInstance()
                        .getExecutorService().submit(worker);
                jobList.put(partition.getId(), job);
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
                    CloudControllerContext.getInstance().addToCartridgeTypeToPartitionIdMap(cartridgeType, partitionId);

                    if (log.isDebugEnabled()) {
                        log.debug("Partition " + partitionId + " added to the cache against cartridge: " +
                                "[cartridge-type] " + cartridgeType);
                    }
                } catch (Exception e) {
                    String message = "Could not cache partitions against the cartridge: [cartridge-type] " + cartridgeType;
                    log.error(message, e);
                    throw new InvalidPartitionException(message, e);
                }
            }

            // if and only if the deployment policy valid
            cartridge.addIaasProviders(partitionToIaasProviders);
            CloudControllerContext.getInstance().updateCartridge(cartridge);

            // persist data
            CloudControllerContext.getInstance().persist();

            log.info("All partitions " + CloudControllerUtil.getPartitionIds(partitions) +
                    " were validated successfully, against the Cartridge: " + cartridgeType);

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
        String partitionId = partition.getId();

        handleNullObject(provider, "Partition [" + partitionId + "] validation failed. Partition provider is null.");
        IaasProvider iaasProvider = CloudControllerConfig.getInstance().getIaasProvider(provider);

        return CloudControllerServiceUtil.validatePartition(partition, iaasProvider);
    }

    public ClusterContext getClusterContext(String clusterId) {
        return CloudControllerContext.getInstance().getClusterContext(clusterId);
    }

    @Override
    public void updateClusterStatus(String serviceName, String clusterId, String instanceId, ClusterStatus status) {
        //TODO
    }

    private void handleNullObject(Object obj, String errorMsg) {
        if (obj == null) {
            log.error(errorMsg);
            throw new CloudControllerException(errorMsg);
        }
    }

    @Override
    public void createApplicationClusters(String appId, ApplicationClusterContext[] appClustersContexts) throws
            ApplicationClusterRegistrationException {
        if (appClustersContexts == null || appClustersContexts.length == 0) {
            String errorMsg = "No application cluster information found, unable to create clusters";
            log.error(errorMsg);
            throw new ApplicationClusterRegistrationException(errorMsg);
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireClusterContextWriteLock();

            // Create a Cluster Context obj. for each of the Clusters in the Application
            List<Cluster> clusters = new ArrayList<Cluster>();
	        Map<String,List<String>> accessUrls= new HashMap<String, List<String>>();

	        for (ApplicationClusterContext appClusterCtxt : appClustersContexts) {
		        if(appClusterCtxt.isLbCluster()) {
			        String[] dependencyClusterIDs = appClusterCtxt.getDependencyClusterIds();
			        if(dependencyClusterIDs!=null) {
				        for (int i = 0; i < dependencyClusterIDs.length; i++) {
					        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(
							        appClusterCtxt.getCartridgeType());
					        List<String> accessUrlPerCluster = new ArrayList();
					        List<PortMapping> portMappings = cartridge.getPortMappings();
					        for (PortMapping portMap : portMappings) {
                                try {
                                    if (portMap.isKubernetesServicePortMapping()) {
                                        URL accessUrl = new URL(portMap.getProtocol(), appClusterCtxt.getHostName(),
                                                portMap.getKubernetesServicePort(), "");
                                        accessUrlPerCluster.add(accessUrl.toString());
                                    } else {
                                        URL accessUrl = new URL(portMap.getProtocol(), appClusterCtxt.getHostName(),
                                                portMap.getProxyPort(), "");
                                        accessUrlPerCluster.add(accessUrl.toString());
                                    }
                                } catch (MalformedURLException e) {
                                    String message = "Could not generate access URL";
                                    log.error(message, e);
                                    throw new ApplicationClusterRegistrationException(message, e);
                                }
                            }
					        accessUrls.put(dependencyClusterIDs[i], accessUrlPerCluster);
				        }
			        }
		        }
	        }

            for (ApplicationClusterContext appClusterCtxt : appClustersContexts) {
                ClusterContext clusterContext = new ClusterContext(
                        appId, appClusterCtxt.getCartridgeType(), appClusterCtxt.getClusterId(),
                        appClusterCtxt.getTextPayload(), appClusterCtxt.getHostName(),
                        appClusterCtxt.isLbCluster(), appClusterCtxt.getProperties());

                CloudControllerContext.getInstance().addClusterContext(clusterContext);

	            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(clusterContext.getCartridgeType());

                // Create cluster object
                Cluster cluster = new Cluster(appClusterCtxt.getCartridgeType(), appClusterCtxt.getClusterId(),
                        appClusterCtxt.getDeploymentPolicyName(), appClusterCtxt.getAutoscalePolicyName(), appId);
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
            TopologyBuilder.handleApplicationClustersCreated(appId, clusters);
            CloudControllerContext.getInstance().persist();
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    public void createClusterInstance(String serviceType, String clusterId,
                                      String alias, String instanceId, String partitionId,
                                      String networkPartitionId) throws ClusterInstanceCreationException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireClusterContextWriteLock();
            TopologyBuilder.handleClusterInstanceCreated(serviceType, clusterId, alias,
                    instanceId, partitionId, networkPartitionId);

            CloudControllerContext.getInstance().persist();
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public KubernetesCluster[] getKubernetesClusters() {
        return CloudControllerContext.getInstance().getKubernetesClusters();
    }

    @Override
    public KubernetesCluster getKubernetesCluster(String kubernetesClusterId) throws NonExistingKubernetesClusterException {
        return CloudControllerContext.getInstance().getKubernetesCluster(kubernetesClusterId);
    }

    @Override
    public KubernetesMaster getMasterForKubernetesCluster(String kubernetesClusterId) throws NonExistingKubernetesClusterException {
        return CloudControllerContext.getInstance().getKubernetesMasterInGroup(kubernetesClusterId);
    }

    @Override
    public KubernetesHost[] getHostsForKubernetesCluster(String kubernetesClusterId) throws NonExistingKubernetesClusterException {
        return CloudControllerContext.getInstance().getKubernetesHostsInGroup(kubernetesClusterId);
    }


    @Override
    public boolean addKubernetesCluster(KubernetesCluster kubernetesCluster) throws InvalidKubernetesClusterException {
        if (kubernetesCluster == null) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster can not be null");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info(String.format("Adding kubernetes cluster: [kubernetes-cluster-id] %s",
                        kubernetesCluster.getClusterId()));
            }
            CloudControllerUtil.validateKubernetesCluster(kubernetesCluster);

            // Add to information model
            CloudControllerContext.getInstance().addKubernetesCluster(kubernetesCluster);
            CloudControllerContext.getInstance().persist();

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes cluster added successfully: [kubernetes-cluster-id] %s",
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
    public boolean addKubernetesHost(String kubernetesClusterId, KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesClusterException {
        if (kubernetesHost == null) {
            throw new InvalidKubernetesHostException("Kubernetes host can not be null");
        }
        if (StringUtils.isEmpty(kubernetesClusterId)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id can not be null");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info(String.format("Adding kubernetes host for kubernetes cluster: [kubernetes-cluster-id] %s " +
                        "[hostname] %s", kubernetesClusterId, kubernetesHost.getHostname()));
            }
            CloudControllerUtil.validateKubernetesHost(kubernetesHost);

            KubernetesCluster kubernetesCluster = getKubernetesCluster(kubernetesClusterId);
            ArrayList<KubernetesHost> kubernetesHostArrayList;

            if (kubernetesCluster.getKubernetesHosts() == null) {
                kubernetesHostArrayList = new ArrayList<KubernetesHost>();
            } else {
                if (CloudControllerContext.getInstance().kubernetesHostExists(kubernetesHost.getHostId())) {
                    throw new InvalidKubernetesHostException("Kubernetes host already exists: [hostnae] " +
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
                log.info(String.format("Kubernetes host added successfully: [id] %s", kubernetesCluster.getClusterId()));
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
    public boolean removeKubernetesCluster(String kubernetesClusterId) throws NonExistingKubernetesClusterException {
        if (StringUtils.isEmpty(kubernetesClusterId)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id can not be empty");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info("Removing Kubernetes cluster: " + kubernetesClusterId);
            }
            try {
                // Remove entry from information model
                CloudControllerContext.getInstance().removeKubernetesCluster(kubernetesClusterId);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Kubernetes cluster removed successfully: [id] %s", kubernetesClusterId));
                }

                CloudControllerContext.getInstance().persist();

                return true;
            } catch (Exception e) {
                throw new NonExistingKubernetesClusterException(e.getMessage(), e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    @Override
    public boolean removeKubernetesHost(String kubernetesHostId) throws NonExistingKubernetesHostException {
        if (kubernetesHostId == null) {
            throw new NonExistingKubernetesHostException("Kubernetes host id can not be null");
        }

        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireKubernetesClusterWriteLock();

            if (log.isInfoEnabled()) {
                log.info("Removing Kubernetes Host: " + kubernetesHostId);
            }
            try {
                KubernetesCluster kubernetesClusterStored = CloudControllerContext.getInstance().getKubernetesClusterContainingHost(kubernetesHostId);

                // Kubernetes master can not be removed
                if (kubernetesClusterStored.getKubernetesMaster().getHostId().equals(kubernetesHostId)) {
                    throw new NonExistingKubernetesHostException("Kubernetes master is not allowed to be removed [id] " + kubernetesHostId);
                }

                List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
                for (KubernetesHost kubernetesHost : kubernetesClusterStored.getKubernetesHosts()) {
                    if (!kubernetesHost.getHostId().equals(kubernetesHostId)) {
                        kubernetesHostList.add(kubernetesHost);
                    }
                }
                // member count will be equal only when host object was not found
                if (kubernetesHostList.size() == kubernetesClusterStored.getKubernetesHosts().length) {
                    throw new NonExistingKubernetesHostException("Kubernetes host not found for [id] " + kubernetesHostId);
                }
                KubernetesHost[] kubernetesHostsArray = new KubernetesHost[kubernetesHostList.size()];
                kubernetesHostList.toArray(kubernetesHostsArray);

                // Update information model
                kubernetesClusterStored.setKubernetesHosts(kubernetesHostsArray);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Kubernetes host removed successfully: [id] %s", kubernetesHostId));
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
	public void addDeployementPolicy(DeploymentPolicy deploymentPolicy)
			throws DeploymentPolicyAlreadyExistsException, InvalidDeploymentPolicyException {

		CloudControllerServiceUtil.validateDeploymentPolicy(deploymentPolicy);

		if (log.isInfoEnabled()) {
			log.info("Adding deployment policy: [deployment-policy-id] " + deploymentPolicy.getDeploymentPolicyID());
		}
		if (log.isDebugEnabled()) {
			log.debug("Deployment policy definition: " + deploymentPolicy.toString());
		}

		String deploymentPolicyID = deploymentPolicy.getDeploymentPolicyID();
		if (cloudControllerContext.getDeploymentPolicy(deploymentPolicyID) != null) {
			String message = "Deployment policy already exists: [deployment-policy-id] " + deploymentPolicyID;
			log.error(message);
			throw new DeploymentPolicyAlreadyExistsException(message);
		}

		// Add cartridge to the cloud controller context and persist
		CloudControllerContext.getInstance().addDeploymentPolicy(deploymentPolicy);
		CloudControllerContext.getInstance().persist();

		if (log.isInfoEnabled()) {
			log.info("Successfully added deployment policy: [deployment-policy-id] " + deploymentPolicyID);
		}

	}

	@Override
	public void updateDeployementPolicy(DeploymentPolicy deploymentPolicy)
			throws DeploymentPolicyNotExistsException, InvalidDeploymentPolicyException {
		
		CloudControllerServiceUtil.validateDeploymentPolicy(deploymentPolicy);

		if (log.isInfoEnabled()) {
			log.info("Updating deployment policy: [deployment-policy-id] " + deploymentPolicy.getDeploymentPolicyID());
		}
		if (log.isDebugEnabled()) {
			log.debug("Updating Deployment policy definition: " + deploymentPolicy.toString());
		}

		String deploymentPolicyID = deploymentPolicy.getDeploymentPolicyID();
		if (cloudControllerContext.getDeploymentPolicy(deploymentPolicyID) == null) {
			String message = "Deployment policy not exists: [deployment-policy-id] " + deploymentPolicyID;
			log.error(message);
			throw new DeploymentPolicyNotExistsException(message);
		}

		// Add cartridge to the cloud controller context and persist
		CloudControllerContext.getInstance().addDeploymentPolicy(deploymentPolicy);
		CloudControllerContext.getInstance().persist();

		if (log.isInfoEnabled()) {
			log.info("Successfully updated deployment policy: [deployment-policy-id] " + deploymentPolicyID);
		}
	}

	@Override
	public void removeDeployementPolicy(String deploymentPolicyID) throws DeploymentPolicyNotExistsException {
		if (log.isInfoEnabled()) {
			log.info("Removing deployment policy: [deployment-policy_id] " + deploymentPolicyID);
		}
		if (cloudControllerContext.getDeploymentPolicy(deploymentPolicyID) == null) {
			String message = "Deployment policy not exists: [deployment-policy-id] " + deploymentPolicyID;
			log.error(message);
			throw new DeploymentPolicyNotExistsException(message);
		}
		CloudControllerContext.getInstance().removeDeploymentPolicy(deploymentPolicyID);
		if (log.isInfoEnabled()) {
			log.info("Successfully removed deployment policy: [deployment_policy_id] " + deploymentPolicyID);
		}

	}

	@Override
	public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyID)
			throws DeploymentPolicyNotExistsException {
		if (log.isInfoEnabled()) {
			log.info("Getting deployment policy: [deployment-policy_id] " + deploymentPolicyID);
		}
		if (cloudControllerContext.getDeploymentPolicy(deploymentPolicyID) == null) {
			String message = "Deployment policy not exists: [deployment-policy-id] " + deploymentPolicyID;
			log.error(message);
			throw new DeploymentPolicyNotExistsException(message);
		}
		DeploymentPolicy deploymentPolicy =
				CloudControllerContext.getInstance().getDeploymentPolicy(deploymentPolicyID);
		return deploymentPolicy;
	}
	
    @Override
    public DeploymentPolicy[] getDeploymentPolicies() {
        try {
            Collection<DeploymentPolicy> deploymentPolicies = cloudControllerContext.getDeploymentPolicies();
            return deploymentPolicies.toArray(new DeploymentPolicy[deploymentPolicies.size()]);
        } catch (Exception e) {
            String message = "Could not get deployment policies";
            log.error(message);
            throw new CloudControllerException(message, e);
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
                log.info("Updating Kubernetes Host: " + kubernetesHost);
            }

            try {
                KubernetesCluster kubernetesClusterStored = CloudControllerContext.getInstance().getKubernetesClusterContainingHost(kubernetesHost.getHostId());
                for (int i = 0; i < kubernetesClusterStored.getKubernetesHosts().length; i++) {
                    if (kubernetesClusterStored.getKubernetesHosts()[i].getHostId().equals(kubernetesHost.getHostId())) {
                        // Update the information model
                        kubernetesClusterStored.getKubernetesHosts()[i] = kubernetesHost;

                        if (log.isInfoEnabled()) {
                            log.info(String.format("Kubernetes host updated successfully: [id] %s", kubernetesHost.getHostId()));
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
        throw new NonExistingKubernetesHostException("Kubernetes host not found [id] " + kubernetesHost.getHostId());
    }
	
    @Override
    public void addNetworkPartition(NetworkPartition networkPartition) throws NetworkPartitionAlreadyExistsException {
    	
        try {
        	handleNullObject(networkPartition, "Network Partition is null");
        	handleNullObject(networkPartition.getId(), "Network Partition ID is null");

        	if(log.isInfoEnabled()) {
                log.info(String.format("Adding network partition: [network-partition-id] %s", networkPartition.getId()));
            }
            
            String networkPartitionID = networkPartition.getId();
            if (cloudControllerContext.getNetworkPartition(networkPartitionID) != null) {
            	String message = "Network partition already exists: [network-partition-id] " + networkPartitionID;
            	log.error(message);
            	throw new NetworkPartitionAlreadyExistsException(message);
            }
            
            if(networkPartition.getPartitions() != null) {
                for(Partition partition : networkPartition.getPartitions()) {
                    if(partition != null) {
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Validating partition: [network-partition-id] %s [partition-id] %s",
                                    networkPartition.getId(), partition.getId()));
                        }
                        validatePartition(partition);
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Partition validated successfully: [network-partition-id] %s " +
                                            "[partition-id] %s", networkPartition.getId(), partition.getId()));
                        }
                    }
                }
            }
            
            // overwrites partitions' kubernetes cluster ids with network partition's kubernetes cluster id
            CloudControllerServiceUtil.overwritesPartitionsKubernetesClusterIdsWithNetworkPartitionKubernetesClusterId(networkPartition);
            
            // adding network partition to CC-Context
            CloudControllerContext.getInstance().addNetworkPartition(networkPartition);
            // persisting CC-Context
            CloudControllerContext.getInstance().persist();
            if(log.isInfoEnabled()) {
                log.info(String.format("Network partition added successfully: [network-partition-id] %s",
                        networkPartition.getId()));
            }
        } catch (Exception e) {
            String message = "Could not add network partition";
            log.error(message);
            throw new CloudControllerException(message, e);
        }
    }

    @Override
    public void removeNetworkPartition(String networkPartitionId) throws NetworkPartitionNotExistsException{
    	
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Removing network partition: [network-partition-id] %s", networkPartitionId));
            }
            handleNullObject(networkPartitionId, "Network Partition ID is null");
            
            if (cloudControllerContext.getNetworkPartition(networkPartitionId) == null) {
            	String message = "Network partition not exists: [network-partiton-id] " + networkPartitionId;
    			log.error(message);
    			throw new NetworkPartitionNotExistsException(message);
			}
            // removing from CC-Context
            CloudControllerContext.getInstance().removeNetworkPartition(networkPartitionId);
            // persisting CC-Context
            CloudControllerContext.getInstance().persist();
            if(log.isInfoEnabled()) {
                log.info(String.format("Network partition removed successfully: [network-partition-id] %s",
                        networkPartitionId));
            }
        } catch (Exception e) {
            String message = "Could not remove network partition";
            log.error(message);
            throw new CloudControllerException(message, e);
        }
    }

    @Override
    public void updateNetworkPartition(NetworkPartition networkPartition) throws NetworkPartitionNotExistsException{
        try {
        	handleNullObject(networkPartition, "Network Partition is null");
        	handleNullObject(networkPartition.getId(), "Network Partition ID is null");

        	if(log.isInfoEnabled()) {
                log.info(String.format("Updating network partition: [network-partition-id] %s", networkPartition.getId()));
            }
            
            String networkPartitionID = networkPartition.getId();
            if (cloudControllerContext.getNetworkPartition(networkPartitionID) == null) {
            	String message = "Network partition not exists: [network-partition-id] " + networkPartitionID;
            	log.error(message);
            	throw new NetworkPartitionNotExistsException(message);
            }
            
            if(networkPartition.getPartitions() != null) {
                for(Partition partition : networkPartition.getPartitions()) {
                    if(partition != null) {
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Validating partition: [network-partition-id] %s [partition-id] %s",
                                    networkPartition.getId(), partition.getId()));
                        }
                        validatePartition(partition);
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Partition validated successfully: [network-partition-id] %s " +
                                            "[partition-id] %s", networkPartition.getId(), partition.getId()));
                        }
                    }
                }
            }
            
            // overriding network partition to CC-Context
            CloudControllerContext.getInstance().addNetworkPartition(networkPartition);
            // persisting CC-Context
            CloudControllerContext.getInstance().persist();
            if(log.isInfoEnabled()) {
                log.info(String.format("Network partition updated successfully: [network-partition-id] %s",
                        networkPartition.getId()));
            }
        } catch (Exception e) {
            String message = String.format("Could not update network partition: [network-partition-id] %s",
                    networkPartition.getId());
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
    public NetworkPartition getNetworkPartition(String networkPartitionId) {
        try {
            return CloudControllerContext.getInstance().getNetworkPartition(networkPartitionId);
        } catch (Exception e) {
            String message = String.format("Could not get network partition: [network-partition-id] %s",
                    networkPartitionId);
            log.error(message);
            throw new CloudControllerException(message, e);
        }
    }
}

