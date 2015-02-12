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
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingRemovedEvent;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingAddedEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingRemovedEventListener;
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
    
    private CartridgeAgentEventListeners eventListenerns;

    // We have an asynchronous activity running to respond to ADC updates. We want to ensure
    // that no publishInstanceActivatedEvent() call is made *before* the port activation test
    // has succeeded. This flag controls that.
    private boolean portsActivated;

    @Override
    public void run() {
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent started");
        }
        
        eventListenerns = new  CartridgeAgentEventListeners();
        
        validateRequiredSystemProperties();
        
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent validated system properties done");
        }

        // Start instance notifier listener thread
        portsActivated = false;
        subscribeToTopicsAndRegisterListeners();
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent subscribeToTopicsAndRegisterListeners done");
        }

        // Start topology event receiver thread
        registerTopologyEventListeners();
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent registerTopologyEventListeners done");
        }
        
        // Start tenant event receiver thread
        registerTenantEventListeners();
        
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent registering all event listeners ... done");
        }
        
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
		
		if (log.isInfoEnabled()) {
            log.info("Cartridge agent initialized done");
        }

        // Execute instance started shell script
        extensionHandler.onInstanceStartedEvent();
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent onInstanceStartedEvent done");
        }

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
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent startServerExtension done");
        }

        // Wait for all ports to be active
        CartridgeAgentUtils.waitUntilPortsActive(CartridgeAgentConfiguration.getInstance().getListenAddress(),
                CartridgeAgentConfiguration.getInstance().getPorts());
        portsActivated = true;
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent portsActivated done");
        }

        // Publish instance activated event
        CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent publishInstanceActivatedEvent done");
        }

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
        
        if (log.isInfoEnabled()) {
            log.info("Cartridge agent getRepoUrl done");
        }

        if ("null".equals(repoUrl) || StringUtils.isBlank(repoUrl)) {
            if (log.isInfoEnabled()) {
                log.info("No artifact repository found");
            }
            // Execute instance activated shell script
            extensionHandler.onInstanceActivatedEvent();
            
            if (log.isInfoEnabled()) {
                log.info("Cartridge agent onInstanceActivatedEvent done");
            }

            // Publish instance activated event
            CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
        } else {
        	if (log.isInfoEnabled()) {
                log.info("Cartridge agent - artifact repository found");
            }
            //Start periodical file processor task
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
            log.debug("SsubscribeToTopicsAndRegisterListeners before");
        }
    	
    	eventListenerns.startInstanceNotifierReceiver();
    	
    	if (log.isDebugEnabled()) {
            log.debug("SsubscribeToTopicsAndRegisterListeners after");
        }
    }

    protected void registerTopologyEventListeners() {
    	if (log.isDebugEnabled()) {
            log.debug("registerTopologyEventListeners before");
        }
    	eventListenerns.startTopologyEventReceiver();
    	
    	if (log.isDebugEnabled()) {
            log.debug("registerTopologyEventListeners after");
        }
    }

    protected void registerTenantEventListeners() {
    	if (log.isDebugEnabled()) {
            log.debug("registerTenantEventListeners before X");
        }
    	
    	if (log.isDebugEnabled()) {
            log.debug("skipping registerTenantEventListeners before X");
        }
    	
    	eventListenerns.startTenantEventReceiver();
    	
    	if (log.isDebugEnabled()) {
            log.debug("registerTenantEventListeners after");
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
