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

import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpAddedEvent;
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpRemovedEvent;
import org.apache.stratos.messaging.event.application.signup.CompleteApplicationSignUpsEvent;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.List;

/**
 * Application signup event publisher.
 */
public class ApplicationSignUpEventPublisher {

    private static void publish(Event event) {
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }

    public static void publishCompleteApplicationSignUpsEvent(List<ApplicationSignUp> applicationSignUps) {

        CompleteApplicationSignUpsEvent completeApplicationSignUpsEvent = new CompleteApplicationSignUpsEvent(
                applicationSignUps);
        publish(completeApplicationSignUpsEvent);
    }

    public static void publishApplicationSignUpAddedEvent(String applicationId, int tenantId, List<String> clusterIds) {

        ApplicationSignUpAddedEvent applicationSignUpAddedEvent = new ApplicationSignUpAddedEvent(
                applicationId, tenantId, clusterIds);
        publish(applicationSignUpAddedEvent);
    }

    public static void publishApplicationSignUpRemovedEvent(String applicationId, int tenantId) {

        ApplicationSignUpRemovedEvent applicationSignUpRemovedEvent = new ApplicationSignUpRemovedEvent(
                applicationId, tenantId);
        publish(applicationSignUpRemovedEvent);
    }
}
