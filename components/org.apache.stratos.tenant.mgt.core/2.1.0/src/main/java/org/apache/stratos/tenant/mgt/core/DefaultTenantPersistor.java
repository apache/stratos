/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stratos.tenant.mgt.core;

import org.apache.stratos.tenant.mgt.core.internal.TenantMgtCoreServiceComponent;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.multitenancy.persistence.TenantPersistor;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.config.CloudServiceConfigParser;
import org.wso2.carbon.stratos.common.config.CloudServicesDescConfig;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.CloudServicesUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.apache.stratos.tenant.mgt.core.util.TenantCoreUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.core.AuthorizationManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TenantPersistenceManager - Methods related to persisting the tenant.
 */
public class DefaultTenantPersistor implements TenantPersistor {

    private static final Log log = LogFactory.getLog(DefaultTenantPersistor.class);
    private static final String ILLEGAL_CHARACTERS_FOR_PATH = ".*[~!#$;%^*()+={}\\[\\]\\|\\\\<>].*";

    private static CloudServicesDescConfig cloudServicesDesc = null;
        

    /**
     * Persists the given tenant
     * @param tenant - tenant to be persisted
     * @param checkDomainValidation - true, if domain is validated.
     * @param successKey - successKey
     * @param originatedService - The Service that the tenant registration was originated.
     * @return tenant Id - the tenant id
     * @throws Exception, if persisting tenant failed.
     */
    public int persistTenant(Tenant tenant, boolean checkDomainValidation, String successKey,
                             String originatedService) throws Exception {
        int tenantId;
        validateAdminUserName(tenant);
        String tenantDomain = tenant.getDomain();

        boolean isDomainAvailable = CommonUtil.isDomainNameAvailable(tenantDomain);
        if (!isDomainAvailable) {
            throw new Exception("Domain is not available to register");
        }

        RealmService realmService = TenantMgtCoreServiceComponent.getRealmService();
        RealmConfiguration realmConfig = realmService.getBootstrapRealmConfiguration();
        TenantMgtConfiguration tenantMgtConfiguration = realmService.getTenantMgtConfiguration();
        MultiTenantRealmConfigBuilder builder = TenantMgtCoreServiceComponent.
                getRealmService().getMultiTenantRealmConfigBuilder();
        RealmConfiguration realmConfigToPersist =
                builder.getRealmConfigForTenantToPersist(realmConfig, tenantMgtConfiguration,
                                                         tenant, -1);
        tenant.setRealmConfig(realmConfigToPersist);
        tenantId = addTenant(tenant);
        tenant.setId(tenantId);

        if (checkDomainValidation) { 
            if (successKey != null) {
                if (CommonUtil.validateDomainFromSuccessKey(TenantMgtCoreServiceComponent.
                        getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID),
                                                            tenant.getDomain(), successKey)) {
                    storeDomainValidationFlagToRegistry(tenant);
                } else {
                    String msg = "Failed to validate domain";
                    throw new Exception(msg);
                }
            }
        } else {
            storeDomainValidationFlagToRegistry(tenant);
        }
        
        try {
            doPostTenantCreationActions(tenant, originatedService);
        } catch (Exception e) {
            String msg = "Error performing post tenant creation actions";
            throw new Exception(msg, e);
        }

