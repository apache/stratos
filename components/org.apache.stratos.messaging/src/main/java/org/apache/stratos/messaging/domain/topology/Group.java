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

import java.util.*;

/**
 * Represents a Group/nested Group in an Application/Group
 */

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
    // Sub Group Map, key = Group.alias
    private Map<String, Group> aliasToGroupMap;
    // Cluster Id map, key = subscription alias for the cartridge type
    private Map<String, ClusterDataHolder> aliasToClusterDataMap;
    // Group status
    private Status status;

    public Group (String name, String alias) {
        this.name = name;
        this.alias = alias;
        this.status = Status.Created;
        aliasToGroupMap = new HashMap<String, Group>();
        aliasToClusterDataMap = new HashMap<String, ClusterDataHolder>();
    }

    @Override
    public void addGroup(Group group) {
        aliasToGroupMap.put(group.name, group);
    }

    @Override
    public void setGroups(Map<String, Group> groupAliasToGroup) {
        aliasToGroupMap.putAll(groupAliasToGroup);
    }

    @Override
    public Group getGroup(String groupAlias) {
        return aliasToGroupMap.get(groupAlias);
    }

    @Override
    public Map<String, Group> getAliasToGroupMap() {
        return this.aliasToGroupMap;
    }

    @Override
    public Map<String, ClusterDataHolder> getClusterDataMap() {
        return this.aliasToClusterDataMap;
    }

    @Override
    public Group getGroupRecursively(String groupAlias) {

        return travereAndCheckRecursively(aliasToGroupMap, groupAlias);
    }

    private Group travereAndCheckRecursively (Map<String,Group> aliasToGroupMap, String groupAlias) {

        if (aliasToGroupMap.containsKey(groupAlias)) {
            synchronized (aliasToGroupMap) {
                if (aliasToGroupMap.containsKey(groupAlias)) {
                    return aliasToGroupMap.get(groupAlias);
                }
            }
        } else {
            for (Group group : aliasToGroupMap.values()) {
                travereAndCheckRecursively(group.getAliasToGroupMap(), groupAlias);
            }
        }

        return null;
    }

    @Override
    public Collection<Group> getGroups() {
        return aliasToGroupMap.values();
    }

    @Override
    public void setDependencyOrder(DependencyOrder dependencyOrder) {
        this.dependencyOrder = dependencyOrder;
    }

    @Override
    public DependencyOrder getDependencyOrder() {
        return dependencyOrder;
    }

//    @Override
//    public void addClusterId(String serviceName, String clusterId) {
//
//        synchronized (serviceNameToClusterIdsMap) {
//            if (serviceNameToClusterIdsMap.get(serviceName) == null) {
//                // not found, create
//                Set<String> clusterIds = new HashSet<String>();
//                clusterIds.add(clusterId);
//                serviceNameToClusterIdsMap.put(serviceName, clusterIds);
//            } else {
//                // the cluster id set already exists, update
//                serviceNameToClusterIdsMap.get(serviceName).add(clusterId);
//            }
//        }
//    }

    @Override
    public void setClusterData(Map<String, ClusterDataHolder> aliasToClusterData) {
        this.aliasToClusterDataMap.putAll(aliasToClusterData);
    }

    @Override
    public ClusterDataHolder getClusterData(String alias) {
        return aliasToClusterDataMap.get(alias);
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
