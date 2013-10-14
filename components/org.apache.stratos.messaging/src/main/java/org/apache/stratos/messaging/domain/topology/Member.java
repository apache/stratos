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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Defines a member node in a cluster.
 */
public class Member implements Serializable {
    private String memberId;
    private String hostName;
    private String cloud;
    private String region;
    private MemberStatus status;
    private Map<String, Port> portMap;
    private Properties properties;

    public Member() {
        this.portMap = new HashMap<String, Port>();
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

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return (this.status == MemberStatus.Active);
    }

    public Collection<Port> getPorts() {
        return portMap.values();
    }

    public void addPort(Port port) {
        this.portMap.put(port.getName(), port);
    }

    public void addPorts(Collection<Port> ports) {
        for(Port port: ports) {
            addPort(port);
        }
    }

    public void removePort(Port port) {
        this.portMap.remove(port.getName());
    }

    public void removePort(String portName) {
        this.portMap.remove(portName);
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getName());
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

