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
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.messaging.domain.topology.Application;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;

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
                    for (Application application : TopologyManager.getTopology().getApplications()) {
                        startApplicationMonitor(application);
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }


        });


        topologyEventReceiver.addEventListener(new ApplicationCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationCreatedEventListener] Received: " + event.getClass());

                ApplicationCreatedEvent applicationCreatedEvent = (ApplicationCreatedEvent) event;

                //acquire read lock
                TopologyManager.acquireReadLock();

                try {
                    //TODO build dependency and organize the application

                    //start the application monitor
                   // startApplicationMonitor(applicationCreatedEvent.getApplication());

                } finally {
                    //release read lock
                    TopologyManager.releaseReadLock();
                }

            }
        });

        topologyEventReceiver.addEventListener(new GroupActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[GroupActivatedEvent] Received: " + event.getClass());
                GroupActivatedEvent groupActivatedEvent = (GroupActivatedEvent) event;

                //trigger status checker
                //StatusChecker.getInstance().onGroupStatusChange(groupActivatedEvent.getGroupId(),
                                                                //groupActivatedEvent.getAppId());

                }
        });

        topologyEventReceiver.addEventListener(new ClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterActivatedEvent] Received: " + event.getClass());
                ClusterActivatedEvent clusterActivatedEvent = (ClusterActivatedEvent) event;

                //trigger status checker
                /*StatusChecker.getInstance().onClusterStatusChange(clusterActivatedEvent.getClusterId(),
                                                                clusterActivatedEvent.getAppId());
*/
                }
        });

        topologyEventReceiver.addEventListener(new ApplicationRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationCreatedEventListener] Received: " + event.getClass());

                ApplicationRemovedEvent applicationCreatedEvent = (ApplicationRemovedEvent) event;

                //acquire read lock
                TopologyManager.acquireReadLock();

                try {
                    //TODO build dependency and organize the application

                    //remove monitors by checking the termination

                } finally {
                    //release read lock
                    TopologyManager.releaseReadLock();
                }

            }
        });

        topologyEventReceiver.addEventListener(new MemberReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberReadyToShutdownEvent memberReadyToShutdownEvent =
                                                                (MemberReadyToShutdownEvent) event;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    AbstractClusterMonitor monitor;
                    String clusterId = memberReadyToShutdownEvent.getClusterId();
                    String memberId = memberReadyToShutdownEvent.getMemberId();

                    if (asCtx.monitorExist(clusterId)) {
                        monitor = asCtx.getMonitor(clusterId);
                    } else if (asCtx.lbMonitorExist(clusterId)) {
                        monitor = asCtx.getLBMonitor(clusterId);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found " +
                                    "in autoscaler context [cluster] %s", clusterId));
                        }
                        return;
                    }

                    NetworkPartitionContext nwPartitionCtxt;
                    nwPartitionCtxt = monitor.getNetworkPartitionCtxt(
                                            memberReadyToShutdownEvent.getNetworkPartitionId());

                    // start a new member in the same Partition
                    String partitionId = monitor.getPartitionOfMember(memberId);
                    PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);


                    // terminate the shutdown ready member
                    CloudControllerClient ccClient = CloudControllerClient.getInstance();
                    ccClient.terminate(memberId);

                    // remove from active member list
                    partitionCtxt.removeActiveMemberById(memberId);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Member is terminated and removed from the active " +
                                        "members list: [member] %s [partition] %s [cluster] %s ",
                                memberId, partitionId, clusterId));
                    }
                } catch (TerminationException e) {
                    log.error(e);
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
                    if (AutoscalerContext.getInstance().monitorExist((cluster.getClusterId()))) {
                        AutoscalerContext.getInstance().getMonitor(e.getClusterId()).
                                                        setStatus(e.getStatus());
                    } else if (AutoscalerContext.getInstance().
                                        lbMonitorExist((cluster.getClusterId()))) {
                        AutoscalerContext.getInstance().getLBMonitor(e.getClusterId()).
                                                        setStatus(e.getStatus());
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

                    AbstractClusterMonitor monitor;

                    if (e.isLbCluster()) {
                        DeploymentPolicy depPolicy = PolicyManager.getInstance().
                                                        getDeploymentPolicy(deploymentPolicy);
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
                        monitor = AutoscalerContext.getInstance()
                                .removeLbMonitor(clusterId);

                    } else {
                        monitor = AutoscalerContext.getInstance()
                                .removeMonitor(clusterId);
                    }

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
                    AbstractClusterMonitor monitor;

                    if (AutoscalerContext.getInstance().monitorExist(clusterId)) {
                        monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                    } else {
                        //This is LB member
                        monitor = AutoscalerContext.getInstance().getLBMonitor(clusterId);
                    }

                    NetworkPartitionContext networkPartitionContext = monitor.
                                                        getNetworkPartitionCtxt(networkPartitionId);

                    PartitionContext partitionContext = networkPartitionContext.
                                                                    getPartitionCtxt(partitionId);
                    String memberId = e.getMemberId();
                    partitionContext.removeMemberStatsContext(memberId);

                    if (partitionContext.removeTerminationPendingMember(memberId)) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Member is removed from termination pending " +
                                    "members list: [member] %s", memberId));
                        }
                    } else if (partitionContext.removePendingMember(memberId)) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Member is removed from pending members list: " +
                                    "[member] %s", memberId));
                        }
                    } else if (partitionContext.removeActiveMemberById(memberId)) {
                        log.warn(String.format("Member is in the wrong list and it is removed " +
                                "from active members list", memberId));
                    } else {
                        log.warn(String.format("Member is not available in any of the list " +
                                "active, pending and termination pending", memberId));
                    }

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Member stat context has been removed " +
                                "               successfully: [member] %s", memberId));
                    }
