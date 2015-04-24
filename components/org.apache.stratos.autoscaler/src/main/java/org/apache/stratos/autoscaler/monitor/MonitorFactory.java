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
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ClusterChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupChildContext;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.component.GroupMonitor;
import org.apache.stratos.autoscaler.monitor.component.ParentComponentMonitor;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.common.client.CloudControllerServiceClient;
import org.apache.stratos.common.partition.NetworkPartition;
import org.apache.stratos.common.partition.Partition;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.domain.application.ScalingDependentList;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Factory class to get the Monitors.
 */
public class MonitorFactory {
    private static final Log log = LogFactory.getLog(MonitorFactory.class);
    public static final String IS_PRIMARY = "PRIMARY";

    /**
     * Factor method used to create relevant monitors based on the given context
     *
     * @param context           Application/Group/Cluster context
     * @param appId             appId of the application which requires to create app monitor
     * @param parentMonitor     parent of the monitor
     * @param parentInstanceIds instance ids of the parent instances
     * @return Monitor which can be ApplicationMonitor/GroupMonitor/ClusterMonitor
     * @throws TopologyInConsistentException throws while traversing thr topology
     * @throws DependencyBuilderException    throws while building dependency for app monitor
     * @throws PolicyValidationException     throws while validating the policy associated with cluster
     * @throws PartitionValidationException  throws while validating the partition used in a cluster
     */
    public static Monitor getMonitor(ParentComponentMonitor parentMonitor,
                                     ApplicationChildContext context, String appId,
                                     List<String> parentInstanceIds)
            throws TopologyInConsistentException, DependencyBuilderException,
            PolicyValidationException, PartitionValidationException {
        Monitor monitor;
        if (context instanceof GroupChildContext) {
            monitor = getGroupMonitor(parentMonitor, context, appId, parentInstanceIds);
        } else if (context instanceof ClusterChildContext) {
            monitor = getClusterMonitor(parentMonitor, (ClusterChildContext) context, parentInstanceIds);
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
    public static Monitor getGroupMonitor(ParentComponentMonitor parentMonitor,
                                          ApplicationChildContext context, String appId,
                                          List<String> instanceIds)
            throws DependencyBuilderException,
            TopologyInConsistentException {
        GroupMonitor groupMonitor;
        Application application = ApplicationHolder.getApplications().getApplication(appId);
        if (application == null) {
            throw new RuntimeException("Application not found: [application-id] " + appId);
        }

        try {
            //acquiring read lock to create the monitor
            ApplicationHolder.acquireReadLock();
            Group group = application.getGroupRecursively(context.getId());
            if (group == null) {
                throw new RuntimeException("Group not found: [group-alias] " + context.getId());
            }

            boolean hasScalingDependents = false;
            if (parentMonitor.getScalingDependencies() != null) {
                for (ScalingDependentList scalingDependentList : parentMonitor.getScalingDependencies()) {
                    if (scalingDependentList.getScalingDependentListComponents().contains(context.getId())) {
                        hasScalingDependents = true;
                    }
                }
            }

            groupMonitor = new GroupMonitor(group, appId, instanceIds, hasScalingDependents);
            groupMonitor.setAppId(appId);
            if (parentMonitor != null) {
                groupMonitor.setParent(parentMonitor);
                //Setting the dependent behaviour of the monitor
                if (parentMonitor.hasStartupDependents() || (context.hasStartupDependents() &&
                        context.hasChild())) {
                    groupMonitor.setHasStartupDependents(true);
                } else {
                    groupMonitor.setHasStartupDependents(false);
                }
                groupMonitor.startScheduler();
            }
        } finally {
            ApplicationHolder.releaseReadLock();
        }

        Group group = application.getGroupRecursively(context.getId());

        // Starting the minimum dependencies
        groupMonitor.createInstanceAndStartDependencyAtStartup(group, instanceIds);

        /**
         * If not first app deployment, acquiring read lock to check current the status of the group,
         * when the stratos got to restarted
         */
        /*if (!initialStartup) {
            //Starting statusChecking to make it sync with the Topology in the restart of stratos.
            for (GroupInstance instance : group.getInstanceIdToInstanceContextMap().values()) {
                ServiceReferenceHolder.getInstance().
                        getGroupStatusProcessorChain().
                        process(group.getUniqueIdentifier(), appId, instance.getInstanceId());
            }

        }*/

        return groupMonitor;

    }

    /**
     * This will create a new app monitor based on the give appId by getting the
     * application from Topology
     *
     * @param applicationId appId of the application which requires to create app monitor
     * @return ApplicationMonitor
     * @throws DependencyBuilderException    throws while building dependency for app monitor
     * @throws TopologyInConsistentException throws while traversing thr topology
     */
    public static ApplicationMonitor getApplicationMonitor(String applicationId)
            throws DependencyBuilderException,
            TopologyInConsistentException, PolicyValidationException {
        ApplicationMonitor applicationMonitor;
        Application application;
        try {
            //acquiring read lock to start the monitor
            ApplicationHolder.acquireReadLock();
            application = ApplicationHolder.getApplications().getApplication(applicationId);
            if (application == null) {
                throw new RuntimeException("Application not found in the topology: [application-id] " + applicationId);
            }

            applicationMonitor = new ApplicationMonitor(application);
            applicationMonitor.setHasStartupDependents(false);
            // Starting the scheduler of the application monitor
            applicationMonitor.startScheduler();
        } finally {
            ApplicationHolder.releaseReadLock();
        }

        applicationMonitor.startMinimumDependencies(application);

        /*//If not first app deployment, then calculate the current status of the app instance.
        if (!initialStartup) {
            for (ApplicationInstance instance :
                    application.getInstanceIdToInstanceContextMap().values()) {
                //Starting statusChecking to make it sync with the Topology in the restart of stratos.
                ServiceReferenceHolder.getInstance().
                        getGroupStatusProcessorChain().
                        process(appId, appId, instance.getInstanceId());

            }
        }*/

        return applicationMonitor;
    }

    /**
     * Updates ClusterContext for given cluster
     *
     * @param parentMonitor parent of the monitor
     * @param context
     * @return ClusterMonitor - Updated ClusterContext
     * @throws org.apache.stratos.autoscaler.exception.policy.PolicyValidationException
     * @throws org.apache.stratos.autoscaler.exception.partition.PartitionValidationException
     */
    public static ClusterMonitor getClusterMonitor(ParentComponentMonitor parentMonitor,
                                                   ClusterChildContext context,
                                                   List<String> parentInstanceIds)
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
            Service service = topology.getService(serviceName);
            if (service == null) {
                String msg = String.format("Service not found in topology: [service] %s", serviceName);
                throw new RuntimeException(msg);
            }

            cluster = service.getCluster(clusterId);
            if (cluster == null) {
                String msg = String.format("Cluster not found in topology: [service] %s [cluster] %s",
                        serviceName, clusterId);
                throw new RuntimeException(msg);
            }


            // deployment policy validation
            String deploymentPolicyId = AutoscalerUtil.getDeploymentPolicyIdByAlias(parentMonitor.appId,
                    AutoscalerUtil.getAliasFromClusterId(clusterId));
            DeploymentPolicy deploymentPolicy = null;
            try {
                deploymentPolicy = PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId);
            } catch (Exception e) {
                String msg = String.format("Error while getting deployment policy from cloud controller [deployment-policy-id] %s", deploymentPolicyId);
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }

            List<Partition> partitionList = new ArrayList<Partition>();
            for (NetworkPartition networkPartition : deploymentPolicy.getNetworkPartitions()) {

                if (networkPartition != null) {

                    for (Partition partition : networkPartition.getPartitions()) {
                        partitionList.add(partition);
                    }

                    try {

                        CloudControllerServiceClient.getInstance().validateNetworkPartitionOfDeploymentPolicy(serviceName, networkPartition.getId());
                    } catch (Exception e) {
                        String msg = String.format("Error while validating deployment policy from cloud controller [network-partition-id] %s", networkPartition.getId());
                        log.error(msg, e);
                        throw new RuntimeException(msg, e);
                    }
                }
            }
            // deployment policy validation ends

            boolean hasScalingDependents = false;
            if (parentMonitor.getScalingDependencies() != null) {
                for (ScalingDependentList scalingDependentList : parentMonitor.getScalingDependencies()) {
                    if (scalingDependentList.getScalingDependentListComponents().contains(clusterId)) {
                        hasScalingDependents = true;
                    }
                }
            }

            boolean groupScalingEnabledSubtree = false;
            if (parentMonitor instanceof GroupMonitor) {
                GroupMonitor groupMonitor = (GroupMonitor) parentMonitor;
                groupScalingEnabledSubtree = findIfChildIsInGroupScalingEnabledSubTree(groupMonitor);
            }

            ClusterMonitor clusterMonitor = new ClusterMonitor(cluster, hasScalingDependents, groupScalingEnabledSubtree,
                    deploymentPolicyId);

            Properties props = cluster.getProperties();
            if (props != null) {
                // Set hasPrimary property
                // hasPrimary is true if there are primary members available in that cluster
                clusterMonitor.setHasPrimary(Boolean.parseBoolean(cluster.getProperties().getProperty(IS_PRIMARY)));
            }

            // Setting the parent of the cluster monitor
            clusterMonitor.setParent(parentMonitor);
            clusterMonitor.setId(clusterId);

            // Setting the startup dependent behaviour of the cluster monitor
            if (parentMonitor.hasStartupDependents() || (context.hasStartupDependents() &&
                    context.hasChild())) {
                clusterMonitor.setHasStartupDependents(true);
            } else {
                clusterMonitor.setHasStartupDependents(false);
            }

            // Creating the instance of the cluster
            clusterMonitor.createClusterInstances(parentInstanceIds, cluster);
            AutoscalerContext.getInstance().addClusterMonitor(clusterMonitor);
            log.info("ClusterMonitor created: " + clusterMonitor.toString());

            return clusterMonitor;
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    private static org.apache.stratos.cloud.controller.stub.domain.Partition[] convertPartitionsToCCPartitions(
            Partition[] partitions) {

        org.apache.stratos.cloud.controller.stub.domain.Partition[] ccPartitions
                = new org.apache.stratos.cloud.controller.stub.domain.Partition[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            org.apache.stratos.cloud.controller.stub.domain.Partition ccPartition
                    = new org.apache.stratos.cloud.controller.stub.domain.Partition();
            ccPartition.setId(partitions[i].getId());
            ccPartition.setDescription(partitions[i].getDescription());
            ccPartition.setIsPublic(partitions[i].getIsPublic());
            ccPartition.setKubernetesClusterId(partitions[i].getKubernetesClusterId());
            ccPartition.setProperties(AutoscalerUtil.toStubProperties(partitions[i].getProperties()));
            ccPartition.setProvider(partitions[i].getProvider());
            ccPartitions[i] = ccPartition;
        }
        return ccPartitions;
    }

//    private static org.apache.stratos.cloud.controller.stub.Properties convertPropertiesToCCProperties(
//            Properties properties) {
//
//        org.apache.stratos.cloud.controller.stub.Properties ccProperties
//                = new org.apache.stratos.cloud.controller.stub.Properties();
//        Property[] propertyArray = properties.getProperties();
//        for(int i = 0; i < propertyArray.length; i++){
//
//            ccProperties.getProperties()[i].setName(properties.getProperties()[i].getName());
//            ccProperties.getProperties()[i].setValue(properties.getProperties()[i].getValue());
//        }
//        return ccProperties;
//    }

    private static boolean findIfChildIsInGroupScalingEnabledSubTree(GroupMonitor groupMonitor) {
        boolean groupScalingEnabledSubtree = false;
        ParentComponentMonitor parentComponentMonitor = groupMonitor.getParent();

        if (parentComponentMonitor != null && parentComponentMonitor instanceof GroupMonitor) {
            findIfChildIsInGroupScalingEnabledSubTree((GroupMonitor) parentComponentMonitor);
        } else {
            return groupMonitor.isGroupScalingEnabled();
        }
        return groupScalingEnabledSubtree;
    }
}