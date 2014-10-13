package org.apache.stratos.cli.beans.topology;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import java.util.List;
import java.util.Properties;

import org.apache.stratos.cli.beans.autoscaler.partition.PropertyBean;

public class Cluster {
    private String serviceName;

    private String clusterId;

    private List<Member> member;

    private String tenantRange;

    private List<String> hostNames;

    private boolean isLbCluster;
    
    private List<PropertyBean> property;

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

    public List<Member> getMember() {
        return member;
    }

    public void addMember(Member member) {
       this.member.add(member);
    }

    public void removeMember(Member member) {
       this.member.remove(member);
    }

    public void setMember(List<Member> member) {
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

    public void addHostNames(String hostName) {
        this.hostNames.add(hostName);
    }

    public void removeHostNames(String hostName) {
        this.hostNames.remove(hostName);
    }

    public boolean isLbCluster() {
        return isLbCluster;
    }

    public void setLbCluster(boolean lbCluster) {
        isLbCluster = lbCluster;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

}
