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
package org.apache.stratos.autoscaler.status.processor.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.status.processor.StatusProcessor;
import org.apache.stratos.messaging.domain.application.*;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;
import java.util.Map;

/**
 * Cluster in-active status processor
 */
public class GroupStatusInactiveProcessor extends GroupStatusProcessor {
    private static final Log log = LogFactory.getLog(GroupStatusInactiveProcessor.class);
    private GroupStatusProcessor nextProcessor;

    @Override
    public void setNext(StatusProcessor nextProcessor) {
        this.nextProcessor = (GroupStatusProcessor) nextProcessor;
    }

    @Override
    public boolean process(String idOfComponent, String appId, String instanceId) {
        boolean statusChanged;
        statusChanged = doProcess(idOfComponent, appId, instanceId);
        if (statusChanged) {
            return true;
        }

        if (nextProcessor != null) {
            // ask the next processor to take care of the message.
            return nextProcessor.process(idOfComponent, appId, instanceId);
        } else {
            log.warn(String.format("No possible state change found for [component] %s [instance] %s",
                    idOfComponent, instanceId));
        }
        return false;
    }


    private boolean doProcess(String idOfComponent, String appId, String instanceId) {
        ParentComponent component;
        Map<String, Group> groups;
        Map<String, ClusterDataHolder> clusterData;

        if (log.isDebugEnabled()) {
            log.debug("StatusChecker calculating the inactive status for the group " +
                    "[ " + idOfComponent + " ] " + " for the instance " + " [ " + instanceId + " ]");
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
            //finding all the children of the application/group
            groups = component.getAliasToGroupMap();
            clusterData = component.getClusterDataMap();

            if (groups.isEmpty() && getAllClusterInactive(clusterData, instanceId) ||
                    clusterData.isEmpty() && getAllGroupInactive(groups, instanceId) ||
                    getAllClusterInactive(clusterData, instanceId) ||
                    getAllGroupInactive(groups, instanceId)) {
                //send the in-activation event
                if (component instanceof Application) {
                    //send application in-activated event
                    log.warn("Sending application instance inactive for [Application] " + appId +
                            " [ApplicationInstance] " + instanceId);
                    ApplicationBuilder.handleApplicationInstanceInactivateEvent(appId, instanceId);

                    return true;
                } else {
                    //send group in-activated event
                    if (((Group) component).getStatus(instanceId) != GroupStatus.Inactive) {
                        log.info("Sending group instance Inactive for [group] " +
                                component.getUniqueIdentifier() + " [instance] " + instanceId);
                        ApplicationBuilder.handleGroupInactivateEvent(appId,
                                component.getUniqueIdentifier(), instanceId);
                        return true;
                    }
                }
            }
        } finally {
            ApplicationHolder.releaseWriteLock();

        }
        return false;
    }


    /**
     * Find out whether any of the clusters of a group in the Inactive state
     *
     * @param clusterData clusters of the group
     * @return whether inactive or not
     */

    private boolean getAllClusterInactive(Map<String, ClusterDataHolder> clusterData,
                                          String instanceId) {
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            Service service = TopologyManager.getTopology().
                    getService(clusterDataHolderEntry.getValue().getServiceType());
            Cluster cluster = service.getCluster(clusterDataHolderEntry.getValue().getClusterId());
            ClusterInstance context = cluster.getInstanceContexts(instanceId);
            if (context != null) {
                if (context.getStatus() == ClusterStatus.Inactive) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Find out whether all the any group is inactive
     *
     * @param groups groups of a group/application
     * @return whether inactive or not
     */
    private boolean getAllGroupInactive(Map<String, Group> groups, String instanceId) {
        for (Group group : groups.values()) {
            GroupInstance context = group.getInstanceContexts(instanceId);
            if (context != null) {
                if (context.getStatus() == GroupStatus.Inactive) {
                    return true;
                }
            } else {
                List<Instance> instanceList = group.getInstanceContextsWithParentId(instanceId);
                //if no instances found and requested status is terminated,
                // then considering this group as terminated
                if (instanceList != null && !instanceList.isEmpty()) {
                    int sameStateInstances = 0;
                    for (Instance context1 : instanceList) {
                        if (((GroupInstance) context1).getStatus().equals(GroupStatus.Inactive)) {
                            sameStateInstances++;
                        }
                    }
                    if (sameStateInstances >= 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
