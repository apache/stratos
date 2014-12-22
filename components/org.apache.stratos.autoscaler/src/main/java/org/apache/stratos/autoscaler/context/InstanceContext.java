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
package org.apache.stratos.autoscaler.context;

import org.apache.stratos.autoscaler.monitor.events.ScalingDownBeyondMinEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingUpBeyondMaxEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This will hold the instances related info.
 */
public abstract class InstanceContext {
    protected String id;

    protected String parentInstanceId;

    //key=id of the child, value=ScalingEvent
    private Map<String, ScalingEvent> idToScalingEvent;
    //key=id of the child, value=ScalingUpBeyondMaxEvent
    private Map<String, ScalingUpBeyondMaxEvent> idToScalingOverMaxEvent;
    //key=id of the child, value=ScalingDownBeyondMinEvent
    private Map<String, ScalingDownBeyondMinEvent> idToScalingDownBeyondMinEvent;


    public InstanceContext(String id) {
        this.id = id;
        setIdToScalingEvent(new ConcurrentHashMap<String, ScalingEvent>());
        setIdToScalingOverMaxEvent(new ConcurrentHashMap<String, ScalingUpBeyondMaxEvent>());
        setIdToScalingDownBeyondMinEvent(new ConcurrentHashMap<String, ScalingDownBeyondMinEvent>());

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentInstanceId() {
        return parentInstanceId;
    }

    public void setParentInstanceId(String parentInstanceId) {
        this.parentInstanceId = parentInstanceId;
    }

    public Map<String, ScalingEvent> getIdToScalingEvent() {
        return idToScalingEvent;
    }

    public void setIdToScalingEvent(Map<String, ScalingEvent> idToScalingEvent) {
        this.idToScalingEvent = idToScalingEvent;
    }

    public Map<String, ScalingUpBeyondMaxEvent> getIdToScalingOverMaxEvent() {
        return idToScalingOverMaxEvent;
    }

    public void setIdToScalingOverMaxEvent(Map<String, ScalingUpBeyondMaxEvent> idToScalingOverMaxEvent) {
        this.idToScalingOverMaxEvent = idToScalingOverMaxEvent;
    }

    public Map<String, ScalingDownBeyondMinEvent> getIdToScalingDownBeyondMinEvent() {
        return idToScalingDownBeyondMinEvent;
    }

    public void setIdToScalingDownBeyondMinEvent(Map<String, ScalingDownBeyondMinEvent> idToScalingDownBeyondMinEvent) {
        this.idToScalingDownBeyondMinEvent = idToScalingDownBeyondMinEvent;
    }

    public void removeScalingEvent(String id) {
        this.idToScalingEvent.remove(id);
    }

    public void addScalingEvent(ScalingEvent scalingEvent) {
        this.idToScalingEvent.put(scalingEvent.getId(), scalingEvent);
    }

    public ScalingEvent getScalingEvent(String id) {
        return this.idToScalingEvent.get(id);
    }

    public ScalingUpBeyondMaxEvent getScalingMaxEvent(String id) {
        return this.idToScalingOverMaxEvent.get(id);
    }

    public void removeScalingOverMaxEvent(String id) {
        this.idToScalingOverMaxEvent.remove(id);
    }

    public void addScalingOverMaxEvent(ScalingUpBeyondMaxEvent scalingUpBeyondMaxEvent) {
        this.idToScalingOverMaxEvent.put(scalingUpBeyondMaxEvent.getId(), scalingUpBeyondMaxEvent);
    }

    public ScalingDownBeyondMinEvent getScalingDownBeyondMinEvent(String id) {
        return this.idToScalingDownBeyondMinEvent.get(id);
    }

    public void removeScalingDownBeyondMinEvent(String id) {
        this.idToScalingDownBeyondMinEvent.remove(id);
    }

    public void addScalingDownBeyondMinEvent(ScalingDownBeyondMinEvent scalingDownBeyondMinEvent) {
        this.idToScalingDownBeyondMinEvent.put(scalingDownBeyondMinEvent.getId(), scalingDownBeyondMinEvent);
    }

    public boolean containsScalingEvent(String id) {
        return this.idToScalingEvent.containsKey(id);
    }

    public boolean containsScalingOverMaxEvent(String id) {
        return this.idToScalingOverMaxEvent.containsKey(id);
    }
}
