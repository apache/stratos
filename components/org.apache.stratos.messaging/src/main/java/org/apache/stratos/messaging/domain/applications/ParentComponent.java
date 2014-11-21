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

package org.apache.stratos.messaging.domain.applications;

import org.apache.stratos.messaging.domain.applications.scaling.instance.context.InstanceContext;

import java.io.Serializable;
import java.util.*;

/**
 * Abstraction for a an entity that can have child entities
 * in an Application within the Topology
 */

public abstract class ParentComponent implements Serializable {

    // Dependency Order
    private DependencyOrder dependencyOrder;
    // Group Map, key = Group.alias
    private Map<String, Group> aliasToGroupMap;
    // Cluster Id map, key = subscription alias for the cartridge type
    private Map<String, ClusterDataHolder> aliasToClusterDataMap;
    // Group/Cluster Instance Context map
    private Map<AliasAndInstanceId, InstanceContext> aliasAndInstanceIdToInstanceContextMap;

    public ParentComponent () {
        aliasToGroupMap = new HashMap<String, Group>();
        aliasToClusterDataMap = new HashMap<String, ClusterDataHolder>();
        aliasAndInstanceIdToInstanceContextMap =
                new HashMap<AliasAndInstanceId, InstanceContext>();
    }

    /**
     * This method should be implemented in each concrete child class
     * to return the unique identifier for each implementation
     * ex.: group alias for a Group, app id for an Application
     *
     * @return unique identifier String
     */
    public abstract String getUniqueIdentifier ();

    /**
     * Adds a group
     *
     * @param group Group instance to be added
     */
    public void addGroup(Group group) {
        aliasToGroupMap.put(group.getUniqueIdentifier(), group);
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

    private Group travereAndCheckRecursively (Map<String,Group> aliasToGroupMap, String groupAlias) {

        if (aliasToGroupMap.containsKey(groupAlias)) {
            synchronized (aliasToGroupMap) {
                if (aliasToGroupMap.containsKey(groupAlias)) {
                    return aliasToGroupMap.get(groupAlias);
                }
            }
        } else {
            for (Group group : aliasToGroupMap.values()) {
                return travereAndCheckRecursively(group.getAliasToGroupMap(), groupAlias);
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
     * Setter for Dependency Order
     *
     * @param dependencyOrder Dependency Order object
     */
    public void setDependencyOrder(DependencyOrder dependencyOrder) {
        this.dependencyOrder = dependencyOrder;
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
    public Set<ClusterDataHolder> getClusterDataRecursively () {

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
     * Adds InstanceContext of a child to the aliasAndInstanceIdToInstanceContextMap,
     * after initializing the map if not already done.
     *
     * @param alias alias of child
     * @param instanceId instance id of child
     * @param instanceContext InstanceContext object
     */
    public void addInstanceContext (String alias, String instanceId, InstanceContext instanceContext) {

        if (aliasAndInstanceIdToInstanceContextMap == null) {
            synchronized (this) {
                if (aliasAndInstanceIdToInstanceContextMap == null) {
                    aliasAndInstanceIdToInstanceContextMap = new HashMap<AliasAndInstanceId, InstanceContext>();
                }
            }
        }

        aliasAndInstanceIdToInstanceContextMap.put(new AliasAndInstanceId(alias, instanceId), instanceContext);
    }

    /**
     * Retrieves InstanceContext obj. for the given alias and instance id
     *
     * @param alias alias
     * @param instanceId instance id
     * @return InstanceContext obj if exists, else null
     */
    public InstanceContext getInstanceContext (String alias, String instanceId) {

        // if the map is not initialized, return null
        if (aliasAndInstanceIdToInstanceContextMap == null) {
            return null;
        }

        return aliasAndInstanceIdToInstanceContextMap.get(new AliasAndInstanceId(alias, instanceId));
    }

    private void getClusterData (Set<ClusterDataHolder> clusterData, Collection<Group> groups) {

        for (Group group : groups) {
            if (group.getClusterDataMap() != null && !group.getClusterDataMap().isEmpty()) {
                clusterData.addAll(group.getClusterDataMap().values());
                if (group.getGroups() != null) {
                    getClusterData(clusterData, group.getGroups());
                }
            }
        }
    }

    private class AliasAndInstanceId {

        private String alias;
        private String instanceId;

        public AliasAndInstanceId(String alias, String instanceId) {
            this.alias = alias;
            this.instanceId = instanceId;
        }


        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public boolean equals(Object other) {
            if(other == null || !(other instanceof AliasAndInstanceId)) {
                return false;
            }

            if(this == other) {
                return true;
            }

            AliasAndInstanceId that = (AliasAndInstanceId)other;
            return this.alias.equals(that.alias) &&
                    this.instanceId.equals(that.instanceId);
        }

        public int hashCode () {
            return alias.hashCode() + instanceId.hashCode();
        }
    }
}
