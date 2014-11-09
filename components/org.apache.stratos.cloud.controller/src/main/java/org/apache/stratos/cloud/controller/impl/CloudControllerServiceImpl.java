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
package org.apache.stratos.cloud.controller.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.InetAddresses;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.concurrent.PartitionValidatorCallable;
import org.apache.stratos.cloud.controller.concurrent.ScheduledThreadExecutor;
import org.apache.stratos.cloud.controller.concurrent.ThreadExecutor;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.functions.ContainerClusterContextToKubernetesService;
import org.apache.stratos.cloud.controller.functions.ContainerClusterContextToReplicationController;
import org.apache.stratos.cloud.controller.functions.PodToMemberContext;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.persist.Deserializer;
import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.cloud.controller.publisher.CartridgeInstanceDataPublisher;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.topology.TopologyManager;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.PodActivationWatcher;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.common.*;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.Label;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.ReplicationController;
import org.apache.stratos.kubernetes.client.model.Service;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.util.Constants;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.Template;
import org.jclouds.rest.ResourceNotFoundException;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.*;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Cloud Controller Service is responsible for starting up new server instances,
 * terminating already started instances, providing pending instance count etc.
 */
public class CloudControllerServiceImpl implements CloudControllerService {

    private static final Log LOG = LogFactory
            .getLog(CloudControllerServiceImpl.class);
    private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder
            .getInstance();

    public CloudControllerServiceImpl() {
        // acquire serialized data from registry
        acquireData();
    }

    private void acquireData() {

        Object obj = RegistryManager.getInstance().retrieve();
        if (obj != null) {
            try {
                Object dataObj = Deserializer
                        .deserializeFromByteArray((byte[]) obj);
                if (dataObj instanceof FasterLookUpDataHolder) {
                    FasterLookUpDataHolder serializedObj = (FasterLookUpDataHolder) dataObj;
                    FasterLookUpDataHolder currentData = FasterLookUpDataHolder
                            .getInstance();

                    // assign necessary data
                    currentData.setClusterIdToContext(serializedObj.getClusterIdToContext());
                    currentData.setMemberIdToContext(serializedObj.getMemberIdToContext());
                    currentData.setClusterIdToMemberContext(serializedObj.getClusterIdToMemberContext());
                    currentData.setCartridges(serializedObj.getCartridges());
                    currentData.setKubClusterIdToKubClusterContext(serializedObj.getKubClusterIdToKubClusterContext());
                    currentData.setServiceGroups(serializedObj.getServiceGroups());

                    if (LOG.isDebugEnabled()) {

                        LOG.debug("Cloud Controller Data is retrieved from registry.");
                    }
                } else {
                    if (LOG.isDebugEnabled()) {

                        LOG.debug("Cloud Controller Data cannot be found in registry.");
                    }
                }
            } catch (Exception e) {

                String msg = "Unable to acquire data from Registry. Hence, any historical data will not get reflected.";
                LOG.warn(msg, e);
            }

        }
    }

    public void deployCartridgeDefinition(CartridgeConfig cartridgeConfig) throws InvalidCartridgeDefinitionException,
            InvalidIaasProviderException {

        handleNullObject(cartridgeConfig, "Invalid Cartridge Definition: Definition is null.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Cartridge definition: " + cartridgeConfig.toString());
        }

        Cartridge cartridge = null;
        try {
            // cartridge can never be null
            cartridge = CloudControllerUtil.toCartridge(cartridgeConfig);
        } catch (Exception e) {
            String msg =
                    "Invalid Cartridge Definition: Cartridge Type: " +
                            cartridgeConfig.getType() +
                            ". Cause: Cannot instantiate a Cartridge Instance with the given Config. " + e.getMessage();
            LOG.error(msg, e);
            throw new InvalidCartridgeDefinitionException(msg, e);
        }

        List<IaasProvider> iaases = cartridge.getIaases();

        if (!StratosConstants.KUBERNETES_DEPLOYER_TYPE.equals(cartridge.getDeployerType())) {
            if (iaases == null || iaases.isEmpty()) {
                String msg = "Invalid Cartridge Definition: Cartridge Type: "
                        + cartridgeConfig.getType()
                        + ". Cause: Iaases of this Cartridge is null or empty.";
                LOG.error(msg);
                throw new InvalidCartridgeDefinitionException(msg);
            }

            if (iaases == null || iaases.isEmpty()) {
                String msg = "Invalid Cartridge Definition: Cartridge Type: " +
                        cartridgeConfig.getType() +
                        ". Cause: Iaases of this Cartridge is null or empty.";
                LOG.error(msg);
                throw new InvalidCartridgeDefinitionException(msg);
            }

            for (IaasProvider iaasProvider : iaases) {
                CloudControllerUtil.getIaas(iaasProvider);
            }
        }

        // TODO transaction begins
        String cartridgeType = cartridge.getType();
        if (dataHolder.getCartridge(cartridgeType) != null) {
            Cartridge cartridgeToBeRemoved = dataHolder.getCartridge(cartridgeType);
            // undeploy
            try {
                undeployCartridgeDefinition(cartridgeToBeRemoved.getType());
            } catch (InvalidCartridgeTypeException e) {
                //ignore
            }
            populateNewCartridge(cartridge, cartridgeToBeRemoved);
        }

        dataHolder.addCartridge(cartridge);

        // persist
        persist();

        List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
        cartridgeList.add(cartridge);

        TopologyBuilder.handleServiceCreated(cartridgeList);
        // transaction ends

