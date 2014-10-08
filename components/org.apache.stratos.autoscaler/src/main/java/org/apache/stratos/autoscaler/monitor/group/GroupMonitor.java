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
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.monitor.EventHandler;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.monitor.events.ClusterStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.GroupStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.domain.topology.Status;
import org.apache.stratos.messaging.event.application.status.StatusEvent;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends Monitor implements EventHandler {
    private static final Log log = LogFactory.getLog(GroupMonitor.class);

    private Monitor parent;

    public GroupMonitor(Group group) {
        super(group);
        this.id = group.getAlias();
        startDependency();

    }

    /*@Override
    public void update(Observable observable, Object event) {
        MonitorStatusEvent1111 statusEvent = (MonitorStatusEvent1111) event;
        Status childStatus = statusEvent.getStatus();
        String notifier = statusEvent.getNotifierId();
        log.info(String.format("[Monitor] %s got notified from the [child] %s" +
                "on its state change from %s to %s", id, notifier, this.status, status));
        if (childStatus == Status.Activated) {
            //start the next dependency
            startDependency(notifier);
        } else if(childStatus == Status.In_Maintenance) {

        }
    }*/


    public void setStatus(Status status) {
        log.info(String.format("[Monitor] %s is notifying the parent" +
                "on its state change from %s to %s", id, this.status, status));
        this.status = status;
        //notifying the parent
        MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent, this.status, this.id);
        //setChanged();
        //notifyObservers(new MonitorStatusEvent1111(status, id));
    }

    @Override
    public void onEvent(MonitorStatusEvent statusEvent) {
        this.monitor(statusEvent);
    }

    public Monitor getParent() {
        return parent;
    }

    public void setParent(Monitor parent) {
        this.parent = parent;
    }

    @Override
    protected void monitor(MonitorStatusEvent statusEvent) {
        ApplicationContext context = this.dependencyTree.
                                            findApplicationContextWithId(statusEvent.getId());
        if(context.getStatusLifeCycle().isEmpty()) {
            startDependency(statusEvent.getId());
        } else {
            //TODO act based on life cyle events
        }

    }
}
