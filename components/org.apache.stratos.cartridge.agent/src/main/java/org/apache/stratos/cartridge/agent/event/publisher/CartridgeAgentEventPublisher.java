package org.apache.stratos.cartridge.agent.event.publisher;

/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.statistics.publisher.HealthStatisticsNotifier;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.Util;

/**
 * Cartridge agent event publisher.
 */
public class CartridgeAgentEventPublisher {
	private static final Log log = LogFactory
			.getLog(CartridgeAgentEventPublisher.class);
	private static boolean started;
	private static boolean activated;
	private static boolean readyToShutdown;
	private static boolean maintenance;

	public static void publishInstanceStartedEvent() {
		if (!isStarted()) {
			if (log.isInfoEnabled()) {
				log.info("Publishing instance started event");
			}
			InstanceStartedEvent event = new InstanceStartedEvent(
					CartridgeAgentConfiguration.getInstance().getServiceName(),
					CartridgeAgentConfiguration.getInstance().getClusterId(),
					CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
					CartridgeAgentConfiguration.getInstance().getPartitionId(),
					CartridgeAgentConfiguration.getInstance().getMemberId(), null);
			String topic = Util.getMessageTopicName(event);
			EventPublisher eventPublisher = EventPublisherPool
					.getPublisher(topic);
			eventPublisher.publish(event);
			setStarted(true);
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
		if (!isActivated()) {
			if (log.isInfoEnabled()) {
				log.info("Publishing instance activated event");
			}
			InstanceActivatedEvent event = new InstanceActivatedEvent(
					CartridgeAgentConfiguration.getInstance().getServiceName(),
					CartridgeAgentConfiguration.getInstance().getClusterId(),
					CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
					CartridgeAgentConfiguration.getInstance().getPartitionId(),
					CartridgeAgentConfiguration.getInstance().getMemberId(), null);

			// Event publisher connection will
			String topic = Util.getMessageTopicName(event);
			EventPublisher eventPublisher = EventPublisherPool
					.getPublisher(topic);
			eventPublisher.publish(event);
			if (log.isInfoEnabled()) {
				log.info("Instance activated event published");
			}

			if (log.isInfoEnabled()) {
				log.info("Starting health statistics notifier");
			}
			Thread thread = new Thread(new HealthStatisticsNotifier());
			thread.start();
			setActivated(true);
			if (log.isInfoEnabled()) {
				log.info("Health statistics notifier started");
			}
		} else {
			if (log.isWarnEnabled()) {
				log.warn("Instance already activated");
			}
		}
	}

	public static void publishInstanceReadyToShutdownEvent() {
		if (!isReadyToShutdown()) {
			if (log.isInfoEnabled()) {
				log.info("Publishing instance activated event");
			}
			InstanceReadyToShutdownEvent event = new InstanceReadyToShutdownEvent(
					CartridgeAgentConfiguration.getInstance().getServiceName(),
					CartridgeAgentConfiguration.getInstance().getClusterId(),
					CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
					CartridgeAgentConfiguration.getInstance().getPartitionId(),
					CartridgeAgentConfiguration.getInstance().getMemberId(), null);
			String topic = Util.getMessageTopicName(event);
			EventPublisher eventPublisher = EventPublisherPool
					.getPublisher(topic);
			eventPublisher.publish(event);
			setReadyToShutdown(true);
			if (log.isInfoEnabled()) {
				log.info("Instance ReadyToShutDown event published");
			}
		} else {
			if (log.isWarnEnabled()) {
				log.warn("Instance already sent ReadyToShutDown event....");
			}
		}
	}

	public static void publishMaintenanceModeEvent() {
		if (!isMaintenance()) {
			if (log.isInfoEnabled()) {
				log.info("Publishing instance maintenance mode event");
			}
			InstanceMaintenanceModeEvent event = new InstanceMaintenanceModeEvent(
					CartridgeAgentConfiguration.getInstance().getServiceName(),
					CartridgeAgentConfiguration.getInstance().getClusterId(),
					CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
					CartridgeAgentConfiguration.getInstance().getPartitionId(),
					CartridgeAgentConfiguration.getInstance().getMemberId(), null);
			String topic = Util.getMessageTopicName(event);
			EventPublisher eventPublisher = EventPublisherPool
					.getPublisher(topic);
			eventPublisher.publish(event);
			setMaintenance(true);
			if (log.isInfoEnabled()) {
				log.info("Instance Maintenance mode event published");
			}
		} else {
			if (log.isWarnEnabled()) {
				log.warn("Instance already in a Maintenance mode....");
			}
		}
	}

	public static boolean isStarted() {
		return started;
	}

	public static void setStarted(boolean started) {
		CartridgeAgentEventPublisher.started = started;
	}

	public static boolean isActivated() {
		return activated;
	}

	public static void setActivated(boolean activated) {
		CartridgeAgentEventPublisher.activated = activated;
	}

	public static boolean isReadyToShutdown() {
		return readyToShutdown;
	}

	public static void setReadyToShutdown(boolean readyToShutdown) {
		CartridgeAgentEventPublisher.readyToShutdown = readyToShutdown;
	}

	public static boolean isMaintenance() {
		return maintenance;
	}

	public static void setMaintenance(boolean maintenance) {
		CartridgeAgentEventPublisher.maintenance = maintenance;
	}
}
