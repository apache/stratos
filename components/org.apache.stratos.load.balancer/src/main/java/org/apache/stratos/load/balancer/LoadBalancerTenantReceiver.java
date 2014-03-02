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
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.listener.tenant.CompleteTenantEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantSubscribedEventListener;
import org.apache.stratos.messaging.listener.tenant.TenantUnSubscribedEventListener;
import org.apache.stratos.messaging.message.processor.tenant.TenantMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.tenant.TenantReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Load balancer tenant receiver updates load balancer context according to
 * incoming tenant events.
 */
public class LoadBalancerTenantReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(LoadBalancerTenantReceiver.class);

    private final TenantReceiver tenantReceiver;
    private boolean terminated;

    public LoadBalancerTenantReceiver() {
        tenantReceiver = new TenantReceiver(createMessageDelegator());
    }

    private TenantEventMessageDelegator createMessageDelegator() {
        TenantMessageProcessorChain processorChain = createEventProcessorChain();
        return new TenantEventMessageDelegator(processorChain);
    }

    private TenantMessageProcessorChain createEventProcessorChain() {
        TenantMessageProcessorChain messageProcessorChain = new TenantMessageProcessorChain();
        messageProcessorChain.addEventListener(new CompleteTenantEventListener() {
            @Override
            protected void onEvent(Event event) {
                CompleteTenantEvent completeTenantEvent = (CompleteTenantEvent) event;
                for (Tenant tenant : completeTenantEvent.getTenants()) {
                    for (String serviceName : tenant.getServiceSubscriptions()) {
                        if(isMultiTenantService(serviceName)) {
                            addTenantSubscriptionToLbContext(serviceName, tenant.getTenantId());
                        }
                    }
                }
            }
        });
        messageProcessorChain.addEventListener(new TenantSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TenantSubscribedEvent tenantSubscribedEvent = (TenantSubscribedEvent) event;
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Tenant subscribed event received: [tenant-id] %d [service] %s",
                            tenantSubscribedEvent.getTenantId(), tenantSubscribedEvent.getServiceName()));
                }
                if(isMultiTenantService(tenantSubscribedEvent.getServiceName())) {
                    addTenantSubscriptionToLbContext(tenantSubscribedEvent.getServiceName(), tenantSubscribedEvent.getTenantId());
                }
            }
        });
        messageProcessorChain.addEventListener(new TenantUnSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TenantUnSubscribedEvent tenantUnSubscribedEvent = (TenantUnSubscribedEvent) event;
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Tenant un-subscribed event received: [tenant-id] %d [service] %s",
                            tenantUnSubscribedEvent.getTenantId(), tenantUnSubscribedEvent.getServiceName()));
                }
                if(isMultiTenantService(tenantUnSubscribedEvent.getServiceName())) {
                    removeTenantSubscriptionFromLbContext(tenantUnSubscribedEvent.getServiceName(), tenantUnSubscribedEvent.getTenantId());
                }
            }
        });
        return messageProcessorChain;
    }

    private boolean isMultiTenantService(String serviceName) {
        try {
            TopologyManager.acquireReadLock();
            Service service = TopologyManager.getTopology().getService(serviceName);
            if(service != null) {
                return (service.getServiceType() == ServiceType.MultiTenant);
            }
            return false;
        }
        finally {
            TopologyManager.releaseReadLock();
        }
    }

    private void addTenantSubscriptionToLbContext(String serviceName, int tenantId) {
        // Find cluster of tenant
        Cluster cluster = findCluster(serviceName, tenantId);
        if (cluster != null) {
            for (String hostName : cluster.getHostNames()) {
                // Add hostName, tenantId, cluster to multi-tenant map
                Map<Integer, Cluster> clusterMap = LoadBalancerContext.getInstance().getMultiTenantClusterMap().getClusters(hostName);
                if (clusterMap == null) {
                    clusterMap = new HashMap<Integer, Cluster>();
                    clusterMap.put(tenantId, cluster);
                    LoadBalancerContext.getInstance().getMultiTenantClusterMap().addClusters(hostName, clusterMap);
                } else {
                    clusterMap.put(tenantId, cluster);
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Cluster added to multi-tenant cluster map: [host-name] %s [tenant-id] %d [cluster] %s",
                            hostName, tenantId, cluster.getClusterId()));
                }
            }
        } else {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not find cluster of tenant: [service] %s [tenant-id] %d",
                        serviceName, tenantId));
            }
        }
    }

    private void removeTenantSubscriptionFromLbContext(String serviceName, int tenantId) {
        // Find cluster of tenant
        Cluster cluster = findCluster(serviceName, tenantId);
        if (cluster != null) {
            for (String hostName : cluster.getHostNames()) {
                LoadBalancerContext.getInstance().getMultiTenantClusterMap().removeClusters(hostName);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Cluster removed from multi-tenant clusters map: [host-name] %s [tenant-id] %d [cluster] %s",
                            hostName, tenantId, cluster.getClusterId()));
                }
            }
        } else {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not find cluster of tenant: [service] %s [tenant-id] %d",
                        serviceName, tenantId));
            }
        }
    }

    private Cluster findCluster(String serviceName, int tenantId) {
        try {
            TopologyManager.acquireReadLock();
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service == null) {
                throw new RuntimeException(String.format("Service not found: %s", serviceName));
            }
            for (Cluster cluster : service.getClusters()) {
                if (cluster.tenantIdInRange(tenantId)) {
                    return cluster;
                }
            }
            return null;
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    @Override
    public void run() {
        Thread tenantReceiverThread = new Thread(tenantReceiver);
        tenantReceiverThread.start();

        // Keep the thread live until terminated
        while (!terminated) {
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException ignore) {
			}
        }
        if (log.isInfoEnabled()) {
            log.info("Load balancer tenant receiver thread terminated");
        }
    }

    /**
     * Terminate load balancer tenant receiver thread.
     */
    public void terminate() {
        tenantReceiver.terminate();
        terminated = true;
    }
}
