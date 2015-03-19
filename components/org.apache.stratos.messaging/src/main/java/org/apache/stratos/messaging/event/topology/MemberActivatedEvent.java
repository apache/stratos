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

package org.apache.stratos.messaging.event.topology;

import java.io.Serializable;
import java.util.*;

import org.apache.stratos.messaging.domain.topology.Port;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This event is fired by Cloud Controller when a member has started it's server and
 * applications are ready to serve the incoming requests.
 */
public class MemberActivatedEvent extends TopologyEvent implements Serializable {
    private static final long serialVersionUID = 5493702477320416932L;

    private final String serviceName;
    private final String clusterId;
    private final String clusterInstanceId;
    private final String memberId;
    private final String networkPartitionId;
    private final String partitionId;

    // Key: Port.proxy
    private Map<Integer, Port> portMap;
    private List<String> memberPrivateIPs;
    private String defaultPrivateIP;
    private String groupId;
    private String applicationId;
    private List<String> memberPublicIPs;
    private String defaultPublicIP;

    public MemberActivatedEvent(String serviceName, String clusterId, String clusterInstanceId, String memberId,
                                String networkPartitionId, String partitionId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.memberId = memberId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
        this.portMap = new HashMap<Integer, Port>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getPartitionId() {
        return this.partitionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public Collection<Port> getPorts() {
        return Collections.unmodifiableCollection(portMap.values());
    }

    public Port getPort(int proxy) {
        if (portMap.containsKey(proxy)) {
            return portMap.get(proxy);
        }
        return null;
    }

    public void addPort(Port port) {
        this.portMap.put(port.getProxy(), port);
    }

    public void addPorts(Collection<Port> ports) {
        for (Port port : ports) {
            addPort(port);
        }
    }

    public void removePort(Port port) {
        this.portMap.remove(port.getProxy());
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getProxy());
    }

    public List<String> getMemberPrivateIPs() {
        return memberPrivateIPs;
    }

    public void setMemberPrivateIPs(List<String> memberPrivateIPs) {
        this.memberPrivateIPs = memberPrivateIPs;
    }

    public String getDefaultPrivateIP() {
        return defaultPrivateIP;
    }

    public void setDefaultPrivateIP(String defaultPrivateIP) {
        this.defaultPrivateIP = defaultPrivateIP;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public List<String> getMemberPublicIPs() {
        return memberPublicIPs;
    }

    public void setMemberPublicIPs(List<String> memberPublicIp) {
        this.memberPublicIPs = memberPublicIp;
    }

    public String getDefaultPublicIP() {
        return defaultPublicIP;
    }

    public void setDefaultPublicIP(String defaultPublicIP) {
        this.defaultPublicIP = defaultPublicIP;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }
}
