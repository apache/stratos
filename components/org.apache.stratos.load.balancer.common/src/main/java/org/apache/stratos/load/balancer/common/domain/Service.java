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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load balancer topology definition.
 */
public class Service {
    private final String serviceName;
    // Key: Cluster.clusterId
    private Map<String, Cluster> clusterIdClusterMap;
    private Map<Integer, Port> portMap;
    private boolean multiTenant;

    public Service(String serviceName) {
        this.serviceName = serviceName;
        this.clusterIdClusterMap = new ConcurrentHashMap<String, Cluster>();
        this.portMap = new ConcurrentHashMap<Integer, Port>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public Collection<Cluster> getClusters() {
        return clusterIdClusterMap.values();
    }

    public void addCluster(Cluster cluster) {
        this.clusterIdClusterMap.put(cluster.getClusterId(), cluster);
    }

    public void removeCluster(String clusterId) {
        this.clusterIdClusterMap.remove(clusterId);
    }

    public boolean clusterExists(String clusterId) {
        return this.clusterIdClusterMap.containsKey(clusterId);
    }

    public Cluster getCluster(String clusterId) {
        return this.clusterIdClusterMap.get(clusterId);
    }

    public boolean isMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(boolean multiTenant) {
        this.multiTenant = multiTenant;
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
            if (!portExists(port)) {
                addPort(port);
            }
        }
    }

    public void removePort(Port port) {
        if (portExists(port)) {
            this.portMap.remove(port.getProxy());
        }
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getProxy());
    }
}
