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

package org.apache.stratos.messaging.message.filter.topology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A filter to discard topology events which are not in a given cluster id list.
 */
public class ClusterFilter {
    private static final Log log = LogFactory.getLog(ClusterFilter.class);
    private static volatile ClusterFilter instance;

    private Map<String, Boolean> clusterIdMap;

    private ClusterFilter() {
        this.clusterIdMap = new HashMap<String, Boolean>();

        String filter = System.getProperty("stratos.messaging.topology.cluster.filter");
        if(StringUtils.isNotBlank(filter)) {
            String[] array = filter.split(",");
            for(String item : array) {
                clusterIdMap.put(item, true);
            }
            if(log.isDebugEnabled()) {
                log.debug(String.format("Cluster filter initialized: [included] %s", filter));
            }
        }
    }

    public static synchronized ClusterFilter getInstance() {
        if (instance == null) {
            synchronized (ClusterFilter.class){
                if (instance == null) {
                    instance = new ClusterFilter();
                    if(log.isDebugEnabled()) {
                        log.debug("Cluster filter object created");
                    }
                }
            }
        }
        return instance;
    }

    public boolean isActive() {
        return clusterIdMap.size() > 0;
    }

    public boolean included(String clusterId) {
        return clusterIdMap.containsKey(clusterId);
    }

    public boolean excluded(String clusterId) {
        return !clusterIdMap.containsKey(clusterId);
    }

    public Collection<String> getIncludedClusterIds() {
        return clusterIdMap.keySet();
    }
}
