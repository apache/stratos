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

package org.apache.stratos.load.balancer.context;

import org.apache.stratos.common.services.DistributedObjectProvider;
import org.apache.stratos.load.balancer.internal.ServiceReferenceHolder;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Algorithm context is used for identifying the cluster and its current member for executing load balancing algorithms.
 * Key: service name, cluster id
 */
public class AlgorithmContext {

    private static final String LOAD_BALANCER_ALGORITHM_CONTEXT_MAP = "load.balancer.algorithm.context.map";
    private static final String CURRENT_MEMBER_INDEX_MAP_LOCK = "current.member.index.map.lock";

    private String serviceName;
    private String clusterId;
    private final Map<String, Integer> clusterMemberIndexMap;
    private final DistributedObjectProvider distributedObjectProvider;

    public AlgorithmContext(String serviceName, String clusterId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        // Initialize distributed object provider
        distributedObjectProvider = ServiceReferenceHolder.getInstance().getDistributedObjectProvider();
        // Initialize cluster->memberIndex map
        clusterMemberIndexMap = distributedObjectProvider.getMap(LOAD_BALANCER_ALGORITHM_CONTEXT_MAP);

        Lock lock = null;
        try {
            lock = acquireCurrentMemberIndexLock();
            putCurrentMemberIndex(serviceName, clusterId, 0);
        } finally {
            if(lock != null) {
                releaseCurrentMemberIndexLock(lock);
            }
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public int getCurrentMemberIndex() {
        return getCurrentMemberIndex(getServiceName(), getClusterId());
    }

    public void setCurrentMemberIndex(int currentMemberIndex) {
        Lock lock = null;
        try {
            lock = acquireCurrentMemberIndexLock();
            putCurrentMemberIndex(getServiceName(), getClusterId(), currentMemberIndex);
        } finally {
            if(lock != null) {
                releaseCurrentMemberIndexLock(lock);
            }
        }
    }

    private String constructKey(String serviceName, String clusterId) {
        return String.format("%s-%s", serviceName, clusterId);
    }

    private Lock acquireCurrentMemberIndexLock() {
        return distributedObjectProvider.acquireLock(CURRENT_MEMBER_INDEX_MAP_LOCK);
    }

    private void releaseCurrentMemberIndexLock(Lock lock) {
        if(lock != null) {
            distributedObjectProvider.releaseLock(lock);
        }
    }

    public void putCurrentMemberIndex(String serviceName, String clusterId, int currentMemberIndex) {
        String key = constructKey(serviceName, clusterId);
        clusterMemberIndexMap.put(key, currentMemberIndex);
    }

    public int getCurrentMemberIndex(String serviceName, String clusterId) {
        String key = constructKey(serviceName, clusterId);
        return clusterMemberIndexMap.get(key);
    }
}
