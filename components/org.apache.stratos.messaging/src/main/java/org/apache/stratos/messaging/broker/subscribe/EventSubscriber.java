/*
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

package org.apache.stratos.messaging.broker.subscribe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.RetryTimer;
import org.apache.stratos.messaging.broker.connect.TopicSubscriberFactory;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Event subscriber for receiving published by event publisher.
 */
public class EventSubscriber implements Runnable {

    private static final Log log = LogFactory.getLog(EventSubscriber.class);
    private final org.apache.stratos.messaging.broker.connect.TopicSubscriber topicSubscriber;

    private final String topicName;
    private boolean subscribed;

    /**
     * @param topicName topic name of this subscriber instance.
     */
    public EventSubscriber(String topicName, MessageListener messageListener) {
        this.topicName = topicName;
        String protocol = MessagingUtil.getMessagingProtocol();
        this.topicSubscriber = TopicSubscriberFactory.createTopicSubscriber(protocol, messageListener, topicName);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Topic subscriber created: [protocol] %s [topic] %s", protocol, topicName));
        }
    }

    private void doSubscribe() throws MqttException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Subscribing to topic: [topic] %s [server] %s",
                    topicName, topicSubscriber.getServerURI()));
        }

        topicSubscriber.connect();
        topicSubscriber.subscribe();
        subscribed = true;
    }

    /**
     * Subscribes to a topic. If for some reason the connection to the topic got
     * lost, this will perform re-subscription periodically, until a connection
     * obtained.
     */
    @Override
    public void run() {

        RetryTimer retryTimer = new RetryTimer();
        while (!subscribed) {
            try {
                doSubscribe();
            } catch (Exception e) {
                subscribed = false;
                if (log.isErrorEnabled()) {
                    log.error("Error while subscribing to topic: " + topicName, e);
                }

                long interval = retryTimer.getNextInterval();
                if (log.isInfoEnabled()) {
                    log.info("Will try to subscribe again in " + interval / 1000 + " sec");
                }
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    /**
     * Terminate topic subscriber.
     */
    public void terminate() {
        if (topicSubscriber != null) {
            topicSubscriber.disconnect();
        }
    }

    public boolean isSubscribed() {
        return subscribed;
    }
}
