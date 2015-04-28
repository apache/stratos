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

import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingUpBeyondMaxEvent;

/**
 * Event Handler to notify the observer/to receive notification
 */
public interface EventHandler {
    /**
     * Triggered when a status event is received from a child.
     *
     * @param statusEvent
     */
    public abstract void onChildStatusEvent(MonitorStatusEvent statusEvent);

    /**
     * Triggered when a status event is received from the parent.
     *
     * @param statusEvent
     * @throws MonitorNotFoundException
     */
    public abstract void onParentStatusEvent(MonitorStatusEvent statusEvent) throws
            MonitorNotFoundException;

    /**
     * Triggered when a scaling event is received from a child.
     *
     * @param scalingEvent the event which passed when scaling
     */
    public abstract void onChildScalingEvent(ScalingEvent scalingEvent);

    /**
     * Triggered when a scaling over max event is received from a child.
     *
     * @param scalingUpBeyondMaxEvent
     */
    public abstract void onChildScalingOverMaxEvent(ScalingUpBeyondMaxEvent scalingUpBeyondMaxEvent);

    /**
     * Triggered when a scaling event is received from the parent.
     *
     * @param scalingEvent
     */
    public abstract void onParentScalingEvent(ScalingEvent scalingEvent);
}
