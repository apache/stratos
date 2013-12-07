/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.listener.tenant.TenantSubscribedEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantUnSubscribedEventListener;
import org.apache.stratos.messaging.message.processor.tenant.TenantMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.tenant.TenantReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.HashMap;
import java.util.Map;

/**
 *  Load balancer tenant receiver updates load balancer context according to
 *  incoming tenant events.
 */
public class LoadBalancerTenantReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(LoadBalancerTenantReceiver.class);

    private final TenantReceiver tenantReiver;
    private boolean terminated;

    public LoadBalancerTenantReceiver() {
        tenantReiver = new TenantReceiver(createMessageDelegator());
    }

    private TenantEventMessageDelegator createMessageDelegator() {
        TenantMessageProcessorChain processorChain = createEventProcessorChain();
        return new TenantEventMessageDelegator(processorChain);
    }

    private TenantMessageProcessorChain createEventProcessorChain() {
        TenantMessageProcessorChain messageProcessorChain = new TenantMessageProcessorChain();
        messageProcessorChain.addEventListener(new TenantSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TenantSubscribedEvent tenantSubscribedEvent = (TenantSubscribedEvent) event;

                // Find cluster of tenant
                Cluster cluster = findCluster(tenantSubscribedEvent.getServiceName(), tenantSubscribedEvent.getTenantId());
                if(cluster != null) {
                    for(String hostName : cluster.getHostNames()) {
                        // Add hostName, tenantId, cluster to multi-tenant map
                        Map<Integer, Cluster> clusterMap = LoadBalancerContext.getInstance().getMultiTenantClusters(hostName);
                        if(clusterMap == null) {
                            clusterMap = new HashMap<Integer, Cluster>();
                            clusterMap.put(tenantSubscribedEvent.getTenantId(), cluster);
                            LoadBalancerContext.getInstance().addMultiTenantClusters(hostName, clusterMap);
                        }
                        else {
                            clusterMap.put(tenantSubscribedEvent.getTenantId(), cluster);
                        }
                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Cluster added to multi-tenant clusters map: [host-name] %s [tenant-id] %d [cluster] %s",
                                       hostName, tenantSubscribedEvent.getTenantId(), cluster.getClusterId()));
                        }
                    }
                }
                else {
                    if(log.isErrorEnabled()) {
                        log.error(String.format("Could not find cluster of tenant: [service] %s [tenant-id] %d",
                                tenantSubscribedEvent.getServiceName(), tenantSubscribedEvent.getTenantId()));
                    }
                }
            }
        });
        messageProcessorChain.addEventListener(new TenantUnSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TenantUnSubscribedEvent tenantUnSubscribedEvent = (TenantUnSubscribedEvent) event;

                // Find cluster of tenant
                Cluster cluster = findCluster(tenantUnSubscribedEvent.getServiceName(), tenantUnSubscribedEvent.getTenantId());
                if(cluster != null) {
                    for(String hostName : cluster.getHostNames()) {
                        LoadBalancerContext.getInstance().removeMultiTenantClusters(hostName);
                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Cluster removed from multi-tenant clusters map: [host-name] %s [tenant-id] %d [cluster] %s",
                                      hostName, tenantUnSubscribedEvent.getTenantId(), cluster.getClusterId()));
                        }
                    }
                }
                else {
                    if(log.isErrorEnabled()) {
                        log.error(String.format("Could not find cluster of tenant: [service] %s [tenant-id] %d",
                                tenantUnSubscribedEvent.getServiceName(), tenantUnSubscribedEvent.getTenantId()));
                    }
                }
            }
        });
        return messageProcessorChain;
    }

    private Cluster findCluster(String serviceName, int tenantId) {
        try {
            TopologyManager.acquireReadLock();
            Service service = TopologyManager.getTopology().getService(serviceName);
            if(service == null) {
                throw new RuntimeException(String.format("Service not found: %s", serviceName));
            }
            for(Cluster cluster : service.getClusters()) {
                if(cluster.tenantIdInRange(tenantId)) {
                    return cluster;
                }
            }
            return null;
        }
        finally {
            TopologyManager.releaseReadLock();
        }
    }

    @Override
    public void run() {
        Thread tenantReceiverThread = new Thread(tenantReiver);
        tenantReceiverThread.start();

        // Keep the thread live until terminated
        while (!terminated);
        if (log.isInfoEnabled()) {
            log.info("Load balancer tenant receiver thread terminated");
        }
    }

    /**
     * Terminate load balancer tenant receiver thread.
     */
    public void terminate() {
        tenantReiver.terminate();
        terminated = true;
    }
}
