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
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.ApplicationMonitorFactory;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.topology.*;
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
    private boolean topologyInitialized;

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

                if (!topologyInitialized) {
                    TopologyManager.acquireReadLock();
                    try {
                        for (Application application : TopologyManager.getTopology().getApplications()) {
                            startApplicationMonitor(application.getUniqueIdentifier());
                        }

                        topologyInitialized = true;
                    } catch (Exception e) {
                        log.error("Error processing event", e);
                    } finally {
                        TopologyManager.releaseReadLock();
                    }
                }
            }
        });


        topologyEventReceiver.addEventListener(new ApplicationCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    log.info("[ApplicationCreatedEvent] Received: " + event.getClass());
                    ApplicationCreatedEvent applicationCreatedEvent = (ApplicationCreatedEvent) event;
                    try {

                        //acquire read lock
                        TopologyManager.acquireReadLockForApplication(
                                applicationCreatedEvent.getApplication().getUniqueIdentifier());
                        //start the application monitor
                        startApplicationMonitor(applicationCreatedEvent.getApplication().getUniqueIdentifier());
                    } catch (Exception e) {
                        String msg = "Error processing event " + e.getLocalizedMessage();
                        log.error(msg, e);
                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLockForApplication(
                                applicationCreatedEvent.getApplication().getUniqueIdentifier());
                    }
                } catch (ClassCastException e) {
                    String msg = "Error while casting the event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterActivatedEvent] Received: " + event.getClass());

                ClusterActivatedEvent clusterActivatedEvent = (ClusterActivatedEvent) event;
                String appId = clusterActivatedEvent.getAppId();
                String clusterId = clusterActivatedEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        (AbstractClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                clusterMonitor.setStatus(ClusterStatus.Active);

                //starting the status checker to decide on the status of it's parent
                //StatusChecker.getInstance().onClusterStatusChange(clusterId, appId);
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInActivateEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterActivatedEvent] Received: " + event.getClass());

                ClusterInActivateEvent clusterInActivateEvent = (ClusterInActivateEvent) event;
                String appId = clusterInActivateEvent.getAppId();
                String clusterId = clusterInActivateEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        (AbstractClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                clusterMonitor.setStatus(ClusterStatus.Inactive);

                //starting the status checker to decide on the status of it's parent
                //StatusChecker.getInstance().onClusterStatusChange(clusterId, appId);
            }
        });

        topologyEventReceiver.addEventListener(new GroupActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[GroupActivatedEvent] Received: " + event.getClass());

                GroupActivatedEvent groupActivatedEvent = (GroupActivatedEvent) event;
                String appId = groupActivatedEvent.getAppId();
                String groupId = groupActivatedEvent.getGroupId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                GroupMonitor monitor = (GroupMonitor) appMonitor.findGroupMonitorWithId(groupId);

                //changing the status in the monitor, will notify its parent monitor
                monitor.setStatus(GroupStatus.Active);

                //starting the status checker to decide on the status of it's parent
                //StatusChecker.getInstance().onGroupStatusChange(groupId, appId);
            }
        });

        topologyEventReceiver.addEventListener(new GroupInActivateEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[GroupInActivateEvent] Received: " + event.getClass());

                GroupInActivateEvent groupInActivateEvent = (GroupInActivateEvent) event;
                String appId = groupInActivateEvent.getAppId();
                String groupId = groupInActivateEvent.getGroupId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                GroupMonitor monitor = (GroupMonitor) appMonitor.findGroupMonitorWithId(groupId);

                //changing the status in the monitor, will notify its parent monitor
                monitor.setStatus(GroupStatus.Inactive);

                //starting the status checker to decide on the status of it's parent
                //StatusChecker.getInstance().onGroupStatusChange(groupId, appId);
            }
        });

        topologyEventReceiver.addEventListener(new ApplicationActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationActivatedEvent] Received: " + event.getClass());

                ApplicationActivatedEvent applicationActivatedEvent = (ApplicationActivatedEvent) event;
                String appId = applicationActivatedEvent.getAppId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                appMonitor.setStatus(ApplicationStatus.Active);
            }
        });


        topologyEventReceiver.addEventListener(new ApplicationRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationRemovedEvent] Received: " + event.getClass());

                ApplicationRemovedEvent applicationRemovedEvent = (ApplicationRemovedEvent) event;

                //acquire read lock
                TopologyManager.acquireReadLockForApplication(applicationRemovedEvent.getApplicationId());

                try {
                    //TODO remove monitors as well as any starting or pending threads
                    ApplicationMonitor monitor = AutoscalerContext.getInstance().
                            getAppMonitor(applicationRemovedEvent.getApplicationId());
                    if (monitor != null) {
                        List<String> clusters = monitor.
                                findClustersOfApplication(applicationRemovedEvent.getApplicationId());
                        for (String clusterId : clusters) {
                            //stopping the cluster monitor and remove it from the AS
                            ((ClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId)).setDestroyed(true);
                            AutoscalerContext.getInstance().removeMonitor(clusterId);
                        }
                        //removing the application monitor
                        AutoscalerContext.getInstance().
                                removeAppMonitor(applicationRemovedEvent.getApplicationId());
                    } else {
                        log.warn("Application Monitor cannot be found for the removed [application] "
                                + applicationRemovedEvent.getApplicationId());
                    }


                } finally {
                    //release read lock
                    TopologyManager.releaseReadLockForApplication(applicationRemovedEvent.getApplicationId());
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
                        monitor = (AbstractClusterMonitor) asCtx.getMonitor(clusterId);
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

                ClusterMaintenanceModeEvent clusterMaitenanceEvent = null;

                try {
                    log.info("Event received: " + event);
                    clusterMaitenanceEvent = (ClusterMaintenanceModeEvent) event;
                    //TopologyManager.acquireReadLock();
                    TopologyManager.acquireReadLockForCluster(clusterMaitenanceEvent.getServiceName(),
                            clusterMaitenanceEvent.getClusterId());

                    Service service = TopologyManager.getTopology().getService(clusterMaitenanceEvent.getServiceName());
                    Cluster cluster = service.getCluster(clusterMaitenanceEvent.getClusterId());
                    AbstractClusterMonitor monitor;
                    if (AutoscalerContext.getInstance().monitorExist((cluster.getClusterId()))) {
                        monitor = (AbstractClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterMaitenanceEvent.getClusterId());
                        monitor.setStatus(ClusterStatus.Inactive);
                    } else {
                        log.error("cluster monitor not exists for the cluster: " + cluster.toString());
                    }
                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(clusterMaitenanceEvent.getServiceName(),
                            clusterMaitenanceEvent.getClusterId());
                }
            }

        });


        topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                ClusterRemovedEvent clusterRemovedEvent = null;
                try {
                    clusterRemovedEvent = (ClusterRemovedEvent) event;
                    //TopologyManager.acquireReadLock();
                    TopologyManager.acquireReadLockForCluster(clusterRemovedEvent.getServiceName(),
                            clusterRemovedEvent.getClusterId());

                    String clusterId = clusterRemovedEvent.getClusterId();
                    String deploymentPolicy = clusterRemovedEvent.getDeploymentPolicy();

                    AbstractClusterMonitor monitor;

                    if (clusterRemovedEvent.isLbCluster()) {
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
                        monitor = (AbstractClusterMonitor) AutoscalerContext.getInstance()
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
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(clusterRemovedEvent.getServiceName(),
                            clusterRemovedEvent.getClusterId());
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

                MemberTerminatedEvent memberTerminatedEvent = null;
                try {
                    //TopologyManager.acquireReadLock();

                    memberTerminatedEvent = (MemberTerminatedEvent) event;
                    String networkPartitionId = memberTerminatedEvent.getNetworkPartitionId();
                    String clusterId = memberTerminatedEvent.getClusterId();
                    String partitionId = memberTerminatedEvent.getPartitionId();
                    AbstractClusterMonitor monitor;

                    TopologyManager.acquireReadLockForCluster(memberTerminatedEvent.getServiceName(),
                            memberTerminatedEvent.getClusterId());

                    if (AutoscalerContext.getInstance().monitorExist(clusterId)) {
                        monitor = (AbstractClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);
                    } else {
                        //This is LB member
                        monitor = AutoscalerContext.getInstance().getLBMonitor(clusterId);
                    }

                    NetworkPartitionContext networkPartitionContext = monitor.
                            getNetworkPartitionCtxt(networkPartitionId);

                    PartitionContext partitionContext = networkPartitionContext.
                            getPartitionCtxt(partitionId);
                    String memberId = memberTerminatedEvent.getMemberId();
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
                    //Checking whether the cluster state can be changed either from in_active to created/terminating to terminated
                    StatusChecker.getInstance().onMemberTermination(clusterId);

//                partitionContext.decrementCurrentActiveMemberCount(1);


                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberTerminatedEvent.getServiceName(),
                            memberTerminatedEvent.getClusterId());
                }
            }

        });

        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;

                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberActivatedEvent.getServiceName(),
                        memberActivatedEvent.getClusterId());

                try {

                    String networkPartitionId = memberActivatedEvent.getNetworkPartitionId();
                    String clusterId = memberActivatedEvent.getClusterId();
                    String partitionId = memberActivatedEvent.getPartitionId();
                    String memberId = memberActivatedEvent.getMemberId();

                    AbstractClusterMonitor monitor;

                    if (AutoscalerContext.getInstance().monitorExist(clusterId)) {
                        monitor = (AbstractClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);
                    } else {
                        //This is LB member
                        monitor = AutoscalerContext.getInstance().getLBMonitor(clusterId);
                    }

                    NetworkPartitionContext networkPartitionContext = monitor.
                            getNetworkPartitionCtxt(networkPartitionId);

                    PartitionContext partitionContext = networkPartitionContext.
                            getPartitionCtxt(partitionId);

                    partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                    // TODO starting the pending clusters which are waiting for this member activation in a cluster


                    if (log.isInfoEnabled()) {
                        log.info(String.format("Member stat context has been added " +
                                "successfully: [member] %s", memberId));
                    }
//                partitionContext.incrementCurrentActiveMemberCount(1);
                    partitionContext.movePendingMemberToActiveMembers(memberId);
                    //triggering the status checker
                    StatusChecker.getInstance().onMemberStatusChange(memberActivatedEvent.getClusterId());

                } catch (Exception e) {
                    log.error("Error processing event", e);
                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId());
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {

                MemberMaintenanceModeEvent memberMaintenanceModeEvent = (MemberMaintenanceModeEvent) event;

                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberMaintenanceModeEvent.getServiceName(),
                        memberMaintenanceModeEvent.getClusterId());

                try {

                    String memberId = memberMaintenanceModeEvent.getMemberId();
                    String partitionId = memberMaintenanceModeEvent.getPartitionId();
                    String networkPartitionId = memberMaintenanceModeEvent.getNetworkPartitionId();

                    PartitionContext partitionContext;
                    String clusterId = memberMaintenanceModeEvent.getClusterId();
                    AbstractClusterMonitor monitor;

                    if (AutoscalerContext.getInstance().monitorExist(clusterId)) {
                        monitor = (AbstractClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterId);
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
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberMaintenanceModeEvent.getServiceName(),
                            memberMaintenanceModeEvent.getClusterId());
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

    protected synchronized void startApplicationMonitor(String applicationId) {
        Thread th = null;
        if (!AutoscalerContext.getInstance()
                .appMonitorExist(applicationId)) {
            th = new Thread(
                    new ApplicationMonitorAdder(applicationId));
        }

        if (th != null) {
            th.start();
            //    try {
            //        th.join();
            //    } catch (InterruptedException ignore) {

            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Application monitor thread has been started successfully: " +
                                "[application] %s ", applicationId));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Application monitor thread already exists: " +
                                "[application] %s ", applicationId));
            }
        }
    }

    private class ApplicationMonitorAdder implements Runnable {
        private String appId;

        public ApplicationMonitorAdder(String appId) {
            this.appId = appId;
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
                    long start = System.currentTimeMillis();
                    if (log.isDebugEnabled()) {
                        log.debug("application monitor is going to be started for [application] " +
                                appId);
                    }
                    applicationMonitor = ApplicationMonitorFactory.getApplicationMonitor(appId);

                    long end = System.currentTimeMillis();
                    log.info("Time taken to start app monitor: " + (end - start) / 1000);
                    success = true;
                } catch (DependencyBuilderException e) {
                    String msg = "Application monitor creation failed for Application: ";
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Application monitor creation failed for Application: ";
                    log.warn(msg, e);
                    retries--;
                }
            } while (!success && retries != 0);

            if (applicationMonitor == null) {
                String msg = "Application monitor creation failed, even after retrying for 5 times, "
                        + "for Application: " + appId;
                log.error(msg);
                throw new RuntimeException(msg);
            }

            AutoscalerContext.getInstance().addAppMonitor(applicationMonitor);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application monitor has been added successfully: " +
                        "[application] %s", applicationMonitor.getId()));
            }
        }
    }


}