        LOG.info("Successfully deployed the Cartridge definition: " + cartridgeType);
    }

    private void populateNewCartridge(Cartridge cartridge,
                                      Cartridge cartridgeToBeRemoved) {

        List<IaasProvider> newIaasProviders = cartridge.getIaases();
        Map<String, IaasProvider> oldPartitionToIaasMap = cartridgeToBeRemoved.getPartitionToIaasProvider();

        for (Entry<String, IaasProvider> entry : oldPartitionToIaasMap.entrySet()) {
            if (entry == null) {
                continue;
            }
            String partitionId = entry.getKey();
            IaasProvider oldIaasProvider = entry.getValue();
            if (newIaasProviders.contains(oldIaasProvider)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Copying a partition from the Cartridge that is undeployed, to the new Cartridge. "
                            + "[partition id] : " + partitionId + " [cartridge type] " + cartridge.getType());
                }
                cartridge.addIaasProvider(partitionId, newIaasProviders.get(newIaasProviders.indexOf(oldIaasProvider)));
            }
        }

    }

    public void undeployCartridgeDefinition(String cartridgeType) throws InvalidCartridgeTypeException {

        Cartridge cartridge = null;
        if ((cartridge = dataHolder.getCartridge(cartridgeType)) != null) {
            if (dataHolder.getCartridges().remove(cartridge)) {
                // invalidate partition validation cache
                dataHolder.removeFromCartridgeTypeToPartitionIds(cartridgeType);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Partition cache invalidated for cartridge " + cartridgeType);
                }

                persist();

                // sends the service removed event
                List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
                cartridgeList.add(cartridge);
                TopologyBuilder.handleServiceRemoved(cartridgeList);

                if (LOG.isInfoEnabled()) {
                    LOG.info("Successfully undeployed the Cartridge definition: " + cartridgeType);
                }
                return;
            }
        }
        String msg = "Cartridge [type] " + cartridgeType + " is not a deployed Cartridge type.";
        LOG.error(msg);
        throw new InvalidCartridgeTypeException(msg);
    }

    public void deployServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException {

        if (servicegroup == null) {
            String msg = "Invalid ServiceGroup Definition: Definition is null.";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);

        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:deployServiceGroup:" + servicegroup.getName());
        }

        String[] subGroups = servicegroup.getCartridges();


        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:deployServiceGroup:subGroups" + subGroups);
            if (subGroups != null) {
                LOG.debug("CloudControllerServiceImpl:deployServiceGroup:subGroups:size" + subGroups.length);
            } else {
                LOG.debug("CloudControllerServiceImpl:deployServiceGroup:subGroups: is null");
            }
        }


        Dependencies dependencies = servicegroup.getDependencies();

        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:deployServiceGroup:dependencies" + dependencies);
        }

        if (dependencies != null) {
            String[] startupOrders = dependencies.getStartupOrders();

            if (LOG.isDebugEnabled()) {
                LOG.debug("CloudControllerServiceImpl:deployServiceGroup:startupOrders" + startupOrders);

                if (startupOrders != null) {
                    LOG.debug("CloudControllerServiceImpl:deployServiceGroup:startupOrder:size" + startupOrders.length);
                } else {
                    LOG.debug("CloudControllerServiceImpl:deployServiceGroup:startupOrder: is null");
                }
            }
        }

        dataHolder.addServiceGroup(servicegroup);

        this.persist();

    }

    public void undeployServiceGroup(String name) throws InvalidServiceGroupException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:undeployServiceGroup: " + name);
        }

        ServiceGroup serviceGroup = null;

        serviceGroup = dataHolder.getServiceGroup(name);

        if (serviceGroup != null) {
            if (dataHolder.getServiceGroups().remove(serviceGroup)) {
                persist();
                if (LOG.isInfoEnabled()) {
                    LOG.info("Successfully undeployed the Service Group definition: " + serviceGroup);
                }
                return;
            }
        }

        String msg = "ServiceGroup " + name + " is not a deployed Service Group definition";
        LOG.error(msg);
        throw new InvalidServiceGroupException(msg);

    }

    @Override
    public ServiceGroup getServiceGroup(String name) throws InvalidServiceGroupException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("getServiceGroupDefinition:" + name);
        }

        ServiceGroup serviceGroup = this.dataHolder.getServiceGroup(name);

        if (serviceGroup == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getServiceGroupDefinition: no entry found for service group " + name);
            }
            String msg = "ServiceGroup " + name + " is not a deployed Service Group definition";
            throw new InvalidServiceGroupException(msg);
        }

        return serviceGroup;
    }

    public String[] getServiceGroupSubGroups(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid ServiceGroup " + serviceGroup);
        }

        return serviceGroup.getSubGroups();
    }

    /**
     *
     */
    public String[] getServiceGroupCartridges(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid ServiceGroup " + serviceGroup);
        }
        String[] cs = serviceGroup.getCartridges();
        return cs;

    }

    public Dependencies getServiceGroupDependencies(String name) throws InvalidServiceGroupException {
        ServiceGroup serviceGroup = this.getServiceGroup(name);
        if (serviceGroup == null) {
            throw new InvalidServiceGroupException("Invalid ServiceGroup " + serviceGroup);
        }
        return serviceGroup.getDependencies();
    }

    @Override
    public MemberContext startInstance(MemberContext memberContext) throws
            UnregisteredCartridgeException, InvalidIaasProviderException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:startInstance");
        }

        handleNullObject(memberContext, "Instance start-up failed. Member is null.");

        String clusterId = memberContext.getClusterId();
        Partition partition = memberContext.getPartition();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Received an instance spawn request : " + memberContext);
        }

        Template template = null;

        handleNullObject(partition, "Instance start-up failed. Specified Partition is null. " +
                memberContext);

        String partitionId = partition.getId();
        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);

        handleNullObject(ctxt, "Instance start-up failed. Invalid cluster id. " + memberContext);

        String cartridgeType = ctxt.getCartridgeType();

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg =
                    "Instance start-up failed. No matching Cartridge found [type] " + cartridgeType + ". " +
                            memberContext.toString();
            LOG.error(msg);
            throw new UnregisteredCartridgeException(msg);
        }

        memberContext.setCartridgeType(cartridgeType);


        IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);
        if (iaasProvider == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("IaasToPartitionMap " + cartridge.hashCode()
                        + " for cartridge " + cartridgeType + " and for partition: " + partitionId);
            }
            String msg = "Instance start-up failed. "
                    + "There's no IaaS provided for the partition: "
                    + partitionId
                    + " and for the Cartridge type: "
                    + cartridgeType
                    + ". Only following "
                    + "partitions can be found in this Cartridge: "
                    + cartridge.getPartitionToIaasProvider().keySet()
                    .toString() + ". " + memberContext.toString()
                    + ". ";
            LOG.fatal(msg);
            throw new InvalidIaasProviderException(msg);
        }
        String type = iaasProvider.getType();
        try {
            // generating the Unique member ID...
            String memberID = generateMemberId(clusterId);
            memberContext.setMemberId(memberID);
            // have to add memberID to the payload
            StringBuilder payload = new StringBuilder(ctxt.getPayload());
            addToPayload(payload, "MEMBER_ID", memberID);
            addToPayload(payload, "LB_CLUSTER_ID", memberContext.getLbClusterId());
            addToPayload(payload, "NETWORK_PARTITION_ID", memberContext.getNetworkPartitionId());
            addToPayload(payload, "PARTITION_ID", partitionId);
            if (memberContext.getProperties() != null) {
                org.apache.stratos.common.Properties properties = memberContext.getProperties();
                if (properties != null) {
                    for (Property prop : properties.getProperties()) {
                        addToPayload(payload, prop.getName(), prop.getValue());
                    }
                }
            }

            Iaas iaas = iaasProvider.getIaas();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Payload: " + payload.toString());
            }

            if (iaas == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Iaas is null of Iaas Provider: " + type + ". Trying to build IaaS...");
                }
                try {
                    iaas = CloudControllerUtil.getIaas(iaasProvider);
                } catch (InvalidIaasProviderException e) {
                    String msg = "Instance start up failed. " + memberContext.toString() +
                            "Unable to build Iaas of this IaasProvider [Provider] : " + type + ". Cause: " + e.getMessage();
                    LOG.error(msg, e);
                    throw new InvalidIaasProviderException(msg, e);
                }

            }

            if (ctxt.isVolumeRequired()) {
                if (ctxt.getVolumes() != null) {
                    for (Volume volume : ctxt.getVolumes()) {

                        if (volume.getId() == null) {
                            // create a new volume
                            createVolumeAndSetInClusterContext(volume, iaasProvider);
                        }
                    }
                }
            }

            if (ctxt.isVolumeRequired()) {
                addToPayload(payload, "PERSISTENCE_MAPPING", getPersistencePayload(ctxt, iaas).toString());
            }
            iaasProvider.setPayload(payload.toString().getBytes());
            iaas.setDynamicPayload();

            template = iaasProvider.getTemplate();

            if (template == null) {
                String msg =
                        "Failed to start an instance. " +
                                memberContext.toString() +
                                ". Reason : Jclouds Template is null for iaas provider [type]: " + iaasProvider.getType();
                LOG.error(msg);
                throw new InvalidIaasProviderException(msg);
            }

            //Start instance start up in a new thread
            ThreadExecutor exec = ThreadExecutor.getInstance();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller is starting the instance start up thread.");
            }
            exec.execute(new JcloudsInstanceCreator(memberContext, iaasProvider, cartridgeType));

            LOG.info("Instance is successfully starting up. " + memberContext.toString());

            return memberContext;

        } catch (Exception e) {
            String msg = "Failed to start an instance. " + memberContext.toString() + " Cause: " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }

    }

    private void createVolumeAndSetInClusterContext(Volume volume,
                                                    IaasProvider iaasProvider) {
        // iaas cannot be null at this state #startInstance method
        Iaas iaas = iaasProvider.getIaas();
        int sizeGB = volume.getSize();
        String snapshotId = volume.getSnapshotId();
        if (StringUtils.isNotEmpty(volume.getVolumeId())) {
            // volumeID is specified, so not creating additional volumes
            if (LOG.isDebugEnabled()) {
                LOG.debug("Volume creation is skipping since a volume ID is specified. [Volume ID]" + volume.getVolumeId());
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Adding persistence mapping " + volume.toString());
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Persistence payload is" + persistencePayload.toString());
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

    /**
     * Persist data in registry.
     */
    private void persist() {
        try {
            RegistryManager.getInstance().persist(
                    dataHolder);
        } catch (RegistryException e) {

            String msg = "Failed to persist the Cloud Controller data in registry. Further, transaction roll back also failed.";
            LOG.fatal(msg);
            throw new CloudControllerException(msg, e);
        }
    }

    private String generateMemberId(String clusterId) {
        UUID memberId = UUID.randomUUID();
        return clusterId + memberId.toString();
    }

    @Override
    public void terminateInstance(String memberId) throws InvalidMemberException, InvalidCartridgeTypeException {

        handleNullObject(memberId, "Termination failed. Null member id.");

        MemberContext ctxt = dataHolder.getMemberContextOfMemberId(memberId);

        if (ctxt == null) {
            String msg = "Termination failed. Invalid Member Id: " + memberId;
            LOG.error(msg);
            throw new InvalidMemberException(msg);
        }

        ThreadExecutor exec = ThreadExecutor.getInstance();
        exec.execute(new InstanceTerminator(ctxt));

    }

    private class InstanceTerminator implements Runnable {

        private MemberContext ctxt;

        public InstanceTerminator(MemberContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {

            String memberId = ctxt.getMemberId();
            String clusterId = ctxt.getClusterId();
            String partitionId = ctxt.getPartition().getId();
            String cartridgeType = ctxt.getCartridgeType();
            String nodeId = ctxt.getNodeId();

            try {
                // these will never be null, since we do not add null values for these.
                Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

                LOG.info("Starting to terminate an instance with member id : " + memberId +
                        " in partition id: " + partitionId + " of cluster id: " + clusterId +
                        " and of cartridge type: " + cartridgeType);

                if (cartridge == null) {
                    String msg =
                            "Termination of Member Id: " + memberId + " failed. " +
                                    "Cannot find a matching Cartridge for type: " +
                                    cartridgeType;
                    LOG.error(msg);
                    throw new InvalidCartridgeTypeException(msg);
                }

                // if no matching node id can be found.
                if (nodeId == null) {

                    String msg =
                            "Termination failed. Cannot find a node id for Member Id: " +
                                    memberId;

                    // log information
                    logTermination(ctxt);
                    LOG.error(msg);
                    throw new InvalidMemberException(msg);
                }

                IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);

                // terminate it!
                terminate(iaasProvider, nodeId, ctxt);

                // log information
                logTermination(ctxt);

            } catch (Exception e) {
                String msg =
                        "Instance termination failed. " + ctxt.toString();
                LOG.error(msg, e);
                throw new CloudControllerException(msg, e);
            }

        }
    }

    private class JcloudsInstanceCreator implements Runnable {

        private MemberContext memberContext;
        private IaasProvider iaasProvider;
        private String cartridgeType;

        public JcloudsInstanceCreator(MemberContext memberContext, IaasProvider iaasProvider,
                                      String cartridgeType) {
            this.memberContext = memberContext;
            this.iaasProvider = iaasProvider;
            this.cartridgeType = cartridgeType;
        }

        @Override
        public void run() {


            String clusterId = memberContext.getClusterId();
            Partition partition = memberContext.getPartition();
            ClusterContext ctxt = dataHolder.getClusterContext(clusterId);
            Iaas iaas = iaasProvider.getIaas();
            String publicIp = null;

            NodeMetadata node = null;
            // generate the group id from domain name and sub domain name.
            // Should have lower-case ASCII letters, numbers, or dashes.
            // Should have a length between 3-15
            String str = clusterId.length() > 10 ? clusterId.substring(0, 10) : clusterId.substring(0, clusterId.length());
            String group = str.replaceAll("[^a-z0-9-]", "");

            try {
                ComputeService computeService = iaasProvider
                        .getComputeService();
                Template template = iaasProvider.getTemplate();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cloud Controller is delegating request to start an instance for "
                            + memberContext + " to Jclouds layer.");
                }
                // create and start a node
                Set<? extends NodeMetadata> nodes = computeService
                        .createNodesInGroup(group, 1, template);
                node = nodes.iterator().next();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cloud Controller received a response for the request to start "
                            + memberContext + " from Jclouds layer.");
                }

                if (node == null) {
                    String msg = "Null response received for instance start-up request to Jclouds.\n"
                            + memberContext.toString();
                    LOG.error(msg);
                    throw new IllegalStateException(msg);
                }

                // node id
                String nodeId = node.getId();
                if (nodeId == null) {
                    String msg = "Node id of the starting instance is null.\n"
                            + memberContext.toString();
                    LOG.fatal(msg);
                    throw new IllegalStateException(msg);
                }

                memberContext.setNodeId(nodeId);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Node id was set. " + memberContext.toString());
                }

                // attach volumes
                if (ctxt.isVolumeRequired()) {
                    // remove region prefix
                    String instanceId = nodeId.indexOf('/') != -1 ? nodeId
                            .substring(nodeId.indexOf('/') + 1, nodeId.length())
                            : nodeId;
                    memberContext.setInstanceId(instanceId);
                    if (ctxt.getVolumes() != null) {
                        for (Volume volume : ctxt.getVolumes()) {
                            try {
                                iaas.attachVolume(instanceId, volume.getId(),
                                        volume.getDevice());
                            } catch (Exception e) {
                                // continue without throwing an exception, since
                                // there is an instance already running
                                LOG.error("Attaching Volume to Instance [ "
                                        + instanceId + " ] failed!", e);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                String msg = "Failed to start an instance. " + memberContext.toString() + " Cause: " + e.getMessage();
                LOG.error(msg, e);
                throw new IllegalStateException(msg, e);
            }

            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IP allocation process started for " + memberContext);
                }
                String autoAssignIpProp =
                        iaasProvider.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP_PROPERTY);

                String pre_defined_ip =
                        iaasProvider.getProperty(CloudControllerConstants.FLOATING_IP_PROPERTY);

                // reset ip
                String ip = "";

                // default behavior is autoIpAssign=false
                if (autoAssignIpProp == null ||
                        (autoAssignIpProp != null && autoAssignIpProp.equals("false"))) {

                    // check if floating ip is well defined in cartridge definition
                    if (pre_defined_ip != null) {
                        if (isValidIpAddress(pre_defined_ip)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("CloudControllerServiceImpl:IpAllocator:pre_defined_ip: invoking associatePredefinedAddress" + pre_defined_ip);
                            }
                            ip = iaas.associatePredefinedAddress(node, pre_defined_ip);

                            if (ip == null || "".equals(ip) || !pre_defined_ip.equals(ip)) {
                                // throw exception and stop instance creation
                                String msg = "Error occurred while allocating predefined floating ip address: " + pre_defined_ip +
                                        " / allocated ip:" + ip +
                                        " - terminating node:" + memberContext.toString();
                                LOG.error(msg);
                                // terminate instance
                                terminate(iaasProvider,
                                        node.getId(), memberContext);
                                throw new CloudControllerException(msg);
                            }
                        } else {
                            String msg = "Invalid floating ip address configured: " + pre_defined_ip +
                                    " - terminating node:" + memberContext.toString();
                            LOG.error(msg);
                            // terminate instance
                            terminate(iaasProvider,
                                    node.getId(), memberContext);
                            throw new CloudControllerException(msg);
                        }

                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("CloudControllerServiceImpl:IpAllocator:no (valid) predefined floating ip configured, "
                                    + "selecting available one from pool");
                        }
                        // allocate an IP address - manual IP assigning mode
                        ip = iaas.associateAddress(node);

                        if (ip != null) {
                            memberContext.setAllocatedIpAddress(ip);
                            LOG.info("Allocated an ip address: "
                                    + memberContext.toString());
                        }
                    }

                    if (ip == null) {
                        String msg = "No IP address found. IP allocation failed for " + memberContext;
                        LOG.error(msg);
                        throw new CloudControllerException(msg);
                    }

                    // build the node with the new ip
                    node = NodeMetadataBuilder.fromNodeMetadata(node)
                            .publicAddresses(ImmutableSet.of(ip)).build();
                }


                // public ip
                if (node.getPublicAddresses() != null &&
                        node.getPublicAddresses().iterator().hasNext()) {
                    ip = node.getPublicAddresses().iterator().next();
                    publicIp = ip;
                    memberContext.setPublicIpAddress(ip);
                    LOG.info("Retrieving Public IP Address : " + memberContext.toString());
                }

                // private IP
                if (node.getPrivateAddresses() != null &&
                        node.getPrivateAddresses().iterator().hasNext()) {
                    ip = node.getPrivateAddresses().iterator().next();
                    memberContext.setPrivateIpAddress(ip);
                    LOG.info("Retrieving Private IP Address. " + memberContext.toString());
                }

                dataHolder.addMemberContext(memberContext);

                // persist in registry
                persist();


                // trigger topology
                TopologyBuilder.handleMemberSpawned(cartridgeType, clusterId,
                        partition.getId(), ip, publicIp, memberContext);

                String memberID = memberContext.getMemberId();

                // update the topology with the newly spawned member
                // publish data
                CartridgeInstanceDataPublisher.publish(memberID,
                        memberContext.getPartition().getId(),
                        memberContext.getNetworkPartitionId(),
                        memberContext.getClusterId(),
                        cartridgeType,
                        MemberStatus.Created.toString(),
                        node);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Node details: " + node.toString());
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("IP allocation process ended for " + memberContext);
                }

            } catch (Exception e) {
                String msg = "Error occurred while allocating an ip address. " + memberContext.toString();
                LOG.error(msg, e);
                throw new CloudControllerException(msg, e);
            }


        }
    }

    private boolean isValidIpAddress(String ip) {
        boolean isValid = InetAddresses.isInetAddress(ip);
        return isValid;
    }

    @Override
    public void terminateAllInstances(String clusterId) throws InvalidClusterException {

        LOG.info("Starting to terminate all instances of cluster : "
                + clusterId);

        handleNullObject(clusterId, "Instance termination failed. Cluster id is null.");

        List<MemberContext> ctxts = dataHolder.getMemberContextsOfClusterId(clusterId);

        if (ctxts == null) {
            String msg = "Instance termination failed. No members found for cluster id: " + clusterId;
            LOG.warn(msg);
            return;
        }

        ThreadExecutor exec = ThreadExecutor.getInstance();
        for (MemberContext memberContext : ctxts) {
            exec.execute(new InstanceTerminator(memberContext));
        }

    }


    /**
     * A helper method to terminate an instance.
     *
     * @param iaasProvider
     * @param ctxt
     * @param nodeId
     * @return will return the IaaSProvider
     */
    private IaasProvider terminate(IaasProvider iaasProvider,
                                   String nodeId, MemberContext ctxt) {
        Iaas iaas = iaasProvider.getIaas();
        if (iaas == null) {

            try {
                iaas = CloudControllerUtil.getIaas(iaasProvider);
            } catch (InvalidIaasProviderException e) {
                String msg =
                        "Instance termination failed. " + ctxt.toString() +
                                ". Cause: Unable to build Iaas of this " + iaasProvider.toString();
                LOG.error(msg, e);
                throw new CloudControllerException(msg, e);
            }

        }

        //detach volumes if any
        detachVolume(iaasProvider, ctxt);

        // destroy the node
        iaasProvider.getComputeService().destroyNode(nodeId);

        // release allocated IP address
        if (ctxt.getAllocatedIpAddress() != null) {
            iaas.releaseAddress(ctxt.getAllocatedIpAddress());
        }

        LOG.info("Member is terminated: " + ctxt.toString());
        return iaasProvider;
    }

    private void detachVolume(IaasProvider iaasProvider, MemberContext ctxt) {
        String clusterId = ctxt.getClusterId();
        ClusterContext clusterCtxt = dataHolder.getClusterContext(clusterId);
        if (clusterCtxt.getVolumes() != null) {
            for (Volume volume : clusterCtxt.getVolumes()) {
                try {
                    String volumeId = volume.getId();
                    if (volumeId == null) {
                        return;
                    }
                    Iaas iaas = iaasProvider.getIaas();
                    iaas.detachVolume(ctxt.getInstanceId(), volumeId);
                } catch (ResourceNotFoundException ignore) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(ignore);
                    }
                }
            }
        }
    }

    private void logTermination(MemberContext memberContext) {

        if (memberContext == null) {
            return;
        }

        String partitionId = memberContext.getPartition() == null ? null : memberContext.getPartition().getId();

        //updating the topology
        TopologyBuilder.handleMemberTerminated(memberContext.getCartridgeType(),
                memberContext.getClusterId(), memberContext.getNetworkPartitionId(),
                partitionId, memberContext.getMemberId());

        //publishing data
        CartridgeInstanceDataPublisher.publish(memberContext.getMemberId(),
                partitionId,
                memberContext.getNetworkPartitionId(),
                memberContext.getClusterId(),
                memberContext.getCartridgeType(),
                MemberStatus.Terminated.toString(),
                null);

        // update data holders
        dataHolder.removeMemberContext(memberContext.getMemberId(), memberContext.getClusterId());

        // persist
        persist();

    }

    @Override
    public boolean registerService(Registrant registrant)
            throws UnregisteredCartridgeException {

        String cartridgeType = registrant.getCartridgeType();
        handleNullObject(cartridgeType, "Service registration failed. Cartridge Type is null.");

        String clusterId = registrant.getClusterId();
        handleNullObject(clusterId, "Service registration failed. Cluster id is null.");

        String payload = registrant.getPayload();
        handleNullObject(payload, "Service registration failed. Payload is null.");

        String hostName = registrant.getHostName();
        handleNullObject(hostName, "Service registration failed. Hostname is null.");

        Cartridge cartridge = null;
        if ((cartridge = dataHolder.getCartridge(cartridgeType)) == null) {

            String msg = "Registration of cluster: " + clusterId +
                    " failed. - Unregistered Cartridge type: " + cartridgeType;
            LOG.error(msg);
            throw new UnregisteredCartridgeException(msg);
        }

        Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
        String property = props.getProperty(Constants.IS_LOAD_BALANCER);
        boolean isLb = property != null ? Boolean.parseBoolean(property) : false;

        //TODO fix the properties issue
        /*ClusterContext ctxt = buildClusterContext(cartridge, clusterId,
        payload, hostName, props, isLb, registrant.getPersistence());


        dataHolder.addClusterContext(ctxt);*/
        TopologyBuilder.handleClusterCreated(registrant, isLb);

        persist();

        LOG.info("Successfully registered: " + registrant);

        return true;
    }

    private ClusterContext buildClusterContext(Cartridge cartridge,
                                               String clusterId, String payload, String hostName,
                                               org.apache.stratos.common.Properties props, boolean isLb, Persistence persistence) {
        //TODO fix properties issue
        // initialize ClusterContext
		ClusterContext ctxt = new ClusterContext(clusterId, cartridge.getType(), payload, 
				hostName, isLb, props);
		
		/*String property;
		property = props.get(Constants.GRACEFUL_SHUTDOWN_TIMEOUT);
		long timeout = property != null ? Long.parseLong(property) : 30000;

        boolean persistanceRequired = false;
        if(persistence != null){
              persistanceRequired = persistence.isPersistanceRequired();
        }

        if(persistanceRequired){
            ctxt.setVolumes(persistence.getVolumes());
            ctxt.setVolumeRequired(true);
        }else{
            ctxt.setVolumeRequired(false);
        }
	    ctxt.setTimeoutInMillis(timeout);
		return ctxt;
        ;*/
        return null;
    }

    @Override
    public String[] getRegisteredCartridges() {
        // get the list of cartridges registered
        List<Cartridge> cartridges = dataHolder
                .getCartridges();

        if (cartridges == null) {
            LOG.info("No registered Cartridge found.");
            return new String[0];
        }

        String[] cartridgeTypes = new String[cartridges.size()];
        int i = 0;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Registered Cartridges : \n");
        }
        for (Cartridge cartridge : cartridges) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(cartridge);
            }
            cartridgeTypes[i] = cartridge.getType();
            i++;
        }

        return cartridgeTypes;
    }

    @Override
    public CartridgeInfo getCartridgeInfo(String cartridgeType)
            throws UnregisteredCartridgeException {
        Cartridge cartridge = dataHolder
                .getCartridge(cartridgeType);

        if (cartridge != null) {

            return CloudControllerUtil.toCartridgeInfo(cartridge);

        }

        String msg = "Cannot find a Cartridge having a type of "
                + cartridgeType + ". Hence unable to find information.";
        LOG.error(msg);
        throw new UnregisteredCartridgeException(msg);
    }

    @Override
    public void unregisterService(String clusterId) throws UnregisteredClusterException {
        final String clusterId_ = clusterId;

        ClusterContext ctxt = dataHolder.getClusterContext(clusterId_);

        handleNullObject(ctxt, "Service unregistration failed. Invalid cluster id: " + clusterId);

        String cartridgeType = ctxt.getCartridgeType();

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg =
                    "Service unregistration failed. No matching Cartridge found [type] " + cartridgeType + ". ";
            LOG.error(msg);
            throw new UnregisteredClusterException(msg);
        }

        // if it's a kubernetes cluster
        if (StratosConstants.KUBERNETES_DEPLOYER_TYPE.equals(cartridge.getDeployerType())) {
            unregisterDockerService(clusterId_);

        } else {

//	        TopologyBuilder.handleClusterMaintenanceMode(dataHolder.getClusterContext(clusterId_));

            Runnable terminateInTimeout = new Runnable() {
                @Override
                public void run() {
                    ClusterContext ctxt = dataHolder.getClusterContext(clusterId_);
                    if (ctxt == null) {
                        String msg = "Service unregistration failed. Cluster not found: " + clusterId_;
                        LOG.error(msg);
                        return;
                    }
                    Collection<Member> members = TopologyManager.getTopology().
                            getService(ctxt.getCartridgeType()).getCluster(clusterId_).getMembers();
                    //finding the responding members from the existing members in the topology.
                    int sizeOfRespondingMembers = 0;
                    for (Member member : members) {
                        if (member.getStatus().getCode() >= MemberStatus.Activated.getCode()) {
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
                                LOG.warn("Instance termination failed of member [id] " + member.getMemberId(), e);
                            }
                        }
                    }
                }
            };
            Runnable unregister = new Runnable() {
                public void run() {
                    ClusterContext ctxt = dataHolder.getClusterContext(clusterId_);
                    if (ctxt == null) {
                        String msg = "Service unregistration failed. Cluster not found: " + clusterId_;
                        LOG.error(msg);
                        return;
                    }
                    Collection<Member> members = TopologyManager.getTopology().
                            getService(ctxt.getCartridgeType()).getCluster(clusterId_).getMembers();
                    // TODO why end time is needed?
                    // long endTime = System.currentTimeMillis() + ctxt.getTimeoutInMillis() * members.size();

                    while (members.size() > 0) {
                        //waiting until all the members got removed from the Topology/ timed out
                        CloudControllerUtil.sleep(1000);
                    }

                    LOG.info("Unregistration of service cluster: " + clusterId_);
                    deleteVolumes(ctxt);
                    onClusterRemoval(clusterId_);
                }

                private void deleteVolumes(ClusterContext ctxt) {
                    if (ctxt.isVolumeRequired()) {
                        Cartridge cartridge = dataHolder.getCartridge(ctxt.getCartridgeType());
                        if (cartridge != null && cartridge.getIaases() != null && ctxt.getVolumes() != null) {
                            for (Volume volume : ctxt.getVolumes()) {
                                if (volume.getId() != null) {
                                    String iaasType = volume.getIaasType();
                                    //Iaas iaas = dataHolder.getIaasProvider(iaasType).getIaas();
                                    Iaas iaas = cartridge.getIaasProvider(iaasType).getIaas();
                                    if (iaas != null) {
                                        try {
                                            // delete the volumes if remove on unsubscription is true.
                                            if (volume.isRemoveOntermination()) {
                                                iaas.deleteVolume(volume.getId());
                                                volume.setId(null);
                                            }
                                        } catch (Exception ignore) {
                                            if (LOG.isErrorEnabled()) {
                                                LOG.error("Error while deleting volume [id] " + volume.getId(), ignore);
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            };
            new Thread(terminateInTimeout).start();
            new Thread(unregister).start();
        }
    }

    @Override
    public void unregisterDockerService(String clusterId)
            throws UnregisteredClusterException {

        // terminate all kubernetes units
        try {
            terminateAllContainers(clusterId);
        } catch (InvalidClusterException e) {
            String msg = "Docker instance termination fails for cluster: " + clusterId;
            LOG.error(msg, e);
            throw new UnregisteredClusterException(msg, e);
        }
        // send cluster removal notifications and update the state
        onClusterRemoval(clusterId);
    }


    @Override
    public boolean validateDeploymentPolicy(String cartridgeType, Partition[] partitions)
            throws InvalidPartitionException, InvalidCartridgeTypeException {

        Map<String, List<String>> validatedCache = dataHolder.getCartridgeTypeToPartitionIds();
        List<String> validatedPartitions = new ArrayList<String>();

        if (validatedCache.containsKey(cartridgeType)) {
            // cache hit for this cartridge
            // get list of partitions
            validatedPartitions = validatedCache.get(cartridgeType);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Partition validation cache hit for cartridge type: " + cartridgeType);
            }

        }

        Map<String, IaasProvider> partitionToIaasProviders =
                new ConcurrentHashMap<String, IaasProvider>();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deployment policy validation started for cartridge type: " + cartridgeType);
        }

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg = "Invalid Cartridge Type: " + cartridgeType;
            LOG.error(msg);
            throw new InvalidCartridgeTypeException(msg);
        }

        Map<String, Future<IaasProvider>> jobList = new HashMap<String, Future<IaasProvider>>();

        for (Partition partition : partitions) {

            if (validatedPartitions.contains(partition.getId())) {
                // partition cache hit
                continue;
            }

            Callable<IaasProvider> worker = new PartitionValidatorCallable(
                    partition, cartridge);
            Future<IaasProvider> job = FasterLookUpDataHolder.getInstance()
                    .getExecutor().submit(worker);
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
                partitionToIaasProviders.put(partitionId, job.get());

                // add to cache
                this.dataHolder.addToCartridgeTypeToPartitionIdMap(cartridgeType, partitionId);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Partition " + partitionId + " added to the cache against cartridge type: " + cartridgeType);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                throw new InvalidPartitionException(e.getMessage(), e);
            }
        }

        // if and only if the deployment policy valid
        cartridge.addIaasProviders(partitionToIaasProviders);

        // persist data
        persist();

        LOG.info("All partitions " + CloudControllerUtil.getPartitionIds(partitions) +
                " were validated successfully, against the Cartridge: " + cartridgeType);

        return true;
    }

    private void onClusterRemoval(final String clusterId) {
        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);
        TopologyBuilder.handleClusterRemoved(ctxt);
        dataHolder.removeClusterContext(clusterId);
        dataHolder.removeMemberContextsOfCluster(clusterId);
        persist();
    }

    @Override
    public boolean validatePartition(Partition partition) throws InvalidPartitionException {
        handleNullObject(partition, "Partition validation failed. Partition is null.");
        String provider = partition.getProvider();
        handleNullObject(provider, "Partition [" + partition.getId() + "] validation failed. Partition provider is null.");
        IaasProvider iaasProvider = dataHolder.getIaasProvider(provider);

        if (iaasProvider == null) {
            String msg =
                    "Invalid Partition - " + partition.toString() + ". Cause: Iaas Provider " +
                            "is null for Partition Provider: " + provider;
            LOG.error(msg);
            throw new InvalidPartitionException(msg);
        }

        Iaas iaas = iaasProvider.getIaas();

        if (iaas == null) {

            try {
                iaas = CloudControllerUtil.getIaas(iaasProvider);
            } catch (InvalidIaasProviderException e) {
                String msg =
                        "Invalid Partition - " + partition.toString() +
                                ". Cause: Unable to build Iaas of this IaasProvider [Provider] : " + provider + ". " + e.getMessage();
                LOG.error(msg, e);
                throw new InvalidPartitionException(msg, e);
            }

        }

        PartitionValidator validator = iaas.getPartitionValidator();
        validator.setIaasProvider(iaasProvider);
        validator.validate(partition.getId(),
                CloudControllerUtil.toJavaUtilProperties(partition.getProperties()));

        return true;
    }

    public ClusterContext getClusterContext(String clusterId) {

        return dataHolder.getClusterContext(clusterId);
    }

    @Override
    public MemberContext[] startContainers(ContainerClusterContext containerClusterContext)
            throws UnregisteredCartridgeException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:startContainers");
        }

        handleNullObject(containerClusterContext, "Container start-up failed. ContainerClusterContext is null.");

        String clusterId = containerClusterContext.getClusterId();
        handleNullObject(clusterId, "Container start-up failed. Cluster id is null.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Received a container spawn request : " + containerClusterContext.toString());
        }

        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);
        handleNullObject(ctxt, "Container start-up failed. Invalid cluster id. " + containerClusterContext.toString());

        String cartridgeType = ctxt.getCartridgeType();

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg =
                    "Instance start-up failed. No matching Cartridge found [type] " + cartridgeType + ". " +
                            containerClusterContext.toString();
            LOG.error(msg);
            throw new UnregisteredCartridgeException(msg);
        }

        try {
            String minReplicas = validateProperty(StratosConstants.KUBERNETES_MIN_REPLICAS, ctxt);
            String kubernetesClusterId = validateProperty(StratosConstants.KUBERNETES_CLUSTER_ID, ctxt);
            String kubernetesMasterIp = validateProperty(StratosConstants.KUBERNETES_MASTER_IP, containerClusterContext);
            String kubernetesPortRange = validateProperty(StratosConstants.KUBERNETES_PORT_RANGE, containerClusterContext);

            KubernetesClusterContext kubClusterContext = getKubernetesClusterContext(kubernetesClusterId, kubernetesMasterIp, kubernetesPortRange);

            KubernetesApiClient kubApi = kubClusterContext.getKubApi();

            // first let's create a replication controller.
            ContainerClusterContextToReplicationController controllerFunction = new ContainerClusterContextToReplicationController();
            ReplicationController controller = controllerFunction.apply(containerClusterContext);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller is delegating request to start a replication controller " + controller +
                        " for " + containerClusterContext + " to Kubernetes layer.");
            }

            kubApi.createReplicationController(controller);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller successfully started the controller "
                        + controller + " via Kubernetes layer.");
            }

            // secondly let's create a kubernetes service proxy to load balance these containers
            ContainerClusterContextToKubernetesService serviceFunction = new ContainerClusterContextToKubernetesService();
            Service service = serviceFunction.apply(containerClusterContext);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller is delegating request to start a service " + service +
                        " for " + containerClusterContext + " to Kubernetes layer.");
            }

            kubApi.createService(service);

            // set host port and update
            Property allocatedServiceHostPortProp = new Property();
            allocatedServiceHostPortProp.setName(StratosConstants.ALLOCATED_SERVICE_HOST_PORT);
            allocatedServiceHostPortProp.setValue(String.valueOf(service.getPort()));
            ctxt.getProperties().addProperty(allocatedServiceHostPortProp);
            dataHolder.addClusterContext(ctxt);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller successfully started the service "
                        + controller + " via Kubernetes layer.");
            }

            // create a label query
            Label l = new Label();
            l.setName(clusterId);
            // execute the label query
            Pod[] newlyCreatedPods = new Pod[0];
            int expectedCount = Integer.parseInt(minReplicas);

            for (int i = 0; i < expectedCount; i++) {
                newlyCreatedPods = kubApi.getSelectedPods(new Label[]{l});

                if (LOG.isDebugEnabled()) {

                    LOG.debug("Pods Count: " + newlyCreatedPods.length + " for cluster: " + clusterId);
                }
                if (newlyCreatedPods.length == expectedCount) {
                    break;
                }
                Thread.sleep(10000);
            }

            if (newlyCreatedPods.length == 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Pods are not created for cluster : %s, hence deleting the service", clusterId));
                }
                terminateAllContainers(clusterId);
                return new MemberContext[0];
            }

            if (LOG.isDebugEnabled()) {

                LOG.debug(String.format("Pods created : %s for cluster : %s", newlyCreatedPods.length, clusterId));
            }

            List<MemberContext> memberContexts = new ArrayList<MemberContext>();

            PodToMemberContext podToMemberContextFunc = new PodToMemberContext();
            // generate Member Contexts
            for (Pod pod : newlyCreatedPods) {
                MemberContext context = podToMemberContextFunc.apply(pod);
                context.setCartridgeType(cartridgeType);
                context.setClusterId(clusterId);

                context.setProperties(CloudControllerUtil.addProperty(context
                                .getProperties(), StratosConstants.ALLOCATED_SERVICE_HOST_PORT,
                        String.valueOf(service.getPort())));

                dataHolder.addMemberContext(context);

                // wait till Pod status turns to running and send member spawned.
                ScheduledThreadExecutor exec = ScheduledThreadExecutor.getInstance();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cloud Controller is starting the instance start up thread.");
                }
                dataHolder.addScheduledFutureJob(context.getMemberId(), exec.schedule(new PodActivationWatcher(pod.getId(), context, kubApi), 5000));

                memberContexts.add(context);
            }

            // persist in registry
            persist();

            LOG.info("Kubernetes entities are successfully starting up: " + memberContexts);

            return memberContexts.toArray(new MemberContext[0]);

        } catch (Exception e) {
            String msg = "Failed to start an instance. " + containerClusterContext.toString() + " Cause: " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    private String validateProperty(String property, ClusterContext ctxt) {

        String propVal = CloudControllerUtil.getProperty(ctxt.getProperties(), property);
        handleNullObject(propVal, "Property validation failed. Cannot find '" + property + "' in " + ctxt);
        return propVal;
    }

    private String validateProperty(String property, ContainerClusterContext ctxt) {

        String propVal = CloudControllerUtil.getProperty(ctxt.getProperties(), property);
        handleNullObject(propVal, "Property validation failed. '" + property + "' in " + ctxt);
        return propVal;

    }

    private KubernetesClusterContext getKubernetesClusterContext(
            String kubernetesClusterId, String kubernetesMasterIp,
            String kubernetesPortRange) {

        KubernetesClusterContext origCtxt = dataHolder.getKubernetesClusterContext(kubernetesClusterId);
        KubernetesClusterContext newCtxt = new KubernetesClusterContext(kubernetesClusterId, kubernetesPortRange, kubernetesMasterIp);

        if (origCtxt == null) {
            dataHolder.addKubernetesClusterContext(newCtxt);
            return newCtxt;
        }

        if (!origCtxt.equals(newCtxt)) {
            // if for some reason master IP etc. have changed
            newCtxt.setAvailableHostPorts(origCtxt.getAvailableHostPorts());
            dataHolder.addKubernetesClusterContext(newCtxt);
            return newCtxt;
        } else {
            return origCtxt;
        }
    }

    @Override
    public MemberContext[] terminateAllContainers(String clusterId)
            throws InvalidClusterException {

        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);
        handleNullObject(ctxt, "Kubernetes units temrination failed. Invalid cluster id. " + clusterId);

        String kubernetesClusterId = CloudControllerUtil.getProperty(ctxt.getProperties(),
                StratosConstants.KUBERNETES_CLUSTER_ID);
        handleNullObject(kubernetesClusterId, "Kubernetes units termination failed. Cannot find '" +
                StratosConstants.KUBERNETES_CLUSTER_ID + "'. " + ctxt);

        KubernetesClusterContext kubClusterContext = dataHolder.getKubernetesClusterContext(kubernetesClusterId);
        handleNullObject(kubClusterContext, "Kubernetes units termination failed. Cannot find a matching Kubernetes Cluster for cluster id: "
                + kubernetesClusterId);

        KubernetesApiClient kubApi = kubClusterContext.getKubApi();
        // delete the service
        try {
            kubApi.deleteService(CloudControllerUtil.getCompatibleId(clusterId));
        } catch (KubernetesClientException e) {
            // we're not going to throw this error, but proceed with other deletions
            LOG.error("Failed to delete Kubernetes service with id: " + clusterId, e);
        }

        // set replicas=0 for the replication controller
        try {
            kubApi.updateReplicationController(clusterId, 0);
        } catch (KubernetesClientException e) {
            // we're not going to throw this error, but proceed with other deletions
            LOG.error("Failed to update Kubernetes Controller with id: " + clusterId, e);
        }

        // delete pods forcefully
        try {
            // create a label query
            Label l = new Label();
            l.setName(clusterId);
            // execute the label query
            Pod[] pods = kubApi.getSelectedPods(new Label[]{l});

            for (Pod pod : pods) {
                try {
                    // delete pods forcefully
                    kubApi.deletePod(pod.getId());
                } catch (KubernetesClientException ignore) {
                    // we can't do nothing here
                    LOG.warn(String.format("Failed to delete Pod [%s] forcefully!", pod.getId()));
                }
            }
        } catch (KubernetesClientException e) {
            // we're not going to throw this error, but proceed with other deletions
            LOG.error("Failed to delete pods forcefully for cluster: " + clusterId, e);
        }

        // delete the replication controller.
        try {
            kubApi.deleteReplicationController(clusterId);
        } catch (KubernetesClientException e) {
            String msg = "Failed to delete Kubernetes Controller with id: " + clusterId;
            LOG.error(msg, e);
            throw new InvalidClusterException(msg, e);
        }

        String allocatedPort = CloudControllerUtil.getProperty(ctxt.getProperties(),
                StratosConstants.ALLOCATED_SERVICE_HOST_PORT);

        if (allocatedPort != null) {
            kubClusterContext.deallocateHostPort(Integer
                    .parseInt(allocatedPort));
        } else {
            LOG.warn("Host port dealloacation failed due to a missing property: "
                    + StratosConstants.ALLOCATED_SERVICE_HOST_PORT);
        }

        List<MemberContext> membersToBeRemoved = dataHolder.getMemberContextsOfClusterId(clusterId);

        for (MemberContext memberContext : membersToBeRemoved) {
            logTermination(memberContext);
        }

        // persist
        persist();

        return membersToBeRemoved.toArray(new MemberContext[0]);
    }

    @Override
    public MemberContext[] updateContainers(String clusterId, int replicas)
            throws UnregisteredCartridgeException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("CloudControllerServiceImpl:updateContainers for cluster : " + clusterId);
        }

        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);
        handleNullObject(ctxt, "Container update failed. Invalid cluster id. " + clusterId);

        String cartridgeType = ctxt.getCartridgeType();

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg =
                    "Container update failed. No matching Cartridge found [type] " + cartridgeType
                            + ". [cluster id] " + clusterId;
            LOG.error(msg);
            throw new UnregisteredCartridgeException(msg);
        }

        try {
            String kubernetesClusterId = validateProperty(StratosConstants.KUBERNETES_CLUSTER_ID, ctxt);

            KubernetesClusterContext kubClusterContext = dataHolder.getKubernetesClusterContext(kubernetesClusterId);

            if (kubClusterContext == null) {
                String msg =
                        "Instance start-up failed. No matching Kubernetes Context Found for [id] " + kubernetesClusterId
                                + ". [cluster id] " + clusterId;
                LOG.error(msg);
                throw new UnregisteredCartridgeException(msg);
            }

            KubernetesApiClient kubApi = kubClusterContext.getKubApi();
            // create a label query
            Label l = new Label();
            l.setName(clusterId);

            // get the current pods - useful when scale down
            Pod[] previousStatePods = kubApi.getSelectedPods(new Label[]{l});

            // update the replication controller - cluster id = replication controller id
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller is delegating request to update a replication controller " + clusterId +
                        " to Kubernetes layer.");
            }

            kubApi.updateReplicationController(clusterId, replicas);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Cloud Controller successfully updated the controller "
                        + clusterId + " via Kubernetes layer.");
            }

            // execute the label query
            Pod[] allPods = new Pod[0];

            // wait replicas*5s time in the worst case ; best case = 0s
            for (int i = 0; i < (replicas * previousStatePods.length + 1); i++) {
                allPods = kubApi.getSelectedPods(new Label[]{l});

                if (LOG.isDebugEnabled()) {

                    LOG.debug("Pods Count: " + allPods.length + " for cluster: " + clusterId);
                }
                if (allPods.length == replicas) {
                    break;
                }
                Thread.sleep(10000);
            }

            if (LOG.isDebugEnabled()) {

                LOG.debug(String.format("Pods created : %s for cluster : %s", allPods.length, clusterId));
            }

            List<MemberContext> memberContexts = new ArrayList<MemberContext>();

            PodToMemberContext podToMemberContextFunc = new PodToMemberContext();
            // generate Member Contexts
            for (Pod pod : allPods) {
                MemberContext context;
                // if member context does not exist -> a new member (scale up)
                if ((context = dataHolder.getMemberContextOfMemberId(pod.getId())) == null) {

                    context = podToMemberContextFunc.apply(pod);
                    context.setCartridgeType(cartridgeType);
                    context.setClusterId(clusterId);

                    context.setProperties(CloudControllerUtil.addProperty(context
                                    .getProperties(), StratosConstants.ALLOCATED_SERVICE_HOST_PORT,
                            CloudControllerUtil.getProperty(ctxt.getProperties(),
                                    StratosConstants.ALLOCATED_SERVICE_HOST_PORT)));

                    // wait till Pod status turns to running and send member spawned.
                    ScheduledThreadExecutor exec = ScheduledThreadExecutor.getInstance();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Cloud Controller is starting the instance start up thread.");
                    }
                    dataHolder.addScheduledFutureJob(context.getMemberId(), exec.schedule(new PodActivationWatcher(pod.getId(), context, kubApi), 5000));

                    memberContexts.add(context);

                }
                // publish data
                // TODO
