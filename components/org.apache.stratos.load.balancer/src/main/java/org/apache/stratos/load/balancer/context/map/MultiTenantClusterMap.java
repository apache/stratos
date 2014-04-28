/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.load.balancer.context.map;

import org.apache.stratos.messaging.domain.topology.Cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-tenant cluster map for accessing clusters using host name and tenant id:
 * Map[HostName, Map[TenantId,Cluster]]
 */
public class MultiTenantClusterMap {

    private ConcurrentHashMap<String, Map<Integer, Cluster>> concurrentHashMap;

    public MultiTenantClusterMap() {
        concurrentHashMap = new ConcurrentHashMap<String, Map<Integer, Cluster>>();
    }

    public Cluster getCluster(String hostName, int tenantId) {
        Map<Integer, Cluster> clusterMap = getClusters(hostName);
        if (clusterMap != null) {
            return clusterMap.get(tenantId);
        }
        return null;
    }

    public Map<Integer, Cluster> getClusters(String hostName) {
        return concurrentHashMap.get(hostName);
    }

    public boolean containsClusters(String hostName) {
        return concurrentHashMap.containsKey(hostName);
    }

    public void addClusters(String hostname, Map<Integer, Cluster> clusters) {
        concurrentHashMap.put(hostname, clusters);
    }

    public void removeClusters(String hostName) {
        concurrentHashMap.remove(hostName);
    }

    public void clear() {
        concurrentHashMap.clear();
    }
}
