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
import org.apache.stratos.messaging.broker.connect.MqttTopicSubscriber;
import org.apache.stratos.messaging.broker.connect.TopicSubscriber;
import org.apache.stratos.messaging.broker.heartbeat.TopicHealthChecker;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.MqttException;

import javax.jms.JMSException;

/**
 * Any instance who needs to subscribe to a topic, should communicate with this
 * object.
 *
 * @author nirmal
 */
public class Subscriber implements Runnable {

    private static final Log log = LogFactory.getLog(Subscriber.class);
    private final TopicSubscriber topicSubscriber;

    private boolean terminated = false;
	private final String topicName;

	private TopicHealthChecker healthChecker;
	private final javax.jms.TopicSubscriber jmsTopicSubscriber = null;
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

		/* Subscribing to specific topic */
        while(true) {
            try {
                topicSubscriber.connect();
                topicSubscriber.subscribe();
                subscribed = true;
                // Continue waiting for messages
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            } finally {
                topicSubscriber.disconnect();
            }
        }
		
	}

//	/**
//	 * @param messageListener this MessageListener will get triggered each time this
//	 *                        subscription receives a message.
//	 */
//	public void setMessageListener(MessageListener messageListener) {
//		this.messageListener = messageListener;
//	}

	/**
	 * Subscribes to a topic. If for some reason the connection to the topic got
	 * lost, this will perform re-subscription periodically, until a connection
	 * obtained.
	 */
	@Override
	public void run() {

		// Keep the thread live until terminated
		while (!terminated) {
			try {
				doSubscribe();
			} catch (Exception e) {
				subscribed = false;
				log.error("Error while subscribing to the topic: " + topicName, e);
			} finally {
				if (subscribed) {
					// start the health checker if subscribed
					healthChecker = new TopicHealthChecker(topicName);
					Thread healthCheckerThread = new Thread(healthChecker);
					healthCheckerThread.start();
					try {
						// waits till the thread finishes.
						healthCheckerThread.join();
					} catch (InterruptedException ignore) {
					}
				} else {
					// subscription failed
					if (log.isInfoEnabled()) {
						log.info("Will try to subscribe again in " +
						         Util.getFailoverPingInterval() / 1000 + " sec");
					}
					try {
						Thread.sleep(Util.getFailoverPingInterval());
					} catch (InterruptedException ignore) {
					}
				}
				// closes all sessions/connections
				try {
					if (jmsTopicSubscriber != null) {
						jmsTopicSubscriber.close();
						if (log.isDebugEnabled()) {
							log.debug(String.format("Topic subscriber closed: [topic] %s",
							                        topicName));
						}
					}
				} catch (JMSException ignore) {
				}
			}
		}
	}

	/**
	 * Terminate topic subscriber.
	 */
	public void terminate() {
		healthChecker.terminate();
		terminated = true;
	}

	public boolean isSubscribed() {
		return subscribed;
	}
}
