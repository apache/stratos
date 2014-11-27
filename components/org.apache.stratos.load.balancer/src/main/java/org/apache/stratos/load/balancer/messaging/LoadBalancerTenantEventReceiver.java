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

package org.apache.stratos.load.balancer.messaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.context.LoadBalancerContextUtil;
import org.apache.stratos.messaging.domain.tenant.Subscription;
import org.apache.stratos.messaging.domain.tenant.SubscriptionDomain;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.tenant.*;
import org.apache.stratos.messaging.listener.tenant.*;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer tenant receiver updates load balancer context according to
 * incoming tenant events.
 */
public class LoadBalancerTenantEventReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(LoadBalancerTenantEventReceiver.class);

    private final TenantEventReceiver tenantEventReceiver;
    private boolean terminated;

    public LoadBalancerTenantEventReceiver() {
        tenantEventReceiver = new TenantEventReceiver();
        addEventListeners();
    }

    private void addEventListeners() {
        tenantEventReceiver.addEventListener(new CompleteTenantEventListener() {
            private boolean initialized;

            @Override
            protected void onEvent(Event event) {
                if (!initialized) {
                    CompleteTenantEvent completeTenantEvent = (CompleteTenantEvent) event;
                    if (log.isDebugEnabled()) {
                        log.debug("Complete tenant event received");
                    }
                    for (Tenant tenant : completeTenantEvent.getTenants()) {
                        for (Subscription subscription : tenant.getSubscriptions()) {
                            if (isMultiTenantService(subscription.getServiceName())) {
                                LoadBalancerContextUtil.addClustersAgainstHostNamesAndTenantIds(
                                        subscription.getServiceName(),
                                        tenant.getTenantId(),
                                        subscription.getClusterIds());
                            }

                            for (SubscriptionDomain subscriptionDomain : subscription.getSubscriptionDomains()) {
                                LoadBalancerContextUtil.addClustersAgainstDomain(
                                        subscription.getServiceName(),
                                        subscription.getClusterIds(),
                                        subscriptionDomain.getDomainName());

                                LoadBalancerContextUtil.addAppContextAgainstDomain(subscriptionDomain.getDomainName(),
                                        subscriptionDomain.getApplicationContext());
                            }
                        }
                    }
                    initialized = true;
                }
            }
        });
        tenantEventReceiver.addEventListener(new TenantSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TenantSubscribedEvent tenantSubscribedEvent = (TenantSubscribedEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Tenant subscribed event received: [tenant-id] %d [service] %s [cluster-ids] %s",
                            tenantSubscribedEvent.getTenantId(),
                            tenantSubscribedEvent.getServiceName(),
                            tenantSubscribedEvent.getClusterIds()));
                }

                if (isMultiTenantService(tenantSubscribedEvent.getServiceName())) {
                    LoadBalancerContextUtil.addClustersAgainstHostNamesAndTenantIds(
                            tenantSubscribedEvent.getServiceName(),
                            tenantSubscribedEvent.getTenantId(),
                            tenantSubscribedEvent.getClusterIds());
                }
            }
        });
        tenantEventReceiver.addEventListener(new TenantUnSubscribedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TenantUnSubscribedEvent tenantUnSubscribedEvent = (TenantUnSubscribedEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Tenant un-subscribed event received: [tenant-id] %d [service] %s [cluster-ids] %s",
                            tenantUnSubscribedEvent.getTenantId(),
                            tenantUnSubscribedEvent.getServiceName(),
                            tenantUnSubscribedEvent.getClusterIds()));
                }

                if (isMultiTenantService(tenantUnSubscribedEvent.getServiceName())) {
                    LoadBalancerContextUtil.removeClustersAgainstHostNamesAndTenantIds(
                            tenantUnSubscribedEvent.getServiceName(),
                            tenantUnSubscribedEvent.getTenantId(),
                            tenantUnSubscribedEvent.getClusterIds()
                    );
                }

                LoadBalancerContextUtil.removeClustersAgainstAllDomains(
                        tenantUnSubscribedEvent.getServiceName(),
                        tenantUnSubscribedEvent.getTenantId(),
                        tenantUnSubscribedEvent.getClusterIds());

                LoadBalancerContextUtil.removeAppContextAgainstAllDomains(
                        tenantUnSubscribedEvent.getServiceName(),
                        tenantUnSubscribedEvent.getTenantId());
            }
        });
        tenantEventReceiver.addEventListener(new SubscriptionDomainsAddedEventListener() {
            @Override
            protected void onEvent(Event event) {
                SubscriptionDomainAddedEvent subscriptionDomainAddedEvent = (SubscriptionDomainAddedEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Tenant subscription domain added event received: [tenant-id] %d " +
                            "[service] %s [cluster-ids] %s [domain-name] %s",
                            subscriptionDomainAddedEvent.getTenantId(),
                            subscriptionDomainAddedEvent.getServiceName(),
                            subscriptionDomainAddedEvent.getClusterIds(),
                            subscriptionDomainAddedEvent.getDomainName()));
                }

                LoadBalancerContextUtil.addClustersAgainstDomain(
                        subscriptionDomainAddedEvent.getServiceName(),
                        subscriptionDomainAddedEvent.getClusterIds(),
                        subscriptionDomainAddedEvent.getDomainName());

                LoadBalancerContextUtil.addAppContextAgainstDomain(
                        subscriptionDomainAddedEvent.getDomainName(),
                        subscriptionDomainAddedEvent.getApplicationContext());
            }
        });
        tenantEventReceiver.addEventListener(new SubscriptionDomainsRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                SubscriptionDomainRemovedEvent subscriptionDomainRemovedEvent = (SubscriptionDomainRemovedEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Tenant subscription domain removed event received: [tenant-id] %d " +
                            "[service] %s [cluster-ids] %s [domain-name] %s",
                            subscriptionDomainRemovedEvent.getTenantId(),
                            subscriptionDomainRemovedEvent.getServiceName(),
                            subscriptionDomainRemovedEvent.getClusterIds(),
                            subscriptionDomainRemovedEvent.getDomainName()));
                }

                LoadBalancerContextUtil.removeClustersAgainstDomain(
                        subscriptionDomainRemovedEvent.getServiceName(),
                        subscriptionDomainRemovedEvent.getClusterIds(),
                        subscriptionDomainRemovedEvent.getDomainName());

                LoadBalancerContextUtil.removeAppContextAgainstDomain(
                        subscriptionDomainRemovedEvent.getDomainName());
            }
        });
    }

    private boolean isMultiTenantService(String serviceName) {
        try {
            TopologyManager.acquireReadLock();
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                return (service.getServiceType() == ServiceType.MultiTenant);
            }
            return false;
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    @Override
    public void run() {
        Thread tenantReceiverThread = new Thread(tenantEventReceiver);
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
        tenantEventReceiver.terminate();
        terminated = true;
    }
}
