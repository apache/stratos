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

import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;

public class Application implements ParentBehavior {

    private static final long serialVersionUID = -5092959597171649688L;
    // Unique id for the Application, defined in Application Definition
    private String id;
    // Key used for authentication (with metadata service, etc.)
    private String key;
    // tenant id
    private int tenantId;
    // tenant domain
    private String tenantDomain;
    // tenant admin user
    private String tenantAdminUserName;
    // Dependency Order
    private DependencyOrder dependencyOrder;
    // Group Map, key = Group.alias
    private Map<String, Group> aliasToGroupMap;
    // Cluster Id map, key = subscription alias for the cartridge type
    private Map<String, ClusterDataHolder> aliasToClusterDataMap;
    // Application status
    private Status status;

    public Application (String id) {
        this.id = id;
        this.key = RandomStringUtils.randomAlphanumeric(16);
        this.status = Status.Created;
        aliasToGroupMap = new HashMap<String, Group>();
        aliasToClusterDataMap = new HashMap<String, ClusterDataHolder>();
    }

    @Override
    public void addGroup(Group group) {
        aliasToGroupMap.put(group.getName(), group);
    }

    @Override
    public void setGroups(Map<String, Group> aliasToGroup) {
        aliasToGroupMap.putAll(aliasToGroup);
    }

    @Override
    public Group getGroup(String groupName) {
        return aliasToGroupMap.get(groupName);
    }

    @Override
    public Map<String, Group> getAliasToGroupMap() {
        return this.aliasToGroupMap;
    }

    @Override
    public Map<String, ClusterDataHolder> getClusterDataMap() {
        return this.aliasToClusterDataMap;
    }

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

    @Override
    public void setClusterData(Map<String, ClusterDataHolder> aliasToClusterData) {
        this.aliasToClusterDataMap.putAll(aliasToClusterData);
    }

    @Override
    public ClusterDataHolder getClusterData(String alias) {
        return aliasToClusterDataMap.get(alias);
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

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getTenantAdminUserName() {
        return tenantAdminUserName;
    }

    public void setTenantAdminUserName(String tenantAdminUserName) {
        this.tenantAdminUserName = tenantAdminUserName;
    }
}
