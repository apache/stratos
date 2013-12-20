package org.apache.stratos.cartridge.agent.event.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.statistics.publisher.HealthStatisticsNotifier;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.Constants;

/**
 * Cartridge agent event publisher.
 */
public class CartridgeAgentEventPublisher {
    private static final Log log = LogFactory.getLog(CartridgeAgentEventPublisher.class);
    private static boolean started;
    private static boolean activated;

    public static void publishInstanceStartedEvent() {
        if (!started) {
            if (log.isInfoEnabled()) {
                log.info("Publishing instance started event");
            }
            InstanceStartedEvent event = new InstanceStartedEvent(
                    CartridgeAgentConfiguration.getInstance().getServiceName(),
                    CartridgeAgentConfiguration.getInstance().getClusterId(),
                    CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                    CartridgeAgentConfiguration.getInstance().getPartitionId(),
                    CartridgeAgentConfiguration.getInstance().getMemberId());

            EventPublisher eventPublisher = new EventPublisher(Constants.INSTANCE_STATUS_TOPIC);
            eventPublisher.publish(event);
            started = true;
            if (log.isInfoEnabled()) {
                log.info("Instance started event published");
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Instance already started");
            }
        }
    }

    public static void publishInstanceActivatedEvent() {
        if (!activated) {
            if (log.isInfoEnabled()) {
                log.info("Publishing instance activated event");
            }
            InstanceActivatedEvent event = new InstanceActivatedEvent(
                    CartridgeAgentConfiguration.getInstance().getServiceName(),
                    CartridgeAgentConfiguration.getInstance().getClusterId(),
                    CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                    CartridgeAgentConfiguration.getInstance().getPartitionId(),
                    CartridgeAgentConfiguration.getInstance().getMemberId());

            EventPublisher eventPublisher = new EventPublisher(Constants.INSTANCE_STATUS_TOPIC);
            eventPublisher.publish(event);
            if (log.isInfoEnabled()) {
                log.info("Instance activated event published");
            }

            if (log.isInfoEnabled()) {
                log.info("Starting health statistics notifier");
            }
            Thread thread = new Thread(new HealthStatisticsNotifier());
            thread.start();
            activated = true;
            if (log.isInfoEnabled()) {
                log.info("Health statistics notifier started");
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Instance already activated");
            }
        }
    }
}
