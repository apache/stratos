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

package org.apache.stratos.load.balancer.common.domain;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load balancer cluster definition.
 */
public class Cluster {

    private static final Log log = LogFactory.getLog(Cluster.class);

    private String serviceName;
    private String clusterId;
    private Set<String> hostNames;
    private String tenantRange;
    private Map<String, Member> memberMap;
    private Map<String, String> hostNameToContextPathMap;
    private String loadBalanceAlgorithmName;
    private Properties properties;

    public Cluster(String serviceName, String clusterId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.hostNames = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.memberMap = new HashMap<String, Member>();
        this.hostNameToContextPathMap = new ConcurrentHashMap<String, String>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public void addHostName(String hostName) {
        hostNames.add(hostName);
    }

    public void addHostName(String hostName, String contextPath) {
        hostNames.add(hostName);
        hostNameToContextPathMap.put(hostName, contextPath);
    }

    public void addMember(Member member) {
        memberMap.put(member.getMemberId(), member);
    }

    public void removeMember(String memberId) {
        Member member = memberMap.get(memberId);
        if (member == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Could not remove member, member not found: [member] %s", memberId));
            }
            return;
        }

        memberMap.remove(memberId);
    }

    public Member getMember(String memberId) {
        return memberMap.get(memberId);
    }

    public Collection<Member> getMembers() {
        return memberMap.values();
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    /**
     * Check whether a given tenant id is in tenant range of the cluster.
     *
     * @param tenantId
     * @return
     */
    public boolean tenantIdInRange(int tenantId) {
        if (StringUtils.isEmpty(getTenantRange())) {
            return false;
        }

        if ("*".equals(getTenantRange())) {
            return true;
        } else {
            String[] array = getTenantRange().split("-");
            int tenantStart = Integer.parseInt(array[0]);
            if (tenantStart <= tenantId) {
                String tenantEndStr = array[1];
                if ("*".equals(tenantEndStr)) {
                    return true;
                } else {
                    int tenantEnd = Integer.parseInt(tenantEndStr);
                    if (tenantId <= tenantEnd) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setLoadBalanceAlgorithmName(String loadBalanceAlgorithmName) {
        this.loadBalanceAlgorithmName = loadBalanceAlgorithmName;
    }

    public String getLoadBalanceAlgorithmName() {
        return loadBalanceAlgorithmName;
    }

    public String getContextPath(String hostName) {
        return hostNameToContextPathMap.get(hostName);
    }

    public void removeHostName(String hostName) {
        hostNames.remove(hostName);
        hostNameToContextPathMap.remove(hostName);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
