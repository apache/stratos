package org.apache.stratos.cartridge.agent;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.extensions.DefaultExtensionHandler;
import org.apache.stratos.cartridge.agent.extensions.ExtensionHandler;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.listener.tenant.CompleteTenantEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantSubscribedEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantUnSubscribedEventListener;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.concurrent.ExecutorService;


public class CartridgeAgentEventListeners
{

    private static final Log log = LogFactory.getLog(CartridgeAgentEventListeners.class);

    private InstanceNotifierEventReceiver instanceNotifierEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    private TenantEventReceiver tenantEventReceiver;
    
    private ExtensionHandler extensionHandler;
    private static final ExecutorService instanceNotifierExecutorService =
            StratosThreadPool.getExecutorService("CARTRIDGEAGENT_NOTIFIER_EVENT_LISTENERS", 1);
    
    private static final ExecutorService topologyEventExecutorService =
            StratosThreadPool.getExecutorService("CARTRIDGEAGENT_TOPOLOGY_EVENT_LISTENERS", 1);
    
    private static final ExecutorService tenantEventExecutorService =
            StratosThreadPool.getExecutorService("CARTRIDGEAGENT_TENANT_EVENT_LISTENERS", 1);

    public CartridgeAgentEventListeners() {
    	if (log.isDebugEnabled()) {
            log.debug("creating cartridgeAgentEventListeners ... ");
        }
        this.topologyEventReceiver = new TopologyEventReceiver();
        this.topologyEventReceiver.setExecutorService(topologyEventExecutorService);
        
        this.instanceNotifierEventReceiver = new InstanceNotifierEventReceiver();
        
        this.tenantEventReceiver = new TenantEventReceiver();
        this.tenantEventReceiver.setExecutorService(tenantEventExecutorService);

        extensionHandler = new DefaultExtensionHandler();
        
        addInstanceNotifierEventListeners();
        addTopologyEventListeners();
        addTenantEventListeners();
        
        if (log.isDebugEnabled()) {
            log.debug("creating cartridgeAgentEventListeners ... done ");
        }
        
    }

    public void startTopologyEventReceiver() {
        
    	if (log.isDebugEnabled()) {
            log.debug("Starting cartridge agent topology event message receiver");
        }
    	
        topologyEventExecutorService.submit(new Runnable() {
            @Override
            public void run() {
            	topologyEventReceiver.execute();
            }
        });

        if (log.isInfoEnabled()) 
        {
            log.info("Cartridge agent topology receiver thread started, waiting for event messages ...");
        }

    }
    
    public void startInstanceNotifierReceiver() {
    	
        if (log.isDebugEnabled()) {
            log.debug("Starting cartridge agent instance notifier event message receiver");
        }

        instanceNotifierExecutorService.submit(new Runnable() {
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
    	
        topologyEventExecutorService.submit(new Runnable() {
            @Override
            public void run() {
            	topologyEventReceiver.execute();
            }
        });

        if (log.isInfoEnabled()) {
            log.info("Cartridge agent tenant receiver thread started, waiting for event messages ...");
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


        if(log.isInfoEnabled()) {
            log.info("Instance notifier event listener added ... ");
        }
    }
    
    private void addTopologyEventListeners() {
        topologyEventReceiver.addEventListener(new MemberCreatedEventListener() {
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
        
        if(log.isInfoEnabled()) {
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

        if(log.isInfoEnabled()) {
            log.info("Tenant event listener added ... ");
        }
    }

    /**
     * Terminate load balancer topology receiver thread.
     */

    public void terminate() {
        topologyEventReceiver.terminate();
    }

}

