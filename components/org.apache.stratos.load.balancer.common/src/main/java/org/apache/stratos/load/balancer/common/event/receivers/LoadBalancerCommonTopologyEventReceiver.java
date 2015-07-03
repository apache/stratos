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

package org.apache.stratos.load.balancer.common.event.receivers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer common topology receiver updates the topology in the given topology provider
 * according to topology events.
 */
public class LoadBalancerCommonTopologyEventReceiver extends TopologyEventReceiver {

    private static final Log log = LogFactory.getLog(LoadBalancerCommonTopologyEventReceiver.class);

    private TopologyProvider topologyProvider;
    private boolean initialized;

    public LoadBalancerCommonTopologyEventReceiver(TopologyProvider topologyProvider) {
        this.topologyProvider = topologyProvider;
        addEventListeners();
    }

    public void execute() {
        super.execute();
        if (log.isInfoEnabled()) {
            log.info("Load balancer topology receiver thread started");
        }
    }

    public void initializeTopology() {
        if (initialized) {
            return;
        }

        try {
            boolean membersFound = false;
            TopologyManager.acquireReadLock();
            for (Service service : TopologyManager.getTopology().getServices()) {
                for (Cluster cluster : service.getClusters()) {
                    for (Member member : cluster.getMembers()) {
                        if (member.getStatus() == MemberStatus.Active) {

                            String serviceName = member.getServiceName();
                            String clusterId = member.getClusterId();
                            String memberId = member.getMemberId();

                            String networkPartitionIdFilter = System.getProperty(
                                    StratosConstants.TOPOLOGY_NETWORK_PARTITION_FILTER);

                            if (networkPartitionIdFilter != null && !networkPartitionIdFilter.equals("")) {
                                if (member.getNetworkPartitionId().equals(networkPartitionIdFilter)) {
                                    addMember(serviceName, clusterId, memberId);
                                    membersFound = true;
                                }
                            } else {
                                addMember(serviceName, clusterId, memberId);
                                membersFound = true;
                            }
                        }
                    }
                }
            }
            if (membersFound) {
                initialized = true;
            }
        } catch (Exception e) {
            log.error("Error processing complete topology event", e);
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void addEventListeners() {

        addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (!initialized) {
                    initializeTopology();
                }
            }
        });

        addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;

                String serviceName = memberActivatedEvent.getServiceName();
                String clusterId = memberActivatedEvent.getClusterId();
                String memberId = memberActivatedEvent.getMemberId();

