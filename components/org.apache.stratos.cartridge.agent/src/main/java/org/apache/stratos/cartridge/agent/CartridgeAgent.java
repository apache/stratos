package org.apache.stratos.cartridge.agent;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisherConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.exception.DataPublisherException;
import org.apache.stratos.cartridge.agent.data.publisher.log.LogPublisherManager;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
import org.apache.stratos.cartridge.agent.extensions.DefaultExtensionHandler;
import org.apache.stratos.cartridge.agent.extensions.ExtensionHandler;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainAddedEvent;
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainRemovedEvent;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.listener.tenant.SubscriptionDomainsAddedEventListener;
import org.apache.stratos.messaging.listener.tenant.SubscriptionDomainsRemovedEventListener;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.tenant.CompleteTenantEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantSubscribedEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantUnSubscribedEventListener;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;

/**
 * Cartridge agent runnable.
 */
public class CartridgeAgent implements Runnable {

   	private static final Log log = LogFactory.getLog(CartridgeAgent.class);
    private static final ExtensionHandler extensionHandler = new DefaultExtensionHandler();
    private boolean terminated;

    // We have an asynchronous activity running to respond to ADC updates. We want to ensure
    // that no publishInstanceActivatedEvent() call is made *before* the port activation test
    // has succeeded. This flag controls that.
    private boolean portsActivated;

    @Override
    public void run() {
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent started");
        }

        validateRequiredSystemProperties();

        // Start instance notifier listener thread
        portsActivated = false;
        subscribeToTopicsAndRegisterListeners();

        // Start topology event receiver thread
        registerTopologyEventListeners();
        
        // Start tenant event receiver thread
        registerTenantEventListeners();
        
		// wait till the member spawned event
		while (!CartridgeAgentConfiguration.getInstance().isInitialized()) {
			try {
				if (log.isDebugEnabled()) {
					log.info("Waiting for Cartridge Agent to be initialized...");
				}
				Thread.sleep(1000);
			} catch (InterruptedException ignore) {
			}
		}

        // Execute instance started shell script
        extensionHandler.onInstanceStartedEvent();

        // Publish instance started event
        CartridgeAgentEventPublisher.publishInstanceStartedEvent();

        // Execute start servers extension
        try {
            extensionHandler.startServerExtension();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error processing start servers event", e);
            }
        }

        // Wait for all ports to be active
        CartridgeAgentUtils.waitUntilPortsActive(CartridgeAgentConfiguration.getInstance().getListenAddress(),
                CartridgeAgentConfiguration.getInstance().getPorts());
        portsActivated = true;

        // Publish instance activated event
        CartridgeAgentEventPublisher.publishInstanceActivatedEvent();

        // Check repo url
        String repoUrl = CartridgeAgentConfiguration.getInstance().getRepoUrl();
        /*if(CartridgeAgentConfiguration.getInstance().isMultitenant()) {
            if (CartridgeAgentConfiguration.getInstance().isCommitsEnabled()) {
                log.info(" Commits enabled. Starting File listener ");
                ScheduledExecutorService scheduler = Executors
                        .newScheduledThreadPool(1);
                scheduler.scheduleWithFixedDelay(new RepositoryFileListener(), 0,
                        10, TimeUnit.SECONDS);
            }
            
            // Start super tenant artifact copy task
            // from temp location to super tenant app path
			//ScheduledExecutorService scheduler = Executors
			//		.newScheduledThreadPool(1);
			//scheduler.scheduleWithFixedDelay(new ArtifactCopyTask(
			//		CartridgeAgentConstants.SUPERTENANT_TEMP_PATH,
			//		CartridgeAgentConfiguration.getInstance().getAppPath()+ "/repository/deployment/server/"
			//		),
			//		0, 10, TimeUnit.SECONDS);
        } */

        if ("null".equals(repoUrl) || StringUtils.isBlank(repoUrl)) {
            if (log.isInfoEnabled()) {
                log.info("No artifact repository found");
            }
            // Execute instance activated shell script
            extensionHandler.onInstanceActivatedEvent();

            // Publish instance activated event
            CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
        } else {
            //Start periodical file checker task
            /*if (CartridgeAgentConfiguration.getInstance().isCommitsEnabled()) {
                log.info(" Commits enabled. Starting File listener ");
                ScheduledExecutorService scheduler = Executors
                        .newScheduledThreadPool(1);
                scheduler.scheduleWithFixedDelay(new RepositoryFileListener(), 0,
                        10, TimeUnit.SECONDS);
            } */
        }

