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

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.exception.NonExistingKubernetesClusterException;
import org.apache.stratos.cloud.controller.exception.NonExistingKubernetesHostException;
import org.apache.stratos.cloud.controller.internal.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;

/**
 * This object holds all runtime data and provides faster access. This is a Singleton class.
 */
public class CloudControllerContext implements Serializable {

    private static final long serialVersionUID = -2662307358852779897L;
    private static final Log log = LogFactory.getLog(CloudControllerContext.class);

    private static final String CC_CLUSTER_ID_TO_MEMBER_CTX_MAP = "CC_CLUSTER_ID_TO_MEMBER_CTX_MAP";
    private static final String CC_CLUSTER_ID_TO_CLUSTER_CTX = "CC_CLUSTER_ID_TO_CLUSTER_CTX";
    private static final String CC_MEMBER_ID_TO_MEMBER_CTX_MAP = "CC_MEMBER_ID_TO_MEMBER_CTX_MAP";
    private static final String CC_MEMBER_ID_TO_SCH_TASK_MAP = "CC_MEMBER_ID_TO_SCH_TASK_MAP";
    private static final String CC_KUB_GROUP_ID_TO_GROUP_MAP = "CC_KUB_GROUP_ID_TO_GROUP_MAP";
    private static final String CC_KUB_CLUSTER_ID_TO_KUB_CLUSTER_CTX_MAP = "CC_KUB_CLUSTER_ID_TO_KUB_CLUSTER_CTX_MAP";
    private static final String CC_CARTRIDGE_TYPE_TO_PARTITION_IDS_MAP = "CC_CARTRIDGE_TYPE_TO_PARTITION_IDS_MAP";
    private static final String CC_CARTRIDGE_TYPE_TO_CARTRIDGES_MAP = "CC_CARTRIDGE_TYPE_TO_CARTRIDGES_MAP";
    private static final String CC_SERVICE_GROUP_NAME_TO_SERVICE_GROUP_MAP = "CC_SERVICE_GROUP_NAME_TO_SERVICE_GROUP_MAP";
	private static final String CC_DEPLOYMENT_POLICY_ID_TO_DEPLOYMENT_POLICY_MAP = "CC_DEPLOYMENT_POLICY_ID_TO_DEPLOYMENT_POLICY_MAP";
	private static final String CC_NETWORK_PARTITION_ID_TO_NETWORK_PARTITION_MAP = "CC_NETWORK_PARTITION_ID_TO_NETWORK_PARTITION_MAP";

    private static final String CC_CLUSTER_CTX_WRITE_LOCK = "CC_CLUSTER_CTX_WRITE_LOCK";
    private static final String CC_MEMBER_CTX_WRITE_LOCK = "CC_MEMBER_CTX_WRITE_LOCK";
    private static final String CC_SCH_TASK_WRITE_LOCK = "CC_SCH_TASK_WRITE_LOCK";
    private static final String CC_KUB_GROUP_WRITE_LOCK = "CC_KUB_GROUP_WRITE_LOCK";
    private static final String CC_KUB_CLUSTER_CTX_WRITE_LOCK = "CC_KUB_CLUSTER_CTX_WRITE_LOCK";
    private static final String CC_CARTRIDGES_WRITE_LOCK = "CC_CARTRIDGES_WRITE_LOCK";
    private static final String CC_SERVICE_GROUPS_WRITE_LOCK = "CC_SERVICE_GROUPS_WRITE_LOCK";

    private static final String CC_PARTITION_TO_IAAS_PROVIDER_BY_CARTRIDGE_MAP = "CC_PARTITION_TO_IAAS_PROVIDER_BY_CARTRIDGE_MAP";
    private static final String CC_CARTRIDGE_TYPE_TO_IAAS_PROVIDER_MAP = "CC_CARTRIDGE_TYPE_TO_IAAS_PROVIDER_MAP";
    private static volatile CloudControllerContext instance;

    private final transient DistributedObjectProvider distributedObjectProvider;

	/* We keep following maps in order to make the look up time, small. */
    
