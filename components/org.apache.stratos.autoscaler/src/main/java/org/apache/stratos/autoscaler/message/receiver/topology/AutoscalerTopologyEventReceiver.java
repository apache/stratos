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
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.ClusterMonitorFactory;
import org.apache.stratos.autoscaler.monitor.VMClusterMonitor;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.ClusterCreatedEventListener;
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
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberReadyToShutdownEvent memberReadyToShutdownEvent = (MemberReadyToShutdownEvent) event;
                    String clusterId = memberReadyToShutdownEvent.getClusterId();
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    AbstractClusterMonitor monitor;
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberReadyToShutdownEvent(memberReadyToShutdownEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });

        topologyEventReceiver.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    log.info("Event received: " + event);
                    ClusterCreatedEvent clusterCreatedEvent = (ClusterCreatedEvent) event;
                    TopologyManager.acquireReadLock();
                    Service service = TopologyManager.getTopology().getService(clusterCreatedEvent.getServiceName());
                    Cluster cluster = service.getCluster(clusterCreatedEvent.getClusterId());
                    startClusterMonitor(cluster);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;
                    String clusterId = clusterRemovedEvent.getClusterId();
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    AbstractClusterMonitor monitor;
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleClusterRemovedEvent(clusterRemovedEvent);
                    asCtx.removeClusterMonitor(clusterId);
                    monitor.destroy();
                    log.info(String.format("Cluster monitor has been removed successfully: [cluster] %s ",
                                           clusterId));
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
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
                    MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
                    String clusterId = memberTerminatedEvent.getClusterId();
                    AbstractClusterMonitor monitor;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberTerminatedEvent(memberTerminatedEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }

        });

        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;
                    String clusterId = memberActivatedEvent.getClusterId();
                    AbstractClusterMonitor monitor;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberActivatedEvent(memberActivatedEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberMaintenanceModeEvent maintenanceModeEvent = (MemberMaintenanceModeEvent) event;
                    String clusterId = maintenanceModeEvent.getClusterId();
                    AbstractClusterMonitor monitor;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberMaintenanceModeEvent(maintenanceModeEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });


        topologyEventReceiver.addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

            }
        });
    }

    private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;

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
                } catch (PolicyValidationException e) {
                    if (log.isDebugEnabled()) {
                        String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                        log.debug(msg, e);
                    }
                    retries--;
                } catch (PartitionValidationException e) {
                    if (log.isDebugEnabled()) {
                        String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                        log.debug(msg, e);
                    }
                    retries--;
                }
            } while (!success && retries != 0);

            if (monitor == null) {
                String msg = "Cluster monitor creation failed, even after retrying for 5 times, "
                             + "for cluster: " + cluster.getClusterId();
                log.error(msg);
                throw new RuntimeException(msg);
            }

//            Thread th = new Thread(monitor);
//            th.start();
            monitor.startScheduler();
            AutoscalerContext.getInstance().addClusterMonitor(monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster monitor has been added successfully: [cluster] %s",
                                       cluster.getClusterId()));
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

        AbstractClusterMonitor monitor;
        monitor = AutoscalerContext.getInstance().getClusterMonitor(cluster.getClusterId());

        if (null == monitor) {
            th = new Thread(new ClusterMonitorAdder(cluster));
        }
        if (th != null) {
            th.start();
            try {
                th.join();
            } catch (InterruptedException ignore) {
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster monitor thread has been started successfully: "
                                        + "[cluster] %s ", cluster.getClusterId()));
            }
        }
    }
}
