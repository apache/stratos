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
package org.apache.stratos.autoscaler.monitor.application;

import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupContext;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitorFactory;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Factory class to get the Monitors.
 */
public class ApplicationMonitorFactory {

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
            if (monitor != null) {
            	((AbstractClusterMonitor)monitor).startScheduler();
            	AutoscalerContext.getInstance().addClusterMonitor((AbstractClusterMonitor)monitor);
			}
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
        ApplicationHolder.acquireReadLock();

        try {
            Group group = ApplicationHolder.getApplications().
                    getApplication(appId).getGroupRecursively(context.getId());
            groupMonitor = new GroupMonitor(group, appId);
            groupMonitor.setAppId(appId);
            if (parentMonitor != null) {
                groupMonitor.setParent(parentMonitor);
                //Setting the dependent behaviour of the monitor
                if (parentMonitor.isDependent() || (context.isDependent() && context.hasChild())) {
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
            ApplicationHolder.releaseReadLock();

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
    public static AbstractClusterMonitor getClusterMonitor(ParentComponentMonitor parentMonitor,
                                                     ClusterContext context, String appId)
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
                } else {
                    String msg = "[Cluster] " + clusterId + " cannot be found in the " +
                            "Topology for [service] " + serviceName;
                    throw new TopologyInConsistentException(msg);
                }
            } else {
                String msg = "[Service] " + serviceName + " cannot be found in the Topology";
                throw new TopologyInConsistentException(msg);
            }
            AbstractClusterMonitor clusterMonitor = ClusterMonitorFactory.getMonitor(cluster);
            //Setting the parent of the cluster monitor
            clusterMonitor.setParent(parentMonitor);
            clusterMonitor.setId(clusterId);
            //setting the dependent behaviour of the cluster monitor
            if(parentMonitor.isDependent() || (context.isDependent() && context.hasChild())) {
                clusterMonitor.setHasDependent(true);
            } else {
                clusterMonitor.setHasDependent(false);
            }
            //setting the status of the cluster, if it doesn't match with Topology cluster status.
            if (cluster.getStatus() != clusterMonitor.getStatus()) {
                //updating the status, so that it will notify the parent
                clusterMonitor.setStatus(cluster.getStatus());
            } else {
                if(!cluster.hasMembers()) {
                    StatusChecker.getInstance().onMemberStatusChange(clusterId);
                }
            }
            return clusterMonitor;

        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }
}
