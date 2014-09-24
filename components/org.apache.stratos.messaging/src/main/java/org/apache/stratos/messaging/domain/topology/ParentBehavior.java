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
import java.util.Set;

public interface ParentBehavior extends Serializable {

    public void addGroup (Group group);

    public void setGroups (Map<String, Group> groupNameToGroup);

    public Group getGroup (String groupName);

    public Map<String, Group> getAliasToGroupMap();

    public Map<String, ClusterDataHolder> getClusterDataMap();

    public Group getGroupRecursively (String groupAlias);

    public Collection<Group> getGroups ();

    public void setDependencyOrder (DependencyOrder dependencyOrder);

    public DependencyOrder getDependencyOrder ();

    public void setClusterData (Map<String, ClusterDataHolder> aliasToClusterData);

    public ClusterDataHolder getClusterData (String alias);
}
