/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
package org.apache.stratos.autoscaler.monitor.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.messaging.domain.topology.Application;
import org.apache.stratos.messaging.domain.topology.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;

/**
 * ApplicationMonitor is to control the child monitors
 */
public class ApplicationMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ApplicationMonitor.class);

    public ApplicationMonitor(Application application) {
        super(application);
        this.id = application.getId();
        startDependency();
    }

    @Override
    public void update(Observable observable, Object event) {
        if (event instanceof MonitorStatusEvent) {
            MonitorStatusEvent statusEvent = (MonitorStatusEvent) event;
            Status childStatus = statusEvent.getStatus();
            String notifier = statusEvent.getNotifierId();
            log.info(String.format("[Monitor] %s got notified from the [child] %s" +
                    "on its state change from %s to %s", id, notifier, this.status, status));
            if (childStatus == Status.Activated) {
                //update the notifier as active in the dependency tree

                //start the next dependency
                startDependency(notifier);
            }
        }
    }


    /**
     * Find the group monitor by traversing recursively in the hierarchical monitors.
     *
     * @param groupId the unique alias of the Group
     * @return the found GroupMonitor
     */
    public GroupMonitor findGroupMonitorWithId(String groupId) {
        return findGroupMonitor(groupId, aliasToGroupMonitorsMap.values());

    }

    public List<String> findClustersOfApplication(String appId) {
        List<String> clusters = new ArrayList<String>();
        //considering only one level
        for (AbstractClusterMonitor monitor : this.clusterIdToClusterMonitorsMap.values()) {
            clusters.add(monitor.getClusterId());
        }
        //TODO rest
        return clusters;

    }


    /**
     * Find the cluster monitor by traversing recursively in the hierarchical monitors.
     *
     * @param clusterId
     * @return
     */
    public AbstractClusterMonitor findClusterMonitorWithId(String clusterId) {
        return findClusterMonitor(clusterId, clusterIdToClusterMonitorsMap.values(), aliasToGroupMonitorsMap.values());

    }

    private AbstractClusterMonitor findClusterMonitor(String clusterId,
                                                      Collection<AbstractClusterMonitor> clusterMonitors,
                                                      Collection<GroupMonitor> groupMonitors) {
        for (AbstractClusterMonitor monitor : clusterMonitors) {
            // check if alias is equal, if so, return
            if (monitor.equals(clusterId)) {
                return monitor;
            }
        }

        for (GroupMonitor groupMonitor : groupMonitors) {
            return findClusterMonitor(clusterId,
                    groupMonitor.getClusterIdToClusterMonitorsMap().values(),
                    groupMonitor.getAliasToGroupMonitorsMap().values());
        }
        return null;

    }

    private GroupMonitor findGroupMonitor(String id, Collection<GroupMonitor> monitors) {
        for (GroupMonitor monitor : monitors) {
            // check if alias is equal, if so, return
            if (monitor.equals(id)) {
                return monitor;
            } else {
                // check if this Group has nested sub Groups. If so, traverse them as well
                if (monitor.getAliasToGroupMonitorsMap() != null) {
                    return findGroupMonitor(id, monitor.getAliasToGroupMonitorsMap().values());
                }
            }
        }
        return null;
    }


    public Monitor findParentMonitorOfGroup(String groupId) {
        return findParentMonitorForGroup(groupId, this);
    }

    private Monitor findParentMonitorForGroup(String groupId, Monitor monitor) {
        //if this monitor has the group, return it as the parent
        if (monitor.getAliasToGroupMonitorsMap().containsKey(groupId)) {
            return monitor;
        } else {
            if (monitor.getAliasToGroupMonitorsMap() != null) {
                //check whether the children has the group and find its parent
                for (GroupMonitor groupMonitor : monitor.getAliasToGroupMonitorsMap().values()) {
                    return findParentMonitorForGroup(groupId, groupMonitor);

                }
            }
        }
        return null;

    }

    public Monitor findParentMonitorOfCluster(String clusterId) {
        return findParentMonitorForCluster(clusterId, this);
    }

    private Monitor findParentMonitorForCluster(String clusterId, Monitor monitor) {
        //if this monitor has the group, return it as the parent
        if (monitor.getClusterIdToClusterMonitorsMap().containsKey(clusterId)) {
            return monitor;
        } else {
            if (monitor.getAliasToGroupMonitorsMap() != null) {
                //check whether the children has the group and find its parent
                for (GroupMonitor groupMonitor : monitor.getAliasToGroupMonitorsMap().values()) {
                    return findParentMonitorForCluster(clusterId, groupMonitor);

                }
            }
        }
        return null;

    }

    public void setStatus(Status status) {
        log.info(String.format("[ApplicationMonitor] %s " +
                "state changes from %s to %s", id, this.status, status));
        this.status = status;
    }
}
