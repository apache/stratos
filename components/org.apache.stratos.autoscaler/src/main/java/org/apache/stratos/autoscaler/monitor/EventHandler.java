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

import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;

/**
 * Event Handler to notify the observer/to receive notification
 */
public interface EventHandler {
    /**
     * Triggered when an event is received from a child.
     *
     * @param statusEvent
     */
    public abstract void onChildEvent(MonitorStatusEvent statusEvent);

    /**
     * Triggered when an event is received from the parent.
     *
     * @param statusEvent
     */
    public abstract void onParentEvent(MonitorStatusEvent statusEvent);

    /**
     * Triggered when termination decision is made.
     *
     * @param terminateAllEvent
     */
    public abstract void onEvent(MonitorTerminateAllEvent terminateAllEvent);

    /**
     * Triggered when scaling decision is made.
     *
     * @param scalingEvent
     */
    public abstract void onEvent(MonitorScalingEvent scalingEvent);
}
