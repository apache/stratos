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
     *
     * @param clusterId1
     */
    public void onMemberStatusChange(String clusterId1) {
        final String clusterId = clusterId1;
        Runnable exCluster = new Runnable() {
            public void run() {
                ClusterMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
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
                        }
                    }
                }
                // if active then notify upper layer
                if (clusterActive) {
                    //send event to cluster status topic
                    StatusEventPublisher.sendClusterActivatedEvent(monitor.getAppId(),
                            monitor.getServiceId(), monitor.getClusterId());

                }

            }
        };
        Thread clusterThread = new Thread(exCluster);
        clusterThread.start();
    }

    /**
     *
     * @param idOfChild
     * @param groupId
     * @param appId
     */
    public void onChildStatusChange(final String idOfChild, final String groupId,
                                    final String appId) {
        updateChild(idOfChild, groupId, appId);
    }

    /**
     * This will calculate whether the children of an application are active or not. If active, then
     * it will send the ApplicationActivatedEvent.
     * @param idOfChild
     * @param appId
     */
    public void onChildStatusChange(final String idOfChild, final String appId) {
        updateChild(idOfChild, appId);
    }

    private void updateChild(final String idOfChild, final String groupId, final String appId) {
        Runnable group = new Runnable() {
            public void run() {
                try {
                    //TODO getting lock
                    TopologyManager.acquireReadLockForApplication(appId);
                    ParentComponent component1 = TopologyManager.getTopology().
                            getApplication(appId).getGroupRecursively(groupId);
                    Map<String, ClusterDataHolder> clusterIds = component1.getClusterDataMap();
                    Map<String, Group> groups = component1.getAliasToGroupMap();
                    updateChildStatus(appId, idOfChild, groups, clusterIds, component1);
                } finally {
                    TopologyManager.releaseReadLockForApplication(appId);

                }

            }
        };
        Thread groupThread = new Thread(group);
        groupThread.start();
    }

    private void updateChild(final String idOfChild, final String appId) {
        Runnable group = new Runnable() {
            public void run() {
                try {
                    //TODO getting lock
                    TopologyManager.acquireReadLockForApplication(appId);
                    ParentComponent component = TopologyManager.getTopology().
                            getApplication(appId);
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
     * @param clusterId
     * @param appId
     * @param partitionContext is to decide in which partition has less members while others have active members
     */
    public void onMemberFaultEvent(final String clusterId, final String appId, final PartitionContext partitionContext) {
        Runnable memberFault = new Runnable() {
            public void run() {
                ClusterMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                boolean clusterActive = false;
                boolean clusterInMaintenance = false;
                for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
                    for (PartitionContext partition : networkPartitionContext.getPartitionCtxts().values()) {
                        if (partitionContext.getPartitionId().equals(partition.getPartitionId()) &&
                                partition.getActiveMemberCount() < partition.getMinimumMemberCount()) {
                            clusterInMaintenance = true;
                        } else {
                            log.info(String.format("Hence the [partition] %s, in [networkpartition], " +
                                            "%s has exceeded the [minimum], %d with current active " +
                                            "[members], %d the [cluster], %s is still in active mode."
                                    , partition.getPartitionId(), partition.getNetworkPartitionId(),
                                    partition.getMinimumMemberCount(), partition.getActiveMemberCount(), clusterId));
                        }
                        if (partitionContext.getMinimumMemberCount() >= partitionContext.getActiveMemberCount()) {
                            clusterActive = true;
                        }
                        clusterActive = false;
                    }

                }
                // if in maintenance then notify upper layer
                if (clusterActive && clusterInMaintenance) {
                    //send clusterInmaintenance event to cluster status topic

                }

            }
        };
        Thread faultHandlingThread = new Thread(memberFault);
        faultHandlingThread.start();
    }

    /**
     * This will use to calculate whether  all children of a particular component is active by travesing Top
     * @param appId
     * @param id
     * @param groups
     * @param clusterData
     * @param parent
     * @return
     */
    private boolean updateChildStatus(String appId, String id, Map<String, Group> groups,
                                      Map<String, ClusterDataHolder> clusterData, ParentComponent parent) {
        boolean groupActive = false;
        boolean clustersActive;
        boolean groupsActive;
        boolean childFound = false;
        boolean clusterFound = false;

        for(ClusterDataHolder clusterDataHolder : clusterData.values()) {
            if(clusterDataHolder.getClusterId().equals(id)) {
                clusterFound = true;
            }
        }
        log.info("cluster found: " + clusterFound);
        if (clusterFound || groups.containsKey(id)) {
            childFound = true;
            if (!clusterData.isEmpty() && !groups.isEmpty()) {
                if(log.isDebugEnabled()) {
                    log.debug("group active found: " + clusterFound);
                }
                clustersActive = getClusterStatus(clusterData);
                groupsActive = getGroupStatus(groups);
                if(log.isDebugEnabled()) {
                    log.debug("Active cluster" + clustersActive + " and group: " + groupActive);
                }
                groupActive = clustersActive && groupsActive;
            } else if (!groups.isEmpty()) {
                groupsActive = getGroupStatus(groups);
                if(log.isDebugEnabled()) {
                    log.info("group active found: " + clusterFound);
                }
                groupActive = groupsActive;
            } else if (!clusterData.isEmpty()) {
                clustersActive = getClusterStatus(clusterData);
                if(log.isDebugEnabled()) {
                    log.debug("Active cluster" + clustersActive + " and group: " + groupActive);
                }
                groupActive = clustersActive;
            } else {
                log.warn("Clusters/groups not found in this [component] "+ appId);
            }
            //send the activation event
            if (parent instanceof Application && groupActive) {
                //send application activated event
                log.info("sending app activate found: " + appId);
                StatusEventPublisher.sendApplicationActivatedEvent(appId);
            } else if (parent instanceof Group && groupActive) {
                //send activation to the parent
                log.info("sending group activate found: " + parent.getUniqueIdentifier());
                StatusEventPublisher.sendGroupActivatedEvent(appId, parent.getUniqueIdentifier());
            }
            return childFound;
        } else {
            log.warn("There is no child found in the [group/cluster] " + id + " found in the " +
                    "[component]" + parent.getUniqueIdentifier());
        }
        return childFound;
    }

    private boolean getGroupStatus(Map<String, Group> groups) {
        boolean groupActiveStatus = false;
        for (Group group : groups.values()) {
            if (group.getStatus() == Status.Activated) {
                groupActiveStatus = true;
            } else {
                groupActiveStatus = false;
            }
        }
        return groupActiveStatus;

    }

    private boolean getClusterStatus(Map<String, ClusterDataHolder> clusterData) {
        boolean clusterActiveStatus = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            Service service = TopologyManager.getTopology().getService(clusterDataHolderEntry.getValue().getServiceType());
            if (service.getCluster(clusterDataHolderEntry.getValue().getClusterId()).
                    getStatus() == Status.Activated) {
                clusterActiveStatus = true;
            } else {
                clusterActiveStatus = false;
            }
        }
        return clusterActiveStatus;
    }

    private static class Holder {
        private static final StatusChecker INSTANCE = new StatusChecker();
    }

}
