package org.apache.stratos.cartridge.agent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.impl.GitBasedArtifactRepository;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.message.processor.instance.notifier.InstanceNotifierMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventMessageReceiver;

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
            throw new RuntimeException(String.format("System property not found: %s", CartridgeAgentConstants.JNDI_PROPERTIES_DIR));
        }

        String payloadPath = System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH);
        if(StringUtils.isBlank(payloadPath)) {
            throw new RuntimeException(String.format("System property not found: %s", CartridgeAgentConstants.PARAM_FILE_PATH));
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

        // Wait for all ports to be active
        CartridgeAgentUtils.waitUntilPortsActive();

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
        while (!terminated);
    }

    private void onArtifactUpdateEvent(ArtifactUpdatedEvent event) {
        ArtifactUpdatedEvent artifactUpdatedEvent = (ArtifactUpdatedEvent) event;
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
            boolean cloneExists = GitBasedArtifactRepository.cloneExists(repoInformation);
            GitBasedArtifactRepository.checkout(repoInformation);
            if(!cloneExists){
                // Executed git clone, publish instance activated event
                CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
            }
        }
    }

    public void terminate() {
        terminated = true;
    }
}
