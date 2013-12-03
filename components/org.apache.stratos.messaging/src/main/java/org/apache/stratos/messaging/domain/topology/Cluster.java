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

package org.apache.stratos.messaging.domain.topology;

import org.apache.stratos.messaging.util.Util;

import java.io.Serializable;
import java.util.*;

/**
 * Defines a cluster of a service.
 * Key: serviceName, clusterId
 */
public class Cluster implements Serializable {

	private static final long serialVersionUID = -361960242360176077L;
	
	private String serviceName;
    private String clusterId;
    private List<String> hostNames;
    private String tenantRange;
    private String autoscalePolicyName;
    private String deploymentPolicyName = "economy-deployment";
    private Cloud cloud;
    private Region region;
    private Zone zone;
    
    // Key: Member.memberId
    private Map<String, Member> memberMap;
    private String loadBalanceAlgorithmName;
    private Properties properties;

    public Cluster(String serviceName, String clusterId, String autoscalePolicyName) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.hostNames = new ArrayList<String>();
        this.autoscalePolicyName = autoscalePolicyName;
        this.memberMap = new HashMap<String, Member>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public List<String> getHostNames() {
        return hostNames;
    }

    public void addHostName(String hostName) {
        this.hostNames.add(hostName);
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        Util.validateTenantRange(tenantRange);
        this.tenantRange = tenantRange;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public Collection<Member> getMembers() {
        return memberMap.values();
    }

    public void addMember(Member member) {
        memberMap.put(member.getMemberId(), member);
    }

    public void removeMember(Member member) {
        memberMap.remove(member.getMemberId());
    }

    public Member getMember(String memberId) {
        return memberMap.get(memberId);
    }

    public boolean memberExists(String memberId) {
        return this.memberMap.containsKey(memberId);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getAutoscalePolicyName() {
        return autoscalePolicyName;
    }

    public void setAutoscalePolicyName(String autoscalePolicyName) {
        this.autoscalePolicyName = autoscalePolicyName;
    }

	public String getDeploymentPolicyName() {
		return deploymentPolicyName;
	}

	public void setDeploymentPolicyName(String deploymentPolicy) {
		this.deploymentPolicyName = deploymentPolicy;
	}

    public String getLoadBalanceAlgorithmName() {
        return loadBalanceAlgorithmName;
    }

    public void setLoadBalanceAlgorithmName(String loadBalanceAlgorithmName) {
        this.loadBalanceAlgorithmName = loadBalanceAlgorithmName;
    }
}

