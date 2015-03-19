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

import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateManager;
import org.apache.stratos.messaging.adapters.MapAdapter;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateTransitionBehavior;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.*;

/**
 * Defines a member node in a cluster.
 * Key: serviceName, clusterId, memberId
 */
@XmlRootElement
public class Member implements Serializable, LifeCycleStateTransitionBehavior<MemberStatus> {
    private static final long serialVersionUID = 4179661867903664661L;

    private final String serviceName;
    private final String clusterId;
    private final String memberId;
    private final String clusterInstanceId;
    private final String networkPartitionId;
    private final String partitionId;

    // member initiated time
    private final long initTime;
    // Key: Port.proxy
    @XmlJavaTypeAdapter(MapAdapter.class)
    private final Map<Integer, Port> portMap;
    private List<String> memberPublicIPs;
    private String defaultPublicIP;
    //private MemberStatus status;
    private List<String> memberPrivateIPs;
    private String defaultPrivateIP;
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Properties properties;
    private String lbClusterId;
    // instance id to use if snapshot wise group scaling is enabled
    private LifeCycleStateManager<MemberStatus> memberStateManager;
    private LoadBalancingIPType loadBalancingIPType;

    public Member(String serviceName, String clusterId, String memberId, String clusterInstanceId,
                  String networkPartitionId, String partitionId, LoadBalancingIPType loadBalancingIPType,
                  long initTime) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
        this.memberId = memberId;
        this.portMap = new HashMap<Integer, Port>();
        this.loadBalancingIPType = loadBalancingIPType;
        this.initTime = initTime;
        this.memberStateManager = new LifeCycleStateManager<MemberStatus>(MemberStatus.Created, memberId);
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

    public long getInitTime() {
        return initTime;
    }


    @Override
    public boolean isStateTransitionValid(MemberStatus newState) {
        return memberStateManager.isStateTransitionValid(newState);
    }

    @Override
    public MemberStatus getStatus() {
        return memberStateManager.getCurrentState();
    }

    public Stack<MemberStatus> getTransitionedStates() {
        return memberStateManager.getStateStack();
    }

    @Override
    public boolean setStatus(MemberStatus newState) {
        return this.memberStateManager.changeState(newState);
    }

    public boolean isActive() {
        return memberStateManager.getCurrentState().equals(MemberStatus.Active);
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

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getDefaultPrivateIP() {
        return defaultPrivateIP;
    }

    public void setDefaultPrivateIP(String defaultPrivateIP) {
        this.defaultPrivateIP = defaultPrivateIP;
    }

    public List<String> getMemberPrivateIPs() {
        return memberPrivateIPs;
    }

    public void setMemberPrivateIPs(List<String> memberPrivateIPs) {
        this.memberPrivateIPs = memberPrivateIPs;
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

    public String getDefaultPublicIP() {
        return defaultPublicIP;
    }

    public void setDefaultPublicIP(String defaultPublicIP) {
        this.defaultPublicIP = defaultPublicIP;
    }

    public List<String> getMemberPublicIPs() {
        return memberPublicIPs;
    }

    public void setMemberPublicIPs(List<String> memberPublicIPs) {
        this.memberPublicIPs = memberPublicIPs;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public LoadBalancingIPType getLoadBalancingIPType() {
        return loadBalancingIPType;
    }

    @Override
    public String toString() {
        return "Member [serviceName=" + getServiceName()
                + ", clusterId=" + getClusterId()
                + ", memberId=" + getMemberId()
                + ", clusterInstanceId=" + getClusterInstanceId()
                + ", networkPartitionId=" + getNetworkPartitionId()
                + ", partitionId=" + getPartitionId()
                + ", initTime=" + getInitTime()
                + ", portMap=" + getPorts()
                + ", defaultPublicIP=" + getDefaultPublicIP()
                + ", memberPublicIPs=" + ((memberPublicIPs != null) ? memberPublicIPs.toString() : "")
                + ", status=" + getStatus()
                + ", defaultPrivateIP=" + getDefaultPrivateIP()
                + ", memberPrivateIPs=" + ((memberPrivateIPs != null) ? memberPrivateIPs.toString() : "")
                + ", lbClusterId=" + getLbClusterId()
                + ", loadBalancingIPType=" + getLoadBalancingIPType()
                + ", properties=" + getProperties() + "]";
    }
}

