package org.apache.stratos.cartridge.agent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.impl.GitBasedArtifactRepository;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisherConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.exception.DataPublisherException;
import org.apache.stratos.cartridge.agent.data.publisher.log.LogPublisherManager;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
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

import java.util.List;

/**
 * Cartridge agent runnable.
 */
public class CartridgeAgent implements Runnable {

    private static final Log log = LogFactory.getLog(CartridgeAgent.class);

    private boolean terminated;

    @Override
    public void run() {
        if(log.isInfoEnabled()) {
            log.info("Cartridge agent started");
        }

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

        // Publish instance started event
        CartridgeAgentEventPublisher.publishInstanceStartedEvent();

        // Execute start servers extension
        ExtensionUtils.executeStartServersExtension();

        // Wait for all ports to be active
        CartridgeAgentUtils.waitUntilPortsActive("localhost", CartridgeAgentConfiguration.getInstance().getPorts());

        // Mount persistance volumes.
        ExtensionUtils.executeVolumeMountExtension();

        // Check repo url
        String repoUrl = CartridgeAgentConfiguration.getInstance().getRepoUrl();
        if ("null".equals(repoUrl) || StringUtils.isBlank(repoUrl)) {
            if(log.isInfoEnabled()) {
                log.info("No artifact repository found");
            }

            // Publish instance activated event
            CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
        }


        // TODO: Start this thread only if this node is configured as a commit true node
        // Start periodical file checker task
        // ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // scheduler.scheduleWithFixedDelay(new RepositoryFileListener(), 0, 10, TimeUnit.SECONDS);

        // Keep the thread live until terminated

        // start log publishing
        LogPublisherManager logPublisherManager = new LogPublisherManager();
        publishLogs(logPublisherManager);

        while (!terminated);

        logPublisherManager.stop();
    }

    private static void publishLogs (LogPublisherManager logPublisherManager) {

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
            boolean cloneExists = GitBasedArtifactRepository.getInstance().cloneExists(repoInformation);
            GitBasedArtifactRepository.getInstance().checkout(repoInformation);

            ExtensionUtils.executeArtifactsUpdatedExtension();

            if(!cloneExists){
                // Executed git clone, publish instance activated event
                CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
            }

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

    public void terminate() {
        terminated = true;
    }
}
