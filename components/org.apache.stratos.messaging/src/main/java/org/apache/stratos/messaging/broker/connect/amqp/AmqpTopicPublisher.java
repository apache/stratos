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
import org.apache.stratos.messaging.broker.connect.RetryTimer;
import org.apache.stratos.messaging.broker.connect.TopicPublisher;
import org.apache.stratos.messaging.domain.exception.MessagingException;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSession;

/**
 * AMQP topic publisher.
 */
public class AmqpTopicPublisher extends AmqpTopicConnector implements TopicPublisher {

    private static final Log log = LogFactory.getLog(AmqpTopicConnector.class);

    private final String topicName;

    private enum ConnectionStatus {Connected, ReConnecting, ReConnected}

    private ConnectionStatus connectionStatus;

    public AmqpTopicPublisher(String topicName) {
        this.topicName = topicName;
        create();
    }

    /**
     * Publish message to message broker.
     *
     * @param message Message to be published
     * @param retry   Retry if message broker is not available
     */
    @Override
    public void publish(String message, boolean retry) {
        boolean published = false;
        while (!published) {
            TopicSession topicSession = null;
            javax.jms.TopicPublisher topicPublisher = null;
            try {
                while (connectionStatus == ConnectionStatus.ReConnecting) {
                    // Connection has been broken, wait until reconnected
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                }

                if (connectionStatus == ConnectionStatus.ReConnected) {
                    // Publisher has reconnected, wait another 2 seconds to make sure all subscribers
                    // have been reconnected to receive the message
                    Thread.sleep(2000);
                    connectionStatus = ConnectionStatus.Connected;
                }

                topicSession = newSession();
                Topic topic = lookupTopic(topicName);
                if (topic == null) {
                    // if the topic doesn't exist, create it.
                    topic = topicSession.createTopic(topicName);
                }
                topicPublisher = topicSession.createPublisher(topic);
                TextMessage textMessage = topicSession.createTextMessage(message);
                topicPublisher.publish(textMessage);
                published = true;
            } catch (Exception e) {
                String errorMessage = "Could not publish to topic: [topic-name] %s";
                log.error(errorMessage, e);
                if (!retry) {
                    // Retry is disabled, throw exception
                    throw new MessagingException(errorMessage, e);
                }
                // Try to reconnect
                reconnect();
            }finally {

                try{
                    if (topicSession != null) {
                        topicSession.close();
                    }
                    if (topicPublisher != null) {
                        topicPublisher.close();
                    }

                }catch (JMSException e) {
                    message = "Error cleaning up pubisher";
                    log.error(message, e);
                    throw new MessagingException(message, e);
                }

            }
        }
    }

    @Override
    protected void reconnect() {
        connectionStatus = ConnectionStatus.ReConnecting;
        RetryTimer retryTimer = new RetryTimer();
        while (connectionStatus == ConnectionStatus.ReConnecting) {
            try {
                long interval = retryTimer.getNextInterval();
                log.info(String.format("Topic publisher will try to reconnect in %d seconds: [topic-name] %s",
                        (interval / 1000), topicName));
                Thread.sleep(interval);
            } catch (InterruptedException ignore) {
            }

            try {
                disconnect();
                create();
                connect();
                connectionStatus = ConnectionStatus.ReConnected;
                log.info(String.format("Topic publisher reconnected: [topic-name] %s", topicName));
            } catch (Exception e) {
                String message = "Could not reconnect to message broker";
                log.warn(message, e);
            }
        }
    }
}
