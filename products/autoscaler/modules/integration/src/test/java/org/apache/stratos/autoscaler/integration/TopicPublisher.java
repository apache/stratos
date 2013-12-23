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

package org.apache.stratos.autoscaler.integration;

import com.google.gson.Gson;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.util.Constants;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

public class TopicPublisher {
	private static final Log log = LogFactory.getLog(TopicPublisher.class);
	private TopicConnection topicConnection;
	private TopicSession topicSession;
	private Topic topic;
	private String topicName;

	public TopicPublisher(String topicName) {
		this.topicName = topicName;
	}

	public void connect() throws NamingException, JMSException, IOException {
		// Prepare JNDI properties
		Properties properties = new Properties();
		properties.put("java.naming.provider.url", "tcp://localhost:61616");
		properties.put("java.naming.factory.initial","org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		properties.put("connectionfactoryName", "ConnectionFactory");
		properties.put("java.naming.security.principal", "admin");
		properties.put("java.naming.security.credentials", "admin");
		InitialContext ctx = new InitialContext(properties);

		// Lookup connection factory
		String connectionFactoryName = properties.get("connectionfactoryName").toString();
		TopicConnectionFactory connectionFactory = (TopicConnectionFactory) ctx.lookup(connectionFactoryName);
		topicConnection = connectionFactory.createTopicConnection();
		topicConnection.start();
		topicSession = topicConnection.createTopicSession(false,QueueSession.AUTO_ACKNOWLEDGE);

		// Create topic
		topic = topicSession.createTopic(topicName);
	}

	public void publish(Event event) throws NamingException, JMSException,IOException {
		Gson gson = new Gson();
		String json = gson.toJson(event);
		publish(json, event.getClass().getName());
	}

	private void publish(Object message, String eventClassName)
			throws NamingException, JMSException, IOException {
		// Send message
		if (message instanceof String) {
			TextMessage textMessage = topicSession.createTextMessage((String) message);
			textMessage.setStringProperty(Constants.EVENT_CLASS_NAME,eventClassName);
			javax.jms.TopicPublisher topicPublisher = topicSession.createPublisher(topic);
			topicPublisher.publish(textMessage);
			log.info("Text message sent: " + (String) message);
		} else if (message instanceof Serializable) {
			ObjectMessage objectMessage = topicSession.createObjectMessage((Serializable) message);
			javax.jms.TopicPublisher topicPublisher = topicSession.createPublisher(topic);
			topicPublisher.publish(objectMessage);
			log.info("Object message sent: "+ ((Serializable) message).toString());
		} else {
			throw new RuntimeException("Unknown message type");
		}
	}

	public void close() throws JMSException {
		// Clean up resources
		if (topicSession != null)
			topicSession.close();
		if (topicConnection != null)
			topicConnection.close();
	}
}
