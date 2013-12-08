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

package org.apache.stratos.load.balancer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.load.balancer.context.LoadBalancerContextUtil;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.ServiceRemovedEvent;
import org.apache.stratos.messaging.listener.topology.ClusterRemovedEventListener;
import org.apache.stratos.messaging.listener.topology.CompleteTopologyEventListener;
import org.apache.stratos.messaging.listener.topology.MemberActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.ServiceRemovedEventListener;
import org.apache.stratos.messaging.message.processor.topology.TopologyMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyReceiver;

/**
 * Load balancer topology receiver updates load balancer context according to
 * incoming topology events.
 */
public class LoadBalancerTopologyReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(LoadBalancerTopologyReceiver.class);

    private TopologyReceiver topologyReceiver;
    private boolean terminated;

    public LoadBalancerTopologyReceiver() {
        this.topologyReceiver = new TopologyReceiver(createMessageDelegator());
    }

    @Override
    public void run() {
        Thread thread = new Thread(topologyReceiver);
        thread.start();
        if (log.isInfoEnabled()) {
            log.info("Load balancer topology receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated) ;
        if (log.isInfoEnabled()) {
            log.info("Load balancer topology receiver thread terminated");
        }
    }

    private TopologyEventMessageDelegator createMessageDelegator() {
        TopologyMessageProcessorChain processorChain = createEventProcessorChain();
        return new TopologyEventMessageDelegator(processorChain);
    }

    private TopologyMessageProcessorChain createEventProcessorChain() {
        // Listen to topology events that affect clusters
        TopologyMessageProcessorChain processorChain = new TopologyMessageProcessorChain();
        processorChain.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyManager.acquireReadLock();
                    for (Service service : TopologyManager.getTopology().getServices()) {
                        for (Cluster cluster : service.getClusters()) {
                            if (hasActiveMembers(cluster)) {
                                LoadBalancerContextUtil.addClusterToLbContext(cluster);
                            }
                        }
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }

            private boolean hasActiveMembers(Cluster cluster) {
                for (Member member : cluster.getMembers()) {
                    if (member.isActive()) {
                        return true;
                    }
                }
                return false;
            }
        });
        processorChain.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyManager.acquireReadLock();

                    // Add cluster to the context when its first member is activated
                    MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;
                    Cluster cluster = LoadBalancerContext.getInstance().getClusterIdClusterMap().getCluster(memberActivatedEvent.getClusterId());
                    if (cluster != null) {
                        LoadBalancerContextUtil.addClusterToLbContext(cluster);
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Cluster not found in cluster id cluster map: [cluster] %s", memberActivatedEvent.getClusterId()));
                        }
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });
        processorChain.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyManager.acquireReadLock();

                    // Remove cluster from context
                    ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;
                    Cluster cluster = LoadBalancerContext.getInstance().getClusterIdClusterMap().getCluster(clusterRemovedEvent.getClusterId());
                    if (cluster != null) {
                        for (String hostName : cluster.getHostNames()) {
                            LoadBalancerContextUtil.removeClusterFromLbContext(hostName);
                        }
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Cluster not found in cluster id cluster map: [cluster] %s", clusterRemovedEvent.getClusterId()));
                        }
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });
        processorChain.addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyManager.acquireReadLock();

                    // Remove all clusters of given service from context
                    ServiceRemovedEvent serviceRemovedEvent = (ServiceRemovedEvent) event;
                    Service service = TopologyManager.getTopology().getService(serviceRemovedEvent.getServiceName());
                    if (service != null) {
                        for (Cluster cluster : service.getClusters()) {
                            for (String hostName : cluster.getHostNames()) {
                                LoadBalancerContextUtil.removeClusterFromLbContext(hostName);
                            }
                        }
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn(String.format("Service not found in topology: [service] %s", serviceRemovedEvent.getServiceName()));
                        }
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });
        return processorChain;
    }

    /**
     * Terminate load balancer topology receiver thread.
     */
    public void terminate() {
        topologyReceiver.terminate();
        terminated = true;
    }
}
