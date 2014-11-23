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
import org.apache.stratos.messaging.broker.connect.TopicSubscriber;
import org.apache.stratos.messaging.broker.connect.mqtt.MqttTopicSubscriber;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Any instance who needs to subscribe to a topic, should communicate with this
 * object.
 */
public class Subscriber implements Runnable {

    private static final Log log = LogFactory.getLog(Subscriber.class);
    private final TopicSubscriber topicSubscriber;

	private final String topicName;
	private boolean subscribed;

	/**
	 * @param topicName topic name of this subscriber instance.
	 */
	public Subscriber(String topicName, MessageListener messageListener) {
		this.topicName = topicName;
        this.topicSubscriber = new MqttTopicSubscriber(messageListener, topicName);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Topic subscriber created: [topic] %s", topicName));
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
		// Keep the thread live until terminated
        while (!subscribed) {
            try {
                doSubscribe();
            } catch (Exception e) {
                subscribed = false;
                if(log.isErrorEnabled()) {
                    log.error("Error while subscribing to topic: " + topicName, e);
                }

                if (log.isInfoEnabled()) {
                    log.info("Will try to subscribe again in " +
                            Util.getFailoverPingInterval() / 1000 + " sec");
                }
                try {
                    Thread.sleep(Util.getFailoverPingInterval());
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

	/**
	 * Terminate topic subscriber.
	 */
	public void terminate() {
        if(topicSubscriber != null) {
            topicSubscriber.disconnect();
        }
    }

	public boolean isSubscribed() {
		return subscribed;
	}
}
