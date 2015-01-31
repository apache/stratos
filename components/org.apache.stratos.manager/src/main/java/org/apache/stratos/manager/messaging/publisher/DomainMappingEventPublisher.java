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

package org.apache.stratos.manager.messaging.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingRemovedEvent;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Domain mapping event publisher.
 */
public class DomainMappingEventPublisher {

    private static final Log log = LogFactory.getLog(DomainMappingEventPublisher.class);

    private static void publish(Event event) {
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }

    public static void publishDomainMappingAddedEvent(String applicationId, int tenantId, String serviceName,
                                                      String clusterId, String domainName, String contextPath) {
        DomainMappingAddedEvent domainMappingAddedEvent = new DomainMappingAddedEvent(applicationId, tenantId,
                serviceName, clusterId, domainName, contextPath);
        publish(domainMappingAddedEvent);

        if(log.isInfoEnabled()) {
            log.info(String.format("Domain mapping added event published: %s", domainMappingAddedEvent.toString()));
        }
    }

    public static void publishDomainNameRemovedEvent(String applicationId, int tenantId, String serviceName,
                                                     String clusterId, String domainName) {
        DomainMappingRemovedEvent domainNameRemovedEvent = new DomainMappingRemovedEvent(applicationId, tenantId,
                serviceName, clusterId, domainName);
        publish(domainNameRemovedEvent);

        if(log.isInfoEnabled()) {
            log.info(String.format("Domain mapping removed event published: %s", domainNameRemovedEvent.toString()));
        }
    }
}