    /** 
     * KubernetesClusters against clusterIds
     * Key - Kubernetes cluster id
     * Value - {@link org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster}
     */
    private Map<String, KubernetesCluster> kubernetesClustersMap;

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
     * Value - {@link org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesClusterContext}
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
    private transient ExecutorService executorService = StratosThreadPool.getExecutorService(
            "cloud.controller.context.thread.pool", 10);

    /**
     * Map of registered {@link org.apache.stratos.cloud.controller.domain.Cartridge}s
     * Key - cartridge type
     * Value - cartridge
     */
    private Map<String, Cartridge> cartridgeTypeToCartridgeMap;

    /**
     * Map of deployed cartridge groups
     * Key - cartridge group name
     * Value cartridge group
     */
    private Map<String, ServiceGroup> serviceGroupNameToServiceGroupMap;

	/**
	 * Map of deploy policy
	 * Key - deployment policy id
	 * Value deployment policy
	 */
	private Map<String, DeploymentPolicy> deploymentPolicyIDToDeployPolicyMap;
	
	/**
	 * Map of network partitions
	 * Key - network partition id
	 * Value network partition
	 */
	private Map<String, NetworkPartition> networkPartitionIDToNetworkPartitionMap;

    /**
     * Key - cartridge id (cartridge type is used as an unique identifier)
     * Value - Inner Key - partition id
     * Value - Inner Value - Corresponding IaasProvider.
     */
    private Map<String, Map<String, IaasProvider>> partitionToIaasProviderByCartridge;

    /**
     * Key - cartridge id (cartridge type is used as an unique identifier)
     * Value - IaasProvider
     */
    private Map<String, List<IaasProvider>> cartridgeTypeToIaasProvider;

    private String streamId;
    private boolean isPublisherRunning;
    private boolean isTopologySyncRunning;
    private boolean clustered;

    private transient AsyncDataPublisher dataPublisher;
    private boolean coordinator;

