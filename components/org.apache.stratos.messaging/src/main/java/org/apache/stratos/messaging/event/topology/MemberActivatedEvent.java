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

/**
 * This event is fired by Cloud Controller when a member has started it's server and
 * applications are ready to serve the incoming requests.
 */
public class MemberActivatedEvent extends TopologyEvent implements Serializable {
    private static final long serialVersionUID = 5493702477320416932L;

    private final String serviceName;
    private final String clusterId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String memberId;
    private Map<Integer, Port> portMap;
    private String memberIp;

    public MemberActivatedEvent(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId) {
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

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getPartitionId(){
        return this.partitionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public Map<Integer, Port> getPorts() {
        return Collections.unmodifiableMap(portMap);
    }

    public void addPort(Port port) {
        this.portMap.put(port.getProxy(), port);
    }

    public void addPorts(Map<Integer, Port> portSet) {
        this.portMap.putAll(portSet);
    }

    public void removePort(Port port) {
        this.portMap.remove(port.getProxy());
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getProxy());
    }

	public String getMemberIp() {
	    return memberIp;
    }

	public void setMemberIp(String memberIp) {
	    this.memberIp = memberIp;
    }
}
