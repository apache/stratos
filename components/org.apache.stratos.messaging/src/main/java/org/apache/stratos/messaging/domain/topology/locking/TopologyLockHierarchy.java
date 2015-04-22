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

    private TopologyLockHierarchy() {
        this.completeTopologyLock = new TopologyLock();
        this.serviceLock = new TopologyLock();
        this.serviceNameToTopologyLockMap = new ConcurrentHashMap<String, TopologyLock>();
        this.clusterIdToTopologyLockMap = new ConcurrentHashMap<String, TopologyLock>();
    }

    public static TopologyLockHierarchy getInstance() {
        if (topologyLockHierarchy == null) {
            synchronized (TopologyLockHierarchy.class) {
                if (topologyLockHierarchy == null) {
                    topologyLockHierarchy = new TopologyLockHierarchy();
                }
            }
        }
        return topologyLockHierarchy;
    }

    public TopologyLock getTopologyLockForService(String serviceName, boolean forceCreationIfNotFound) {
        TopologyLock topologyLock = serviceNameToTopologyLockMap.get(serviceName);
        if (topologyLock == null && forceCreationIfNotFound) {
            synchronized (TopologyLockHierarchy.class) {
                if (topologyLock == null) {
                    topologyLock = new TopologyLock();
                    serviceNameToTopologyLockMap.put(serviceName, topologyLock);
                }
            }
        }
        return topologyLock;
    }

    public TopologyLock getTopologyLockForCluster(String clusterId, boolean forceCreationIfNotFound) {
        TopologyLock topologyLock = clusterIdToTopologyLockMap.get(clusterId);
        if (topologyLock == null && forceCreationIfNotFound) {
            synchronized (TopologyLockHierarchy.class) {
                if (topologyLock == null) {
                    topologyLock = new TopologyLock();
                    clusterIdToTopologyLockMap.put(clusterId, topologyLock);
                }
            }
        }
        return topologyLock;
    }

    public void removeTopologyLockForService(String serviceName) {
        if (serviceNameToTopologyLockMap.remove(serviceName) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Removed lock for service " + serviceName);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Lock already removed for service " + serviceName);
            }
        }
    }

    public void removeTopologyLockForCluster(String clusterId) {
        if (clusterIdToTopologyLockMap.remove(clusterId) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Removed lock for cluster " + clusterId);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Lock already removed for cluster " + clusterId);
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
