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

package org.apache.stratos.load.balancer.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer context utility class.
 */
public class LoadBalancerContextUtil {
    private static final Log log = LogFactory.getLog(LoadBalancerContextUtil.class);

    public static void addClusterToLbContext(Cluster cluster) {
        if (cluster == null)
            return;

        // Add cluster to ClusterIdClusterMap
        LoadBalancerContext.getInstance().getClusterIdClusterMap().addCluster(cluster);

        Service service = TopologyManager.getTopology().getService(cluster.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service not found: [cluster] %s", cluster.getClusterId()));
        }

        // Add cluster to HostNameClusterMap
        for (String hostName : cluster.getHostNames()) {
            if (!LoadBalancerContext.getInstance().getHostNameClusterMap().containsCluster((hostName))) {
                LoadBalancerContext.getInstance().getHostNameClusterMap().addCluster(hostName, cluster);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Cluster added to single tenant cluster map: [cluster] %s [hostname] %s", cluster.getClusterId(), hostName));
                }
            }
        }
    }

    public static void removeClusterFromLbContext(String clusterId) {
        Cluster cluster = LoadBalancerContext.getInstance().getClusterIdClusterMap().getCluster(clusterId);
        if (cluster == null) {
            return;
        }

        Service service = TopologyManager.getTopology().getService(cluster.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service not found: [cluster] %s", cluster.getClusterId()));
        }

        // Remove cluster from HostNameClusterMap
        for (String hostName : cluster.getHostNames()) {
            if (LoadBalancerContext.getInstance().getHostNameClusterMap().containsCluster(hostName)) {
                LoadBalancerContext.getInstance().getHostNameClusterMap().removeCluster(hostName);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Cluster removed from single tenant cluster map: [cluster] %s [hostname] %s", cluster.getClusterId(), hostName));
                }
            }
        }

        // Remove cluster from ClusterIdClusterMap
        LoadBalancerContext.getInstance().getClusterIdClusterMap().removeCluster(clusterId);
    }
}
