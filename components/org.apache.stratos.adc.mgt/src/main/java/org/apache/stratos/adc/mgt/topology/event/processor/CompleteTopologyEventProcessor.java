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

package org.apache.stratos.adc.mgt.topology.event.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.Collection;

public class CompleteTopologyEventProcessor extends TopologyEventProcessor {

    private static final Log log = LogFactory.getLog(CompleteTopologyEventProcessor.class);

    @Override
    public void process(Message message) {

        doProcessing(message);
        //go to next processor in the chain
        if(nextTopologyEventProcessor != null) {
            nextTopologyEventProcessor.process(message);
        }
    }

    private void doProcessing (Message message) {

        String messageType = null;

        try {
            messageType = message.getStringProperty(Constants.EVENT_CLASS_NAME);

        } catch (JMSException e) {
            log.error("Error in getting message type from received Message " + message.getClass().toString(), e);
            return;
        }

        log.info("Received Complete Topology message: " + messageType);

        if (CompleteTopologyEvent.class.getName().equals(messageType)) {
            log.info("Received message: " + messageType);

            CompleteTopologyEvent event = getCompleteTopologyEvent(message);
            //get all services
            Collection<Service> serviceCollection = event.getTopology().getServices();
            //iterate through the services
            for (Service service : serviceCollection) {
                //get all clusters
                Collection<Cluster> clusterCollection = service.getClusters();
                //iterate through the clusters
                for (Cluster cluster : clusterCollection) {
                    CartridgeSubscriptionInfo cartridgeSubscriptionInfo =
                            getCartridgeSubscriptionInfo(cluster.getClusterId());

                    if(cartridgeSubscriptionInfo != null) {
                        //add the information to Topology Cluster Info. model
                        TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscriptionInfo.getTenantId(),
                                cartridgeSubscriptionInfo.getCartridge(),
                                cartridgeSubscriptionInfo.getAlias(), cluster);
                    }
                }
            }
        }
    }

    private CompleteTopologyEvent getCompleteTopologyEvent (Message message) {

        String json = null;
        try {
            json = ((TextMessage)message).getText();

        } catch (JMSException e) {
            log.error("Error in getting Json message type from received Message ", e);
            return null;
        }
        CompleteTopologyEvent event = (CompleteTopologyEvent) Util.jsonToObject(json, CompleteTopologyEvent.class);

        if(log.isDebugEnabled()) {
            log.debug("Received message details: [ " +
                    "Services: " + event.getTopology().getServices().toString() + " ]");
        }

        return event;
    }

    private CartridgeSubscriptionInfo getCartridgeSubscriptionInfo (String clusterDomain) {

        try {
            return PersistenceManager.getSubscriptionFromClusterId(clusterDomain);

        } catch (Exception e) {
            log.error("Error getting subscription information for cluster " + clusterDomain, e);
            return null;
        }
    }
}
