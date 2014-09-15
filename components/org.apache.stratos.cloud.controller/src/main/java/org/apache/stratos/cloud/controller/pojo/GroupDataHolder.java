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

package org.apache.stratos.cloud.controller.pojo;

import org.apache.stratos.messaging.domain.topology.Group;

import java.util.HashMap;
import java.util.Map;

public class GroupDataHolder {

    // Sub Group Map, key = Group.name
    private Map<String, Group> groupMap;
    // Cluster Data Holder
    private ClusterDataHolder clusterDataHolder;
    // nested Group Data
    private GroupDataHolder groupDataHolder;

    public GroupDataHolder () {
        this.groupMap = new HashMap<String, Group>();
    }

    public void addGroup (Group group) {
        groupMap.put(group.getName(), group);
    }

    public void addGroups (Map<String, Group> groupNameToGroup) {
        groupMap.putAll(groupNameToGroup);
    }

    public Map<String, Group> getGroupMap() {
        return groupMap;
    }

    public ClusterDataHolder getClusterDataHolder() {
        return clusterDataHolder;
    }

    public GroupDataHolder getGroupDataHolder() {
        return groupDataHolder;
    }

    public void setGroupDataHolder(GroupDataHolder groupDataHolder) {
        this.groupDataHolder = groupDataHolder;
    }

    public void setClusterDataHolder(ClusterDataHolder clusterDataHolder) {
        this.clusterDataHolder = clusterDataHolder;
    }
}
