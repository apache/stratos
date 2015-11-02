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

import org.apache.stratos.messaging.domain.instance.Instance;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstraction for a an entity that can have child entities
 * in an Application within the Topology
 */

public abstract class ParentComponent<T extends Instance> implements Serializable {

    // Group Map, key = Group.alias
    protected final Map<String, Group> aliasToGroupMap;
    // Cluster Id map, key = subscription alias for the cartridge type
    protected final Map<String, ClusterDataHolder> aliasToClusterDataMap;
    // Cluster Id map, key = cartridge type
    private final Map<String, ClusterDataHolder> typeToClusterDataMap;
    // alias to deployment policy id, key = cartridge or cartridge-group alias
    private Map<String, String> aliasToDeploymentPolicyIdMap;
    // Group/Cluster Instance Context map, key = instance id
    private Map<String, T> instanceIdToInstanceContextMap;
    // Dependency Order
    private DependencyOrder dependencyOrder;
    // flag for Group level scaling
    private boolean isGroupScalingEnabled;
    //flag for group instance level monitoring
    private boolean isGroupInstanceMonitoringEnabled;
    //deployment policy
    private String deploymentPolicy;
    //generate the sequence no for the instanceId
    private AtomicInteger instanceIdSequence;


    public ParentComponent() {
        this.isGroupScalingEnabled = false;
        this.isGroupInstanceMonitoringEnabled = false;
        aliasToGroupMap = new HashMap<String, Group>();
        aliasToClusterDataMap = new HashMap<String, ClusterDataHolder>();
        typeToClusterDataMap = new HashMap<String, ClusterDataHolder>();
        instanceIdSequence = new AtomicInteger();
    }

    /**
     * This method should be implemented in each concrete child class
     * to return the unique identifier for each implementation
     * ex.: group alias for a Group, app id for an Application
     *
     * @return unique identifier String
     */
    public abstract String getUniqueIdentifier();

    /**
     * Adds a group
     *
     * @param group Group instance to be added
     */
    public void addGroup(Group group) {
        aliasToGroupMap.put(group.getUniqueIdentifier(), group);
    }

    /**
     * Get the Group for the given alias in the context of the parent entity.
     * Will not search recursively in the nested levels.
     *
     * @param groupAlias alias of the Group
     * @return Group, if exists for the given alias, else null
     */
    public Group getGroup(String groupAlias) {
        return aliasToGroupMap.get(groupAlias);
    }

    /**
     * Get the Group for the given alias in the context of the parent entity.
     * Will search recursively in the nested levels.
     *
     * @param groupAlias alias of the Group
     * @return Group, if exists for the given alias, else null
     */
    public Group getGroupRecursively(String groupAlias) {

        return travereAndCheckRecursively(aliasToGroupMap, groupAlias);
    }

    private Group travereAndCheckRecursively(Map<String, Group> aliasToGroupMap, String groupAlias) {

        if (aliasToGroupMap.containsKey(groupAlias)) {
            synchronized (aliasToGroupMap) {
                if (aliasToGroupMap.containsKey(groupAlias)) {
                    return aliasToGroupMap.get(groupAlias);
                }
            }
        } else {
            for (Group group : aliasToGroupMap.values()) {
                Group foundGroup = travereAndCheckRecursively(group.getAliasToGroupMap(), groupAlias);
                if (foundGroup != null) {
                    return foundGroup;
                }
            }
        }

        return null;
    }

    /**
     * This will recursively search for the cluster data holder in the application by the alias.
     *
     * @param alias the alias of the cluster
     * @return found data holder
     */
    public ClusterDataHolder getClusterDataHolderRecursivelyByAlias(String alias) {
        if (aliasToClusterDataMap.containsKey(alias)) {
            return aliasToClusterDataMap.get(alias);
        } else {
            ClusterDataHolder foundDataHolder = traverseAndCheckClusterDataHolderRecursively(aliasToGroupMap, alias);
            if (foundDataHolder != null) {
                return foundDataHolder;
            }
        }
        return null;
    }

