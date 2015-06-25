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
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.component.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.events.*;
import org.apache.stratos.messaging.domain.application.GroupStatus;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;

/**
 * This will build the necessary monitor status events to be sent to the parent/child  monitor
 */
public class MonitorStatusEventBuilder {
    private static final Log log = LogFactory.getLog(MonitorStatusEventBuilder.class);

    public static void handleClusterStatusEvent(ParentComponentMonitor parent, ClusterStatus status, String clusterId,
                                                String instanceId) {
        ClusterStatusEvent clusterStatusEvent = new ClusterStatusEvent(status, clusterId, instanceId);
        notifyParent(parent, clusterStatusEvent);
    }

    public static void handleGroupStatusEvent(ParentComponentMonitor parent, GroupStatus status,
                                              String groupId, String instanceId) {
        GroupStatusEvent groupStatusEvent = new GroupStatusEvent(status, groupId, instanceId);
        notifyParent(parent, groupStatusEvent);
    }

    public static void handleClusterScalingEvent(ParentComponentMonitor parent,
                                                 String networkPartitionId, String instanceId, float factor,
                                                 String id) {

        log.info(String.format("Scaling event to the parent, [id] %s, [network Partition Id] %s, [instance id] %s, " +
                "[factor] %s", id, networkPartitionId, instanceId, factor));
        //Send notifications to parent of the cluster monitor
        ScalingEvent scalingEvent = new ScalingEvent(id, networkPartitionId, instanceId, factor);
        notifyParent(parent, scalingEvent);
    }

    public static void handleScalingOverMaxEvent(ParentComponentMonitor parent,
                                                 String networkPartitionId, String instanceId,
                                                 String appId) {

        //Send notifications to parent of the cluster monitor
        ScalingUpBeyondMaxEvent scalingUpBeyondMaxEvent = new ScalingUpBeyondMaxEvent(appId, networkPartitionId,
                instanceId);
        notifyParent(parent, scalingUpBeyondMaxEvent);
    }

    public static void handleScalingDownBeyondMinEvent(ParentComponentMonitor parent, String networkPartitionId,
                                                       String instanceId, String appId) {

        //Send notifications to parent of the cluster monitor
        ScalingDownBeyondMinEvent scalingDownBeyondMinEvent = new ScalingDownBeyondMinEvent(appId, networkPartitionId,
                instanceId);
        notifyParent(parent, scalingDownBeyondMinEvent);
    }

    private static void notifyParent(ParentComponentMonitor parent, MonitorStatusEvent statusEvent) {
        parent.onChildStatusEvent(statusEvent);
    }

    public static void notifyChildren(ParentComponentMonitor componentMonitor, MonitorStatusEvent statusEvent)
            throws MonitorNotFoundException {
        for (Monitor activeChildMonitor : componentMonitor.getAliasToActiveChildMonitorsMap().values()) {
            activeChildMonitor.onParentStatusEvent(statusEvent);
        }
    }

    private static void notifyParent(ParentComponentMonitor parent, ScalingEvent scalingEvent) {
        parent.onChildScalingEvent(scalingEvent);
    }

    private static void notifyParent(ParentComponentMonitor parent, ScalingDownBeyondMinEvent scalingDownBeyondMinEvent) {
        parent.onChildScalingDownBeyondMinEvent(scalingDownBeyondMinEvent);
    }

    private static void notifyParent(ParentComponentMonitor parent, ScalingUpBeyondMaxEvent scalingUpBeyondMaxEvent) {
        parent.onChildScalingOverMaxEvent(scalingUpBeyondMaxEvent);
    }
}
