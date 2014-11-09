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
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.ApplicationSubscriptionException;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.subscription.ApplicationSubscription;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.List;

public class StratosManagerTopologyEventReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(StratosManagerTopologyEventReceiver.class);

    private TopologyEventReceiver topologyEventReceiver;
    private boolean terminated;

    public StratosManagerTopologyEventReceiver() {
        this.terminated = false;
        this.topologyEventReceiver = new TopologyEventReceiver();
        addEventListeners();
    }

    private void addEventListeners() {
        //add listner to Complete Topology Event
        topologyEventReceiver.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {

                if (TopologyClusterInformationModel.getInstance().isInitialized()) {
                    return;
                }

                log.info("[CompleteTopologyEventListener] Received: " + event.getClass());

                try {
                    TopologyManager.acquireReadLock();

                    for (Service service : TopologyManager.getTopology()
                            .getServices()) {
                        // iterate through all clusters
                        for (Cluster cluster : service.getClusters()) {
                            TopologyClusterInformationModel.getInstance()
                                    .addCluster(cluster);
                        }
                    }

                    TopologyClusterInformationModel.getInstance().setInitialized(true);

                } finally {
                    TopologyManager.releaseReadLock();
                }
            }
        });

        //Cluster Created event listner
        topologyEventReceiver.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterCreatedEventListener] Received: " + event.getClass());

                ClusterCreatedEvent clustercreatedEvent = (ClusterCreatedEvent) event;

                String serviceType = clustercreatedEvent.getCluster().getServiceName();
                //acquire read lock
                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(clustercreatedEvent.getCluster().getServiceName(),
                        clustercreatedEvent.getCluster().getClusterId());

                try {
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).
                            getCluster(clustercreatedEvent.getCluster().getClusterId());
                    TopologyClusterInformationModel.getInstance().addCluster(cluster);

                } finally {
                    //release read lock
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(clustercreatedEvent.getCluster().getServiceName(),
                            clustercreatedEvent.getCluster().getClusterId());
                }

            }
        });


        //Cluster Removed event listner
        topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterRemovedEventListener] Received: " + event.getClass());

                ClusterRemovedEvent clusterRemovedEvent = (ClusterRemovedEvent) event;
                TopologyClusterInformationModel.getInstance().removeCluster(clusterRemovedEvent.getClusterId());
            }
        });
        
        
      //Instance Spawned event listner
        topologyEventReceiver.addEventListener(new InstanceSpawnedEventListener() {

            @Override
            protected void onEvent(Event event) {

                log.info("[InstanceSpawnedEventListener] Received: " + event.getClass());

                InstanceSpawnedEvent instanceSpawnedEvent = (InstanceSpawnedEvent) event;

                String clusterDomain = instanceSpawnedEvent.getClusterId();

                String serviceType = instanceSpawnedEvent.getServiceName();
                //acquire read lock
                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(instanceSpawnedEvent.getServiceName(),
                        instanceSpawnedEvent.getClusterId());

                try {
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);
                    TopologyClusterInformationModel.getInstance().addCluster(cluster);
                } finally {
                    //release read lock
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(instanceSpawnedEvent.getServiceName(),
                            instanceSpawnedEvent.getClusterId());
                }
            }
        });

        //Member Started event listner
        topologyEventReceiver.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[MemberStartedEventListener] Received: " + event.getClass());

                MemberStartedEvent memberStartedEvent = (MemberStartedEvent) event;

                String clusterDomain = memberStartedEvent.getClusterId();

                String serviceType = memberStartedEvent.getServiceName();
                //acquire read lock
                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberStartedEvent.getServiceName(),
                        memberStartedEvent.getClusterId());

                try {
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);
                    TopologyClusterInformationModel.getInstance().addCluster(cluster);
                } finally {
                    //release read lock
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberStartedEvent.getServiceName(),
                            memberStartedEvent.getClusterId());
                }

            }
        });

        //Member Activated event listner
        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[MemberActivatedEventListener] Received: " + event.getClass());

                MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;

                String clusterDomain = memberActivatedEvent.getClusterId();

                String serviceType = memberActivatedEvent.getServiceName();
                //acquire read lock
                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberActivatedEvent.getServiceName(),
                        memberActivatedEvent.getClusterId());

                try {
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);
                    TopologyClusterInformationModel.getInstance().addCluster(cluster);
                } finally {
                    //release read lock
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId());
                }
            }
        });

        //Member Suspended event listner
        topologyEventReceiver.addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[MemberSuspendedEventListener] Received: " + event.getClass());

                MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;

                String clusterDomain = memberSuspendedEvent.getClusterId();

                String serviceType = memberSuspendedEvent.getServiceName();
                //acquire read lock
                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberSuspendedEvent.getServiceName(),
                        memberSuspendedEvent.getClusterId());

                try {
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);
                    TopologyClusterInformationModel.getInstance().addCluster(cluster);

                } finally {
                    //release read lock
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberSuspendedEvent.getServiceName(),
                            memberSuspendedEvent.getClusterId());
                }
            }
        });

        //Member Terminated event listner
        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[MemberTerminatedEventListener] Received: " + event.getClass());

                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;

                String clusterDomain = memberTerminatedEvent.getClusterId();

                String serviceType = memberTerminatedEvent.getServiceName();
                //acquire read lock
                //TopologyManager.acquireReadLock();
                TopologyManager.acquireReadLockForCluster(memberTerminatedEvent.getServiceName(),
                        memberTerminatedEvent.getClusterId());

                try {
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterDomain);

                    // check and remove terminated member
