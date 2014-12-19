/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.beans.topology;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="groupInstances")
public class GroupInstanceBean implements Serializable {

    private String parentInstanceId;
    private String groupId;
    private String instanceId;
    private String status;
    private List<GroupInstanceBean> groupInstances;
    private List<ClusterInstanceBean> clusterInstances;

    public GroupInstanceBean() {
        groupInstances = new ArrayList<GroupInstanceBean>();
        clusterInstances = new ArrayList<ClusterInstanceBean>();
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

    public String getParentInstanceId() {
        return parentInstanceId;
    }

    public void setParentInstanceId(String parentInstanceId) {
        this.parentInstanceId = parentInstanceId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<GroupInstanceBean> getGroupInstances() {
        return groupInstances;
    }

    public void setGroupInstances(List<GroupInstanceBean> groupInstances) {
        this.groupInstances = groupInstances;
    }

    public List<ClusterInstanceBean> getClusterInstances() {
        return clusterInstances;
    }

    public void setClusterInstances(List<ClusterInstanceBean> clusterInstances) {
        this.clusterInstances = clusterInstances;
    }
}
