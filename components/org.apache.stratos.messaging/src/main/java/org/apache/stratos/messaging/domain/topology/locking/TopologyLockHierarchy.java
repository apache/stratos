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

package org.apache.stratos.messaging.domain.topology.locking;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TopologyLockHierarchy {

    private static final Log log = LogFactory.getLog(TopologyLockHierarchy.class);

    // lock for te full Topology
    private final TopologyLock completeTopologyLock;

    // lock for Services
    private final TopologyLock serviceLock;

    // key = Service.name
    private final Map<String, TopologyLock> serviceNameToTopologyLockMap;

    // key = Cluster.id
    private final Map<String, TopologyLock> clusterIdToTopologyLockMap;

    private static volatile TopologyLockHierarchy topologyLockHierarchy;

    private TopologyLockHierarchy () {

        this.completeTopologyLock = new TopologyLock();
        this.serviceLock = new TopologyLock();
        this.serviceNameToTopologyLockMap = new ConcurrentHashMap<String, TopologyLock>();
        this.clusterIdToTopologyLockMap = new ConcurrentHashMap<String, TopologyLock>();
    }

    public static TopologyLockHierarchy getInstance () {

        if (topologyLockHierarchy == null) {
            synchronized (TopologyLockHierarchy.class) {
                if (topologyLockHierarchy == null) {
                    topologyLockHierarchy = new TopologyLockHierarchy();
                }
            }
        }

        return topologyLockHierarchy;
    }

    public void addServiceLock (String serviceName, final TopologyLock topologyLock) {

        if (!serviceNameToTopologyLockMap.containsKey(serviceName)) {
            synchronized (serviceNameToTopologyLockMap) {
                if (!serviceNameToTopologyLockMap.containsKey(serviceName)) {
                    serviceNameToTopologyLockMap.put(serviceName, topologyLock);
                    log.info("Added lock for Service " + serviceName);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Topology Lock for Service " + serviceName + " already exists");
            }
        }
    }

    public TopologyLock getTopologyLockForService (String serviceName) {
        return serviceNameToTopologyLockMap.get(serviceName);
    }

    public void addClusterLock (String clusterId, final TopologyLock topologyLock) {

        if (!clusterIdToTopologyLockMap.containsKey(clusterId)) {
            synchronized (clusterIdToTopologyLockMap) {
                if (!clusterIdToTopologyLockMap.containsKey(clusterId)) {
                    clusterIdToTopologyLockMap.put(clusterId, topologyLock);
                    log.info("Added lock for Cluster " + clusterId);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Topology Lock for Cluster " + clusterId + " already exists");
            }
        }
    }

    public TopologyLock getTopologyLockForCluster (String clusterId) {
        return clusterIdToTopologyLockMap.get(clusterId);
    }

    public void removeTopologyLockForService (String serviceName) {
        if (serviceNameToTopologyLockMap.remove(serviceName) != null) {
            log.info("Removed lock for Service " + serviceName);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Lock already removed for Service " + serviceName);
            }
        }
    }

    public void removeTopologyLockForCluster (String clusterId) {
        if (clusterIdToTopologyLockMap.remove(clusterId) != null) {
            log.info("Removed lock for Cluster " + clusterId);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Lock already removed for Cluster " + clusterId);
            }
        }
    }

    public TopologyLock getServiceLock() {
        return serviceLock;
    }

    public TopologyLock getCompleteTopologyLock() {
        return completeTopologyLock;
    }
}
