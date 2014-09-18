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

package org.apache.stratos.messaging.domain.topology;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Group implements ParentBehavior {

    private static final long serialVersionUID = 8347096598203655846L;
    // Name of the Group, specified in Group Definition
    private String name;
    // Group alias
    private String alias;
    // Group deployment policy
    private String deploymentPolicy;
    // Group level autoscaling policy
    private String autoscalingPolicy;
    // Dependency Order
    private DependencyOrder dependencyOrder;
    // Sub Group Map, key = Group.name
    private Map<String, Group> groupMap;
    // Cluster id map, key = service name
    private Map<String, String> clusterIdMap;
    // Group status
    private Status status;

    public Group (String name, String alias) {
        this.name = name;
        this.alias = alias;
        this.status = Status.Created;
        groupMap = new HashMap<String, Group>();
        clusterIdMap = new HashMap<String, String>();
    }

    @Override
    public void addGroup(Group group) {
        groupMap.put(group.name, group);
    }

    @Override
    public void setGroups(Map<String, Group> groupNameToGroup) {
        groupMap.putAll(groupNameToGroup);
    }

    @Override
    public Group getGroup(String groupName) {
        return groupMap.get(groupName);
    }

    @Override
    public Group getGroupRecursively(String groupAlias) {

        return travereAndCheckRecursively(groupMap.values(), groupAlias);
    }

    private Group travereAndCheckRecursively (Collection<Group> groups, String groupAlias) {

        for (Group group : groups) {
            // check if alias is equal, if so, return
            if (groupAlias.equals(group.getAlias())) {
                return group;
            } else {
                // check if this Group has nested sub Groups. If so, traverse them as well
                if (group.getGroups() != null) {
                    return travereAndCheckRecursively(group.getGroups(), groupAlias);
                }
            }
        }

        return null;
    }

    @Override
    public Collection<Group> getGroups() {
        return groupMap.values();
    }

    @Override
    public void setDependencyOrder(DependencyOrder dependencyOrder) {
        this.dependencyOrder = dependencyOrder;
    }

    @Override
    public DependencyOrder getDependencyOrder() {
        return dependencyOrder;
    }

    @Override
    public void addClusterId(String serviceName, String clusterId) {
        clusterIdMap.put(serviceName, clusterId);
    }

    @Override
    public void setClusterIds(Map<String, String> serviceNameToClusterId) {
        clusterIdMap.putAll(serviceNameToClusterId);
    }

    @Override
    public String getClusterId(String serviceName) {
        return clusterIdMap.get(serviceName);
    }

    @Override
    public Collection<String> getClusterIds() {
        return clusterIdMap.values();
    }

    public Map<String, String> getClusterIdMap () {
        return clusterIdMap;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getAutoscalingPolicy() {
        return autoscalingPolicy;
    }

    public void setAutoscalingPolicy(String autoscalingPolicy) {
        this.autoscalingPolicy = autoscalingPolicy;
    }

    public boolean equals(Object other) {
        if(other == null || !(other instanceof Group)) {
            return false;
        }

        if(this == other) {
            return true;
        }

        Group that = (Group)other;
        return this.name.equals(that.name) && this.alias.equals(that.alias);
    }

    public int hashCode () {
        return name.hashCode() + alias.hashCode();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
