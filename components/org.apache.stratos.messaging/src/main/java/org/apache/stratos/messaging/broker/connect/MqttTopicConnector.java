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

package org.apache.stratos.messaging.broker.connect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.MessageListener;
import org.apache.stratos.messaging.domain.Message;
import org.apache.stratos.messaging.domain.exception.MessagingException;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;
import java.util.Properties;

/**
 * Mqtt topic connector implementation.
 */
public class MqttTopicConnector implements TopicConnector {

    public static final String MQTT_URL_DEFAULT = "defaultValue";

    /**
     * Quality of Service for message delivery:
     * Setting it to 2 to make sure that message is guaranteed to deliver once
     * using two-phase acknowledgement across the network.
     */
    private static final int QOS = 2;
    private static final Log log = LogFactory.getLog(MqttTopicConnector.class);
    private static String configFileLocation = System.getProperty("jndi.properties.dir");
    private static Properties mqttProperties = Util.getProperties(configFileLocation
            + File.separator + "mqtttopic.properties");
    private final MqttClient mqttClient;

    public MqttTopicConnector() {
        try {
            String mqttUrl = mqttProperties.getProperty("mqtturl", MQTT_URL_DEFAULT);
            MemoryPersistence memoryPersistence = new MemoryPersistence();
            String clientId = Util.getRandomString(23);
            mqttClient = new MqttClient(mqttUrl, clientId, memoryPersistence);
            if (log.isDebugEnabled()) {
                log.debug("MQTT client created: [client-id] " + clientId);
            }
        } catch (Exception e) {
            String message = "Could not create MQTT client";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }

    @Override
    public String getServerURI() {
        return mqttClient.getServerURI();
    }

    @Override
    public void connect() {
        try {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            // Do not maintain a session between the client and the server since it is nearly impossible to
            // generate a unique client id for each subscriber & publisher with the distributed nature of stratos.
            // Reliable message delivery is managed by topic subscriber and publisher.
            connectOptions.setCleanSession(true);
            mqttClient.connect(connectOptions);
        } catch (Exception e) {
            String message = "Could not connect to message broker";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }

    @Override
    public void disconnect() {
        try {
            mqttClient.disconnect();
        } catch (Exception e) {
            String message = "Could not disconnect from message broker";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }

    @Override
    public void publish(String topicName, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(QOS);
            mqttClient.publish(topicName, mqttMessage);
        } catch (Exception e) {
            String errorMessage = "Could not publish message: " + message;
            log.error(errorMessage, e);
            throw new MessagingException(errorMessage, e);
        }
    }

    @Override
    public void subscribe(final String topicName, final MessageListener messageListener) {
        try {
            mqttClient.subscribe(topicName);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection to the message broker is lost");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String messageText = new String(message.getPayload());
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Message received: %s", messageText));
                    }
                    messageListener.messageReceived(new Message(topic, messageText));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Message delivery complete: [message-id] %d",
                                token.getMessageId()));
                    }
                }
            });
        } catch (Exception e) {
            String message = "Could not subscribe to topic";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }
}
