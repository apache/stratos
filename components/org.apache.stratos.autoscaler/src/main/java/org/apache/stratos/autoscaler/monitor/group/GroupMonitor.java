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
package org.apache.stratos.autoscaler.monitor.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.grouping.topic.StatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.*;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.domain.topology.GroupStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.ArrayList;
import java.util.List;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends ParentComponentMonitor implements EventHandler {
    private static final Log log = LogFactory.getLog(GroupMonitor.class);
    //status of the monitor whether it is running/in_maintainable/terminated
    private GroupStatus status;

    /**
     * Constructor of GroupMonitor
     *
     * @param group Takes the group from the Topology
     * @throws DependencyBuilderException    throws when couldn't build the Topology
     * @throws TopologyInConsistentException throws when topology is inconsistent
     */
    public GroupMonitor(Group group, String appId) throws DependencyBuilderException,
            TopologyInConsistentException {
        super(group);
        this.appId = appId;
        this.setStatus(group.getStatus());
        startDependency();
    }

    @Override
    public void onEvent(MonitorStatusEvent statusEvent) {
        monitor(statusEvent);
    }

    @Override
    public void onEvent(MonitorTerminateAllEvent terminateAllEvent) {
        this.terminateChildren = true;

    }

    @Override
    public void onEvent(MonitorScalingEvent scalingEvent) {

    }

    @Override
    protected void monitor(MonitorStatusEvent statusEvent) {
        String id = statusEvent.getId();
        LifeCycleState status1 = statusEvent.getStatus();
        ApplicationContext context = this.dependencyTree.findApplicationContextWithId(id);
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination
        if (!isParent(id)) {
            if (status1 == ClusterStatus.Active || status1 == GroupStatus.Active) {
                try {
                    //if the activated monitor is in in_active map move it to active map
                    if(this.aliasToInActiveMonitorsMap.containsKey(id)) {
                        this.aliasToActiveMonitorsMap.put(id, this.aliasToInActiveMonitorsMap.remove(id));
                    }
                    boolean startDep = startDependency(statusEvent.getId());
                    if (log.isDebugEnabled()) {
                        log.debug("started a child: " + startDep + " by the group/cluster: " + id);

                    }
                    if (!startDep) {
                        StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
                    }
                } catch (TopologyInConsistentException e) {
                    //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
                    log.error(e);
                }

            } else if (status1 == ClusterStatus.Inactive || status1 == GroupStatus.Inactive) {

                List<ApplicationContext> terminationList;
                Monitor monitor;
                terminationList = this.dependencyTree.getTerminationDependencies(id);
                //Temporarily move the group/cluster to inactive list
                this.aliasToInActiveMonitorsMap.put(id, this.aliasToActiveMonitorsMap.remove(id));

                if (terminationList != null) {
                    //Checking the termination dependents status
                    for (ApplicationContext terminationContext : terminationList) {
                        //Check whether dependent is in_active, then start to kill it
                        monitor = this.aliasToActiveMonitorsMap.
                                get(terminationContext.getId());
                        //start to kill it
                        if(monitor.hasMonitors()) {
                            //it is a group
                            StatusEventPublisher.sendGroupTerminatingEvent(this.appId, terminationContext.getId());
                        } else {
                            StatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                                    ((AbstractClusterMonitor)monitor).getServiceId(), terminationContext.getId());

                        }
                    }
                } else {
                log.warn("Wrong inActive event received from [Child] " + id + "  to the [parent]"
                    + " where child is identified as a independent");
                /*//find any other immediate dependent which is in_active/created state
                ApplicationContext context1 = this.dependencyTree.findParentContextWithId(id);
                if(context1 != null) {
                    if(this.aliasToInActiveMonitorsMap.containsKey(context1.getId())) {
                        monitor = this.aliasToInActiveMonitorsMap.get(id);
                        //killall
                        monitor.onEvent(new MonitorTerminateAllEvent(id));

                    } else {
                        log.warn("Wrong inActive event received from [Child] " + id + "  to the [parent]"
                        + " where child is identified as a independent");
                    }
                }*/
                }
                //To update the status of the Group
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);

            } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
                //Check whether hasDependent true
                if(!this.aliasToInActiveMonitorsMap.containsKey(id)) {
                    this.aliasToInActiveMonitorsMap.put(id, this.aliasToActiveMonitorsMap.remove(id));
                }

                Monitor monitor = this.aliasToInActiveMonitorsMap.get(id);
                for(Monitor monitor1 : monitor.getAliasToActiveMonitorsMap().values()) {
                    if(monitor.hasMonitors()) {
                        StatusEventPublisher.sendGroupTerminatingEvent(this.appId, monitor1.getId());
                    } else {
                        StatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                                ((AbstractClusterMonitor)monitor1).getServiceId(), monitor.getId());
                    }
                }
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
            } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
                //Check whether all dependent goes Terminated and then start them in parallel.
                this.aliasToInActiveMonitorsMap.remove(id);
                if(this.status != GroupStatus.Terminating) {
                    List<ApplicationContext> terminationList;
                    boolean allDependentTerminated = true;
                    terminationList = this.dependencyTree.getTerminationDependencies(id);
                    if(terminationList != null) {
                        for(ApplicationContext context1 : terminationList) {
                            if(this.aliasToInActiveMonitorsMap.containsKey(context1.getId())) {
                                log.info("Waiting for the [Parent Monitor] " + context1.getId()
                                        + " to be terminated");
                                allDependentTerminated = false;
                            } else if(this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                                log.warn("Dependent [monitor] " + context1.getId() + " not in the correct state");
                                allDependentTerminated = false;
                            } else {
                                allDependentTerminated = true;
                            }
                        }

                        if(allDependentTerminated) {

                        }
                    } else {
                        List<ApplicationContext> parentContexts = this.dependencyTree.findAllParentContextWithId(id);
                        boolean canStart = false;
                        if(parentContexts != null) {
                            for(ApplicationContext context1 : parentContexts) {
                                if(this.aliasToInActiveMonitorsMap.containsKey(context1.getId())) {
                                    log.info("Waiting for the [Parent Monitor] " + context1.getId()
                                            + " to be terminated");
                                    canStart = false;
                                } else if(this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                                    if(canStart) {
                                        log.warn("Found the Dependent [monitor] " + context1.getId()
                                                + " in the active list wrong state");
                                    }
                                } else {
                                    log.info("[Parent Monitor] " + context1.getId()
                                            + " has already been terminated");
                                    canStart = true;
                                }
                            }

                            if(canStart) {
                                //start the monitor
                            }

                        } else {
                           //Start the monitor
                        }

                    }
                } else {
                    StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
                    log.info("Executing the un-subscription request for the [monitor] " + id);
                }
            }
        }
    }

    public ParentComponentMonitor getParent() {
        return parent;
    }

    public void setParent(ParentComponentMonitor parent) {
        this.parent = parent;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    private boolean isParent(String id) {
        if (this.parent.getId().equals(id)) {
            return true;
        } else {
            return false;
        }
    }

    public GroupStatus getStatus() {
        return status;
    }

    /**
     * Will set the status of the monitor based on Topology Group status/child status like scaling
     *
     * @param status
     */
    public void setStatus(GroupStatus status) {
        log.info(String.format("[Monitor] %s is notifying the parent" +
                "on its state change from %s to %s", id, this.status, status));
        this.status = status;
        //notifying the parent
        MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent, this.status, this.id);
    }
}
