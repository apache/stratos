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
package org.apache.stratos.cloud.controller.context;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.clustering.DistributedObjectHandler;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.internal.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.registry.Deserializer;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

/**
 * This object holds all runtime data and provides faster access. This is a Singleton class.
 */
public class CloudControllerContext implements Serializable {

    private static final long serialVersionUID = -2662307358852779897L;
    private static final Log log = LogFactory.getLog(CloudControllerContext.class);

    public static final String CC_CLUSTER_ID_TO_MEMBER_CTX = "CC_CLUSTER_ID_TO_MEMBER_CTX";
    public static final String CC_MEMBER_ID_TO_MEMBER_CTX = "CC_MEMBER_ID_TO_MEMBER_CTX";
    public static final String CC_MEMBER_ID_TO_SCH_TASK = "CC_MEMBER_ID_TO_SCH_TASK";
    public static final String CC_KUB_CLUSTER_ID_TO_KUB_CLUSTER_CTX = "CC_KUB_CLUSTER_ID_TO_KUB_CLUSTER_CTX";
    public static final String CC_CLUSTER_ID_TO_CLUSTER_CTX = "CC_CLUSTER_ID_TO_CLUSTER_CTX";
    public static final String CC_CARTRIDGE_TYPE_TO_PARTITION_IDS = "CC_CARTRIDGE_TYPE_TO_PARTITION_IDS";
    public static final String CC_CARTRIDGES = "CC_CARTRIDGES";
    public static final String CC_SERVICE_GROUPS = "CC_SERVICE_GROUPS";

    private static volatile CloudControllerContext instance;

    private final DistributedObjectHandler distributedObjectHandler;

	/* We keep following maps in order to make the look up time, small. */

    /**
     * Key - cluster id
     * Value - list of {@link MemberContext}
     */
    private Map<String, List<MemberContext>> clusterIdToMemberContextListMap;

    /**
     * Key - member id
     * Value - {@link MemberContext}
     */
    private Map<String, MemberContext> memberIdToMemberContextMap;

    /**
     * Key - member id
     * Value - ScheduledFuture task
     */
    private transient Map<String, ScheduledFuture<?>> memberIdToScheduledTaskMap;

    /**
     * Key - Kubernetes cluster id
     * Value - {@link org.apache.stratos.cloud.controller.domain.KubernetesClusterContext}
     */
    private Map<String, KubernetesClusterContext> kubClusterIdToKubClusterContextMap;

    /**
     * Key - cluster id
     * Value - {@link org.apache.stratos.cloud.controller.domain.ClusterContext}
     */
    private Map<String, ClusterContext> clusterIdToContextMap;

    /**
     * This works as a cache to hold already validated partitions against a cartridge type.
     * Key - cartridge type
     * Value - list of partition ids
     */
    private Map<String, List<String>> cartridgeTypeToPartitionIdsMap = new ConcurrentHashMap<String, List<String>>();

    /**
     * Thread pool used in this task to execute parallel tasks.
     */
    private transient ExecutorService executorService = Executors.newFixedThreadPool(20);

    /**
     * List of registered {@link org.apache.stratos.cloud.controller.domain.Cartridge}s
     */
    private List<Cartridge> cartridges;

    /**
     * List of deployed service groups
     */
    private List<ServiceGroup> serviceGroups;

    private String streamId;
    private boolean isPublisherRunning;
    private boolean isTopologySyncRunning;
    private boolean clustered;

    private transient AsyncDataPublisher dataPublisher;

    private CloudControllerContext() {
        // Check clustering status
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        if ((axisConfiguration != null) && (axisConfiguration.getClusteringAgent() != null)) {
            clustered = true;
        }

        // Initialize distributed object handler
        distributedObjectHandler = new DistributedObjectHandler(isClustered(),
                ServiceReferenceHolder.getInstance().getHazelcastInstance());

        // Initialize objects
        clusterIdToMemberContextListMap = distributedObjectHandler.getMap(CC_CLUSTER_ID_TO_MEMBER_CTX);
        memberIdToMemberContextMap = distributedObjectHandler.getMap(CC_MEMBER_ID_TO_MEMBER_CTX);
        memberIdToScheduledTaskMap = distributedObjectHandler.getMap(CC_MEMBER_ID_TO_SCH_TASK);
        kubClusterIdToKubClusterContextMap = distributedObjectHandler.getMap(CC_KUB_CLUSTER_ID_TO_KUB_CLUSTER_CTX);
        clusterIdToContextMap = distributedObjectHandler.getMap(CC_CLUSTER_ID_TO_CLUSTER_CTX);
        cartridgeTypeToPartitionIdsMap = distributedObjectHandler.getMap(CC_CARTRIDGE_TYPE_TO_PARTITION_IDS);
        cartridges = distributedObjectHandler.getList(CC_CARTRIDGES);
        serviceGroups = distributedObjectHandler.getList(CC_SERVICE_GROUPS);

        // Update context from the registry
        updateContextFromRegistry();
    }

