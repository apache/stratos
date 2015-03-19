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

package org.apache.stratos.messaging.domain.application;

import org.apache.stratos.messaging.domain.instance.GroupInstance;

import java.util.*;

/**
 * Represents a Group/nested Group in an Application/Group
 */

public class Group extends ParentComponent<GroupInstance> {

    private static final long serialVersionUID = 8347096598203655846L;
    // Name of the Group, specified in Group Definition
    private String name;
    // Group alias
    private String alias;
    //minimum group instances
    private int groupMinInstances;
    //maximum group instances
    private int groupMaxInstances;
    // Group level autoscaling policy
    private String autoscalingPolicy;
    // application id
    private String applicationId;

    // Group/Cluster Instance Context map, key = instance id
//    private final Map<String, Set<InstanceContext>> instanceIdToInstanceContextMap;
    // Life cycle state manager
    //protected LifeCycleStateManager<GroupStatus> groupStateManager;

    public Group(String applicationId, String name, String alias) {
        super();
        this.applicationId = applicationId;
        this.name = name;
        this.alias = alias;
        this.setInstanceIdToInstanceContextMap(new HashMap<String, GroupInstance>());
        //instanceIdToInstanceContextMap = new HashMap<String, Set<InstanceContext>>();
        //this.groupStateManager = new LifeCycleStateManager<GroupStatus>(GroupStatus.Created, alias);
    }

    public String getUniqueIdentifier() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getAutoscalingPolicy() {
        return autoscalingPolicy;
    }

    public void setAutoscalingPolicy(String autoscalingPolicy) {
        this.autoscalingPolicy = autoscalingPolicy;
    }

    public boolean isStateTransitionValid(GroupStatus newState, String groupInstanceId) {
        return getInstanceIdToInstanceContextMap().get(groupInstanceId).isStateTransitionValid(newState);
    }

    public Stack<GroupStatus> getTransitionedStates(String groupInstanceId) {
        return getInstanceIdToInstanceContextMap().get(groupInstanceId).getTransitionedStates();
    }

    public GroupStatus getStatus(String groupInstanceId) {
        return getInstanceIdToInstanceContextMap().get(groupInstanceId).getStatus();
    }

    public boolean setStatus(GroupStatus newState, String groupInstanceId) {
        return this.getInstanceIdToInstanceContextMap().get(groupInstanceId).setStatus(newState);
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof Group)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        Group that = (Group) other;
        return this.name.equals(that.name) && this.alias.equals(that.alias);
    }

    public int hashCode() {
        return name.hashCode() + alias.hashCode();
    }

    public String getApplicationId() {
        return applicationId;
    }


    public int getGroupMinInstances() {
        return groupMinInstances;
    }

    public void setGroupMinInstances(int groupMinInstances) {
        this.groupMinInstances = groupMinInstances;
    }

    public int getGroupMaxInstances() {
        return groupMaxInstances;
    }

    public void setGroupMaxInstances(int groupMaxInstances) {
        this.groupMaxInstances = groupMaxInstances;
    }

    public Set<ClusterDataHolder> getClusterDataHoldersOfGroup() {
        Set<ClusterDataHolder> appClusterData = new HashSet<ClusterDataHolder>();

        // get top level Cluster Data
        if (this.aliasToClusterDataMap != null && !this.aliasToClusterDataMap.isEmpty()) {
            appClusterData.addAll(this.aliasToClusterDataMap.values());
        }

        // find other nested Cluster Data (in the Groups)
        if (getGroups() != null) {
            getClusterData(appClusterData, getGroups());
        }

        return appClusterData;

    }
}
