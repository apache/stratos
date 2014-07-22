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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Host/domain name cluster map for accessing clusters using host name:
 * Map[HostName, Cluster]
 */
public class HostNameClusterMap {

    private ConcurrentHashMap<String, Cluster> concurrentHashMap;

    public HostNameClusterMap() {
        concurrentHashMap = new ConcurrentHashMap<String, Cluster>();
    }

    public Cluster getCluster(String hostName) {
        return concurrentHashMap.get(hostName);
    }

    public boolean containsCluster(String hostName) {
        return concurrentHashMap.containsKey(hostName);
    }

    public void addCluster(String hostName, Cluster cluster) {
        concurrentHashMap.put(hostName, cluster);
    }

    public void removeCluster(Cluster cluster) {
        for (String hostName : cluster.getHostNames()) {
            removeCluster(hostName);
        }
    }

    public void removeCluster(String hostName) {
        concurrentHashMap.remove(hostName);
    }

    public void clear() {
        concurrentHashMap.clear();
    }
}
