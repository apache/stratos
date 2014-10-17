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
package org.apache.stratos.autoscaler.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.GroupContext;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;

import java.util.Map;

/**
 * Factory class to get the Monitors.
 */
public class ApplicationMonitorFactory {
    private static final Log log = LogFactory.getLog(ApplicationMonitorFactory.class);

    public static Monitor getMonitor(ApplicationContext context, String appId)
            throws TopologyInConsistentException,
            DependencyBuilderException, PolicyValidationException, PartitionValidationException {
        Monitor monitor;

        if (context instanceof GroupContext) {
            monitor = getGroupMonitor(context.getId(), appId);
        } else if (context instanceof ClusterContext) {
            monitor = getClusterMonitor((ClusterContext) context, appId);
        } else {
            monitor = getApplicationMonitor(appId);
        }
        return monitor;
    }

    public static Monitor getGroupMonitor(String groupId, String appId) throws DependencyBuilderException,
            TopologyInConsistentException {
        GroupMonitor groupMonitor;
        TopologyManager.acquireReadLockForApplication(appId);

        try {
            Group group = TopologyManager.getTopology().getApplication(appId).getGroupRecursively(groupId);
            groupMonitor = new GroupMonitor(group, appId);
            groupMonitor.setAppId(appId);
            if (group.getStatus() != groupMonitor.getStatus()) {
                //updating the status, so that it will notify the parent
                groupMonitor.setStatus(group.getStatus());
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);

        }
        return groupMonitor;

    }

    public static ApplicationMonitor getApplicationMonitor(String appId)
            throws DependencyBuilderException,
            TopologyInConsistentException {
        ApplicationMonitor applicationMonitor;
        TopologyManager.acquireReadLockForApplication(appId);
        try {
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                applicationMonitor = new ApplicationMonitor(application);
            } else {
                String msg = "[Application] " + appId + " cannot be found in the Topology";
                throw new TopologyInConsistentException(msg);
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }

        return applicationMonitor;

    }

    /**
     * Updates ClusterContext for given cluster
     *
     * @param context
     * @return ClusterMonitor - Updated ClusterContext
     * @throws org.apache.stratos.autoscaler.exception.PolicyValidationException
     * @throws org.apache.stratos.autoscaler.exception.PartitionValidationException
     */
    public static ClusterMonitor getClusterMonitor(ClusterContext context, String appId)
            throws PolicyValidationException,
            PartitionValidationException,
            TopologyInConsistentException {
        //Retrieving the Cluster from Topology
        String clusterId = context.getId();
        String serviceName = context.getServiceName();

        Cluster cluster;
        //acquire read lock for the service and cluster
        TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
        try {
            Topology topology = TopologyManager.getTopology();
            if (topology.serviceExists(serviceName)) {
                Service service = topology.getService(serviceName);
                if (service.clusterExists(clusterId)) {
                    cluster = service.getCluster(clusterId);
                    if (log.isDebugEnabled()) {
                        log.debug("Dependency check starting the [cluster]" + clusterId);
                    }
                    // startClusterMonitor(this, cluster);
                    //context.setCurrentStatus(Status.Created);
                } else {
                    String msg = "[Cluster] " + clusterId + " cannot be found in the " +
                            "Topology for [service] " + serviceName;
                    throw new TopologyInConsistentException(msg);
                }
            } else {
                String msg = "[Service] " + serviceName + " cannot be found in the Topology";
                throw new TopologyInConsistentException(msg);

            }
        } finally {
            //release read lock for the service and cluster
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        String deploymentPolicyName = cluster.getDeploymentPolicyName();

        if (log.isDebugEnabled()) {
            log.debug("Deployment policy name: " + deploymentPolicyName);
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy policy =
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

        Partition[] allPartitions = deploymentPolicy.getAllPartitions();
        if (allPartitions == null) {
            String msg =
                    "Deployment Policy's Partitions are null. Policy name: " +
                            deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        CloudControllerClient.getInstance().validateDeploymentPolicy(cluster.getServiceName(), deploymentPolicy);

        ClusterMonitor clusterMonitor =
                new ClusterMonitor(cluster.getClusterId(),
                        cluster.getServiceName(),
                        deploymentPolicy, policy);
        clusterMonitor.setAppId(cluster.getAppId());

        for (PartitionGroup partitionGroup : deploymentPolicy.getPartitionGroups()) {

            NetworkPartitionContext networkPartitionContext = new NetworkPartitionContext(partitionGroup.getId(),
                    partitionGroup.getPartitionAlgo(), partitionGroup.getPartitions());

            for (Partition partition : partitionGroup.getPartitions()) {
                PartitionContext partitionContext = new PartitionContext(partition);
                partitionContext.setServiceName(cluster.getServiceName());
                partitionContext.setProperties(cluster.getProperties());
                partitionContext.setNetworkPartitionId(partitionGroup.getId());

                for (Member member : cluster.getMembers()) {
                    String memberId = member.getMemberId();
                    if (member.getPartitionId().equalsIgnoreCase(partition.getId())) {
                        MemberContext memberContext = new MemberContext();
                        memberContext.setClusterId(member.getClusterId());
                        memberContext.setMemberId(memberId);
                        memberContext.setPartition(partition);
                        memberContext.setProperties(convertMemberPropsToMemberContextProps(member.getProperties()));

                        if (MemberStatus.Activated.equals(member.getStatus())) {
                            partitionContext.addActiveMember(memberContext);
//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
//                            partitionContext.incrementCurrentActiveMemberCount(1);

                        } else if (MemberStatus.Created.equals(member.getStatus()) || MemberStatus.Starting.equals(member.getStatus())) {
                            partitionContext.addPendingMember(memberContext);

//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
                        } else if (MemberStatus.Suspended.equals(member.getStatus())) {
//                            partitionContext.addFaultyMember(memberId);
                        }
                        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member stat context has been added: [member] %s", memberId));
                        }
                    }

                }
                networkPartitionContext.addPartitionContext(partitionContext);
                if (log.isInfoEnabled()) {
                    log.info(String.format("Partition context has been added: [partition] %s",
                            partitionContext.getPartitionId()));
                }
            }

            clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
            //clusterMonitor.setCurrentStatus(Status.Created);
            if (log.isInfoEnabled()) {
                log.info(String.format("Network partition context has been added: [network partition] %s",
                        networkPartitionContext.getId()));
            }
        }

        // set hasPrimary property
        // hasPrimary is true if there are primary members available in that cluster
        if (cluster.getProperties() != null) {
            clusterMonitor.setHasPrimary(Boolean.parseBoolean(cluster.getProperties().getProperty(Constants.IS_PRIMARY)));
        }

        log.info("Cluster monitor created: " + clusterMonitor.toString());
        return clusterMonitor;
    }


    private static Properties convertMemberPropsToMemberContextProps(
            java.util.Properties properties) {
        Properties props = new Properties();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Property prop = new Property();
            prop.setName((String) e.getKey());
            prop.setValue((String) e.getValue());
            props.addProperties(prop);
        }
        return props;
    }
}
