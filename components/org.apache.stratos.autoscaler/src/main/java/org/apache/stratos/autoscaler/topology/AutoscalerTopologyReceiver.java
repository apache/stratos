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

package org.apache.stratos.autoscaler.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterMonitor;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.processor.topology.TopologyMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyReceiver;

import java.util.Collection;

/**
 * Load balancer topology receiver.
 */
public class AutoscalerTopologyReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(AutoscalerTopologyReceiver.class);

    private TopologyReceiver topologyReceiver;
    private boolean terminated;

    public AutoscalerTopologyReceiver() {
		this.topologyReceiver = new TopologyReceiver(createMessageDelegator());
    }

    @Override
    public void run() {
        //FIXME this activated before autoscaler deployer activated.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ignore) {
        }
        Thread thread = new Thread(topologyReceiver);
        thread.start();
        if(log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated);
        if(log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread terminated");
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

                                Thread th = new Thread(new ClusterMonitorAdder(cluster));
                                th.start();
                        }
                    }
                }
                finally {
                    TopologyManager.releaseReadLock();
                }
            }

        });
        return new TopologyEventMessageDelegator(processorChain);
    }

    private TopologyMessageProcessorChain createEventProcessorChain() {
        // Listen to topology events that affect clusters
        TopologyMessageProcessorChain processorChain = new TopologyMessageProcessorChain();
        processorChain.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
            try {
                ClusterCreatedEvent e = (ClusterCreatedEvent) event;
                TopologyManager.acquireReadLock();
                Service service = TopologyManager.getTopology().getService(e.getServiceName());
                Cluster cluster = service.getCluster(e.getClusterId());
                Thread th = new Thread(new ClusterMonitorAdder(cluster));
                th.start();
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
                    ClusterRemovedEvent e = (ClusterRemovedEvent) event;
                    TopologyManager.acquireReadLock();
                    
                    removeClusterFromContext(e.getClusterId());
                }
                finally {
                    TopologyManager.releaseReadLock();
                }
            }

        });
        
        processorChain.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
            		
            }

        });
        
        processorChain.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
             
            	try {
            		TopologyManager.acquireReadLock();
					MemberTerminatedEvent e = (MemberTerminatedEvent) event;
                    String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(e.getPartitionId()).getId() ;
					AutoscalerContext.getInstance().getMonitor(e.getClusterId())
                            .getNetworkPartitionCtxt(networkPartitionId).getPartitionCtxt(e.getPartitionId())
                            .removeMemberStatsContext(e.getMemberId());
//					ClusterContext clusCtx = monitor.getClusterCtxt();
//					String networkPartitionId = monitor.
//                    if (networkPartitionId != null) {
//                        NetworkPartitionContext networkPartContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        networkPartContext.decrementCurrentMemberCount(1);
//                    }

				} finally {
					TopologyManager.releaseReadLock();
				}
            }

        });
        
        processorChain.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

            	try {
					TopologyManager.acquireReadLock();
					
					MemberActivatedEvent e = (MemberActivatedEvent)event;
                    String memberId = e.getMemberId();
                    String partitionId = e.getPartitionId();

                    String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(e.getPartitionId()).getId() ;
					PartitionContext partitionContext = AutoscalerContext.getInstance().getMonitor(e.getClusterId())
                            .getNetworkPartitionCtxt(networkPartitionId).getPartitionCtxt(partitionId);
//					ClusterContext clusCtx = monitor.getClusterCtxt();
//                    monitor.getNetworkPartitionCtxt(e.getId()).getPartitionCtxt(partitionId);
//                            .addMemberStatsContext(new MemberStatsContext(e.getMemberId()));
                    partitionContext.addMemberStatsContext(new MemberStatsContext(e.getMemberId()));
//					PartitionContext partCtxt = monitor.getNetworkPartitionCtxt(e.getId())
//                            .getPartitionCtxt(partitionId);
					partitionContext.incrementCurrentMemberCount(1);
					partitionContext.removePendingMember(memberId);
					
				}
                finally{
                	TopologyManager.releaseReadLock();
                }
            }
        });
        
        processorChain.addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
