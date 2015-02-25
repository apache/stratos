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

package org.apache.stratos.autoscaler.event.receiver.topology;

import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterContextFactory;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.event.publisher.InstanceNotificationPublisher;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.events.ClusterStatusEvent;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ApplicationClustersCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterInstanceActivatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterInstanceCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterInstanceInactivateEvent;
import org.apache.stratos.messaging.event.topology.ClusterInstanceTerminatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterInstanceTerminatingEvent;
import org.apache.stratos.messaging.event.topology.ClusterResetEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.ApplicationClustersCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInstanceActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInstanceCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInstanceInactivateEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInstanceTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInstanceTerminatingEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterResetEventListener;
import org.apache.stratos.messaging.listener.topology.CompleteTopologyEventListener;
import org.apache.stratos.messaging.listener.topology.MemberActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberMaintenanceListener;
import org.apache.stratos.messaging.listener.topology.MemberReadyToShutdownEventListener;
import org.apache.stratos.messaging.listener.topology.MemberStartedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberTerminatedEventListener;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Autoscaler topology receiver.
 */
public class AutoscalerTopologyEventReceiver {

    private static final Log log = LogFactory.getLog(AutoscalerTopologyEventReceiver.class);

    private TopologyEventReceiver topologyEventReceiver;
    private boolean terminated;
    private boolean topologyInitialized;
    private ExecutorService executorService;

    public AutoscalerTopologyEventReceiver() {
        this.topologyEventReceiver = new TopologyEventReceiver();
        addEventListeners();
    }

    public void execute() {
        //FIXME this activated before autoscaler deployer activated.

        topologyEventReceiver.setExecutorService(getExecutorService());
        topologyEventReceiver.execute();

        if (log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread started");
        }

    }

