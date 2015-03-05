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

package org.apache.stratos.load.balancer.common.event.receivers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.messaging.domain.application.signup.DomainMapping;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpAddedEvent;
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpRemovedEvent;
import org.apache.stratos.messaging.event.application.signup.CompleteApplicationSignUpsEvent;
import org.apache.stratos.messaging.listener.application.signup.ApplicationSignUpAddedEventListener;
import org.apache.stratos.messaging.listener.application.signup.ApplicationSignUpRemovedEventListener;
import org.apache.stratos.messaging.listener.application.signup.CompleteApplicationSignUpsEventListener;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpEventReceiver;

/**
 * Load balancer common application signup event receiver updates the topology in the given topology provider
 * with the hostnames found in application signup events.
 */
public class LoadBalancerCommonApplicationSignUpEventReceiver extends ApplicationSignUpEventReceiver {

    private static final Log log = LogFactory.getLog(LoadBalancerCommonApplicationSignUpEventReceiver.class);

    private TopologyProvider topologyProvider;

    public LoadBalancerCommonApplicationSignUpEventReceiver(TopologyProvider topologyProvider) {
        this.topologyProvider = topologyProvider;
        addEventListeners();
    }

    private void addEventListeners() {
        addEventListener(new CompleteApplicationSignUpsEventListener() {
            private boolean initialized = false;

            @Override
            protected void onEvent(Event event) {
                try {
                    if (initialized) {
                        return;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Complete application signup event received");
                    }

                    CompleteApplicationSignUpsEvent completeApplicationSignUpsEvent = (CompleteApplicationSignUpsEvent) event;
                    for (ApplicationSignUp applicationSignUp : completeApplicationSignUpsEvent.getApplicationSignUps()) {

                        // Add tenant signups
                        for (String clusterId : applicationSignUp.getClusterIds()) {
                            topologyProvider.addTenantSignUp(clusterId, applicationSignUp.getTenantId());
                        }

                        // Add domain mappings
                        if (applicationSignUp.getDomainMappings() != null) {
                            for (DomainMapping domainMapping : applicationSignUp.getDomainMappings()) {
                                if (domainMapping != null) {
                                    Cluster cluster = topologyProvider.getClusterByClusterId(domainMapping.getClusterId());
                                    if (cluster != null) {
                                        cluster.addHostName(domainMapping.getDomainName());
                                        log.info(String.format("Domain mapping added: [cluster] %s [domain] %s",
                                                cluster.getClusterId(), domainMapping.getDomainName()));
                                    }
                                }
                            }
                        }
                    }
                    initialized = true;
                } catch (Exception e) {
                    log.error("Could not process complete application signup event", e);
                }
            }
        });

        addEventListener(new ApplicationSignUpAddedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    ApplicationSignUpAddedEvent applicationSignUpAddedEvent = (ApplicationSignUpAddedEvent) event;
                    for (String clusterId : applicationSignUpAddedEvent.getClusterIds()) {
                        topologyProvider.addTenantSignUp(clusterId, applicationSignUpAddedEvent.getTenantId());
                    }
                } catch (Exception e) {
                    log.error("Could not process application signup added event", e);
                }
            }
        });

        addEventListener(new ApplicationSignUpRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    ApplicationSignUpRemovedEvent applicationSignUpRemovedEvent = (ApplicationSignUpRemovedEvent) event;
                    String applicationId = applicationSignUpRemovedEvent.getApplicationId();

                    Application application = ApplicationManager.getApplications().getApplication(applicationId);
                    if (application != null) {
                        for (ClusterDataHolder clusterDataHolder : application.getClusterDataMap().values())
                            topologyProvider.removeTenantSignUp(clusterDataHolder.getClusterId(),
                                    applicationSignUpRemovedEvent.getTenantId());
                    }
                } catch (Exception e) {
                    log.error("Could not process application signup removed event", e);
                }
            }
        });
    }
}
