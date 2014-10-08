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
package org.apache.stratos.autoscaler.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.events.ApplicationStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.ClusterStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.GroupStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.messaging.domain.topology.Status;

/**
 * This will build the necessary monitor status events to be sent to the parent/child  monitor
 */
public class MonitorStatusEventBuilder {
    private static final Log log = LogFactory.getLog(MonitorStatusEventBuilder.class);

    public static void handleClusterStatusEvent(Monitor parent, Status status, String clusterId) {
        ClusterStatusEvent clusterStatusEvent = new ClusterStatusEvent(status, clusterId);
        notifyParent(parent, clusterStatusEvent);
    }

    public static void handleGroupStatusEvent(Monitor parent, Status status, String groupId) {
        GroupStatusEvent groupStatusEvent = new GroupStatusEvent(status, groupId);
        notifyParent(parent, groupStatusEvent);
    }

    public static void handleApplicationStatusEvent(Monitor parent, Status status, String appId) {
        ApplicationStatusEvent applicationStatusEvent = new ApplicationStatusEvent(status, appId);
        notifyParent(parent, applicationStatusEvent);
    }

    private static void notifyParent(Monitor parent, MonitorStatusEvent statusEvent) {
       parent.onEvent(statusEvent);
    }


}
