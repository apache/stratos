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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
    // Group Map, key = Group.name
    private Map<String, Group> groupMap;
    // Cluster Id map, key = service name
    private Map<String, String> clusterIdMap;
    // Application status
    private Status status;

    public Application (String id) {
        this.id = id;
        this.key = RandomStringUtils.randomAlphanumeric(16);
        this.status = Status.Created;
        groupMap = new HashMap<String, Group>();
        clusterIdMap = new HashMap<String, String>();
    }

    @Override
    public void addGroup(Group group) {
        groupMap.put(group.getName(), group);
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
