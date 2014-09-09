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

public class Application implements SubscribableBehavior {

    // Unique id for the Application, defined in Application Definition
    private String id;
    // Key used for authentication (with metadata service, etc.)
    private String key;
    // Dependency Order
    private DependencyOrder dependencyOrder;
    // Group Map, key = Group.name
    private Map<String, Group> groupMap;
    // Cluster Id map, key = service name
    private Map<String, String> clusterIdMap;

    public Application (String id) {
        this.id = id;
        this.key = RandomStringUtils.randomAlphanumeric(16);
        groupMap = new HashMap<String, Group>();
        clusterIdMap = new HashMap<String, String>();
    }

    @Override
    public void addGroup(Group group) {
        groupMap.put(group.getName(), group);
    }

    @Override
    public Group getGroup(String groupName) {
        return groupMap.get(groupName);
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
    public String getClusterId(String serviceName) {
        return clusterIdMap.get(serviceName);
    }

    @Override
    public Collection<String> getClusterIds() {
        return clusterIdMap.values();
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }
}
