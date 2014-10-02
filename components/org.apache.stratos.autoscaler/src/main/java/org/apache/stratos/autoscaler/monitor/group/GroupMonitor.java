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
import org.apache.stratos.autoscaler.grouping.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.GroupContext;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;
import java.util.Observable;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(GroupMonitor.class);

    public GroupMonitor(Group group) {
        super(group);
        this.id = group.getAlias();
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
                //start the next dependency
                startDependency(notifier);
            } else if(childStatus == Status.In_Maintenance) {

            }

        }

    }

    public void setStatus(Status status) {
        log.info(String.format("[Monitor] %s is notifying the parent" +
                "on its state change from %s to %s", id, this.status, status));
        this.status = status;
        setChanged();
        notifyObservers(new MonitorStatusEvent(status, id));
    }

    @Override


    //monitor the status of the cluster and the groups
    public void monitor() {


    }
}
