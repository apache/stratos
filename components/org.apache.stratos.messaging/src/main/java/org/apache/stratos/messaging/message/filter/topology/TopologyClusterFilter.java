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

    public boolean clusterIdIncluded(String value) {
        return included(TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID, value);
    }

    public boolean clusterIdExcluded(String value) {
        return excluded(TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID, value);
    }

    public Collection<String> getIncludedClusterIds() {
        return getIncludedPropertyValues(TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID);
    }
}