    public static CloudControllerContext getInstance() {
        if (instance == null) {
            synchronized (CloudControllerContext.class) {
                if (instance == null) {
                    instance = new CloudControllerContext();
                }
            }
        }
        return instance;
    }

    public List<Cartridge> getCartridges() {
        return cartridges;
    }

    public void setCartridges(List<Cartridge> cartridges) {
        this.cartridges = cartridges;
    }

    public void setServiceGroups(List<ServiceGroup> serviceGroups) {
        this.serviceGroups = serviceGroups;
    }

    public List<ServiceGroup> getServiceGroups() {
        return this.serviceGroups;
    }

    public Cartridge getCartridge(String cartridgeType) {
        for (Cartridge cartridge : cartridges) {
            if (cartridge.getType().equals(cartridgeType)) {
                return cartridge;
            }
        }
        return null;
    }

    public void addCartridge(Cartridge newCartridges) {
        distributedObjectHandler.addToList(cartridges, newCartridges);
    }

    public ServiceGroup getServiceGroup(String name) {
        for (ServiceGroup serviceGroup : serviceGroups) {
            if (serviceGroup.getName().equals(name)) {
                return serviceGroup;
            }
        }
        return null;
    }

    public void addServiceGroup(ServiceGroup newServiceGroup) {
        distributedObjectHandler.addToList(serviceGroups, newServiceGroup);
    }

    public void removeServiceGroup(List<ServiceGroup> serviceGroup) {
        if (this.serviceGroups != null) {
            this.serviceGroups.removeAll(serviceGroup);
        }
    }

    public AsyncDataPublisher getDataPublisher() {
        return dataPublisher;
    }

    public void setDataPublisher(AsyncDataPublisher dataPublisher) {
        this.dataPublisher = dataPublisher;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public boolean isPublisherRunning() {
        return isPublisherRunning;
    }

    public void setPublisherRunning(boolean isPublisherRunning) {
        this.isPublisherRunning = isPublisherRunning;
    }

    public boolean isTopologySyncRunning() {
        return isTopologySyncRunning;
    }

    public void setTopologySyncRunning(boolean isTopologySyncRunning) {
        this.isTopologySyncRunning = isTopologySyncRunning;
    }

    public void addMemberContext(MemberContext memberContext) {
        distributedObjectHandler.putToMap(memberIdToMemberContextMap, memberContext.getMemberId(), memberContext);

        List<MemberContext> memberContextList;
        if ((memberContextList = clusterIdToMemberContextListMap.get(memberContext.getClusterId())) == null) {
            memberContextList = new ArrayList<MemberContext>();
        }
        if (memberContextList.contains(memberContext)) {
            distributedObjectHandler.removeFromList(memberContextList,memberContext);
        }
        distributedObjectHandler.addToList(memberContextList, memberContext);
        distributedObjectHandler.putToMap(clusterIdToMemberContextListMap, memberContext.getClusterId(),
                memberContextList);
        if (log.isDebugEnabled()) {
            log.debug("Added member context to the cloud controller context: " + memberContext);
        }
    }

    public void addScheduledFutureJob(String memberId, ScheduledFuture<?> job) {
        distributedObjectHandler.putToMap(memberIdToScheduledTaskMap, memberId, job);
    }

    public List<MemberContext> removeMemberContextsOfCluster(String clusterId) {
        List<MemberContext> memberContextList = clusterIdToMemberContextListMap.get(clusterId);
        distributedObjectHandler.removeFromMap(clusterIdToMemberContextListMap, clusterId);
        if (memberContextList == null) {
            return new ArrayList<MemberContext>();
        }
        for (MemberContext memberContext : memberContextList) {
            String memberId = memberContext.getMemberId();
            distributedObjectHandler.removeFromMap(memberIdToMemberContextMap, memberId);
            ScheduledFuture<?> task = memberIdToScheduledTaskMap.get(memberId);
            distributedObjectHandler.removeFromMap(memberIdToScheduledTaskMap, memberId);
            stopTask(task);

            if (log.isDebugEnabled()) {
                log.debug("Removed member context from cloud controller context: " +
                        "[member-id] " + memberId);
            }
        }
        return memberContextList;
    }

    public MemberContext removeMemberContext(String memberId, String clusterId) {
        MemberContext removedMemberContext = memberIdToMemberContextMap.get(memberId);
        distributedObjectHandler.removeFromMap(memberIdToMemberContextMap, memberId);

        List<MemberContext> memberContextList = clusterIdToMemberContextListMap.get(clusterId);
        if (memberContextList != null) {
            List<MemberContext> newCtxts = new ArrayList<MemberContext>(memberContextList);
            for (Iterator<MemberContext> iterator = newCtxts.iterator(); iterator.hasNext(); ) {
                MemberContext memberContext = iterator.next();
                if (memberId.equals(memberContext.getMemberId())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Member context removed from cloud controller context: [member-id] " + memberId);
                    }
                    iterator.remove();
                }
            }
            distributedObjectHandler.putToMap(clusterIdToMemberContextListMap, clusterId, newCtxts);
        }
        ScheduledFuture<?> task = memberIdToScheduledTaskMap.get(memberId);
        distributedObjectHandler.removeFromMap(memberIdToScheduledTaskMap, memberId);
        stopTask(task);
        return removedMemberContext;
    }

