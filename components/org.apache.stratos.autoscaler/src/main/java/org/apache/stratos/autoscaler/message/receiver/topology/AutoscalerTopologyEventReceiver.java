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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.KubernetesClusterContext;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.ClusterMonitorFactory;
import org.apache.stratos.autoscaler.monitor.ContainerClusterMonitor;
import org.apache.stratos.autoscaler.monitor.VMClusterMonitor;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.common.enums.ClusterType;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.ClusterCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterMaintenanceModeEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterRemovedEventListener;
import org.apache.stratos.messaging.listener.topology.CompleteTopologyEventListener;
import org.apache.stratos.messaging.listener.topology.MemberActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberMaintenanceListener;
import org.apache.stratos.messaging.listener.topology.MemberReadyToShutdownEventListener;
import org.apache.stratos.messaging.listener.topology.MemberStartedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.ServiceRemovedEventListener;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

/**
 * Autoscaler topology receiver.
 */
public class AutoscalerTopologyEventReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(AutoscalerTopologyEventReceiver.class);

    private TopologyEventReceiver topologyEventReceiver;
    private boolean terminated;

    public AutoscalerTopologyEventReceiver() {
        this.topologyEventReceiver = new TopologyEventReceiver();
        addEventListeners();
    }

    @Override
    public void run() {
        //FIXME this activated before autoscaler deployer activated.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ignore) {
        }
        Thread thread = new Thread(topologyEventReceiver);
        thread.start();
        if (log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread terminated");
        }
    }

    private void addEventListeners() {
        // Listen to topology events that affect clusters
        topologyEventReceiver.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {

                try {
                    TopologyManager.acquireReadLock();
                    for (Service service : TopologyManager.getTopology().getServices()) {
                        for (Cluster cluster : service.getClusters()) {
                            startClusterMonitor(cluster);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }


        });

        topologyEventReceiver.addEventListener(new MemberReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberReadyToShutdownEvent memberReadyToShutdownEvent = (MemberReadyToShutdownEvent)event;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    AbstractClusterMonitor monitor;
                    String clusterId = memberReadyToShutdownEvent.getClusterId();
                    String memberId = memberReadyToShutdownEvent.getMemberId();

                    if(asCtx.clusterMonitorExist(clusterId)) {
                        monitor = asCtx.getClusterMonitor(clusterId);
                    } else {
                        if(log.isDebugEnabled()){
                            log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                        }
                        return;
                    }
                    
                    TopologyManager.acquireReadLock();
                    
                    if(monitor.getClusterType() == ClusterType.VMServiceCluster 
                    		|| monitor.getClusterType() == ClusterType.VMLbCluster) {
                    	
                        NetworkPartitionContext nwPartitionCtxt;
                        nwPartitionCtxt = ((VMClusterMonitor) monitor).getNetworkPartitionCtxt(memberReadyToShutdownEvent.getNetworkPartitionId());

                        // start a new member in the same Partition
                        String partitionId = ((VMClusterMonitor) monitor).getPartitionOfMember(memberId);
                        PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);


                        // terminate the shutdown ready member
                        CloudControllerClient ccClient = CloudControllerClient.getInstance();
                        ccClient.terminate(memberId);

                        // remove from active member list
                        partitionCtxt.removeActiveMemberById(memberId);
                        
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member is terminated and removed from the active members list: [member] %s [partition] %s [cluster] %s ",
                                                   memberId, partitionId, clusterId));
                        }
                    } else if(monitor.getClusterType() == ClusterType.DockerServiceCluster) {
                    	// no need to do anything
                    }

                } catch (TerminationException e) {
                    log.error(e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }

        });

        topologyEventReceiver.addEventListener(new ClusterCreatedEventListener() {
                    @Override
                    protected void onEvent(Event event) {
                        try {
                            log.info("Event received: " + event);
                            ClusterCreatedEvent e = (ClusterCreatedEvent) event;
                            TopologyManager.acquireReadLock();
                            Service service = TopologyManager.getTopology().getService(e.getServiceName());
                            Cluster cluster = service.getCluster(e.getClusterId());
                            startClusterMonitor(cluster);
                        } catch (Exception e) {
                            log.error("Error processing event", e);
                        } finally {
                            TopologyManager.releaseReadLock();
                        }
                    }

                });

        topologyEventReceiver.addEventListener(new ClusterMaintenanceModeEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    log.info("Event received: " + event);
                    ClusterMaintenanceModeEvent e = (ClusterMaintenanceModeEvent) event;
                    TopologyManager.acquireReadLock();
                    Service service = TopologyManager.getTopology().getService(e.getServiceName());
                    Cluster cluster = service.getCluster(e.getClusterId());
                    if(AutoscalerContext.getInstance().clusterMonitorExist(cluster.getClusterId())) {
                    	AutoscalerContext.getInstance().getClusterMonitor(e.getClusterId()).setStatus(e.getStatus());
                    } else {
                        log.error("cluster monitor not exists for the cluster: " + cluster.toString());
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }

                });

        topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    ClusterRemovedEvent e = (ClusterRemovedEvent) event;
                    TopologyManager.acquireReadLock();

                    String clusterId = e.getClusterId();
                    String deploymentPolicy = e.getDeploymentPolicy();

                    AbstractClusterMonitor monitor = null;

                    if (e.isLbCluster()) {
                        DeploymentPolicy depPolicy = PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicy);
                        if (depPolicy != null) {
                            List<NetworkPartitionLbHolder> lbHolders = PartitionManager.getInstance()
                                    .getNetworkPartitionLbHolders(depPolicy);

                            for (NetworkPartitionLbHolder networkPartitionLbHolder : lbHolders) {
                                // removes lb cluster ids
                                boolean isRemoved = networkPartitionLbHolder.removeLbClusterId(clusterId);
                                if (isRemoved) {
                                    log.info("Removed the lb cluster [id]:"
                                            + clusterId
                                            + " reference from Network Partition [id]: "
                                            + networkPartitionLbHolder
                                            .getNetworkPartitionId());

                                }
                                if (log.isDebugEnabled()) {
                                    log.debug(networkPartitionLbHolder);
                                }

                            }
                        }
                    }
                    
                    monitor = AutoscalerContext.getInstance().removeClusterMonitor(clusterId);                               

                    // runTerminateAllRule(monitor);
                    if (monitor != null) {
                        monitor.destroy();
                        log.info(String.format("Cluster monitor has been removed successfully: [cluster] %s ",
                                clusterId));
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }

        });

        topologyEventReceiver.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {

            }

        });

        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                try {
                    TopologyManager.acquireReadLock();
                    MemberTerminatedEvent e = (MemberTerminatedEvent) event;
                    String networkPartitionId = e.getNetworkPartitionId();
                    String clusterId = e.getClusterId();
                    String partitionId = e.getPartitionId();
                    String memberId = e.getMemberId();
                    AbstractClusterMonitor monitor;
                    
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();

                    if(asCtx.clusterMonitorExist(clusterId)) {
                        monitor = asCtx.getClusterMonitor(clusterId);
                    } else {
                        if(log.isDebugEnabled()){
                            log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                        }
                        return;
                    }
                    
                    if(monitor.getClusterType() == ClusterType.VMServiceCluster 
                    		|| monitor.getClusterType() == ClusterType.VMLbCluster) {
                    	
                        NetworkPartitionContext networkPartitionContext = ((VMClusterMonitor) monitor).getNetworkPartitionCtxt(networkPartitionId);

                        PartitionContext partitionContext = networkPartitionContext.getPartitionCtxt(partitionId);
                        partitionContext.removeMemberStatsContext(memberId);

                        if (partitionContext.removeTerminationPendingMember(memberId)) {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Member is removed from termination pending members list: [member] %s", memberId));
                            }
                        } else if (partitionContext.removePendingMember(memberId)) {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Member is removed from pending members list: [member] %s", memberId));
                            }
                        } else if (partitionContext.removeActiveMemberById(memberId)) {
                            log.warn(String.format("Member is in the wrong list and it is removed from active members list", memberId));
                        } else if (partitionContext.removeObsoleteMember(memberId)){
                        	log.warn(String.format("Member's obsolated timeout has been expired and it is removed from obsolated members list", memberId));
                        } else {
                            log.warn(String.format("Member is not available in any of the list active, pending and termination pending", memberId));
                        }

                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member stat context has been removed successfully: [member] %s", memberId));
                        }
                    } else if(monitor.getClusterType() == ClusterType.DockerServiceCluster) {
                    	// no need to do anything
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }

        });

        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                try {
                    TopologyManager.acquireReadLock();

                    MemberActivatedEvent e = (MemberActivatedEvent) event;
                    String memberId = e.getMemberId();
                    String partitionId = e.getPartitionId();
                    String networkPartitionId = e.getNetworkPartitionId();

                    String clusterId = e.getClusterId();
                    AbstractClusterMonitor monitor;
                    
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    if(asCtx.clusterMonitorExist(clusterId)) {
                        monitor = asCtx.getClusterMonitor(clusterId);
                    } else {
                        if(log.isDebugEnabled()){
                            log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                        }
                        return;
                    }
                    
                    if (monitor.getClusterType() == ClusterType.VMServiceCluster) {    
                    	PartitionContext partitionContext;
                        partitionContext = ((VMClusterMonitor) monitor).getNetworkPartitionCtxt(networkPartitionId).getPartitionCtxt(partitionId);
                        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member stat context has been added successfully: [member] %s", memberId));
                        }
                        partitionContext.movePendingMemberToActiveMembers(memberId);
					} else if(monitor.getClusterType() == ClusterType.DockerServiceCluster) {
						KubernetesClusterContext kubernetesClusterContext;
						kubernetesClusterContext = ((ContainerClusterMonitor) monitor).getKubernetesClusterCtxt();
						kubernetesClusterContext.addMemberStatsContext(new MemberStatsContext(memberId));
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member stat context has been added successfully: [member] %s", memberId));
                        }
						kubernetesClusterContext.movePendingMemberToActiveMembers(memberId);
					}
                    
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberReadyToShutdownEventListener() {
           @Override
           protected void onEvent(Event event) {
               try {
            	   TopologyManager.acquireReadLock();
            	   
                   MemberReadyToShutdownEvent memberReadyToShutdownEvent = (MemberReadyToShutdownEvent)event;
                   AutoscalerContext asCtx = AutoscalerContext.getInstance();
                   AbstractClusterMonitor monitor;
                   String clusterId = memberReadyToShutdownEvent.getClusterId();
                   String memberId = memberReadyToShutdownEvent.getMemberId();

                   if(asCtx.clusterMonitorExist(clusterId)) {
                       monitor = asCtx.getClusterMonitor(clusterId);
                   } else {
                       if(log.isDebugEnabled()){
                           log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                       }
                       return;
                   }

                   if(monitor.getClusterType() == ClusterType.VMServiceCluster 
                		   || monitor.getClusterType() == ClusterType.VMLbCluster) {
                	   
                       NetworkPartitionContext nwPartitionCtxt;
                       nwPartitionCtxt = ((VMClusterMonitor) monitor).getNetworkPartitionCtxt(memberReadyToShutdownEvent.getNetworkPartitionId());

                       // start a new member in the same Partition
                       String partitionId = ((VMClusterMonitor) monitor).getPartitionOfMember(memberId);
                       PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);


                       // terminate the shutdown ready member
                       CloudControllerClient ccClient = CloudControllerClient.getInstance();
                       ccClient.terminate(memberId);

                       // remove from active member list
                       partitionCtxt.removeActiveMemberById(memberId);

                       if (log.isInfoEnabled()) {
                           log.info(String.format("Member is terminated and removed from the active members list: [member] %s [partition] %s [cluster] %s ",
                                                  memberId, partitionId, clusterId));
                       }
                   } else if(monitor.getClusterType() == ClusterType.DockerServiceCluster) {
                	   // no need to do anything
                   }

               } catch (TerminationException e) {
                   log.error(e);
               }
           }

       });


        topologyEventReceiver.addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {

                try {
                    TopologyManager.acquireReadLock();

                    MemberMaintenanceModeEvent e = (MemberMaintenanceModeEvent) event;
                    String memberId = e.getMemberId();
                    String partitionId = e.getPartitionId();
                    String networkPartitionId = e.getNetworkPartitionId();

                    String clusterId = e.getClusterId();
                    AbstractClusterMonitor monitor;
                    
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    if (asCtx.clusterMonitorExist(clusterId)) {
                        monitor = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
                    } else {
                        if(log.isDebugEnabled()){
                            log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                        }
                        return;
                    }
                    
                    if(monitor.getClusterType() == ClusterType.VMServiceCluster 
                 		   || monitor.getClusterType() == ClusterType.VMLbCluster) {
                    	
                    	PartitionContext partitionContext;
                    	partitionContext = ((VMClusterMonitor) monitor).getNetworkPartitionCtxt(networkPartitionId).getPartitionCtxt(partitionId);
                        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Member has been moved as pending termination: [member] %s", memberId));
                        }
                        partitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
                    } else if(monitor.getClusterType() == ClusterType.DockerServiceCluster) {
                    	// no need to do anything
                    }

                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });


        topologyEventReceiver.addEventListener(new ServiceRemovedEventListener() {
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
    }

    private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;
        private String clusterMonitorType;
        public ClusterMonitorAdder(Cluster cluster) {
            this.cluster = cluster;
        }

        public void run() {
            AbstractClusterMonitor monitor = null;
            int retries = 5;
            boolean success = false;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }

                try {
                    monitor = ClusterMonitorFactory.getMonitor(cluster);
                    success = true;
                    clusterMonitorType = monitor.getClusterType().name();
                } catch (PolicyValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
                    retries--;

                } catch (PartitionValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
                    retries--;
                }
            } while (!success && retries != 0);

            if (monitor == null) {
                String msg = "Cluster monitor creation failed, even after retrying for 5 times, "
                        + "for cluster: " + cluster.getClusterId();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            Thread th = new Thread(monitor);
            th.start();
            AutoscalerContext.getInstance().addClusterMonitor(monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("%s monitor has been added successfully: [cluster] %s",
                        clusterMonitorType, cluster.getClusterId()));
            }
        }
    }
 
    @SuppressWarnings("unused")
	private void runTerminateAllRule(VMClusterMonitor monitor) {

        FactHandle terminateAllFactHandle = null;

        StatefulKnowledgeSession terminateAllKnowledgeSession = null;

        for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
            terminateAllFactHandle = AutoscalerRuleEvaluator.evaluateTerminateAll(terminateAllKnowledgeSession
                    , terminateAllFactHandle, networkPartitionContext);
        }

    }

    /**
     * Terminate load balancer topology receiver thread.
     */
    public void terminate() {
        topologyEventReceiver.terminate();
        terminated = true;
    }

    protected synchronized void startClusterMonitor(Cluster cluster) {
        Thread th = null;
        if (!AutoscalerContext.getInstance().clusterMonitorExist(cluster.getClusterId())) {
        	th = new Thread(new ClusterMonitorAdder(cluster));
        } 
        if (th != null) {
            th.start();
            try {
                th.join();
            } catch (InterruptedException ignore) {
            }

            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Cluster monitor thread has been started successfully: [cluster] %s ",
                                cluster.getClusterId()));
            }
        }
    }
}
