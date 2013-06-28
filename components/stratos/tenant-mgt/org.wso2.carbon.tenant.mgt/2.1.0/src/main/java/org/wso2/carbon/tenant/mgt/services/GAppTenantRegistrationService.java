package org.wso2.carbon.tenant.mgt.services;

import org.wso2.carbon.core.multitenancy.persistence.TenantPersistor;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.tenant.mgt.exception.TenantManagementException;
import org.wso2.carbon.tenant.mgt.internal.TenantMgtServiceComponent;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.util.UUIDGenerator;

public class GAppTenantRegistrationService {
	
    private static final String GOOGLE_APPS_IDP_NAME = "GoogleApps";
    
    private static final Log log = LogFactory.getLog(GAppTenantRegistrationService.class);
    
    
    public boolean isRegisteredAsGoogleAppDomain(String domain) throws TenantManagementException {

        TenantManager tenantManager = 
            TenantMgtServiceComponent.getRealmService().getTenantManager();
        try {
            int tenantId = tenantManager.getTenantId(domain);

            if (tenantId == -1) {
                return false;
            }

            Tenant tenant = (Tenant) tenantManager.getTenant(tenantId);
            RealmConfiguration realmConfig = tenant.getRealmConfig();
            String value = realmConfig.getUserStoreProperties().get(
                            UserCoreConstants.RealmConfig.PROPERTY_EXTERNAL_IDP);

            if (value == null) {
                throw new TenantManagementException(
                        "This domain has been already registered as a non-Google App domain");
            }

            if (value.equals(GOOGLE_APPS_IDP_NAME)) {
                return true;
            }
            
            throw new TenantManagementException(
                    "This domain has been already registered with a different External IdP");
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new TenantManagementException("System error occured while connecting user store");
        }
    }
    
    public boolean registerGoogleAppsTenant(
                                TenantInfoBean tenantInfoBean)throws TenantManagementException {
        try {
            int tenantId = -1;
            Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
            TenantPersistor tenantPersistor = TenantMgtServiceComponent.getTenantPersistor();

            MultiTenantRealmConfigBuilder builder =
                    TenantMgtServiceComponent.getRealmService().getMultiTenantRealmConfigBuilder();
            TenantMgtConfiguration tenantMgtConfiguration =
                    TenantMgtServiceComponent.getRealmService().getTenantMgtConfiguration();
            RealmConfiguration bootStrapRealmConfig =
                    TenantMgtServiceComponent.getRealmService().getBootstrapRealmConfiguration();
            RealmConfiguration realmConfigToPersist =
                    builder.getRealmConfigForTenantToPersist(bootStrapRealmConfig,
                            tenantMgtConfiguration, tenant, -1);
            realmConfigToPersist.getUserStoreProperties().put(
                    UserCoreConstants.RealmConfig.PROPERTY_EXTERNAL_IDP, GOOGLE_APPS_IDP_NAME);
            tenant.setRealmConfig(realmConfigToPersist);
            tenant.setAdminPassword(UUIDGenerator.getUUID());

            tenantId = tenantPersistor.persistTenant(tenant);
            tenantInfoBean.setTenantId(tenantId);

            TenantMgtUtil.addClaimsToUserStoreManager(tenant);

            // Notify tenant addition
            try {
                TenantMgtUtil.triggerAddTenant(tenantInfoBean);
            } catch (StratosException e) {
                String msg = "Error in notifying tenant addition.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }

            // adding the subscription entry
            try {
                if (TenantMgtServiceComponent.getBillingService() != null) {
                    tenantInfoBean.setTenantId(tenantId); // required for the following method
                    TenantMgtServiceComponent.getBillingService().addUsagePlan(tenant,
                            tenantInfoBean.getUsagePlan());
                    if (log.isDebugEnabled()) {
                        log.debug("Subscription added successfully for the tenant: " +
                                  tenantInfoBean.getTenantDomain());
                    }
                }
            } catch (Exception e) {
                log.error("Error occurred while adding the subscription for tenant: " +
                          tenantInfoBean.getTenantDomain() + " " + e.getMessage(), e);
            }

            TenantMgtServiceComponent.getRealmService().getTenantManager().activateTenant(tenantId);
            return true;
        } catch (Exception e) {
            log.error("Error creating tenant for GooogleApp market place implementation", e);
            throw new TenantManagementException(
                    "Error creating tenant for GooogleApp market place implementation", e);
        }
    }
}
