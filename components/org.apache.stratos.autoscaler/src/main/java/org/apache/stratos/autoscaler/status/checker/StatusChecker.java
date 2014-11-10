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
package org.apache.stratos.autoscaler.status.checker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
import org.apache.stratos.messaging.domain.applications.*;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.Map;


/**
 * This will be used to evaluate the status of a group
 * and notify the interested parties about the status changes.
 */
public class StatusChecker {
    private static final Log log = LogFactory.getLog(StatusChecker.class);


    private StatusChecker() {

    }

    public static StatusChecker getInstance() {
        //TODO synchronized
        return Holder.INSTANCE;
    }

    /**
     * Calculating whether the cluster has all min instances as active and send the
     * ClusterActivatedEvent.
     *
     * @param clusterId id of the cluster
     */
    public void onMemberStatusChange(final String clusterId) {
        Runnable group = new Runnable() {
            public void run() {
                VMClusterMonitor monitor = (VMClusterMonitor) AutoscalerContext.getInstance().getClusterMonitor(clusterId);
                boolean clusterActive = false;
                if (monitor != null) {
                    clusterActive = clusterActive(monitor);

                }
                log.info("Status checker running for [cluster] " + clusterId +
                        " the status [clusterActive] " + clusterActive);
                // if active then notify upper layer
                if (clusterActive) {
                    //send event to cluster status topic
                    monitor.setHasFaultyMember(false);
                    ClusterStatusEventPublisher.sendClusterActivatedEvent(monitor.getAppId(),
                            monitor.getServiceId(), monitor.getClusterId());
                }
            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    /**
     * This will calculate the status of the cluster upon a member termination.
     * The possible states which cluster can change upon member termination are
     * Active --> InActive, Terminating-->Terminated, Terminating-->Reset(Created)
     *
     * @param clusterId id of the cluster
     */
    public void onMemberTermination(final String clusterId) {
        Runnable group = new Runnable() {
            public void run() {
                VMClusterMonitor monitor = (VMClusterMonitor) AutoscalerContext.getInstance().getClusterMonitor(clusterId);
                boolean clusterMonitorHasMembers = clusterMonitorHasMembers(monitor);
                boolean clusterActive = clusterActive(monitor);

                try {
                    TopologyManager.acquireReadLockForCluster(monitor.getServiceId(), monitor.getClusterId());
                    Service service = TopologyManager.getTopology().getService(monitor.getServiceId());
                    Cluster cluster;
                    String appId = monitor.getAppId();
                    if (service != null) {
                        cluster = service.getCluster(monitor.getClusterId());
                        if (cluster != null) {
                            try {

                                ApplicationHolder.acquireReadLock();
                                Application application = ApplicationHolder.getApplications().getApplication(appId);
                                //if all members removed from the cluster and cluster is in terminating,
                                // either it has to be terminated or Reset
                                if (!clusterMonitorHasMembers && cluster.getStatus() == ClusterStatus.Terminating) {
                                    if (application.getStatus() == ApplicationStatus.Terminating) {
                                        ClusterStatusEventPublisher.sendClusterTerminatedEvent(appId, monitor.getServiceId(),
                                                monitor.getClusterId());
                                    } else {
                                        ClusterStatusEventPublisher.sendClusterResetEvent(appId, monitor.getServiceId(),
                                                monitor.getClusterId());
                                    }

                                } else {
                                    //if the cluster is not active and, if it is in Active state
                                    if (!clusterActive && cluster.getStatus() == ClusterStatus.Active) {
                                        ClusterStatusEventPublisher.sendClusterInActivateEvent(monitor.getAppId(),
                                                monitor.getServiceId(), clusterId);
                                    } else {
                                        log.info("Cluster has non terminated [members] and in the [status] "
                                                + cluster.getStatus().toString());
                                    }
                                }
                            } finally {
                                ApplicationHolder.releaseReadLock();
                            }
                        }
                    }


                } finally {
                    TopologyManager.releaseReadLockForCluster(monitor.getServiceId(), monitor.getClusterId());

                }
            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();

    }

    /**
     * Calculate whether the cluster is active based on the minimum count available in each partition
     *
     * @param monitor Cluster monitor which has the member
     * @return whether cluster is active or not
     */
    private boolean clusterActive(VMClusterMonitor monitor) {
        boolean clusterActive = false;
        for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
            //minimum check per partition
            for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts().values()) {
                if (partitionContext.getMinimumMemberCount() == partitionContext.getActiveMemberCount()) {
                    clusterActive = true;
                } else if (partitionContext.getActiveMemberCount() > partitionContext.getMinimumMemberCount()) {
                    log.info("cluster already activated...");
                    clusterActive = true;
                } else {
                    return false;
                }
            }
        }
        return clusterActive;
    }

    private boolean clusterMonitorHasMembers(VMClusterMonitor monitor) {
        boolean hasMember = false;
        for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
            //minimum check per partition
            for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts().values()) {
                if (partitionContext.getNonTerminatedMemberCount() > 0) {
                    hasMember = true;
                    return hasMember;
                }
            }
        }
        return hasMember;
    }

    /**
     * This will calculate the status of the cluster upon a member fault event
     *
     * @param clusterId   id of the cluster
     * @param partitionId is to decide in which partition has less members while others have active members
     */
    public void onMemberFaultEvent(final String clusterId, final String partitionId) {
        Runnable group = new Runnable() {
            public void run() {
                VMClusterMonitor monitor = (VMClusterMonitor) AutoscalerContext.getInstance().getClusterMonitor(clusterId);
                boolean clusterInActive = getClusterInactive(monitor, partitionId);
                String appId = monitor.getAppId();
                if (clusterInActive) {
                    //if the monitor is dependent, temporarily pausing it
                    if (monitor.isDependent()) {
                        monitor.setHasFaultyMember(true);
                    }
                    //send cluster In-Active event to cluster status topic
                    ClusterStatusEventPublisher.sendClusterInActivateEvent(appId, monitor.getServiceId(), clusterId);

                } else {
                    boolean clusterActive = clusterActive(monitor);
                    if (clusterActive) {
                        ClusterStatusEventPublisher.sendClusterActivatedEvent(appId, monitor.getServiceId(), clusterId);
                    }
                }

            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    private boolean getClusterInactive(VMClusterMonitor monitor, String partitionId) {
        boolean clusterInActive = false;
        for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
            for (PartitionContext partition : networkPartitionContext.getPartitionCtxts().values()) {
                if (partitionId.equals(partition.getPartitionId()) &&
                        partition.getActiveMemberCount() <= partition.getMinimumMemberCount()) {
                    clusterInActive = true;
                    return clusterInActive;
                }
            }

        }
        return clusterInActive;
    }

    /**
     * @param idOfChild
     * @param groupId
     * @param appId
     */
    public void onChildStatusChange(final String idOfChild, final String groupId,
                                    final String appId) {
        updateChild(idOfChild, groupId, appId);
    }

    private void updateChild(final String idOfChild, final String groupId, final String appId) {
        Runnable group = new Runnable() {
            public void run() {
                try {
                    ApplicationHolder.acquireWriteLock();
                    ParentComponent component;
                    if (groupId.equals(appId)) {
                        //it is an application
                        component = ApplicationHolder.getApplications().
                                getApplication(appId);
                    } else {
                        //it is a group
                        component = ApplicationHolder.getApplications().
                                getApplication(appId).getGroupRecursively(groupId);
                    }
                    Map<String, ClusterDataHolder> clusterIds = component.getClusterDataMap();
                    Map<String, Group> groups = component.getAliasToGroupMap();
                    updateChildStatus(appId, idOfChild, groups, clusterIds, component);
                } finally {
                    ApplicationHolder.releaseWriteLock();

                }

            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    /**
     * This will use to calculate whether  all children of a particular component is active by travesing Top
     *
     * @param appId
     * @param id
     * @param groups
     * @param clusterData
     * @param parent
     * @return
     */
    private boolean updateChildStatus(String appId, String id, Map<String, Group> groups,
                                      Map<String, ClusterDataHolder> clusterData, ParentComponent parent) {
        boolean childFound = false;
        boolean clusterFound = false;

        for (ClusterDataHolder clusterDataHolder : clusterData.values()) {
            if (clusterDataHolder.getClusterId().equals(id)) {
                clusterFound = true;
            }
        }
        log.info("cluster found: " + clusterFound);
        if (clusterFound || groups.containsKey(id)) {
            childFound = true;
            /*try {
                ApplicationHolder.acquireWriteLock();*/
                Application application = ApplicationHolder.getApplications().getApplication(appId);

                if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Active) ||
                        clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Active) ||
                        getAllClusterInSameState(clusterData, ClusterStatus.Active) &&
                                getAllGroupInSameState(groups, GroupStatus.Active)) {
                    //send activation event
                    if (parent instanceof Application) {
                        //send application activated event
                        log.info("sending app activate: " + appId);
                        ApplicationBuilder.handleApplicationActivatedEvent(appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group activate: " + parent.getUniqueIdentifier());
                        ApplicationBuilder.handleGroupActivatedEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Terminated) ||
                        clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Terminated) ||
                        getAllClusterInSameState(clusterData, ClusterStatus.Terminated) &&
                                getAllGroupInSameState(groups, GroupStatus.Terminated)) {
                    //send the terminated event
                    if (parent instanceof Application) {
                        //validating the life cycle
                        if (application.getStatus().equals(ApplicationStatus.Terminating)) {
                            log.info("sending app terminated: " + appId);
                            ApplicationBuilder.handleApplicationTerminatedEvent(appId);
                        } else {
                            log.info("[Application] " + appId + " is in the [status] " +
                                    application.getStatus().toString() + ". Hence not sending terminated event");
                        }
                        //StatusEventPublisher.sendApp(appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group created : " + parent.getUniqueIdentifier());
                        ApplicationBuilder.handleGroupTerminatedEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Created) ||
                        clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Created) ||
                        getAllClusterInSameState(clusterData, ClusterStatus.Created) &&
                                getAllGroupInSameState(groups, GroupStatus.Created)) {
                    if (parent instanceof Application) {
                        log.info("[Application] " + appId + "couldn't change to Created, since it is" +
                                "already in " + application.getStatus().toString());
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group created : " + parent.getUniqueIdentifier());
                        ApplicationBuilder.handleGroupCreatedEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && getAllClusterInactive(clusterData) ||
                        clusterData.isEmpty() && getAllGroupInActive(groups) ||
                        getAllClusterInactive(clusterData) && getAllGroupInActive(groups)) {
                    //send the in activation event
                    if (parent instanceof Application) {
                        //send application activated event
                        log.warn("Application can't be in in-active : " + appId);
                        //StatusEventPublisher.sendApplicationInactivatedEvent(appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group in-active: " + parent.getUniqueIdentifier());
                        ApplicationBuilder.handleGroupInActivateEvent(appId, parent.getUniqueIdentifier());
                    }
                } else {
                    log.warn("Clusters/groups not found in this [component] " + appId);
                }
            /*} finally {
                ApplicationHolder.releaseWriteLock();
            }*/


            return childFound;
        } else {
            log.warn("There is no child found in the [group/cluster] " + id + " found in the " +
                    "[component]" + parent.getUniqueIdentifier());
        }
        return childFound;
    }

    private boolean getAllGroupInActive(Map<String, Group> groups) {
        boolean groupStat = false;
        for (Group group : groups.values()) {
            if (group.getStatus() == GroupStatus.Inactive) {
                groupStat = true;
                return groupStat;
            } else {
                groupStat = false;
            }
        }
        return groupStat;
    }

    private boolean getAllGroupInSameState(Map<String, Group> groups, GroupStatus status) {
        boolean groupStat = false;
        for (Group group : groups.values()) {
            if (group.getStatus() == status) {
                groupStat = true;
            } else {
                groupStat = false;
                return groupStat;
            }
        }
        return groupStat;
    }


    private boolean getAllClusterInactive(Map<String, ClusterDataHolder> clusterData) {
        boolean clusterStat = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            Service service = TopologyManager.getTopology().getService(clusterDataHolderEntry.getValue().getServiceType());
            Cluster cluster = service.getCluster(clusterDataHolderEntry.getValue().getClusterId());
            if (cluster.getStatus() == ClusterStatus.Inactive) {
                clusterStat = true;
                return clusterStat;
            } else {
                clusterStat = false;

            }
        }
        return clusterStat;
    }

    private boolean getAllClusterInSameState(Map<String, ClusterDataHolder> clusterData,
                                             ClusterStatus status) {
        boolean clusterStat = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            String serviceName = clusterDataHolderEntry.getValue().getServiceType();
            String clusterId = clusterDataHolderEntry.getValue().getClusterId();
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            try {
                Service service = TopologyManager.getTopology().getService(serviceName);
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.getStatus() == status) {
                    clusterStat = true;
                } else {
                    clusterStat = false;
                    return clusterStat;
                }
            } finally {
               TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
            }

        }
        return clusterStat;
    }

