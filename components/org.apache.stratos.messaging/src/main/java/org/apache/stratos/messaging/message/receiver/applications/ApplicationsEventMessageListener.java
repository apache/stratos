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
package org.apache.stratos.messaging.message.receiver.applications;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class ApplicationsEventMessageListener implements MqttCallback {
    private static final Log log = LogFactory.getLog(ApplicationsEventMessageListener.class);

    private ApplicationsEventMessageQueue messageQueue;

    public ApplicationsEventMessageListener(ApplicationsEventMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT Connection is lost", throwable);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Message delivery is complete: %s",
                    ((deliveryToken != null) ? deliveryToken.toString() : "")));
        }
    }

    @Override
    public void messageArrived(String topicName, MqttMessage message) throws Exception {
        TextMessage textMessage = new ActiveMQTextMessage();
        textMessage.setText(new String(message.getPayload()));
        textMessage.setStringProperty(Constants.EVENT_CLASS_NAME, Util.getEventNameForTopic(topicName));

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Tenant message received: %s", textMessage.getText()));
            }
            // Add received message to the queue
            messageQueue.add(textMessage);

        } catch (JMSException e) {
            log.error(e.getMessage(), e);
        }
    }
}
