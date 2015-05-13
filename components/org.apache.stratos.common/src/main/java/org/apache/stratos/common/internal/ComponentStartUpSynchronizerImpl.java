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

package org.apache.stratos.common.internal;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.Component;
import org.apache.stratos.common.services.ComponentActivationEventListener;
import org.apache.stratos.common.services.ComponentStartUpEventListener;
import org.apache.stratos.common.services.ComponentStartUpSynchronizer;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stratos component startup synchronizer.
 */
public class ComponentStartUpSynchronizerImpl implements ComponentStartUpSynchronizer {

    private static final Log log = LogFactory.getLog(ComponentStartUpSynchronizerImpl.class);

    private static final String COMPONENT_STATUS_MAP = "stratos.component.status.map";
    private static final String COMPONENT_STARTUP_SYNCHRONIZER_ENABLED = "stratos.component.startup.synchronizer.enabled";
    private static final String COMPONENT_ACTIVATION_CHECK_INTERVAL = "stratos.component.activation.check.interval";
    private static final String COMPONENT_ACTIVATION_TIMEOUT = "stratos.component.activation.timeout";
    private static final long DEFAULT_COMPONENT_ACTIVATION_CHECK_INTERVAL = 1000;
    private static final long DEFAULT_COMPONENT_ACTIVATION_TIMEOUT = 600000;

    private Map<Component, Boolean> componentStatusMap;
    private List<ComponentStartUpEventListener> eventListeners;
    private boolean componentStartUpSynchronizerEnabled;
    private long componentActivationCheckInterval;
    private long componentActivationTimeout;

    ComponentStartUpSynchronizerImpl(DistributedObjectProvider distributedObjectProvider) {
        componentStatusMap = distributedObjectProvider.getMap(COMPONENT_STATUS_MAP);
        eventListeners = new ArrayList<ComponentStartUpEventListener>();

        componentStartUpSynchronizerEnabled = Boolean.getBoolean(COMPONENT_STARTUP_SYNCHRONIZER_ENABLED);

        componentActivationCheckInterval = Long.getLong(COMPONENT_ACTIVATION_CHECK_INTERVAL,
                DEFAULT_COMPONENT_ACTIVATION_CHECK_INTERVAL);
        log.info(String.format("Component activation check interval: %s seconds",
                (componentActivationCheckInterval / 1000)));

        componentActivationTimeout = Long.getLong(COMPONENT_ACTIVATION_TIMEOUT,
                DEFAULT_COMPONENT_ACTIVATION_TIMEOUT);
        log.info(String.format("Component activation timeout: %s seconds", (componentActivationTimeout / 1000)));
    }

    /**
     * Returns true if component startup synchronizer is enabled.
     * @return
     */
    @Override
    public boolean isEnabled() {
        return componentStartUpSynchronizerEnabled;
    }

    /**
     * Set the status of a component.
     * @param component
     * @param active
     */
    @Override
    public void setComponentStatus(Component component, boolean active) {
        componentStatusMap.put(component, active);

        if (active) {
            notifyComponentActivationEventListeners(component);
            log.info(String.format("%s component became active", component));
        } else {
            log.info(String.format("%s component became inactive", component));
        }
    }

    /**
     * Notify component activation event listeners.
     * @param component
     */
    private void notifyComponentActivationEventListeners(Component component) {
        for (ComponentStartUpEventListener eventListener : eventListeners) {
            if (eventListener instanceof ComponentActivationEventListener) {
                try {
                    ComponentActivationEventListener componentActivationEventListener =
                            (ComponentActivationEventListener) eventListener;
                    componentActivationEventListener.activated(component);
                } catch (Exception e) {
                    log.error("An error occurred while notifying component activation event listener", e);
                }
            }
        }
    }

    /**
     * Returns true if a given component is active.
     * @param component
     * @return
     */
    @Override
    public boolean isComponentActive(Component component) {
        if (componentStatusMap.containsKey(component)) {
            return componentStatusMap.get(component);
        }
        return false;
    }

    /**
     * Wait for a component to be activated
     * @param owner owner component
     * @param component component to be activated
     */
    @Override
    public void waitForComponentActivation(Component owner, Component component) {

        if(!componentStartUpSynchronizerEnabled) {
            log.debug(String.format("Component activation check is disabled, %s did not wait for %s to be activated",
                    owner, component));
            return;
        }

        long startTime = System.currentTimeMillis();
        boolean logged = false;
        while (!isComponentActive(component)) {
            if (!logged) {
                log.info(String.format("%s is waiting for %s to be activated...",
                        owner, component));
                logged = true;
            }

            try {
                Thread.sleep(componentActivationCheckInterval);
            } catch (InterruptedException ignore) {
                throw new RuntimeException(String.format("Thread interrupted, %s could not wait for " +
                        "%s to be activated", owner, component));
            }

            long currentTime = System.currentTimeMillis();
            if ((currentTime - startTime) > componentActivationTimeout) {
                throw new RuntimeException(String.format("%s component did not activated within %d seconds ",
                        component, (componentActivationTimeout / 1000)));
            }
        }
    }

    /**
     * Wait for a web service to be activated.
     * @param serviceName
     * @throws AxisFault
     */
    @Override
    public void waitForWebServiceActivation(String serviceName) throws AxisFault {

        if(!componentStartUpSynchronizerEnabled) {
            log.debug(String.format("Component activation check is disabled, did not wait for %s to be activated",
                    serviceName));
            return;
        }

        AxisConfiguration axisConfiguration = CarbonConfigurationContextFactory.getConfigurationContext()
                .getAxisConfiguration();
        AxisService cloudControllerService = axisConfiguration.getService(serviceName);
        if (!cloudControllerService.isActive()) {
            while (!cloudControllerService.isActive()) {
                log.info(String.format("Waiting for %s web service to be activated...", serviceName));
                try {
                    Thread.sleep(componentActivationCheckInterval);
                } catch (InterruptedException ignore) {
                    return;
                }
            }
            log.info(String.format("%s web service activated", serviceName));
        }
    }

    /**
     * Add component startup event listener.
     * @param eventListener
     */
    @Override
    public void addEventListener(ComponentStartUpEventListener eventListener) {
        eventListeners.add(eventListener);
    }
}
