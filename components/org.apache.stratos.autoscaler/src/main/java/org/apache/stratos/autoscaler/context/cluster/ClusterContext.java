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
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.cloud.controller.stub.domain.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.cloud.controller.stub.domain.NetworkPartitionRef;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.cloud.controller.stub.domain.PartitionRef;
import org.apache.stratos.common.client.CloudControllerServiceClient;
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

    protected AutoscalePolicy autoscalePolicy;

    public ClusterContext(String clusterId, String serviceId, AutoscalePolicy autoscalePolicy,
                          boolean hasScalingDependents) {

        super(clusterId, serviceId);
        this.networkPartitionCtxts = new ConcurrentHashMap<String, ClusterLevelNetworkPartitionContext>();
        this.autoscalePolicy = autoscalePolicy;
    }

    public Map<String, ClusterLevelNetworkPartitionContext> getNetworkPartitionCtxts() {
        return networkPartitionCtxts;
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

        String networkPartitionId = member.getNetworkPartitionId();
        if (networkPartitionCtxts.containsKey(networkPartitionId)) {

            return networkPartitionCtxts.get(networkPartitionId);
        }
        return null;
    }

    public void addInstanceContext(String instanceId, Cluster cluster, boolean hasScalingDependents,
                                   boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {
        ClusterLevelNetworkPartitionContext networkPartitionContext = null;
        ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
        
        String deploymentPolicyName = AutoscalerUtil.getDeploymentPolicyIdByAlias(cluster.getAppId(), AutoscalerUtil.getAliasFromClusterId(clusterId));
   	 	DeploymentPolicy deploymentPolicy;
   	 	try {
   	 		deploymentPolicy = CloudControllerServiceClient.getInstance().getDeploymentPolicy(deploymentPolicyName);
   	 	} catch (Exception e) {
   	 		String msg = String.format("Error while getting deployment policy from cloud controller [deployment-policy-id] %s", deploymentPolicyName);
   	 		log.error(msg, e);
   	 		throw new AutoScalerException(msg, e);
   	 	}
        
        if (networkPartitionCtxts.containsKey(clusterInstance.getNetworkPartitionId())) {
            networkPartitionContext = this.networkPartitionCtxts.get(
                    clusterInstance.getNetworkPartitionId());
        } else {
        	
        	NetworkPartitionRef[] networkPartitionRefs = deploymentPolicy.getNetworkPartitionsRef();
        	NetworkPartitionRef networkPartitionRef = null;
        	if (networkPartitionRefs != null && networkPartitionRefs.length != 0) {
				for (NetworkPartitionRef i : networkPartitionRefs) {
					if (i.getId().equals(clusterInstance.getNetworkPartitionId())) {
						networkPartitionRef = i;
					}
				}
			}
        	
        	if (networkPartitionRef == null) {
        		 //Parent should have the partition specified
        		networkPartitionContext = new ClusterLevelNetworkPartitionContext(clusterInstance.getNetworkPartitionId());
			} else {
				networkPartitionContext = new ClusterLevelNetworkPartitionContext(networkPartitionRef.getId(),
						networkPartitionRef.getPartitionAlgo(), 0);
			}
        }

        if (clusterInstance.getPartitionId() != null) {
            //Need to add partition Context based on the given one from the parent
            networkPartitionContext = addPartition(clusterInstance, cluster,
                    networkPartitionContext, null, hasScalingDependents, groupScalingEnabledSubtree);
        } else {
            networkPartitionContext = parseDeploymentPolicy(clusterInstance, cluster,
                    networkPartitionContext, hasScalingDependents, groupScalingEnabledSubtree);
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
			ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext,
			boolean hasGroupScalingDependent, boolean groupScalingEnabledSubtree)
			throws PolicyValidationException, PartitionValidationException {

		String deploymentPolicyName = AutoscalerUtil.getDeploymentPolicyIdByAlias(cluster.getAppId(), AutoscalerUtil.getAliasFromClusterId(clusterId));
		DeploymentPolicy deploymentPolicy;
		try {
			deploymentPolicy = CloudControllerServiceClient.getInstance().getDeploymentPolicy(deploymentPolicyName);
		} catch (Exception e) {
			String msg = String
					.format("Error while getting deployment policy from cloud controller [deployment-policy-id] %s",
							deploymentPolicyName);
			log.error(msg, e);
			throw new AutoScalerException(msg, e);
		}

		NetworkPartitionRef[] networkPartitionRefs = deploymentPolicy
				.getNetworkPartitionsRef();
		PartitionRef[] partitionRefs = null;
		if (networkPartitionRefs != null && networkPartitionRefs.length != 0) {
			for (NetworkPartitionRef networkPartitionRef : networkPartitionRefs) {
				if (networkPartitionRef.getId().equals(
						clusterLevelNetworkPartitionContext.getId())) {
					partitionRefs = networkPartitionRef.getPartitions();
				}
			}
		}

		if (partitionRefs == null) {
			String msg = "PartitionRefs are null in deployment policy for [cluster-alias] "
					+ AutoscalerUtil.getAliasFromClusterId(clusterId);
			log.error(msg);
			throw new PolicyValidationException(msg);
		}

		// Retrieving the ChildLevelNetworkPartition and create NP Context
		NetworkPartitionRef networkPartitionRef = null;
		if (networkPartitionRefs != null && networkPartitionRefs.length != 0) {
			for (NetworkPartitionRef networkPartitionRef2 : networkPartitionRefs) {
				if (networkPartitionRef2.getId().equals(
						clusterInstance.getNetworkPartitionId())) {
					networkPartitionRef = networkPartitionRef2;
				}
			}
		}

		// Fill cluster instance context with child level partitions
		if (networkPartitionRef != null) {
			for (PartitionRef partitionRef : networkPartitionRef
					.getPartitions()) {
				addPartition(clusterInstance, cluster,
						clusterLevelNetworkPartitionContext, partitionRef,
						hasGroupScalingDependent, groupScalingEnabledSubtree);
			}
		}

		return clusterLevelNetworkPartitionContext;
	}

    private ClusterLevelNetworkPartitionContext addPartition(
            ClusterInstance clusterInstance,
            Cluster cluster,
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext,
            PartitionRef partitionRef,
            boolean hasScalingDependents, boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {
    	
        if (clusterLevelNetworkPartitionContext == null) {
            String msg = "Network Partition is null in deployment policy : [cluster-alias]: " + clusterInstance.getAlias();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        String nPartitionId = clusterLevelNetworkPartitionContext.getId();

        //Getting the associated  partition
        if (clusterInstance.getPartitionId() == null && partitionRef == null) {
            String msg = "[Partition] " + clusterInstance.getPartitionId() + " for [networkPartition] " +
                            clusterInstance.getNetworkPartitionId() + "is null " +
                            "in deployment policy: [cluster-alias]: " + clusterInstance.getAlias();
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
        if (partitionRef != null) {
            //use it own defined partition
            partitionId = partitionRef.getId();
            maxInstances = partitionRef.getMax();
        } else {
            //handling the partition given by the parent
            partitionId = clusterInstance.getPartitionId();
        }
        
        //Retrieving the actual partition from application
        Partition[] partitions = null;
		try {
			partitions = CloudControllerServiceClient.getInstance().getNetworkPartition(nPartitionId).getPartitions();
		} catch (Exception e) {
			String msg = String.format("Error while getting network partitioin from cloud controller : [network-partition-id] %s", nPartitionId);
			log.error(msg, e);
			throw new AutoScalerException(msg, e);
		}
		
        Partition partition = null;
        if (partitions != null && partitions.length != 0) {
			for (Partition partition2 : partitions) {
				if (partition2.getId().equals(partitionId)) {
					partition = partition2;
				}
			}
		}

        //Creating cluster level partition context
        ClusterLevelPartitionContext clusterLevelPartitionContext = new ClusterLevelPartitionContext(
                maxInstances,
                partition,
                clusterInstance.getNetworkPartitionId());
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
}
