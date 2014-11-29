/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.context.map;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.load.balancer.internal.ServiceReferenceHolder;
import org.wso2.carbon.caching.impl.DistributedMapProvider;
import org.wso2.carbon.caching.impl.MapEntryListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Algorithm context map is a singleton class for managing load balancing algorithm context
 * of each service cluster.
 */
public class AlgorithmContextMap {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(AlgorithmContextMap.class);
    private static final String LOAD_BALANCER_ALGORITHM_CONTEXT_MAP = "LOAD_BALANCER_ALGORITHM_CONTEXT_MAP";
    private static AlgorithmContextMap instance;

    private final Map<String, Integer> clusterMemberIndexMap;

    private AlgorithmContextMap() {
        if (LoadBalancerContext.getInstance().isClustered()) {
            DistributedMapProvider distributedMapProvider = ServiceReferenceHolder.getInstance().getDistributedMapProvider();
            clusterMemberIndexMap = distributedMapProvider.getMap(LOAD_BALANCER_ALGORITHM_CONTEXT_MAP,
                    new MapEntryListener() {
                        @Override
                        public <X> void entryAdded(X x) {
                            if(log.isDebugEnabled()) {
                                log.debug("Entry added to distributed algorithm context map: " + x);
                            }
                        }

                        @Override
                        public <X> void entryRemoved(X x) {
                            if(log.isDebugEnabled()) {
                                log.debug("Entry removed from distributed algorithm context map: " + x);
                            }
                        }

                        @Override
                        public <X> void entryUpdated(X x) {
                            if(log.isDebugEnabled()) {
                                log.debug("Entry updated in distributed algorithm context map: " + x);
                            }
                        }
                    });
            if (clusterMemberIndexMap != null) {
                if (log.isInfoEnabled()) {
                    log.info("Load balancer context map initialized in distributed mode");
                }
            } else {
                log.error("Could not initialize algorithm context map from distributed map provider");
            }
        } else {
            clusterMemberIndexMap = new ConcurrentHashMap<String, Integer>();
            if (log.isInfoEnabled()) {
                log.info("Load balancer context map initialized locally");
            }
        }
    }

    public static AlgorithmContextMap getInstance() {
        if (instance == null) {
            synchronized (AlgorithmContextMap.class) {
                if (instance == null) {
                    instance = new AlgorithmContextMap();
                }
            }
        }
        return instance;
    }

    private String constructKey(String serviceName, String clusterId) {
        return String.format("%s-%s", serviceName, clusterId);
    }

    private com.hazelcast.core.ILock acquireDistributedLock(Object object) {
        if (log.isDebugEnabled()) {
            log.debug("Acquiring distributed lock for algorithm context map...");
        }
        HazelcastInstance hazelcastInstance = ServiceReferenceHolder.getInstance().getHazelcastInstance();
        ILock lock = hazelcastInstance.getLock(object);
        if (log.isDebugEnabled()) {
            log.debug("Distributed lock acquired for algorithm context map");
        }
        return lock;
    }

    private void releaseDistributedLock(ILock lock) {
        if (log.isDebugEnabled()) {
            log.debug("Releasing distributed lock for algorithm context map...");
        }
        lock.forceUnlock();
        if (log.isDebugEnabled()) {
            log.debug("Distributed lock released for algorithm context map");
        }
    }

    public void putCurrentMemberIndex(String serviceName, String clusterId, int currentMemberIndex) {
        String key = constructKey(serviceName, clusterId);
        if (LoadBalancerContext.getInstance().isClustered()) {
            ILock lock = null;
            try {
                lock = acquireDistributedLock(clusterMemberIndexMap);
                clusterMemberIndexMap.put(key, currentMemberIndex);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            clusterMemberIndexMap.put(key, currentMemberIndex);
        }
    }

    public void removeCluster(String serviceName, String clusterId) {
        String key = constructKey(serviceName, clusterId);
        if (LoadBalancerContext.getInstance().isClustered()) {
            ILock lock = null;
            try {
                lock = acquireDistributedLock(clusterMemberIndexMap);
                clusterMemberIndexMap.remove(key);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            clusterMemberIndexMap.remove(key);
        }
    }

    public int getCurrentMemberIndex(String serviceName, String clusterId) {
        String key = constructKey(serviceName, clusterId);
        return clusterMemberIndexMap.get(key);
    }
}
