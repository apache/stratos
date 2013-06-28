/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.throttling.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.MeteringAccessValidationUtils;
import org.wso2.carbon.stratos.common.util.StratosConfiguration;
import org.wso2.carbon.throttling.agent.cache.TenantThrottlingInfo;
import org.wso2.carbon.throttling.agent.cache.ThrottlingActionInfo;
import org.wso2.carbon.throttling.agent.cache.ThrottlingInfoCache;
import org.wso2.carbon.throttling.agent.cache.ThrottlingInfoCacheUpdaterTask;
import org.wso2.carbon.throttling.agent.client.MultitenancyThrottlingServiceClient;
import org.wso2.carbon.throttling.agent.client.ThrottlingRuleInvoker;
import org.wso2.carbon.throttling.agent.conf.ThrottlingAgentConfiguration;
import org.wso2.carbon.throttling.agent.listeners.PerRegistryRequestListener;
import org.wso2.carbon.throttling.agent.listeners.PerUserAddListener;
import org.wso2.carbon.user.core.listener.UserStoreManagerListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThrottlingAgent {
    private static final Log log = LogFactory.getLog(ThrottlingAgent.class);

    private static final String CONFIG_FILE = "throttling-agent-config.xml";
    private static final String MANAGER_SERVER_URL_PARAM_NAME = "managerServiceUrl";
    private static final String USERNAME_PARAM_NAME = "userName";
    private static final String PASSWORD_PARAM_NAME = "password";

    private ThrottlingAgentConfiguration configuration;

    private RegistryService registryService;

    private RealmService realmService;

    private ConfigurationContextService configurationContextService;

    private ThrottlingInfoCache throttlingInfoCache;

    private ServiceTracker throttlingRuleInvokerTracker = null;

    private ScheduledExecutorService scheduler;

    private BundleContext bundleContext;
    private StratosConfiguration stratosConfiguration=null;

    public StratosConfiguration getStratosConfiguration() {
        return stratosConfiguration;
    }

    public void setStratosConfiguration(StratosConfiguration stratosConfiguration) {
        this.stratosConfiguration = stratosConfiguration;
    }

    public ThrottlingAgent(BundleContext bundleContext) throws Exception {
        this.scheduler = Executors.newScheduledThreadPool(1, new ThrottlingAgentThreadFactory());
        this.throttlingInfoCache = new ThrottlingInfoCache();
        this.bundleContext = bundleContext;
    }

    public void init() throws RegistryException {

        if("true".equals(ServerConfiguration.getInstance().getFirstProperty("EnableMetering"))){

            UserRegistry superTenantGovernanceRegistry = registryService.getGovernanceSystemRegistry();

            scheduler.scheduleAtFixedRate(
                    new ThrottlingInfoCacheUpdaterTask(throttlingInfoCache, superTenantGovernanceRegistry), 2, 15,
                    TimeUnit.MINUTES);

            PerRegistryRequestListener.registerPerRegistryRequestListener(RegistryContext.getBaseInstance());
            if (bundleContext != null) {
                bundleContext.registerService(UserStoreManagerListener.class.getName(),
                        new PerUserAddListener(), null);
            }

        }
        throttlingRuleInvokerTracker = new ServiceTracker(bundleContext, ThrottlingRuleInvoker.class.getName(),
                null);
        throttlingRuleInvokerTracker.open();
    }

    public ThrottlingInfoCache getThrottlingInfoCache() {
        return throttlingInfoCache;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RegistryService getRegistryService(){
        return this.registryService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        this.configurationContextService = configurationContextService;
    }

    public ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    public void updateThrottlingCacheForTenant() throws Exception {
        // TODO: Need to refactor this and updater task

        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        String tenantValidationInfoResourcePath =
                StratosConstants.TENANT_USER_VALIDATION_STORE_PATH +
                        RegistryConstants.PATH_SEPARATOR + tenantId;
        try {
            if (registryService.getGovernanceSystemRegistry().resourceExists(tenantValidationInfoResourcePath)) {
                Resource tenantValidationInfoResource =
                        registryService.getGovernanceSystemRegistry().get(tenantValidationInfoResourcePath);
                Properties properties = tenantValidationInfoResource.getProperties();
                Set<String> actions = MeteringAccessValidationUtils.getAvailableActions(properties);
                for (String action : actions) {
                    String blocked =
                            tenantValidationInfoResource.getProperty(MeteringAccessValidationUtils
                                    .generateIsBlockedPropertyKey(action));

                    String blockMessage =
                            tenantValidationInfoResource.getProperty(MeteringAccessValidationUtils
                                    .generateErrorMsgPropertyKey(action));
                    TenantThrottlingInfo tenantThrottlingInfo = throttlingInfoCache.getTenantThrottlingInfo(tenantId);
                    if (tenantThrottlingInfo == null) {
                        throttlingInfoCache.addTenant(tenantId);
                        tenantThrottlingInfo = throttlingInfoCache.getTenantThrottlingInfo(tenantId);
                    }
                    tenantThrottlingInfo.updateThrottlingActionInfo(action,
                            new ThrottlingActionInfo("true".equals(blocked), blockMessage));
                }
            }
        } catch (RegistryException re) {
            String msg =
                    "Error while getting throttling info for tenant " + tenantId + ".";
            log.error(msg, re);
        }
    }

    private ThrottlingAgentConfiguration loadThrottlingConfiguration() throws Exception {
        // it is acceptable that throttling agent file is not present, when the
        // embedded rule invoker is in use.
        ThrottlingAgentConfiguration throttlingAgentConfig = null;
        String configFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator + CONFIG_FILE;
        if (new File(configFileName).exists()) {
            throttlingAgentConfig = new ThrottlingAgentConfiguration(configFileName);
        } else {
            log.warn("Throttling agent config file is not present. File name: " + configFileName + ".");
        }

        return throttlingAgentConfig;
    }

    public ThrottlingRuleInvoker getThrottlingRuleInvoker() throws Exception {
        // first check the OSGi service exists, if so return it
        ThrottlingRuleInvoker embeddedRuleInvoker =
                (ThrottlingRuleInvoker) throttlingRuleInvokerTracker.getService();
        if (embeddedRuleInvoker != null) {
            return embeddedRuleInvoker;
        }


        if (stratosConfiguration == null) {
            String msg =
                    "Neither embedded nor web service implementation of throttling rule invoker found.";
            log.error(msg);
            throw new Exception(msg);
        }
        String serverUrl = stratosConfiguration.getManagerServiceUrl();
        String userName =stratosConfiguration.getAdminUserName() ;
        String password = stratosConfiguration.getAdminPassword();

        return new MultitenancyThrottlingServiceClient(serverUrl, userName, password);
    }

    public void executeManagerThrottlingRules(int tenantId) throws Exception {
        ThrottlingRuleInvoker client = getThrottlingRuleInvoker();
        client.executeThrottlingRules(tenantId);
    }


    class ThrottlingAgentThreadFactory implements ThreadFactory {
        private int counter = 0;

        public Thread newThread(Runnable r) {
            return new Thread(r, "ThrottlingAgentThreadFactory-" + counter++);
        }
    }

    public void executeThrottlingRules(int tenantId) {
        try {
            executeManagerThrottlingRules(tenantId);
            updateThrottlingCacheForTenant();
        } catch (Exception e) {
            log.error("Error in executing throttling rules");
        }
    }

}