    private CloudControllerContext() {
        // Check clustering status
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        if ((axisConfiguration != null) && (axisConfiguration.getClusteringAgent() != null)) {
            clustered = true;
        }

        // Initialize distributed object provider
        distributedObjectProvider = ServiceReferenceHolder.getInstance().getDistributedObjectProvider();

        // Initialize objects
        kubernetesClustersMap = distributedObjectProvider.getMap(CC_KUB_GROUP_ID_TO_GROUP_MAP);
        clusterIdToMemberContextListMap = distributedObjectProvider.getMap(CC_CLUSTER_ID_TO_MEMBER_CTX_MAP);
        memberIdToMemberContextMap = distributedObjectProvider.getMap(CC_MEMBER_ID_TO_MEMBER_CTX_MAP);
        memberIdToScheduledTaskMap = distributedObjectProvider.getMap(CC_MEMBER_ID_TO_SCH_TASK_MAP);
        kubClusterIdToKubClusterContextMap = distributedObjectProvider.getMap(CC_KUB_CLUSTER_ID_TO_KUB_CLUSTER_CTX_MAP);
        clusterIdToContextMap = distributedObjectProvider.getMap(CC_CLUSTER_ID_TO_CLUSTER_CTX);
        cartridgeTypeToPartitionIdsMap = distributedObjectProvider.getMap(CC_CARTRIDGE_TYPE_TO_PARTITION_IDS_MAP);
        cartridgeTypeToCartridgeMap = distributedObjectProvider.getMap(CC_CARTRIDGE_TYPE_TO_CARTRIDGES_MAP);
        serviceGroupNameToServiceGroupMap = distributedObjectProvider.getMap(CC_SERVICE_GROUP_NAME_TO_SERVICE_GROUP_MAP);
		deploymentPolicyIDToDeployPolicyMap=distributedObjectProvider.getMap(CC_DEPLOYMENT_POLICY_ID_TO_DEPLOYMENT_POLICY_MAP);
		networkPartitionIDToNetworkPartitionMap = distributedObjectProvider.getMap(CC_NETWORK_PARTITION_ID_TO_NETWORK_PARTITION_MAP);
        partitionToIaasProviderByCartridge = distributedObjectProvider.getMap(CC_PARTITION_TO_IAAS_PROVIDER_BY_CARTRIDGE_MAP);
        cartridgeTypeToIaasProvider = distributedObjectProvider.getMap(CC_CARTRIDGE_TYPE_TO_IAAS_PROVIDER_MAP);
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

    public java.util.Collection<Cartridge> getCartridges() {
        return cartridgeTypeToCartridgeMap.values();
    }

    public void addCartridges(List<Cartridge> cartridges) {
        for(Cartridge cartridge : cartridges) {
            addCartridge(cartridge);
        }
    }

    public void addServiceGroups(List<ServiceGroup> serviceGroups) {
        for(ServiceGroup serviceGroup : serviceGroups) {
            addServiceGroup(serviceGroup);
        }
    }

    public Collection<ServiceGroup> getServiceGroups() {
        return serviceGroupNameToServiceGroupMap.values();
    }

    public Cartridge getCartridge(String cartridgeType) {
        return cartridgeTypeToCartridgeMap.get(cartridgeType);
    }

    private Lock acquireWriteLock(String object) {
        return distributedObjectProvider.acquireLock(object);
    }

    public void releaseWriteLock(Lock lock) {
        distributedObjectProvider.releaseLock(lock);
    }

    public Lock acquireClusterContextWriteLock() {
        return acquireWriteLock(CC_CLUSTER_CTX_WRITE_LOCK);
    }

    public Lock acquireMemberContextWriteLock() {
        return acquireWriteLock(CC_MEMBER_CTX_WRITE_LOCK);
    }

    public Lock acquireScheduleTaskWriteLock() {
        return acquireWriteLock(CC_SCH_TASK_WRITE_LOCK);
    }

    public Lock acquireKubernetesClusterWriteLock() {
        return acquireWriteLock(CC_KUB_GROUP_WRITE_LOCK);
    }

    public Lock acquireKubernetesClusterContextWriteLock() {
        return acquireWriteLock(CC_KUB_CLUSTER_CTX_WRITE_LOCK);
    }

    public Lock acquireCartridgesWriteLock() {
        return acquireWriteLock(CC_CARTRIDGES_WRITE_LOCK);
    }

    public Lock acquireServiceGroupsWriteLock() {
        return acquireWriteLock(CC_SERVICE_GROUPS_WRITE_LOCK);
    }

    public void addCartridge(Cartridge cartridge) {
        cartridgeTypeToCartridgeMap.put(cartridge.getType(), cartridge);
    }

	public void addDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
		deploymentPolicyIDToDeployPolicyMap.put(deploymentPolicy.getDeploymentPolicyID(), deploymentPolicy);
	}