//        if (CartridgeAgentConfiguration.getInstance().isInternalRepo()) {
//            // Start periodic file copy for super tenant
//            // From repo/deployment/server to /tmp/-1234
//
//            ScheduledExecutorService scheduler = Executors
//                    .newScheduledThreadPool(1);
//            scheduler.scheduleWithFixedDelay(
//            		new ArtifactCopyTask(CartridgeAgentConfiguration.getInstance().getAppPath()
//            		+ "/repository/deployment/server/",
//            		CartridgeAgentConstants.SUPERTENANT_TEMP_PATH), 0,
//                    10, TimeUnit.SECONDS);
//        }

        String persistenceMappingsPayload = CartridgeAgentConfiguration.getInstance().getPersistenceMappings();
        if (persistenceMappingsPayload != null) {
            extensionHandler.volumeMountExtension(persistenceMappingsPayload);
        }

        // start log publishing
        LogPublisherManager logPublisherManager = new LogPublisherManager();
        publishLogs(logPublisherManager);

        // Keep the thread live until terminated
        while (!terminated) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }

        logPublisherManager.stop();
    }

    protected void subscribeToTopicsAndRegisterListeners() {
        if (log.isDebugEnabled()) {
            log.debug("Starting instance notifier event message receiver thread");
        }

        InstanceNotifierEventReceiver instanceNotifierEventReceiver = new InstanceNotifierEventReceiver();
        instanceNotifierEventReceiver.addEventListener(new ArtifactUpdateEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    extensionHandler.onArtifactUpdatedEvent((ArtifactUpdatedEvent) event);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing artifact update event", e);
                    }
                }
            }
        });

        instanceNotifierEventReceiver.addEventListener(new InstanceCleanupMemberEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();
                    InstanceCleanupMemberEvent instanceCleanupMemberEvent = (InstanceCleanupMemberEvent) event;
                    if (memberIdInPayload.equals(instanceCleanupMemberEvent.getMemberId())) {
                        extensionHandler.onInstanceCleanupMemberEvent(instanceCleanupMemberEvent);
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing instance cleanup member event", e);
                    }
                }

            }
        });

        instanceNotifierEventReceiver.addEventListener(new InstanceCleanupClusterEventListener() {
            @Override
            protected void onEvent(Event event) {
                String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
                InstanceCleanupClusterEvent instanceCleanupClusterEvent = (InstanceCleanupClusterEvent) event;
                if (clusterIdInPayload.equals(instanceCleanupClusterEvent.getClusterId())) {
                    extensionHandler.onInstanceCleanupClusterEvent(instanceCleanupClusterEvent);
                }
            }
        });
        Thread eventReceiverThread = new Thread(instanceNotifierEventReceiver);
        eventReceiverThread.start();
        if(log.isInfoEnabled()) {
            log.info("Instance notifier event message receiver thread started");
        }

        // Wait until message receiver is subscribed to the topic to send the instance started event
        while (!instanceNotifierEventReceiver.isSubscribed()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

    protected void registerTopologyEventListeners() {
        if (log.isDebugEnabled()) {
            log.debug("Starting topology event message receiver thread");
        }
        
        TopologyEventReceiver topologyEventReceiver = new TopologyEventReceiver();
        
        topologyEventReceiver.addEventListener(new InstanceSpawnedEventListener() {
        	@Override
        	protected void onEvent(Event event) {
        		try {
        			boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
        			if (initialized) {
        				// no need to process this event, if the member is initialized.
        				return;
        			}
        			TopologyManager.acquireReadLock();
        			if (log.isDebugEnabled()) {
        				log.debug("Instance spawned event received");
        			}
        			InstanceSpawnedEvent instanceSpawnedEvent = (InstanceSpawnedEvent) event;
        			extensionHandler.onInstanceSpawnedEvent(instanceSpawnedEvent);
        		} catch (Exception e) {
        			if (log.isErrorEnabled()) {
        				log.error("Error processing instance spawned event", e);
        			}
        		} finally {
        			TopologyManager.releaseReadLock();
        		}
        	}
        });
        
        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
            	boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
            	if (!initialized) {
            		return;
            	}
                try {
                    TopologyManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Member activated event received");
                    }
                    MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;
                    extensionHandler.onMemberActivatedEvent(memberActivatedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing member activated event", e);
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
            	boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
            	if (!initialized) {
            		return;
            	}
                try {
                    TopologyManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Member terminated event received");
                    }
                    MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
                    extensionHandler.onMemberTerminatedEvent(memberTerminatedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing member terminated event", e);
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {
            	boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
            	if (!initialized) {
            		return;
            	}
                try {
                    TopologyManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Member suspended event received");
                    }
                    MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;
                    extensionHandler.onMemberSuspendedEvent(memberSuspendedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing member suspended event", e);
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new CompleteTopologyEventListener() {

            @Override
            protected void onEvent(Event event) {
            	boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
                if (!initialized) {
                    try {
                        TopologyManager.acquireReadLock();
                        if (log.isDebugEnabled()) {
                            log.debug("Complete topology event received");
                        }
                        CompleteTopologyEvent completeTopologyEvent = (CompleteTopologyEvent) event;
                        extensionHandler.onCompleteTopologyEvent(completeTopologyEvent);
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Error processing complete topology event", e);
                        }
                    } finally {
                        TopologyManager.releaseReadLock();
                    }
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
            	boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
            	if (!initialized) {
            		return;
            	}
                try {
                    TopologyManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Member started event received");
                    }
                    MemberStartedEvent memberStartedEvent = (MemberStartedEvent) event;
                    extensionHandler.onMemberStartedEvent(memberStartedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing member started event", e);
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        Thread thread = new Thread(topologyEventReceiver);
        thread.start();
        if (log.isDebugEnabled()) {
            log.info("Cartridge Agent topology receiver thread started");
        }
    }

    protected void registerTenantEventListeners() {

        if (log.isDebugEnabled()) {
            log.debug("Starting tenant event message receiver thread");
        }
        TenantEventReceiver tenantEventReceiver = new TenantEventReceiver();
        tenantEventReceiver.addEventListener(new SubscriptionDomainsAddedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Subscription domain added event received");
                    }
                    SubscriptionDomainAddedEvent subscriptionDomainAddedEvent = (SubscriptionDomainAddedEvent) event;
                    extensionHandler.onSubscriptionDomainAddedEvent(subscriptionDomainAddedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing subscription domains added event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }

            }
        });

        tenantEventReceiver.addEventListener(new SubscriptionDomainsRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Subscription domain removed event received");
                    }
                    SubscriptionDomainRemovedEvent subscriptionDomainRemovedEvent = (SubscriptionDomainRemovedEvent) event;
                    extensionHandler.onSubscriptionDomainRemovedEvent(subscriptionDomainRemovedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing subscription domains removed event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }
            }
        });

        tenantEventReceiver.addEventListener(new CompleteTenantEventListener() {
            private boolean initialized;
            @Override
            protected void onEvent(Event event) {
                if (!initialized) {
                    try {
                        TenantManager.acquireReadLock();
                        if (log.isDebugEnabled()) {
                            log.debug("Complete tenant event received");
                        }
                        CompleteTenantEvent completeTenantEvent = (CompleteTenantEvent) event;
                        extensionHandler.onCompleteTenantEvent(completeTenantEvent);
                        initialized = true;
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Error processing complete tenant event", e);
                        }
                    } finally {
                        TenantManager.releaseReadLock();
                    }

                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Complete tenant event updating task disabled");
                    }
                }
            }
        });

        tenantEventReceiver.addEventListener(new TenantSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant subscribed event received");
                    }
                    TenantSubscribedEvent tenantSubscribedEvent = (TenantSubscribedEvent) event;
                    extensionHandler.onTenantSubscribedEvent(tenantSubscribedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing tenant subscribed event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }
            }
        });

        tenantEventReceiver.addEventListener(new TenantUnSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant unSubscribed event received");
                    }
                    TenantUnSubscribedEvent tenantUnSubscribedEvent = (TenantUnSubscribedEvent) event;
                    extensionHandler.onTenantUnSubscribedEvent(tenantUnSubscribedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing tenant unSubscribed event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }
            }
        });

        Thread tenantEventReceiverThread = new Thread(tenantEventReceiver);
        tenantEventReceiverThread.start();
        if (log.isInfoEnabled()) {
            log.info("Tenant event message receiver thread started");
        }
    }

    protected void validateRequiredSystemProperties() {
        String jndiPropertiesDir = System.getProperty(CartridgeAgentConstants.JNDI_PROPERTIES_DIR);
        if (StringUtils.isBlank(jndiPropertiesDir)) {
            if (log.isErrorEnabled()) {
                log.error(String.format("System property not found: %s", CartridgeAgentConstants.JNDI_PROPERTIES_DIR));
            }
            return;
        }

        String payloadPath = System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH);
        if (StringUtils.isBlank(payloadPath)) {
            if (log.isErrorEnabled()) {
                log.error(String.format("System property not found: %s", CartridgeAgentConstants.PARAM_FILE_PATH));
            }
            return;
        }

        String extensionsDir = System.getProperty(CartridgeAgentConstants.EXTENSIONS_DIR);
        if (StringUtils.isBlank(extensionsDir)) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("System property not found: %s", CartridgeAgentConstants.EXTENSIONS_DIR));
            }
        }
    }

    private static void publishLogs(LogPublisherManager logPublisherManager) {
        // check if enabled
        if (DataPublisherConfiguration.getInstance().isEnabled()) {
            List<String> logFilePaths = CartridgeAgentConfiguration.getInstance().getLogFilePaths();
            if (logFilePaths == null) {
                log.error("No valid log file paths found, no logs will be published");
                return;
            } else {
                // initialize the log publishing
                try {
                    logPublisherManager.init(DataPublisherConfiguration.getInstance());

                } catch (DataPublisherException e) {
                    log.error("Error occurred in log publisher initialization", e);
                    return;
                }

                // start a log publisher for each file path
                for (String logFilePath : logFilePaths) {
                    try {
                        logPublisherManager.start(logFilePath);
                    } catch (DataPublisherException e) {
                        log.error("Error occurred in publishing logs ", e);
                    }
                }
            }
        }
    }

    public static ExtensionHandler getExtensionHandler() {
        return extensionHandler;
    }


    public void terminate() {
        terminated = true;
    }
}
