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

package org.apache.stratos.messaging.message.processor.application.updater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.application.locking.ApplicationLock;
import org.apache.stratos.messaging.domain.application.locking.ApplicationLockHierarchy;

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

public class ApplicationsUpdater {

    private static final Log log = LogFactory.getLog(ApplicationsUpdater.class);

    private static volatile ApplicationLockHierarchy applicationLockHierarchy =
            ApplicationLockHierarchy.getInstance();

    // Top level locks - should be used to lock the entire Topology

    /**
     * Acquires write lock for all Applications
     */
    public static void acquireWriteLockForApplications() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock acquired for Applications");
        }
        applicationLockHierarchy.getApplicationLock().acquireWriteLock();
    }

    /**
     * Releases write lock for all Applications
     */
    public static void releaseWriteLockForApplications() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock released for Applications");
        }
        applicationLockHierarchy.getApplicationLock().releaseWriteLock();
    }

    /**
     * Acquires write lock for the Application
     *
     * @param appId Application id
     */
    public static void acquireWriteLockForApplication(String appId) {
        ApplicationLock applicationLock = applicationLockHierarchy.getLockForApplication(appId);
        applicationLock.acquireWriteLock();
        if (log.isDebugEnabled()) {
            log.debug("Write lock acquired for Application " + appId);
        }
    }

    /**
     * Releases write lock for the Application
     *
     * @param appId Application id
     */
    public static void releaseWriteLockForApplication(String appId) {
        ApplicationLock applicationLock = applicationLockHierarchy.getLockForApplication(appId);
        applicationLock.releaseWriteLock();
        if (log.isDebugEnabled()) {
            log.debug("Write lock released for application: [application-id] " + appId);
        }
    }
}
