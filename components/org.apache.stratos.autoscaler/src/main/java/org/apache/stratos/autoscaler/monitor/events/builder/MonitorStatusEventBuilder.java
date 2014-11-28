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
package org.apache.stratos.autoscaler.monitor.events.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.component.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.events.*;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.applications.GroupStatus;

/**
 * This will build the necessary monitor status events to be sent to the parent/child  monitor
 */
public class MonitorStatusEventBuilder {
    private static final Log log = LogFactory.getLog(MonitorStatusEventBuilder.class);

    public static void handleClusterStatusEvent(ParentComponentMonitor parent, ClusterStatus status, String clusterId) {
        ClusterStatusEvent clusterStatusEvent = new ClusterStatusEvent(status, clusterId, null);
        notifyParent(parent, clusterStatusEvent);
    }

    public static void handleGroupStatusEvent(ParentComponentMonitor parent, GroupStatus status,
                                              String groupId, String instanceId) {
        GroupStatusEvent groupStatusEvent = new GroupStatusEvent(status, groupId, instanceId);
        notifyParent(parent, groupStatusEvent);
    }

    public static void handleApplicationStatusEvent(ParentComponentMonitor parent, ApplicationStatus status,
                                                    String appId, String instanceId) {
        ApplicationStatusEvent applicationStatusEvent = new ApplicationStatusEvent(status, appId, instanceId);
        notifyParent(parent, applicationStatusEvent);
    }

    public static void handleClusterScalingEvent(ParentComponentMonitor parent,
                                                 String networkPartitionId, float factor, String appId) {

        //Send notifications to parent of the cluster monitor
        MonitorScalingEvent monitorScalingEvent = new MonitorScalingEvent(appId, networkPartitionId, null,factor) ;
        notifyParent(parent, monitorScalingEvent);
    }

    private static void notifyParent(ParentComponentMonitor parent, MonitorStatusEvent statusEvent) {
        parent.onChildStatusEvent(statusEvent);
    }

    public static void notifyChildren (ParentComponentMonitor componentMonitor, MonitorStatusEvent statusEvent) {
        for (Monitor activeChildMonitor : componentMonitor.getAliasToActiveMonitorsMap().values()) {
            activeChildMonitor.onParentStatusEvent(statusEvent);
        }
    }

    public static void notifyChildGroup(Monitor child, GroupStatus state, String instanceId) {
        MonitorStatusEvent statusEvent = new GroupStatusEvent(state, child.getId(), instanceId);
        child.onParentStatusEvent(statusEvent);
    }

    public static void notifyChildCluster(Monitor child, ClusterStatus state, String instanceId) {
        MonitorStatusEvent statusEvent = new ClusterStatusEvent(state, child.getId(), instanceId);
        child.onParentStatusEvent(statusEvent);
    }

    private static void notifyParent(ParentComponentMonitor parent, MonitorScalingEvent scalingEvent) {
        parent.onChildScalingEvent(scalingEvent);
    }

    public static void notifyChildren (ParentComponentMonitor componentMonitor, MonitorScalingEvent scalingEvent) {
        for (Monitor activeChildMonitor : componentMonitor.getAliasToActiveMonitorsMap().values()) {
            activeChildMonitor.onParentScalingEvent(scalingEvent);
        }
    }

}