//                    if (cluster.memberExists(memberTerminatedEvent.getMemberId())) {
//                        // release the read lock and acquire the write lock
////                        TopologyManager.releaseReadLock();
////                        TopologyManager.acquireWriteLock();
//                        TopologyManager.releaseReadLockForCluster(memberTerminatedEvent.getServiceName(),
//                                memberTerminatedEvent.getClusterId());
//                        TopologyManager.acquireWriteLockForCluster(memberTerminatedEvent.getServiceName(),
//                                memberTerminatedEvent.getClusterId());
//
//                        try {
//                            // re-check the state; another thread might have acquired the write lock and modified
//                            if (cluster.memberExists(memberTerminatedEvent.getMemberId())) {
//                                // remove the member from the cluster
//                                Member terminatedMember = cluster.getMember(memberTerminatedEvent.getMemberId());
//                                cluster.removeMember(terminatedMember);
//                                if (log.isDebugEnabled()) {
//                                    log.debug("Removed the terminated member with id " + memberTerminatedEvent.getMemberId() + " from the cluster");
//                                }
//                            }
//
//                            // downgrade to read lock - 1. acquire read lock, 2. release write lock
//                            // acquire read lock
//                            //TopologyManager.acquireReadLock();
//                            TopologyManager.acquireReadLockForCluster(memberTerminatedEvent.getServiceName(),
//                                    memberTerminatedEvent.getClusterId());
//
//                        } finally {
//                            // release the write lock
//                           // TopologyManager.releaseWriteLock();
//                            TopologyManager.releaseWriteLockForCluster(memberTerminatedEvent.getServiceName(),
//                                    memberTerminatedEvent.getClusterId());
//                        }
//                    }
                    TopologyClusterInformationModel.getInstance().addCluster(cluster);
                } finally {
                    //release read lock
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForCluster(memberTerminatedEvent.getServiceName(),
                            memberTerminatedEvent.getClusterId());
                }
            }
        });
        
      //add listner to Complete Topology Event
