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

import org.apache.stratos.messaging.util.bean.type.map.MapAdapter;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.*;

/**
 * Defines a member node in a cluster.
 * Key: serviceName, clusterId, memberId
 */
@XmlRootElement
public class Member implements Serializable {
    private static final long serialVersionUID = 4179661867903664661L;

    private final String serviceName;
    private final String clusterId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String memberId;
    @XmlJavaTypeAdapter(MapAdapter.class)
    private final Map<Integer, Port> portMap;
    private String memberPublicIp;
    private MemberStatus status;
    private String memberIp;
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Properties properties;
    private String lbClusterId;

    public Member(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
        this.memberId = memberId;
        this.portMap = new HashMap<Integer, Port>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getMemberId() {
        return memberId;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return (this.status == MemberStatus.Activated);
    }

    public Port getPort(int proxy) {
        if(portMap.containsKey(proxy)) {
            return portMap.get(proxy);
        }
        return null;
    }

    public Map<Integer, Port> getPorts() {
        return Collections.unmodifiableMap(portMap);
    }

    public void addPort(Port port) {
        this.portMap.put(port.getProxy(), port);
    }

    public void addPorts(Map<Integer, Port> portMap) {
        this.portMap.putAll(portMap);
    }

    public void removePort(Port port) {
        this.portMap.remove(port.getProxy());
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getProxy());
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getMemberIp() {
        return memberIp;
    }

    public void setMemberIp(String memberIp) {
        this.memberIp = memberIp;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getMemberPublicIp() {
        return memberPublicIp;
    }

    public void setMemberPublicIp(String memberPublicIp) {
        this.memberPublicIp = memberPublicIp;
    }

}

