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

package org.apache.stratos.messaging.broker.connect.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicPublisher;
import org.apache.stratos.messaging.domain.exception.MessagingException;

import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSession;

/**
 * AMQP topic publisher.
 */
public class AmqpTopicPublisher extends AmqpTopicConnector implements TopicPublisher {

    private static final Log log = LogFactory.getLog(AmqpTopicConnector.class);

    public AmqpTopicPublisher() {
        create();
    }

    @Override
    public void publish(String topicName, String message) {
        try {
            TopicSession topicSession = newSession();
            Topic topic = lookupTopic(topicName);
            if (topic == null) {
                // if the topic doesn't exist, create it.
                topic = topicSession.createTopic(topicName);
            }
            javax.jms.TopicPublisher topicPublisher = topicSession.createPublisher(topic);
            TextMessage textMessage = topicSession.createTextMessage(message);
            topicPublisher.publish(textMessage);
        } catch (Exception e) {
            String errorMessage = "Could not publish to topic: [topic-name] %s";
            log.error(errorMessage, e);
            throw new MessagingException(errorMessage, e);
        }
    }
}
