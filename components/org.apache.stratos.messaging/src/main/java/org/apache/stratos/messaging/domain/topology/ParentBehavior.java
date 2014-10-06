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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Abstraction for a an entity that can have child entities
 * in an Application withing the Topology
 */

public interface ParentBehavior extends Serializable {

    /**
     * Adds a group
     *
     * @param group Group instance to be added
     */
    public void addGroup (Group group);

    /**
     * Setter for Group alias to Group map
     *
     * @param groupAliasToGroup Map, key = alias given to the Group, value = Group
     */
    public void setGroups (Map<String, Group> groupAliasToGroup);

    /**
     * Get the Group for the given alias in the context of the parent entity.
     * Will not search recursively in the nested levels.
     *
     * @param groupAlias alias of the Group
     * @return Group, if exists for the given alias, else null
     */
    public Group getGroup (String groupAlias);

    /**
     * Get the Group for the given alias in the context of the parent entity.
     * Will search recursively in the nested levels.
     *
     * @param groupAlias alias of the Group
     * @return Group, if exists for the given alias, else null
     */
    public Group getGroupRecursively (String groupAlias);

    /**
     * Getter for alias to Group map
     * Will not search recursively in the nested levels.
     *
     * @return Map, key = alias given to the Group, value = Group
     */
    public Map<String, Group> getAliasToGroupMap();

    /**
     * Getter for cluster alias to ClusterData map for this level
     *
     * @return Map, key = alias given to the cluster, value =  ClusterData object
     */
    public Map<String, ClusterDataHolder> getClusterDataMap();

    /**
     * Collection of Groups in this level
     *
     * @return Group Collection object, empty if no Groups are found
     */
    public Collection<Group> getGroups ();

    /**
     * Setter for Dependency Order
     *
     * @param dependencyOrder Dependency Order object
     */
    public void setDependencyOrder (DependencyOrder dependencyOrder);

    /**
     * Getter for Dependency Order for this level
     *
     * @return Dependency Order object
     */
    public DependencyOrder getDependencyOrder ();

    /**
     * Setter for alias to Cluster Data map
     *
     * @param aliasToClusterData Map, key = alias given to the cluster, value =  ClusterData object
     */
    public void setClusterData (Map<String, ClusterDataHolder> aliasToClusterData);

    /**
     * Getter for Cluster Data instance for the given alias
     * Will not search recursively in the nested levels.
     *
     * @param alias
     * @return
     */
    public ClusterDataHolder getClusterData (String alias);
}
