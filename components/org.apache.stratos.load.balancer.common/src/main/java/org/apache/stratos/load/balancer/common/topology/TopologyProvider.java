/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.common.topology;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.domain.Member;
import org.apache.stratos.load.balancer.common.domain.Service;
import org.apache.stratos.load.balancer.common.domain.Topology;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load balancer topology provider.
 */
public class TopologyProvider {

    private static final Log log = LogFactory.getLog(TopologyProvider.class);

    private Topology topology;
    private Map<String, Cluster> clusterIdToClusterMap;
    private Map<String, Cluster> hostNameToClusterMap;
    private Map<String, Map<Integer, Cluster>> hostNameToTenantIdToClusterMap;
    private Map<String, String> memberHostNameToClusterHostNameMap;

    public TopologyProvider() {
        this.topology = new Topology();
        this.clusterIdToClusterMap = new ConcurrentHashMap<String, Cluster>();
        this.hostNameToClusterMap = new ConcurrentHashMap<String, Cluster>();
        this.hostNameToTenantIdToClusterMap = new ConcurrentHashMap<String, Map<Integer, Cluster>>();
        this.memberHostNameToClusterHostNameMap = new ConcurrentHashMap<String, String>();
    }

    public boolean serviceExists(String serviceName) {
        return topology.getService(serviceName) != null;
    }

    /**
     * Add service to the topology.
     * @param service
     */
    public void addService(Service service) {
        if(service != null) {
            topology.addService(service);
            log.info(String.format("Service added: [service] %s", service.getServiceName()));

            Collection<Cluster> clusters = service.getClusters();
            if ((clusters != null) && (clusters.size() > 0)) {
                for (Cluster cluster : clusters) {
                    addCluster(cluster);
                }
            }
        }
    }

    /**
     * Add cluster to the topology.
     * @param cluster
     */
    public void addCluster(Cluster cluster) {
        if(cluster != null) {
            Service service = topology.getService(cluster.getServiceName());
            if(service == null) {
                throw new RuntimeException(String.format("Could not add cluster, service not found: [service] %s",
                        cluster.getServiceName()));
            }
            service.addCluster(cluster);
            clusterIdToClusterMap.put(cluster.getClusterId(), cluster);

            for(String hostName : cluster.getHostNames()) {
                hostNameToClusterMap.put(hostName, cluster);
            }

            if((cluster.getHostNames() != null) && (cluster.getHostNames().size() > 0)) {
                log.info(String.format("Cluster added: [cluster] %s [hostnames] %s", cluster.getClusterId(),
                        cluster.getHostNames()));
            }

            Collection<Member> members = cluster.getMembers();
            if((members != null) && (members.size() > 0)) {
                for(Member member : members) {
                    addMember(member);
                }
            }
        }
    }

    /**
     * Add a member to its cluster.
     * @param member
     */
    public void addMember(Member member) {
        Cluster cluster = getClusterByClusterId(member.getClusterId());
        if(cluster == null) {
            log.warn(String.format("Could not add member, cluster not found: [cluster] %s",
                    member.getClusterId()));
            return;
        }
        if(StringUtils.isBlank(member.getHostName())) {
            log.warn(String.format("Could not add member, member hostname not found: [cluster] %s [member] %s",
                    member.getClusterId(), member.getMemberId()));
            return;
        }

        cluster.addMember(member);
        if((cluster.getHostNames() != null) && (cluster.getHostNames().size() > 0)) {
            memberHostNameToClusterHostNameMap.put(member.getHostName(), cluster.getHostNames().iterator().next());
        }

        log.info(String.format("Member added to cluster: [cluster] %s [member] %s",
                member.getClusterId(), member.getHostName()));
    }

    /**
     * Remove cluster from the topology.
     * @param clusterId
     */
    public void removeCluster(String clusterId) {
        Cluster cluster = getClusterByClusterId(clusterId);
        if(cluster == null) {
            log.warn(String.format("Could not remove cluster, cluster not found: [cluster] %s", clusterId));
            return;
        }

        for(String hostName : cluster.getHostNames()) {
            hostNameToClusterMap.remove(hostName);
        }
        clusterIdToClusterMap.remove(cluster.getClusterId());

        if((cluster.getHostNames() != null) && (cluster.getHostNames().size() > 0)) {
            log.info(String.format("Cluster removed: [cluster] %s [hostnames] %s", cluster.getClusterId(),
                    cluster.getHostNames()));
        }
    }

    /**
     * Returns true if cluster exists by cluster id else return false.
     * @param clusterId
     * @return
     */
    public boolean clusterExistsByClusterId(String clusterId) {
        return (getClusterByClusterId(clusterId) != null);
    }

