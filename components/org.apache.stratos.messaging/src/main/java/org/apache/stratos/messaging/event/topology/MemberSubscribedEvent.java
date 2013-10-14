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

import org.apache.stratos.messaging.domain.topology.Port;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This event is fired by Cloud Controller when a member is subscribed to a cluster.
 */
public class MemberSubscribedEvent extends TopologyEvent implements Serializable {
    private String serviceDomainName;
    private String clusterDomainName;
    private String hostName;
    private Map<String, Port> portMap;
    private String cloudName;
    private String region;
    private Properties properties;

    public MemberSubscribedEvent() {
        this.portMap = new HashMap<String, Port>();
    }

    public String getServiceDomainName() {
        return serviceDomainName;
    }

    public void setServiceDomainName(String serviceDomainName) {
        this.serviceDomainName = serviceDomainName;
    }

    public String getClusterDomainName() {
        return clusterDomainName;
    }

    public void setClusterDomainName(String clusterDomainName) {
        this.clusterDomainName = clusterDomainName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Collection<Port> getPorts() {
        return portMap.values();
    }

    public void addPort(Port port) {
        this.portMap.put(port.getName(), port);
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

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