//                try {
//                    TopologyManager.acquireReadLock();
//
//                    // Remove all clusters of given service from context
//                    ServiceRemovedEvent serviceRemovedEvent = (ServiceRemovedEvent)event;
//                    for(Service service : TopologyManager.getTopology().getServices()) {
//                        for(Cluster cluster : service.getClusters()) {
//                            removeClusterFromContext(cluster.getHostName());
//                        }
//                    }
//                }
//                finally {
//                    TopologyManager.releaseReadLock();
//                }
            }
        });
        return processorChain;
    }
    
    private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;
        
        public ClusterMonitorAdder(Cluster cluster) {
            this.cluster = cluster;
        }

        public void run() {
//            if(cluster.isLbCluster()){
//                LbClusterMonitor monitor;
//
//                try{
//                monitor = AutoscalerUtil.getLbClusterMonitor(cluster);
//
//                } catch (PolicyValidationException e) {
//                    String msg = "LB cluster monitor creation failed for cluster: "+cluster.getClusterId();
//                    log.error(msg, e);
//                    throw new RuntimeException(msg, e);
//
//                } catch(PartitionValidationException e){
//                    String msg = "LB cluster monitor creation failed for cluster: "+cluster.getClusterId();
//                    log.error(msg, e);
//                    throw new RuntimeException(msg, e);
//                }
//
//                Thread th = new Thread(monitor);
//                th.start();
//                AutoscalerContext.getInstance().addLbMonitor(monitor);
//
//                if (log.isDebugEnabled()) {
//                    log.debug(String.format("LB cluster monitor has been added: [cluster] %s", cluster.getClusterId()));
//                }
//            } else {
                ClusterMonitor monitor;
                try {
                    monitor = AutoscalerUtil.getClusterMonitor(cluster);

                } catch (PolicyValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: "+cluster.getClusterId();
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);

                } catch(PartitionValidationException e){
                    String msg = "Cluster monitor creation failed for cluster: "+cluster.getClusterId();
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }

                Thread th = new Thread(monitor);
                th.start();
                AutoscalerContext.getInstance().addMonitor(monitor);
                log.info(String.format("Cluster monitor has been added: [cluster] %s",
                                                        cluster.getClusterId()));
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Cluster monitor has been added: [cluster] %s",
                                            cluster.getClusterId()));
                }
//            }
        }
    }

//    private void addClusterToContext(Cluster cluster) {
//        ClusterContext ctxt;
//        try {
//            ctxt = AutoscalerUtil.getClusterMonitor(cluster);
//        } catch (PolicyValidationException e) {
//            String msg = "Cluster monitor creation failed for cluster: "+cluster.getClusterId();
//            log.error(msg, e);
//            throw new RuntimeException(msg, e);
//        }
//        AutoscalerContext ruleCtxt = AutoscalerContext.getInstance();
//        ClusterMonitor monitor =
//                                 new ClusterMonitor(cluster.getClusterId(), ctxt,
//                                                    ruleCtxt.getStatefulSession());
//        Thread th = new Thread(monitor);
//        th.start();
//        AutoscalerContext.getInstance().addMonitor(monitor);
//        if (log.isDebugEnabled()) {
//            log.debug(String.format("Cluster monitor has been added: [cluster] %s",
//                                    cluster.getClusterId()));
//        }
//    }

    private void removeClusterFromContext(String clusterId) {
        ClusterMonitor monitor = AutoscalerContext.getInstance().removeMonitor(clusterId);
//        monitor.unsubscribe();
        monitor.destroy();
            if(log.isDebugEnabled()) {
                log.debug(String.format("Cluster monitor has been removed: [cluster] %s ", clusterId));
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
