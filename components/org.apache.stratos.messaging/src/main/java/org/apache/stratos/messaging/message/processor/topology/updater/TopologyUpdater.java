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

package org.apache.stratos.messaging.message.processor.topology.updater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.locking.TopologyLock;
import org.apache.stratos.messaging.domain.topology.locking.TopologyLockHierarchy;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Used to lock the Topology for writes by messaging component
 * <p/>
 * Acquire a write lock:
 * <p/>
 * From root level, acquire read lock, and acquire a write lock only for the
 * relevant sub tree.
 * <p/>
 * Example 1: Acquiring write lock for a Cluster to modify the Cluster object -
 * acquiring:
 * public static void acquireWriteLockForCluster (String serviceName, String clusterId)
 * <p/>
 * releasing:
 * public static void releaseWriteLockForCluster (String serviceName, String clusterId)
 * <p/>
 * Example 2: Acquiring write lock to add a new Cluster object -
 * acquiring:
 * public static void acquireWriteLockForService (String serviceName)
 * <p/>
 * releasing:
 * public static void releaseWriteLockForService (String serviceName)
 * <p/>
 * Example 3: Acquiring the write lock to add a deploy a Cartridge (add a new Service)
 * acquire:
 * public static void acquireWriteLockForServices()
 * <p/>
 * release:
 * public static void releaseWriteLockForServices()
 */

public class TopologyUpdater {

    private static final Log log = LogFactory.getLog(TopologyUpdater.class);

    private static volatile TopologyLockHierarchy topologyLockHierarchy = TopologyLockHierarchy.getInstance();

    // Top level locks - should be used to lock the entire Topology

    /**
     * Acquires write lock for the Complete Topology
     */
    public static void acquireWriteLock() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock acquired for Topology");
        }
        topologyLockHierarchy.getCompleteTopologyLock().acquireWriteLock();
    }

    /**
     * Releases write lock for the Complete Topology
     */
    public static void releaseWriteLock() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock released for Topology");
        }
        topologyLockHierarchy.getCompleteTopologyLock().releaseWriteLock();
    }

    // Service write locks

    /**
     * Acquires write lock for the all Services
     */
    public static void acquireWriteLockForServices() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock acquired for Services");
        }
        topologyLockHierarchy.getServiceLock().acquireWriteLock();
    }

    /**
     * Releases write lock for the all Services
     */
    public static void releaseWriteLockForServices() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock released for Services");
        }
        topologyLockHierarchy.getServiceLock().releaseWriteLock();
    }

    /**
     * Acquires write lock for a Service
     *
     * @param serviceName service name to acquire write lock
     */
    public static void acquireWriteLockForService(String serviceName) {

        // acquire read lock for all Applications
        TopologyManager.acquireReadLockForServices();

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName, true);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.acquireWriteLock();
            if (log.isDebugEnabled()) {
                log.debug("Write lock acquired for Service " + serviceName);
            }
        }
    }

    /**
     * Releases write lock for a Service
     *
     * @param serviceName service name to release write lock
     */
    public static void releaseWriteLockForService(String serviceName) {

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName, false);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.releaseWriteLock();
            if (log.isDebugEnabled()) {
                log.debug("Write lock released for Service " + serviceName);
            }
        }

        // release read lock for all Services
        TopologyManager.releaseReadLockForServices();
    }

    /**
     * Acquires write lock for a Cluster. This will acquire the write lock in the following order
     * 1. for the Service
     * 2. for the Cluster
     *
     * @param serviceName service name to acquire write lock
     * @param clusterId   cluster id to acquire write lock
     */
    public static void acquireWriteLockForCluster(String serviceName, String clusterId) {

        // acquire read lock for the relevant Services
        TopologyManager.acquireReadLockForService(serviceName);

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId, true);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            topologyClusterLock.acquireWriteLock();
            if (log.isDebugEnabled()) {
                log.debug("Write lock acquired for Cluster " + clusterId);
            }
        }
    }

    /**
     * Releases write lock for a Cluster. This will release the write lock in the following order
     * 1. for the Cluster
     * 2. for the Service
     *
     * @param serviceName service name to release write lock
     * @param clusterId   cluster id to release write lock
     */
    public static void releaseWriteLockForCluster(String serviceName, String clusterId) {

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId, false);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            topologyClusterLock.releaseWriteLock();
            if (log.isDebugEnabled()) {
                log.debug("Write lock released for Cluster " + clusterId);
            }
        }

        // release read lock for relevant Service
        TopologyManager.releaseReadLockForService(serviceName);
    }

    private static void handleLockNotFound(String errorMsg) {
        log.warn(errorMsg);
        //throw new RuntimeException(errorMsg);
    }

}