    private void addEventListeners() {
        // Listen to topology events that affect clusters
        topologyEventReceiver.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (!topologyInitialized) {
                    log.info("[CompleteTopologyEvent] Received: " + event.getClass());
                    try {
                        ApplicationHolder.acquireReadLock();
                        Applications applications = ApplicationHolder.getApplications();
                        if (applications != null) {
                            for (Application application : applications.getApplications().values()) {
                                if (AutoscalerUtil.allClustersInitialized(application)) {
                                	ApplicationPolicy applicationPolicy = 
                                			PolicyManager.getInstance().getApplicationPolicy(application.getUniqueIdentifier());
                                    if (applicationPolicy != null) {
                                        AutoscalerUtil.getInstance().
                                                startApplicationMonitor(application.getUniqueIdentifier());
                                    } else {
                                        log.info("The relevant application policy is not yet " +
                                                "deployed for this [application] " +
                                                application.getUniqueIdentifier());
                                    }
                                } else {
                                    log.error("Complete Topology is not consistent with the applications " +
                                            "which got persisted");
                                }
                            }
                            topologyInitialized = true;
                        } else {
                            log.info("No applications found in the complete topology");
                        }
                    } catch (Exception e) {
                        log.error("Error processing event", e);
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                }
            }
        });


        topologyEventReceiver.addEventListener(new ApplicationClustersCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    log.info("[ApplicationClustersCreatedEvent] Received: " + event.getClass());
                    ApplicationClustersCreatedEvent applicationClustersCreatedEvent =
                            (ApplicationClustersCreatedEvent) event;
                    String appId = applicationClustersCreatedEvent.getAppId();
                    try {
                        //acquire read lock
                        ApplicationHolder.acquireReadLock();
                        //start the application monitor if the policy exists
                        ApplicationPolicy applicationPolicy = PolicyManager.getInstance().getApplicationPolicy(appId);
                        if (applicationPolicy != null && !AutoscalerContext.getInstance().containsApplicationPendingMonitor(appId)) {
                            AutoscalerUtil.getInstance().startApplicationMonitor(appId);
                        }
                    } catch (Exception e) {
                        String msg = "Error processing event " + e.getLocalizedMessage();
                        log.error(msg, e);
                    } finally {
                        //release read lock
                        ApplicationHolder.releaseReadLock();
                    }
                } catch (ClassCastException e) {
                    String msg = "Error while casting the event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterActivatedEvent] Received: " + event.getClass());
                ClusterInstanceActivatedEvent clusterActivatedEvent = (ClusterInstanceActivatedEvent) event;
                String clusterId = clusterActivatedEvent.getClusterId();
                String instanceId = clusterActivatedEvent.getInstanceId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                ClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                //changing the status in the monitor, will notify its parent monitor
                monitor.notifyParentMonitor(ClusterStatus.Active, instanceId);

            }
        });

        topologyEventReceiver.addEventListener(new ClusterResetEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterCreatedEvent] Received: " + event.getClass());
                ClusterResetEvent clusterResetEvent = (ClusterResetEvent) event;
                String clusterId = clusterResetEvent.getClusterId();
                String instanceId = clusterResetEvent.getInstanceId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                ClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                //changing the status in the monitor, will notify its parent monitor
                monitor.destroy();
                monitor.notifyParentMonitor(ClusterStatus.Created, instanceId);

            }
        });

        topologyEventReceiver.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterCreatedEvent] Received: " + event.getClass());
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterInactivateEvent] Received: " + event.getClass());
                ClusterInstanceInactivateEvent clusterInactivateEvent = (ClusterInstanceInactivateEvent) event;
                String clusterId = clusterInactivateEvent.getClusterId();
                String instanceId = clusterInactivateEvent.getInstanceId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                ClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                //changing the status in the monitor, will notify its parent monitor
                monitor.notifyParentMonitor(ClusterStatus.Inactive, instanceId);
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterTerminatingEvent] Received: " + event.getClass());
                ClusterInstanceTerminatingEvent clusterTerminatingEvent = (ClusterInstanceTerminatingEvent) event;
                String clusterId = clusterTerminatingEvent.getClusterId();
                String clusterInstanceId = clusterTerminatingEvent.getInstanceId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                ClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    // if monitor does not exist, send cluster terminated event
                    ClusterStatusEventPublisher.sendClusterTerminatedEvent(clusterTerminatingEvent.getAppId(),
                            clusterTerminatingEvent.getServiceName(), clusterId, clusterInstanceId);
                    return;
                }
                //changing the status in the monitor, will notify its parent monitor
                ClusterInstance clusterInstance = (ClusterInstance) monitor.getInstance(clusterInstanceId);
                if (clusterInstance.getPreviousState() == ClusterStatus.Active) {
                    // terminated gracefully
                    monitor.notifyParentMonitor(ClusterStatus.Terminating, clusterInstanceId);
                    InstanceNotificationPublisher.getInstance().
                            sendInstanceCleanupEventForCluster(clusterId, clusterInstanceId);
                } else {
                    monitor.notifyParentMonitor(ClusterStatus.Terminating, clusterInstanceId);
                    monitor.terminateAllMembers(clusterInstanceId, clusterInstance.getNetworkPartitionId());
                }
                ServiceReferenceHolder.getInstance().getClusterStatusProcessorChain().
                        process("", clusterId, clusterInstanceId);
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterTerminatedEvent] Received: " + event.getClass());
                ClusterInstanceTerminatedEvent clusterTerminatedEvent = (ClusterInstanceTerminatedEvent) event;
                String clusterId = clusterTerminatedEvent.getClusterId();
                String instanceId = clusterTerminatedEvent.getInstanceId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                ClusterMonitor monitor;
                ApplicationMonitor appMonitor = null;
                monitor = asCtx.getClusterMonitor(clusterId);
                appMonitor = AutoscalerContext.getInstance().
                        getAppMonitor(clusterTerminatedEvent.getAppId());
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    // if the cluster monitor is null, assume that its termianted
                    appMonitor = AutoscalerContext.getInstance().
                            getAppMonitor(clusterTerminatedEvent.getAppId());
                    if (appMonitor != null) {
                        appMonitor.onChildStatusEvent(
                                new ClusterStatusEvent(ClusterStatus.Terminated,
                                        clusterId, instanceId));
                    }
                    return;
                }
                //changing the status in the monitor, will notify its parent monitor
                monitor.notifyParentMonitor(ClusterStatus.Terminated, instanceId);
                //Removing the instance and instanceContext
                ClusterInstance instance = (ClusterInstance) monitor.getInstance(instanceId);
                ((ClusterContext)monitor.getClusterContext()).
                        getNetworkPartitionCtxt(instance.getNetworkPartitionId()).
                        removeInstanceContext(instanceId);
                monitor.removeInstance(instanceId);
                if (!monitor.hasInstance() && appMonitor.isTerminating()) {
                    //Destroying and Removing the Cluster monitor
                    monitor.destroy();
                    AutoscalerContext.getInstance().removeClusterMonitor(clusterId);
                }

            }
        });

        topologyEventReceiver.addEventListener(new MemberReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                        log.info("[MemberReadyToShutdownEvent] Received: " + event.getClass());
                    MemberReadyToShutdownEvent memberReadyToShutdownEvent = (MemberReadyToShutdownEvent) event;
                    String clusterId = memberReadyToShutdownEvent.getClusterId();
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    ClusterMonitor monitor;
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
                    ClusterMonitor monitor;
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
                    ClusterMonitor monitor;
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
                    ClusterMonitor monitor;
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

        topologyEventReceiver.addEventListener(new ClusterInstanceCreatedEventListener() {
           @Override
           protected void onEvent(Event event) {

               ClusterInstanceCreatedEvent clusterInstanceCreatedEvent =
                       (ClusterInstanceCreatedEvent) event;
               ClusterMonitor clusterMonitor = AutoscalerContext.getInstance().
                       getClusterMonitor(clusterInstanceCreatedEvent.getClusterId());
               ClusterInstance clusterInstance = ((ClusterInstanceCreatedEvent) event).
                                                    getClusterInstance();
               String instanceId = clusterInstance.getInstanceId();
               //FIXME to take lock when clusterMonitor is running
               if (clusterMonitor != null) {
                   TopologyManager.acquireReadLockForCluster(clusterInstanceCreatedEvent.getServiceName(),
                           clusterInstanceCreatedEvent.getClusterId());

                   try {
                       Service service = TopologyManager.getTopology().
                               getService(clusterInstanceCreatedEvent.getServiceName());

                       if (service != null) {
                           Cluster cluster = service.getCluster(clusterInstanceCreatedEvent.getClusterId());
                            if (cluster != null) {
                                try {
                                    ClusterContext clusterContext =
                                            (ClusterContext) clusterMonitor.getClusterContext();
                                    if (clusterContext == null) {
                                        clusterContext = ClusterContextFactory.getVMClusterContext(instanceId, cluster,
                                                clusterMonitor.hasScalingDependents());
                                        clusterMonitor.setClusterContext(clusterContext);

                                    }
                                    log.info(" Cluster monitor has scaling dependents"
                                    		+ "  ["+clusterMonitor.hasScalingDependents()+"] "); // TODO -- remove this log..
                                    clusterContext.addInstanceContext(instanceId, cluster,
                                            clusterMonitor.hasScalingDependents(), clusterMonitor.groupScalingEnabledSubtree());
                                    if (clusterMonitor.getInstance(instanceId) == null) {
                                        // adding the same instance in topology to monitor as a reference
                                        ClusterInstance clusterInstance1 = cluster.getInstanceContexts(instanceId);
                                        clusterMonitor.addInstance(clusterInstance1);
                                    }

                                    if (clusterMonitor.hasMonitoringStarted().compareAndSet(false, true)) {
                                        clusterMonitor.startScheduler();
                                        log.info("Monitoring task for Cluster Monitor with cluster id "
                                                + clusterInstanceCreatedEvent.getClusterId() + " started successfully");
                                    } else {
                                        //monitor already started. Invoking it directly to speed up the process
                                        ((ClusterMonitor)clusterMonitor).monitor();
                                    }
                                } catch (PolicyValidationException e) {
                                    log.error(e.getMessage(), e);
                                } catch (PartitionValidationException e) {
                                    log.error(e.getMessage(), e);
                                }
                            }

                       } else {
                           log.error("Service " + clusterInstanceCreatedEvent.getServiceName() +
                                   " not found, no cluster instance added to ClusterMonitor " +
                                   clusterInstanceCreatedEvent.getClusterId());
                       }

                   } finally {
                       TopologyManager.releaseReadLockForCluster(clusterInstanceCreatedEvent.getServiceName(),
                               clusterInstanceCreatedEvent.getClusterId());
                   }

               } else {
                   log.error("No Cluster Monitor found for cluster id " +
                           clusterInstanceCreatedEvent.getClusterId());
               }
           }
       }

        );
    }

    /**
     * Terminate load balancer topology receiver thread.
     */

    public void terminate() {
        topologyEventReceiver.terminate();
        terminated = true;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
