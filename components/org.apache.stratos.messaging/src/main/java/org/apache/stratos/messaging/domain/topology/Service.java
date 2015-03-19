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

import org.apache.stratos.messaging.domain.topology.locking.TopologyLockHierarchy;

import java.io.Serializable;
import java.util.*;

/**
 * Defines a service in the topology. A service represents a cartridge type.
 * Key: serviceName
 */
public class Service implements Serializable {

    private static final long serialVersionUID = -8835648141999889756L;

    private final String serviceName;
    private final ServiceType serviceType;
    // Key: Cluster.clusterId
    private Map<String, Cluster> clusterIdClusterMap;
    // Key: Port.proxy
    private Map<Integer, Port> portMap;
    private Properties properties;

    public Service(String serviceName, ServiceType serviceType) {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.clusterIdClusterMap = new HashMap<String, Cluster>();
        this.portMap = new HashMap<Integer, Port>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public Collection<Cluster> getClusters() {
        return clusterIdClusterMap.values();
    }

    public void addCluster(Cluster cluster) {
        this.clusterIdClusterMap.put(cluster.getClusterId(), cluster);
    }

    public void removeCluster(Cluster cluster) {
        this.clusterIdClusterMap.remove(cluster.getClusterId());
        TopologyLockHierarchy.getInstance().removeTopologyLockForCluster(cluster.getClusterId());
    }

    public Cluster removeCluster(String clusterId) {
        Cluster removedCluster = this.clusterIdClusterMap.remove(clusterId);
        TopologyLockHierarchy.getInstance().removeTopologyLockForCluster(clusterId);
        return removedCluster;
    }

    public boolean clusterExists(String clusterId) {
        return this.clusterIdClusterMap.containsKey(clusterId);
    }

    public Cluster getCluster(String clusterId) {
        return this.clusterIdClusterMap.get(clusterId);
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

    @Override
    public String toString() {
        return "Service [serviceName=" + serviceName + ", serviceType=" + serviceType +
                ", clusterIdClusterMap=" + clusterIdClusterMap + ", portMap=" + portMap +
                ", properties=" + properties + "]";
    }
}
