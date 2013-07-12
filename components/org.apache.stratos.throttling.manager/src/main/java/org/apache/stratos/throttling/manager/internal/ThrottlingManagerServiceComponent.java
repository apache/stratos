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
package org.apache.stratos.throttling.manager.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.throttling.manager.utils.Util;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.rule.kernel.config.RuleEngineConfigService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.billing.mgt.api.MultitenancyBillingInfo;
import org.apache.stratos.usage.api.TenantUsageRetriever;

/**
 * @scr.component name="org.wso2.carbon.throttling.manager"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="billingManager.service"
 * interface="org.wso2.carbon.billing.core.BillingManager" cardinality="1..1"
 * policy="dynamic" bind="setBillingManager" unbind="unsetBillingManager"
 * @scr.reference name="rule.engine.config.server.component"
 * interface="org.wso2.carbon.rule.kernel.config.RuleEngineConfigService"
 * cardinality="1..1"
 * policy="dynamic" bind="setRuleEngineConfigService"
 * unbind="unsetRuleEngineConfigService"
 * @scr.reference name="metering.service"
 * interface="org.apache.stratos.usage.api.TenantUsageRetriever" cardinality="1..1"
 * policy="dynamic" bind="setTenantUsageRetriever" unbind="unsetTenantUsageRetriever"
 * @scr.reference name="org.wso2.carbon.billing.mgt.api.MultitenancyBillingInfo"
 * interface="org.wso2.carbon.billing.mgt.api.MultitenancyBillingInfo" cardinality="1..1"
 * policy="dynamic" bind="setMultitenancyBillingInfo" unbind="unsetMultitenancyBillingInfo"
 */
public class ThrottlingManagerServiceComponent {
    private static Log log = LogFactory.getLog(ThrottlingManagerServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            Util.setBundleContext(context.getBundleContext());
            Util.loadThrottlingRules();
            Util.registerThrottlingRuleInvoker();
            Util.initializeThrottling();
            log.debug(" Multitenancy Throttling Manager bundle is activated ");
        } catch (Throwable e) {
            log.error(" Multitenancy Throttling Manager bundle failed activating ", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Multitenancy Throttling Manager bundle is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        Util.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        Util.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        Util.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        Util.setRealmService(null);
    }

    protected void setBillingManager(BillingManager billingManager) {
        log.debug("Receiving billingManager service");
        Util.setBillingManager(billingManager);
    }

    protected void unsetBillingManager(BillingManager billingManager) {
        log.debug("Halting billingManager service");
        Util.setBillingManager(null);
    }

    protected void setRuleEngineConfigService(RuleEngineConfigService ruleEngineConfigService) {
        Util.setRuleEngineConfigService(ruleEngineConfigService);
    }

    protected void unsetRuleEngineConfigService(RuleEngineConfigService ruleEngineConfigService) {
        // we are not dynamically removing schedule helpers
    }

    protected void setTenantUsageRetriever(TenantUsageRetriever tenantUsageRetriever) {
        log.debug("Setting Tenant Usage Retriever service");
        Util.setTenantUsageRetriever(tenantUsageRetriever);
    }

    protected void unsetTenantUsageRetriever(TenantUsageRetriever tenantUsageRetriever) {
        log.debug("Unsetting Tenant Usage Retriever service");
        Util.setBillingManager(null);
    }

    protected void setMultitenancyBillingInfo(MultitenancyBillingInfo mtBillingInfo) {
        log.debug("Setting MT billing info service");
        Util.setMultitenancyBillingInfo(mtBillingInfo);
    }

    protected void unsetMultitenancyBillingInfo(MultitenancyBillingInfo mtBillingInfo) {
        log.debug("Unsetting MT billing info service");
        Util.setMultitenancyBillingInfo(null);
    }
}
