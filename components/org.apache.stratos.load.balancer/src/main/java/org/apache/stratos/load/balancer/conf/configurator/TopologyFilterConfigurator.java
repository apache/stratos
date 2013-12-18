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

package org.apache.stratos.load.balancer.conf.configurator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;
import org.apache.stratos.messaging.util.Constants;

/**
 * Topology filter configurator to configure topology filters.
 */
public class TopologyFilterConfigurator {
    private static final Log log = LogFactory.getLog(TopologyFilterConfigurator.class);

    public static void configure(LoadBalancerConfiguration configuration) {
        String serviceFilter = configuration.getTopologyServiceFilter();
        if (StringUtils.isNotBlank(serviceFilter)) {
            System.setProperty(Constants.TOPOLOGY_SERVICE_FILTER, serviceFilter);
        }

        String clusterFilter = configuration.getTopologyClusterFilter();
        if (StringUtils.isNotBlank(clusterFilter)) {
            System.setProperty(Constants.TOPOLOGY_CLUSTER_FILTER, clusterFilter);
        }

        String memberFilter = configuration.getTopologyMemberFilter();
        if (StringUtils.isNotBlank(memberFilter)) {
            System.setProperty(Constants.TOPOLOGY_MEMBER_FILTER, memberFilter);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Topology filter settings: [service-filter] %s", System.getProperty(Constants.TOPOLOGY_SERVICE_FILTER)));
            log.debug(String.format("Topology filter settings: [cluster-filter] %s", System.getProperty(Constants.TOPOLOGY_CLUSTER_FILTER)));
            log.debug(String.format("Topology filter settings: [member-filter] %s", System.getProperty(Constants.TOPOLOGY_MEMBER_FILTER)));
        }
    }
}
