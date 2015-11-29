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
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.Collection;
import java.util.Map;

/**
 * Cluster active status processor
 */
public class GroupStatusActiveProcessor extends GroupStatusProcessor {
    private static final Log log = LogFactory.getLog(GroupStatusActiveProcessor.class);
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

            log.warn(String.format("No possible state change found for [component] %s, [instance] %s", idOfComponent,
                    instanceId));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean doProcess(String idOfComponent, String appId, String instanceId) {
        ParentComponent component;
        Map<String, Group> groups;
        Map<String, ClusterDataHolder> clusterData;

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "GroupStatusActiveProcessor is checking the status of [application-id] %s, [group-id] %s, "
                            + "[group-instance-id] %s", appId, idOfComponent, instanceId));
        }
        try {
            ApplicationHolder.acquireWriteLock();

            Application application = ApplicationHolder.getApplications().
                    getApplication(appId);
            component = application;
            if (!idOfComponent.equals(appId)) {
                //it is an application
                component = application.getGroupRecursively(idOfComponent);
            }
            //finding all the children of the application/group
            groups = component.getAliasToGroupMap();
            clusterData = component.getClusterDataMap();

            if (groups.isEmpty() && getAllClusterInSameState(clusterData, ClusterStatus.Active, instanceId) ||
                    clusterData.isEmpty() && getAllGroupInSameState(groups, GroupStatus.Active, instanceId) ||
                    getAllClusterInSameState(clusterData, ClusterStatus.Active, instanceId) && getAllGroupInSameState(
                            groups, GroupStatus.Active, instanceId)) {

                if (component instanceof Application) {
                    //send application activated event
                    log.info(String.format(
                            "Sending application instance active event for [application-id] %s, [instance-id] %s",
                            appId, instanceId));
                    ApplicationBuilder.handleApplicationInstanceActivatedEvent(appId, instanceId);
                    return true;
                } else {
                    //send activation to the parent
                    log.info(String.format(
                            "Sending group instance active event for [application-id] %s, [group-id] %s, "
                                    + "[instance-id] %s", appId, component.getUniqueIdentifier(), instanceId));
                    ApplicationBuilder
                            .handleGroupInstanceActivatedEvent(appId, component.getUniqueIdentifier(), instanceId);
                    return true;
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "GroupStatusActiveProcessor did not detect any status change for [application-id] %s, "
                                    + "[group-id] %s, [instance-id] %s", appId, idOfComponent, instanceId));
                    for (Map.Entry<String, Group> entry : groups.entrySet()) {
                        Collection<Group> groupCollection = entry.getValue().getGroups();
                        for (Group group : groupCollection) {
                            for (GroupInstance groupInstance : group.getInstanceIdToInstanceContextMap().values()) {
                                log.debug(String.format("Groups: [group-id] %s, [group-instance-id] %s, [status] %s",
                                        group.getUniqueIdentifier(), groupInstance.getInstanceId(), entry.getKey(),
                                        groupInstance.getStatus()));
                            }

                        }

                    }
                    for (Map.Entry<String, ClusterDataHolder> entry : clusterData.entrySet()) {
                        String serviceName = entry.getValue().getServiceType();
                        String clusterId = entry.getValue().getClusterId();
                        TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
                        try {
                            Service service = TopologyManager.getTopology().getService(serviceName);
                            Cluster cluster = service.getCluster(clusterId);
                            ClusterInstance context = cluster.getInstanceContexts(instanceId);
                            if (context != null) {
                                log.debug(String.format(
                                        "ClusterData: [cluster-id] %s, [cluster-instance-id] %s, [status] %s",
                                        entry.getKey(), instanceId, context.getStatus()));
                            } else {
                                log.debug(String.format(
                                        "ClusterData: cluster instance context is null: [cluster-instance-id] %s",
                                        instanceId));
                            }
                        } finally {
                            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
                        }
                    }
                }
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        return false;
    }

}
