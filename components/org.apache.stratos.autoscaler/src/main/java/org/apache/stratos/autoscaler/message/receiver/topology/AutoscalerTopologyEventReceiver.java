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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.topic.InstanceNotificationPublisher;
import org.apache.stratos.autoscaler.grouping.topic.StatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.messaging.domain.topology.Application;
import org.apache.stratos.messaging.domain.topology.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.GroupStatus;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ApplicationActivatedEvent;
import org.apache.stratos.messaging.event.topology.ApplicationCreatedEvent;
import org.apache.stratos.messaging.event.topology.ApplicationTerminatedEvent;
import org.apache.stratos.messaging.event.topology.ApplicationTerminatingEvent;
import org.apache.stratos.messaging.event.topology.ApplicationUndeployedEvent;
import org.apache.stratos.messaging.event.topology.ClusterActivatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterInactivateEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.ClusterTerminatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterTerminatingEvent;
import org.apache.stratos.messaging.event.topology.GroupActivatedEvent;
import org.apache.stratos.messaging.event.topology.GroupInactivateEvent;
import org.apache.stratos.messaging.event.topology.GroupTerminatedEvent;
import org.apache.stratos.messaging.event.topology.GroupTerminatingEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.ApplicationActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.ApplicationCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ApplicationTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.ApplicationTerminatingEventListener;
import org.apache.stratos.messaging.listener.topology.ApplicationUndeployedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInActivateEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterRemovedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterTerminatingEventListener;
import org.apache.stratos.messaging.listener.topology.CompleteTopologyEventListener;
import org.apache.stratos.messaging.listener.topology.GroupActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.GroupInActivateEventListener;
import org.apache.stratos.messaging.listener.topology.GroupTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.GroupTerminatingEventListener;
import org.apache.stratos.messaging.listener.topology.MemberActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberMaintenanceListener;
import org.apache.stratos.messaging.listener.topology.MemberReadyToShutdownEventListener;
import org.apache.stratos.messaging.listener.topology.MemberTerminatedEventListener;
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
                    log.info("[CompleteTopologyEvent] Received: " + event.getClass());

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
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if(clusterMonitor!= null) {
                    clusterMonitor.setStatus(ClusterStatus.Active);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterCreatedEvent] Received: " + event.getClass());

                ClusterCreatedEvent clusterCreatedEvent = (ClusterCreatedEvent) event;
                String clusterId = clusterCreatedEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                clusterMonitor.setStatus(ClusterStatus.Created);

            }
        });

        topologyEventReceiver.addEventListener(new ClusterInActivateEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterInActivateEvent] Received: " + event.getClass());

                ClusterInactivateEvent clusterInactivateEvent = (ClusterInactivateEvent) event;
                String appId = clusterInactivateEvent.getAppId();
                String clusterId = clusterInactivateEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if(clusterMonitor!= null) {
                    clusterMonitor.setStatus(ClusterStatus.Inactive);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterTerminatingEvent] Received: " + event.getClass());

                ClusterTerminatingEvent clusterTerminatingEvent = (ClusterTerminatingEvent) event;
                String clusterId = clusterTerminatingEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if (clusterMonitor != null) {
                    if (clusterMonitor.getStatus() == ClusterStatus.Active) {
                        // terminated gracefully
                        clusterMonitor.setStatus(ClusterStatus.Terminating);
                        InstanceNotificationPublisher.sendInstanceCleanupEventForCluster(clusterId);
                    } else {
                        clusterMonitor.setStatus(ClusterStatus.Terminating);
                        clusterMonitor.terminateAllMembers();
                    }

                } else {
                    log.warn("No Cluster Monitor found for cluster id " + clusterId);
                }
            }
        });

        topologyEventReceiver.addEventListener(new ClusterTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterTerminatedEvent] Received: " + event.getClass());

                ClusterTerminatedEvent clusterTerminatedEvent = (ClusterTerminatedEvent) event;
                String appId = clusterTerminatedEvent.getAppId();
                String clusterId = clusterTerminatedEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if (clusterMonitor != null) {
                    clusterMonitor.setStatus(ClusterStatus.Terminated);
                }
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
                if (monitor != null) {
                    monitor.setStatus(GroupStatus.Active);
                }
            }
        });

        topologyEventReceiver.addEventListener(new GroupInActivateEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[GroupInActivateEvent] Received: " + event.getClass());

                GroupInactivateEvent groupInactivateEvent = (GroupInactivateEvent) event;
                String appId = groupInactivateEvent.getAppId();
                String groupId = groupInactivateEvent.getGroupId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                GroupMonitor monitor = (GroupMonitor) appMonitor.findGroupMonitorWithId(groupId);

                //changing the status in the monitor, will notify its parent monitor
                if (monitor != null) {
                    monitor.setStatus(GroupStatus.Inactive);
                }

            }
        });

        topologyEventReceiver.addEventListener(new GroupTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[GroupTerminatingEvent] Received: " + event.getClass());

                GroupTerminatingEvent groupTerminatingEvent = (GroupTerminatingEvent) event;
                String appId = groupTerminatingEvent.getAppId();
                String groupId = groupTerminatingEvent.getGroupId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                GroupMonitor monitor = (GroupMonitor) appMonitor.findGroupMonitorWithId(groupId);

                //changing the status in the monitor, will notify its parent monitor
                if (monitor != null) {
                    monitor.setStatus(GroupStatus.Terminating);
                }

            }
        });

        topologyEventReceiver.addEventListener(new GroupTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[GroupTerminatedEvent] Received: " + event.getClass());

                GroupTerminatedEvent groupTerminatedEvent = (GroupTerminatedEvent) event;
                String appId = groupTerminatedEvent.getAppId();
                String groupId = groupTerminatedEvent.getGroupId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                GroupMonitor monitor = (GroupMonitor) appMonitor.findGroupMonitorWithId(groupId);

                //changing the status in the monitor, will notify its parent monitor
                if (monitor != null) {
                    monitor.setStatus(GroupStatus.Terminated);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ApplicationActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationActivatedEvent] Received: " + event.getClass());

                ApplicationActivatedEvent applicationActivatedEvent = (ApplicationActivatedEvent) event;
                String appId = applicationActivatedEvent.getAppId();

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
                if(appMonitor != null) {
                    appMonitor.setStatus(ApplicationStatus.Active);
                }
            }
        });

        topologyEventReceiver.addEventListener(new ApplicationUndeployedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationUndeployedEvent] Received: " + event.getClass());

                ApplicationUndeployedEvent applicationUndeployedEvent = (ApplicationUndeployedEvent) event;

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().
                        getAppMonitor(applicationUndeployedEvent.getApplicationId());

                // if any of Cluster Monitors are not added yet, should send the
                // Cluster Terminated event for those clusters
                Set<ClusterDataHolder> clusterDataHolders = applicationUndeployedEvent.getClusterData();
                if (clusterDataHolders != null) {
                    for (ClusterDataHolder clusterDataHolder : clusterDataHolders) {
                        AbstractClusterMonitor clusterMonitor =
                                AutoscalerContext.getInstance().getClusterMonitor(clusterDataHolder.getClusterId());
                        if (clusterMonitor == null) {
                            // Cluster Monitor not found; send Cluster Terminated event to cleanup
                            StatusEventPublisher.sendClusterTerminatedEvent(
                                    applicationUndeployedEvent.getApplicationId(),
                                    clusterDataHolder.getServiceType(),
                                    clusterDataHolder.getClusterId());
                        } else {
                            // if the Cluster Monitor exists, mark it as destroyed to stop it from spawning
                            // more instances
                            clusterMonitor.setDestroyed(true);
                        }
                    }
                }

                if (appMonitor != null) {
                    // set Application Monitor state to 'Terminating'
                    appMonitor.setStatus(ApplicationStatus.Terminating);

                } else {
                    // ApplicationMonitor is not found, send Terminating event to clean up
                    StatusEventPublisher.sendApplicationTerminatedEvent(
                            applicationUndeployedEvent.getApplicationId(), applicationUndeployedEvent.getClusterData());
                }
            }
        });


        topologyEventReceiver.addEventListener(new ApplicationTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationTerminatingEvent] Received: " + event.getClass());

                ApplicationTerminatingEvent appTerminatingEvent = (ApplicationTerminatingEvent) event;

                // acquire read locks for application and relevant clusters
                TopologyManager.acquireReadLockForApplication(appTerminatingEvent.getAppId());

                try {
                    ApplicationMonitor appMonitor = AutoscalerContext.getInstance().
                            getAppMonitor(appTerminatingEvent.getAppId());

                    if (appMonitor != null) {
                        // update the status as Terminating
                        appMonitor.setStatus(ApplicationStatus.Terminating);

                    } else {
                        log.warn("Application Monitor cannot be found for the undeployed [application] "
                                + appTerminatingEvent.getAppId());
                    }

                } finally {
                    TopologyManager.
                            releaseReadLockForApplication(appTerminatingEvent.getAppId());
                }
            }
        });

        topologyEventReceiver.addEventListener(new ApplicationTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationTerminatedEvent] Received: " + event.getClass());

                ApplicationTerminatedEvent applicationRemovedEvent = (ApplicationTerminatedEvent) event;
                Set<ClusterDataHolder> clusterDataHolders = applicationRemovedEvent.getClusterData();

                try {
                    //TODO remove monitors as well as any starting or pending threads
                    ApplicationMonitor monitor = AutoscalerContext.getInstance().
                            getAppMonitor(applicationRemovedEvent.getAppId());
                    if (monitor != null) {
                        for (ClusterDataHolder clusterData : clusterDataHolders) {
                            //stopping the cluster monitor and remove it from the AS
                            VMClusterMonitor clusterMonitor = ((VMClusterMonitor)
                                    AutoscalerContext.getInstance().getClusterMonitor(clusterData.getClusterId()));
                            if (clusterMonitor != null) {
                                clusterMonitor.setDestroyed(true);
                                AutoscalerContext.getInstance().removeClusterMonitor(clusterData.getClusterId());
                            } else {
                                log.warn("Cluster Monitor not found for [ cluster id ] " +
                                        clusterData.getClusterId() + ", unable to remove");
                            }
                        }
                        //removing the application monitor
                        AutoscalerContext.getInstance().
                                removeAppMonitor(applicationRemovedEvent.getAppId());
                    } else {
                        log.warn("Application Monitor cannot be found for the terminated [application] "
                                + applicationRemovedEvent.getAppId() + ", unable to remove");
                    }


                } catch (Exception e) {
                    String msg = "Error processing event " + e.getMessage();
                    log.error(msg, e);
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
					String msg = "Error processing event "
									+ e.getLocalizedMessage();
							log.error(msg, e);
				}
			}
		});

        topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
        	@Override
        	protected void onEvent(Event event) {
        		ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;
        		try {
        		     //TopologyManager.acquireReadLock();
                    TopologyManager.acquireReadLockForCluster(clusterRemovedEvent.getServiceName(),
                            clusterRemovedEvent.getClusterId());
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
        		} finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(clusterRemovedEvent.getServiceName(),
                            clusterRemovedEvent.getClusterId());
                }
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
        		MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;
        		try {
        			 //TopologyManager.acquireReadLock();
                    TopologyManager.acquireReadLockForCluster(memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId());
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
        		String clusterId = memberMaintenanceModeEvent.getClusterId();
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
        		monitor.handleMemberMaintenanceModeEvent(memberMaintenanceModeEvent);
        		} catch (Exception e) {
        			String msg = "Error processing event " + e.getLocalizedMessage();
        			log.error(msg, e);
        		} finally {
                    TopologyManager.releaseReadLockForCluster(memberMaintenanceModeEvent.getServiceName(),
                            memberMaintenanceModeEvent.getClusterId());
                }
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

    protected synchronized void startApplicationMonitor(String applicationId) {
        Thread th = null;
        if (!AutoscalerContext.getInstance().appMonitorExist(applicationId)) {
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
