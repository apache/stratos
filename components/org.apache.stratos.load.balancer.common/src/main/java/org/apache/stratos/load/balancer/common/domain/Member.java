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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Load balancer member definition.
 */
public class Member {

    private String serviceName;
    private String clusterId;
    private String memberId;
    private String hostName;
    private Map<Integer, Port> portMap;
    private String instanceId;
    private Properties properties;

	public Member(String serviceName, String clusterId, String memberId, String hostName) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.memberId = memberId;
        this.hostName = hostName;
        this.portMap = new HashMap<Integer, Port>();
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getHostName() {
        return hostName;
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

    public Collection getPorts() {
        return portMap.values();
    }

    public String getServiceName() {
        return serviceName;
    }
    
    public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