    private void stopTask(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(true);
            log.info("Scheduled pod activation watcher task canceled");
        }
    }

    public MemberContext getMemberContextOfMemberId(String memberId) {
        return memberIdToMemberContextMap.get(memberId);
    }

    public List<MemberContext> getMemberContextsOfClusterId(String clusterId) {
        return clusterIdToMemberContextListMap.get(clusterId);
    }

    public void addClusterContext(ClusterContext ctxt) {
        distributedObjectHandler.putToMap(clusterIdToContextMap, ctxt.getClusterId(), ctxt);
    }

    public ClusterContext getClusterContext(String clusterId) {
        return clusterIdToContextMap.get(clusterId);
    }

    public ClusterContext removeClusterContext(String clusterId) {
        ClusterContext removed = clusterIdToContextMap.get(clusterId);
        distributedObjectHandler.removeFromMap(clusterIdToContextMap, clusterId);
        return removed;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public List<String> getPartitionIds(String cartridgeType) {
        return cartridgeTypeToPartitionIdsMap.get(cartridgeType);
    }

    public void addToCartridgeTypeToPartitionIdMap(String cartridgeType, String partitionId) {
        List<String> list = this.cartridgeTypeToPartitionIdsMap.get(cartridgeType);
        if (list == null) {
            list = new ArrayList<String>();
        }
        list.add(partitionId);
        distributedObjectHandler.putToMap(cartridgeTypeToPartitionIdsMap, cartridgeType, list);
    }

    public void removeFromCartridgeTypeToPartitionIds(String cartridgeType) {
        distributedObjectHandler.removeFromMap(cartridgeTypeToPartitionIdsMap, cartridgeType);
    }

    public KubernetesClusterContext getKubernetesClusterContext(String kubClusterId) {
        return kubClusterIdToKubClusterContextMap.get(kubClusterId);
    }

    public void addKubernetesClusterContext(KubernetesClusterContext kubernetesClusterContext) {
        distributedObjectHandler.putToMap(kubClusterIdToKubClusterContextMap,
                kubernetesClusterContext.getKubernetesClusterId(),
                kubernetesClusterContext);
    }

    public boolean isClustered() {
        return clustered;
    }

    public boolean isCoordinator() {
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        ClusteringAgent clusteringAgent = axisConfiguration.getClusteringAgent();
        return ((axisConfiguration != null) && (clusteringAgent != null) && (clusteringAgent.isCoordinator()));
    }

    public void persist() throws RegistryException {
        if ((!isClustered()) || (isCoordinator())) {
            RegistryManager.getInstance().persist(CloudControllerConstants.DATA_RESOURCE, this);
        }
    }

    private void updateContextFromRegistry() {
        if ((!isClustered()) || (isCoordinator())) {
            try {
                Object obj = RegistryManager.getInstance().read(CloudControllerConstants.DATA_RESOURCE);
                if (obj != null) {
                    Object dataObj = Deserializer.deserializeFromByteArray((byte[]) obj);
                    if (dataObj instanceof CloudControllerContext) {
                        CloudControllerContext serializedObj = (CloudControllerContext) dataObj;

                        copyMap(clusterIdToMemberContextListMap, serializedObj.clusterIdToMemberContextListMap);
                        copyMap(memberIdToMemberContextMap, serializedObj.memberIdToMemberContextMap);
                        copyMap(memberIdToScheduledTaskMap, serializedObj.memberIdToScheduledTaskMap);
                        copyMap(kubClusterIdToKubClusterContextMap, serializedObj.kubClusterIdToKubClusterContextMap);
                        copyMap(clusterIdToContextMap, serializedObj.clusterIdToContextMap);
                        copyMap(cartridgeTypeToPartitionIdsMap, serializedObj.cartridgeTypeToPartitionIdsMap);

                        copyList(cartridges, serializedObj.getCartridges());
                        copyList(serviceGroups, serializedObj.getServiceGroups());

                        if (log.isDebugEnabled()) {
                            log.debug("Cloud controller context is read from the registry");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Cloud controller context could not be found in the registry");
                        }
                    }
                }
            } catch (Exception e) {
                String msg = "Unable to read cloud controller context from the registry. " +
                        "Hence, any historical data will not be reflected";
                log.warn(msg, e);
            }
        }
    }

    private void copyMap(Map sourceMap, Map destinationMap) {
        for(Object key : sourceMap.keySet()) {
            distributedObjectHandler.putToMap(destinationMap, key, sourceMap.get(key));
        }
    }

    private void copyList(List sourceList, List destinationList) {
        for(Object item : sourceList) {
            distributedObjectHandler.addToList(destinationList, item);
        }
    }
}