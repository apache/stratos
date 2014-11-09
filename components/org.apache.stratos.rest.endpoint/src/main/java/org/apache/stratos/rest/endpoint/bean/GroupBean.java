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
package org.apache.stratos.rest.endpoint.bean;

import org.apache.stratos.rest.endpoint.bean.topology.Cluster;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="groups")
public class GroupBean {
    private List<GroupBean> subGroups = null;
    private List<Cluster> clusters = null;
    private String alias;
    private String deploymentPolicy;
    private String autoScalingPolicy;

    public GroupBean(){
        this.setClusters(new ArrayList<Cluster>());
        this.setSubGroups(new ArrayList<GroupBean>());
    }

    public void addGroup(GroupBean groupBean){
        getSubGroups().add(groupBean);
    }
    public void addCluster(Cluster cluster){
        getClusters().add(cluster);
    }

    public List<GroupBean> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<GroupBean> subGroups) {
        this.subGroups = subGroups;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public void setAutoScalingPolicy(String autoScalingPolicy) {
        this.autoScalingPolicy = autoScalingPolicy;
    }

    public String getAlias() {
        return alias;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public String getAutoScalingPolicy() {
        return autoScalingPolicy;
    }
}
