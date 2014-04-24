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

package org.apache.stratos.messaging.broker.publish;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.publish.MessagePublisher;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSession;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Any instance who needs to publish data to a topic, should communicate with
 * this
 * object. This Topic publisher is responsible to convert the POJO to be
 * published
 * to JSON format, before publishing.
 * 
 * @author nirmal
 * 
 */
public class TopicPublisher extends MessagePublisher {

	private static final Log log = LogFactory.getLog(TopicPublisher.class);
	private TopicSession topicSession;
	private TopicConnector connector;
	private javax.jms.TopicPublisher topicPublisher = null;
    private boolean initialized;

	/**
	 * @param aTopicName
	 *            topic name of this publisher instance.
	 */
	TopicPublisher(String aTopicName) {
		super(aTopicName);
		connector = new TopicConnector();
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic publisher connector created: [topic] %s", getName()));
        }
	}

	/**
	 * Publishes to a topic. If for some reason the connection to the topic got
	 * lost, this will perform re-subscription periodically, until a connection
	 * obtained.
	 */
	public void publish(Object messageObj) {
		publish(messageObj, null);
	}
	
	public void publish(Object messageObj, Properties headers) {
        synchronized (TopicPublisher.class) {
            Gson gson = new Gson();
            String message = gson.toJson(messageObj);
            boolean published = false;
            while(!published) {

                try {
                    doPublish(message, headers);
                    published = true;
                } catch (Exception e) {
                    initialized = false;
                    if(log.isErrorEnabled()) {
                        log.error("Error while publishing to the topic: " + getName(), e);
                    }
                    if(log.isInfoEnabled()) {
                        log.info("Will try to re-publish in 60 sec");
                    }
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
	}

	public void close() {
        synchronized (TopicPublisher.class) {
            // closes all sessions/connections
            try {
                if(topicPublisher != null) {
                    topicPublisher.close();
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Topic publisher closed: [topic] %s", getName()));
                    }
                }
                if(topicSession != null) {
                    topicSession.close();
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Topic publisher session closed: [topic] %s", getName()));
                    }
                }
                if(connector != null) {
                    connector.close();
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Topic publisher connector closed: [topic] %s", getName()));
                    }
                }
            } catch (JMSException ignore) {
            }
        }
	}

	private void doPublish(String message, Properties headers) throws Exception, JMSException {
        if(!initialized) {
            // Initialize a topic connection to the message broker
            connector.init(getName());
            initialized = true;
            if(log.isDebugEnabled()) {
                log.debug(String.format("Topic publisher connector initialized: [topic] %s", getName()));
            }
        }

        try {
        // Create a new session
        topicSession = createSession(connector);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic publisher session created: [topic] %s", getName()));
        }
        // Create a publisher from session
        topicPublisher = createPublisher(topicSession);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Topic publisher created: [topic] %s", getName()));
        }

        // Create text message
        TextMessage textMessage = topicSession.createTextMessage(message);
		
		if (headers != null) {
            // Add header properties
			@SuppressWarnings("rawtypes")
			Enumeration e = headers.propertyNames();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				textMessage.setStringProperty(key, headers.getProperty(key));
			}
		}

		topicPublisher.publish(textMessage);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Message published: [topic] %s [header] %s [body] %s", getName(), (headers != null) ? headers.toString() : "null", message));
        }
        }
        finally {
            if(topicPublisher != null) {
                topicPublisher.close();
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Topic publisher closed: [topic] %s", getName()));
                }
            }
            if(topicSession != null) {
                topicSession.close();
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Topic publisher session closed: [topic] %s", getName()));
                }
            }
        }
    }

    private TopicSession createSession(TopicConnector topicConnector) throws Exception {
        // Create a new session
        return topicConnector.newSession();
    }

	private javax.jms.TopicPublisher createPublisher(TopicSession topicSession) throws Exception, JMSException {
        Topic topic = connector.getTopic();
		if (topic == null) {
			// if the topic doesn't exist, create it.
			topic = topicSession.createTopic(getName());
		}
		return topicSession.createPublisher(topic);
	}
}
