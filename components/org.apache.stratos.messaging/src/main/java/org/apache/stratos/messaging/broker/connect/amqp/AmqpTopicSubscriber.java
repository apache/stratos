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

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicSubscriber;
import org.apache.stratos.messaging.broker.subscribe.MessageListener;
import org.apache.stratos.messaging.domain.exception.MessagingException;

import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicSession;

/**
 * AMQP topic subscriber.
 */
public class AmqpTopicSubscriber extends AmqpTopicConnector implements TopicSubscriber {

    protected static final Log log = LogFactory.getLog(AmqpTopicSubscriber.class);

    private final MessageListener messageListener;
    private final String topicName;

    public AmqpTopicSubscriber(MessageListener messageListener, String topicName) {
        this.messageListener = messageListener;
        this.topicName = topicName;
        create();
    }

    @Override
    public void subscribe() {
        try {
            TopicSession topicSession = newSession();
            Topic topic = lookupTopic(topicName);
            if (topic == null) {
                // if topic doesn't exist, create it.
                topic = topicSession.createTopic(topicName);
            }
            javax.jms.TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
            topicSubscriber.setMessageListener(new javax.jms.MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        ActiveMQTextMessage activeMQTextMessage = (ActiveMQTextMessage) message;
                        String topicName = activeMQTextMessage.getDestination().getPhysicalName();
                        org.apache.stratos.messaging.domain.Message message_ =
                                new org.apache.stratos.messaging.domain.Message(topicName, activeMQTextMessage.getText());
                        messageListener.messageReceived(message_);
                    } catch (Exception e) {
                        String error = "An error occurred when receiving message";
                        log.error(error, e);
                    }
                }
            });
        } catch (Exception e) {
            String message = "Could not subscribe to topic: " + topicName;
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }
}
