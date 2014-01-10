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
package org.apache.stratos.manager.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.publisher.InstanceNotificationPublisher;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Set;

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
            if (InstanceStartedEvent.class.getName().equals(type)) {
                String json = receivedMessage.getText();
                InstanceStartedEvent event = (InstanceStartedEvent) Util.jsonToObject(json, InstanceStartedEvent.class);
                String clusterId = event.getClusterId();
                if(log.isInfoEnabled()) {
                    log.info("Cluster id: " + clusterId);
                }
                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                /*CartridgeSubscriptionInfo subscription = PersistenceManager.getSubscriptionFromClusterId(clusterId);

                if (subscription.getRepository() != null) {
                    InstanceNotificationPublisher publisher = new InstanceNotificationPublisher(subscription.getRepository(), clusterId, String.valueOf(subscription.getTenantId()));
                    publisher.publish();
                }
                else {
                    //TODO: make this log debug
                    log.info("No repository found for subscription with alias: " + subscription.getAlias() + ", type: " + subscription.getCartridge() +
                        ". Not sending the Depsync event");
                }*/
                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                Set<CartridgeSubscription> cartridgeSubscriptions = new DataInsertionAndRetrievalManager().getCartridgeSubscription(clusterId);
                if (cartridgeSubscriptions == null || cartridgeSubscriptions.isEmpty()) {
                    // No subscriptions, return
                    if (log.isDebugEnabled()) {
                        log.debug("No subscription information found for cluster id " + clusterId);
                    }
                    return;
                }

                for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
                    // If only this is a non-multitenant Cartridge Subscription and repository is not null, need to
                    // send an ArtifactUpdatedEvent event. If this is a multitenant cartridge, sending this event
                    // will be done in SubscriptionMultiTenantBehaviour#createSubscription method
                    if (!cartridgeSubscription.getCartridgeInfo().getMultiTenant() && cartridgeSubscription.getRepository() != null) {
                        InstanceNotificationPublisher publisher = new InstanceNotificationPublisher();
                        publisher.sendArtifactUpdateEvent(cartridgeSubscription.getRepository(), clusterId,
                                String.valueOf(cartridgeSubscription.getSubscriber().getTenantId()));

                    } else {
                        if(log.isDebugEnabled()) {
                            log.debug("No repository found for subscription with alias: " + cartridgeSubscription.getAlias() + ", type: " + cartridgeSubscription.getType()+
                                    ". Not sending the Artifact Updated event");
                        }
                    }
                }

            }
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Could not process instance status message", e);
            }
        }
    }

}
