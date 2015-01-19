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

package org.apache.stratos.load.balancer.messaging.receiver;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.load.balancer.context.LoadBalancerContextUtil;
import org.apache.stratos.load.balancer.context.map.AlgorithmContextMap;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer topology receiver updates load balancer context according to
 * incoming topology events.
 */
public class LoadBalancerTopologyEventReceiver extends TopologyEventReceiver {

    private static final Log log = LogFactory.getLog(LoadBalancerTopologyEventReceiver.class);

    public LoadBalancerTopologyEventReceiver() {
        addEventListeners();
    }

    public void execute() {
	    super.execute();
        if (log.isInfoEnabled()) {
            log.info("Load balancer topology receiver thread started");
        }
    }

    private void addEventListeners() {
        // Listen to topology events that affect clusters
        addEventListener(new CompleteTopologyEventListener() {
            private boolean initialized;

            @Override
            protected void onEvent(Event event) {
                if (!initialized) {
                    try {
                        TopologyManager.acquireReadLock();
                        for (Service service : TopologyManager.getTopology().getServices()) {
                            for (Cluster cluster : service.getClusters()) {
                                if (clusterHasActiveMembers(cluster)) {
                                    LoadBalancerContextUtil.addClusterAgainstHostNames(cluster);
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Cluster does not have any active members");
                                    }
                                }
                                for (Member member : cluster.getMembers()) {
                                    if (member.getStatus() == MemberStatus.Active) {
                                        addMemberIpsToMemberIpHostnameMap(cluster, member);
                                    }

                                }
                            }
                        }
                        initialized = true;
                    } catch (Exception e) {
                        log.error("Error processing event", e);
                    } finally {
                        TopologyManager.releaseReadLock();
                    }
                }
            }

            private boolean clusterHasActiveMembers(Cluster cluster) {
                for (Member member : cluster.getMembers()) {
                    if (member.getStatus() == MemberStatus.Active) {
                        return true;
                    }
                }
                return false;
            }
        });
        addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;

                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberActivatedEvent.getServiceName(),
                        memberActivatedEvent.getClusterId());

                try {

                    Service service = TopologyManager.getTopology().getService(memberActivatedEvent.getServiceName());
                    if (service == null) {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Service not found in topology: [service] %s",
                                    memberActivatedEvent.getServiceName()));
                        }
                        return;
                    }
                    Cluster cluster = service.getCluster(memberActivatedEvent.getClusterId());
                    if (cluster == null) {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Cluster not found in topology: [service] %s [cluster] %s",
                                    memberActivatedEvent.getServiceName(), memberActivatedEvent.getClusterId()));
                        }
                        return;
                    }
                    Member member = cluster.getMember(memberActivatedEvent.getMemberId());
                    if (member == null) {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Member not found in topology: [service] %s [cluster] %s [member] %s",
                                    memberActivatedEvent.getServiceName(), memberActivatedEvent.getClusterId(),
                                    memberActivatedEvent.getMemberId()));
                        }
                        return;
                    }

                    // Add member to member-ip -> hostname map
                    addMemberIpsToMemberIpHostnameMap(cluster, member);

                    if (LoadBalancerContext.getInstance().getClusterIdClusterMap().containsCluster(
                            member.getClusterId())) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Cluster already exists in load balancer context: [service] %s " +
                                    "[cluster] %s", member.getServiceName(), member.getClusterId()));
                        }
                        // At this point member is already added to the cluster object in load balancer context
                        return;
                    }

                    // Add cluster to load balancer context when its first member is activated:
                    // Cluster not found in load balancer context, add it
                    LoadBalancerContextUtil.addClusterAgainstHostNames(cluster);
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId());
                }
            }
        });
        addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {

                MemberMaintenanceModeEvent memberMaintenanceModeEvent = (MemberMaintenanceModeEvent) event;

                TopologyManager.acquireReadLockForCluster(memberMaintenanceModeEvent.getServiceName(),
                        memberMaintenanceModeEvent.getClusterId());

                try {
                    //TopologyManager.acquireReadLock();

                    Member member = findMember(memberMaintenanceModeEvent.getServiceName(),
                            memberMaintenanceModeEvent.getClusterId(), memberMaintenanceModeEvent.getMemberId());

                    if (member != null) {
                        removeMemberIpsFromMemberIpHostnameMap(member);
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberMaintenanceModeEvent.getServiceName(),
                            memberMaintenanceModeEvent.getClusterId());
                }
            }
        });
        addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {

                MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;
                TopologyManager.acquireReadLockForCluster(memberSuspendedEvent.getServiceName(),
                        memberSuspendedEvent.getClusterId());

                try {
                    //TopologyManager.acquireReadLock();
                    Member member = findMember(memberSuspendedEvent.getServiceName(),
                            memberSuspendedEvent.getClusterId(), memberSuspendedEvent.getMemberId());

                    if (member != null) {
                        removeMemberIpsFromMemberIpHostnameMap(member);
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberSuspendedEvent.getServiceName(),
                            memberSuspendedEvent.getClusterId());
                }
            }
        });
        addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                //TopologyManager.acquireReadLock();
                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;

                TopologyManager.acquireReadLockForCluster(memberTerminatedEvent.getServiceName(),
                        memberTerminatedEvent.getClusterId());

                try {
                    Member member = findMember(memberTerminatedEvent.getServiceName(),
                            memberTerminatedEvent.getClusterId(), memberTerminatedEvent.getMemberId());

                    if (member != null) {
                        removeMemberIpsFromMemberIpHostnameMap(member);
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberTerminatedEvent.getServiceName(),
                            memberTerminatedEvent.getClusterId());
                }
            }
        });
        addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                // Remove cluster from context
                ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;
                TopologyManager.acquireReadLockForCluster(clusterRemovedEvent.getServiceName(),
                        clusterRemovedEvent.getClusterId());

                try {
                    AlgorithmContextMap.getInstance().removeCluster(clusterRemovedEvent.getServiceName(),
                            clusterRemovedEvent.getClusterId());
                } catch (Exception e) {
                    log.error("Could not remove cluster from load balancer algorithm context map", e);
                }

                try {
                    Cluster cluster = LoadBalancerContext.getInstance().getClusterIdClusterMap().getCluster(clusterRemovedEvent.getClusterId());
                    if (cluster != null) {
                        for (Member member : cluster.getMembers()) {
                            removeMemberIpsFromMemberIpHostnameMap(member);
                        }
                        LoadBalancerContextUtil.removeClusterAgainstHostNames(cluster.getClusterId());
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Cluster not found in load balancer context: [service] %s [cluster] %s",
                                    clusterRemovedEvent.getServiceName(), clusterRemovedEvent.getClusterId()));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLockForCluster(clusterRemovedEvent.getServiceName(),
                            clusterRemovedEvent.getClusterId());
                }
            }
        });
        addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                ServiceRemovedEvent serviceRemovedEvent = (ServiceRemovedEvent) event;
                TopologyManager.acquireReadLockForService(serviceRemovedEvent.getServiceName());

                try {
                    //TopologyManager.acquireReadLock();

                    // Remove all clusters of given service from context
                    Service service = TopologyManager.getTopology().getService(serviceRemovedEvent.getServiceName());
                    if (service != null) {
                        for (Cluster cluster : service.getClusters()) {
                            for (Member member : cluster.getMembers()) {
                                removeMemberIpsFromMemberIpHostnameMap(member);
                            }
                            LoadBalancerContextUtil.removeClusterAgainstHostNames(cluster.getClusterId());
                        }
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Service not found in topology: [service] %s",
                                    serviceRemovedEvent.getServiceName()));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForService(serviceRemovedEvent.getServiceName());
                }
            }
        });
    }

    private Member findMember(String serviceName, String clusterId, String memberId) {
        Service service = TopologyManager.getTopology().getService(serviceName);
        if (service == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Service not found in topology: [service] %s", serviceName));
            }
            return null;
        }

        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cluster not found in topology: [service] %s [cluster] %s", serviceName, clusterId));
            }
            return null;
        }

        Member member = cluster.getMember(memberId);
        if (member == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Member not found in topology: [service] %s [cluster] %s [member] %s", serviceName,
                        clusterId, memberId));
            }
            return null;
        }
        return member;
    }

    private void addMemberIpsToMemberIpHostnameMap(Cluster cluster, Member member) {
        if ((cluster.getHostNames() == null) || (cluster.getHostNames().size() == 0)) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Hostnames not found in cluster %s, could not add member ips to member-ip " +
                        "-> hostname map", member.getClusterId()));
            }
            return;
        }

        String hostname = cluster.getHostNames().get(0);
        if (cluster.getHostNames().size() > 1) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Multiple hostnames found in cluster %s, using %s",
                        cluster.getHostNames().toString(), hostname));
            }
        }

        if (StringUtils.isNotBlank(member.getDefaultPrivateIP())) {
            LoadBalancerContext.getInstance().getMemberIpHostnameMap().put(member.getDefaultPrivateIP(), hostname);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member private ip added to member-ip -> hostname map: [service] %s [cluster] " +
                                "%s [member] %s [private-ip] %s", member.getServiceName(), member.getClusterId(),
                        member.getMemberId(), member.getDefaultPrivateIP()
                ));
            }
        }
        if (StringUtils.isNotBlank(member.getDefaultPublicIP())) {
            LoadBalancerContext.getInstance().getMemberIpHostnameMap().put(member.getDefaultPublicIP(), hostname);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member public ip added to member-ip -> hostname map: [service] %s [cluster] " +
                                "%s [member] %s [public-ip] %s", member.getServiceName(), member.getClusterId(),
                        member.getMemberId(), member.getDefaultPublicIP()
                ));
            }
        }
    }

    private void removeMemberIpsFromMemberIpHostnameMap(Member member) {
        if (StringUtils.isNotBlank(member.getDefaultPrivateIP())) {
            LoadBalancerContext.getInstance().getMemberIpHostnameMap().remove(member.getDefaultPrivateIP());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member private ip removed from member-ip -> hostname map: [private-ip] %s",
                        member.getDefaultPrivateIP()));
            }
        }
        if (StringUtils.isNotBlank(member.getDefaultPublicIP())) {
            LoadBalancerContext.getInstance().getMemberIpHostnameMap().remove(member.getDefaultPublicIP());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member public ip removed from member-ip -> hostname map: [public-ip] %s",
                        member.getDefaultPublicIP()));
            }
        }
    }
}