    /**
     * Returns true if cluster exists by host name else return false.
     * @param hostName
     * @return
     */
    public boolean clusterExistsByHostName(String hostName) {
        return (hostNameToClusterMap.containsKey(hostName) || hostNameToTenantIdToClusterMap.containsKey(hostName));
    }

    /**
     * Get cluster by cluster id.
     * @param clusterId
     * @return
     */
    public Cluster getClusterByClusterId(String clusterId) {
        return clusterIdToClusterMap.get(clusterId);
    }

    /**
     * Add tenant signup for cluster.
     * @param clusterId
     * @param tenantId
     */
    public void addTenantSignUp(String clusterId, int tenantId) {
        Cluster cluster = getClusterByClusterId(clusterId);
        if(cluster != null) {
            boolean subscribed = false;
            for(String hostName : cluster.getHostNames()) {
                Map<Integer, Cluster> tenantIdToClusterMap = hostNameToTenantIdToClusterMap.get(hostName);
                if(tenantIdToClusterMap == null) {
                    tenantIdToClusterMap = new ConcurrentHashMap<Integer, Cluster>();
                    hostNameToTenantIdToClusterMap.put(hostName, tenantIdToClusterMap);
                }
                tenantIdToClusterMap.put(tenantId, cluster);
                subscribed = true;
            }
            if(subscribed) {
                log.info(String.format("Tenant signed up to cluster: [tenant] %d [cluster] %s [hostnames] %s",
                        tenantId, cluster.getClusterId(), cluster.getHostNames()));
            }
        }
    }

    /**
     * Remove tenant signup for cluster.
     * @param clusterId
     * @param tenantId
     */
    public void removeTenantSignUp(String clusterId, int tenantId) {
        Cluster cluster = getClusterByClusterId(clusterId);
        if(cluster == null) {
            log.warn(String.format("Could not remove tenant signup from cluster, cluster not found: [cluster] %s", clusterId));
        }

        for(String hostName : cluster.getHostNames()) {
            Map<Integer, Cluster> tenantIdToClusterMap = hostNameToTenantIdToClusterMap.get(hostName);
            if (tenantIdToClusterMap != null) {
                Cluster cluster_ = tenantIdToClusterMap.get(tenantId);
                if (cluster_ != null) {
                    tenantIdToClusterMap.remove(tenantId);
                    log.info(String.format("Tenant signup removed from cluster: [tenant] %d [cluster] %s [hostnames] %s",
                            tenantId, cluster.getClusterId(), cluster.getHostNames()));
                }
            }
        }
    }

    /**
     * Get cluster by hostname.
     * @param hostName
     * @return
     */
    public Cluster getClusterByHostName(String hostName) {
        return hostNameToClusterMap.get(hostName);
    }

    /**
     * Get cluster by hostname for tenant.
     * @param hostName
     * @param tenantId
     * @return
     */
    public Cluster getClusterByHostName(String hostName, int tenantId) {
        Map<Integer, Cluster> tenantIdToClusterMap = hostNameToTenantIdToClusterMap.get(hostName);
        if(tenantIdToClusterMap != null) {
            return tenantIdToClusterMap.get(tenantId);
        }
        return null;
    }

    /**
     * Remove a member from its cluster.
     * @param clusterId
     * @param memberId
     */
    public void removeMember(String clusterId, String memberId) {
        Cluster cluster = getClusterByClusterId(clusterId);
        if(cluster == null) {
            log.warn(String.format("Could not remove member, cluster not found: [cluster] %s", clusterId));
            return;
        }

        Member member = cluster.getMember(memberId);
        if(member != null) {
            cluster.removeMember(memberId);
            if(memberHostNameToClusterHostNameMap.containsKey(member.getHostName())) {
                memberHostNameToClusterHostNameMap.remove(member.getHostName());
            }

            log.info(String.format("Member removed from cluster: [cluster] %s [member] %s",
                    clusterId, member.getHostName()));

            if(cluster.getMembers().size() == 0) {
                log.info(String.format("No members found in cluster, removing cluster: " +
                        "[cluster] %s", cluster.getClusterId()));
                removeCluster(cluster.getClusterId());
            }
        }
    }

    /**
     * Get cluster hostname of member by member hostname/ip address.
     * @param memberHostName
     * @return
     */
    public String getClusterHostname(String memberHostName) {
        return memberHostNameToClusterHostNameMap.get(memberHostName);
    }

    public Topology getTopology() {
        return topology;
    }
}
