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
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="applicationInstances")
public class ApplicationInstanceBean {

	private String status;
	private String instanceId;
    private String parentInstanceId;
    private String applicationId;
    private List<GroupInstanceBean> groupInstances;
    private List<ClusterInstanceBean> clusterInstances;

    public ApplicationInstanceBean() {
        groupInstances = new ArrayList<GroupInstanceBean>();
        clusterInstances = new ArrayList<ClusterInstanceBean>();
    }

    public void addGroupInstance(GroupInstanceBean groupInstance) {
        this.getGroups().add(groupInstance);
    }

    public void addClusterInstance(ClusterInstanceBean clusterInstance) {
        this.getClusterInstances().add(clusterInstance);
    }
    public List<ClusterInstanceBean> getClusterInstances() {
        return clusterInstances;
    }

    public void setGroupInstances(List<GroupInstanceBean> instances) {
        this.groupInstances = instances;
    }

    public List<GroupInstanceBean> getGroups() {
        return groupInstances;
    }

    public void setGroups(List<GroupInstanceBean> groups) {
        this.groupInstances = groups;
    }

    public List<GroupInstanceBean> getGroupInstances() {
        return groupInstances;
    }

    public void setClusters(List<ClusterInstanceBean> clusters) {
        this.clusterInstances = clusters;
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

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
}
