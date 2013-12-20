package org.apache.stratos.cartridge.agent.event.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.Constants;

/**
 * Cartridge agent event publisher.
 */
public class CartridgeAgentEventPublisher {
    private static final Log log = LogFactory.getLog(CartridgeAgentEventPublisher.class);

    public static void publishInstanceStartedEvent() {
        if(log.isDebugEnabled()) {
            log.debug("Publishing instance started event");
        }
        InstanceStartedEvent event = new InstanceStartedEvent(
                CartridgeAgentConfiguration.getInstance().getServiceName(),
                CartridgeAgentConfiguration.getInstance().getClusterId(),
                CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                CartridgeAgentConfiguration.getInstance().getPartitionId(),
                CartridgeAgentConfiguration.getInstance().getMemberId());

        EventPublisher eventPublisher = new EventPublisher(Constants.INSTANCE_STATUS_TOPIC);
        eventPublisher.publish(event);
        if(log.isDebugEnabled()) {
            log.debug("Instance started event published");
        }
    }

    public static void publishInstanceActivatedEvent() {
        if(log.isDebugEnabled()) {
            log.debug("Publishing instance activated event");
        }
        InstanceActivatedEvent event = new InstanceActivatedEvent(
                CartridgeAgentConfiguration.getInstance().getServiceName(),
                CartridgeAgentConfiguration.getInstance().getClusterId(),
                CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                CartridgeAgentConfiguration.getInstance().getPartitionId(),
                CartridgeAgentConfiguration.getInstance().getMemberId());

        EventPublisher eventPublisher = new EventPublisher(Constants.INSTANCE_STATUS_TOPIC);
        eventPublisher.publish(event);
        if(log.isDebugEnabled()) {
            log.debug("Instance activated event published");
        }
    }
}
