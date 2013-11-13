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
    private String hostName;
    private String tenantRange;
    private String autoscalePolicyName;
    private String haPolicyName;
    private Cloud cloud;
    private Region region;
    private Zone zone;
    
    // Key: Member.memberId
    private Map<String, Member> memberMap;
    private Properties properties;
    private Map<String, Member> membertoNodeIdMap;

    public Cluster(String serviceName, String clusterId, String autoscalePolicyName) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.autoscalePolicyName = autoscalePolicyName;
        this.memberMap = new HashMap<String, Member>();
        this.membertoNodeIdMap = new HashMap<String, Member>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
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

    public void addMemberToIaasNodeId(Member member) {
           membertoNodeIdMap.put(member.getIaasNodeId(), member);
       }

       public void removeMemberFromIaasNodeId(Member member) {
           membertoNodeIdMap.remove(member.getIaasNodeId());
       }

       public Member getMemberFromIaasNodeId(String iaasNodeId) {
           return membertoNodeIdMap.get(iaasNodeId);
       }

       public boolean memberExistsFromIaasNodeId(String iaasNodeId) {
           return this.membertoNodeIdMap.containsKey(iaasNodeId);
       }

	public String getHaPolicyName() {
		return haPolicyName;
	}

	public void setHaPolicyName(String haPolicyName) {
		this.haPolicyName = haPolicyName;
	}

}

