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
package org.apache.stratos.autoscaler.context.application;

import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.ParentLevelPartitionContext;
import org.apache.stratos.autoscaler.monitor.events.ScalingDownBeyondMinEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingUpBeyondMaxEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This will hold the application instance related info.
 */
public class ParentInstanceContext extends InstanceContext {
    //key=id of the child, value=ScalingEvent
    private Map<String, ScalingEvent> idToScalingEvent;
    //key=id of the child, value=ScalingUpBeyondMaxEvent
    private Map<String, ScalingUpBeyondMaxEvent> idToScalingOverMaxEvent;
    //key=id of the child, value=ScalingDownBeyondMinEvent
    private Map<String, ScalingDownBeyondMinEvent> idToScalingDownBeyondMinEvent;
    //partitions of this network partition
    private final List<ParentLevelPartitionContext> partitionCtxts;

    public ParentInstanceContext(String id) {
        super(id);
        partitionCtxts = new ArrayList<ParentLevelPartitionContext>();
        setIdToScalingEvent(new ConcurrentHashMap<String, ScalingEvent>());
        setIdToScalingOverMaxEvent(new ConcurrentHashMap<String, ScalingUpBeyondMaxEvent>());
        setIdToScalingDownBeyondMinEvent(new ConcurrentHashMap<String, ScalingDownBeyondMinEvent>());

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


    public List<ParentLevelPartitionContext> getPartitionCtxts() {

        return partitionCtxts;
    }

    public ParentLevelPartitionContext getPartitionCtxt(String partitionId) {


        for (ParentLevelPartitionContext partitionContext : partitionCtxts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return partitionContext;
            }
        }
        return null;
    }

    public void addPartitionContext(ParentLevelPartitionContext partitionContext) {
        partitionCtxts.add(partitionContext);
    }

    public int getNonTerminatedMemberCountOfPartition(String partitionId) {

        for (ParentLevelPartitionContext partitionContext : partitionCtxts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return partitionContext.getNonTerminatedInstanceCount();
            }
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {

        for (ParentLevelPartitionContext partitionContext : partitionCtxts) {
            if (partitionContext.getPartitionId().equals(currentPartitionId)) {
                return partitionContext.getActiveInstanceCount();
            }
        }
        return 0;
    }
}