                try {
                    TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
                    String networkPartitionIdFilter = System.getProperty(
                            StratosConstants.TOPOLOGY_NETWORK_PARTITION_FILTER);

                    if (networkPartitionIdFilter != null && !networkPartitionIdFilter.equals("")) {
                        if (memberActivatedEvent.getNetworkPartitionId().equals(networkPartitionIdFilter)) {
                            addMember(serviceName, clusterId, memberId);
                        }
                    } else {
                        addMember(serviceName, clusterId, memberId);
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
                }
            }
        });

        addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {

                MemberMaintenanceModeEvent memberMaintenanceModeEvent = (MemberMaintenanceModeEvent) event;

                String serviceName = memberMaintenanceModeEvent.getServiceName();
                String clusterId = memberMaintenanceModeEvent.getClusterId();
                String memberId = memberMaintenanceModeEvent.getMemberId();

                try {
                    TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
                    removeMember(serviceName, clusterId, memberId);
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceName,
                            clusterId);
                }
            }
        });

        addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {

                MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;

                String serviceName = memberSuspendedEvent.getServiceName();
                String clusterId = memberSuspendedEvent.getClusterId();
                String memberId = memberSuspendedEvent.getMemberId();

                try {
                    TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
                    removeMember(serviceName, clusterId, memberId);
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForCluster(memberSuspendedEvent.getServiceName(),
                            memberSuspendedEvent.getClusterId());
                }
            }
        });

        addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;

                String serviceName = memberTerminatedEvent.getServiceName();
                String clusterId = memberTerminatedEvent.getClusterId();
                String memberId = memberTerminatedEvent.getMemberId();

                try {
                    TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
                    removeMember(serviceName, clusterId, memberId);
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
                }
            }
        });

        addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;
                String serviceName = clusterRemovedEvent.getServiceName();
                String clusterId = clusterRemovedEvent.getClusterId();

                try {
                    TopologyManager.acquireReadLockForCluster(serviceName, clusterId);

                    Service service = TopologyManager.getTopology().getService(serviceName);
                    if (service == null) {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Service not found in topology: [service] %s", serviceName));
                        }
                        return;
                    }

                    Cluster cluster = service.getCluster(clusterId);
                    removeCluster(cluster);
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
                }
            }
        });

        addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                ServiceRemovedEvent serviceRemovedEvent = (ServiceRemovedEvent) event;
                String serviceName = serviceRemovedEvent.getServiceName();

                try {
                    TopologyManager.acquireReadLockForService(serviceName);

                    Service service = TopologyManager.getTopology().getService(serviceName);
                    if (service == null) {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Service not found in topology: [service] %s",
                                    serviceName));
                        }
                        return;
                    }
                    for (Cluster cluster : service.getClusters()) {
                        removeCluster(cluster);
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForService(serviceName);
                }
            }
        });
    }

    /**
     * Remove cluster from topology provider
     *
     * @param cluster
     */
    protected void removeCluster(Cluster cluster) {
        for (Member member : cluster.getMembers()) {
            removeMember(member.getServiceName(), member.getClusterId(), member.getMemberId());
        }
    }

    /**
     * Add member to topology provider
     *
     * @param serviceName
     * @param clusterId
     * @param memberId
     */
    protected void addMember(String serviceName, String clusterId, String memberId) {
        Service service = TopologyManager.getTopology().getService(serviceName);
        if (service == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Service not found in topology: [service] %s",
                        serviceName));
            }
            return;
        }

        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cluster not found in topology: [service] %s [cluster] %s",
                        serviceName, clusterId));
            }
            return;
        }
        validateHostNames(cluster);

        // Add service if not exists
        if (!topologyProvider.serviceExists(serviceName)) {
            topologyProvider.addService(transformService(service));
        }

        // Add cluster if not exists
        if (!topologyProvider.clusterExistsByClusterId(cluster.getClusterId())) {
            topologyProvider.addCluster(transformCluster(cluster));
        }

        Member member = cluster.getMember(memberId);
        if (member == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Member not found in topology: [service] %s [cluster] %s [member] %s",
                        serviceName, clusterId,
                        memberId));
            }
            return;
        }

        org.apache.stratos.load.balancer.common.domain.Member lbMember = transformMember(member);
        org.apache.stratos.load.balancer.common.domain.Service lbService = topologyProvider.getTopology().
                getService(serviceName);
        if (lbService == null) {
            log.warn(String.format("Service not found: %s", serviceName));
            return;
        }
        lbService.addPorts(lbMember.getPorts());
        topologyProvider.addMember(lbMember);
    }

    /**
     * Remove member from topology provider
     *
     * @param serviceName
     * @param clusterId
     * @param memberId
     */
    protected void removeMember(String serviceName, String clusterId, String memberId) {
        Service service = TopologyManager.getTopology().getService(serviceName);
        if (service == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Service not found in topology: [service] %s",
                        serviceName));
            }
            return;
        }

        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cluster not found in topology: [service] %s [cluster] %s",
                        serviceName, clusterId));
            }
            return;
        }
        validateHostNames(cluster);

        Member member = cluster.getMember(memberId);
        if (member == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Member not found in topology: [service] %s [cluster] %s [member] %s",
                        serviceName, clusterId,
                        memberId));
            }
            return;
        }

        if (member != null) {
            topologyProvider.removeMember(cluster.getClusterId(), member.getMemberId());
        }
    }

    private void validateHostNames(Cluster cluster) {
        if ((cluster.getHostNames() == null) || (cluster.getHostNames().size() == 0)) {
            throw new RuntimeException(String.format("Host names not found in cluster: " +
                    "[cluster] %s", cluster.getClusterId()));
        }
    }

    private org.apache.stratos.load.balancer.common.domain.Service transformService(Service messagingService) {
        org.apache.stratos.load.balancer.common.domain.Service service =
                new org.apache.stratos.load.balancer.common.domain.Service(messagingService.getServiceName());
        return service;
    }

    private org.apache.stratos.load.balancer.common.domain.Port transformPort(Port messagingPort) {
        org.apache.stratos.load.balancer.common.domain.Port port =
                new org.apache.stratos.load.balancer.common.domain.Port(messagingPort.getProtocol(),
                        messagingPort.getValue(), messagingPort.getProxy());
        return port;
    }

    private org.apache.stratos.load.balancer.common.domain.Cluster transformCluster(Cluster messagingCluster) {
        org.apache.stratos.load.balancer.common.domain.Cluster cluster =
                new org.apache.stratos.load.balancer.common.domain.Cluster(messagingCluster.getServiceName(),
                        messagingCluster.getClusterId());
        cluster.setTenantRange(messagingCluster.getTenantRange());
        if (messagingCluster.getHostNames() != null) {
            for (String hostName : messagingCluster.getHostNames()) {
                cluster.addHostName(hostName);
            }
        }
        return cluster;
    }

    private org.apache.stratos.load.balancer.common.domain.Member transformMember(Member messagingMember) {

        String hostName;
        if (messagingMember.getLoadBalancingIPType() == LoadBalancingIPType.Private) {
            if (StringUtils.isEmpty(messagingMember.getDefaultPrivateIP())) {
                throw new RuntimeException(String.format("Default private IP not found: [member] %s",
                        messagingMember.getMemberId()));
            }
            hostName = messagingMember.getDefaultPrivateIP();
        } else if (messagingMember.getLoadBalancingIPType() == LoadBalancingIPType.Public) {
            if (StringUtils.isEmpty(messagingMember.getDefaultPublicIP())) {
                throw new RuntimeException(String.format("Default public IP not found: [member] %s",
                        messagingMember.getMemberId()));
            }
            hostName = messagingMember.getDefaultPublicIP();
        } else {
            throw new RuntimeException(String.format("Unknown load balancing IP type found: %s",
                    messagingMember.getLoadBalancingIPType()));
        }

        org.apache.stratos.load.balancer.common.domain.Member member =
                new org.apache.stratos.load.balancer.common.domain.Member(messagingMember.getServiceName(),
                        messagingMember.getClusterId(), messagingMember.getMemberId(), hostName);

        if (messagingMember.getPorts() != null) {
            for (Port port : messagingMember.getPorts()) {
                member.addPort(transformPort(port));
            }
        }
        return member;
    }
}
