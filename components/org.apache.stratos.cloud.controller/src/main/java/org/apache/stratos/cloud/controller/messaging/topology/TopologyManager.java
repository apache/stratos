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
package org.apache.stratos.cloud.controller.messaging.topology;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.messaging.domain.topology.Topology;


/**
 * Persistence and retrieval of Topology from Registry
 */
public class TopologyManager {
    private static final Log log = LogFactory.getLog(TopologyManager.class);

    private static volatile ReadWriteLock lock = new ReadWriteLock("topology-manager");
    private static volatile Topology topology;

    private TopologyManager() {
    }

    public static void acquireReadLock() {
        lock.acquireReadLock();
        if(log.isDebugEnabled()) {
            log.debug("Read lock acquired");
        }
    }

    public static void releaseReadLock() {
        lock.releaseReadLock();
        if(log.isDebugEnabled()) {
            log.debug("Read lock released");
        }
    }

    public static void acquireWriteLock() {
        lock.acquireWriteLock();
        if(log.isDebugEnabled()) {
            log.debug("Write lock acquired");
        }
    }

    public static void releaseWriteLock() {
        lock.releaseWriteLock();
        if(log.isDebugEnabled()) {
            log.debug("Write lock released");
        }
    }

    public static Topology getTopology() {
        if (topology == null) {
            synchronized (TopologyManager.class) {
                if (topology == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Trying to retrieve topology from registry");
                    }
                    topology = CloudControllerUtil.retrieveTopology();
                    if (topology == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Topology not found in registry, creating new");
                        }
                        topology = new Topology();
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Topology initialized");
                    }
                }
            }
        }
        return topology;
    }

    /**
     * Update in-memory topology and persist it in registry.
     * @param topology_
     */
    public static void updateTopology(Topology topology_) {
        synchronized (TopologyManager.class) {
            if (log.isDebugEnabled()) {
                log.debug("Updating topology");
            }
            topology = topology_;
            CloudControllerUtil.persistTopology(topology);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Topology updated: %s", toJson(topology)));
            }
        }

    }

    private static String toJson(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }
}

