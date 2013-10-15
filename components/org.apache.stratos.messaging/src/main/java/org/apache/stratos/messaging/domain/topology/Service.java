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

import java.util.*;

/**
 * Defines a service in the topology. A service represents a cartridge type.
 * Key: serviceName
 */
public class Service {
    private String serviceName;
    // Key: Cluster.clusterId
    private Map<String, Cluster> clusterMap;
    private Properties properties;

    public Service() {
        this.clusterMap = new HashMap<String, Cluster>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Collection<Cluster> getClusterMap() {
        return clusterMap.values();
    }

    public void addCluster(Cluster cluster) {
        this.clusterMap.put(cluster.getClusterId(), cluster);
    }

    public void removeCluster(Cluster cluster) {
        this.clusterMap.remove(cluster.getClusterId());
    }

    public void removeCluster(String clusterId) {
        this.clusterMap.remove(clusterId);
    }

    public boolean clusterExists(String clusterId) {
        return this.clusterMap.containsKey(clusterId);
    }

    public Cluster getCluster(String clusterId) {
        return this.clusterMap.get(clusterId);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
