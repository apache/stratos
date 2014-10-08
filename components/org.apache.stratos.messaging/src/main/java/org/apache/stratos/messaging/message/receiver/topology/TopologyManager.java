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

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  A singleton class for managing the topology data structure.
 *
 *  Usage:
 *  Acquire a relevant lock and invoke the getTopology() method inside a try block.
 *  Once processing is done release the lock using a finally block.
 *
 *  Acquiring Locks:
 *
 *  Stratos supports hierarchical locking. As per the practice, we need to lock the
 *  hierarchy from root level till the relevant sub tree.
 *
 *  Acquire a write lock:
 *
 *  From root level, acquire read lock, and acquire a write lock only for the
 *  relevant sub tree.
 *
 *  Acquire a read lock:
 *
 *  From root level, acquire read locks till the relevant sub tree
 *
 *  Examples -
 *
 *  Example 1: Acquiring write lock for a Cluster to modify the Cluster object -
 *           acquiring:
 *           1. acquire read lock for all Services
 *           2. acquire read lock for the particular Service, to which the cluster belongs to
 *           3. acquire write lock for the Cluster
 *
 *           releasing:
 *           1. release write lock for the Cluster
 *           2. release read lock for the particular Service
 *           3. release read lock for all Services
 *
 *  Example 2: Acquiring write lock to add a new Cluster object -
 *           acquiring:
 *           1. acquire read lock for all Services
 *           2. acquire write lock for the particular Service, to which the cluster belongs to
 *
 *           releasing:
 *           1. release write lock for the particular Service
 *           2. release read lock for all Services
 *
 *  Example 3: Acquiring read lock to read Cluster information
 *           acquiring:
 *           1. acquire read lock for all Services
 *           2. acquire read lock for the particular Service, to which the cluster belongs to
 *           3. acquire read lock for the relevant Cluster
 *
 *           releasing:
 *           1. release read lock for the relevant Cluster
 *           2. release read lock for the particular Service
 *           3. release read lock for all Services
 *
 *  Example 4: Acquiring the write lock to add a deploy a Cartridge (add a new Service)
 *           acquire:
 *           1. acquire write lock for all Services
 *
 *           release:
 *           1. release write lock for all Services
 */
public class TopologyManager {
    private static final Log log = LogFactory.getLog(TopologyManager.class);

    private static volatile Topology topology;
    private static final TopologyLockHierarchy topologyLockHierarchy =
            TopologyLockHierarchy.getInstance();
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();


    // Top level locks - should be used to lock the entire Topology

