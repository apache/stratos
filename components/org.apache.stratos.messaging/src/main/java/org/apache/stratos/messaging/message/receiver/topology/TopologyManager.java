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

package org.apache.stratos.messaging.message.receiver.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.domain.topology.locking.TopologyLock;
import org.apache.stratos.messaging.domain.topology.locking.TopologyLockHierarchy;

/**
 * A singleton class for managing the topology data structure.
 * <p/>
 * Usage:
 * Acquire a relevant lock and invoke the getTopology() method inside a try block.
 * Once processing is done release the lock using a finally block.
 * <p/>
 * Acquiring Locks:
 * <p/>
 * Stratos supports hierarchical locking. As per the practice, we need to lock the
 * hierarchy from root level till the relevant sub tree.
 * <p/>
 * Acquire a read lock:
 * <p/>
 * From root level, acquire read locks till the relevant sub tree
 * <p/>
 * Examples -
 * <p/>
 * Example 1: Acquiring read lock to read Cluster information
 * acquiring:
 * public static void acquireReadLockForCluster (String serviceName, String clusterId)
 * <p/>
 * releasing:
 * public static void releaseReadLockForCluster (String serviceName, String clusterId)
 * <p/>
 * <p/>
 * Example 2: Acquiring read lock for a particular Service
 * acquiring:
 * public static void acquireReadLockForService (String serviceName)
 * <p/>
 * releasing:
 * public static void releaseReadLockForService (String serviceName)
 */
public class TopologyManager {
    private static final Log log = LogFactory.getLog(TopologyManager.class);

    private static volatile Topology topology;
    private static volatile TopologyLockHierarchy topologyLockHierarchy =
            TopologyLockHierarchy.getInstance();

    /**
     * Acquires read lock for the Complete Topology
     */
    public static void acquireReadLock() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock acquired for Topology");
        }
        topologyLockHierarchy.getCompleteTopologyLock().acquireReadLock();
    }

    /**
     * Releases read lock for the Complete Topology
     */
    public static void releaseReadLock() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock released for Topology");
        }
        topologyLockHierarchy.getCompleteTopologyLock().releaseReadLock();
    }

    // Service read locks

    /**
     * Acquires read lock for the all Services
     */
    public static void acquireReadLockForServices() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock acquired for Services");
        }
        topologyLockHierarchy.getServiceLock().acquireReadLock();
    }

    /**
     * Releases read lock for the all Services
     */
    public static void releaseReadLockForServices() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock released for Services");
        }
        topologyLockHierarchy.getServiceLock().releaseReadLock();
    }

    /**
     * Acquires read lock for a Service
     *
     * @param serviceName service name to acquire read lock
     */
    public static void acquireReadLockForService(String serviceName) {

        // acquire read lock for all Services
        acquireReadLockForServices();

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName, true);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.acquireReadLock();
            if (log.isDebugEnabled()) {
                log.debug("Read lock acquired for Service " + serviceName);
            }
        }
    }

    /**
     * Releases read lock for a Service
     *
     * @param serviceName service name to release read lock
     */
    public static void releaseReadLockForService(String serviceName) {

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName, false);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.releaseReadLock();
            if (log.isDebugEnabled()) {
                log.debug("Read lock released for Service " + serviceName);
            }
        }

        // release read lock for all Services
        releaseReadLockForServices();
    }

    /**
     * Acquires read lock for a Cluster. This will acquire the read lock in the following order
     * 1. for the Service
     * 2. for the Cluster
     *
     * @param serviceName service name to acquire read lock
     * @param clusterId   cluster id to acquire read lock
     */
    public static void acquireReadLockForCluster(String serviceName, String clusterId) {

        // acquire read lock for the relevant Services
        acquireReadLockForService(serviceName);

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId, true);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            // acquire read lock for the relevant Cluster
            topologyClusterLock.acquireReadLock();
            if (log.isDebugEnabled()) {
                log.debug("Read lock acquired for Cluster " + clusterId);
            }
        }
    }

    /**
     * Releases read lock for a Cluster. This will release the read lock in the following order
     * 1. for the Cluster
     * 2. for the Service
     *
     * @param serviceName service name to release read lock
     * @param clusterId   cluster id to release read lock
     */
    public static void releaseReadLockForCluster(String serviceName, String clusterId) {

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId, false);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            // release read lock for the relevant Cluster
            topologyClusterLock.releaseReadLock();
            if (log.isDebugEnabled()) {
                log.debug("Read lock released for Cluster " + clusterId);
            }
        }

        // release read lock for relevant Service
        releaseReadLockForService(serviceName);
    }

    private static void handleLockNotFound(String errorMsg) {
        log.error(errorMsg);
        //throw new RuntimeException(errorMsg);
    }

    public static Topology getTopology() {
        if (topology == null) {
            synchronized (TopologyManager.class) {
                if (topology == null) {
                    topology = new Topology();
                    if (log.isDebugEnabled()) {
                        log.debug("Topology object created");
                    }
                }
            }
        }
        return topology;
    }
}
