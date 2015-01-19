/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.context.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ChildPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelNetworkPartition;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelPartition;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.Partition;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;

/*
 * It holds the runtime data of a VM cluster
 */
public class ClusterContext extends AbstractClusterContext {

    private static final long serialVersionUID = 17570842529682141L;

	private static final Log log = LogFactory.getLog(ClusterContext.class);

    // Map<NetworkpartitionId, Network Partition Context>
    protected Map<String, ClusterLevelNetworkPartitionContext> networkPartitionCtxts;

    protected DeploymentPolicy deploymentPolicy;
    protected AutoscalePolicy autoscalePolicy;

    public ClusterContext(String clusterId, String serviceId, AutoscalePolicy autoscalePolicy,
                          DeploymentPolicy deploymentPolicy, boolean hasScalingDependents) {

        super(clusterId, serviceId);
        this.deploymentPolicy = deploymentPolicy;
        this.networkPartitionCtxts = new ConcurrentHashMap<String, ClusterLevelNetworkPartitionContext>();
        this.autoscalePolicy = autoscalePolicy;

    }

    public Map<String, ClusterLevelNetworkPartitionContext> getNetworkPartitionCtxts() {
        return networkPartitionCtxts;
    }

    public DeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public AutoscalePolicy getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public void setAutoscalePolicy(AutoscalePolicy autoscalePolicy) {
        this.autoscalePolicy = autoscalePolicy;
    }

    public ClusterLevelNetworkPartitionContext getNetworkPartitionCtxt(String networkPartitionId) {
        return networkPartitionCtxts.get(networkPartitionId);
    }

    public void setPartitionCtxt(Map<String, ClusterLevelNetworkPartitionContext> partitionCtxt) {
        this.networkPartitionCtxts = partitionCtxt;
    }

    public boolean partitionCtxtAvailable(String partitionId) {
        return networkPartitionCtxts.containsKey(partitionId);
    }

    public void addNetworkPartitionCtxt(ClusterLevelNetworkPartitionContext ctxt) {
        this.networkPartitionCtxts.put(ctxt.getId(), ctxt);
    }

    public ClusterLevelNetworkPartitionContext getPartitionCtxt(String id) {
        return this.networkPartitionCtxts.get(id);
    }

    public ClusterLevelNetworkPartitionContext getNetworkPartitionCtxt(Member member) {
        log.info("***** getNetworkPartitionCtxt " + member.getNetworkPartitionId());
        String networkPartitionId = member.getNetworkPartitionId();
        if (networkPartitionCtxts.containsKey(networkPartitionId)) {
            log.info("returnnig network partition context " + networkPartitionCtxts.get(networkPartitionId));
            return networkPartitionCtxts.get(networkPartitionId);
        }

        log.info("returning null getNetworkPartitionCtxt");
        return null;
    }

