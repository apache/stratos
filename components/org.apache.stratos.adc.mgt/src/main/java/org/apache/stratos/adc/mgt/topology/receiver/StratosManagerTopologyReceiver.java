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

package org.apache.stratos.adc.mgt.topology.receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.processor.topology.TopologyMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyReceiver;

public class StratosManagerTopologyReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(StratosManagerTopologyReceiver.class);

    private TopologyReceiver stratosManagerTopologyReceiver;
    private boolean terminate;

    public StratosManagerTopologyReceiver() {
        this.terminate = false;
        this.stratosManagerTopologyReceiver = new TopologyReceiver(createMessageDelegator());
    }

    private TopologyEventMessageDelegator createMessageDelegator() {
        TopologyMessageProcessorChain processorChain = createEventProcessorChain();
        return new TopologyEventMessageDelegator(processorChain);
    }

    private TopologyMessageProcessorChain createEventProcessorChain() {

        TopologyMessageProcessorChain processorChain = new TopologyMessageProcessorChain();

        //add listner to Complete Topology Event
        processorChain.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [CompleteTopologyEventListener] Received: " + event.getClass() + " **********");

                try {
                    TopologyManager.acquireReadLock();
                    for (Service service : TopologyManager.getTopology().getServices()) {
                        //iterate through all clusters
                        for (Cluster cluster : service.getClusters()) {
                            //get subscription details
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
                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        //Cluster Created event listner
        processorChain.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [ClusterCreatedEventListener] Received: " + event.getClass() + " **********");

                ClusterCreatedEvent clustercreatedEvent = (ClusterCreatedEvent) event;
                //get subscription details
                CartridgeSubscriptionInfo cartridgeSubscriptionInfo =
                        getCartridgeSubscriptionInfo(clustercreatedEvent.getClusterId());

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

                    //add the information to Topology Cluster Info. model
                    TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscriptionInfo.getTenantId(),
                            cartridgeSubscriptionInfo.getCartridge(),
                            cartridgeSubscriptionInfo.getAlias(), cluster);
                }

            }
        });

        //Cluster Removed event listner
        processorChain.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [ClusterRemovedEventListener] Received: " + event.getClass() + " **********");

                ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;

                CartridgeSubscriptionInfo cartridgeSubscriptionInfo =
                        getCartridgeSubscriptionInfo(clusterRemovedEvent.getClusterId());

                if (cartridgeSubscriptionInfo != null) {
                    //remove the information from Topology Cluster Info. model
                    TopologyClusterInformationModel.getInstance().removeCluster(cartridgeSubscriptionInfo.getTenantId(),
                            cartridgeSubscriptionInfo.getCartridge(),
                            cartridgeSubscriptionInfo.getAlias());
                }
            }
        });

        //Member Started event listner
        processorChain.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [MemberStartedEventListener] Received: " + event.getClass() + " **********");

                MemberStartedEvent memberStartedEvent = (MemberStartedEvent) event;

                String clusterDomain = memberStartedEvent.getClusterId();
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
        });

        //Member Activated event listner
        processorChain.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [MemberActivatedEventListener] Received: " + event.getClass() + " **********");

                MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;

                String clusterDomain = memberActivatedEvent.getClusterId();
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
        });

        //Member Suspended event listner
        processorChain.addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [MemberSuspendedEventListener] Received: " + event.getClass() + " **********");

                MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;

                String clusterDomain = memberSuspendedEvent.getClusterId();
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
        });

        //Member Terminated event listner
        processorChain.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [MemberTerminatedEventListener] Received: " + event.getClass() + " **********");

                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;

                String clusterDomain = memberTerminatedEvent.getClusterId();
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
        });

        return processorChain;
    }

    private CartridgeSubscriptionInfo getCartridgeSubscriptionInfo (String clusterDomain) {

        try {
            return PersistenceManager.getSubscriptionFromClusterId(clusterDomain);

        } catch (Exception e) {
            log.error("Error getting subscription information for cluster " + clusterDomain, e);
            return null;
        }
    }

    @Override
    public void run() {

        Thread thread = new Thread(stratosManagerTopologyReceiver);
        thread.start();
        log.info("Stratos Manager topology receiver thread started");

        //Keep running till terminate is set from deactivate method of the component
        while (!terminate) {
            //loop while terminate = false
        }
        log.info("Stratos Manager topology receiver thread terminated");
    }

    //terminate Topology Receiver
    public void terminate () {
        stratosManagerTopologyReceiver.terminate();
        terminate = true;
    }
}
