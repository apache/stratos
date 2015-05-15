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

package org.apache.stratos.cartridge.agent.event.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.statistics.publisher.HealthStatisticsNotifier;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.List;

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
                    //application_id = CartridgeAgentConfiguration().application_id
                    CartridgeAgentConfiguration.getInstance().getApplicationId(),
                    //service_name = CartridgeAgentConfiguration().service_name
                    CartridgeAgentConfiguration.getInstance().getServiceName(),
                    //cluster_id = CartridgeAgentConfiguration().cluster_id
                    CartridgeAgentConfiguration.getInstance().getClusterId(),
                    // member_id = CartridgeAgentConfiguration().member_id
                    CartridgeAgentConfiguration.getInstance().getMemberId(),

                    //cluster_instance_id = CartridgeAgentConfiguration().cluster_instance_id
                    CartridgeAgentConfiguration.getInstance().getClusterInstanceId(),
                    //network_partition_id = CartridgeAgentConfiguration().network_partition_id
                    CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                    //partition_id = CartridgeAgentConfiguration().partition_id
                    CartridgeAgentConfiguration.getInstance().getPartitionId());

			/*
             *
        
        public InstanceStartedEvent(String applicationId, String serviceName, String clusterId, String memberId,
                                String clusterInstanceId, String networkPartitionId, String partitionId)
       
        //instance_id = CartridgeAgentConfiguration().instance_id
        
        
        

        instance_started_event = InstanceStartedEvent(application_id, service_name, cluster_id, cluster_instance_id, member_id,
                                                      instance_id, network_partition_id, partition_id)
        publisher = get_publisher(cartridgeagentconstants.INSTANCE_STATUS_TOPIC + cartridgeagentconstants.INSTANCE_STARTED_EVENT)
        publisher.publish(instance_started_event)
        started = True
        log.info("Instance started event published")
			 */

            String topic = MessagingUtil.getMessageTopicName(event);
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
            // Wait for all ports to be active, if ports are not activated, do not publish instance activated since
            // the service is not up
            List<Integer> ports = CartridgeAgentConfiguration.getInstance().getPorts();
            String listenAddress = CartridgeAgentConfiguration.getInstance().getListenAddress();
            boolean portsActivated = CartridgeAgentUtils.waitUntilPortsActive(listenAddress, ports);

            if (portsActivated) {
                if (log.isInfoEnabled()) {
                    log.info("Publishing instance activated event");
                }
                InstanceActivatedEvent event = new InstanceActivatedEvent(
                        CartridgeAgentConfiguration.getInstance().getServiceName(),
                        CartridgeAgentConfiguration.getInstance().getClusterId(),
                        CartridgeAgentConfiguration.getInstance().getMemberId(),
                        CartridgeAgentConfiguration.getInstance().getInstanceId(),
                        CartridgeAgentConfiguration.getInstance().getClusterInstanceId(),
                        CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                        CartridgeAgentConfiguration.getInstance().getPartitionId());

                // Event publisher connection will
                String topic = MessagingUtil.getMessageTopicName(event);
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
                if (log.isInfoEnabled()) {
                    // There would not be a large number of ports
                    String portsStr = "";
                    for (Integer port : ports) {
                        portsStr += port + ", ";
                    }
                    log.error(String.format(
                            "Ports activation timed out. Aborting InstanceActivatedEvent publishing. [IPAddress] %s [Ports] %s",
                            listenAddress,
                            portsStr));
                }
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
                    CartridgeAgentConfiguration.getInstance().getMemberId(),
                    CartridgeAgentConfiguration.getInstance().getClusterInstanceId(),
                    CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                    CartridgeAgentConfiguration.getInstance().getPartitionId());
            String topic = MessagingUtil.getMessageTopicName(event);
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
                    CartridgeAgentConfiguration.getInstance().getMemberId(),
                    CartridgeAgentConfiguration.getInstance().getClusterInstanceId(),
                    CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                    CartridgeAgentConfiguration.getInstance().getPartitionId());
            String topic = MessagingUtil.getMessageTopicName(event);
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
