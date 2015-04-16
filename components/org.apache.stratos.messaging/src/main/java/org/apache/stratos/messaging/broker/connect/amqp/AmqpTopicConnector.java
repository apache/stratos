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

package org.apache.stratos.messaging.broker.connect.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.domain.exception.MessagingException;
import org.apache.stratos.messaging.util.MessagingUtil;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.util.Properties;

/**
 * AMQP topic connector.
 */
public abstract class AmqpTopicConnector implements TopicConnector {

    private static final Log log = LogFactory.getLog(AmqpTopicConnector.class);

    private TopicConnectionFactory connectionFactory;
    private TopicConnection topicConnection;
    private InitialContext initialContext;

    @Override
    public void create() {
        try {
            String jndiPropFileDir = System.getProperty("jndi.properties.dir");
            Properties environment = MessagingUtil.getProperties(jndiPropFileDir + File.separator + "jndi.properties");
            environment.put("org.wso2.carbon.context.RequestBaseContext", "true"); // always returns the base context.
            initialContext = new InitialContext(environment);
            // Lookup connection factory
            String connectionFactoryName = environment.get("connectionfactoryName").toString();
            connectionFactory = (TopicConnectionFactory) initialContext.lookup(connectionFactoryName);
        } catch (Exception e) {
            String message = "Could not create topic connector";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }

    @Override
    public String getServerURI() {
        return "";
    }

    @Override
    public void connect() {
        try {
            topicConnection = connectionFactory.createTopicConnection();
            topicConnection.setExceptionListener(new ExceptionListener() {
                @Override
                public void onException(JMSException e) {
                    log.warn("Connection to the message broker failed");
                    reconnect();
                }
            });
            topicConnection.start();
        } catch (JMSException e) {
            String message = "Could not connect to message broker";
            log.error(message, e);
            throw new MessagingException(message, e);
        }
    }

    @Override
    public void disconnect() {
        if (topicConnection != null) {
            try {
                topicConnection.stop();
                topicConnection.close();
            } catch (JMSException ignore) {
                log.warn("Could not disconnect from message broker");
            }
        }
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

    public Topic lookupTopic(String topicName) {
        try {
            return (Topic) initialContext.lookup(topicName);
        } catch (NamingException ignore) {
            return null;
        }
    }

    protected abstract void reconnect();
}
