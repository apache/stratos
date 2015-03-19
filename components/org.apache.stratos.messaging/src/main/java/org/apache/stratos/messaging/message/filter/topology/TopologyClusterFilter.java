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
import org.apache.stratos.messaging.message.filter.MessageFilter;

import java.util.Collection;

/**
 * A filter to discard topology events which are not in a given cluster id list.
 */
public class TopologyClusterFilter extends MessageFilter {

    private static final Log log = LogFactory.getLog(TopologyServiceFilter.class);

    public static final String TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID = "cluster-id";
    public static final String TOPOLOGY_CLUSTER_FILTER = "stratos.topology.cluster.filter";

    private static volatile TopologyClusterFilter instance;

    public TopologyClusterFilter() {
        super(TOPOLOGY_CLUSTER_FILTER);
    }

    /**
     * Returns true if cluster is excluded else returns false.
     *
     * @param clusterId
     * @return
     */
    public static boolean apply(String clusterId) {
        boolean excluded = false;
        if (getInstance().isActive()) {
            if (StringUtils.isNotBlank(clusterId) && getInstance().clusterExcluded(clusterId)) {
                excluded = true;
            }
            if (excluded && log.isDebugEnabled()) {
                log.debug(String.format("Cluster is excluded: [cluster] %s", clusterId));
            }
        }
        return excluded;
    }

    public static TopologyClusterFilter getInstance() {
        if (instance == null) {
            synchronized (TopologyClusterFilter.class) {
                if (instance == null) {
                    instance = new TopologyClusterFilter();
                    if (log.isDebugEnabled()) {
                        log.debug("Topology cluster filter instance created");
                    }
                }
            }
        }
        return instance;
    }

    private boolean clusterExcluded(String value) {
        return excluded(TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID, value);
    }

    private Collection<String> getIncludedClusterIds() {
        return getIncludedPropertyValues(TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID + "=");
        for (String clusterId : TopologyClusterFilter.getInstance().getIncludedClusterIds()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(clusterId);
        }
        return sb.toString();
    }
}
