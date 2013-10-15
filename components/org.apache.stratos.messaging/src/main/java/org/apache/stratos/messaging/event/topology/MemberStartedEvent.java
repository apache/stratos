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

import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Port;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This event is fired by Cloud Controller when a member is started by the IaaS in a given cluster.
 */
public class MemberStartedEvent extends TopologyEvent implements Serializable {
    private String serviceName;
    private String clusterId;
    private String memberId;
    private String hostName;
    private MemberStatus status;
    private Map<String, Port> portMap;
    private Properties properties;

    public MemberStartedEvent() {
        this.portMap = new HashMap<String, Port>();
    }

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

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
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

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
