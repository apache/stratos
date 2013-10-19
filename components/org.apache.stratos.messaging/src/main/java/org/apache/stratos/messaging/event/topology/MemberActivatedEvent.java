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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.stratos.messaging.domain.topology.Port;

/**
 * This event is fired by Cloud Controller when a member has started it's server and
 * applications are ready to serve the incoming requests.
 */
public class MemberActivatedEvent extends TopologyEvent implements Serializable {
    private static final long serialVersionUID = 5493702477320416932L;
	private String serviceName;
    private String clusterId;
    private String memberId;
    private Map<String, Port> portMap;
    private String memberIp;

    public MemberActivatedEvent() {
    	this.portMap = new HashMap<String, Port>();
    }
    
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public Collection<Port> getPorts() {
        return portMap.values();
    }

    public void addPort(Port port) {
        this.portMap.put(port.getProtocol(), port);
    }

    public void removePort(Port port) {
        this.portMap.remove(port.getProtocol());
    }

    public void removePort(String portName) {
        this.portMap.remove(portName);
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getProtocol());
    }

    public Port getPort(String portName) {
        return this.portMap.get(portName);
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

	public String getMemberIp() {
	    return memberIp;
    }

	public void setMemberIp(String memberIp) {
	    this.memberIp = memberIp;
    }
}
