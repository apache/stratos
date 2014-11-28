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

package org.apache.stratos.autoscaler.context.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.partition.NetworkPartition;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClusterContextFactory {

    private static final Log log = LogFactory.getLog(ClusterContextFactory.class);

    public static VMServiceClusterContext getVMServiceClusterContext (Cluster cluster) throws PolicyValidationException, PartitionValidationException {

        if (null == cluster) {
            return null;
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        String deploymentPolicyName = cluster.getDeploymentPolicyName();

        if (log.isDebugEnabled()) {
            log.debug("Deployment policy name: " + deploymentPolicyName);
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy autoscalePolicy =
                PolicyManager.getInstance()
                        .getAutoscalePolicy(autoscalePolicyName);
        DeploymentPolicy deploymentPolicy =
                PolicyManager.getInstance()
                        .getDeploymentPolicy(deploymentPolicyName);

        if (deploymentPolicy == null) {
            String msg = "Deployment policy is null: [policy-name] " + deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        Partition[] allPartitions = deploymentPolicy.getAllPartitions();
        if (allPartitions == null) {
            String msg =
                    "Partitions are null in deployment policy: [policy-name]: " +
                            deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        CloudControllerClient.getInstance().validateDeploymentPolicy(cluster.getServiceName(), deploymentPolicy);

        Map<String, ClusterLevelNetworkPartitionContext> networkPartitionContextMap = new HashMap<String, ClusterLevelNetworkPartitionContext>();

        for (NetworkPartition networkPartition : deploymentPolicy.getNetworkPartitions()) {

            String networkPartitionId = networkPartition.getId();
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext = new ClusterLevelNetworkPartitionContext(networkPartitionId,
                    networkPartition.getPartitionAlgo(),
                    networkPartition.getPartitions());

            for (Partition partition : networkPartition.getPartitions()) {
                ClusterLevelPartitionContext clusterMonitorPartitionContext = new ClusterLevelPartitionContext(partition);
                clusterMonitorPartitionContext.setServiceName(cluster.getServiceName());
                clusterMonitorPartitionContext.setProperties(cluster.getProperties());
                clusterMonitorPartitionContext.setNetworkPartitionId(networkPartition.getId());

                for (Member member : cluster.getMembers()) {
                    String memberId = member.getMemberId();
                    if (member.getPartitionId().equalsIgnoreCase(partition.getId())) {
                        MemberContext memberContext = new MemberContext();
                        memberContext.setClusterId(member.getClusterId());
                        memberContext.setMemberId(memberId);
                        memberContext.setInitTime(member.getInitTime());
                        memberContext.setPartition(partition);
                        memberContext.setProperties(convertMemberPropsToMemberContextProps(member.getProperties()));

                        if (MemberStatus.Activated.equals(member.getStatus())) {
                            if (log.isDebugEnabled()) {
                                String msg = String.format("Active member loaded from topology and added to active member list, %s", member.toString());
                                log.debug(msg);
                            }
                            clusterMonitorPartitionContext.addActiveMember(memberContext);
//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
//                            partitionContext.incrementCurrentActiveMemberCount(1);

                        } else if (MemberStatus.Created.equals(member.getStatus()) || MemberStatus.Starting.equals(member.getStatus())) {
                            if (log.isDebugEnabled()) {
                                String msg = String.format("Pending member loaded from topology and added to pending member list, %s", member.toString());
                                log.debug(msg);
                            }
                            clusterMonitorPartitionContext.addPendingMember(memberContext);

//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
                        } else if (MemberStatus.Suspended.equals(member.getStatus())) {
//                            partitionContext.addFaultyMember(memberId);
                        }
                        clusterMonitorPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member stat context has been added: [member] %s", memberId));
                        }
                    }

                }
                clusterLevelNetworkPartitionContext.addPartitionContext(clusterMonitorPartitionContext);
                if (log.isInfoEnabled()) {
                    log.info(String.format("Partition context has been added: [partition] %s",
                            clusterMonitorPartitionContext.getPartitionId()));
                }
            }

            networkPartitionContextMap.put(networkPartitionId, clusterLevelNetworkPartitionContext);
            if (log.isInfoEnabled()) {
                log.info(String.format("Network partition context has been added: [network partition] %s",
                        clusterLevelNetworkPartitionContext.getId()));
            }
        }

        return new VMServiceClusterContext(cluster.getClusterId(), cluster.getServiceName(), autoscalePolicy,
                        deploymentPolicy, networkPartitionContextMap);
    }

    public static VMClusterContext getVMLBClusterContext (Cluster cluster) throws PolicyValidationException {

        // FIXME fix the following code to correctly update
        // AutoscalerContext context = AutoscalerContext.getInstance();
        if (null == cluster) {
            return null;
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        String deploymentPolicyName = cluster.getDeploymentPolicyName();

        if (log.isDebugEnabled()) {
            log.debug("Deployment policy name: " + deploymentPolicyName);
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy autoscalePolicy =
                PolicyManager.getInstance()
                        .getAutoscalePolicy(autoscalePolicyName);
        DeploymentPolicy deploymentPolicy =
                PolicyManager.getInstance()
                        .getDeploymentPolicy(deploymentPolicyName);

        if (deploymentPolicy == null) {
            String msg = "Deployment Policy is null. Policy name: " + deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        String clusterId = cluster.getClusterId();

        Map<String, ClusterLevelNetworkPartitionContext> networkPartitionContextMap = new HashMap<String, ClusterLevelNetworkPartitionContext>();

        // partition group = network partition context
        for (NetworkPartition networkPartition : deploymentPolicy.getNetworkPartitions()) {

            String networkPartitionId = networkPartition.getId();
            NetworkPartitionLbHolder networkPartitionLbHolder =
                    PartitionManager.getInstance()
                            .getNetworkPartitionLbHolder(networkPartitionId);
//                                                              PartitionManager.getInstance()
//                                                                              .getNetworkPartitionLbHolder(partitionGroup.getPartitionId());
            // FIXME pick a random partition
            Partition partition =
                    networkPartition.getPartitions()[new Random().nextInt(networkPartition.getPartitions().length)];
            ClusterLevelPartitionContext clusterMonitorPartitionContext = new ClusterLevelPartitionContext(partition);
            clusterMonitorPartitionContext.setServiceName(cluster.getServiceName());
            clusterMonitorPartitionContext.setProperties(cluster.getProperties());
            clusterMonitorPartitionContext.setNetworkPartitionId(networkPartitionId);
            clusterMonitorPartitionContext.setMinimumMemberCount(1);//Here it hard codes the minimum value as one for LB cartridge partitions

            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext = new ClusterLevelNetworkPartitionContext(networkPartitionId,
                    networkPartition.getPartitionAlgo(),
                    networkPartition.getPartitions());
            for (Member member : cluster.getMembers()) {
                String memberId = member.getMemberId();
                if (member.getNetworkPartitionId().equalsIgnoreCase(clusterLevelNetworkPartitionContext.getId())) {
                    MemberContext memberContext = new MemberContext();
                    memberContext.setClusterId(member.getClusterId());
                    memberContext.setMemberId(memberId);
                    memberContext.setPartition(partition);
                    memberContext.setInitTime(member.getInitTime());

                    if (MemberStatus.Activated.equals(member.getStatus())) {
                        if (log.isDebugEnabled()) {
                            String msg = String.format("Active member loaded from topology and added to active member list, %s", member.toString());
                            log.debug(msg);
                        }
                        clusterMonitorPartitionContext.addActiveMember(memberContext);
//                        networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
//                        partitionContext.incrementCurrentActiveMemberCount(1);
                    } else if (MemberStatus.Created.equals(member.getStatus()) ||
                            MemberStatus.Starting.equals(member.getStatus())) {
                        if (log.isDebugEnabled()) {
                            String msg = String.format("Pending member loaded from topology and added to pending member list, %s", member.toString());
                            log.debug(msg);
                        }
                        clusterMonitorPartitionContext.addPendingMember(memberContext);
//                        networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
                    } else if (MemberStatus.Suspended.equals(member.getStatus())) {
//                        partitionContext.addFaultyMember(memberId);
                    }

                    clusterMonitorPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Member stat context has been added: [member] %s", memberId));
                    }
                }

            }
            clusterLevelNetworkPartitionContext.addPartitionContext(clusterMonitorPartitionContext);

            // populate lb cluster id in network partition context.
            java.util.Properties props = cluster.getProperties();

            // get service type of load balanced cluster
            String loadBalancedServiceType = props.getProperty(StratosConstants.LOAD_BALANCED_SERVICE_TYPE);

            if (props.containsKey(StratosConstants.LOAD_BALANCER_REF)) {
                String value = props.getProperty(StratosConstants.LOAD_BALANCER_REF);

                if (value.equals(StratosConstants.DEFAULT_LOAD_BALANCER)) {
                    networkPartitionLbHolder.setDefaultLbClusterId(clusterId);

                } else if (value.equals(StratosConstants.SERVICE_AWARE_LOAD_BALANCER)) {
                    String serviceName = cluster.getServiceName();
                    // TODO: check if this is correct
                    networkPartitionLbHolder.addServiceLB(serviceName, clusterId);

                    if (loadBalancedServiceType != null && !loadBalancedServiceType.isEmpty()) {
                        networkPartitionLbHolder.addServiceLB(loadBalancedServiceType, clusterId);
                        if (log.isDebugEnabled()) {
                            log.debug("Added cluster id " + clusterId + " as the LB cluster id for service type " + loadBalancedServiceType);
                        }
                    }
                }
            }

            networkPartitionContextMap.put(networkPartitionId, clusterLevelNetworkPartitionContext);
        }

        return new VMClusterContext(clusterId, cluster.getServiceName(), autoscalePolicy,
                deploymentPolicy, networkPartitionContextMap);
    }

    public static KubernetesClusterContext getKubernetesClusterContext (Cluster cluster) throws PolicyValidationException {

        if (null == cluster) {
            return null;
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();

        AutoscalePolicy autoscalePolicy =
                PolicyManager.getInstance()
                        .getAutoscalePolicy(autoscalePolicyName);
        if (log.isDebugEnabled()) {
            log.debug("Autoscaling policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy policy = PolicyManager.getInstance().getAutoscalePolicy(autoscalePolicyName);

        if (policy == null) {
            String msg = String.format("Autoscaling policy is null: [policy-name] %s", autoscalePolicyName);
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        java.util.Properties properties = cluster.getProperties();
        if(properties == null) {
            String message = String.format("Properties not found in kubernetes cluster: [cluster-id] %s",
                    cluster.getClusterId());
            log.error(message);
            throw new RuntimeException(message);
        }
        String minReplicasProperty = properties.getProperty(StratosConstants.KUBERNETES_MIN_REPLICAS);
        int minReplicas = 0;
        if (minReplicasProperty != null && !minReplicasProperty.isEmpty()) {
            minReplicas = Integer.parseInt(minReplicasProperty);
        }

        int maxReplicas = 0;
        String maxReplicasProperty = properties.getProperty(StratosConstants.KUBERNETES_MAX_REPLICAS);
        if (maxReplicasProperty != null && !maxReplicasProperty.isEmpty()) {
            maxReplicas = Integer.parseInt(maxReplicasProperty);
        }

        String kubernetesHostClusterID = properties.getProperty(StratosConstants.KUBERNETES_CLUSTER_ID);
        KubernetesClusterContext kubernetesClusterCtxt = new KubernetesClusterContext(kubernetesHostClusterID,
                cluster.getClusterId(), cluster.getServiceName(),  autoscalePolicy, minReplicas, maxReplicas);

        //populate the members after restarting
        for (Member member : cluster.getMembers()) {
            String memberId = member.getMemberId();
            String clusterId = member.getClusterId();
            MemberContext memberContext = new MemberContext();
            memberContext.setMemberId(memberId);
            memberContext.setClusterId(clusterId);
            memberContext.setInitTime(member.getInitTime());

            // if there is at least one member in the topology, that means service has been created already
            // this is to avoid calling startContainer() method again
            kubernetesClusterCtxt.setServiceClusterCreated(true);

            if (MemberStatus.Activated.equals(member.getStatus())) {
                if (log.isDebugEnabled()) {
                    String msg = String.format("Active member loaded from topology and added to active member list, %s", member.toString());
                    log.debug(msg);
                }
                //dockerClusterMonitor.getKubernetesClusterCtxt().addActiveMember(memberContext);
            } else if (MemberStatus.Created.equals(member.getStatus())
                    || MemberStatus.Starting.equals(member.getStatus())) {
                if (log.isDebugEnabled()) {
                    String msg = String.format("Pending member loaded from topology and added to pending member list, %s", member.toString());
                    log.debug(msg);
                }
                //dockerClusterMonitor.getKubernetesClusterCtxt().addPendingMember(memberContext);
            }

            kubernetesClusterCtxt.addMemberStatsContext(new MemberStatsContext(memberId));
            if (log.isInfoEnabled()) {
                log.info(String.format("Member stat context has been added: [member] %s", memberId));
            }
        }

        // find lb reference type
        if (properties.containsKey(StratosConstants.LOAD_BALANCER_REF)) {
            String value = properties.getProperty(StratosConstants.LOAD_BALANCER_REF);
            //dockerClusterMonitor.setLbReferenceType(value);
            if (log.isDebugEnabled()) {
                log.debug("Set the lb reference type: " + value);
            }
        }

        return kubernetesClusterCtxt;
    }

    private static Properties convertMemberPropsToMemberContextProps(
            java.util.Properties properties) {
        Properties props = new Properties();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Property property = new Property();
            property.setName((String) e.getKey());
            property.setValue((String) e.getValue());
            props.addProperties(property);
        }
        return props;
    }
}