    /**
     * Acquires read lock for the Complete Topology
     */
    public static void acquireReadLock() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock acquired for Topology");
        }
        readLock.lock();
    }

    /**
     * Releases read lock for the Complete Topology
     */
    public static void releaseReadLock() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock released for Topology");
        }
        readLock.unlock();
    }

    /**
     * Acquires write lock for the Complete Topology
     */
    public static void acquireWriteLock() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock acquired for Topology");
        }
        writeLock.lock();
    }

    /**
     * Releases write lock for the Complete Topology
     */
    public static void releaseWriteLock() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock released for Topology");
        }
        writeLock.unlock();
    }

    // Application and Service read locks
    /**
     * Acquires read lock for the all Applications
     */
    public static void acquireReadLockForApplications() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock acquired for Applications");
        }
        topologyLockHierarchy.getApplicatioLock().acquireReadLock();
    }

    /**
     * Releases read lock for the all Applications
     */
    public static void releaseReadLockForApplications() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock released for Applications");
        }
        topologyLockHierarchy.getApplicatioLock().releaseReadLock();
    }

    /**
     * Acquires read lock for the all Services
     */
    public static void acquireReadLockForServices() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock acquired for Services");
        }
        topologyLockHierarchy.getServiceLock().acquireReadLock();
    }

    /**
     * Releases read lock for the all Services
     */
    public static void releaseReadLockForServices() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock released for Services");
        }
        topologyLockHierarchy.getServiceLock().releaseReadLock();
    }

    // Application and Service write locks
    /**
     * Acquires write lock for the all Applications
     */
    public static void acquireWriteLockForApplications() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock acquired for Applications");
        }
        topologyLockHierarchy.getApplicatioLock().acquireWriteLock();
    }

    /**
     * Releases write lock for the all Applications
     */
    public static void releaseWriteLockForApplications() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock released for Applications");
        }
        topologyLockHierarchy.getApplicatioLock().releaseWritelock();
    }

    /**
     * Acquires write lock for the all Services
     */
    public static void acquireWriteLockForServices() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock acquired for Services");
        }
        topologyLockHierarchy.getServiceLock().acquireWriteLock();
    }

    /**
     * Releases write lock for the all Services
     */
    public static void releaseWriteLockForServices() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock released for Services");
        }
        topologyLockHierarchy.getServiceLock().releaseWritelock();
    }

    /**
     * Acquires read lock for a Service
     *
     * @param serviceName service name to acquire read lock
     */
    public static void acquireReadLockForService (String serviceName) {

        // acquire read lock for all Services
        acquireReadLockForServices();

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.acquireReadLock();
            if(log.isDebugEnabled()) {
                log.debug("Read lock acquired for Service " + serviceName);
            }
        }
    }

    /**
     * Releases read lock for a Service
     *
     * @param serviceName service name to release read lock
     */
    public static void releaseReadLockForService (String serviceName) {

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.releaseReadLock();
            if(log.isDebugEnabled()) {
                log.debug("Read lock released for Service " + serviceName);
            }
        }

        // release read lock for all Services
        releaseReadLockForServices();
    }

    /**
     * Acquires write lock for a Service
     *
     * @param serviceName service name to acquire write lock
     */
    public static void acquireWriteLockForService (String serviceName) {

        // acquire read lock for all Applications
        acquireReadLockForServices();

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.acquireWriteLock();
            if(log.isDebugEnabled()) {
                log.debug("Write lock acquired for Service " + serviceName);
            }
        }
    }

    /**
     * Releases write lock for a Service
     *
     * @param serviceName service name to release write lock
     */
    public static void releaseWriteLockForService (String serviceName) {

        TopologyLock topologyServiceLock = topologyLockHierarchy.getTopologyLockForService(serviceName);
        if (topologyServiceLock == null) {
            handleLockNotFound("Topology lock not found for Service " + serviceName);

        } else {
            topologyServiceLock.releaseWritelock();
            if(log.isDebugEnabled()) {
                log.debug("Write lock released for Service " + serviceName);
            }
        }

        // release read lock for all Services
        releaseReadLockForServices();
    }

    /**
     * Acquires read lock for a Cluster. This will acquire the read lock in the following order
     *      1. for the Service
     *      2. for the Cluster
     *
     * @param serviceName service name to acquire read lock
     * @param clusterId cluster id to acquire read lock
     */
    public static void acquireReadLockForCluster (String serviceName, String clusterId) {

        // acquire read lock for the relevant Services
        acquireReadLockForService(serviceName);

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        }  else {
            // acquire read lock for the relevant Cluster
            topologyClusterLock.acquireReadLock();
            if(log.isDebugEnabled()) {
                log.debug("Read lock acquired for Cluster " + clusterId);
            }
        }
    }

    /**
     * Releases read lock for a Cluster. This will release the read lock in the following order
     *      1. for the Cluster
     *      2. for the Service
     *
     * @param serviceName service name to release read lock
     * @param clusterId cluster id to release read lock
     */
    public static void releaseReadLockForCluster (String serviceName, String clusterId) {

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            // release read lock for the relevant Cluster
            topologyClusterLock.releaseReadLock();
            if(log.isDebugEnabled()) {
                log.debug("Read lock released for Cluster " + clusterId);
            }
        }

        // release read lock for relevant Service
        releaseReadLockForService(serviceName);
    }

    /**
     * Acquires write lock for a Cluster. This will acquire the write lock in the following order
     *      1. for the Service
     *      2. for the Cluster
     *
     * @param serviceName service name to acquire write lock
     * @param clusterId cluster id to acquire write lock
     */
    public static void acquireWriteLockForCluster (String serviceName, String clusterId) {

        // acquire read lock for the relevant Services
        acquireReadLockForService(serviceName);

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            topologyClusterLock.acquireWriteLock();
            if(log.isDebugEnabled()) {
                log.debug("Write lock acquired for Cluster " + clusterId);
            }
        }
    }

    /**
     * Releases write lock for a Cluster. This will release the write lock in the following order
     *      1. for the Cluster
     *      2. for the Service
     *
     * @param serviceName service name to release write lock
     * @param clusterId cluster id to release write lock
     */
    public static void releaseWriteLockForCluster (String serviceName, String clusterId) {

        TopologyLock topologyClusterLock = topologyLockHierarchy.getTopologyLockForCluster(clusterId);
        if (topologyClusterLock == null) {
            handleLockNotFound("Topology lock not found for Cluster " + clusterId);

        } else {
            topologyClusterLock.releaseWritelock();
            if(log.isDebugEnabled()) {
                log.debug("Write lock released for Cluster " + clusterId);
            }
        }

        // release read lock for relevant Service
        releaseReadLockForService(serviceName);
    }

    /**
     * Acquires read lock for the Application
     *
     * @param appId Application id
     */
    public static void acquireReadLockForApplication (String appId) {

        // acquire read lock for all Applications
        acquireReadLockForApplications();

        TopologyLock topologyAppLock = topologyLockHierarchy.getTopologyLockForApplication(appId);
        if (topologyAppLock == null)  {
            handleLockNotFound("Topology lock not found for Application " + appId);

        } else {
            // now, lock Application
            topologyAppLock.acquireReadLock();
            if(log.isDebugEnabled()) {
                log.debug("Read lock acquired for Application " + appId);
            }
        }
    }

    /**
     * Releases read lock for the Application
     *
     * @param appId Application id
     */
    public static void releaseReadLockForApplication (String appId) {

        TopologyLock topologyAppLock = topologyLockHierarchy.getTopologyLockForApplication(appId);
        if (topologyAppLock == null)  {
            handleLockNotFound("Topology lock not found for Application " + appId);

        } else {
            // release App lock
            topologyAppLock.releaseReadLock();
            if(log.isDebugEnabled()) {
                log.debug("Read lock released for Application " + appId);
            }
        }

        // release read lock for all Applications
        releaseReadLockForApplications();
    }

    /**
     * Acquires write lock for the Application
     *
     * @param appId Application id
     */
    public static synchronized void acquireWriteLockForApplication (String appId) {

        // acquire read lock for all Applications
        acquireReadLockForApplications();

        TopologyLock topologyAppLock = topologyLockHierarchy.getTopologyLockForApplication(appId);
        if (topologyAppLock == null)  {
            handleLockNotFound("Topology lock not found for Application " + appId);

        } else {
            // now, lock Application
            topologyAppLock.acquireWriteLock();
            if(log.isDebugEnabled()) {
                log.debug("Write lock acquired for Application " + appId);
            }
        }
    }

    /**
     * Releases write lock for the Application
     *
     * @param appId Application id
     */
    public static synchronized void releaseWriteLockForApplication (String appId) {

        TopologyLock topologyAppLock = topologyLockHierarchy.getTopologyLockForApplication(appId);
        if (topologyAppLock == null)  {
            handleLockNotFound("Topology lock not found for Application " + appId);

        } else {
            // release App lock
            topologyAppLock.releaseWritelock();
            if(log.isDebugEnabled()) {
                log.debug("Write lock released for Application " + appId);
            }
        }

        // release read lock for all Applications
        releaseReadLockForApplications();
    }

    private static void handleLockNotFound (String errorMsg) {
        log.warn(errorMsg);
        //throw new RuntimeException(errorMsg);
    }

    public static Topology getTopology() {
        if (topology == null) {
            synchronized (TopologyManager.class){
                if (topology == null) {
                    topology = new Topology();
                    if(log.isDebugEnabled()) {
                        log.debug("Topology object created");
                    }
                }
            }
        }
        return topology;
    }
}
