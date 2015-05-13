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
package org.apache.stratos.cartridge.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.extensions.DefaultExtensionHandler;
import org.apache.stratos.cartridge.agent.extensions.ExtensionHandler;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpRemovedEvent;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.tenant.TenantCreatedEvent;
import org.apache.stratos.messaging.event.tenant.TenantRemovedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUpdatedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.application.signup.ApplicationSignUpRemovedEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.listener.tenant.CompleteTenantEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantCreatedEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantRemovedEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantUpdatedEventListener;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpEventReceiver;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.concurrent.ExecutorService;


public class CartridgeAgentEventListeners {

    private static final Log log = LogFactory.getLog(CartridgeAgentEventListeners.class);

    private InstanceNotifierEventReceiver instanceNotifierEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    private TenantEventReceiver tenantEventReceiver;
    private ApplicationSignUpEventReceiver applicationsEventReceiver;

    private ExtensionHandler extensionHandler;
    private static final ExecutorService eventListenerExecutorService =
            StratosThreadPool.getExecutorService("cartridge.agent.event.listener.thread.pool", 10);

    public CartridgeAgentEventListeners() {
        if (log.isDebugEnabled()) {
            log.debug("Creating cartridge agent event listeners...");
        }
        this.applicationsEventReceiver = new ApplicationSignUpEventReceiver();
        this.applicationsEventReceiver.setExecutorService(eventListenerExecutorService);

        this.topologyEventReceiver = new TopologyEventReceiver();
        this.topologyEventReceiver.setExecutorService(eventListenerExecutorService);

        this.instanceNotifierEventReceiver = new InstanceNotifierEventReceiver();

        this.tenantEventReceiver = new TenantEventReceiver();
        this.tenantEventReceiver.setExecutorService(eventListenerExecutorService);

        extensionHandler = new DefaultExtensionHandler();

        addInstanceNotifierEventListeners();
        addTopologyEventListeners();
        addTenantEventListeners();
        addApplicationsEventListeners();

        if (log.isDebugEnabled()) {
            log.debug("Cartridge agent event listeners created");
        }
    }

    public void startTopologyEventReceiver() {

        if (log.isDebugEnabled()) {
            log.debug("Starting cartridge agent topology event message receiver");
        }

        eventListenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                topologyEventReceiver.execute();
            }
        });

        if (log.isInfoEnabled()) {
            log.info("Cartridge agent topology receiver thread started, waiting for event messages ...");
        }

    }

    public void startInstanceNotifierReceiver() {

        if (log.isDebugEnabled()) {
            log.debug("Starting cartridge agent instance notifier event message receiver");
        }

        eventListenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                instanceNotifierEventReceiver.execute();
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("Cartridge agent Instance notifier event message receiver started, waiting for event messages ...");
        }
    }

    public void startTenantEventReceiver() {

        if (log.isDebugEnabled()) {
            log.debug("Starting cartridge agent tenant event message receiver");
        }

        eventListenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                topologyEventReceiver.execute();
            }
        });

        if (log.isInfoEnabled()) {
            log.info("Cartridge agent tenant receiver thread started, waiting for event messages ...");
        }

    }

    public void startApplicationsEventReceiver() {

        if (log.isDebugEnabled()) {
            log.debug("Starting cartridge agent application event message receiver");
        }

        eventListenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                applicationsEventReceiver.execute();
            }
        });

        if (log.isInfoEnabled()) {
            log.info("Cartridge agent application receiver thread started, waiting for event messages ...");
        }

    }


    private void addInstanceNotifierEventListeners() {
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


        if (log.isInfoEnabled()) {
            log.info("Instance notifier event listener added ... ");
        }
    }

    private void addTopologyEventListeners() {
        topologyEventReceiver.addEventListener(new MemberInitializedEventListener() {
            @Override
            protected void onEvent(Event event) {
                boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
                if (initialized) {
                    // no need to process this event, if the member is initialized.
                    return;
                }
                try {
                    TopologyManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Member initialized event received");
                    }
                    MemberInitializedEvent memberInitializedEvent = (MemberInitializedEvent) event;
                    extensionHandler.onMemberInitializedEvent(memberInitializedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing member created event", e);
                    }
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                boolean initialized = CartridgeAgentConfiguration.getInstance().isInitialized();
                if (initialized) {
                    // no need to process this event, if the member is initialized.
                    return;
                }
                try {
                    TopologyManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Member created event received");
                    }
                    MemberCreatedEvent memberCreatedEvent = (MemberCreatedEvent) event;
                    extensionHandler.onMemberCreatedEvent(memberCreatedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing member created event", e);
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

        if (log.isInfoEnabled()) {
            log.info("Topology event listener added ... ");
        }
    }

    private void addTenantEventListeners() {

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

        tenantEventReceiver.addEventListener(new TenantRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant removed event received");
                    }
                    TenantRemovedEvent tenantRemovedEvent = (TenantRemovedEvent) event;
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant removed event received: " + tenantRemovedEvent);
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing tenant subscribed event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }
            }
        });

        tenantEventReceiver.addEventListener(new TenantUpdatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant updated event received");
                    }
                    TenantUpdatedEvent tenantUpdatedEvent = (TenantUpdatedEvent) event;
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant updated event received: " + tenantUpdatedEvent);
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing tenant updated event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }
            }
        });

        tenantEventReceiver.addEventListener(new TenantCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TenantManager.acquireReadLock();
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant updated event received");
                    }
                    TenantCreatedEvent tenantCreatedEvent = (TenantCreatedEvent) event;
                    if (log.isDebugEnabled()) {
                        log.debug("Tenant updated event received: " + tenantCreatedEvent);
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing tenant updated event", e);
                    }
                } finally {
                    TenantManager.releaseReadLock();
                }
            }
        });

        if (log.isInfoEnabled()) {
            log.info("Tenant event listener added ... ");
        }
    }

    private void addApplicationsEventListeners() {
        applicationsEventReceiver.addEventListener(new ApplicationSignUpRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    ApplicationSignUpRemovedEvent applicationSignUpRemovedEvent = (ApplicationSignUpRemovedEvent) event;
                    extensionHandler.onApplicationSignUpRemovedEvent(applicationSignUpRemovedEvent);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error processing ApplicationSignUpRemovedEvent event", e);
                    }
                }
            }
        });

        if (log.isInfoEnabled()) {
            log.info("applications event listener added ... ");
        }
    }

    /**
     * Terminate load balancer topology receiver thread.
     */

    public void terminate() {
        topologyEventReceiver.terminate();
    }

}

