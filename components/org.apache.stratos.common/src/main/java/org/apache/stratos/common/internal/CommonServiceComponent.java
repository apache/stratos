/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.internal;

import com.hazelcast.core.HazelcastInstance;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.clustering.impl.HazelcastDistributedObjectProvider;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.services.ComponentStartUpSynchronizer;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.common.util.StratosConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.caching.impl.DistributedMapProvider;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="apache.stratos.common" immediate="true"
 * @scr.reference name="hazelcast.instance.service" interface="com.hazelcast.core.HazelcastInstance"
 *                cardinality="0..1"policy="dynamic" bind="setHazelcastInstance" unbind="unsetHazelcastInstance"
 * @scr.reference name="distributedMapProvider" interface="org.wso2.carbon.caching.impl.DistributedMapProvider"
 *                cardinality="0..1" policy="dynamic" bind="setDistributedMapProvider" unbind="unsetDistributedMapProvider"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="configuration.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 *
 */
public class CommonServiceComponent {

    private static Log log = LogFactory.getLog(CommonServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            final BundleContext bundleContext = context.getBundleContext();
            if (CommonUtil.getStratosConfig() == null) {
                StratosConfiguration stratosConfig = CommonUtil.loadStratosConfiguration();
                CommonUtil.setStratosConfig(stratosConfig);
            }

            // Loading the EULA
            if (CommonUtil.getEula() == null) {
                String eula = CommonUtil.loadTermsOfUsage();
                CommonUtil.setEula(eula);
            }

            AxisConfiguration axisConfig = ServiceReferenceHolder.getInstance().getAxisConfiguration();
            if((axisConfig != null) && (axisConfig.getClusteringAgent() != null)) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            // Wait for the hazelcast instance to be available
                            long startTime = System.currentTimeMillis();
                            log.info("Waiting for the hazelcast instance to be initialized...");
                            while (ServiceReferenceHolder.getInstance().getHazelcastInstance() == null) {
                                Thread.sleep(1000);
                                if ((System.currentTimeMillis() - startTime) >= StratosConstants.HAZELCAST_INSTANCE_INIT_TIMEOUT) {
                                    throw new RuntimeException("Hazelcast instance was not initialized within "
                                            + StratosConstants.HAZELCAST_INSTANCE_INIT_TIMEOUT / 1000 + " seconds");
                                }
                            }
                            registerDistributedObjectProviderService(bundleContext);
                            registerComponentStartUpSynchronizer(bundleContext);

                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                };
                thread.setName("Distributed object provider registration thread");
                thread.start();
            } else {
                // Register distributed object provider service
                registerDistributedObjectProviderService(bundleContext);
                registerComponentStartUpSynchronizer(bundleContext);
            }

            // Register manager configuration service
            try {
                StratosConfiguration stratosConfiguration = CommonUtil.loadStratosConfiguration();
                bundleContext.registerService(StratosConfiguration.class.getName(), stratosConfiguration, null);
            } catch (Exception ex) {
                String msg = "An error occurred while registering stratos configuration service";
                log.error(msg, ex);
            }

            if (log.isInfoEnabled()) {
                log.info("Stratos common service component is activated");
            }
        } catch (Exception e) {
            log.error("Error in activating stratos common service component", e);
        }
    }

    private void registerDistributedObjectProviderService(BundleContext bundleContext) {
        DistributedObjectProvider distributedObjectProvider = new HazelcastDistributedObjectProvider();
        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(distributedObjectProvider);
        bundleContext.registerService(DistributedObjectProvider.class, distributedObjectProvider, null);
    }

    private void registerComponentStartUpSynchronizer(BundleContext bundleContext) {
        ComponentStartUpSynchronizer componentStartUpSynchronizer =
                new ComponentStartUpSynchronizerImpl(
                        ServiceReferenceHolder.getInstance().getDistributedObjectProvider());
        bundleContext.registerService(ComponentStartUpSynchronizer.class, componentStartUpSynchronizer, null);
    }

    protected void deactivate(ComponentContext context) {
        log.debug("Stratos common service is deactivated");
    }

    protected void setRegistryService(RegistryService registryService) {
        ServiceReferenceHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        ServiceReferenceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        ServiceReferenceHolder.getInstance().setRealmService(null);
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(hazelcastInstance);
    }

    public void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(null);
    }

    protected void setDistributedMapProvider(DistributedMapProvider mapProvider) {
        ServiceReferenceHolder.getInstance().setDistributedMapProvider(mapProvider);
    }

    protected void unsetDistributedMapProvider(DistributedMapProvider mapProvider) {
        ServiceReferenceHolder.getInstance().setDistributedMapProvider(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ServiceReferenceHolder.getInstance().setAxisConfiguration(cfgCtxService.getServerConfigContext().getAxisConfiguration());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ServiceReferenceHolder.getInstance().setAxisConfiguration(null);
    }
}
