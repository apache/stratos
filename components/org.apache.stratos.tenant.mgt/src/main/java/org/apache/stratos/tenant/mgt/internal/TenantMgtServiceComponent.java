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
package org.apache.stratos.tenant.mgt.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.apache.stratos.common.TenantBillingService;
import org.apache.stratos.common.listeners.TenantMgtListener;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.common.util.StratosConfiguration;
import org.apache.stratos.tenant.mgt.internal.util.TenantMgtRampartUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @scr.component name="org.apache.stratos.tenant.mgt" immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="configuration.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"
 * @scr.reference name="org.apache.stratos.tenant.mgt.listener.service"
 *                interface="org.apache.stratos.common.listeners.TenantMgtListener"
 *                cardinality="0..n" policy="dynamic"
 *                bind="setTenantMgtListenerService"
 *                unbind="unsetTenantMgtListenerService"
 * @scr.reference name="default.tenant.billing.service"
 *                interface="org.apache.stratos.common.TenantBillingService"
 *                cardinality="0..1" policy="dynamic"
 *                bind="setTenantBillingService"
 *                unbind="unsetTenantBillingService"
 */
public class TenantMgtServiceComponent {
    private static Log log = LogFactory.getLog(TenantMgtServiceComponent.class);

    private static final String GAPP_TENANT_REG_SERVICE_NAME = "GAppTenantRegistrationService";

    private static RealmService realmService;
    private static RegistryService registryService;
    
    private static ConfigurationContextService configurationContextService;
    
    private static List<TenantMgtListener> tenantMgtListeners = new ArrayList<TenantMgtListener>();
    private static TenantBillingService billingService = null;

    protected void activate(ComponentContext context) {
        try {

            // Loading the stratos configurations from Stratos.xml
            if (CommonUtil.getStratosConfig() == null) {
                StratosConfiguration stratosConfig = CommonUtil.loadStratosConfiguration();
                CommonUtil.setStratosConfig(stratosConfig);
            }

            // Loading the EULA
            if (CommonUtil.getEula() == null) {
                String eula = CommonUtil.loadTermsOfUsage();
                CommonUtil.setEula(eula);
            }
            
            populateRampartConfig(configurationContextService.
					              getServerConfigContext().getAxisConfiguration());

            log.debug("******* Tenant Config bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Tenant Config bundle failed activating ****", e);
        }
    }

    protected void setTenantMgtListenerService(TenantMgtListener tenantMgtListener) {
        addTenantMgtListener(tenantMgtListener);
    }

    protected void unsetTenantMgtListenerService(TenantMgtListener tenantMgtListener) {
        removeTenantMgtListener(tenantMgtListener);
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Governance Tenant Config bundle is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        TenantMgtServiceComponent.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {
        setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        TenantMgtServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        setRealmService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        log.debug("Receiving ConfigurationContext Service");
        TenantMgtServiceComponent.configurationContextService = configurationContextService;

    }

    protected void unsetConfigurationContextService(ConfigurationContextService configurationContextService) {
        log.debug("Unsetting ConfigurationContext Service");
        setConfigurationContextService(null);
    }

    public static void addTenantMgtListener(TenantMgtListener tenantMgtListener) {
        tenantMgtListeners.add(tenantMgtListener);
        sortTenantMgtListeners();
    }

    public static void removeTenantMgtListener(TenantMgtListener tenantMgtListener) {
        tenantMgtListeners.remove(tenantMgtListener);
        sortTenantMgtListeners();
    }

    public static void sortTenantMgtListeners() {
        Collections.sort(tenantMgtListeners, new Comparator<TenantMgtListener>() {
            public int compare(TenantMgtListener o1, TenantMgtListener o2) {
                return o1.getListenerOrder() - o2.getListenerOrder();
            }
        });
    }

    public static List<TenantMgtListener> getTenantMgtListeners() {
        return tenantMgtListeners;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    public static ConfigurationContext getConfigurationContext() {
        if (configurationContextService.getServerConfigContext() == null) {
            return null;
        }
        return configurationContextService.getServerConfigContext();
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }


    public static RealmService getRealmService() {
        return realmService;
    }

    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static RealmConfiguration getBootstrapRealmConfiguration() {
        return realmService.getBootstrapRealmConfiguration();
    }

    public static UserRegistry getGovernanceSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getGovernanceSystemRegistry(tenantId);
    }

    public static UserRegistry getConfigSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getConfigSystemRegistry(tenantId);
    }

    
   /** Updates RelyingPartyService with Crypto information
    *
    * @param config AxisConfiguration
    * @throws Exception
    */
   private void populateRampartConfig(AxisConfiguration config) throws Exception {
       AxisService service;
       // Get the RelyingParty Service to update security policy with keystore information
       service = config.getService(GAPP_TENANT_REG_SERVICE_NAME);
       if (service == null) {
           String msg = GAPP_TENANT_REG_SERVICE_NAME + " is not available in the Configuration Context";
           log.error(msg);
           throw new Exception(msg);
       }
       // Create a Rampart Config with default crypto information
       Policy rampartConfig = TenantMgtRampartUtil.getDefaultRampartConfig();
       // Add the RampartConfig to service policy
       service.getPolicySubject().attachPolicy(rampartConfig);

   }
   
   protected void setTenantBillingService(TenantBillingService tenantBillingService) {
       billingService = tenantBillingService;
   }
   
   protected void unsetTenantBillingService(TenantBillingService tenantBilling) {
       setTenantBillingService(null);
   }
   
   public static TenantBillingService getBillingService() {
       return billingService;
   }
   
   
}
