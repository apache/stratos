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
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.topology.Application;
import org.apache.stratos.messaging.domain.topology.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ApplicationMonitor is to control the child monitors
 */
public class ApplicationMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ApplicationMonitor.class);

    public ApplicationMonitor(Application application) throws DependencyBuilderException,
                                                        TopologyInConsistentException {
        super(application);
        //starting the first set of dependencies from its children
        this.id = application.getUniqueIdentifier();
        startDependency();
    }

    /**
     * To find all the clusters of an application
     *
     * @param appId the application which contains the clusters
     * @return all the clusters of the application
     */
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
     * @param clusterId cluster id of the monitor to be searched
     * @return the found cluster monitor
     */
    public AbstractClusterMonitor findClusterMonitorWithId(String clusterId) {
        return findClusterMonitor(clusterId, clusterIdToClusterMonitorsMap.values(),
                aliasToGroupMonitorsMap.values());

    }

    /**
     * utility method to recursively search for cluster monitors in the App monitor
     *
     * @param clusterId       cluster id of the monitor to be searched
     * @param clusterMonitors cluster monitors found in the app Monitor
     * @param groupMonitors   group monitors found in the app monitor
     * @return the found cluster monitor
     */
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

    /**
     * Find the group monitor by traversing recursively in the hierarchical monitors.
     *
     * @param groupId the unique alias of the Group
     * @return the found GroupMonitor
     */
    public GroupMonitor findGroupMonitorWithId(String groupId) {
        return findGroupMonitor(groupId, aliasToGroupMonitorsMap.values());

    }


    /**
     * Utility method to find the group monitor recursively within app monitor
     *
     * @param id       the unique alias of the Group
     * @param monitors the group monitors found in the app monitor
     * @return the found GroupMonitor
     */
    private GroupMonitor findGroupMonitor(String id, Collection<GroupMonitor> monitors) {
        for (GroupMonitor monitor : monitors) {
            // check if alias is equal, if so, return
            if (monitor.getId().equals(id)) {
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


    /**
     * To find the parent monitor of a group's associate monitor
     *
     * @param groupId the id of the group
     * @return the found parent monitor of the group
     */
    public Monitor findParentMonitorOfGroup(String groupId) {
        return findParentMonitorForGroup(groupId, this);
    }

    /**
     * Find the parent monitor of the given group in the app monitor
     *
     * @param groupId the id of the group
     * @param monitor the app monitor
     * @return the found parent monitor of the group
     */
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

    /**
     * Find the parent monitor of the given cluster in the app monitor
     *
     * @param clusterId the id of the cluster
     * @return the found parent monitor of the cluster
     */
    public Monitor findParentMonitorOfCluster(String clusterId) {
        return findParentMonitorForCluster(clusterId, this);
    }

    /**
     * Find the parent monitor of the given cluster in the app monitor
     *
     * @param clusterId the id of the cluster
     * @param monitor   the app monitor
     * @return the found parent monitor of the cluster
     */
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

    /**
     * To set the status of the application monitor
     *
     * @param status the status
     */
    public void setStatus(Status status) {
        log.info(String.format("[ApplicationMonitor] %s " +
                "state changes from %s to %s", id, this.status, status));
        this.status = status;
    }

    @Override
    public void onEvent(MonitorStatusEvent statusEvent) {
        monitor(statusEvent);
    }

    @Override
    protected void monitor(MonitorStatusEvent statusEvent) {
        /*ApplicationContext context = this.dependencyTree.
                findApplicationContextWithId(statusEvent.getId());
        //TODO remove activated
        if(context.getStatusLifeCycle().isEmpty() || context.getStatus() == Status.Activated) {
            try {
                //if life cycle is empty, need to start the monitor
                boolean dependencyStarted = startDependency(statusEvent.getId());
                if(!dependencyStarted) {
                    //Have to check whether all other dependencies started

                }
                //updating the life cycle
                context.addStatusToLIfeCycle(statusEvent.getStatus());
            } catch (TopologyInConsistentException e) {
                //TODO revert the siblings
                log.error(e);
            }
        } else {
            //TODO act based on life cycle events
        }*/

        String id = statusEvent.getId();
        ApplicationContext context = this.dependencyTree.
                findApplicationContextWithId(id);
        if(context.getStatusLifeCycle().isEmpty()) {
            try {
                //if life cycle is empty, need to start the monitor
                boolean startDep = startDependency(statusEvent.getId());
                if(log.isDebugEnabled()) {
                    log.debug("started a child: " + startDep + " by the group/cluster: " + id);

                }
                //updating the life cycle and current status
                context.setStatus(statusEvent.getStatus());
                context.addStatusToLIfeCycle(statusEvent.getStatus());
                if(!startDep) {
                    //Checking in the children whether all are active,
                    // since no dependency found to be started.
                    StatusChecker.getInstance().onChildStatusChange(id, this.appId);
                }
            } catch (TopologyInConsistentException e) {
                //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
                log.error(e);
            }
        } else {
            //TODO act based on life cycle events
        }


    }
}