    public void addInstanceContext(String instanceId, Cluster cluster, boolean hasScalingDependents,
                                   boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {
        ClusterLevelNetworkPartitionContext networkPartitionContext = null;
        ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
        ChildPolicy policy = this.deploymentPolicy.
                getChildPolicy(AutoscalerUtil.getAliasFromClusterId(clusterId));
        if (networkPartitionCtxts.containsKey(clusterInstance.getNetworkPartitionId())) {
            networkPartitionContext = this.networkPartitionCtxts.get(
                    clusterInstance.getNetworkPartitionId());
        } else {
            if (policy != null) {
                ChildLevelNetworkPartition networkPartition = policy.
                        getChildLevelNetworkPartition(clusterInstance.getNetworkPartitionId());
                networkPartitionContext = new ClusterLevelNetworkPartitionContext(networkPartition.getId(),
                        networkPartition.getPartitionAlgo(), 0);
            } else {
                //Parent should have the partition specified
                networkPartitionContext = new ClusterLevelNetworkPartitionContext(
                        clusterInstance.getNetworkPartitionId());
            }

        }

        if (clusterInstance.getPartitionId() != null) {
            //Need to add partition Context based on the given one from the parent
            networkPartitionContext = addPartition(clusterInstance, cluster,
                    networkPartitionContext, null, hasScalingDependents, groupScalingEnabledSubtree);
        } else {
            networkPartitionContext = parseDeploymentPolicy(clusterInstance, cluster,
                    policy, networkPartitionContext, hasScalingDependents, groupScalingEnabledSubtree);
        }
        if (!networkPartitionCtxts.containsKey(clusterInstance.getNetworkPartitionId())) {
            this.networkPartitionCtxts.put(clusterInstance.getNetworkPartitionId(),
                    networkPartitionContext);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster instance context has been added to network partition, [cluster instance]" +
                                " %s [network partition] %s", clusterInstance.getInstanceId(),
                        clusterInstance.getNetworkPartitionId()));
            }
        }

    }

    private ClusterLevelNetworkPartitionContext parseDeploymentPolicy(
            ClusterInstance clusterInstance,
            Cluster cluster,
            ChildPolicy childPolicy,
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext,
            boolean hasGroupScalingDependent, boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {

        if (childPolicy == null) {
            String msg = "Deployment policy is null";
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        if (log.isDebugEnabled()) {
            log.debug("Child policy alias: " + childPolicy.getAlias());
        }

        ChildLevelPartition[] childLevelPartitions = childPolicy.
                getChildLevelNetworkPartition(
                        clusterLevelNetworkPartitionContext.getId()).
                getChildLevelPartitions();
        if (childLevelPartitions == null) {
            String msg = "Partitions are null in child policy: [alias]: " +
                            childPolicy.getAlias();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        //Retrieving the ChildLevelNetworkPartition and create NP Context
        ChildLevelNetworkPartition networkPartition;
        networkPartition = childPolicy.
                getChildLevelNetworkPartition(clusterInstance.getNetworkPartitionId());
        
        //Fill cluster instance context with child level partitions
        for (ChildLevelPartition childLevelPartition : networkPartition.getChildLevelPartitions()) {
            addPartition(clusterInstance, cluster, clusterLevelNetworkPartitionContext, childLevelPartition,
                    hasGroupScalingDependent, groupScalingEnabledSubtree);
        }
        return clusterLevelNetworkPartitionContext;
    }

    private ClusterLevelNetworkPartitionContext addPartition(
            ClusterInstance clusterInstance,
            Cluster cluster,
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext,
            ChildLevelPartition childLevelPartition,
            boolean hasScalingDependents, boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {
        if (clusterLevelNetworkPartitionContext == null) {
            String msg =
                    "Network Partition is null in deployment policy: [application-id]: " +
                            deploymentPolicy.getApplicationId();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        String nPartitionId = clusterLevelNetworkPartitionContext.getId();

        //Getting the associated  partition
        if (clusterInstance.getPartitionId() == null && childLevelPartition == null) {
            String msg =
                    "[Partition] " + clusterInstance.getPartitionId() + " for [networkPartition] " +
                            clusterInstance.getNetworkPartitionId() + "is null " +
                            "in deployment policy: [application-id]: " + deploymentPolicy.getApplicationId();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        ClusterInstanceContext clusterInstanceContext = (ClusterInstanceContext) clusterLevelNetworkPartitionContext.
                getInstanceContext(clusterInstance.getInstanceId());
        int maxInstances = 1;
        if (clusterInstanceContext == null) {
            int minInstances = 1;
            try {
                ApplicationHolder.acquireReadLock();
                Application application = ApplicationHolder.getApplications().
                        getApplication(cluster.getAppId());
                ClusterDataHolder dataHolder = application.
                        getClusterDataHolderRecursivelyByAlias(
                                AutoscalerUtil.getAliasFromClusterId(clusterId));
                minInstances = dataHolder.getMinInstances();
                maxInstances = dataHolder.getMaxInstances();
            } finally {
                ApplicationHolder.releaseReadLock();
            }
            clusterInstanceContext = new ClusterInstanceContext(clusterInstance.getInstanceId(),
                    clusterLevelNetworkPartitionContext.getPartitionAlgorithm(),
                    minInstances, maxInstances, nPartitionId, clusterId, hasScalingDependents, groupScalingEnabledSubtree);
        }
        String partitionId;
        if (childLevelPartition != null) {
            //use it own defined partition
            partitionId = childLevelPartition.getPartitionId();
            maxInstances = childLevelPartition.getMax();
        } else {
            //handling the partition given by the parent
            partitionId = clusterInstance.getPartitionId();
        }
        //Retrieving the actual partition from application
        Partition appPartition = deploymentPolicy.getApplicationLevelNetworkPartition(nPartitionId).
                getPartition(partitionId);
        org.apache.stratos.cloud.controller.stub.domain.Partition partition =
                convertTOCCPartition(appPartition);

        //Validate the partition
        //TODO validate partition removal
        //CloudControllerClient.getInstance().validatePartition(partition);

        //Creating cluster level partition context
        ClusterLevelPartitionContext clusterLevelPartitionContext = new ClusterLevelPartitionContext(
                maxInstances,
                partition,
                clusterInstance.getNetworkPartitionId(), clusterId);
        clusterLevelPartitionContext.setServiceName(cluster.getServiceName());
        clusterLevelPartitionContext.setProperties(cluster.getProperties());

        //add members to partition Context
        addMembersFromTopology(cluster, partition, clusterLevelPartitionContext,
                clusterInstanceContext.getId());

        //adding it to the monitors context
        clusterInstanceContext.addPartitionCtxt(clusterLevelPartitionContext);
        if (log.isInfoEnabled()) {
            log.info(String.format("Partition context has been added: [partition] %s",
                    clusterLevelPartitionContext.getPartitionId()));
        }

        clusterLevelNetworkPartitionContext.addInstanceContext(clusterInstanceContext);

        if (log.isInfoEnabled()) {
            log.info(String.format("Cluster Instance context has been added: " +
                    "[ClusterInstanceContext] %s", clusterInstanceContext.getId()));
        }


        return clusterLevelNetworkPartitionContext;
    }

    private void addMembersFromTopology(Cluster cluster,
                                        org.apache.stratos.cloud.controller.stub.domain.Partition partition,
                                        ClusterLevelPartitionContext clusterLevelPartitionContext,
                                        String ClusterInstanceId) {
        for (Member member : cluster.getMembers()) {
            String memberId = member.getMemberId();
            if (member.getPartitionId().equalsIgnoreCase(partition.getId()) &&
                    member.getClusterInstanceId().equals(ClusterInstanceId)) {
                MemberContext memberContext = new MemberContext();
                memberContext.setClusterId(member.getClusterId());
                memberContext.setMemberId(memberId);
                memberContext.setInitTime(member.getInitTime());
                memberContext.setPartition(partition);
                memberContext.setProperties(AutoscalerUtil.toStubProperties(member.getProperties()));

                if (MemberStatus.Active.equals(member.getStatus())) {
                    clusterLevelPartitionContext.addActiveMember(memberContext);
                    if (log.isDebugEnabled()) {
                        String msg = String.format("Active member read from topology and added to active member list: %s", member.toString());
                        log.debug(msg);
                    }
                } else if (MemberStatus.Created.equals(member.getStatus()) || MemberStatus.Starting.equals(member.getStatus())) {
                    clusterLevelPartitionContext.addPendingMember(memberContext);
                    if (log.isDebugEnabled()) {
                        String msg = String.format("Pending member read from topology and added to pending member list: %s", member.toString());
                        log.debug(msg);
                    }
                }
                clusterLevelPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                if (log.isInfoEnabled()) {
                    log.info(String.format("Member stat context has been added: [member-id] %s", memberId));
                }
            }
        }
    }

    private org.apache.stratos.cloud.controller.stub.domain.Partition convertTOCCPartition(Partition partition) {
        org.apache.stratos.cloud.controller.stub.domain.Partition ccPartition = new
                org.apache.stratos.cloud.controller.stub.domain.Partition();

        ccPartition.setId(partition.getId());
        ccPartition.setProvider(partition.getProvider());
        ccPartition.setDescription(partition.getDescription());
        ccPartition.setKubernetesClusterId(partition.getKubernetesClusterId());
        ccPartition.setProperties(AutoscalerUtil.toStubProperties(partition.getProperties()));

        return ccPartition;
    }
}
