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

package org.apache.stratos.cartridge.agent.executor.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.impl.GitBasedArtifactRepository;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
import org.apache.stratos.cartridge.agent.executor.ExtensionExecutor;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;
import org.apache.stratos.cartridge.agent.util.ExtensionUtils;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.message.processor.instance.notifier.InstanceNotifierMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventMessageReceiver;

/**
 * This extension is suppose to start all the event listeners that the Cartridge Agent
 * needs.
 * 
 */
public class StartListenersExtensionExecutor extends ExtensionExecutor {
	
	private static final Log log = LogFactory.getLog(StartListenersExtensionExecutor.class);

	public StartListenersExtensionExecutor() {
		super(StartListenersExtensionExecutor.class.getName());
	}
	
	public StartListenersExtensionExecutor(List<String> fileNames) {
		super.setFileNamesToBeExecuted(fileNames);
	}

	@Override
	public void execute() {

		String jndiPropertiesDir = System.getProperty(CartridgeAgentConstants.JNDI_PROPERTIES_DIR);
        if(StringUtils.isBlank(jndiPropertiesDir)) {
            if(log.isErrorEnabled()){
                log.error(String.format("System property not found: %s", CartridgeAgentConstants.JNDI_PROPERTIES_DIR));
            }
            return;
        }

        String payloadPath = System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH);
        if(StringUtils.isBlank(payloadPath)) {
            if(log.isErrorEnabled()){
                log.error(String.format("System property not found: %s", CartridgeAgentConstants.PARAM_FILE_PATH));
            }
            return;
        }

        String extensionsDir = System.getProperty(CartridgeAgentConstants.EXTENSIONS_DIR);
        if(StringUtils.isBlank(extensionsDir)) {
            if(log.isWarnEnabled()){
                log.warn(String.format("System property not found: %s", CartridgeAgentConstants.EXTENSIONS_DIR));
            }
        }

        // Start instance notifier listener thread
        if(log.isDebugEnabled()) {
            log.debug("Starting instance notifier event message receiver thread");
        }
        
        InstanceNotifierMessageProcessorChain processorChain = new InstanceNotifierMessageProcessorChain();
        processorChain.addEventListener(new ArtifactUpdateEventListener() {
            @Override
            protected void onEvent(Event event) {
                onArtifactUpdateEvent((ArtifactUpdatedEvent) event);
            }
        });

        processorChain.addEventListener(new InstanceCleanupMemberEventListener() {
            @Override
            protected void onEvent(Event event) {
                String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();
                InstanceCleanupMemberEvent instanceCleanupMemberEvent = (InstanceCleanupMemberEvent)event;
                if(memberIdInPayload.equals(instanceCleanupMemberEvent.getMemberId())) {
                    onInstanceCleanupEvent();
                }
            }
        });

        processorChain.addEventListener(new InstanceCleanupClusterEventListener() {
            @Override
            protected void onEvent(Event event) {
                String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
                InstanceCleanupClusterEvent instanceCleanupClusterEvent = (InstanceCleanupClusterEvent)event;
                if(clusterIdInPayload.equals(instanceCleanupClusterEvent.getClusterId())) {
                    onInstanceCleanupEvent();
                }
            }
        });
        InstanceNotifierEventMessageDelegator messageDelegator = new InstanceNotifierEventMessageDelegator(processorChain);
        InstanceNotifierEventMessageReceiver messageReceiver = new InstanceNotifierEventMessageReceiver(messageDelegator);
        Thread messageReceiverThread = new Thread(messageReceiver);
        messageReceiverThread.start();

        // Wait until message receiver is subscribed to the topic to
        // send the instance started event
        while (!messageReceiver.isSubscribed())  {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        
	}
	
	private void onArtifactUpdateEvent(ArtifactUpdatedEvent event) {
        ArtifactUpdatedEvent artifactUpdatedEvent = event;
        if(log.isInfoEnabled()) {
            log.info(String.format("Artifact update event received: %s", artifactUpdatedEvent.toString()));
        }

        String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
        String localRepoPath = CartridgeAgentConfiguration.getInstance().getAppPath();
        String clusterIdInMessage = artifactUpdatedEvent.getClusterId();
        String repoURL = artifactUpdatedEvent.getRepoURL();
        String repoPassword = CartridgeAgentUtils.decryptPassword(artifactUpdatedEvent.getRepoPassword());
        String repoUsername = artifactUpdatedEvent.getRepoUserName();
        String tenantId = artifactUpdatedEvent.getTenantId();
        boolean isMultitenant = CartridgeAgentConfiguration.getInstance().isMultitenant();

        if(StringUtils.isNotEmpty(repoURL) && (clusterIdInPayload != null) && clusterIdInPayload.equals(clusterIdInMessage)) {
            if(log.isInfoEnabled()) {
                log.info("Executing git checkout");
            }
            RepositoryInformation repoInformation = new RepositoryInformation();
            repoInformation.setRepoUsername(repoUsername);
            repoInformation.setRepoPassword(repoPassword);
            repoInformation.setRepoUrl(repoURL);
            repoInformation.setRepoPath(localRepoPath);
            repoInformation.setTenantId(tenantId);
            repoInformation.setMultitenant(isMultitenant);
//            boolean cloneExists = GitBasedArtifactRepository.getInstance().cloneExists(repoInformation);
            GitBasedArtifactRepository.getInstance().checkout(repoInformation);

            ExtensionUtils.executeArtifactsUpdatedExtension();

            //TODO I think we do not need to wait till git repo gets cloned, to send the instance activated event. - Nirmal
//            if(!cloneExists){
//                // Executed git clone, publish instance activated event
//                CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
//            }

            // Start the artifact update task
            boolean artifactUpdateEnabled = Boolean.parseBoolean(System.getProperty(CartridgeAgentConstants.ENABLE_ARTIFACT_UPDATE));
            if (artifactUpdateEnabled) {

                long artifactUpdateInterval = 10;
                // get update interval
                String artifactUpdateIntervalStr = System.getProperty(CartridgeAgentConstants.ARTIFACT_UPDATE_INTERVAL);

                if (artifactUpdateIntervalStr != null && !artifactUpdateIntervalStr.isEmpty()) {
                    try {
                        artifactUpdateInterval = Long.parseLong(artifactUpdateIntervalStr);

                    } catch (NumberFormatException e) {
                        log.error("Invalid artifact sync interval specified ", e);
                        artifactUpdateInterval = 10;
                    }
                }

                log.info("Artifact updating task enabled, update interval: " + artifactUpdateInterval + "s");
                GitBasedArtifactRepository.getInstance().scheduleSyncTask(repoInformation, artifactUpdateInterval);

            } else {
                log.info("Artifact updating task disabled");
            }

        }
    }

    private void onInstanceCleanupEvent() {
        if(log.isInfoEnabled()) {
            log.info("Executing cleaning up the data in the cartridge instance...");
        }
        //sending event on the maintenance mode
        CartridgeAgentEventPublisher.publishMaintenanceModeEvent();

        //cleaning up the cartridge instance's data
        ExtensionUtils.executeCleanupExtension();
        if(log.isInfoEnabled()) {
            log.info("cleaning up finished in the cartridge instance...");
        }
        if(log.isInfoEnabled()) {
            log.info("publishing ready to shutdown event...");
        }
        //publishing the Ready to shutdown event after performing the cleanup
        CartridgeAgentEventPublisher.publishInstanceReadyToShutdownEvent();
    }

	@Override
	public void cleanUp() {
	}

	
}