    private ClusterDataHolder traverseAndCheckClusterDataHolderRecursively(
            Map<String, Group> aliasToGroupMap, String alias) {
        for (Group group : aliasToGroupMap.values()) {
            if (group.getClusterDataMap() != null && !group.getClusterDataMap().isEmpty()) {
                if (group.getClusterData(alias) != null) {
                    return group.getClusterData(alias);
                }
            }
        }

        for (Group group : aliasToGroupMap.values()) {
            if (group.getGroups() != null) {
                ClusterDataHolder foundDataHolder =
                        traverseAndCheckClusterDataHolderRecursively(group.getAliasToGroupMap(),
                                alias);
                if (foundDataHolder != null) {
                    return foundDataHolder;
                }
            }
        }
        return null;
    }

    /**
     * Getter for alias to Group map
     * Will not search recursively in the nested levels.
     *
     * @return Map, key = alias given to the Group, value = Group
     */
    public Map<String, Group> getAliasToGroupMap() {
        return this.aliasToGroupMap;
    }

    /**
     * Getter for cluster alias to ClusterData map for this level
     *
     * @return Map, key = alias given to the cluster, value =  ClusterData object
     */
    public Map<String, ClusterDataHolder> getClusterDataMap() {
        return this.aliasToClusterDataMap;
    }

    /**
     * Collection of Groups in this level
     *
     * @return Group Collection object, empty if no Groups are found
     */
    public Collection<Group> getGroups() {
        return aliasToGroupMap.values();
    }

    /**
     * Setter for Group alias to Group map
     *
     * @param groupAliasToGroup Map, key = alias given to the Group, value = Group
     */
    public void setGroups(Map<String, Group> groupAliasToGroup) {
        aliasToGroupMap.putAll(groupAliasToGroup);
    }

    /**
     * Getter for Dependency Order for this level
     *
     * @return Dependency Order object
     */
    public DependencyOrder getDependencyOrder() {
        return dependencyOrder;
    }

    /**
     * Setter for Dependency Order
     *
     * @param dependencyOrder Dependency Order object
     */
    public void setDependencyOrder(DependencyOrder dependencyOrder) {
        this.dependencyOrder = dependencyOrder;
    }

    /**
     * Setter for alias to Cluster Data map
     *
     * @param aliasToClusterData Map, key = alias given to the cluster, value =  ClusterData object
     */
    public void setClusterData(Map<String, ClusterDataHolder> aliasToClusterData) {
        this.aliasToClusterDataMap.putAll(aliasToClusterData);
    }

    /**
     * Getter for Cluster Data instance for the given alias
     * Will not search recursively in the nested levels.
     *
     * @param alias
     * @return
     */
    public ClusterDataHolder getClusterData(String alias) {
        return aliasToClusterDataMap.get(alias);
    }