//                CartridgeInstanceDataPublisher.publish(context.getMemberId(), null, null, context.getClusterId(), cartridgeType, MemberStatus.Created.toString(), node);

            }

            if (memberContexts.isEmpty()) {
                // terminated members
                @SuppressWarnings("unchecked")
                List<Pod> difference = ListUtils.subtract(Arrays.asList(previousStatePods), Arrays.asList(allPods));
                for (Pod pod : difference) {
                    if (pod != null) {
                        MemberContext context = dataHolder.getMemberContextOfMemberId(pod.getId());
                        logTermination(context);
                        memberContexts.add(context);
                    }
                }
            }


            // persist in registry
            persist();

            LOG.info("Kubernetes entities are successfully starting up. " + memberContexts);

            return memberContexts.toArray(new MemberContext[0]);

        } catch (Exception e) {
            String msg = "Failed to update containers belong to cluster " + clusterId + ". Cause: " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @Override
    public MemberContext terminateContainer(String memberId) throws MemberTerminationFailedException {

        handleNullObject(memberId, "Failed to terminate member. Invalid Member id. [Member id] " + memberId);

        MemberContext memberContext = dataHolder.getMemberContextOfMemberId(memberId);

        handleNullObject(memberContext, "Failed to terminate member. Member id not found. [Member id] " + memberId);

        String clusterId = memberContext.getClusterId();

        handleNullObject(clusterId, "Failed to terminate member. Cluster id is null. [Member id] " + memberId);

        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);

        handleNullObject(ctxt,
                String.format("Failed to terminate member [Member id] %s. Invalid cluster id %s ", memberId, clusterId));

        String kubernetesClusterId = CloudControllerUtil.getProperty(ctxt.getProperties(),
                StratosConstants.KUBERNETES_CLUSTER_ID);

        handleNullObject(kubernetesClusterId, String.format("Failed to terminate member [Member id] %s. Cannot find '" +
                StratosConstants.KUBERNETES_CLUSTER_ID + "' in [cluster context] %s ", memberId, ctxt));

        KubernetesClusterContext kubClusterContext = dataHolder.getKubernetesClusterContext(kubernetesClusterId);

        handleNullObject(kubClusterContext, String.format("Failed to terminate member [Member id] %s. Cannot find a matching Kubernetes Cluster in [cluster context] %s ", memberId, ctxt));

        KubernetesApiClient kubApi = kubClusterContext.getKubApi();
        // delete the Pod
        try {
            // member id = pod id
            kubApi.deletePod(memberId);

            MemberContext memberToBeRemoved = dataHolder.getMemberContextOfMemberId(memberId);

            logTermination(memberToBeRemoved);

            return memberToBeRemoved;

        } catch (KubernetesClientException e) {
            String msg = String.format("Failed to terminate member [Member id] %s", memberId);
            LOG.error(msg, e);
            throw new MemberTerminationFailedException(msg, e);
        }
    }

    private void handleNullObject(Object obj, String errorMsg) {
        if (obj == null) {
            LOG.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    @Override
    public void createApplicationClusters(String appId, ApplicationClusterContextDTO[] appClustersContexts)  throws
            ApplicationClusterRegistrationException {

        // Create a Cluster Context obj. for each of the Clusters in the Application
        if (appClustersContexts == null || appClustersContexts.length == 0) {
            String errorMsg = "No application cluster information found, unable to create clusters";
            LOG.error(errorMsg);
            throw new ApplicationClusterRegistrationException(errorMsg);
        }
        List<Cluster> clusters = new ArrayList<Cluster>();


        for (ApplicationClusterContextDTO appClusterCtxt : appClustersContexts) {
            dataHolder.addClusterContext(new ClusterContext(appClusterCtxt.getClusterId(),
                    appClusterCtxt.getCartridgeType(), appClusterCtxt.getTextPayload(),
                    appClusterCtxt.getHostName(), appClusterCtxt.isLbCluster(), appClusterCtxt.getProperties()));
            // create Cluster objects
            Cluster newCluster = new Cluster(appClusterCtxt.getCartridgeType(), appClusterCtxt.getClusterId(),
                    appClusterCtxt.getDeploymentPolicyName(), appClusterCtxt.getAutoscalePolicyName(), appId);
            newCluster.setLbCluster(false);
            newCluster.setTenantRange(appClusterCtxt.getTenantRange());
            newCluster.setStatus(ClusterStatus.Created);
            newCluster.setHostNames(Arrays.asList(appClusterCtxt.getHostName()));
            Cartridge cartridge = dataHolder.getCartridge(appClusterCtxt.getCartridgeType());
            if(cartridge.getDeployerType() != null &&
                    cartridge.getDeployerType().equals(StratosConstants.KUBERNETES_DEPLOYER_TYPE)) {
                newCluster.setKubernetesCluster(true);
            }
            if (appClusterCtxt.getProperties() != null) {
				Properties properties = CloudControllerUtil.toJavaUtilProperties(appClusterCtxt.getProperties());
				newCluster.setProperties(properties);
			}
            
            clusters.add(newCluster);
        }
        TopologyBuilder.handleApplicationClustersCreated(appId, clusters);

        persist();

    }

//    public void deployApplicationDefinition (ApplicationContext applicationContext) throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        Application application = applicationParser.parse(applicationContext);
//
//        // Create a Cluster Context obj. for each of the Clusters in the Application
//        for (ApplicationClusterContext applicationClusterContext : applicationParser.getApplicationClusterContexts()) {
//            dataHolder.addClusterContext(new ClusterContext(applicationClusterContext.getClusterId(),
//                    applicationClusterContext.getCartridgeType(), applicationClusterContext.getTextPayload(),
//                    applicationClusterContext.getHostName(), applicationClusterContext.isLbCluster()));
//        }
//
//
//    }
//
//    @Override
//    public void unDeployApplicationDefinition(String applicationId, int tenantId, String tenantDomain) throws ApplicationDefinitionException {
//
//        //TopologyBuilder.handleApplicationUndeployed(applicationId);
//    }


//    public void deployApplicationDefinition (ApplicationContext applicationContext) throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        Application application = applicationParser.parse(applicationContext);
//
//        // Create a Cluster Context obj. for each of the Clusters in the Application
//        for (ApplicationClusterContext applicationClusterContext : applicationParser.getApplicationClusterContexts()) {
//            dataHolder.addClusterContext(new ClusterContext(applicationClusterContext.getClusterId(),
//                    applicationClusterContext.getCartridgeType(), applicationClusterContext.getTextPayload(),
//                    applicationClusterContext.getHostName(), applicationClusterContext.isLbCluster()));
//        }
//
//        /*TopologyBuilder.handleApplicationDeployed(application, applicationParser.getApplicationClusterContexts(),
//                applicationParser.getPayloadData());
//*/
//        persist();
//    }
//
//    @Override
//    public void unDeployApplicationDefinition(String applicationId, int tenantId, String tenantDomain) throws ApplicationDefinitionException {
//
//        //TopologyBuilder.handleApplicationUndeployed(applicationId);
//    }

}

