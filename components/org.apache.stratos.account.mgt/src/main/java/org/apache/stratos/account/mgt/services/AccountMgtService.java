/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
package org.apache.stratos.account.mgt.services;

import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.email.verification.util.EmailVerifcationSubscriber;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.account.mgt.beans.AccountInfoBean;
import org.apache.stratos.account.mgt.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Account Management Service Class
 */
public class AccountMgtService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(AccountMgtService.class);

    /**
     * Updates the contact email.
     *
     * @param contactEmail email
     * @throws Exception, if update contact failed.
     */
    public void updateContact(String contactEmail) throws Exception {
        EmailVerifcationSubscriber emailverifier = Util.getEmailVerificationService();

        TenantManager tenantManager = Util.getTenantManager();
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();

        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information for the tenant id: " +
                    tenantId + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        // generating the confirmation key
        String confirmationKey = UUIDGenerator.generateUUID();
        UserRegistry superTenantSystemRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        Resource resource;
        String emailVerificationPath = StratosConstants.ADMIN_EMAIL_VERIFICATION_FLAG_PATH +
                RegistryConstants.PATH_SEPARATOR + tenantId;
        if (superTenantSystemRegistry.resourceExists(emailVerificationPath)) {
            resource = superTenantSystemRegistry.get(emailVerificationPath);
        } else {
            resource = superTenantSystemRegistry.newResource();
        }
        resource.setContent(confirmationKey);
        superTenantSystemRegistry.put(emailVerificationPath, resource);

        try {
            Map<String, String> datatostore = new HashMap<String, String>();
            datatostore.put("first-name",
                    ClaimsMgtUtil.getFirstName(Util.getRealmService(), tenantId));
            datatostore.put("email", contactEmail);
            datatostore.put("userName", tenant.getAdminName());
            datatostore.put("tenantDomain", tenant.getDomain());
            datatostore.put("confirmationKey", confirmationKey);
            emailverifier.requestUserVerification(datatostore, Util.getEmailVerifierConfig());
        } catch (Exception e) {
            String msg = "Error in adding tenant, tenant domain: " + tenant.getDomain() + ".";
            log.error(msg);
            throw new Exception(msg, e);
        }
    }

    /**
     * gets the contact of the tenant admin
     *
     * @throws Exception, if getting the contact email address failed.
     * @return, the contact email address
     */
    public String getContact() throws Exception {
        TenantManager tenantManager = Util.getTenantManager();
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();
        // get the tenant information from the tenant manager
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information for the tenant id: " +
                    tenantId + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        return tenant.getEmail();
    }

    /**
     * Updates the fullname information
     *
     * @param accountInfoBean profile information stored in AccountInfoBean
     * @return true, if updated successfully.
     * @throws Exception UserStoreException.
     */
    public boolean updateFullname(AccountInfoBean accountInfoBean) throws Exception {
        TenantManager tenantManager = Util.getTenantManager();
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();
        // get the tenant information from the tenant manager
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information for the tenant id: " +
                    tenantId + ".";
            log.info(msg, e);
            throw new Exception(msg, e);
        }
        RealmService realmService = Util.getRealmService();
        try {
            Map<String, String> claimsMap = new HashMap<String, String>();
            claimsMap.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME,
                    accountInfoBean.getFirstname());
            claimsMap.put(UserCoreConstants.ClaimTypeURIs.SURNAME, accountInfoBean.getLastname());
            UserStoreManager userStoreManager =
                    (UserStoreManager) realmService.getTenantUserRealm(tenantId)
                            .getUserStoreManager();
            userStoreManager.setUserClaimValues(
                    ClaimsMgtUtil.getAdminUserNameFromTenantId(realmService, tenantId),
                    claimsMap, UserCoreConstants.DEFAULT_PROFILE);
            log.info("FirstName: " + accountInfoBean.getFirstname() +
                    " has been updated to the tenant admin " +
                    ClaimsMgtUtil.getAdminUserNameFromTenantId(realmService, tenantId) + " of " +
                    tenant.getDomain());
            
            //Notify tenant update to all listeners
            TenantInfoBean tenantInfoBean = new TenantInfoBean();
            tenantInfoBean.setTenantId(tenantId);
            tenantInfoBean.setFirstname(accountInfoBean.getFirstname());
            tenantInfoBean.setLastname(accountInfoBean.getLastname());
            Util.alertTenantUpdate(tenantInfoBean);
            
            return true;
        } catch (Exception e) {
            // this is expected, as many users haven't given their fullnames
            // during their registration.
            String msg =
                    "Error in updating the firstname: " + accountInfoBean.getFirstname() +
                            " for the tenant admin: " +
                            ClaimsMgtUtil.getAdminUserNameFromTenantId(realmService, tenantId);
            log.info(msg);
            throw new Exception(msg, e);
        }
    }

    /**
     * gets the profile information - saved as claims -
     * currently saved claims are first name and last name - hence the profile so far is a fullname.
     *
     * @return AccountInfoBean - Currently depicts the fullname as an object.
     * @throws Exception, UserStoreException
     */
    public AccountInfoBean getFullname() throws Exception {

        String firstname = "", lastname = "";
        TenantManager tenantManager = Util.getTenantManager();
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();
        // get the tenant information from the tenant manager
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information for the tenant id: " +
                    tenantId + ".";
            log.info(msg, e);
            throw new Exception(msg, e);
        }

        // getting the other parameters from the claims.
        try {
            firstname = ClaimsMgtUtil.getFirstName(Util.getRealmService(), tenantId);

        } catch (Exception e) {
            String msg = "Error in retrieving the firstname for the admin of the domain " +
                    tenant.getDomain();
            log.info(msg);
        }
        try {
            lastname = ClaimsMgtUtil.getLastName(Util.getRealmService(), tenantId);
        } catch (Exception e) {
            // this is expected, as many users haven't given their lastnames
            // during their registration.
            String msg = "Error in retrieving the Lastname for the admin of the domain " +
                    tenant.getDomain();
            log.info(msg);
        }

        AccountInfoBean accountInfoBean = new AccountInfoBean();
        accountInfoBean.setFirstname(firstname);
        accountInfoBean.setLastname(lastname);
        return accountInfoBean;
    }


    /**
     * deactivates the tenant
     *
     * @throws Exception, if deactivating the tenant failed.
     */
    public void deactivate() throws Exception {
        // The one who have a proper permission will be able to deactivate the tenant.
        TenantManager tenantManager = Util.getTenantManager();
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();
        try {
            tenantManager.deactivateTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in deactivating the tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        //Notify tenant deactivation to Listeners
        Util.alertTenantDeactivation(tenantId);
    }

    /**
     * checks whether the domain is validated.
     *
     * @return true, if the domain has been validated.
     * @throws Exception, if the domain validation failed.
     */
    public boolean isDomainValidated() throws Exception {
        // first we will get the current domain name
        TenantManager tenantManager = Util.getTenantManager();
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();
        // get the tenant information from the tenant manager
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information for the tenant id: " +
                    tenantId + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        String domainName = tenant.getDomain();
        TenantMgtUtil.validateDomain(domainName);

        String domainValidationPath = StratosConstants.TENANT_DOMAIN_VERIFICATION_FLAG_PATH +
                RegistryConstants.PATH_SEPARATOR + tenantId;
        UserRegistry superTenantRegistry = Util.getGovernanceSystemRegistry(
                MultitenantConstants.SUPER_TENANT_ID);
        if (superTenantRegistry.resourceExists(domainValidationPath)) {
            Resource validationFlagR = superTenantRegistry.get(domainValidationPath);
            return "true".equals(validationFlagR.getProperty(domainName));
        }
        return false;
    }

    /**
     * If the domain validation has been completed.
     *
     * @param validatedDomain the domain being validated.
     * @param successKey      success key
     * @return true, if the domain has been validated successfully.
     * @throws Exception, if the domain validation failed.
     */
    public boolean finishedDomainValidation(
            String validatedDomain, String successKey) throws Exception {
        // create a flag on domain validation, so that we can move the content
        // of the current domain name to the new validated domain name
        if (!CommonUtil.validateDomainFromSuccessKey(Util.getGovernanceSystemRegistry(
                MultitenantConstants.SUPER_TENANT_ID), validatedDomain, successKey)) {
            String msg = "Domain: " + validatedDomain + " is not validated against successKey: " +
                    successKey + ".";
            log.error(msg);
            throw new Exception(msg);
        }

        // we keep an entry about domain validation here.

        // first we will get the current domain name
        UserRegistry registry = (UserRegistry) getGovernanceRegistry();
        if (registry == null) {
            // we can't continue without having a valid registry in the session
            String msg = "Error in retrieving the registry for the login tenant.";
            log.error(msg);
            throw new Exception(msg);
        }
        int tenantId = registry.getTenantId();

        // keep the domain validation path.

        String domainValidationPath = StratosConstants.TENANT_DOMAIN_VERIFICATION_FLAG_PATH +
                RegistryConstants.PATH_SEPARATOR + tenantId;
        UserRegistry superTenantRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        Resource validationFlagR = superTenantRegistry.newResource();
        validationFlagR.setProperty(validatedDomain, "true");
        superTenantRegistry.put(domainValidationPath, validationFlagR);

        return true;
    }

    /**
     * Check whether the domain is available.
     *
     * @param domainName domain name
     * @return true, if the domain is available to register.
     * @throws Exception, if the domain validation failed.
     */
    public boolean checkDomainAvailability(String domainName) throws Exception {
        TenantManager tenantManager = Util.getTenantManager();
        int tenantId = tenantManager.getTenantId(domainName);
        return tenantId < 0;
    }

    /**
     * check whether the email has been validated.
     *
     * @throws Exception, if the validation failed.
     * @return, true if already validated.
     */
    public boolean isEmailValidated() throws Exception {
        UserRegistry userRegistry = (UserRegistry) getGovernanceRegistry();
        if (userRegistry.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no email validation step required for super tenant
            return true;
        }

        String email = getContact();
        UserRegistry superTenantSystemRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        String emailVerificationPath = StratosConstants.ADMIN_EMAIL_VERIFICATION_FLAG_PATH +
                RegistryConstants.PATH_SEPARATOR +
                userRegistry.getTenantId();
        if (!superTenantSystemRegistry.resourceExists(emailVerificationPath)) {
            // the confirmation key should exist,otherwise fail registration
            return false;
        }
        Resource resource = superTenantSystemRegistry.get(emailVerificationPath);

        return "true".equals(resource.getProperty(email));
    }
}
