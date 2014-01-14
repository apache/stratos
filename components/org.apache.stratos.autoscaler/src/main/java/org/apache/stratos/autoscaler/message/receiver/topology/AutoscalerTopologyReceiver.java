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

package org.apache.stratos.autoscaler.message.receiver.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.AbstractMonitor;
import org.apache.stratos.autoscaler.monitor.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.LbClusterMonitor;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
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
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

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
                for(Service service : TopologyManager.getTopology().getServices()) {
                    for(Cluster cluster : service.getClusters()) {
                        Thread th;
                        if(cluster.isLbCluster()){
                            th = new Thread(new LBClusterMonitorAdder(cluster));
                        }else{
                            th = new Thread(new ClusterMonitorAdder(cluster));
                        }

                        th.start();
                    }
                }
            }
            finally {
                TopologyManager.releaseReadLock();
            }
            }

        });

        processorChain.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
            try {
                ClusterCreatedEvent e = (ClusterCreatedEvent) event;
                TopologyManager.acquireReadLock();
                Service service = TopologyManager.getTopology().getService(e.getServiceName());
                Cluster cluster = service.getCluster(e.getClusterId());
                if (cluster.isLbCluster()) {
                    Thread th = new Thread(new LBClusterMonitorAdder(cluster));
                    th.start();
                } else {
                    Thread th = new Thread(new ClusterMonitorAdder(cluster));
                    th.start();
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
                ClusterRemovedEvent e = (ClusterRemovedEvent) event;
                TopologyManager.acquireReadLock();
                String serviceName = e.getServiceName();
                String clusterId = e.getClusterId();
                AbstractMonitor monitor;

                if(e.isLbCluster()){
                    monitor = AutoscalerContext.getInstance().removeLbMonitor(clusterId);

                } else {
                    monitor = AutoscalerContext.getInstance().removeMonitor(clusterId);
                }

//                runTerminateAllRule(monitor);
                monitor.destroy();
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Cluster monitor has been removed: [cluster] %s ", clusterId));
                }
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
                String networkPartitionId = e.getNetworkPartitionId();
                String clusterId = e.getClusterId();
                String partitionId = e.getPartitionId();
                AbstractMonitor monitor;

                if(AutoscalerContext.getInstance().moniterExist(clusterId)){

                    monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                } else {

                    //This is LB member
                    monitor = AutoscalerContext.getInstance().getLBMonitor(clusterId);
                }

                NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);

                PartitionContext partitionContext = networkPartitionContext.getPartitionCtxt(partitionId);
                partitionContext.removeMemberStatsContext(e.getMemberId());
                if(!partitionContext.removeTerminationPendingMember(e.getMemberId())){

                    if(log.isErrorEnabled()){
                        log.error(String.format("Member is not available in termination pending list: [member] %s", e.getMemberId()));
                    }
                } else if(log.isInfoEnabled()){
                    log.info(String.format("Member stat context has been removed: [member] %s", e.getMemberId()));
                }
//                partitionContext.decrementCurrentActiveMemberCount(1);


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
                String networkPartitionId = e.getNetworkPartitionId();

                PartitionContext partitionContext;
                String clusterId = e.getClusterId();
                AbstractMonitor monitor;

                if(AutoscalerContext.getInstance().moniterExist(clusterId)) {
                    monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                    partitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId).getPartitionCtxt(partitionId);
                } else {
                    monitor = AutoscalerContext.getInstance().getLBMonitor(clusterId);
                    partitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId).getPartitionCtxt(partitionId);
                }
                partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                if(log.isInfoEnabled()){
                    log.info(String.format("Member stat context has been added: [member] %s", memberId));
                }
//                partitionContext.incrementCurrentActiveMemberCount(1);
                partitionContext.movePendingMemberToActiveMembers(memberId);

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
//                            removeMonitor(cluster.getHostName());
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
    
    private class LBClusterMonitorAdder implements Runnable {
        private Cluster cluster;
        
        public LBClusterMonitorAdder(Cluster cluster) {
            this.cluster = cluster;
        }

        public void run() {
            LbClusterMonitor monitor;
            try {
                monitor = AutoscalerUtil.getLBClusterMonitor(cluster);

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
            AutoscalerContext.getInstance().addLbMonitor(monitor);
            if(log.isInfoEnabled()){
                log.info(String.format("LB Cluster monitor has been added: [cluster] %s",
                                                        cluster.getClusterId()));
            }
        }
    }
    
    private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;
        
        public ClusterMonitorAdder(Cluster cluster) {
            this.cluster = cluster;
        }

        public void run() {
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
            if(log.isInfoEnabled()){
                log.info(String.format("Cluster monitor has been added: [cluster] %s",
                                                        cluster.getClusterId()));
            }
        }
    }

    private void runTerminateAllRule(AbstractMonitor monitor){

        FactHandle terminateAllFactHandle = null;

        StatefulKnowledgeSession terminateAllKnowledgeSession = null;

        for(NetworkPartitionContext networkPartitionContext: monitor.getNetworkPartitionCtxts().values()) {
            terminateAllFactHandle = AutoscalerRuleEvaluator.evaluateTerminateAll(terminateAllKnowledgeSession
                    , terminateAllFactHandle, networkPartitionContext);
        }

    }

    /**
     * Terminate load balancer topology receiver thread.
     */
    public void terminate() {
        topologyReceiver.terminate();
        terminated = true;
    }
}
