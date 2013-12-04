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
package org.apache.stratos.adc.mgt.listener;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.publisher.ArtifactUpdatePublisher;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.messaging.event.instance.status.MemberStartedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

public class InstanceStatusListener implements MessageListener {

    private static final Log log = LogFactory
            .getLog(InstanceStatusListener.class);

    @Override
    public void onMessage(Message message) {
        TextMessage receivedMessage = (TextMessage) message;
        if(log.isInfoEnabled()) {
            log.info("Instance status message received");
        }

        try {
            String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
            if(log.isInfoEnabled()) {
                log.info(String.format("Event class name: %s ", type));
            }
            // If member started event is received publish artifact update message
            // To do a git clone
            if (MemberStartedEvent.class.getName().equals(type)) {
                String json = receivedMessage.getText();
                MemberStartedEvent event = (MemberStartedEvent) Util.jsonToObject(json, MemberStartedEvent.class);
                String clusterId = event.getClusterId();
                if(log.isInfoEnabled()) {
                    log.info("Cluster id: " + clusterId);
                }

                CartridgeSubscriptionInfo subscription = PersistenceManager.getSubscriptionFromClusterId(clusterId);
                ArtifactUpdatePublisher publisher = new ArtifactUpdatePublisher(subscription.getRepository(), clusterId, String.valueOf(subscription.getTenantId()));
                publisher.publish();
            }
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Could not process instance status message", e);
            }
        }
    }

}
