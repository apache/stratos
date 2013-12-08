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
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer context utility class.
 */
public class LoadBalancerContextUtil {
    private static final Log log = LogFactory.getLog(LoadBalancerContextUtil.class);

    public static void addClusterToLbContext(Cluster cluster) {
        // Add cluster to Map<ClusterId, Cluster>
        LoadBalancerContext.getInstance().addCluster(cluster);

        Service service = TopologyManager.getTopology().getService(cluster.getServiceName());
        if (service.getServiceType() == ServiceType.SingleTenant) {
            // Add cluster to SingleTenantClusterMap
            for (String hostName : cluster.getHostNames()) {
                if (!LoadBalancerContext.getInstance().singleTenantClusterExists((hostName))) {
                    LoadBalancerContext.getInstance().addSingleTenantCluster(hostName, cluster);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Cluster added to single tenant cluster map: [cluster] %s [hostname] %s", cluster.getClusterId(), hostName));
                    }
                }
            }
        }
        // MultiTenantClusterMap is updated by tenant receiver.
    }

    public static void removeClusterFromLbContext(String clusterId) {
        Cluster cluster = LoadBalancerContext.getInstance().getCluster(clusterId);
        Service service = TopologyManager.getTopology().getService(cluster.getServiceName());
        if (service.getServiceType() == ServiceType.SingleTenant) {
            // Remove cluster from SingleTenantClusterMap
            for (String hostName : cluster.getHostNames()) {
                if (LoadBalancerContext.getInstance().singleTenantClusterExists(hostName)) {
                    LoadBalancerContext.getInstance().removeSingleTenantCluster(hostName);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Cluster removed from single tenant cluster map: [cluster] %s [hostname] %s", cluster.getClusterId(), hostName));
                    }
                }
            }
        }
        // MultiTenantClusterMap is updated by tenant receiver.

        // Remove cluster from Map<ClusterId,Cluster>
        LoadBalancerContext.getInstance().removeCluster(clusterId);
    }
}
