/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.topic.instance.status;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.util.Constants;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * this is to handle the topology subscription
 */
public class InstanceStatusEventMessageListener implements MqttCallback {
    public static final String ORG_APACHE_STRATOS_MESSAGING_EVENT = "org.apache.stratos.messaging.event.";
    private static final Log log = LogFactory.getLog(InstanceStatusEventMessageListener.class);

    @Override
    public void connectionLost(Throwable arg0) {
        if (log.isDebugEnabled()) {
            log.debug("Connection lost");
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        if (log.isDebugEnabled()) {
            log.debug("Delivery completed");
        }
    }

    @Override
    public void messageArrived(String arg0, MqttMessage message) throws Exception {
        if (message instanceof MqttMessage) {

            TextMessage receivedMessage = new ActiveMQTextMessage();

            receivedMessage.setText(new String(message.getPayload()));
            receivedMessage.setStringProperty(Constants.EVENT_CLASS_NAME,
                    ORG_APACHE_STRATOS_MESSAGING_EVENT.concat(arg0.replace("/",
                            ".")));

            try {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Instance notifier message received: %s",
                            ((TextMessage) message).getText()));
                }
                // Add received message to the queue
                InstanceStatusEventMessageQueue.getInstance().add(receivedMessage);

            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            }
        }

    }
}