	public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyID) {
		return deploymentPolicyIDToDeployPolicyMap.get(deploymentPolicyID);
	}
	
	public Collection<DeploymentPolicy> getDeploymentPolicies() {
		return deploymentPolicyIDToDeployPolicyMap.values();
	}

	public void removeDeploymentPolicy(String deploymentPolicyID) {
		if (deploymentPolicyIDToDeployPolicyMap.containsKey(deploymentPolicyID)) {
			deploymentPolicyIDToDeployPolicyMap.remove(deploymentPolicyID);
		}
	}
	
	public void addNetworkPartition(NetworkPartition networkPartition) {
		networkPartitionIDToNetworkPartitionMap.put(networkPartition.getId(), networkPartition);
	}

	public NetworkPartition getNetworkPartition(String networkPartitionID) {
		return networkPartitionIDToNetworkPartitionMap.get(networkPartitionID);
	}
	
	public Collection<NetworkPartition> getNetworkPartitions() {
		return networkPartitionIDToNetworkPartitionMap.values();
	}

	public void removeNetworkPartition(String networkPartitionID) {
		networkPartitionIDToNetworkPartitionMap.remove(networkPartitionID);
	}

	public void removeCartridge(Cartridge cartridge) {
        if(cartridgeTypeToCartridgeMap.containsKey(cartridge.getType())) {
            cartridgeTypeToCartridgeMap.remove(cartridge.getType());
        }
    }

    public void updateCartridge(Cartridge cartridge) {
        cartridgeTypeToCartridgeMap.put(cartridge.getType(), cartridge);
    }

    public ServiceGroup getServiceGroup(String name) {
        return serviceGroupNameToServiceGroupMap.get(name);
    }

    public void addServiceGroup(ServiceGroup serviceGroup) {
        serviceGroupNameToServiceGroupMap.put(serviceGroup.getName(), serviceGroup);
    }

    public void removeServiceGroups(List<ServiceGroup> serviceGroups) {
        for(ServiceGroup serviceGroup : serviceGroups) {
            removeServiceGroup(serviceGroup);
        }
    }

    private void removeServiceGroup(ServiceGroup serviceGroup) {
        if(serviceGroupNameToServiceGroupMap.containsKey(serviceGroup.getName())) {
            serviceGroupNameToServiceGroupMap.remove(serviceGroup.getName());
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
        memberIdToMemberContextMap.put(memberContext.getMemberId(), memberContext);

        List<MemberContext> memberContextList;
        if ((memberContextList = clusterIdToMemberContextListMap.get(memberContext.getClusterId())) == null) {
            memberContextList = new ArrayList<MemberContext>();
        }
        if (memberContextList.contains(memberContext)) {
            memberContextList.remove(memberContext);
        }
        memberContextList.add(memberContext);
        clusterIdToMemberContextListMap.put(memberContext.getClusterId(), memberContextList);
        if (log.isDebugEnabled()) {
            log.debug("Added member context to the cloud controller context: " + memberContext);
        }
    }

    public void updateMemberContext(MemberContext memberContext) {
        memberIdToMemberContextMap.put(memberContext.getMemberId(), memberContext);

        List<MemberContext> memberContextList;
        if ((memberContextList = clusterIdToMemberContextListMap.get(memberContext.getClusterId())) == null) {
            memberContextList = new ArrayList<MemberContext>();
        }
        if (memberContextList.contains(memberContext)) {
            memberContextList.remove(memberContext);
        }
        memberContextList.add(memberContext);
        clusterIdToMemberContextListMap.put(memberContext.getClusterId(), memberContextList);
    }

    public void addScheduledFutureJob(String memberId, ScheduledFuture<?> job) {
        memberIdToScheduledTaskMap.put(memberId, job);
    }

    public List<MemberContext> removeMemberContextsOfCluster(String clusterId) {
        List<MemberContext> memberContextList = clusterIdToMemberContextListMap.get(clusterId);
        clusterIdToMemberContextListMap.remove(clusterId);
        if (memberContextList == null) {
            return new ArrayList<MemberContext>();
        }
        for (MemberContext memberContext : memberContextList) {
            String memberId = memberContext.getMemberId();
            memberIdToMemberContextMap.remove(memberId);
            ScheduledFuture<?> task = memberIdToScheduledTaskMap.get(memberId);
            memberIdToScheduledTaskMap.remove(memberId);
            stopTask(task);

            if (log.isDebugEnabled()) {
                log.debug("Removed member context from cloud controller context: " +
                        "[member-id] " + memberId);
            }
        }
        return memberContextList;
    }

    public MemberContext removeMemberContext(String clusterId, String memberId) {
        MemberContext removedMemberContext = memberIdToMemberContextMap.get(memberId);
        memberIdToMemberContextMap.remove(memberId);

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
            clusterIdToMemberContextListMap.put(clusterId, newCtxts);
        }
        ScheduledFuture<?> task = memberIdToScheduledTaskMap.get(memberId);
        memberIdToScheduledTaskMap.remove(memberId);
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

    public void addClusterContext(ClusterContext clusterContext) {
        clusterIdToContextMap.put(clusterContext.getClusterId(), clusterContext);
    }

    public void updateClusterContext(ClusterContext clusterContext) {
        clusterIdToContextMap.put(clusterContext.getClusterId(), clusterContext);
    }

    public ClusterContext getClusterContext(String clusterId) {
        return clusterIdToContextMap.get(clusterId);
    }

    public ClusterContext removeClusterContext(String clusterId) {
        ClusterContext removed = clusterIdToContextMap.get(clusterId);
        clusterIdToContextMap.remove(clusterId);
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
        cartridgeTypeToPartitionIdsMap.put(cartridgeType, list);
    }

    public void removeFromCartridgeTypeToPartitionIds(String cartridgeType) {
        cartridgeTypeToPartitionIdsMap.remove(cartridgeType);
    }

    public KubernetesClusterContext getKubernetesClusterContext(String kubernetesClusterId) {
        return kubClusterIdToKubClusterContextMap.get(kubernetesClusterId);
    }

    public void addKubernetesClusterContext(KubernetesClusterContext kubernetesClusterContext) {
        kubClusterIdToKubClusterContextMap.put(
                kubernetesClusterContext.getKubernetesClusterId(), kubernetesClusterContext);
    }

    public void updateKubernetesClusterContext(KubernetesClusterContext kubernetesClusterContext) {
        kubClusterIdToKubClusterContextMap.put(
                kubernetesClusterContext.getKubernetesClusterId(), kubernetesClusterContext);
    }
    
    /**
     * Remove a registered Kubernetes cluster from registry
     */
    public synchronized void removeKubernetesCluster(String kubernetesClusterId) {
        // Remove entry from information model
        kubernetesClustersMap.remove(kubernetesClusterId);
    }

    /**
     * Remove a registered Kubernetes host from registry
     */
    public synchronized boolean removeKubernetesHost(String kubernetesHostId) throws NonExistingKubernetesHostException {
        if (kubernetesHostId == null) {
            throw new NonExistingKubernetesHostException("Kubernetes host id can not be null");
        }
        if (log.isInfoEnabled()) {
            log.info("Removing Kubernetes Host: " + kubernetesHostId);
        }
        try {
            KubernetesCluster kubernetesClusterStored = getKubernetesClusterContainingHost(kubernetesHostId);

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

            return true;
        } catch (Exception e) {
            throw new NonExistingKubernetesHostException(e.getMessage(), e);
        }
    }

    public void addKubernetesCluster(KubernetesCluster kubernetesCluster) {
        kubernetesClustersMap.put(kubernetesCluster.getClusterId(), kubernetesCluster);
    }

    public void updateKubernetesCluster(KubernetesCluster kubernetesCluster) {
        kubernetesClustersMap.put(kubernetesCluster.getClusterId(), kubernetesCluster);
    }
    
    public boolean kubernetesClusterExists(KubernetesCluster kubernetesCluster) {
        return kubernetesClustersMap.containsKey(kubernetesCluster);
    }

    public boolean kubernetesHostExists(String hostId) {
        if (StringUtils.isEmpty(hostId)) {
            return false;
        }
        for (KubernetesCluster kubernetesCluster : kubernetesClustersMap.values()) {
            if (kubernetesCluster.getKubernetesHosts() != null) {
                for (KubernetesHost kubernetesHost : kubernetesCluster.getKubernetesHosts()) {
                    if (kubernetesHost.getHostId().equals(hostId)) {
                        return true;
                    }
                }
            }
            if (hostId.equals(kubernetesCluster.getKubernetesMaster().getHostId())) {
                return true;
            }
        }
        return false;
    }


    public KubernetesHost[] getKubernetesHostsInGroup(String kubernetesClusterId) throws NonExistingKubernetesClusterException {
        if (StringUtils.isEmpty(kubernetesClusterId)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id is null");
        }

        KubernetesCluster kubernetesCluster = kubernetesClustersMap.get(kubernetesClusterId);
        if (kubernetesCluster != null) {
            return kubernetesCluster.getKubernetesHosts();
        }
        throw new NonExistingKubernetesClusterException("Kubernetes cluster not found: [kubernetes-cluster-id] "
                + kubernetesClusterId);
    }

    public KubernetesMaster getKubernetesMasterInGroup(String kubernetesClusterId)
            throws NonExistingKubernetesClusterException {
        if (StringUtils.isEmpty(kubernetesClusterId)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id is null");
        }
        KubernetesCluster kubernetesCluster = kubernetesClustersMap.get(kubernetesClusterId);
        if (kubernetesCluster != null) {
            return kubernetesCluster.getKubernetesMaster();
        }
        throw new NonExistingKubernetesClusterException("Kubernetes master not found: [kubernetes-cluster-id] "
                + kubernetesClusterId);
    }

    public KubernetesCluster getKubernetesCluster(String kubernetesClusterId)
            throws NonExistingKubernetesClusterException {
        if (StringUtils.isEmpty(kubernetesClusterId)) {
            throw new NonExistingKubernetesClusterException("Kubernetes cluster id is empty");
        }
        KubernetesCluster kubernetesCluster = kubernetesClustersMap.get(kubernetesClusterId);
        if (kubernetesCluster != null) {
            return kubernetesCluster;
        }
        throw new NonExistingKubernetesClusterException("Kubernetes cluster not found: [kubernetes-cluster-id] " + kubernetesClusterId);
    }

    public KubernetesCluster getKubernetesClusterContainingHost(String hostId) throws NonExistingKubernetesClusterException {
        if (StringUtils.isEmpty(hostId)) {
            return null;
        }
        for (KubernetesCluster kubernetesCluster : kubernetesClustersMap.values()) {
            if (hostId.equals(kubernetesCluster.getKubernetesMaster().getHostId())) {
                return kubernetesCluster;
            }
            if (kubernetesCluster.getKubernetesHosts() != null) {
                for (KubernetesHost kubernetesHost : kubernetesCluster.getKubernetesHosts()) {
                    if (kubernetesHost.getHostId().equals(hostId)) {
                        return kubernetesCluster;
                    }
                }
            }
        }
        throw new NonExistingKubernetesClusterException("Kubernetes cluster not found containing host id: " + hostId);
    }

    public KubernetesCluster[] getKubernetesClusters() {
        return kubernetesClustersMap.values().toArray(new KubernetesCluster[kubernetesClustersMap.size()]);
    }

    public boolean isClustered() {
        return clustered;
    }

    public boolean isCoordinator() {
        return coordinator;
    }

    public void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }

    public void persist() {
        if ((!isClustered()) || (isCoordinator())) {
            try {
                RegistryManager.getInstance().persist(CloudControllerConstants.DATA_RESOURCE, this);
            } catch (RegistryException e) {
                log.error("Could not persist cloud controller context in registry", e);
            }
        }
    }

    private void updateContextFromRegistry() {
        if ((!isClustered()) || (isCoordinator())) {
            try {
                Object dataObj = RegistryManager.getInstance().read(CloudControllerConstants.DATA_RESOURCE);
                if (dataObj != null) {
                    if (dataObj instanceof CloudControllerContext) {
                        CloudControllerContext serializedObj = (CloudControllerContext) dataObj;

                        copyMap(serializedObj.kubernetesClustersMap, kubernetesClustersMap);
                        copyMap(serializedObj.clusterIdToMemberContextListMap, clusterIdToMemberContextListMap);
                        copyMap(serializedObj.memberIdToMemberContextMap, memberIdToMemberContextMap);
                        copyMap(serializedObj.kubClusterIdToKubClusterContextMap, kubClusterIdToKubClusterContextMap);
                        copyMap(serializedObj.clusterIdToContextMap, clusterIdToContextMap);
                        copyMap(serializedObj.cartridgeTypeToPartitionIdsMap, cartridgeTypeToPartitionIdsMap);
                        copyMap(serializedObj.cartridgeTypeToCartridgeMap, cartridgeTypeToCartridgeMap);
                        copyMap(serializedObj.serviceGroupNameToServiceGroupMap, serviceGroupNameToServiceGroupMap);
                        copyMap(serializedObj.deploymentPolicyIDToDeployPolicyMap, deploymentPolicyIDToDeployPolicyMap);
                        copyMap(serializedObj.networkPartitionIDToNetworkPartitionMap, networkPartitionIDToNetworkPartitionMap);

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
            destinationMap.put(key, sourceMap.get(key));
        }
    }

    private void copyList(List sourceList, List destinationList) {
        for(Object item : sourceList) {
            destinationList.add(item);
        }
    }

    public Map<String, Map<String, IaasProvider>> getPartitionToIaasProviderByCartridge() {
        return partitionToIaasProviderByCartridge;
    }

    public void setPartitionToIaasProvider(Map<String, Map<String, IaasProvider>> partitionToIaasProviderByCartridge) {
        this.partitionToIaasProviderByCartridge = partitionToIaasProviderByCartridge;
    }

    public void addIaasProvider(String cartridgeType, String partitionId, IaasProvider iaasProvider) {
        Map<String, IaasProvider> partitionToIaasProvider = new ConcurrentHashMap<String, IaasProvider>();
        partitionToIaasProvider.put(partitionId, iaasProvider);
        this.partitionToIaasProviderByCartridge.put(cartridgeType, partitionToIaasProvider);
    }

    public void addIaasProviders(String cartridgeType, Map<String, IaasProvider> map) {
        Map<String, IaasProvider> partitionToIaasProvider = new ConcurrentHashMap<String, IaasProvider>();
        for (Iterator<String> iterator = map.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            IaasProvider value = map.get(key);

            partitionToIaasProvider.put(key, value);
            if (log.isDebugEnabled()) {
                log.debug("Partition map updated for the Cartridge: " + cartridgeType + ". "
                        + "Current Partition List: " + partitionToIaasProvider.keySet().toString());
            }
        }
        this.partitionToIaasProviderByCartridge.put(cartridgeType, partitionToIaasProvider);
    }

    public IaasProvider getIaasProviderOfPartition(String cartridgeType, String partitionId) {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving partition: " + partitionId + " for the Cartridge: " + this.hashCode() + ". "
                    + "Current Partition List: " + getPartitionToIaasProvider(cartridgeType).keySet().toString());
        }
        return getPartitionToIaasProvider(cartridgeType).get(partitionId);
    }

    public Map<String, IaasProvider> getPartitionToIaasProvider(String cartridgeType) {
        return this.partitionToIaasProviderByCartridge.get(cartridgeType);
    }

    public Map<String, List<IaasProvider>> getCartridgeTypeToIaasProvider() {
        return this.cartridgeTypeToIaasProvider;
    }

    public void setCartridgeTypeToIaasProvider(Map<String, List<IaasProvider>> cartridgeTypeToIaasProvider) {
        this.cartridgeTypeToIaasProvider = cartridgeTypeToIaasProvider;
    }

    public void addIaasProvider(String cartridgeType, IaasProvider iaas) {
        List<IaasProvider> iaasProviderList = new ArrayList<IaasProvider>();
        if (cartridgeTypeToIaasProvider.get(cartridgeType) != null) {
            iaasProviderList = cartridgeTypeToIaasProvider.get(cartridgeType);
        }

        //removing from a list using iterator
        if (iaasProviderList != null) {
            for (IaasProvider anIaas : iaasProviderList) {
                if (anIaas.equals(iaas)) {
                    int idx = iaasProviderList.indexOf(anIaas);
                    iaasProviderList.remove(idx);
                    iaasProviderList.add(idx, iaas);
                    return;
                }
            }
        }

        iaasProviderList.add(iaas);
        this.cartridgeTypeToIaasProvider.put(cartridgeType, iaasProviderList);
    }

    public IaasProvider getIaasProvider(String cartridgeType, String iaasType){
        List<IaasProvider> iaasProviderList = cartridgeTypeToIaasProvider.get(cartridgeType);

        if (iaasProviderList != null) {
            for (IaasProvider iaas : iaasProviderList) {
                if(iaas.getType().equals(iaasType)){
                    return iaas;
                }
            }
        }
        return null;
    }

    public List<IaasProvider> getIaases(String cartridgeType) {
        List<IaasProvider> iaasProviderList = cartridgeTypeToIaasProvider.get(cartridgeType);

        if (iaasProviderList != null) {
            return iaasProviderList;
        }
        return  null;
    }
}