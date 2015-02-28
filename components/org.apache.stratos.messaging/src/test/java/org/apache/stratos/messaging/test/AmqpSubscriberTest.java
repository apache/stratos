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

package org.apache.stratos.messaging.test;

import com.google.gson.Gson;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.broker.subscribe.MessageListener;
import org.apache.stratos.messaging.domain.Message;
import org.apache.stratos.messaging.event.Event;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Amqp topic subscriber test.
 */
public class AmqpSubscriberTest {

    private static final Log log = LogFactory.getLog(AmqpSubscriberTest.class);
    private BrokerService broker;

    @BeforeClass
    public static void setUp(){
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
    }

    private static String getResourcesFolderPath() {
        String path = AmqpSubscriberTest.class.getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }

    private void initializeActiveMQ() {
        try {
            log.info("Initializing ActiveMQ...");
            broker = new BrokerService();
            broker.setBrokerName("testBroker");
            broker.addConnector("tcp://localhost:61617");
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize ActiveMQ", e);
        }
    }

    private void startActiveMQ() {
        try {
            long time1 = System.currentTimeMillis();
            broker.start();
            long time2 = System.currentTimeMillis();
            log.info(String.format("ActiveMQ started in %d sec", (time2 - time1)/1000));
        } catch (Exception e) {
            throw new RuntimeException("Could not start ActiveMQ", e);
        }
    }

    private void stopActiveMQ() {
        try {
            broker.stop();
        } catch (Exception e) {
            throw new RuntimeException("Could not stop ActiveMQ", e);
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    @Test(timeout = 20000)
    public void testSubscriberReconnection() {
        final String topicName = "test-topic";
        final List<String> messagesSent = new ArrayList<String>();
        final List<String> messagesReceived = new ArrayList<String>();
        final Gson gson = new Gson();

        initializeActiveMQ();
        startActiveMQ();
        EventSubscriber eventSubscriber = new EventSubscriber(topicName, new MessageListener() {
            @Override
            public void messageReceived(Message message) {
                TextMessageEvent event = gson.fromJson(message.getText(), TextMessageEvent.class);
                messagesReceived.add(event.getMessage());
            }
        });
        Thread subscriberThread = new Thread(eventSubscriber);
        subscriberThread.start();

        final EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
        String message1 = "message1";
        eventPublisher.publish(new TextMessageEvent(message1), true);
        messagesSent.add(message1);

        String message2 = "message2";
        eventPublisher.publish(new TextMessageEvent(message2), true);
        messagesSent.add(message2);

        stopActiveMQ();

        initializeActiveMQ();
        startActiveMQ();
        sleep(4000);

        String message3 = "message3";
        eventPublisher.publish(new TextMessageEvent(message3), true);
        messagesSent.add(message3);
        sleep(2000);

        assertTrue("Topic subscriber has not received all messages", ListUtils.isEqualList(messagesSent, messagesReceived));
    }

    private class TextMessageEvent extends Event {

        private String message;

        public TextMessageEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
