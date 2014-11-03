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
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupContext;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.applications.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;

import java.util.Map;

/**
 * Factory class to get the Monitors.
 */
public class ApplicationMonitorFactory {
    private static final Log log = LogFactory.getLog(ApplicationMonitorFactory.class);

    /**
     * Factor method used to create relevant monitors based on the given context
     *
     * @param context       Application/Group/Cluster context
     * @param appId         appId of the application which requires to create app monitor
     * @param parentMonitor parent of the monitor
     * @return Monitor which can be ApplicationMonitor/GroupMonitor/ClusterMonitor
     * @throws TopologyInConsistentException throws while traversing thr topology
     * @throws DependencyBuilderException    throws while building dependency for app monitor
     * @throws PolicyValidationException     throws while validating the policy associated with cluster
     * @throws PartitionValidationException  throws while validating the partition used in a cluster
     */
    public static Monitor getMonitor(ParentComponentMonitor parentMonitor, ApplicationContext context, String appId)
            throws TopologyInConsistentException,
            DependencyBuilderException, PolicyValidationException, PartitionValidationException {
        Monitor monitor;

        if (context instanceof GroupContext) {
            monitor = getGroupMonitor(parentMonitor, context, appId);
        } else if (context instanceof ClusterContext) {
            monitor = getClusterMonitor(parentMonitor, (ClusterContext) context, appId);
            //Start the thread
            Thread th = new Thread((AbstractClusterMonitor) monitor);
            th.start();
        } else {
            monitor = getApplicationMonitor(appId);
        }
        return monitor;
    }

    /**
     * This will create the GroupMonitor based on given groupId by going thr Topology
     *
     * @param parentMonitor parent of the monitor
     * @param context       groupId of the group
     * @param appId         appId of the relevant application
     * @return Group monitor
     * @throws DependencyBuilderException    throws while building dependency for app monitor
     * @throws TopologyInConsistentException throws while traversing thr topology
     */
    public static Monitor getGroupMonitor(ParentComponentMonitor parentMonitor, ApplicationContext context, String appId)
            throws DependencyBuilderException,
            TopologyInConsistentException {
        GroupMonitor groupMonitor;
        ApplicationManager.acquireReadLockForApplication(appId);

        try {
            Group group = ApplicationManager.getApplications().getApplication(appId).getGroupRecursively(context.getId());
            groupMonitor = new GroupMonitor(group, appId);
            groupMonitor.setAppId(appId);
            if(parentMonitor != null) {
                groupMonitor.setParent(parentMonitor);
                //Setting the dependent behaviour of the monitor
                if(parentMonitor.isDependent() || (context.isDependent() && context.hasChild())) {
                    groupMonitor.setHasDependent(true);
                } else {
                    groupMonitor.setHasDependent(false);
                }
                //TODO make sure when it is async

                if (group.getStatus() != groupMonitor.getStatus()) {
                    //updating the status, if the group is not in created state when creating group Monitor
                    //so that groupMonitor will notify the parent (useful when restarting stratos)
                    groupMonitor.setStatus(group.getStatus());
                }
            }

        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);

        }
        return groupMonitor;

    }

    /**
     * This will create a new app monitor based on the give appId by getting the
     * application from Topology
     *
     * @param appId appId of the application which requires to create app monitor
     * @return ApplicationMonitor
     * @throws DependencyBuilderException    throws while building dependency for app monitor
     * @throws TopologyInConsistentException throws while traversing thr topology
     */
    public static ApplicationMonitor getApplicationMonitor(String appId)
            throws DependencyBuilderException,
            TopologyInConsistentException {
        ApplicationMonitor applicationMonitor;
        ApplicationHolder.acquireReadLock();
        try {
            Application application = ApplicationHolder.getApplications().getApplication(appId);
            if (application != null) {
                applicationMonitor = new ApplicationMonitor(application);
                applicationMonitor.setHasDependent(false);

            } else {
                String msg = "[Application] " + appId + " cannot be found in the Topology";
                throw new TopologyInConsistentException(msg);
            }
        } finally {
            ApplicationHolder.releaseReadLock();

        }

        return applicationMonitor;

    }

    /**
     * Updates ClusterContext for given cluster
     *
     * @param parentMonitor parent of the monitor
     * @param context
     * @return ClusterMonitor - Updated ClusterContext
     * @throws org.apache.stratos.autoscaler.exception.PolicyValidationException
     * @throws org.apache.stratos.autoscaler.exception.PartitionValidationException
     */
    public static ClusterMonitor getClusterMonitor(ParentComponentMonitor parentMonitor,
                                                   ClusterContext context, String appId)
            throws PolicyValidationException,
            PartitionValidationException,
            TopologyInConsistentException {
        //Retrieving the Cluster from Topology
        String clusterId = context.getId();
        String serviceName = context.getServiceName();

        Cluster cluster;
        ClusterMonitor clusterMonitor;
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

            clusterMonitor = new ClusterMonitor(cluster.getClusterId(), cluster.getServiceName(),
                    deploymentPolicy, policy);

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
                                //triggering the status checker
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
                clusterMonitor.setParent(parentMonitor);
                if(parentMonitor.isDependent() || (context.isDependent() && context.hasChild())) {
                    clusterMonitor.setHasDependent(true);
                } else {
                    clusterMonitor.setHasDependent(false);
                }
                AutoscalerContext.getInstance().addMonitor(clusterMonitor);
                if (log.isInfoEnabled()) {
                    log.info(String.format("Network partition context has been added: [network partition] %s",
                            networkPartitionContext.getId()));
                }
            }
            //TODO to make sure when group monitor is async
            //if cluster is not in created state then notify the parent monitor
            if (cluster.getStatus() != clusterMonitor.getStatus()) {
                //updating the status, so that it will notify the parent
                clusterMonitor.setStatus(cluster.getStatus());
            }

            if (!cluster.hasMembers()) {
                //triggering the status checker if cluster has members to decide
                // on the current status of the cluster
                StatusChecker.getInstance().onMemberStatusChange(clusterId);
            }
        } finally {
            //release read lock for the service and cluster
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
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