    /*private GroupStatus getGroupStatus(Map<String, Group> groups) {
        GroupStatus status = null;
        boolean groupActive = true;
        boolean groupTerminated = true;
        boolean groupCreated = true;

        for (Group group : groups.values()) {
            if (group.getStatus() == GroupStatus.Active) {
                groupActive = groupActive && true;
                groupTerminated = false;
                groupCreated = false;
            } else if (group.getStatus() == GroupStatus.Inactive) {
                status = GroupStatus.Inactive;
                groupActive = false;
                groupTerminated = false;
                groupCreated = false;
                break;
            } else if (group.getStatus() == GroupStatus.Terminated) {
                groupActive = false;
                groupCreated = false;
                groupTerminated = groupTerminated && true;
            } else if (group.getStatus() == GroupStatus.Created) {
                groupActive = false;
                groupTerminated = false;
                groupCreated = groupCreated && true;
            }
        }

        if (groups == null || groups != null && groups.isEmpty()) {
            groupActive = false;
            groupTerminated = false;
            groupCreated = false;
        }

        if (groupActive) {
            status = GroupStatus.Active;
        } else if (groupTerminated) {
            status = GroupStatus.Terminated;
        } else if (groupCreated) {
            status = GroupStatus.Created;
        }
        return status;

    }

    private ClusterStatus getClusterStatus(Map<String, ClusterDataHolder> clusterData) {
        ClusterStatus status = null;
        boolean clusterActive = true;
        boolean clusterTerminated = true;
        boolean clusterCreated = true;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            Service service = TopologyManager.getTopology().getService(clusterDataHolderEntry.getValue().getServiceType());
            Cluster cluster = service.getCluster(clusterDataHolderEntry.getValue().getClusterId());
            if (cluster.getStatus() == ClusterStatus.Active) {
                clusterActive = clusterActive && true;
                clusterTerminated = false;
                clusterCreated = false;
            } else if (cluster.getStatus() == ClusterStatus.Inactive) {
                status = ClusterStatus.Inactive;
                clusterActive = false;
                clusterTerminated = false;
                clusterCreated = false;
                break;
            } else if (cluster.getStatus() == ClusterStatus.Terminated) {
                clusterActive = false;
                clusterCreated = false;
                clusterTerminated = clusterTerminated && true;
            } else if (cluster.getStatus() == ClusterStatus.Created) {
                clusterActive = false;
                clusterTerminated = false;
                clusterCreated = clusterCreated && true;
            }
        }

        if (clusterData == null || clusterData != null && clusterData.isEmpty()) {
            clusterActive = false;
            clusterTerminated = false;
            clusterCreated = false;
        }

        if (clusterActive) {
            status = ClusterStatus.Active;
        } else if (clusterTerminated) {
            status = ClusterStatus.Terminated;
        } else if (clusterCreated) {
            status = ClusterStatus.Created;
        }
        return status;
    }*/

    private static class Holder {
        private static final StatusChecker INSTANCE = new StatusChecker();
    }

}
