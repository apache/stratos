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
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;

/**
 * Topology filter configurator to configure topology filters.
 */
public class TopologyFilterConfigurator {
    private static final Log log = LogFactory.getLog(TopologyFilterConfigurator.class);

    public static void configure(LoadBalancerConfiguration configuration) {
        String serviceFilter = configuration.getTopologyServiceFilter();
        if (StringUtils.isNotBlank(serviceFilter)) {
            System.setProperty(StratosConstants.TOPOLOGY_SERVICE_FILTER, serviceFilter);
        }

        String clusterFilter = configuration.getTopologyClusterFilter();
        if (StringUtils.isNotBlank(clusterFilter)) {
            System.setProperty(StratosConstants.TOPOLOGY_CLUSTER_FILTER, clusterFilter);
        }

        String memberFilter = configuration.getTopologyMemberFilter();
        if (StringUtils.isNotBlank(memberFilter)) {
            System.setProperty(StratosConstants.TOPOLOGY_MEMBER_FILTER, memberFilter);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting service-fTopologyFilterConfigurator.javailter = %s", System.getProperty(StratosConstants.TOPOLOGY_SERVICE_FILTER)));
            log.debug(String.format("Setting cluster-filter = %s", System.getProperty(StratosConstants.TOPOLOGY_CLUSTER_FILTER)));
            log.debug(String.format("Setting member-filter = %s", System.getProperty(StratosConstants.TOPOLOGY_MEMBER_FILTER)));
        }
    }
}
