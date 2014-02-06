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

package org.apache.stratos.messaging.broker.connect;

import java.util.Properties;
import java.io.File;

import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.stratos.messaging.util.Util;
import org.apache.stratos.messaging.util.Constants;

/**
 * This class is responsible for loading the jndi.properties file from the
 * classpath
 * and initialize the topic connection. Later if some other object needs a topic
 * session, this object is capable of providing one.
 *
 * @author nirmal
 */
public class TopicConnector {

    private TopicConnection topicConnection;
    private String jndiPropFileDir;
    private Topic topic;

    public TopicConnector() {
        jndiPropFileDir = System.getProperty("jndi.properties.dir");
    }

    public void init(String topicName) throws Exception {
        InitialContext ctx;
        Properties environment = Util.getProperties(jndiPropFileDir + File.separator + "jndi.properties");
        environment.put(Constants.REQUEST_BASE_CONTEXT, "true"); // always returns the base context.
        ctx = new InitialContext(environment);
        // Lookup connection factory
        String connectionFactoryName = environment.get("connectionfactoryName").toString();
        TopicConnectionFactory connFactory = (TopicConnectionFactory) ctx.lookup(connectionFactoryName);
        // Lookup the topic
        try {
            setTopic((Topic) ctx.lookup(topicName));
        } catch (NamingException e) {
        }
        topicConnection = connFactory.createTopicConnection();
        topicConnection.start();

    }

    /**
     * Provides a new topic session.
     *
     * @return topic session instance
     * @throws JMSException if unable to create a topic session
     */
    public TopicSession newSession() throws Exception {
        return topicConnection.createTopicSession(false, QueueSession.AUTO_ACKNOWLEDGE);
    }

    public void close() throws JMSException {
        if (topicConnection == null) {
            return;
        }
        topicConnection.close();
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}
