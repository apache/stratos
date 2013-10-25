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

import java.util.Enumeration;
import java.util.Properties;

import javax.jms.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.publish.MessagePublisher;

import com.google.gson.Gson;
import org.apache.stratos.messaging.util.Constants;

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

	/**
	 * @param aTopicName
	 *            topic name of this publisher instance.
	 */
	public TopicPublisher(String aTopicName) {
		super(aTopicName);
		connector = new TopicConnector();
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
		
		Gson gson = new Gson();
		String message = gson.toJson(messageObj);
		try {
			doPublish(message, headers);
			
		} catch (Exception e) {
			log.error("Error while publishing to the topic: " + getName(), e);
			// TODO would it be worth to throw this exception?
		}
	}

	public void close() {

		// closes all sessions/connections
		try {
			topicPublisher.close();
			topicSession.close();
			connector.close();
		} catch (JMSException ignore) {
		}
	}

	private void doPublish(String message, Properties headers) throws Exception, JMSException {
		setPublisher();

		TextMessage textMessage = topicSession.createTextMessage(message);
		
		if (headers != null) {
			@SuppressWarnings("rawtypes")
			Enumeration e = headers.propertyNames();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				textMessage.setStringProperty(key, headers.getProperty(key));
			}
		}

		topicPublisher.publish(textMessage);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Message published: [topic] %s [header] %s [body] %s", getName(), headers.toString(), message));
        }
	}

	private void setPublisher() throws Exception, JMSException {
		if (topicSession != null && topicPublisher != null) {
			return;
		}
		// initialize a TopicConnector
		connector.init(getName());
		// get a session
		topicSession = connector.newSession();
		Topic topic = connector.getTopic();
		if (topic == null) {
			// if the topic doesn't exist, create it.
			topic = topicSession.createTopic(getName());
		}
		topicPublisher = topicSession.createPublisher(topic);
	}

}
