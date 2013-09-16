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

package org.apache.stratos.lb.common.mb.publish;

import javax.jms.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.lb.common.mb.connect.TopicConnector;

/**
 * Any instance who needs to publish data to a topic, should communicate with this
 * object.
 * 
 * @author nirmal
 * 
 */
public class TopicPublisher {

	private static final Log log = LogFactory.getLog(TopicPublisher.class);
	private TopicSession topicSession;
	private String topicName;
	private TopicConnector connector;
	private javax.jms.TopicPublisher topicPublisher = null;

	/**
	 * @param aTopicName
	 *            topic name of this publisher instance.
	 */
	public TopicPublisher(String aTopicName) {
		topicName = aTopicName;
		connector = new TopicConnector();
	}

	/**
	 * Publishes to a topic. If for some reason the connection to the topic got
	 * lost, this will perform re-subscription periodically, until a connection
	 * obtained.
	 */
	public void publish(String message) {

		try {
			doPublish(message);

		} catch (Exception e) {
			log.error("Error while publishing to the topic: " + topicName, e);
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

	private void doPublish(String message) throws Exception, JMSException {
		setPublisher();
		
		TextMessage textMessage = topicSession.createTextMessage(message);

		topicPublisher.publish(textMessage);
	}

	private void setPublisher() throws Exception, JMSException {
		if(topicSession != null && topicPublisher != null) {
			return;
		}
		// initialize a TopicConnector
		connector.init();
		// get a session
		topicSession = connector.newSession();
		Topic topic;
		topic = topicSession.createTopic(topicName);
		topicPublisher = topicSession.createPublisher(topic);
	}
	
	public String getTopicName() {
		return topicName;
	}

}
