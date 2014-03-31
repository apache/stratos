/*
 *
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
 *
 */
package org.apache.stratos.messaging.message.filter.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.filter.MessageFilter;
import org.apache.stratos.messaging.util.Constants;

import java.util.Collection;

/**
 * A filter to discard topology events which are not in a load balancer cluster.
 */
public class TopologyMemberFilter extends MessageFilter {
    private static final Log log = LogFactory.getLog(TopologyServiceFilter.class);
    private static volatile TopologyMemberFilter instance;

    public TopologyMemberFilter() {
        super(Constants.TOPOLOGY_MEMBER_FILTER);
    }

    public static TopologyMemberFilter getInstance() {
        if (instance == null) {
            synchronized (TopologyMemberFilter.class){
                if (instance == null) {
                    instance = new TopologyMemberFilter();
                    if(log.isDebugEnabled()) {
                        log.debug("Topology member filter instance created");
                    }
                }
            }
        }
        return instance;
    }

    public boolean lbClusterIdIncluded(String value) {
        return included(Constants.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID, value);
    }

    public boolean lbClusterIdExcluded(String value) {
        return excluded(Constants.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID, value);
    }

    public Collection<String> getIncludedLbClusterIds() {
        return getIncludedPropertyValues(Constants.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID);
    }
}
