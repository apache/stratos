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
package org.apache.stratos.autoscaler.context.group;

import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingOverMaxEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This will hold the group instance related info
 */
public class GroupInstanceContext extends InstanceContext {

    //partitions of this network partition
    private final List<GroupLevelPartitionContext> partitionCtxts;
    //key=id of the child, value=ScalingEvent
    private Map<String, ScalingEvent> idToScalingEvent;
    //key=id of the child, value=MaxOutScalingEvent
    private Map<String, ScalingOverMaxEvent> idToScalingOverMaxEvent;

    public GroupInstanceContext(String id) {
        super(id);
        partitionCtxts = new ArrayList<GroupLevelPartitionContext>();
        setIdToScalingEvent(new HashMap<String, ScalingEvent>());
        setIdToScalingOverMaxEvent(new HashMap<String, ScalingOverMaxEvent>());
    }

    public List<GroupLevelPartitionContext> getPartitionCtxts() {

        return partitionCtxts;
    }

    public GroupLevelPartitionContext getPartitionCtxt(String partitionId) {


        for(GroupLevelPartitionContext partitionContext : partitionCtxts){
            if(partitionContext.getPartitionId().equals(partitionId)){
                return partitionContext;
            }
        }
        return null;
    }

    public void addPartitionContext(GroupLevelPartitionContext partitionContext) {
        partitionCtxts.add(partitionContext);
    }

    public int getNonTerminatedMemberCountOfPartition(String partitionId) {

        for(GroupLevelPartitionContext partitionContext : partitionCtxts){
            if(partitionContext.getPartitionId().equals(partitionId)){
                return partitionContext.getNonTerminatedInstanceCount();
            }
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {

        for(GroupLevelPartitionContext partitionContext : partitionCtxts){
            if(partitionContext.getPartitionId().equals(currentPartitionId)){
                return partitionContext.getActiveInstanceCount();
            }
        }
        return 0;
    }

    public Map<String, ScalingEvent> getIdToScalingEvent() {
        return idToScalingEvent;
    }

    public void setIdToScalingEvent(Map<String, ScalingEvent> idToScalingEvent) {
        this.idToScalingEvent = idToScalingEvent;
    }

    public Map<String, ScalingOverMaxEvent> getIdToScalingOverMaxEvent() {
        return idToScalingOverMaxEvent;
    }

    public void setIdToScalingOverMaxEvent(Map<String, ScalingOverMaxEvent> idToScalingOverMaxEvent) {
        this.idToScalingOverMaxEvent = idToScalingOverMaxEvent;
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

    public ScalingOverMaxEvent getScalingMaxEvent(String id) {
        return this.idToScalingOverMaxEvent.get(id);
    }

    public void removeScalingOverMaxEvent(String id) {
        this.idToScalingOverMaxEvent.remove(id);
    }

    public void addScalingOverMaxEvent(ScalingOverMaxEvent scalingOverMaxEvent) {
        this.idToScalingOverMaxEvent.put(scalingOverMaxEvent.getId(), scalingOverMaxEvent);
    }

    public boolean containsScalingEvent(String id) {
        return this.idToScalingEvent.containsKey(id);
    }

    public boolean containsScalingOverMaxEvent(String id) {
        return this.idToScalingOverMaxEvent.containsKey(id);
    }
}
