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

import javax.jms.JMSException;
import javax.jms.TopicSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.MQTTConnector;
import org.apache.stratos.messaging.broker.heartbeat.TopicHealthChecker;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;
import org.apache.stratos.messaging.message.processor.instance.notifier.InstanceNotifierMessageProcessorChain;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;

/**
 * Any instance who needs to subscribe to a topic, should communicate with this
 * object.
 * 
 * @author nirmal
 * 
 */
public class TopicSubscriber implements Runnable {

	private static final Log log = LogFactory.getLog(TopicSubscriber.class);

	private boolean terminated = false;
	private MqttCallback messageListener;
	private TopicSession topicSession;
	private final String topicName;

	private TopicHealthChecker healthChecker;
	private final javax.jms.TopicSubscriber topicSubscriber = null;
	private boolean subscribed;
	private final MessageProcessorChain processorChain;

	/**
	 * @param aTopicName
	 *            topic name of this subscriber instance.
	 */
	public TopicSubscriber(String aTopicName) {
		topicName = aTopicName;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Topic subscriber connector created: [topic] %s", topicName));
		}
		this.processorChain = new InstanceNotifierMessageProcessorChain();
	}

	private void doSubscribe() throws Exception, JMSException {

		MqttClient mqttClient = MQTTConnector.getMQTTSubClient(Util.getRandomString(5));
		try {

			mqttClient.connect();
			if (log.isDebugEnabled()) {
				log.debug("Subscribing to topic '" + topicName + "' from " +
				          mqttClient.getServerURI());
			}
			// Subscribing to specific topic

			mqttClient.subscribe(topicName);

			// Continue waiting for messages until the Enter is pressed
			mqttClient.setCallback(messageListener);
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

		} finally {
			mqttClient.disconnect();
		}
	}

	/**
	 * @param messageListener
	 *            this MessageListener will get triggered each time this
	 *            subscription receives a message.
	 */
	public void setMessageListener(MqttCallback messageListener) {

		this.messageListener = messageListener;
	}

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
					if (topicSubscriber != null) {
						topicSubscriber.close();
						if (log.isDebugEnabled()) {
							log.debug(String.format("Topic subscriber closed: [topic] %s",
							                        topicName));
						}
					}
					if (topicSession != null) {
						topicSession.close();
						if (log.isDebugEnabled()) {
							log.debug(String.format("Topic subscriber session closed: [topic] %s",
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
