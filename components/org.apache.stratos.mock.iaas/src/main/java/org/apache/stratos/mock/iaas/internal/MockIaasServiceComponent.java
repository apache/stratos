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

package org.apache.stratos.mock.iaas.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.Component;
import org.apache.stratos.common.services.ComponentStartUpSynchronizer;
import org.apache.stratos.mock.iaas.config.MockIaasConfig;
import org.apache.stratos.mock.iaas.persistence.PersistenceManager;
import org.apache.stratos.mock.iaas.persistence.PersistenceManagerFactory;
import org.apache.stratos.mock.iaas.persistence.PersistenceManagerType;
import org.apache.stratos.mock.iaas.services.MockIaasService;
import org.apache.stratos.mock.iaas.services.impl.MockIaasServiceImpl;
import org.apache.stratos.mock.iaas.services.impl.MockIaasServiceUtil;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 *
 * @scr.component name="org.apache.stratos.mock.iaas.internal.MockIaasServiceComponent" immediate="true"
 * @scr.reference name="registry.service" interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="componentStartUpSynchronizer" interface="org.apache.stratos.common.services.ComponentStartUpSynchronizer"
 *                cardinality="1..1" policy="dynamic" bind="setComponentStartUpSynchronizer" unbind="unsetComponentStartUpSynchronizer"
 */
public class MockIaasServiceComponent {

    private static final Log log = LogFactory.getLog(MockIaasServiceComponent.class);

    protected void activate(final ComponentContext context) {
        Runnable mockIaasActivator = new Runnable() {
            @Override
            public void run() {
                try {
                    if(!MockIaasConfig.getInstance().isEnabled()) {
                        log.debug("Mock IaaS is disabled, Mock IaaS service component is not activated");
                        return;
                    }

                    ComponentStartUpSynchronizer componentStartUpSynchronizer =
                            ServiceReferenceHolder.getInstance().getComponentStartUpSynchronizer();

                    // Wait for stratos manager to be activated
                    componentStartUpSynchronizer.waitForComponentActivation(Component.MockIaaS,
                            Component.StratosManager);

                    PersistenceManager persistenceManager =
                            PersistenceManagerFactory.getPersistenceManager(PersistenceManagerType.Registry);
                    MockIaasServiceUtil mockIaasServiceUtil = new MockIaasServiceUtil(persistenceManager);
                    mockIaasServiceUtil.startInstancesPersisted();

                    MockIaasService mockIaasService = new MockIaasServiceImpl();
                    context.getBundleContext().registerService(MockIaasService.class.getName(), mockIaasService, null);
                    log.info("Mock IaaS service registered");

                    componentStartUpSynchronizer.setComponentStatus(Component.MockIaaS, true);
                    log.info("Mock IaaS service component activated");
                } catch (Exception e) {
                    log.error("An error occurred when starting mock instances", e);
                }
            }
        };
        Thread mockIaasActivatorThread = new Thread(mockIaasActivator);
        mockIaasActivatorThread.start();
    }

    protected void deactivate(ComponentContext context) {
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }

        try {
            UserRegistry registry = registryService.getGovernanceSystemRegistry();
            ServiceReferenceHolder.getInstance().setRegistry(registry);
        } catch (RegistryException e) {
            String msg = "Failed when retrieving Governance System Registry.";
            log.error(msg, e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Un-setting the Registry Service");
        }
        ServiceReferenceHolder.getInstance().setRegistry(null);
    }

    protected void setComponentStartUpSynchronizer(ComponentStartUpSynchronizer componentStartUpSynchronizer) {
        ServiceReferenceHolder.getInstance().setComponentStartUpSynchronizer(componentStartUpSynchronizer);
    }

    protected void unsetComponentStartUpSynchronizer(ComponentStartUpSynchronizer componentStartUpSynchronizer) {
        ServiceReferenceHolder.getInstance().setComponentStartUpSynchronizer(null);
    }
}
