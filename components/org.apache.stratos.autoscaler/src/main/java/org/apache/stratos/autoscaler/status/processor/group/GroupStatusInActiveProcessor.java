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
import org.apache.stratos.messaging.domain.applications.*;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.Map;

/**
 * Cluster active status processor
 */
public class GroupStatusInActiveProcessor extends GroupStatusProcessor {
    private static final Log log = LogFactory.getLog(GroupStatusInActiveProcessor.class);
    private GroupStatusProcessor nextProcessor;

    @Override
    public void setNext(StatusProcessor nextProcessor) {
        this.nextProcessor = (GroupStatusProcessor) nextProcessor;
    }

    @Override
    public boolean process(String idOfComponent, String appId,
                           String instanceId) {
        boolean statusChanged;
        statusChanged = doProcess(idOfComponent, appId, instanceId);
        if (statusChanged) {
            return statusChanged;
        }

        if (nextProcessor != null) {
            // ask the next processor to take care of the message.
            return nextProcessor.process(idOfComponent, appId, instanceId);
        } else {

            log.warn(String.format("No possible state change found for [component] %s [instance]",
                    idOfComponent, instanceId));
        }
        return false;
    }


    private boolean doProcess(String idOfComponent, String appId, String instanceId) {
        ParentComponent component;
        Map<String, Group> groups;
        Map<String, ClusterDataHolder> clusterData;

        if (log.isInfoEnabled()) {
            log.info("StatusChecker calculating the status for the group [ " + idOfComponent + " ]");
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
                    clusterData.isEmpty() && getAllGroupInActive(groups, instanceId) ||
                    getAllClusterInactive(clusterData, instanceId) || getAllGroupInActive(groups, instanceId)) {
                //send the in activation event
                if (component instanceof Application) {
                    //send application activated event
                    log.warn("Sending application instance in-active for [Application] " + appId +
                    " [ApplicationInstance] " + instanceId);
                    ApplicationBuilder.handleApplicationInstanceInactivateEvent(appId, instanceId);

                    return true;
                    //ApplicationBuilder.handleApp(appId);
                } else if (component instanceof Group) {
                    //send activation to the parent
                    if (((Group) component).getStatus(instanceId) != GroupStatus.Inactive) {
                        log.info("sending group in-active: " + component.getUniqueIdentifier());
                        ApplicationBuilder.handleGroupInActivateEvent(appId, component.getUniqueIdentifier(), instanceId);
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
     * Find out whether any of the clusters of a group in the InActive state
     *
     * @param clusterData clusters of the group
     * @return whether inActive or not
     */

    private boolean getAllClusterInactive(Map<String, ClusterDataHolder> clusterData, String instanceId) {
        boolean clusterStat = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            Service service = TopologyManager.getTopology().getService(clusterDataHolderEntry.getValue().getServiceType());
            Cluster cluster = service.getCluster(clusterDataHolderEntry.getValue().getClusterId());
            ClusterInstance context = cluster.getInstanceContexts(instanceId);
            if (context.getStatus() == ClusterStatus.Inactive) {
                clusterStat = true;
                return clusterStat;
            } else {
                clusterStat = false;

            }
        }
        return clusterStat;
    }


    /**
     * Find out whether all the any group is inActive
     *
     * @param groups groups of a group/application
     * @return whether inActive or not
     */
    private boolean getAllGroupInActive(Map<String, Group> groups, String instanceId) {
        boolean groupStat = false;
        for (Group group : groups.values()) {
            GroupInstance context = group.getInstanceContexts(instanceId);
            if (context != null && context.getStatus() == GroupStatus.Inactive) {
                groupStat = true;
                return groupStat;
            } else {
                groupStat = false;
            }
            //TODO get by parent
        }
        return groupStat;
    }


}
