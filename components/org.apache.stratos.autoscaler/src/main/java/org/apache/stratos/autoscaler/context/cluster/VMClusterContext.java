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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.ChildLevelPartition;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelNetworkPartition;
import org.apache.stratos.cloud.controller.stub.domain.*;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;

import java.util.HashMap;
import java.util.Map;

/*
 * It holds the runtime data of a VM cluster
 */
public class VMClusterContext extends AbstractClusterContext {

    private static final Log log = LogFactory.getLog(VMClusterContext.class);

    // Map<NetworkpartitionId, Network Partition Context>
    protected Map<String, ClusterLevelNetworkPartitionContext> networkPartitionCtxts;
    protected DeploymentPolicy deploymentPolicy;
    protected AutoscalePolicy autoscalePolicy;

    public VMClusterContext(String clusterId, String serviceId, AutoscalePolicy autoscalePolicy,
                            DeploymentPolicy deploymentPolicy) {

        super(clusterId, serviceId);
        this.deploymentPolicy = deploymentPolicy;
        this.networkPartitionCtxts = new HashMap<String, ClusterLevelNetworkPartitionContext>();
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

    public void addInstanceContext(String instanceId, Cluster cluster)
            throws PolicyValidationException, PartitionValidationException {
        ClusterLevelNetworkPartitionContext networkPartitionContext = null;
        ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
        if (networkPartitionCtxts.containsKey(clusterInstance.getNetworkPartitionId())) {
            networkPartitionContext = this.networkPartitionCtxts.get(
                    clusterInstance.getNetworkPartitionId());
        }
        if (clusterInstance.getPartitionId() != null) {
            //Need to add partition Context based on the given one from the parent
            networkPartitionContext = addPartition(clusterInstance, cluster,
                    this.deploymentPolicy, networkPartitionContext);

        } else {
            networkPartitionContext = parseDeploymentPolicy(clusterInstance, cluster,
                    this.deploymentPolicy, networkPartitionContext);
        }
        if (!networkPartitionCtxts.containsKey(clusterInstance.getNetworkPartitionId())) {
            this.networkPartitionCtxts.put(clusterInstance.getNetworkPartitionId(),
                    networkPartitionContext);
            if (log.isInfoEnabled()) {
                log.info(String.format("Network partition context has been added: " +
                        "[network partition] %s", clusterInstance.getNetworkPartitionId()));
            }
        }

    }

    private ClusterLevelNetworkPartitionContext parseDeploymentPolicy(
            ClusterInstance instance,
            Cluster cluster,
            DeploymentPolicy deploymentPolicy,
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext)
            throws PolicyValidationException, PartitionValidationException {
        if (log.isDebugEnabled()) {
            log.debug("Deployment policy name: " + deploymentPolicy.getId());
        }

        if (deploymentPolicy == null) {
            String msg = "Deployment policy is null: [policy-name] " + deploymentPolicy.getId();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        Partition[] allPartitions = deploymentPolicy.getAllPartitions();
        if (allPartitions == null) {
            String msg =
                    "Partitions are null in deployment policy: [policy-name]: " +
                            deploymentPolicy.getId();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        CloudControllerClient.getInstance().validateDeploymentPolicy(cluster.getServiceName(),
                deploymentPolicy);

        ChildLevelNetworkPartition networkPartition;
        networkPartition = deploymentPolicy.getChildLevelNetworkPartition(instance.getNetworkPartitionId());
        String networkPartitionId = networkPartition.getId();

        if (clusterLevelNetworkPartitionContext == null) {
            clusterLevelNetworkPartitionContext = new ClusterLevelNetworkPartitionContext(
                    networkPartitionId);
        }
        ClusterInstanceContext clusterInstanceContext = clusterLevelNetworkPartitionContext.
                getClusterInstanceContext(instance.getInstanceId());
        if (clusterInstanceContext == null) {
            clusterInstanceContext = new ClusterInstanceContext(instance.getInstanceId(),
                    networkPartition.getPartitionAlgo(),
                    networkPartition.getChildLevelPartitions());
        }

        for (ChildLevelPartition partition : networkPartition.getChildLevelPartitions()) {
            //FIXME to have correct member expirery time
            ClusterLevelPartitionContext clusterLevelPartitionContext =
                    new ClusterLevelPartitionContext(0);
            clusterLevelPartitionContext.setServiceName(cluster.getServiceName());
            clusterLevelPartitionContext.setProperties(cluster.getProperties());
            clusterLevelPartitionContext.setNetworkPartitionId(networkPartition.getId());
            //add members to partition Context
            Partition partition1 = deploymentPolicy.
                    getApplicationLevelNetworkPartition(networkPartitionId).getPartition(partition.getPartitionId());

            addMembersFromTopology(cluster, partition1, clusterLevelPartitionContext);

            clusterInstanceContext.addPartitionCtxt(clusterLevelPartitionContext);
            if (log.isInfoEnabled()) {
                log.info(String.format("Partition context has been added: [partition] %s",
                        clusterLevelPartitionContext.getPartitionId()));
            }
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Network partition context has been added: " +
                    "[network partition] %s", clusterLevelNetworkPartitionContext.getId()));
        }
        return clusterLevelNetworkPartitionContext;
    }

    private ClusterLevelNetworkPartitionContext addPartition(
            ClusterInstance clusterInstance,
            Cluster cluster,
            DeploymentPolicy deploymentPolicy,
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext)
            throws PolicyValidationException, PartitionValidationException {

        ChildLevelNetworkPartition networkPartition = deploymentPolicy.
                getChildLevelNetworkPartition(clusterInstance.getNetworkPartitionId());
        if (networkPartition == null) {
            String msg =
                    "Network Partition is null in deployment policy: [policy-name]: " +
                            deploymentPolicy.getId();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }
        ChildLevelPartition partition = networkPartition.getChildLevelPartition(clusterInstance.getPartitionId());
        if (partition == null) {
            String msg =
                    "[Partition] " + clusterInstance.getPartitionId() + " for [networkPartition] " +
                            clusterInstance.getNetworkPartitionId() + "is null " +
                            "in deployment policy: [policy-name]: " + deploymentPolicy.getId();
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        Partition partition1 = deploymentPolicy.getApplicationLevelNetworkPartition(networkPartition.getId()).
                getPartition(partition.getPartitionId());
        CloudControllerClient.getInstance().validatePartition(partition1);
        if (clusterLevelNetworkPartitionContext == null) {
            clusterLevelNetworkPartitionContext =
                    new ClusterLevelNetworkPartitionContext(clusterInstance.getNetworkPartitionId());
        }
        //FIXME to have correct member expiry time
        ClusterLevelPartitionContext clusterLevelPartitionContext =
                new ClusterLevelPartitionContext(0);
        clusterLevelPartitionContext.setServiceName(cluster.getServiceName());
        clusterLevelPartitionContext.setProperties(cluster.getProperties());
        clusterLevelPartitionContext.setNetworkPartitionId(networkPartition.getId());
        //add members to partition Context
        addMembersFromTopology(cluster, partition1, clusterLevelPartitionContext);

        ClusterInstanceContext clusterInstanceContext = clusterLevelNetworkPartitionContext.
                getClusterInstanceContext(clusterInstance.getInstanceId());
        if (clusterInstanceContext == null) {
            clusterInstanceContext = new ClusterInstanceContext(clusterInstance.getInstanceId(),
                    networkPartition.getPartitionAlgo(),
                    networkPartition.getChildLevelPartitions());
        }
        clusterInstanceContext.addPartitionCtxt(clusterLevelPartitionContext);
        clusterLevelNetworkPartitionContext.addClusterInstanceContext(clusterInstanceContext);

        if (log.isInfoEnabled()) {
            log.info(String.format("Partition context has been added: [partition] %s",
                    clusterLevelPartitionContext.getPartitionId()));
        }


        return clusterLevelNetworkPartitionContext;
    }

    private void addMembersFromTopology(Cluster cluster, Partition partition,
                                        ClusterLevelPartitionContext clusterLevelPartitionContext) {
        for (Member member : cluster.getMembers()) {
            String memberId = member.getMemberId();
            if (member.getPartitionId().equalsIgnoreCase(partition.getId())) {
                MemberContext memberContext = new MemberContext();
                memberContext.setClusterId(member.getClusterId());
                memberContext.setMemberId(memberId);
                memberContext.setInitTime(member.getInitTime());
                memberContext.setPartition(partition);
                //FIXME********memberContext.setProperties(convertMemberPropsToMemberContextProps(member.getProperties()));

                if (MemberStatus.Activated.equals(member.getStatus())) {
                    if (log.isDebugEnabled()) {
                        String msg = String.format("Active member loaded from topology and added to active member list, %s", member.toString());
                        log.debug(msg);
                    }
                    clusterLevelPartitionContext.addActiveMember(memberContext);
//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
//                            partitionContext.incrementCurrentActiveMemberCount(1);

                } else if (MemberStatus.Created.equals(member.getStatus()) || MemberStatus.Starting.equals(member.getStatus())) {
                    if (log.isDebugEnabled()) {
                        String msg = String.format("Pending member loaded from topology and added to pending member list, %s", member.toString());
                        log.debug(msg);
                    }
                    clusterLevelPartitionContext.addPendingMember(memberContext);

//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
                } else if (MemberStatus.Suspended.equals(member.getStatus())) {
//                            partitionContext.addFaultyMember(memberId);
                }
                clusterLevelPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                if (log.isInfoEnabled()) {
                    log.info(String.format("Member stat context has been added: [member] %s", memberId));
                }
            }

        }
    }

    //FIXME**********
    /*private org.apache.stratos.cloud.controller.stub.pojo.Properties convertMemberPropsToMemberContextProps(
            java.util.Properties properties) {
        org.apache.stratos.cloud.controller.stub.pojo.Properties props = new org.apache.stratos.cloud.controller.stub.pojo.Properties();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Property property = new Property();
            property.setName((String) e.getKey());
            property.setValue((String) e.getValue());
            props.addProperties(property);
        }
        return props;
    }*/


}
