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
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberSuspendedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

public class InstanceStatusEventProcessor extends TopologyEventProcessor {

    private static final Log log = LogFactory.getLog(InstanceStatusEventProcessor.class);

    private Map<String, Integer> clusterIdToActiveInstanceCountMap;

    public InstanceStatusEventProcessor() {
        clusterIdToActiveInstanceCountMap = new HashMap<String, Integer>();
    }

    @Override
    public void process(Message message) {

        //new InstanceStatusListenerThread(message).start();
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

        log.info("Received Topology message: " + messageType);

        if (MemberStartedEvent.class.getName().equals(messageType)) {

            log.info("Received message: " + messageType);

            MemberStartedEvent event = getMemberStartedEvent(message);
            String clusterDomain = event.getClusterId();
            CartridgeSubscriptionInfo cartridgeSubscriptionInfo = getCartridgeSubscriptionInfo(clusterDomain);

            if(cartridgeSubscriptionInfo != null) {

                Cluster cluster;
                //acquire read lock
                TopologyManager.acquireReadLock();
                try {
                    cluster = TopologyManager.getTopology().
                            getService(cartridgeSubscriptionInfo.getCartridge()).getCluster(cartridgeSubscriptionInfo.getClusterDomain());
                } finally {
                    //release read lock
                    TopologyManager.releaseReadLock();
                }

                TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscriptionInfo.getTenantId(),
                        cartridgeSubscriptionInfo.getCartridge(),
                        cartridgeSubscriptionInfo.getAlias(), cluster);
            }

        }
        else if (MemberActivatedEvent.class.getName().equals(messageType)) {

            log.info("Received message: " + messageType);

            MemberActivatedEvent event = getMemberActivetedEvent(message);
            String clusterDomain = event.getClusterId();
            CartridgeSubscriptionInfo cartridgeSubscriptionInfo = getCartridgeSubscriptionInfo(clusterDomain);

            if(cartridgeSubscriptionInfo != null) {

                Cluster cluster;
                //acquire read lock
                TopologyManager.acquireReadLock();
                try {
                    cluster = TopologyManager.getTopology().
                            getService(cartridgeSubscriptionInfo.getCartridge()).getCluster(cartridgeSubscriptionInfo.getClusterDomain());
                } finally {
                    //release read lock
                    TopologyManager.releaseReadLock();
                }
                //update the model
                TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscriptionInfo.getTenantId(),
                        cartridgeSubscriptionInfo.getCartridge(),
                        cartridgeSubscriptionInfo.getAlias(), cluster);
            }


        } else if (MemberSuspendedEvent.class.getName().equals(messageType)) {

            log.info("Received message: " + messageType);

            MemberStartedEvent event = getMemberStartedEvent(message);
            String clusterDomain = event.getClusterId();
            CartridgeSubscriptionInfo cartridgeSubscriptionInfo = getCartridgeSubscriptionInfo(clusterDomain);

            if(cartridgeSubscriptionInfo != null) {

                Cluster cluster;
                //acquire read lock
                TopologyManager.acquireReadLock();
                try {
                    cluster = TopologyManager.getTopology().
                            getService(cartridgeSubscriptionInfo.getCartridge()).getCluster(cartridgeSubscriptionInfo.getClusterDomain());
                } finally {
                    //release read lock
                    TopologyManager.releaseReadLock();
                }
                //update the model
                TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscriptionInfo.getTenantId(),
                        cartridgeSubscriptionInfo.getCartridge(),
                        cartridgeSubscriptionInfo.getAlias(), cluster);
            }

        } else if (MemberTerminatedEvent.class.getName().equals(messageType)) {

            log.info("Received message: " + messageType);

            MemberStartedEvent event = getMemberStartedEvent(message);
            String clusterDomain = event.getClusterId();
            CartridgeSubscriptionInfo cartridgeSubscriptionInfo = getCartridgeSubscriptionInfo(clusterDomain);

            if(cartridgeSubscriptionInfo != null) {

                Cluster cluster;
                //acquire read lock
                TopologyManager.acquireReadLock();
                try {
                    cluster = TopologyManager.getTopology().
                            getService(cartridgeSubscriptionInfo.getCartridge()).getCluster(cartridgeSubscriptionInfo.getClusterDomain());
                } finally {
                    //release read lock
                    TopologyManager.releaseReadLock();
                }
                //update the model
                TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscriptionInfo.getTenantId(),
                        cartridgeSubscriptionInfo.getCartridge(),
                        cartridgeSubscriptionInfo.getAlias(), cluster);
            }

        } else {
            //cannot happen
        }
    }

    private MemberStartedEvent getMemberStartedEvent (Message message) {

        String json = null;
        try {
            json = ((TextMessage)message).getText();

        } catch (JMSException e) {
            log.error("Error in getting Json message type from received Message ", e);
            return null;
        }
        MemberStartedEvent event = (MemberStartedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

        if(log.isDebugEnabled()) {
            log.debug("Received message details: [ " +
                    "Cluster Id: " + event.getClusterId() +
                    "\nMember Id: " + event.getMemberId() +
                    "\nService name: " + event.getServiceName() +
                    "\nStatus: " + event.getStatus().name() + " ]");
        }

        return event;
    }

    private MemberActivatedEvent getMemberActivetedEvent (Message message) {

        String json = null;
        try {
            json = ((TextMessage)message).getText();

        } catch (JMSException e) {
            log.error("Error in getting Json message type from received Message ", e);
            return null;
        }
        MemberActivatedEvent event = (MemberActivatedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

        if(log.isDebugEnabled()) {
            log.debug("Received message details: [ " +
                    "Cluster Id: " + event.getClusterId() +
                    "\nMember Id: " + event.getMemberId() +
                    "\nService name: " + event.getServiceName() +
                    "\nIp: " + event.getMemberIp() + " ]");
        }

        return event;
    }

    private MemberSuspendedEvent getMemberSuspendedEvent (Message message) {

        String json = null;
        try {
            json = ((TextMessage)message).getText();

        } catch (JMSException e) {
            log.error("Error in getting Json message type from received Message ", e);
            return null;
        }
        MemberSuspendedEvent event = (MemberSuspendedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

        if(log.isDebugEnabled()) {
            log.debug("Received message details: [ " +
                    "Cluster Id: " + event.getClusterId() +
                    "\nMember Id: " + event.getMemberId() +
                    "\nService name: " + event.getServiceName() + " ]");
        }

        return event;
    }

    private MemberTerminatedEvent getMemberTerminatedEvebt (Message message) {

        String json = null;
        try {
            json = ((TextMessage)message).getText();

        } catch (JMSException e) {
            log.error("Error in getting Json message type from received Message ", e);
            return null;
        }
        MemberTerminatedEvent event = (MemberTerminatedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

        if(log.isDebugEnabled()) {
            log.debug("Received message details: [ " +
                    "Cluster Id: " + event.getClusterId() +
                    "\nMember Id: " + event.getMemberId() +
                    "\nService name: " + event.getServiceName() + " ]");
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

    /**
     * Message Processing Thread class for InstanceStatusEventProcessor
     */
    /*private class InstanceStatusListenerThread extends Thread {

        Message message;

        public InstanceStatusListenerThread (Message message) {
            this.message = message;
        }

        public void run () {

            String messageType = null;

            try {
                messageType = message.getStringProperty(Constants.EVENT_CLASS_NAME);

            } catch (JMSException e) {
                log.error("Error in getting message type from received Message " + message.getClass().toString(), e);
                return;
            }

            if (MemberStartedEvent.class.getName().equals(messageType)) {

                log.info("Received message: " + messageType);

                MemberStartedEvent event = getMemberStartedEvent();
                String clusterDomain = event.getClusterId();
                CartridgeSubscriptionInfo cartridgeSubscriptionInfo = getCartridgeSubscriptionInfo(clusterDomain);
                if(cartridgeSubscriptionInfo != null) {

                }

            }
            else if (MemberActivatedEvent.class.getName().equals(messageType)) {

                log.info("Received message: " + messageType);

                MemberActivatedEvent event = getMemberActivetedEvent();
                String clusterDomain = event.getClusterId();


            } else if (MemberSuspendedEvent.class.getName().equals(messageType)) {

                log.info("Received message: " + messageType);

                MemberStartedEvent event = getMemberStartedEvent();
                String clusterDomain = event.getClusterId();

            } else if (MemberTerminatedEvent.class.getName().equals(messageType)) {

                log.info("Received message: " + messageType);

                MemberStartedEvent event = getMemberStartedEvent();
                String clusterDomain = event.getClusterId();

            } else {
                //cannot happen
            }
        }

        private MemberStartedEvent getMemberStartedEvent () {

            String json = null;
            try {
                json = ((TextMessage)message).getText();

            } catch (JMSException e) {
                log.error("Error in getting Json message type from received Message ", e);
                return null;
            }
            MemberStartedEvent event = (MemberStartedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

            if(log.isDebugEnabled()) {
                log.debug("Received message details: [ " +
                        "Cluster Id: " + event.getClusterId() +
                        "\nMember Id: " + event.getMemberId() +
                        "\nService name: " + event.getServiceName() +
                        "\nStatus: " + event.getStatus().name() + " ]");
            }

            return event;
        }

        private MemberActivatedEvent getMemberActivetedEvent () {

            String json = null;
            try {
                json = ((TextMessage)message).getText();

            } catch (JMSException e) {
                log.error("Error in getting Json message type from received Message ", e);
                return null;
            }
            MemberActivatedEvent event = (MemberActivatedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

            if(log.isDebugEnabled()) {
                log.debug("Received message details: [ " +
                        "Cluster Id: " + event.getClusterId() +
                        "\nMember Id: " + event.getMemberId() +
                        "\nService name: " + event.getServiceName() +
                        "\nIp: " + event.getMemberIp() + " ]");
            }

            return event;
        }

        private MemberSuspendedEvent getMemberSuspendedEvent () {

            String json = null;
            try {
                json = ((TextMessage)message).getText();

            } catch (JMSException e) {
                log.error("Error in getting Json message type from received Message ", e);
                return null;
            }
            MemberSuspendedEvent event = (MemberSuspendedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

            if(log.isDebugEnabled()) {
                log.debug("Received message details: [ " +
                        "Cluster Id: " + event.getClusterId() +
                        "\nMember Id: " + event.getMemberId() +
                        "\nService name: " + event.getServiceName() + " ]");
            }

            return event;
        }

        private MemberTerminatedEvent getMemberTerminatedEvebt () {

            String json = null;
            try {
                json = ((TextMessage)message).getText();

            } catch (JMSException e) {
                log.error("Error in getting Json message type from received Message ", e);
                return null;
            }
            MemberTerminatedEvent event = (MemberTerminatedEvent) Util.jsonToObject(json, MemberStartedEvent.class);

            if(log.isDebugEnabled()) {
                log.debug("Received message details: [ " +
                        "Cluster Id: " + event.getClusterId() +
                        "\nMember Id: " + event.getMemberId() +
                        "\nService name: " + event.getServiceName() + " ]");
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
    }*/
}
