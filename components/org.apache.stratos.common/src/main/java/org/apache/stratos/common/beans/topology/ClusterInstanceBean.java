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
import java.util.List;

@XmlRootElement(name="clusterInstances")
public class ClusterInstanceBean implements Serializable {

	private String status;
	private String instanceId;
    private String parentInstanceId;
    private String alias;
    private String serviceName;
    private String clusterId;
    private List<MemberBean> member;
    private String tenantRange;
    private List<String> hostNames;

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public List<MemberBean> getMember() {
        return member;
    }

    public void setMember(List<MemberBean> member) {
        this.member = member;
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public List<String> getHostNames() {
        return hostNames;
    }

    public void setHostNames(List<String> hostNames) {
        this.hostNames = hostNames;
    }

    @Override
    public String toString() {
        return "Cluster [serviceName=" + getServiceName() + ", clusterId=" + getClusterId() + ", member=" + getMember()
                + ", tenantRange=" + getTenantRange() + ", hostNames=" + getHostNames();
    }

    public String getParentInstanceId() {
        return parentInstanceId;
    }

    public void setParentInstanceId(String parentInstanceId) {
        this.parentInstanceId = parentInstanceId;
    }
}