//                partitionContext.decrementCurrentActiveMemberCount(1);


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

                    PartitionContext partitionContext = null;

                    partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                    // TODO starting the pending clusters which are waiting for this member activation in a cluster


                    if (log.isInfoEnabled()) {
                        log.info(String.format("Member stat context has been added " +
                                                "successfully: [member] %s", memberId));
                    }
//                partitionContext.incrementCurrentActiveMemberCount(1);
                    partitionContext.movePendingMemberToActiveMembers(memberId);

                    //trigger status checker
                    StatusChecker.getInstance().onMemberStatusChange(e.getClusterId());

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
                    MemberReadyToShutdownEvent memberReadyToShutdownEvent =
                                                                (MemberReadyToShutdownEvent) event;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    AbstractClusterMonitor monitor;
                    String clusterId = memberReadyToShutdownEvent.getClusterId();
                    String memberId = memberReadyToShutdownEvent.getMemberId();

                    if (asCtx.monitorExist(clusterId)) {
                        monitor = asCtx.getMonitor(clusterId);
                    } else if (asCtx.lbMonitorExist(clusterId)) {
                        monitor = asCtx.getLBMonitor(clusterId);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler " +
                                    "context [cluster] %s", clusterId));
                        }
                        return;
                    }

                    NetworkPartitionContext nwPartitionCtxt;
                    nwPartitionCtxt = monitor.getNetworkPartitionCtxt(memberReadyToShutdownEvent.
                                                                            getNetworkPartitionId());

                    // start a new member in the same Partition
                    String partitionId = monitor.getPartitionOfMember(memberId);
                    PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);


                    // terminate the shutdown ready member
                    CloudControllerClient ccClient = CloudControllerClient.getInstance();
                    ccClient.terminate(memberId);

                    // remove from active member list
                    partitionCtxt.removeActiveMemberById(memberId);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Member is terminated and removed from the active " +
                                        "members list: [member] %s [partition] %s [cluster] %s ",
                                memberId, partitionId, clusterId));
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

                    PartitionContext partitionContext;
                    String clusterId = e.getClusterId();
                    AbstractClusterMonitor monitor;

                    if (AutoscalerContext.getInstance().monitorExist(clusterId)) {
                        monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                        partitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId).
                                                                    getPartitionCtxt(partitionId);
                    } else {
                        monitor = AutoscalerContext.getInstance().getLBMonitor(clusterId);
                        partitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId).
                                                                    getPartitionCtxt(partitionId);
                    }
                    partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member has been moved as pending termination: " +
                                                "[member] %s", memberId));
                    }
                    partitionContext.moveActiveMemberToTerminationPendingMembers(memberId);

                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });
    }


    /**
     * Terminate load balancer topology receiver thread.
     */
    public void terminate() {
        topologyEventReceiver.terminate();
        terminated = true;
    }

    protected synchronized void startApplicationMonitor(Application application) {
        Thread th = null;
        if (!AutoscalerContext.getInstance()
                .appMonitorExist(application.getId())) {
            th = new Thread(
                    new ApplicationMonitorAdder(application));
        }
        if (th != null) {
            th.start();
            try {
                th.join();
            } catch (InterruptedException ignore) {
            }

            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Application monitor thread has been started successfully: " +
                                        "[application] %s ", application.getId()));
            }
        }
    }

    private class ApplicationMonitorAdder implements Runnable {
        private Application application;

        public ApplicationMonitorAdder(Application application) {
            this.application = application;
        }

        public void run() {
            ApplicationMonitor applicationMonitor = null;
            int retries = 5;
            boolean success = false;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }

                try {
                    applicationMonitor = AutoscalerUtil.getApplicationMonitor(application);
                    success = true;
                    //TODO exception handling
                } catch (Exception e) {
                    String msg = "Application monitor creation failed for Application: " +
                                    application.getId();
                    log.debug(msg, e);
                    retries--;

                }
            } while (!success && retries != 0);

            if (applicationMonitor == null) {
                String msg = "Application monitor creation failed, even after retrying for 5 times, "
                        + "for Application: " + applicationMonitor.getId();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            Thread th = new Thread(applicationMonitor);
            th.start();

            if (log.isInfoEnabled()) {
                log.info(String.format("Application monitor has been added successfully: " +
                                "[application] %s", applicationMonitor.getId()));
            }
        }
    }


}
