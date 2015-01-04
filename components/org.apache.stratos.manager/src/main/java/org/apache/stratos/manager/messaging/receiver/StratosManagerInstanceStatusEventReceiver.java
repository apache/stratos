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

package org.apache.stratos.manager.messaging.receiver;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.pojo.*;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.manager.messaging.publisher.InstanceNotificationPublisher;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.listener.instance.status.InstanceStartedEventListener;
import org.apache.stratos.messaging.message.receiver.instance.status.InstanceStatusEventReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Stratos manager instance status event receiver.
 */
public class StratosManagerInstanceStatusEventReceiver extends InstanceStatusEventReceiver {

    private static final Log log = LogFactory.getLog(StratosManagerInstanceStatusEventReceiver.class);

    public StratosManagerInstanceStatusEventReceiver() {
        addEventListeners();
    }

    private void addEventListeners() {
        addEventListener(new InstanceStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceStartedEvent instanceStartedEvent = (InstanceStartedEvent) event;
                if (log.isInfoEnabled()) {
                    log.info(String.format("Instance started event received: [application-id] %s [cartridge-type] %s " +
                                    "[member-id] %s", instanceStartedEvent.getApplicationId(),
                            instanceStartedEvent.getServiceName(), instanceStartedEvent.getMemberId()));
                }

                try {
                    String applicationId = instanceStartedEvent.getApplicationId();
                    String serviceName = instanceStartedEvent.getServiceName();
                    String clusterId = instanceStartedEvent.getClusterId();

                    if (StringUtils.isBlank(applicationId)) {
                        throw new RuntimeException("Application id not found in instance started event: " + instanceStartedEvent);
                    }

                    if (StringUtils.isBlank(serviceName)) {
                        throw new RuntimeException("Service name not found in instance started event: " + instanceStartedEvent);
                    }

                    if (StringUtils.isBlank(clusterId)) {
                        throw new RuntimeException("Cluster id not found in instance started event: " + instanceStartedEvent);
                    }

                    AutoscalerServiceClient autoscalerServiceClient = AutoscalerServiceClient.getServiceClient();
                    ApplicationContext applicationContext = autoscalerServiceClient.getApplication(applicationId);
                    if (applicationContext == null) {
                        throw new RuntimeException("Application not found: [application-id] " + applicationId);
                    }

                    List<SubscribableInfoContext> subscribableInfoContexts = findCartridgeContext(applicationContext, serviceName);
                    if ((subscribableInfoContexts == null) || (subscribableInfoContexts.size() == 0)) {
                        throw new RuntimeException(String.format("Subscribable information not found: " +
                                "[application-id] %s [cartridge-type] %s", applicationId, serviceName));
                    }

                    InstanceNotificationPublisher publisher = new InstanceNotificationPublisher();

                    for(SubscribableInfoContext subscribableInfoContext : subscribableInfoContexts) {
                        String tenantId = "-1234";
                        publisher.publishArtifactUpdatedEvent(clusterId, tenantId, subscribableInfoContext.getRepoUrl(), subscribableInfoContext.getRepoUsername(),
                                subscribableInfoContext.getRepoPassword(), false);

                        if (log.isInfoEnabled()) {
                            log.info(String.format("Artifact updated event published: [application-id] %s [cartridge-type] %s " +
                                            "[member-id] %s [subscription-alias] %s [repo-url] %s",
                                    instanceStartedEvent.getApplicationId(), instanceStartedEvent.getServiceName(),
                                    instanceStartedEvent.getMemberId(), subscribableInfoContext.getAlias(),
                                    subscribableInfoContext.getRepoUrl()));
                        }
                    }
                } catch (Exception e) {
                    String message = "Could not send artifact updated event";
                    log.error(message, e);
                }
            }
        });
    }

    /**
     * Find subscribable info contexts of cartridge type/service name.
     * @param applicationContext
     * @param serviceName
     * @return
     */
    private List<SubscribableInfoContext> findCartridgeContext(ApplicationContext applicationContext, String serviceName) {
        List<SubscribableInfoContext> subscribableInfoContexts = new ArrayList<SubscribableInfoContext>();

        ComponentContext componentContext = applicationContext.getComponents();
        List<SubscribableInfoContext> contexts = findSubscribableInfoContexts(serviceName,
                componentContext.getCartridgeContexts());
        subscribableInfoContexts.addAll(contexts);

        GroupContext[] groupContexts = componentContext.getGroupContexts();
        if(groupContexts != null) {
            for(GroupContext groupContext : groupContexts) {
                if(groupContext != null) {
                    contexts = findSubscribableInfoContexts(serviceName, groupContext.getCartridgeContexts());
                    subscribableInfoContexts.addAll(contexts);
                }
            }
        }

        return subscribableInfoContexts;
    }

    private List<SubscribableInfoContext> findSubscribableInfoContexts(String serviceName, CartridgeContext[] cartridgeContexts) {
        List<SubscribableInfoContext> subscribableInfoContexts = new ArrayList<SubscribableInfoContext>();
        if (cartridgeContexts != null) {
            for (CartridgeContext cartridgeContext : cartridgeContexts) {
                if (cartridgeContext != null) {
                    if (cartridgeContext.getType().equals(serviceName)) {
                        subscribableInfoContexts.add(cartridgeContext.getSubscribableInfoContext());
                    }
                }
            }
        }
        return subscribableInfoContexts;
    }
}
