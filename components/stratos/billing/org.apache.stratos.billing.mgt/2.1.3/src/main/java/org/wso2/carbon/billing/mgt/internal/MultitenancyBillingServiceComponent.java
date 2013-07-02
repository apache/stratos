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
package org.wso2.carbon.billing.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.billing.mgt.util.Util;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.usage.api.TenantUsageRetriever;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.carbon.billing.mgt"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="billingManager.service"
 * interface="org.wso2.carbon.billing.core.BillingManager" cardinality="1..1"
 * policy="dynamic" bind="setBillingManager" unbind="unsetBillingManager"
 * @scr.reference name="tenant.usage.retriever.service"
 * interface="org.wso2.carbon.usage.api.TenantUsageRetriever" cardinality="1..1"
 * policy="dynamic" bind="setTenantUsageRetriever" unbind="unsetTenantUsageRetriever"
 */
public class MultitenancyBillingServiceComponent {
    private static Log log = LogFactory.getLog(MultitenancyBillingServiceComponent.class);
    private static ConfigurationContextService contextService;

    protected void activate(ComponentContext context) {
        try {
            Util.registerSubscriptionFeedingHandlers(context.getBundleContext());
            Util.scheduleBilling();
            Util.registerBillingInfo(context.getBundleContext());
            Util.initDataAccessManager();
            Util.initializeThrottling(context.getBundleContext());
            Util.registerTenantBillingService(context.getBundleContext());
            log.debug("******* Multitenancy Billing bundle is activated ******* ");
        } catch (Throwable e) {
            log.error("******* Multitenancy Billing bundle failed activating ****", e);
        }
    }

    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext context) {
        log.debug("******* Multitenancy Billing is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        Util.setRegistryService(registryService);
    }

    @SuppressWarnings("unused")
    protected void unsetRegistryService(RegistryService registryService) {
        Util.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        Util.setRealmService(realmService);
    }

    @SuppressWarnings("unused")
    protected void unsetRealmService(RealmService realmService) {
        Util.setRealmService(null);
    }

    public void setBillingManager(BillingManager billingManager) {
        log.debug("Receiving billingManager service");
        Util.setBillingManager(billingManager);
    }

    @SuppressWarnings("unused")
    public void unsetBillingManager(BillingManager billingManager) {
        log.debug("Unsetting billingManager service");
        Util.setBillingManager(null);
    }

    public void setTenantUsageRetriever(TenantUsageRetriever tenantUsageRetriever) {
        log.debug("Setting Tenant Usage Retriever service");
        Util.setTenantUsageRetriever(tenantUsageRetriever);
    }

    @SuppressWarnings("unused")
    public void unsetTenantUsageRetriever(TenantUsageRetriever tenantUsageRetriever) {
        log.debug("Unsetting Tenant Usage Retriever service");
        Util.setBillingManager(null);
    }

}
