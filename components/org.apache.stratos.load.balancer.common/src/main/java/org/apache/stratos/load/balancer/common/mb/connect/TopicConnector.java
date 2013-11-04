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

package org.apache.stratos.load.balancer.common.mb.connect;

import java.util.Properties;
import java.io.File;

import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;

import org.apache.stratos.load.balancer.common.util.Util;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * This class is responsible for loading the jndi.properties file from the classpath
 * and initialize the topic connection. Later if some other object needs a topic
 * session, this object is capable of providing one. 
 * @author nirmal
 *
 */
public class TopicConnector {
	
	private TopicConnection topicConnection;
	private String jndiPropFileDir = CarbonUtils.getCarbonConfigDirPath();
	private String topicName;
	private Topic topic;

	public TopicConnector(String topic) {
		jndiPropFileDir = System.getProperty("jndi.properties.dir");
		topicName = topic;
	}

	public void init() throws Exception {
		InitialContext ctx;
		Properties environment = Util.getProperties(jndiPropFileDir+File.separator+"jndi.properties");
		ctx = new InitialContext(environment);
		// Lookup connection factory
		TopicConnectionFactory connFactory = (TopicConnectionFactory) ctx
				.lookup("topicConnectionfactory");
		topicConnection = connFactory.createTopicConnection();
		setTopic((Topic) ctx.lookup(topicName));
		topicConnection.start();

	}

	/**
	 * Provides a new topic session.
	 * @return topic session instance
	 * @throws JMSException if unable to create a topic session
	 */
	public TopicSession newSession() throws Exception {
		if(topicConnection == null) {
			init();
		}
		return topicConnection.createTopicSession(false,
				QueueSession.AUTO_ACKNOWLEDGE);
	}
	
	public void close() throws JMSException {
		if (topicConnection == null) {
			return;
		}
		topicConnection.close();
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}
}
