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

package org.apache.stratos.messaging.broker.connect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.exception.MessagingException;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.util.Properties;

/**
 * This class is responsible for loading the mqtt config file from the
 * classpath
 * Initialize the topic connection.
 * and initialize the topic connection. Later if some other object needs a topic
 * session, this object is capable of providing one.
 */
public class MQTTConnector {

    public static final String MQTT_URL_DEFAULT = "defaultValue";

    private static final Log log = LogFactory.getLog(MQTTConnector.class);
    private static String configFileLocation = System.getProperty("jndi.properties.dir");
    private static Properties mqttProperties = Util.getProperties(configFileLocation
            + File.separator + "mqtttopic.properties");

    public static MqttClient getMqttClient() {
        try {
            String mqttUrl = mqttProperties.getProperty("mqtturl", MQTT_URL_DEFAULT);
            MemoryPersistence memoryPersistence = new MemoryPersistence();
            String clientId = Util.getRandomString(23);
            MqttClient mqttClient = new MqttClient(mqttUrl, clientId, memoryPersistence);
            if (log.isDebugEnabled()) {
                log.debug("MQTT client created: [client-id] " + clientId);
            }
            return mqttClient;
        } catch (Exception e) {
            String message = "Could not create MQTT client";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }
}
