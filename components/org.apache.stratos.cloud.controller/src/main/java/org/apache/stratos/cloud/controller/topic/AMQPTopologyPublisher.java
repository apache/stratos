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
package org.apache.stratos.cloud.controller.topic;

import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.stratos.cloud.controller.interfaces.TopologyPublisher;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.TopologyConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AMQPTopologyPublisher extends TopologyPublisher {
	private TopicPublisher topicPublisher;
	private TopicSession topicSession;
	private TopicConnection topicConnection;
	private TopicConnectionFactory topicConnectionFactory;
	private String topologySynchronizerCron = CloudControllerConstants.TOPOLOGY_SYNC_CRON;
	private String amqpConnectionUrl = CloudControllerConstants.AMQP_CONNECTION_URL;
	private String amqpInitialContextFactory = CloudControllerConstants.AMQP_INITIAL_CONTEXT_FACTORY;
	private String amqpTopicConnectionFactory = CloudControllerConstants.AMQP_TOPIC_CONNECTION_FACTORY;
	private static final Log log = LogFactory
			.getLog(AMQPTopologyPublisher.class);

	public void publish(String topicName, String message) {
		try {
			topicConnection = topicConnectionFactory.createTopicConnection();
			topicConnection.start();
			topicSession = topicConnection.createTopicSession(false,
					Session.AUTO_ACKNOWLEDGE);

			Topic topic = topicSession.createTopic(topicName);
			topicPublisher = topicSession.createPublisher(topic);
			TextMessage textMessage = topicSession.createTextMessage(message);

			topicPublisher.publish(textMessage);

			topicPublisher.close();
			topicSession.close();
			topicConnection.stop();
			topicConnection.close();

		} catch (JMSException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void init() {
		TopologyConfig config = FasterLookUpDataHolder.getInstance()
				.getTopologyConfig();
		String cron = config.getProperty(CloudControllerConstants.CRON_PROPERTY);
		topologySynchronizerCron = cron == null ? topologySynchronizerCron
				: cron;

		String url = config
				.getProperty(CloudControllerConstants.AMQP_CONNECTION_URL_PROPERTY);
		amqpConnectionUrl = url == null ? amqpConnectionUrl : url;

		Properties initialContextProperties = new Properties();

		String initialContextFactory = config
				.getProperty(CloudControllerConstants.AMQP_INITIAL_CONTEXT_FACTORY_PROPERTY);
		amqpInitialContextFactory = initialContextFactory == null ? amqpInitialContextFactory
				: initialContextFactory;

		initialContextProperties.put("java.naming.factory.initial",
				amqpInitialContextFactory);

		initialContextProperties.put("connectionfactory.qpidConnectionfactory",
				amqpConnectionUrl);

		String connectionFactory = config
				.getProperty(CloudControllerConstants.AMQP_TOPIC_CONNECTION_FACTORY_PROPERTY);
		amqpTopicConnectionFactory = connectionFactory == null ? amqpTopicConnectionFactory
				: connectionFactory;

		try {
			InitialContext initialContext = new InitialContext(
					initialContextProperties);
			topicConnectionFactory = (TopicConnectionFactory) initialContext
					.lookup(amqpTopicConnectionFactory);

			// topicConnection.stop();
			// topicConnection.close();

		} catch (NamingException e) {
			log.error(e.getMessage(), e);
		}

	}

	@Override
	public String getCron() {
		return topologySynchronizerCron;
	}

}
