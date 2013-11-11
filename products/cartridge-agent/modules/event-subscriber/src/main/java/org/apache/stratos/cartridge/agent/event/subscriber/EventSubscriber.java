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

package org.apache.stratos.cartridge.agent.event.subscriber;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.util.Constants;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;

/**
 * A generic topic publisher.
 */
public class EventSubscriber {
    private static final Log log = LogFactory.getLog(EventSubscriber.class);

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private Topic topic;
    private String mbIpAddress;
    private int mbPort;
    private String topicName;

    public EventSubscriber(String mbIpAddress, int mbPort, String topicName) {
        this.mbIpAddress = mbIpAddress;
        this.mbPort = mbPort;
        this.topicName = topicName;
    }

    private Properties getProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(EventSubscriber.class.getClassLoader().getResourceAsStream("jndi.properties"));

        // Set message broker ip and port
        String connectionFactoryName = properties.get("connectionfactoryName").toString();
        String key = "connectionfactory." + connectionFactoryName;
        String connectionFactoryStr = (String) properties.get(key);
        connectionFactoryStr = connectionFactoryStr.replace("MB-IP-ADDRESS", mbIpAddress);
        connectionFactoryStr = connectionFactoryStr.replace("MB-PORT", String.valueOf(mbPort));
        properties.setProperty(key, connectionFactoryStr);

        return properties;
    }

    public void connect() throws NamingException, JMSException, IOException {
        // Prepare JNDI properties
        Properties properties = getProperties();
        InitialContext ctx = new InitialContext(properties);

        // Lookup connection factory
        String connectionFactoryName = properties.get("connectionfactoryName").toString();
        TopicConnectionFactory connectionFactory = (TopicConnectionFactory) ctx.lookup(connectionFactoryName);
        topicConnection = connectionFactory.createTopicConnection();
        topicConnection.start();
        topicSession = topicConnection.createTopicSession(false, QueueSession.AUTO_ACKNOWLEDGE);

        // Create topic
        topic = topicSession.createTopic(topicName);
    }

    public void publish(Event event) throws NamingException, JMSException, IOException {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        publish(json, event.getClass().getName());
    }

    private void publish(String message, String eventClassName) throws NamingException, JMSException, IOException {
        TextMessage textMessage = topicSession.createTextMessage((String) message);
        textMessage.setStringProperty(Constants.EVENT_CLASS_NAME, eventClassName);
        javax.jms.TopicPublisher topicPublisher = topicSession.createPublisher(topic);
        topicPublisher.publish(textMessage);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Message published: [topic] %s [header] %s [body] %s", topicName, eventClassName, message));
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
