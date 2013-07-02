/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.billing.core.internal;

import org.wso2.carbon.billing.core.BillingHandler;
import org.wso2.carbon.billing.core.BillingTenantMgtListenerImpl;
import org.wso2.carbon.billing.core.conf.BillingTaskConfiguration;
import org.wso2.carbon.billing.core.scheduler.ScheduleHelper;
import org.wso2.carbon.ndatasource.core.DataSourceService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.rule.kernel.config.RuleEngineConfigService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.stratos.common.util.StratosConfiguration;
import org.wso2.carbon.user.core.service.RealmService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osgi.service.component.ComponentContext;

/**
 * @scr.component name="org.wso2.carbon.billing.core" immediate="true"
 * @scr.reference name="registry.service"
 *                interface=
 *                "org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1"
 *                policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1"
 *                policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="org.wso2.carbon.billing.core.billing.handler.service"
 *                interface="org.wso2.carbon.billing.core.BillingHandler"
 *                cardinality="0..n"
 *                policy="dynamic" bind="setBillingHandlerService"
 *                unbind="unsetBillingHandlerService"
 * @scr.reference name="org.wso2.carbon.billing.core.task.schedule.helper.service"
 *                interface="org.wso2.carbon.billing.core.scheduler.ScheduleHelper"
 *                cardinality="0..n"
 *                policy="dynamic" bind="setScheduleHelperService"
 *                unbind="unsetScheduleHelperService"
 * @scr.reference name="rule.engine.config.server.component"
 *                interface="org.wso2.carbon.rule.kernel.config.RuleEngineConfigService"
 *                cardinality="1..1"
 *                policy="dynamic" bind="setRuleEngineConfigService"
 *                unbind="unsetRuleEngineConfigService"
 * @scr.reference name="org.wso2.carbon.tenant.mgt.listener.service"
 *                interface="org.wso2.carbon.stratos.common.listeners.TenantMgtListener"
 *                cardinality="0..n" policy="dynamic"
 *                bind="setTenantMgtListenerService"
 *                unbind="unsetTenantMgtListenerService"
 * @scr.reference name="datasources.service"
 *                interface="org.wso2.carbon.ndatasource.core.DataSourceService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setDataSourceService" unbind="unsetDataSourceService" 
 */
public class BillingServiceComponent {
    private static Log log = LogFactory.getLog(BillingServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            if(CommonUtil.getStratosConfig()==null){
                StratosConfiguration stratosConfig = CommonUtil.loadStratosConfiguration();
                CommonUtil.setStratosConfig(stratosConfig);
            }
            Util.initBillingManager(context.getBundleContext());
            Util.initDataAccessManager();

            BillingTenantMgtListenerImpl billingTenantMgtListener = new BillingTenantMgtListenerImpl();
            context.getBundleContext().registerService(
                    org.wso2.carbon.stratos.common.listeners.TenantMgtListener.class.getName(),
                    billingTenantMgtListener, null);
            if (log.isDebugEnabled()) {
                log.debug("******* Billing bundle is activated ******* ");
            }
        } catch (Throwable e) {
            log.error("******* Billing bundle failed activating ****", e);
        }
    }

    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext context) {
        Util.cleanBillingManager();
        log.debug("******* Billing is deactivated ******* ");
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

    protected void setBillingHandlerService(BillingHandler billingHandler) {
        BillingTaskConfiguration.addBillingHandler(billingHandler);
    }

    @SuppressWarnings("unused")
    protected void unsetBillingHandlerService(BillingHandler billingHandler) {
        // we are not dynamically removing billing handlers
    }

    protected void setScheduleHelperService(ScheduleHelper scheduleHelper) {
        BillingTaskConfiguration.addScheduleHelper(scheduleHelper);
    }

    @SuppressWarnings("unused")
    protected void unsetScheduleHelperService(ScheduleHelper scheduleHelper) {
        // we are not dynamically removing schedule helpers
    }

    protected void setRuleEngineConfigService(RuleEngineConfigService ruleEngineConfigService) {
        Util.setRuleEngineConfigService(ruleEngineConfigService);
    }

    @SuppressWarnings("unused")
    protected void unsetRuleEngineConfigService(RuleEngineConfigService ruleEngineConfigService) {
        Util.setRuleEngineConfigService(null);
    }

    protected void setTenantMgtListenerService(TenantMgtListener tenantMgtListener) {
        Util.addTenantMgtListenerService(tenantMgtListener);
    }

    protected void unsetTenantMgtListenerService(TenantMgtListener tenantMgtListener) {
        Util.removeTenantMgtListenerService(tenantMgtListener);
    }

    protected void setDataSourceService(DataSourceService dataSourceService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Data Sources Service");
        }
        Util.setDataSourceService(dataSourceService);
    }

    protected void unsetDataSourceService(DataSourceService dataSourceService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Data Sources Service");
        }
        Util.setDataSourceService(null);
    }
}
