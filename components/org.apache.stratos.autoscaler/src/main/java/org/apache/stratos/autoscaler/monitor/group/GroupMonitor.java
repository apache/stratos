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
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.EventHandler;
import org.apache.stratos.autoscaler.monitor.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.monitor.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.events.GroupStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

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
        this.status = group.getStatus();
        startDependency();
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

        } else if (status1 == ClusterStatus.Created || status1 == GroupStatus.Created) {
            if (this.aliasToInactiveMonitorsMap.containsKey(id)) {
                this.aliasToInactiveMonitorsMap.remove(id);
            }
            if (this.status == GroupStatus.Terminating) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
            } else {
                onChildTerminatedEvent(id);
            }
        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inActive in the map
            this.markMonitorAsInactive(id);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Check whether all dependent goes Terminated and then start them in parallel.
            if (this.aliasToInactiveMonitorsMap.containsKey(id)) {
                this.aliasToInactiveMonitorsMap.remove(id);
            } else {
                log.warn("[monitor] " + id + " cannot be found in the inActive monitors list");
            }
            if (this.status == GroupStatus.Terminating || this.status == GroupStatus.Terminated) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
                log.info("Executing the un-subscription request for the [monitor] " + id);
            }
        }
    }

    @Override
    public void onParentEvent(MonitorStatusEvent statusEvent) {
        // send the ClusterTerminating event
        if (statusEvent.getStatus() == GroupStatus.Terminating || statusEvent.getStatus() ==
                ApplicationStatus.Terminating) {
            ApplicationBuilder.handleGroupTerminatingEvent(appId, id);
        }
    }

    @Override
    public void onEvent(MonitorTerminateAllEvent terminateAllEvent) {
        this.terminateChildren = true;

    }

    @Override
    public void onEvent(MonitorScalingEvent scalingEvent) {

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

        //if(this.status != status) {
        this.status = status;
        //notifying the parent
        if (status == GroupStatus.Inactive && !this.hasDependent) {
            log.info("[Group] " + this.id + "is not notifying the parent, " +
                    "since it is identified as the independent unit");
        } else {
            // notify parent
            log.info("[Group] " + this.id + "is notifying the [parent] " + this.parent.getId());
            MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent, this.status, this.id);
        }
        //notify the children about the state change
        MonitorStatusEventBuilder.notifyChildren(this, new GroupStatusEvent(status, getId()));
    }
}
