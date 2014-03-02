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

package org.apache.stratos.manager.topology.receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.processor.topology.TopologyMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyReceiver;

import java.util.Set;

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
                            Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(cluster.getClusterId());

                            if(cartridgeSubscriptions != null) {
                                // iterate and do the relevant changes
                                for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
                                    //add the information to Topology Cluster Info. model
                                    TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                            cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                                }
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
                Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(clustercreatedEvent.getClusterId());

                if(cartridgeSubscriptions != null) {

                    String serviceType = clustercreatedEvent.getServiceName();
                    //acquire read lock
                    TopologyManager.acquireReadLock();

                    try {
                        Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clustercreatedEvent.getClusterId());

                        // iterate and do the relevant changes
                        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {

                            //add the information to Topology Cluster Info. model
                            TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                        }

                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLock();
                    }
                }
            }
        });


        // Removal of cluster is done in the unsubscription, therefore commenting this listener.
        //Cluster Removed event listner
        /*processorChain.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("********** [ClusterRemovedEventListener] Received: " + event.getClass() + " **********");

                ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;

                Set<CartridgeSubscription> cartridgeSubscriptions =
                        getCartridgeSubscription(clusterRemovedEvent.getClusterId());

                if(cartridgeSubscriptions != null) {

                    // iterate
                    for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
                        //add the information to Topology Cluster Info. model
                        TopologyClusterInformationModel.getInstance().removeCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                cartridgeSubscription.getType(), cartridgeSubscription.getAlias());
                    }
                }
            }
        });*/
        
        
      //Instance Spawned event listner
        processorChain.addEventListener(new InstanceSpawnedEventListener() {
        	
            @Override
            protected void onEvent(Event event) {

                log.info("********** [InstanceSpawnedEventListener] Received: " + event.getClass() + " **********");

                InstanceSpawnedEvent instanceSpawnedEvent = (InstanceSpawnedEvent) event;

                String clusterDomain = instanceSpawnedEvent.getClusterId();
                Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(clusterDomain);

                if(cartridgeSubscriptions != null) {

                    String serviceType = instanceSpawnedEvent.getServiceName();
                    //acquire read lock
                    TopologyManager.acquireReadLock();

                    try {
                        Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);

                        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {

                            TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                        }

                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLock();
                    }
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
                Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(clusterDomain);

                if(cartridgeSubscriptions != null) {

                    String serviceType = memberStartedEvent.getServiceName();
                    //acquire read lock
                    TopologyManager.acquireReadLock();

                    try {
                        Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);

                        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {

                            TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                        }

                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLock();
                    }
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
                Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(clusterDomain);

                if(cartridgeSubscriptions != null) {

                    String serviceType = memberActivatedEvent.getServiceName();
                    //acquire read lock
                    TopologyManager.acquireReadLock();

                    try {
                        Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);

                        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {

                            TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                        }

                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLock();
                    }
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
                Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(clusterDomain);

                if(cartridgeSubscriptions != null) {

                    String serviceType = memberSuspendedEvent.getServiceName();
                    //acquire read lock
                    TopologyManager.acquireReadLock();

                    try {
                        Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);

                        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {

                            TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                        }

                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLock();
                    }
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
                Set<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscription(clusterDomain);

                if(cartridgeSubscriptions != null) {

                    String serviceType = memberTerminatedEvent.getServiceName();
                    //acquire read lock
                    TopologyManager.acquireReadLock();

                    try {
                        Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);

                        // check and remove terminated member
                        if (cluster.memberExists(memberTerminatedEvent.getMemberId())) {
                            // release the read lock and acquire the write lock
                            TopologyManager.releaseReadLock();
                            TopologyManager.acquireWriteLock();

                                try {
                                    // re-check the state; another thread might have acquired the write lock and modified
                                    if (cluster.memberExists(memberTerminatedEvent.getMemberId())) {
                                        // remove the member from the cluster
                                        Member terminatedMember = cluster.getMember(memberTerminatedEvent.getMemberId());
                                        cluster.removeMember(terminatedMember);
                                        if (log.isDebugEnabled()) {
                                            log.debug("Removed the terminated member with id " + memberTerminatedEvent.getMemberId() + " from the cluster");
                                        }
                                    }

                                    // downgrade to read lock - 1. acquire read lock, 2. release write lock
                                    // acquire read lock
                                    TopologyManager.acquireReadLock();

                                } finally {
                                    // release the write lock
                                    TopologyManager.releaseWriteLock();
                                }
                        }

                        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {

                            TopologyClusterInformationModel.getInstance().addCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias(), cluster);
                        }

                    } finally {
                        //release read lock
                        TopologyManager.releaseReadLock();
                    }
                }

            }
        });

        return processorChain;
    }

    private Set<CartridgeSubscription> getCartridgeSubscription(String clusterDomain) {

        try {
            return new DataInsertionAndRetrievalManager().getCartridgeSubscriptionForCluster(clusterDomain);

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
        	try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        log.info("Stratos Manager topology receiver thread terminated");
    }

    //terminate Topology Receiver
    public void terminate () {
        stratosManagerTopologyReceiver.terminate();
        terminate = true;
    }
}
