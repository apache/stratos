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

    public static final String MQTTURL = "defaultValue";
    public static final String CLIENT_ID = "Startos";
    public static final String TMPFILELOCATION = "/tmp";
    private static MqttClient topicClient;

    private static MqttClient topicClientSub;
    private static final Log log = LogFactory.getLog(MQTTConnector.class);
    private static String configFileLocation = System.getProperty("jndi.properties.dir");
    private static Properties mqttProp =
            Util.getProperties(configFileLocation + File.separator +
                    "mqtttopic.properties");

    public static synchronized MqttClient getMQTTConClient() {

        if (topicClient == null) {


            String broker = mqttProp.getProperty("mqtturl", MQTTURL);

            String clientId = mqttProp.getProperty("clientID", CLIENT_ID);
            MemoryPersistence persistence = new MemoryPersistence();

            try {
                topicClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                if (log.isDebugEnabled()) {
                    log.debug("MQTT client connected");
                }

            } catch (MqttException me) {

                log.error("Failed to initiate autoscaler service client. ", me);
            }

        }
        return topicClient;

    }

    public static synchronized MqttClient getMQTTSubClient(String identifier) {
        // if (topicClientSub == null) {

        String broker = mqttProp.getProperty("mqtturl", MQTTURL);

        String tempFile = mqttProp.getProperty("tempfilelocation", TMPFILELOCATION);
        // Creating new default persistence for mqtt client
        MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(tempFile);

        try {
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            // mqtt client with specific url and a random client id
            topicClientSub = new MqttClient(broker, identifier, persistence);

            if (log.isDebugEnabled()) {
                log.debug("MQTT client connected");
            }

        } catch (MqttException me) {

            log.error("Failed to initiate autoscaler service client. ", me);
        }

        // }
        return topicClientSub;

    }
}
