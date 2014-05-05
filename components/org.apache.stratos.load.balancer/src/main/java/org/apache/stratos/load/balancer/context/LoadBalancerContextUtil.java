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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.tenant.Subscription;
import org.apache.stratos.messaging.domain.tenant.SubscriptionDomain;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Load balancer context utility class.
 */
public class LoadBalancerContextUtil {
    private static final Log log = LogFactory.getLog(LoadBalancerContextUtil.class);

    /**
     * Add cluster against its host names.
     *
     * @param cluster
     */
    public static void addClusterAgainstHostNames(Cluster cluster) {
        if (cluster == null)
            return;

        Service service = TopologyManager.getTopology().getService(cluster.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("Service not found: [cluster] %s", cluster.getClusterId()));
        }

        // Add cluster to ClusterIdClusterMap
        LoadBalancerContext.getInstance().getClusterIdClusterMap().addCluster(cluster);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Cluster added to cluster-id -> cluster map: [cluster] %s ", cluster.getClusterId()));
        }

        // Add cluster to HostNameClusterMap
        for (String hostName : cluster.getHostNames()) {
            addClusterToHostNameClusterMap(hostName, cluster);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster added to host/domain name -> cluster map: [hostName] %s [cluster] %s",
                        hostName, cluster.getClusterId()));
            }
        }
    }

    /**
     * Remove cluster mapped against its host names.
     *
     * @param clusterId
     */
    public static void removeClusterAgainstHostNames(String clusterId) {
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
            removeClusterFromHostNameClusterMap(hostName, cluster);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster removed from host/domain name -> clusters map: [host-name] %s [cluster] %s",
                        hostName, cluster.getClusterId()));
            }
        }

        // Remove cluster from ClusterIdClusterMap
        LoadBalancerContext.getInstance().getClusterIdClusterMap().removeCluster(clusterId);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Cluster removed from cluster-id -> cluster map: [cluster] %s ", cluster.getClusterId()));
        }
    }

    /**
     * Add clusters against host names, tenant id for the given service, cluster ids.
     *
     * @param serviceName
     * @param tenantId
     * @param clusterIds
     */
    public static void addClustersAgainstHostNamesAndTenantIds(String serviceName, int tenantId, Set<String> clusterIds) {
        try {
            TopologyManager.acquireReadLock();

            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service == null) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Service not found in topology: [service] %s", serviceName));
                }
                return;
            }
            Cluster cluster;
            for (String clusterId : clusterIds) {
                cluster = service.getCluster(clusterId);
                if (cluster != null) {
                    // Add cluster against host names and tenant id
                    addClusterAgainstHostNamesAndTenantId(serviceName, tenantId, cluster);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Cluster not found in service: [service] %s [cluster] %s", serviceName, clusterId));
                    }
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    /**
     * Remove clusters mapped against host names and tenant id.
     *
     * @param serviceName
     * @param tenantId
     */
    public static void removeClustersAgainstHostNamesAndTenantIds(String serviceName, int tenantId, Set<String> clusterIds) {
        try {
            TopologyManager.acquireReadLock();

            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service == null) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Service not found in topology: [service] %s", serviceName));
                }
                return;
            }
            Cluster cluster;
            for (String clusterId : clusterIds) {
                cluster = service.getCluster(clusterId);
                if (cluster != null) {
                    // Remove cluster mapped against host names and tenant id
                    removeClusterAgainstHostNamesAndTenantId(serviceName, tenantId, cluster);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Cluster not found in service: [service] %s [cluster] %s", serviceName, clusterId));
                    }
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    /**
     * Add clusters against domain name for the given service, cluster ids.
     *
     * @param serviceName
     * @param clusterIds
     * @param domainName
     */
    public static void addClustersAgainstDomain(String serviceName, Set<String> clusterIds, String domainName) {
        try {
            TopologyManager.acquireReadLock();
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service == null) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Service not found in topology: [service] %s", serviceName));
                }
                return;
            }
            Cluster cluster;
            for (String clusterId : clusterIds) {
                cluster = service.getCluster(clusterId);
                if (cluster != null) {
                    addClusterAgainstDomain(serviceName, cluster, domainName);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Cluster not found in service: [service] %s [cluster] %s", serviceName, clusterId));
                    }
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    /**
     * Remove clusters mapped against domain name for the given service, cluster ids.
     *
     * @param serviceName
     * @param clusterIds
     * @param domainName
     */
    public static void removeClustersAgainstDomain(String serviceName, Set<String> clusterIds, String domainName) {
        try {
            TopologyManager.acquireReadLock();

            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service == null) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Service not found in topology: [service] %s", serviceName));
                }
                return;
            }
            Cluster cluster;
            for (String clusterId : clusterIds) {
                cluster = service.getCluster(clusterId);
                if (cluster != null) {
                    // Remove clusters mapped against domain names
                    removeClusterAgainstDomain(cluster, domainName);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Cluster not found in service: [service] %s [cluster] %s", serviceName, clusterId));
                    }
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    /**
     * Find cluster from service name, tenant id.
     * Acquire a topology manager read lock appropriately.
     *
     * @param serviceName
     * @param tenantId
     * @return
     */
    private static Cluster findCluster(String serviceName, int tenantId) {
        Service service = TopologyManager.getTopology().getService(serviceName);
        if (service == null) {
            throw new RuntimeException(String.format("Service not found: %s", serviceName));
        }
        for (Cluster cluster : service.getClusters()) {
            if (cluster.tenantIdInRange(tenantId)) {
                return cluster;
            }
        }
        return null;
    }

    /**
     * Add clusters against host names and tenant id to load balancer context.
     *
     * @param serviceName
     * @param tenantId
     * @param cluster
     */
    private static void addClusterAgainstHostNamesAndTenantId(String serviceName, int tenantId, Cluster cluster) {
        // Add clusters against host names
        if (log.isDebugEnabled()) {
            log.debug(String.format("Adding cluster to multi-tenant cluster map against host names: [service] %s " +
                    "[tenant-id] %d [cluster] %s", serviceName, tenantId, cluster.getClusterId()));
        }
        for (String hostName : cluster.getHostNames()) {
            addClusterToMultiTenantClusterMap(hostName, tenantId, cluster);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster added to multi-tenant cluster map: [host-name] %s [tenant-id] %d [cluster] %s",
                        hostName, tenantId, cluster.getClusterId()));
            }
        }
    }

    /**
     * Remove clusters mapped against host names and tenant id from load balancer context.
     *
     * @param serviceName
     * @param tenantId
     * @param cluster
     */
    private static void removeClusterAgainstHostNamesAndTenantId(String serviceName, int tenantId, Cluster cluster) {
        // Remove clusters mapped against host names
        if (log.isDebugEnabled()) {
            log.debug(String.format("Removing cluster from multi-tenant cluster map against host names: [service] %s " +
                    "[tenant-id] %d [cluster] %s", serviceName, tenantId, cluster.getClusterId()));
        }
        for (String hostName : cluster.getHostNames()) {
            LoadBalancerContext.getInstance().getMultiTenantClusterMap().removeCluster(hostName, tenantId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster removed from multi-tenant clusters map: [host-name] %s [tenant-id] %d [cluster] %s",
                        hostName, tenantId, cluster.getClusterId()));
            }
        }
    }


    /**
     * Add clusters against domains to load balancer context.
     *
     * @param serviceName
     * @param cluster
     * @param domainName
     */
    private static void addClusterAgainstDomain(String serviceName, Cluster cluster, String domainName) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Adding cluster to host/domain name -> cluster map against domain: [service] %s " +
                    "[domain-name] %s [cluster] %s", serviceName, domainName, cluster.getClusterId()));
        }
        if (StringUtils.isNotBlank(domainName)) {
            addClusterToHostNameClusterMap(domainName, cluster);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster added to host/domain name -> cluster map: [domain-name] %s [cluster] %s",
                        domainName, cluster.getClusterId()));
            }
        }
    }

    /**
     * Remove clusters mapped against all subscription domain names for the given service, tenant, cluster ids.
     *
     * @param serviceName
     * @param tenantId
     * @param clusterIds
     */
    public static void removeClustersAgainstAllDomains(String serviceName, int tenantId, Set<String> clusterIds) {
        try {
            TenantManager.acquireReadLock();
            TopologyManager.acquireReadLock();

            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service == null) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Service not found in topology: [service] %s", serviceName));
                }
                return;
            }
            for (String clusterId : clusterIds) {
                Cluster cluster = service.getCluster(clusterId);
                Tenant tenant = TenantManager.getInstance().getTenant(tenantId);
                if (tenant != null) {
                    for (Subscription subscription : tenant.getSubscriptions()) {
                        if (subscription.getServiceName().equals(serviceName)) {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Removing cluster from host/domain name -> cluster map: [service] %s " +
                                        "[tenant-id] %d [domains] %s", serviceName, tenantId, subscription.getSubscriptionDomains()));
                            }
                            for(SubscriptionDomain subscriptionDomain : subscription.getSubscriptionDomains()) {
                                removeClusterAgainstDomain(cluster, subscriptionDomain.getDomainName());
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Tenant not subscribed to service: %s", serviceName));
                            }
                        }
                    }
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
            TenantManager.releaseReadLock();
        }
    }

    private static void removeClusterAgainstDomain(Cluster cluster, String domainName) {
        removeClusterFromHostNameClusterMap(domainName, cluster);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Cluster removed from host/domain name -> cluster map: [domain-name] %s [cluster] %s",
                    domainName, cluster.getClusterId()));
        }
    }

    /**
     * Add cluster to host/domain name cluster map.
     *
     * @param hostName
     * @param cluster
     */
    private static void addClusterToHostNameClusterMap(String hostName, Cluster cluster) {
        if (!LoadBalancerContext.getInstance().getHostNameClusterMap().containsCluster((hostName))) {
            LoadBalancerContext.getInstance().getHostNameClusterMap().addCluster(hostName, cluster);
        }
    }

    /**
     * Remove cluseter from host/domain names cluster map.
     *
     * @param hostName
     * @param cluster
     */
    private static void removeClusterFromHostNameClusterMap(String hostName, Cluster cluster) {
        if (LoadBalancerContext.getInstance().getHostNameClusterMap().containsCluster(hostName)) {
            LoadBalancerContext.getInstance().getHostNameClusterMap().removeCluster(hostName);
        }
    }

    /**
     * Add cluster to multi-tenant cluster map.
     *
     * @param hostName
     * @param tenantId
     * @param cluster
     */
    private static void addClusterToMultiTenantClusterMap(String hostName, int tenantId, Cluster cluster) {
        // Add hostName, tenantId, cluster to multi-tenant map
        Map<Integer, Cluster> clusterMap = LoadBalancerContext.getInstance().getMultiTenantClusterMap().getClusters(hostName);
        if (clusterMap == null) {
            clusterMap = new HashMap<Integer, Cluster>();
            clusterMap.put(tenantId, cluster);
            LoadBalancerContext.getInstance().getMultiTenantClusterMap().addClusters(hostName, clusterMap);
        } else {
            clusterMap.put(tenantId, cluster);
        }
    }
}
