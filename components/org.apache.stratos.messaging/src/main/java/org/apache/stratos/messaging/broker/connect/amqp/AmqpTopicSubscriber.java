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

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.RetryTimer;
import org.apache.stratos.messaging.broker.connect.TopicSubscriber;
import org.apache.stratos.messaging.broker.subscribe.MessageListener;
import org.apache.stratos.messaging.domain.exception.MessagingException;

import javax.jms.*;

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
                        String topicName = null, messageText = null;

                        if (message instanceof ActiveMQTextMessage) {
                            ActiveMQTextMessage textMessage = (ActiveMQTextMessage) message;
                            topicName = textMessage.getDestination().getPhysicalName();
                            messageText = textMessage.getText();
                        } else if (message instanceof ActiveMQBytesMessage) {
                            ActiveMQBytesMessage bytesMessage = (ActiveMQBytesMessage) message;
                            topicName = bytesMessage.getDestination().getPhysicalName();
                            messageText = new String(bytesMessage.getContent().data);
                        } else {
                            throw new RuntimeException(String.format("Could not receive message, " +
                                    "unknown JMS message type: %s", message.getClass().getName()));
                        }
                        org.apache.stratos.messaging.domain.Message message_ =
                                new org.apache.stratos.messaging.domain.Message(topicName, messageText);
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

    @Override
    protected void reconnect() {
        RetryTimer retryTimer = new RetryTimer();
        boolean connected = false;
        while (!connected) {
            try {
                long interval = retryTimer.getNextInterval();
                log.info(String.format("Topic subscriber will try to reconnect in %d seconds: [topic-name] %s",
                        (interval / 1000), topicName));
                Thread.sleep(interval);
            } catch (InterruptedException ignore) {
            }

            try {
                disconnect();
                create();
                connect();
                subscribe();
                connected = true;
                log.info(String.format("Topic subscriber reconnected: [topic-name] %s", topicName));
            } catch (Exception e) {
                String message = "Could not reconnect to message broker";
                log.warn(message, e);
            }
        }
    }
}
