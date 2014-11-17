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
import org.apache.stratos.messaging.domain.exception.MessagingException;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


/**
 * Mqtt topic connector implementation.
 */
public class MqttTopicPublisher implements TopicPublisher {

    protected static final Log log = LogFactory.getLog(MqttTopicPublisher.class);

    private final MqttClient mqttClient;

    public MqttTopicPublisher() {
        try {
            String mqttUrl = MqttConstants.MQTT_PROPERTIES.getProperty("mqtturl", MqttConstants.MQTT_URL_DEFAULT);
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

    public void create() {

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
            synchronized (mqttClient) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
            }
        } catch (Exception e) {
            String message = "Could not disconnect from message broker";
            log.error(message, e);
        }
    }

    public void publish(String topicName, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(MqttConstants.QOS);
            mqttClient.publish(topicName, mqttMessage);
        } catch (Exception e) {
            String errorMessage = "Could not publish message: " + message;
            log.error(errorMessage, e);
            throw new MessagingException(errorMessage, e);
        }
    }
}
