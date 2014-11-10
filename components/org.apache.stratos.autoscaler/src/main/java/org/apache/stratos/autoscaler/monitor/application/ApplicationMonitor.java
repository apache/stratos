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
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.monitor.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.events.ApplicationStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.Collection;

/**
 * ApplicationMonitor is to control the child monitors
 */
public class ApplicationMonitor extends ParentComponentMonitor {
    private static final Log log = LogFactory.getLog(ApplicationMonitor.class);
    //status of the monitor whether it is running/in_maintainable/terminated
    protected ApplicationStatus status;

    public ApplicationMonitor(Application application) throws DependencyBuilderException,
            TopologyInConsistentException {
        super(application);
        //setting the appId for the application
        this.appId = application.getUniqueIdentifier();
        this.status = application.getStatus();
        //starting the first set of dependencies from its children
        startDependency();

    }

    /**
     * To find all the clusters of an application
     *
     * @param appId the application which contains the clusters
     * @return all the clusters of the application
     */
//    public List<String> findClustersOfApplication(String appId) {
//        List<String> clusters = new ArrayList<String>();
//        Set<ClusterDataHolder> clusterData;
//
//        TopologyManager.acquireReadLockForApplication(appId);
//        try {
//            clusterData = TopologyManager.getTopology().getApplication(appId).getClusterDataRecursively();
//
//        } finally {
//            TopologyManager.releaseReadLockForApplication(appId);
//        }
//
//        if (clusterData != null) {
//            for (ClusterDataHolder clusterDataHolder : clusterData) {
//                clusters.add(clusterDataHolder.getClusterId());
//            }
//        }
//
//        return clusters;
//    }

    /**
     * Find the cluster monitor by traversing recursively in the hierarchical monitors.
     *
     * @param clusterId cluster id of the monitor to be searched
     * @return the found cluster monitor
     */
    public AbstractClusterMonitor findClusterMonitorWithId(String clusterId) {
        /*return findClusterMonitor(clusterId, clusterIdToClusterMonitorsMap.values(),
                aliasToActiveMonitorsMap.values());*/
        return null;

    }


    /**
     * Find the group monitor by traversing recursively in the hierarchical monitors.
     *
     * @param groupId the unique alias of the Group
     * @return the found GroupMonitor
     */
    public Monitor findGroupMonitorWithId(String groupId) {
        Monitor monitor;
        //searching within active monitors
        monitor = findGroupMonitor(groupId, aliasToActiveMonitorsMap.values(), true);
        if (monitor == null) {
            //searching within inActive monitors
            monitor = findGroupMonitor(groupId, aliasToInactiveMonitorsMap.values(), false);
        }
        return monitor;
    }


    /**
     * Utility method to find the group monitor recursively within app monitor
     *
     * @param id       the unique alias of the Group
     * @param monitors the group monitors found in the app monitor
     * @return the found GroupMonitor
     */
    private Monitor findGroupMonitor(String id, Collection<Monitor> monitors, boolean active) {
        for (Monitor monitor : monitors) {
            // check if alias is equal, if so, return
            if (monitor.getId().equals(id)) {
                return monitor;
            } else {
                // check if this Group has nested sub Groups. If so, traverse them as well
                if (monitor.getAliasToActiveMonitorsMap() != null && active) {
                    return findGroupMonitor(id, monitor.getAliasToActiveMonitorsMap().values(), active);
                } else if (monitor.getAliasToInActiveMonitorsMap() != null && !active) {
                    return findGroupMonitor(id, monitor.getAliasToInActiveMonitorsMap().values(), active);

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
    public void setStatus(ApplicationStatus status) {
        log.info(String.format("[ApplicationMonitor] %s " +
                "state changes from %s to %s", id, this.status, status));

        this.status = status;
        //notify the children about the state change
        MonitorStatusEventBuilder.notifyChildren(this, new ApplicationStatusEvent(status, appId));
    }

    @Override
    public void onChildEvent(MonitorStatusEvent statusEvent) {
        String id = statusEvent.getId();
        LifeCycleState status1 = statusEvent.getStatus();
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination
        if (status1 == ClusterStatus.Active || status1 == GroupStatus.Active) {
            onChildActivatedEvent(id);

        } else if (status1 == ClusterStatus.Inactive || status1 == GroupStatus.Inactive) {
            onChildInactiveEvent(id);

        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inActive in the map
            this.markMonitorAsInactive(id);

        } else if (status1 == ClusterStatus.Created || status1 == GroupStatus.Created) {
            if (this.aliasToInactiveMonitorsMap.containsKey(id)) {
                this.aliasToInactiveMonitorsMap.remove(id);
            }
            if (this.status == ApplicationStatus.Terminating) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
            } else {
                onChildTerminatedEvent(id);
            }
        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Check whether all dependent goes Terminated and then start them in parallel.
            if(this.aliasToInactiveMonitorsMap.containsKey(id)) {
                this.aliasToInactiveMonitorsMap.remove(id);
            } else {
                log.warn("[monitor] " + id + " cannot be found in the inActive monitors list");
            }

            if (this.status == ApplicationStatus.Terminating || this.status == ApplicationStatus.Terminated) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
                log.info("Executing the un-subscription request for the [monitor] " + id);
            } else {
                onChildTerminatedEvent(id);
            }
        }
    }

    @Override
    public void onParentEvent(MonitorStatusEvent statusEvent) {
        // nothing to do
    }

    @Override
    public void onEvent(MonitorTerminateAllEvent terminateAllEvent) {

    }

    @Override
    public void onEvent(MonitorScalingEvent scalingEvent) {

    }
}
