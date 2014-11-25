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
import org.apache.stratos.messaging.domain.instance.context.ClusterInstanceContext;
import org.apache.stratos.messaging.domain.instance.context.GroupInstanceContext;
import org.apache.stratos.messaging.domain.instance.context.InstanceContext;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.topology.ClusterInstanceCreatedEvent;
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
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster activated event for [application]: "
                                + monitor.getAppId() + " [cluster]: " + clusterId);
                    }
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
    public void onMemberTermination(final String clusterId, final String instanceId) {
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
                                if (!clusterMonitorHasMembers && cluster.getStatus(null) == ClusterStatus.Terminating) {
                                    if (application.getStatus(null) == ApplicationStatus.Terminating) {
                                        if (log.isInfoEnabled()) {
                                            log.info("Publishing Cluster terminated event for [application]: " + appId +
                                                    " [cluster]: " + clusterId);
                                        }
                                        ClusterStatusEventPublisher.sendClusterTerminatedEvent(appId, monitor.getServiceId(),
                                                monitor.getClusterId(), instanceId);
                                    } else {
                                        if (log.isInfoEnabled()) {
                                            log.info("Publishing Cluster created event for [application]: " + appId +
                                                    " [cluster]: " + clusterId);
                                        }
                                        ClusterStatusEventPublisher.sendClusterResetEvent(appId, monitor.getServiceId(),
                                                monitor.getClusterId(), instanceId);
                                    }

                                } else {
                                    //if the cluster is not active and, if it is in Active state
                                    if (!clusterActive && cluster.getStatus(null) == ClusterStatus.Active) {
                                        if (log.isInfoEnabled()) {
                                            log.info("Publishing Cluster in-activate event for [application]: "
                                                    + monitor.getAppId() + " [cluster]: " + clusterId);
                                        }
                                        ClusterStatusEventPublisher.sendClusterInActivateEvent(monitor.getAppId(),
                                                monitor.getServiceId(), clusterId, instanceId);
                                    } else {
                                        log.info("Cluster has non terminated [members] and in the [status] "
                                                + cluster.getStatus(null).toString());
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

    /**
     * Find out whether cluster monitor has any non terminated members
     *
     * @param monitor the cluster monitor
     * @return whether has members or not
     */
    private boolean clusterMonitorHasMembers(VMClusterMonitor monitor) {
        boolean hasMember = false;
        for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
            //minimum check per partition
            for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts().values()) {
                if (partitionContext.getNonTerminatedMemberCount() > 0) {
                    hasMember = true;
                } else {
                    hasMember = false;
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
    public void onMemberFaultEvent(final String clusterId, final String partitionId, final String instanceId) {
        Runnable group = new Runnable() {
            public void run() {
                VMClusterMonitor monitor = (VMClusterMonitor) AutoscalerContext.getInstance().getClusterMonitor(clusterId);
                boolean clusterInActive = getClusterInactive(monitor, partitionId);
                String appId = monitor.getAppId();
                if (clusterInActive) {
                    //if the monitor is dependent, temporarily pausing it
                    if (monitor.hasStartupDependents()) {
                        monitor.setHasFaultyMember(true);
                    }
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster in-activate event for [application]: "
                                + monitor.getAppId() + " [cluster]: " + clusterId);
                    }
                    //send cluster In-Active event to cluster status topic
                    ClusterStatusEventPublisher.sendClusterInActivateEvent(appId,
                            monitor.getServiceId(), clusterId, instanceId);

                } else {
                    boolean clusterActive = clusterActive(monitor);
                    if (clusterActive) {
                        if (log.isInfoEnabled()) {
                            log.info("Publishing Cluster active event for [application]: "
                                    + monitor.getAppId() + " [cluster]: " + clusterId);
                        }
                        ClusterStatusEventPublisher.sendClusterActivatedEvent(appId, monitor.getServiceId(), clusterId);
                    }
                }

            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    /**
     * This will calculate whether all the minimum of partition in a cluster satisfy in order
     * to decide on the cluster status.
     *
     * @param monitor     Cluster monitor of which the status needs to be calculated
     * @param partitionId partition which got the faulty member
     * @return whether cluster inActive or not
     */
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
     * This will use to calculate whether  all children of a particular component is active by traversing Top
     *
     * @param appId         application id
     * @param idOfComponent id of the component to which calculate the status
     * @param idOfChild     children of the component as groups
     */
    public void onChildStatusChange(String idOfChild, String idOfComponent, String appId, String instanceId) {
        ParentComponent component;
        Map<String, Group> groups;
        Map<String, ClusterDataHolder> clusterData;

        if (log.isInfoEnabled()) {
            log.info("StatusChecker calculating the status for the group [ " + idOfChild + " ]");
        }

        try {
            ApplicationHolder.acquireWriteLock();
            if (idOfComponent.equals(appId)) {
                //it is an application
                component = ApplicationHolder.getApplications().
                        getApplication(appId);
            } else {
                //it is a group
                component = ApplicationHolder.getApplications().
                        getApplication(appId).getGroupRecursively(idOfComponent);
            }
            groups = component.getAliasToGroupMap();
            clusterData = component.getClusterDataMap();

            if(component.isGroupScalingEnabled()) {
                //TODO
                handleStateWithGroupScalingEnabled();
            } else {
                handleStateChangeGroupScalingDisabled(component, appId, instanceId, groups, clusterData);
            }
        } finally {
            ApplicationHolder.releaseWriteLock();

        }

    }

    private void handleStateWithGroupScalingEnabled() {

    }

    private void handleStateChangeGroupScalingDisabled(ParentComponent component, String appId,
                                                       String instanceId,
                                                       Map<String, Group> groups,
                                                       Map<String, ClusterDataHolder> clusterData) {
        if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Active, instanceId) ||
                clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Active, instanceId) ||
                getAllClusterInSameState(clusterData, ClusterStatus.Active, instanceId) &&
                        getAllGroupInSameState(groups, GroupStatus.Active, instanceId)) {
            //send activation event
            if (component instanceof Application) {
                //send application activated event
                if (((Application) component).getStatus(null) != ApplicationStatus.Active) {
                    log.info("sending app activate: " + appId);
                    ApplicationBuilder.handleApplicationActivatedEvent(appId, instanceId);
                }
            } else if (component instanceof Group) {
                //send activation to the parent
                if (((Group) component).getStatus(null) != GroupStatus.Active) {
                    log.info("sending group activate: " + component.getUniqueIdentifier());
                    ApplicationBuilder.handleGroupActivatedEvent(appId, component.getUniqueIdentifier(), instanceId);
                }
            }
        } else if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Terminated, instanceId) ||
                clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Terminated, instanceId) ||
                getAllClusterInSameState(clusterData, ClusterStatus.Terminated, instanceId) &&
                        getAllGroupInSameState(groups, GroupStatus.Terminated, instanceId)) {
            //send the terminated event
            if (component instanceof Application) {
                log.info("sending app terminated: " + appId);
                ApplicationBuilder.handleApplicationTerminatedEvent(appId);
            } else if (component instanceof Group) {
                //send activation to the parent
                if (((Group) component).getStatus(null) != GroupStatus.Terminated) {
                    log.info("sending group terminated : " + component.getUniqueIdentifier());
                    ApplicationBuilder.handleGroupTerminatedEvent(appId, component.getUniqueIdentifier(), instanceId);
                }
            }
        } else if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Created, instanceId) ||
                clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Created, instanceId) ||
                getAllClusterInSameState(clusterData, ClusterStatus.Created, instanceId) &&
                        getAllGroupInSameState(groups, GroupStatus.Created, instanceId)) {
            if (component instanceof Application) {
                log.info("[Application] " + appId + "couldn't change to Created, since it is" +
                        "already in " + ((Application) component).getStatus(null).toString());
            } else if (component instanceof Group) {
                //send activation to the parent
                if (((Group) component).getStatus(null) != GroupStatus.Created) {
                    log.info("sending group created : " + component.getUniqueIdentifier());
                    ApplicationBuilder.handleGroupCreatedEvent(appId, component.getUniqueIdentifier(), instanceId);
                }
            }
        } else if (groups.isEmpty() && getAllClusterInactive(clusterData) ||
                clusterData.isEmpty() && getAllGroupInActive(groups) ||
                getAllClusterInactive(clusterData) || getAllGroupInActive(groups)) {
            //send the in activation event
            if (component instanceof Application) {
                //send application activated event
                log.warn("Application can't be in in-active : " + appId);
                //StatusEventPublisher.sendApplicationInactivatedEvent(appId);
            } else if (component instanceof Group) {
                //send activation to the parent
                if (((Group) component).getStatus(null) != GroupStatus.Inactive) {
                    log.info("sending group in-active: " + component.getUniqueIdentifier());
                    ApplicationBuilder.handleGroupInActivateEvent(appId, component.getUniqueIdentifier(), instanceId);
                }
            }
        } else {
            if (component instanceof Application) {
                //send application activated event
                log.warn("Application can't be in in-active : " + appId);
                //StatusEventPublisher.sendApplicationInactivatedEvent(appId);
            } else if (component instanceof Group) {
                //send activation to the parent
                if (((Group) component).getStatus(null) != GroupStatus.Inactive) {
                    log.info("sending group in-active: " + component.getUniqueIdentifier());
                    ApplicationBuilder.handleGroupInActivateEvent(appId, component.getUniqueIdentifier(), "test*****");
                }
            }
        }
    }

    private boolean getAllInstancesOfGroupActive(Group group) {
        int activeGroupInstances = 0;
        for(GroupInstanceContext context : group.getInstanceIdToInstanceContextMap().values()) {
            if(context.getStatus() == GroupStatus.Active) {
                activeGroupInstances++;
            }
        }
        if(activeGroupInstances >= group.getComponentDeploymentPolicy().getMin()) {
            return true;
        }
        return false;
    }

    /**
     * Find out whether all the any group is inActive
     *
     * @param groups groups of a group/application
     * @return whether inActive or not
     */
    private boolean getAllGroupInActive(Map<String, Group> groups) {
        boolean groupStat = false;
        for (Group group : groups.values()) {
            if (group.getStatus(null) == GroupStatus.Inactive) {
                groupStat = true;
                return groupStat;
            } else {
                groupStat = false;
            }
        }
        return groupStat;
    }

    /**
     * Find out whether all the groups of a group in the same state or not
     *
     * @param groups groups of a group/application
     * @param status the state to check in all groups
     * @return whether groups in the given state or not
     */
    private boolean getAllGroupInSameState(Map<String, Group> groups, GroupStatus status, String instanceId) {
        boolean groupStat = false;
        for (Group group : groups.values()) {
            GroupInstanceContext context = group.getInstanceContexts(instanceId);
            if(context != null) {
                if(context.getStatus() == status) {
                    groupStat = true;
                } else {
                    groupStat = false;
                    return groupStat;
                }
            } else {
                groupStat = false;
                return groupStat;
            }
        }
        return groupStat;
    }


    /**
     * Find out whether any of the clusters of a group in the InActive state
     *
     * @param clusterData clusters of the group
     * @return whether inActive or not
     */
    private boolean getAllClusterInactive(Map<String, ClusterDataHolder> clusterData) {
        boolean clusterStat = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            Service service = TopologyManager.getTopology().getService(clusterDataHolderEntry.getValue().getServiceType());
            Cluster cluster = service.getCluster(clusterDataHolderEntry.getValue().getClusterId());
            if (cluster.getStatus(null) == ClusterStatus.Inactive) {
                clusterStat = true;
                return clusterStat;
            } else {
                clusterStat = false;

            }
        }
        return clusterStat;
    }

    /**
     * Find out whether all the clusters of a group are in the same state
     *
     * @param clusterData clusters of the group
     * @param status      the status to check of the group
     * @return whether all groups in the same state or not
     */
    private boolean getAllClusterInSameState(Map<String, ClusterDataHolder> clusterData,
                                             ClusterStatus status, String instanceId) {
        boolean clusterStat = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            String serviceName = clusterDataHolderEntry.getValue().getServiceType();
            String clusterId = clusterDataHolderEntry.getValue().getClusterId();
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            try {
                Service service = TopologyManager.getTopology().getService(serviceName);
                Cluster cluster = service.getCluster(clusterId);
                ClusterInstanceContext context = cluster.getInstanceContexts(instanceId);
                if (context.getStatus() == status) {
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

    private static class Holder {
        private static final StatusChecker INSTANCE = new StatusChecker();
    }

}
