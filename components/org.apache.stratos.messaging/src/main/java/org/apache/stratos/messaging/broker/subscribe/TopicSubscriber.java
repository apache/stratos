/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.broker.subscribe;

import javax.jms.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.broker.heartbeat.TopicHealthChecker;

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
    private MessageListener messageListener;
	private TopicSession topicSession;
	private String topicName;
	private TopicConnector connector;
    private TopicHealthChecker healthChecker;
	private javax.jms.TopicSubscriber topicSubscriber = null;
    private boolean subscribed;

    /**
	 * @param aTopicName
	 *            topic name of this subscriber instance.
	 */
	public TopicSubscriber(String aTopicName) {
		topicName = aTopicName;
		connector = new TopicConnector();
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic subscriber connector created: [topic] %s", topicName));
        }
	}

	private void doSubscribe() throws Exception, JMSException {
		// Initialize a topic connection
		connector.init(topicName);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic subscriber connector initialized: [topic] %s", topicName));
        }
        // Create new session
        topicSession = createSession(connector);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic subscriber session created: [topic] %s", topicName));
        }
        // Create a new subscriber
        createSubscriber(topicSession);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic subscriber created: [topic] %s", topicName));
        }
        subscribed = true;
	}

    private void createSubscriber(TopicSession topicSession) throws JMSException {
        Topic topic = connector.getTopic();
        if (topic == null) {
            // if topic doesn't exist, create it.
            topic = topicSession.createTopic(topicName);
        }
        topicSubscriber = topicSession.createSubscriber(topic);
        topicSubscriber.setMessageListener(messageListener);
    }

    private TopicSession createSession(TopicConnector topicConnector) throws Exception {
        // Create a new session
        return topicConnector.newSession();
    }

	/**
	 * @param messageListener
	 *            this MessageListener will get triggered each time this
	 *            subscription receives a message.
	 */
	public void setMessageListener(MessageListener messageListener) {

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
                if(subscribed) {
                    // start the health checker if subscribed
                    healthChecker = new TopicHealthChecker(topicName);
                    Thread healthCheckerThread = new Thread(healthChecker);
                    healthCheckerThread.start();
                    try {
                        // waits till the thread finishes.
                        healthCheckerThread.join();
                    } catch (InterruptedException ignore) {
                    }
                }
                else {
				    // subscription failed
                    if(log.isInfoEnabled()) {
                        log.info("Will try to subscribe again in 30 sec");
                    }
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ignore) {
                    }
                }
                // closes all sessions/connections
                try {
                    if (topicSubscriber != null) {
                        topicSubscriber.close();
                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Topic subscriber closed: [topic] %s", topicName));
                        }
                    }
                    if (topicSession != null) {
                        topicSession.close();
                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Topic subscriber session closed: [topic] %s", topicName));
                        }
                    }
                    if (connector != null) {
                        connector.close();
                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Topic subscriber connector closed: [topic] %s", topicName));
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