        return tenantId;
    }

    private void doPostTenantCreationActions(Tenant tenant,
                                             String originatedService) throws Exception {
        RealmService realmService = TenantMgtCoreServiceComponent.getRealmService();
        UserRealm userRealm;
        try {
            userRealm = (UserRealm) realmService.getTenantUserRealm(tenant.getId());
        } catch (UserStoreException e) {
            String msg = "Error in creating Realm for tenant: " + tenant.getDomain();
            throw new Exception(msg, e);
        }

        updateTenantAdminPassword(userRealm, tenant);
        TenantMgtCoreServiceComponent.getRegistryLoader().loadTenantRegistry(tenant.getId());
        copyUIPermissions(tenant.getId());

        TenantCoreUtil.setOriginatedService(tenant.getId(), originatedService);
        setActivationFlags(tenant.getId(), originatedService);

        TenantCoreUtil.initializeRegistry(tenant.getId());

    }

    /**
     * Store the domain validation flag in the registry if the domain has been
     * validated.
     * 
     * @param tenant - the tenant
     * @throws RegistryException, if storing the domain validation flag failed.
     */
    private void storeDomainValidationFlagToRegistry(Tenant tenant) throws RegistryException {

        try {
            String domainValidationPath = StratosConstants.TENANT_DOMAIN_VERIFICATION_FLAG_PATH +
                                                  RegistryConstants.PATH_SEPARATOR + tenant.getId();
            UserRegistry superTenantRegistry = TenantMgtCoreServiceComponent.
                    getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
            Resource validationFlagR = superTenantRegistry.newResource();
            validationFlagR.setProperty(tenant.getDomain(), "true");
            superTenantRegistry.put(domainValidationPath, validationFlagR);

        } catch (RegistryException e) {
            String msg = "Error in storing the domain validation flag to the registry";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Domain Validation Flag is stored to the registry.");
        }
    }

    /**
     * Adds a tenant to the tenant manager
     * 
     * @param tenant - the tenant
     * @return tenantId - the tenant id
     * @throws Exception - UserStoreException
     */
    private int addTenant(Tenant tenant) throws Exception {
        int tenantId;
        TenantManager tenantManager = TenantMgtCoreServiceComponent.getTenantManager();
        try {
            tenantId = tenantManager.addTenant(tenant);
            if (log.isDebugEnabled()) {
                log.debug("Tenant is successfully added: " + tenant.getDomain());
            }
        } catch (UserStoreException e) {
            String msg = "Error in adding tenant with domain: " + tenant.getDomain();
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        return tenantId;
    }

    /**
     * Sets the password for the tenant
     * 
     * @param tenant - the tenant
     * @param userRealm - user realm
     * @throws Exception - UserStoreException
     */
    private void updateTenantAdminPassword(UserRealm userRealm, Tenant tenant) throws Exception {
        try {
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            if (!userStoreManager.isReadOnly()) {
                userStoreManager.updateCredentialByAdmin(tenant.getAdminName(),
                                                         tenant.getAdminPassword());
                if (log.isDebugEnabled()) {
                    log.debug("Successfully set the password for the tenant.");
                }
            }
        } catch (UserStoreException e) {
            String msg = "Error in changing the tenant admin password for tenant domain: " +
                                 tenant.getDomain() + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Authorize the role
     *
     * @param tenantId - tenant id
     * @throws Exception - UserStoreException
     */
    private void copyUIPermissions(int tenantId) throws Exception {
        try {
            UserRealm realm = (UserRealm) TenantMgtCoreServiceComponent.
                    getRealmService().getTenantUserRealm(tenantId);
            String adminRole = realm.getRealmConfiguration().getAdminRoleName();
            AuthorizationManager authMan = realm.getAuthorizationManager();
            // Authorize the admin role, if not authorized yet.
            if (!authMan.isRoleAuthorized(adminRole,
                                          CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION,
                                          UserMgtConstants.EXECUTE_ACTION)) {
                authMan.authorizeRole(adminRole, CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION,
                                      UserMgtConstants.EXECUTE_ACTION);
            }
        } catch (UserStoreException e) {
            String msg = "Error in authorizing the admin role.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Role has successfully been authorized.");
        }
    }
    
    private void setActivationFlags(int tenantId, String originalService) throws Exception {

        boolean useDefaultConfig = true;
        try {
            
            if(cloudServicesDesc == null ) { 
                cloudServicesDesc = CloudServiceConfigParser.loadCloudServicesConfiguration();
            }

            if (originalService != null &&
                !originalService.equals(StratosConstants.CLOUD_MANAGER_SERVICE) ) {
                CloudServicesUtil.activateOriginalAndCompulsoryServices(cloudServicesDesc,
                                                                        originalService, tenantId);
                useDefaultConfig = false;
            }

            if (useDefaultConfig) {
                CloudServicesUtil.activateAllServices(cloudServicesDesc, tenantId);
            }
        } catch (Exception e) {
            log.error("Error registering the originated service", e);
            throw e;
        }
        
    }

    /**
     * Validates that the chosen AdminUserName is valid.
     * 
     * @param tenant
     *            tenant information
     * @throws Exception
     *             UserStoreException
     */
    private void validateAdminUserName(Tenant tenant) throws Exception {
        UserRealm superTenantUserRealm =
                                        TenantMgtCoreServiceComponent.getRealmService().
                                                                      getBootstrapRealm();
        RealmConfiguration realmConfig = TenantMgtCoreServiceComponent.
                getBootstrapRealmConfiguration();
        String uniqueAcrossTenants = realmConfig.getUserStoreProperty(
                UserCoreConstants.RealmConfig.PROPERTY_USERNAME_UNIQUE);
        if ("true".equals(uniqueAcrossTenants)) {
            try {
                if (superTenantUserRealm.getUserStoreManager().isExistingUser(
                        tenant.getAdminName())) {
                    throw new Exception("User name : " + tenant.getAdminName() +
                                        " exists in the system. " +
                                        "Please pick another user name for tenant Administrator.");
                }
            } catch (UserStoreException e) {
                String msg = "Error in checking whether the user already exists in the system";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }
        if (tenant.getAdminName().matches(DefaultTenantPersistor.ILLEGAL_CHARACTERS_FOR_PATH)) {
            String msg = "The tenant admin ' " + tenant.getAdminName() +
                                 " ' contains one or more illegal characters" +
                                 " (~!@#$;%^*()+={}[]|\\<>)";
            log.error(msg);
            throw new Exception(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("Admin User Name has been validated.");
        }
    }

    /**
     * Persists the given tenant
     * @param tenant - tenant to be persisted
     * @return tenant Id
     * @throws Exception, if persisting tenant failed.
     */
    public int persistTenant(Tenant tenant) throws Exception {
        String tenantDomain = tenant.getDomain();
        int tenantId;
        validateAdminUserName(tenant);
        boolean isDomainAvailable = CommonUtil.isDomainNameAvailable(tenantDomain);
        if (!isDomainAvailable) {
            throw new Exception("Domain is not available to register");
        }

        tenantId = addTenant(tenant);
        tenant.setId(tenantId);

        try {
            doPostTenantCreationActions(tenant, null);
        } catch (Exception e) {
            String msg = "Error performing post tenant creation actions";
            if(log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new Exception(msg);
        }
        return tenantId;
    }
}
