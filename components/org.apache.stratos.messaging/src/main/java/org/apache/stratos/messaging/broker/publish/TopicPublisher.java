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

package org.apache.stratos.messaging.broker.publish;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.MQTTConnector;
import org.apache.stratos.messaging.util.Constants;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

/**
 * Any instance who needs to publish data to a topic, should communicate with
 * this
 * object. This Topic publisher is responsible to convert the POJO to be
 * published
 * to JSON format, before publishing.
 * 
 * 
 * 
 */
public class TopicPublisher {

    private static final Log log = LogFactory.getLog(TopicPublisher.class);

    /**
     * Quality of Service for message delivery:
     * Setting it to 2 to make sure that message is guaranteed to deliver once
     * using two-phase acknowledgement across the network.
     */
	private static final int QOS = 2;
    public static final int PUBLISH_RETRY_INTERVAL = 60000;

    public static TopicPublisher topicPub;
	private boolean initialized;
	private final String topic;
	MqttClient mqttClient;

	/**
	 * @param aTopicName topic name of this publisher instance.
	 */
	TopicPublisher(String aTopicName) {
		this.topic = aTopicName;
		if (log.isDebugEnabled()) {
			log.debug(String.format("Topic publisher connector created: [topic] %s", topic));
		}
	}

	/**
	 * Publishes to a topic. If for some reason the connection to the topic got
	 * lost, this will perform re-subscription periodically, until a connection
	 * obtained.
	 */

	public void publish(Object messageObj, boolean retry) {
		synchronized (TopicPublisher.class) {
            Gson gson = new Gson();
            String message = gson.toJson(messageObj);
            boolean published = false;

            while (!published) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String clientId = timestamp + RandomStringUtils.random(Constants.CLIENT_ID_MAX_LENGTH - timestamp.length());
                mqttClient = MQTTConnector.getMqttClient(clientId);
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                // Set quality of service
                mqttMessage.setQos(QOS);

                try {
                    mqttClient.publish(topic, mqttMessage);
                    published = true;
                } catch (Exception e) {
                    initialized = false;
                    if (!retry) {
                        if (log.isDebugEnabled()) {
                            log.debug("Retry disabled for topic " + topic);
                        }
                        throw new RuntimeException(e);
                    }

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Will try to re-publish in %d sec", (PUBLISH_RETRY_INTERVAL/1000)));
                    }
                    try {
                        Thread.sleep(PUBLISH_RETRY_INTERVAL);
                    } catch (InterruptedException ignore) {
                    }
                } finally {
                    try {
                        mqttClient.disconnect();
                    } catch (MqttException ignore) {
                    }
                }
            }
        }
	}
}
