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
import org.apache.stratos.manager.components.ApplicationSignUpHandler;
import org.apache.stratos.manager.components.ArtifactDistributionCoordinator;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
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

    private ApplicationSignUpHandler signUpManager;
    private ArtifactDistributionCoordinator artifactDistributionCoordinator;


    public StratosManagerInstanceStatusEventReceiver() {
        signUpManager = new ApplicationSignUpHandler();
        artifactDistributionCoordinator = new ArtifactDistributionCoordinator();

        addEventListeners();
    }

    @Override
    public void execute() {
        super.execute();

        if(log.isInfoEnabled()) {
            log.info("Stratos manager instance status event receiver thread started");
        }
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

                    ApplicationSignUp[] applicationSignUps = signUpManager.getApplicationSignUps(applicationId);
                    if ((applicationSignUps == null) || (applicationSignUps.length == 0) || (
                            (applicationSignUps.length == 1) && (applicationSignUps[0] == null))) {
                        log.warn(String.format("Application signups not found for application, artifact updated event" +
                                "not sent: [application-id] %s [cartridge-type] %s", applicationId, serviceName));
                        return;
                    }

                    for (ApplicationSignUp applicationSignUp : applicationSignUps) {
                        artifactDistributionCoordinator.notifyArtifactUpdatedEventForSignUp(
                                applicationSignUp.getApplicationId(), applicationSignUp.getTenantId(), clusterId);
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
     *
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
        if (groupContexts != null) {
            for (GroupContext groupContext : groupContexts) {
                if (groupContext != null) {
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
