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
import org.apache.stratos.load.balancer.common.topology.TopologyReceiver;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.topology.ClusterRemovedEventListener;
import org.apache.stratos.messaging.listener.topology.CompleteTopologyEventListener;
import org.apache.stratos.messaging.listener.topology.MemberActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.ServiceRemovedEventListener;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.processor.topology.TopologyMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.Collection;

/**
 * Load balancer topology receiver.
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
        if(log.isInfoEnabled()) {
            log.info("Load balancer topology receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated);
        if(log.isInfoEnabled()) {
            log.info("Load balancer topology receiver thread terminated");
        }
    }

    private TopologyEventMessageDelegator createMessageDelegator() {
        TopologyMessageProcessorChain processorChain = createEventProcessorChain();
        processorChain.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyManager.acquireReadLock();
                    for(Service service : TopologyManager.getTopology().getServices()) {
                        for(Cluster cluster : service.getClusters()) {
                            if(hasActiveMembers(cluster)) {
                                addClusterToLbContext(cluster);
                            }
                        }
                    }
                }
                finally {
                    TopologyManager.releaseReadLock();
                }
            }

            private boolean hasActiveMembers(Cluster cluster) {
                for(Member member : cluster.getMembers()) {
                    if(member.isActive()) {
                        return true;
                    }
                }
                return false;
            }
        });
        return new TopologyEventMessageDelegator(processorChain);
    }

    private TopologyMessageProcessorChain createEventProcessorChain() {
        // Listen to topology events that affect clusters
        TopologyMessageProcessorChain processorChain = new TopologyMessageProcessorChain();
        processorChain.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyManager.acquireReadLock();

                    // Add cluster to the context when its first member is activated
                    MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent)event;
                    Cluster cluster = findCluster(memberActivatedEvent.getClusterId());
                    if(cluster == null) {
                        if(log.isErrorEnabled()) {
                            log.error(String.format("Cluster not found in topology: [cluster] %s", memberActivatedEvent.getClusterId()));
                        }
                    }
                    addClusterToLbContext(cluster);
                }
                finally {
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
                    ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent)event;
                    removeClusterFromLbContext(clusterRemovedEvent.getHostName());
                }
                finally {
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
                    ServiceRemovedEvent serviceRemovedEvent = (ServiceRemovedEvent)event;
                    for(Service service : TopologyManager.getTopology().getServices()) {
                        for(Cluster cluster : service.getClusters()) {
                            for(String hostName : cluster.getHostNames()) {
                                removeClusterFromLbContext(hostName);
                            }
                        }
                    }
                }
                finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });
        return processorChain;
    }

    private void addClusterToLbContext(Cluster cluster) {
        for(String hostName : cluster.getHostNames()) {
            if(!LoadBalancerContext.getInstance().clusterExists(hostName)) {
                LoadBalancerContext.getInstance().addCluster(hostName, cluster);
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Cluster added to load balancer context: [cluster] %s [hostname] %s", cluster.getClusterId(), hostName));
                }
            }
        }
    }

    private void removeClusterFromLbContext(String hostName) {
        if(LoadBalancerContext.getInstance().clusterExists(hostName)) {
            Cluster cluster = LoadBalancerContext.getInstance().getCluster(hostName);
            LoadBalancerContext.getInstance().removeCluster(hostName);
            if(log.isDebugEnabled()) {
                log.debug(String.format("Cluster removed from load balancer context: [cluster] %s [hostname] %s", cluster.getClusterId(), hostName));
            }
        }
    }

    private Cluster findCluster(String clusterId) {
        if(clusterId == null) {
            return null;
        }

        Collection<Service> services = TopologyManager.getTopology().getServices();
        for (Service service : services) {
            for (Cluster cluster : service.getClusters()) {
                if (clusterId.equals(cluster.getClusterId())) {
                    return cluster;
                }
            }
        }
        return null;
    }

    /**
     * Terminate load balancer topology receiver thread.
     */
    public void terminate() {
        topologyReceiver.terminate();
        terminated = true;
    }
}
