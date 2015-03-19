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

package org.apache.stratos.messaging.broker.connect.mqtt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.domain.exception.MessagingException;
import org.apache.stratos.messaging.util.MessagingConstants;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * MQTT topic connector implementation.
 */
public abstract class MqttTopicConnector implements TopicConnector {

    private static final Log log = LogFactory.getLog(MqttTopicConnector.class);

    protected MqttClient mqttClient;

    /**
     * Connect to message broker using MQTT client object created.
     */
    @Override
    public void connect() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Connecting to message broker");
            }

            if (mqttClient == null) {
                if (log.isDebugEnabled()) {
                    log.debug("MQTT client initialization has failed previously, trying again");
                }
                create();
            }

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            // Do not maintain a session between the client and the server since it is nearly impossible to
            // generate a unique client id for each subscriber & publisher with the distributed nature of stratos.
            // Reliable message delivery is managed by topic subscriber and publisher.
            connectOptions.setCleanSession(true);
            // TODO: test this
            // set the keep alive interval less than MB's inactive connection detection time
            //connectOptions.setKeepAliveInterval(15);
            mqttClient.connect(connectOptions);
        } catch (Exception e) {
            String message = "Could not connect to message broker";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }

    /**
     * Create MQTT client object with required configuration.
     */
    @Override
    public void create() {

        try {
            String mqttUrl = System.getProperty("mqtturl");
            if (StringUtils.isBlank(mqttUrl)) {
                mqttUrl = MessagingConstants.MQTT_PROPERTIES.getProperty("mqtturl", MessagingConstants.MQTT_URL_DEFAULT);
            }
            MemoryPersistence memoryPersistence = new MemoryPersistence();
            String clientId = MessagingUtil.getRandomString(23);
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

    /**
     * Disconnect from message broker and close the connection.
     */
    @Override
    public void disconnect() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Disconnecting from message broker");
            }

            if (mqttClient == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Could not disconnect from message broker, MQTT client has not been initialized");
                }
                return;
            }

            synchronized (mqttClient) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                closeConnection();
            }
        } catch (Exception e) {
            String errorMsg = "Error in disconnecting from Message Broker";
            log.error(errorMsg, e);
        }
    }

    private void closeConnection() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Closing connection to message broker");
            }

            if (mqttClient == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Could not close connection, MQTT client has not been initialized");
                }
                return;
            }

            mqttClient.close();
        } catch (Exception e) {
            String message = "Could not close MQTT client";
            log.error(message, e);
        } finally {
            mqttClient = null;
        }
    }

    /**
     * Return server URI.
     *
     * @return
     */
    @Override
    public String getServerURI() {
        if (mqttClient == null) {
            return null;
        }

        return mqttClient.getServerURI();
    }
}
