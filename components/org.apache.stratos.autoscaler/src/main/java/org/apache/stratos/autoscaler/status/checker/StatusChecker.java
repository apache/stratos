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
import org.apache.stratos.messaging.domain.topology.util.GroupStatus;
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
     * @param groupId
     * @param appId
     */
    public void onGroupStatusChange(final String groupId, final String appId) {
        updateChild(groupId, appId);
    }

    /**
     * @param clusterId
     * @param appId
     */
    public void onClusterStatusChange(final String clusterId, final String appId) {
        updateChild(clusterId, appId);
    }

    private void updateChild(final String clusterId, final String appId) {
        Runnable group = new Runnable() {
            public void run() {
                Application application = TopologyManager.getTopology().getApplication(appId);
                Map<String, ClusterDataHolder> clusterIds = application.getClusterDataMap();
                Map<String, Group> groups = application.getAliasToGroupMap();
                updateChildStatus(appId, clusterId, groups, clusterIds, application);
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

    private boolean updateChildStatus(String appId, String id, Map<String, Group> groups,
                                      Map<String, ClusterDataHolder> clusterData, ParentBehavior parent) {
        boolean groupActive = false;
        boolean clustersActive;
        boolean groupsActive;
        boolean childFound = false;

        if (clusterData.containsValue(id) || groups.containsKey(id)) {
            childFound = true;
            if (!clusterData.isEmpty() && !groups.isEmpty()) {
                clustersActive = getClusterStatus(clusterData);
                groupsActive = getGroupStatus(groups);
                groupActive = clustersActive && groupsActive;
            } else if (!groups.isEmpty()) {
                groupsActive = getGroupStatus(groups);
                groupActive = groupsActive;
            } else if (!clusterData.isEmpty()) {
                clustersActive = getClusterStatus(clusterData);
                groupActive = clustersActive;
            } else {
                log.warn("Clusters/groups not found in this [component] "+ appId);
            }
            //send the activation event
            if (parent instanceof Application && groupActive) {
                //send application activated event
                StatusEventPublisher.sendApplicationActivatedEvent(appId);
            } else if (parent instanceof Group && groupActive) {
                //send activation to the parent
                StatusEventPublisher.sendGroupActivatedEvent(appId, ((Group) parent).getAlias());
            }
            return childFound;
        } else {
            if (!groups.isEmpty()) {
                for (Group group : groups.values()) {
                    return updateChildStatus(appId, id, group.getAliasToGroupMap(), group.getClusterDataMap(), group);

                }
            }
        }
        return childFound;
    }

    private boolean getGroupStatus(Map<String, Group> groups) {
        boolean groupActiveStatus = false;
        for (Group group : groups.values()) {
            if (group.getStatus().equals(GroupStatus.Active)) {
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
                    getStatus().equals(ClusterStatus.Active)) {
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
