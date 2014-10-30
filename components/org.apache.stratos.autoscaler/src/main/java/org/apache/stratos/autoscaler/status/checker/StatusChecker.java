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
import org.apache.stratos.autoscaler.grouping.topic.StatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.messaging.domain.topology.*;
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
                ClusterMonitor monitor = (ClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);
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
                    StatusEventPublisher.sendClusterActivatedEvent(monitor.getAppId(),
                            monitor.getServiceId(), monitor.getClusterId());
                }
            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    public void onMemberTermination(final String clusterId) {
        Runnable group = new Runnable() {
            public void run() {
                ClusterMonitor monitor = (ClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);
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

                                TopologyManager.acquireReadLockForApplication(appId);
                                Application application = TopologyManager.getTopology().getApplication(appId);

                                if (!clusterMonitorHasMembers && cluster.getStatus() == ClusterStatus.Terminating) {
                                    if (application.getStatus() == ApplicationStatus.Terminating) {
                                        StatusEventPublisher.sendClusterTerminatedEvent(appId, monitor.getServiceId(),
                                                monitor.getClusterId());
                                    } else {
                                        StatusEventPublisher.sendClusterCreatedEvent(appId, monitor.getServiceId(),
                                                monitor.getClusterId());
                                    }

                                } else {
                                    log.info("Cluster has non terminated [members] and in the [status] "
                                            + cluster.getStatus().toString());

                        /*if(!clusterActive && !(cluster.getStatus() == ClusterStatus.Inactive ||
                                cluster.getStatus() == ClusterStatus.Terminating)) {
                            cluster.getStatus()
                            StatusEventPublisher.sendClusterInActivateEvent(monitor.getAppId(),
                                    monitor.getServiceId(), clusterId);

                        }*/
                                }
                            } finally {
                                TopologyManager.releaseReadLockForApplication(appId);
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

    private boolean clusterActive(AbstractClusterMonitor monitor) {
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
                    clusterActive = false;
                    return clusterActive;
                }
            }
        }
        return clusterActive;
    }

    private boolean clusterMonitorHasMembers(AbstractClusterMonitor monitor) {
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
     * @param clusterId
     * @param partitionId is to decide in which partition has less members while others have active members
     */
    public void onMemberFaultEvent(final String clusterId, final String partitionId) {
        Runnable group = new Runnable() {
            public void run() {
                ClusterMonitor monitor = (ClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);
                boolean clusterInActive = getClusterInActive(monitor, partitionId);
                String appId = monitor.getAppId();
                if (clusterInActive) {
                    //if the monitor is dependent, temporarily pausing it
                    if (monitor.isDependent()) {
                        monitor.setHasFaultyMember(true);
                    }
                    //send cluster In-Active event to cluster status topic
                    StatusEventPublisher.sendClusterInActivateEvent(appId, monitor.getServiceId(), clusterId);

                } else {
                    boolean clusterActive = clusterActive(monitor);
                    if (clusterActive) {
                        StatusEventPublisher.sendClusterActivatedEvent(appId, monitor.getServiceId(), clusterId);
                    }
                }

            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    private boolean getClusterInActive(AbstractClusterMonitor monitor, String partitionId) {
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
                    TopologyManager.acquireReadLockForApplication(appId);
                    ParentComponent component;
                    if (groupId.equals(appId)) {
                        //it is an application
                        component = TopologyManager.getTopology().
                                getApplication(appId);
                    } else {
                        //it is a group
                        component = TopologyManager.getTopology().
                                getApplication(appId).getGroupRecursively(groupId);
                    }
                    Map<String, ClusterDataHolder> clusterIds = component.getClusterDataMap();
                    Map<String, Group> groups = component.getAliasToGroupMap();
                    updateChildStatus(appId, idOfChild, groups, clusterIds, component);
                } finally {
                    TopologyManager.releaseReadLockForApplication(appId);

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
        ClusterStatus clusterStatus;
        GroupStatus groupStatus;
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
            clusterStatus = getClusterStatus(clusterData);
            groupStatus = getGroupStatus(groups);
            try {
                TopologyManager.acquireReadLockForApplication(appId);
                Application application = TopologyManager.getTopology().getApplication(appId);

                if (groups.isEmpty() && clusterStatus == ClusterStatus.Active ||
                        clusterData.isEmpty() && groupStatus == GroupStatus.Active ||
                        groupStatus == GroupStatus.Active && clusterStatus == ClusterStatus.Active) {
                    //send activation event
                    if (parent instanceof Application) {
                        //send application activated event
                        log.info("sending app activate: " + appId);
                        StatusEventPublisher.sendApplicationActivatedEvent(appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group activate: " + parent.getUniqueIdentifier());
                        StatusEventPublisher.sendGroupActivatedEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && clusterStatus == ClusterStatus.Inactive ||
                        clusterData.isEmpty() && groupStatus == GroupStatus.Inactive ||
                        groupStatus == GroupStatus.Inactive && clusterStatus == ClusterStatus.Inactive) {
                    //send the in activation event
                    if (parent instanceof Application) {
                        //send application activated event
                        log.warn("Application can't be in in-active : " + appId);
                        //StatusEventPublisher.sendApplicationInactivatedEvent(appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group in-active: " + parent.getUniqueIdentifier());
                        StatusEventPublisher.sendGroupInActivateEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && clusterStatus == ClusterStatus.Terminated ||
                        clusterData.isEmpty() && groupStatus == GroupStatus.Terminated ||
                        groupStatus == GroupStatus.Terminated && clusterStatus == ClusterStatus.Terminated) {
                    //send the terminated event
                    if (parent instanceof Application) {
                        //validating the life cycle
                        if (application.getStatus().equals(ApplicationStatus.Terminating)) {
                            log.info("sending app terminated: " + appId);
                            StatusEventPublisher.sendApplicationTerminatedEvent(appId, parent.getClusterDataRecursively());
                        } else {
                            log.info("[Application] " + appId + " is in the [status] " +
                                    application.getStatus().toString() + ". Hence not sending terminated event");
                        }
                        //StatusEventPublisher.sendApp(appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group created : " + parent.getUniqueIdentifier());
                        StatusEventPublisher.sendGroupCreatedEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && clusterStatus == ClusterStatus.Terminating ||
                        clusterData.isEmpty() && groupStatus == GroupStatus.Terminating ||
                        groupStatus == GroupStatus.Terminating && clusterStatus == ClusterStatus.Terminating) {
                    if (parent instanceof Application) {
                        log.info("Application can't be in terminating: " + appId);
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group terminating : " + parent.getUniqueIdentifier());
                        StatusEventPublisher.sendGroupTerminatingEvent(appId, parent.getUniqueIdentifier());
                    }
                } else if (groups.isEmpty() && clusterStatus == ClusterStatus.Created ||
                        clusterData.isEmpty() && groupStatus == GroupStatus.Created ||
                        groupStatus == GroupStatus.Created && clusterStatus == ClusterStatus.Created) {
                    if (parent instanceof Application) {
                        log.info("[Application] " + appId + "couldn't change to Created, since it is" +
                                "already in " + application.getStatus().toString());
                    } else if (parent instanceof Group) {
                        //send activation to the parent
                        log.info("sending group created : " + parent.getUniqueIdentifier());
                        StatusEventPublisher.sendGroupCreatedEvent(appId, parent.getUniqueIdentifier());
                    }
                } else {
                    log.warn("Clusters/groups not found in this [component] " + appId);
                }
            } finally {
                TopologyManager.releaseReadLockForApplication(appId);
            }


            return childFound;
        } else {
            log.warn("There is no child found in the [group/cluster] " + id + " found in the " +
                    "[component]" + parent.getUniqueIdentifier());
        }
        return childFound;
    }

    private GroupStatus getGroupStatus(Map<String, Group> groups) {
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
            } else if (group.getStatus() == GroupStatus.Terminating) {
                groupActive = false;
                groupTerminated = false;
                groupCreated = false;
                status = GroupStatus.Terminating;

            }
        }

        if (groupActive) {
            status = GroupStatus.Active;
        } else if (groupTerminated) {
            status = GroupStatus.Terminated;
        } else if(groupCreated) {
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
            } else if (cluster.getStatus() == ClusterStatus.Terminating) {
                status = ClusterStatus.Terminating;
                clusterActive = false;
                clusterTerminated = false;
                clusterCreated = false;
            } else if (cluster.getStatus() == ClusterStatus.Created) {
                clusterActive = false;
                clusterTerminated = false;
                clusterCreated = clusterCreated && true;
            }
        }

        if (clusterActive) {
            status = ClusterStatus.Active;
        } else if (clusterTerminated) {
            status = ClusterStatus.Terminated;
        } else if(clusterCreated) {
            status = ClusterStatus.Created;
        }
        return status;
    }

    private static class Holder {
        private static final StatusChecker INSTANCE = new StatusChecker();
    }

}
