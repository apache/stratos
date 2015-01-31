/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.mock.iaas.event.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;

/**
 * Mock member event publisher.
 */
public class MockMemberEventPublisher {

    private static final Log log = LogFactory.getLog(MockMemberEventPublisher.class);

    public static void publishInstanceStartedEvent(MockInstanceContext mockMemberContext) {
        if (log.isInfoEnabled()) {
            log.info("Publishing instance started event");
        }
        InstanceStartedEvent event = new InstanceStartedEvent(
                mockMemberContext.getApplicationId(),
                mockMemberContext.getServiceName(),
                mockMemberContext.getClusterId(),
                mockMemberContext.getMemberId(),
                mockMemberContext.getClusterInstanceId(),
                mockMemberContext.getNetworkPartitionId(),
                mockMemberContext.getPartitionId());
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool
                .getPublisher(topic);
        eventPublisher.publish(event);
        if (log.isInfoEnabled()) {
            log.info("Instance started event published");
        }
    }

    public static void publishInstanceActivatedEvent(MockInstanceContext mockMemberContext) {
        if (log.isInfoEnabled()) {
            log.info("Publishing instance activated event");
        }
        InstanceActivatedEvent event = new InstanceActivatedEvent(
                mockMemberContext.getServiceName(),
                mockMemberContext.getClusterId(),
                mockMemberContext.getMemberId(),
                mockMemberContext.getInstanceId(),
                mockMemberContext.getClusterInstanceId(),
                mockMemberContext.getNetworkPartitionId(),
                mockMemberContext.getPartitionId());

        // Event publisher connection will
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
        if (log.isInfoEnabled()) {
            log.info("Instance activated event published");
        }
    }

    public static void publishInstanceReadyToShutdownEvent(MockInstanceContext mockMemberContext) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing instance ready to shutdown event: [member-id] %s",
                    mockMemberContext.getMemberId()));
        }
        InstanceReadyToShutdownEvent event = new InstanceReadyToShutdownEvent(
                mockMemberContext.getServiceName(),
                mockMemberContext.getClusterId(),
                mockMemberContext.getMemberId(),
                mockMemberContext.getClusterInstanceId(),
                mockMemberContext.getNetworkPartitionId(),
                mockMemberContext.getPartitionId());
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool
                .getPublisher(topic);
        eventPublisher.publish(event);
        if (log.isInfoEnabled()) {
            log.info(String.format("Instance ready to shutDown event published: [member-id] %s",
                    mockMemberContext.getMemberId()));
        }
    }

    public static void publishMaintenanceModeEvent(MockInstanceContext mockMemberContext) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing instance maintenance mode event: [member-id] %s",
                    mockMemberContext.getMemberId()));
        }
        InstanceMaintenanceModeEvent event = new InstanceMaintenanceModeEvent(
                mockMemberContext.getServiceName(),
                mockMemberContext.getClusterId(),
                mockMemberContext.getMemberId(),
                mockMemberContext.getClusterInstanceId(),
                mockMemberContext.getNetworkPartitionId(),
                mockMemberContext.getPartitionId());
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);

        if (log.isInfoEnabled()) {
            log.info(String.format("Instance Maintenance mode event published: [member-id] %s",
                    mockMemberContext.getMemberId()));
        }
    }
}
