/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.billing.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.billing.core.*;
import org.wso2.carbon.billing.mgt.api.DefaultTenantBilling;
import org.wso2.carbon.billing.mgt.api.MultitenancyBillingInfo;
import org.wso2.carbon.billing.mgt.handlers.MultitenancySubscriptionFeedingHandler;
import org.wso2.carbon.stratos.common.TenantBillingService;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.usage.api.TenantUsageRetriever;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.throttling.agent.ThrottlingAgent;
import org.wso2.carbon.utils.ConfigurationContextService;

public class Util {
    private static BillingManager billingManager = null;
    private static DataAccessManager dataAccessManager = null;
    private static RegistryService registryService;
    private static RealmService realmService;
    private static TenantUsageRetriever tenantUsageRetriever;
    private static MultitenancyBillingInfo billingInfo;

    public static ConfigurationContextService getContextService() {
        return contextService;
    }

    public static void setContextService(ConfigurationContextService contextService) {
        Util.contextService = contextService;
    }

    private static ServiceTracker throttlingRuleInvokerTracker = null;
    private static BundleContext bundleContext;
    private static Log log = LogFactory.getLog(Util.class);
    private static ConfigurationContextService contextService;

    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }

    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    public static void setTenantUsageRetriever(TenantUsageRetriever tenantUsageRetriever) {
        Util.tenantUsageRetriever = tenantUsageRetriever;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static TenantUsageRetriever getTenantUsageRetriever() {
        return tenantUsageRetriever;
    }

    public static TenantManager getTenantManager() {
        if (realmService == null) {
            return null;
        }
        return realmService.getTenantManager();
    }

    public static BillingManager getBillingManager() {
        return billingManager;
    }

    public static void setBillingManager(BillingManager billingManager) {
        Util.billingManager = billingManager;
    }

    public static void registerSubscriptionFeedingHandlers(BundleContext bundleContext) {
        bundleContext.registerService(BillingHandler.class.getName(),
                new MultitenancySubscriptionFeedingHandler(), null);
    }
    
    public static void registerTenantBillingService(BundleContext bundleContext) {
        bundleContext.registerService(TenantBillingService.class.getName(),
                new DefaultTenantBilling(), null);
    }

    public static void scheduleBilling() throws BillingException {
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_SCHEDULED_TASK_ID);
        if (billingEngine != null) {
            billingEngine.scheduleBilling();
        } else {
            log.info("No billing engine for scheduled tasks");
        }

    }

    public static void registerBillingInfo(BundleContext bundleContext) throws Exception {
        billingInfo = new MultitenancyBillingInfo();
        bundleContext.registerService(MultitenancyBillingInfo.class.getName(), billingInfo, null);
    }

    public static MultitenancyBillingInfo getMultitenancyBillingInfo() {
        return billingInfo;
    }

    public static DataAccessManager getDataAccessManager() {
        return dataAccessManager;
    }

    public static void setDataAccessManager(DataAccessManager dataAccessManager) {
        Util.dataAccessManager = dataAccessManager;
    }

    public static void initDataAccessManager() {
        DataAccessManager dataAccessManager = new DataAccessManager(
                billingManager.getBillingConfiguration().getDataSource());
        Util.dataAccessManager = dataAccessManager;
    }

    /**
     * This method used to create service tracker that tracks throttlingAgent service which
     * registered when throttling agent starts
     *
     * @param bundleContext bundle context that belongs to component
     */
    public static void initializeThrottling(BundleContext bundleContext) {
        throttlingRuleInvokerTracker = new ServiceTracker(bundleContext, ThrottlingAgent.class.getName(),
                null);
        throttlingRuleInvokerTracker.open();
    }

    /**
     * This method updates the throttling rules for given tenant and update the cache at manager
     *
     * @param tenantId Tenant Id of the tenant that need to update throttling rules at manager
     */
    public static void executeThrottlingRules(int tenantId) {
        try {
            ThrottlingAgent embeddedRuleInvoker =
                    (ThrottlingAgent) throttlingRuleInvokerTracker.getService();
            if (embeddedRuleInvoker != null) {
                embeddedRuleInvoker.executeThrottlingRules(tenantId);
            }
        } catch (Exception e) {
            log.error("Error in executing throttling rules in manager" + e.toString());
        }
    }
}