    /**
     * Collects the Cluster Data for the parent component and all the
     * child components recursively
     *
     * @return Set of ClusterDataHolder objects if available, else null
     */
    public Set<ClusterDataHolder> getClusterDataRecursively() {

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

    /**
     * Collects the Group for the parent component and all the
     * child components recursively
     *
     * @return Set of Group objects if available, else null
     */
    public Set<Group> getAllGroupsRecursively() {

        Set<Group> appGroups = new HashSet<Group>();

        // get top level Cluster Data
        if (this.aliasToGroupMap != null && !this.aliasToGroupMap.isEmpty()) {
            appGroups.addAll(this.aliasToGroupMap.values());
        }

        // find other nested Cluster Data (in the Groups)
        if (getGroups() != null) {
            getGroupsRecursively(appGroups, getGroups());
        }

        return appGroups;
    }

    /**
     * Adds InstanceContext of a child to the instanceIdToInstanceContextMap.
     *
     * @param instanceId instance id of child
     * @param instance   InstanceContext object
     */
    public void addInstance(String instanceId, T instance) {
        getInstanceIdToInstanceContextMap().put(instanceId, instance);
    }

    /**
     * This will remove the instance from instanceIdToInstanceContextMap
     *
     * @param instanceId instance id of child
     */
    public void removeInstance(String instanceId) {
        getInstanceIdToInstanceContextMap().remove(instanceId);
    }

    /**
     * Adds InstanceContext of a child to the instanceIdToInstanceContextMap.
     *
     * @param instanceId instance id of child
     */
    public boolean containsInstanceContext(String instanceId) {
        return getInstanceIdToInstanceContextMap().containsKey(instanceId);
    }


    /**
     * Retrieves InstanceContext obj. for the given instance id
     *
     * @param instanceId instance id
     * @return InstanceContext obj. if exists, else null
     */
    public T getInstanceContexts(String instanceId) {
        // if map is empty, return null
        if (getInstanceIdToInstanceContextMap().isEmpty()) {
            return null;
        }

        // if instanceId is null, just get the first InstanceContext
        if (instanceId == null) {
            return getInstanceIdToInstanceContextMap().entrySet().iterator().next().getValue();
        }

        return getInstanceIdToInstanceContextMap().get(instanceId);
    }

    /**
     * Retrieves InstanceContext obj. for the given instance id
     *
     * @param parentInstanceId parent instance id
     * @return InstanceContext obj. if exists, else null
     */
    public List<Instance> getInstanceContextsWithParentId(String parentInstanceId) {
        // if map is empty, return null
        List<Instance> contexts = new ArrayList<Instance>();

        if (getInstanceIdToInstanceContextMap().isEmpty()) {
            return contexts;
        }

        // if instanceId is null, just get the first InstanceContext
        if (parentInstanceId != null) {
            for (Instance context : getInstanceIdToInstanceContextMap().values()) {
                if (parentInstanceId.equals(context.getParentId())) {
                    contexts.add(context);
                }
            }
        }

        return contexts;
    }

    /**
     * Retrieves the current number of instance contexts which are kept track in this node
     *
     * @return number of instance contexts
     */
    public int getInstanceContextCount() {

        return getInstanceIdToInstanceContextMap().keySet().size();
    }

    public int getComponentsCount() {
        return getClusterDataMap().keySet().size() + getAliasToGroupMap().keySet().size();
    }

    protected void getClusterData(Set<ClusterDataHolder> clusterData, Collection<Group> groups) {

        for (Group group : groups) {
            if (group.getClusterDataMap() != null && !group.getClusterDataMap().isEmpty()) {
                clusterData.addAll(group.getClusterDataMap().values());
            }
            if (group.getGroups() != null && !group.getGroups().isEmpty()) {
                getClusterData(clusterData, group.getGroups());
            }
        }
    }

    protected void getGroupsRecursively(Set<Group> groupsSet, Collection<Group> groups) {

        for (Group group : groups) {
            if (group.getGroups() != null && !group.getGroups().isEmpty()) {
                groupsSet.addAll(group.getGroups());
                if (group.getGroups() != null) {
                    getGroupsRecursively(groupsSet, group.getGroups());
                }
            }
        }
    }


    public Map<String, T> getInstanceIdToInstanceContextMap() {
        return instanceIdToInstanceContextMap;
    }

    public boolean isGroupScalingEnabled() {
        return isGroupScalingEnabled;
    }

    public void setGroupScalingEnabled(boolean isGroupScalingEnabled) {
        this.isGroupScalingEnabled = isGroupScalingEnabled;
    }

    public boolean isGroupInstanceMonitoringEnabled() {
        return isGroupInstanceMonitoringEnabled;
    }

    public void setGroupInstanceMonitoringEnabled(boolean isGroupInstanceMonitoringEnabled) {
        this.isGroupInstanceMonitoringEnabled = isGroupInstanceMonitoringEnabled;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getNextInstanceId(String alias) {
        int nextSequence = instanceIdSequence.incrementAndGet();
        String instanceId = alias + "-" + nextSequence;
        return instanceId;
    }

    public void setInstanceIdToInstanceContextMap(Map<String, T> instanceIdToInstanceContextMap) {
        this.instanceIdToInstanceContextMap = instanceIdToInstanceContextMap;
    }

    public Map<String, ClusterDataHolder> getClusterDataForType() {
        return typeToClusterDataMap;
    }

    public Map<String, ClusterDataHolder> getClusterDataForAlias() {
        return aliasToClusterDataMap;
    }

    /**
     * Setter for alias to Cluster Data map
     *
     * @param typeToClusterData Map, key = alias given to the cluster, value =  ClusterData object
     */
    public void setClusterDataForType(Map<String, ClusterDataHolder> typeToClusterData) {
        this.typeToClusterDataMap.putAll(typeToClusterData);
    }

    public Map<String, String> getAliasToDeploymentPolicyIdMap() {
        return aliasToDeploymentPolicyIdMap;
    }

    public void setAliasToDeploymentPolicyIdMap(
            Map<String, String> aliasToDeploymentPolicyIdMap) {
        this.aliasToDeploymentPolicyIdMap = aliasToDeploymentPolicyIdMap;
    }
}