/*
        topologyEventReceiver.addEventListener(new ApplicationCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {

            	ApplicationCreatedEvent appCreateEvent = (ApplicationCreatedEvent) event;

                log.info("[ApplicationCreatedEventListener] Received: " + event.getClass());

                try {
                    //TopologyManager.acquireReadLock();
                    TopologyManager.acquireReadLockForApplication(appCreateEvent.getApplication().getUniqueIdentifier());
                    
                    // create and persist Application subscritpion
                    CartridgeSubscriptionManager cartridgeSubscriptionManager = new CartridgeSubscriptionManager();
                    ApplicationSubscription compositeAppSubscription;
                    Application app = appCreateEvent.getApplication();
                    String appId = app.getUniqueIdentifier();
                    int tenantId = app.getTenantId();
                    String domain = app.getTenantDomain();
                    
                    if (log.isDebugEnabled()) {
                    	log.debug("received application created event for app: " + appId + " and tenant: " + tenantId + 
                    			" domain:" + domain);
                    }
                    try {
                        PrivilegedCarbonContext.startTenantFlow();
                        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                        carbonContext.setTenantDomain(domain);
                        carbonContext.setTenantId(tenantId);
                       // create Application Subscription and persist
                        compositeAppSubscription = cartridgeSubscriptionManager.createApplicationSubscription(appId, tenantId);
                        cartridgeSubscriptionManager.persistApplicationSubscription(compositeAppSubscription);
                    } catch (ApplicationSubscriptionException e) {
                        log.error("failed to persist application subscription, caught exception: " + e);
                    } catch (ADCException e) {
                    	log.error("failed to persist application subscription, caught exception: " + e);
                    } finally {
                    	PrivilegedCarbonContext.endTenantFlow();
                    }

                    // add the clusters to the topology information model
                    List<Cluster> appClusters = appCreateEvent.getClusterList();
                    if (appClusters != null && !appClusters.isEmpty()) {
                        for (Cluster appCluster :  appClusters) {
                            TopologyClusterInformationModel.getInstance().addCluster(appCluster);
                        }
                    } else {
                        log.warn("No clusters were found in the Application Created event for app id [ " +
                                appId + " ] to add to Cluster Information model");
                    }

                } finally {
                    //TopologyManager.releaseReadLock();
                    TopologyManager.releaseReadLockForApplication(appCreateEvent.getApplication().getUniqueIdentifier());
                }
            }
        });
*/

        //add listener 
/*
        topologyEventReceiver.addEventListener(new ApplicationTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {

                ApplicationTerminatedEvent appRemovedEvent = (ApplicationTerminatedEvent) event;

                log.info("[ApplicationTerminatedEvent] Received: " + event.getClass());

                try {
                    // no need to lock since Topology is not accessed
                    //TopologyManager.acquireReadLock();
                    //TopologyManager.acquireReadLockForApplication(appRemovedEvent.getAppId());
                    
                    // create and persist Application subscritpion
                    CartridgeSubscriptionManager cartridgeSubscriptionManager = new CartridgeSubscriptionManager();
                    String appId = appRemovedEvent.getAppId();
                    
                    int tenantId = appRemovedEvent.getTenantId();
                    String domain = appRemovedEvent.getTenantDomain();
                    
                    if (log.isDebugEnabled()) {
                    	log.debug("received application created event for app: " + appId + " and tenant: " + tenantId + 
                    			" domain:" + domain);
                    }
                    try {
                        PrivilegedCarbonContext.startTenantFlow();
                        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                        carbonContext.setTenantDomain(domain);
                        carbonContext.setTenantId(tenantId);
                       // create Application Subscription and persist
                        cartridgeSubscriptionManager.removeApplicationSubscription(appId, tenantId);
                        
                    } catch (ApplicationSubscriptionException e) {
                        log.error("failed to persist application subscription, caught exception: " + e);
                    } finally {
                    	PrivilegedCarbonContext.endTenantFlow();
                    }
                } finally {
                    //TopologyManager.releaseReadLock();
                    //TopologyManager.releaseReadLockForApplication(appRemovedEvent.getAppId());
                }
            }
        });
*/
    }


    @Override
    public void run() {
        Thread thread = new Thread(topologyEventReceiver);
        thread.start();
        log.info("Stratos Manager topology receiver thread started");

        //Keep running till terminate is set from deactivate method of the component
        while (!terminated) {
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
        topologyEventReceiver.terminate();
        terminated = true;
    }
}
