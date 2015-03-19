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

package org.apache.stratos.messaging.message.receiver.domain.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.messaging.domain.application.signup.DomainMapping;

import java.util.*;

/**
 * Domain mapping manager.
 */
public class DomainMappingManager {

    private static final Log log = LogFactory.getLog(DomainMappingManager.class);

    private static volatile DomainMappingManager instance;
    private static volatile ReadWriteLock lock = new ReadWriteLock("domain-mapping-manager");

    private Map<String, List<DomainMapping>> clusterIdToDomainMappingsMap;
    private boolean initialized;

    public static void acquireReadLock() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock acquired");
        }
        lock.acquireReadLock();
    }

    public static void releaseReadLock() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock released");
        }
        lock.releaseReadLock();
    }

    public static void acquireWriteLock() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock acquired");
        }
        lock.acquireWriteLock();
    }

    public static void releaseWriteLock() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock released");
        }
        lock.releaseWriteLock();
    }

    public static DomainMappingManager getInstance() {
        if (instance == null) {
            synchronized (DomainMappingManager.class) {
                if (instance == null) {
                    instance = new DomainMappingManager();
                    if (log.isDebugEnabled()) {
                        log.debug("Domain mapping manager instance created");
                    }
                }
            }
        }
        return instance;
    }

    private DomainMappingManager() {
        clusterIdToDomainMappingsMap = new HashMap<String, List<DomainMapping>>();
    }

    /**
     * Add domain mappings
     *
     * @param domainMappings
     */
    public void addDomainMappings(List<DomainMapping> domainMappings) {
        for (DomainMapping domainMapping : domainMappings) {
            addDomainMapping(domainMapping);
        }
    }

    /**
     * Add domain mapping.
     *
     * @param domainMapping
     */
    public void addDomainMapping(DomainMapping domainMapping) {
        List<DomainMapping> domainMappings = getDomainMappings(domainMapping.getClusterId());
        if (domainMappings == null) {
            domainMappings = new ArrayList<DomainMapping>();
            clusterIdToDomainMappingsMap.put(domainMapping.getClusterId(), domainMappings);
        }
        domainMappings.add(domainMapping);
    }

    /**
     * Get domain mappings of cluster.
     *
     * @param clusterId
     * @return
     */
    public List<DomainMapping> getDomainMappings(String clusterId) {
        if (clusterIdToDomainMappingsMap.containsKey(clusterId)) {
            return clusterIdToDomainMappingsMap.get(clusterId);
        }
        return null;
    }

    /**
     * Get domain mapping of cluster by domain name.
     *
     * @param clusterId
     * @param domainName
     * @return
     */
    public DomainMapping getDomainMapping(String clusterId, String domainName) {
        List<DomainMapping> domainMappings = getDomainMappings(clusterId);
        if (domainMappings == null) {
            log.warn(String.format("Domain mappings not found for cluster: [cluster-id] %s", clusterId));
            return null;
        }

        Iterator<DomainMapping> iterator = domainMappings.iterator();
        while (iterator.hasNext()) {
            DomainMapping domainMapping = iterator.next();
            if (domainMapping.getDomainName().equals(domainName)) {
                return domainMapping;
            }
        }
        return null;
    }

    /**
     * Remove domain mapping of cluster by domain name.
     *
     * @param clusterId
     * @param domainName
     */
    public void removeDomainMapping(String clusterId, String domainName) {
        List<DomainMapping> domainMappings = getDomainMappings(clusterId);
        if (domainMappings == null) {
            throw new RuntimeException(String.format("Domain mappings not found for cluster: [cluster-id] %s", clusterId));
        }

        Iterator<DomainMapping> iterator = domainMappings.iterator();
        while (iterator.hasNext()) {
            DomainMapping domainMapping = iterator.next();
            if (domainMapping.getDomainName().equals(domainName)) {
                iterator.remove();
                if (log.isInfoEnabled()) {
                    log.info(String.format("Domain mapping removed: [cluster-id] %s [domain-name] %s",
                            clusterId, domainName));
                }
                return;
            }
        }
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
