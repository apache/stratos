/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.account.mgt.services;

import org.wso2.carbon.account.mgt.internal.AccountMgtServiceComponent;
import org.wso2.carbon.account.mgt.util.Util;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Email Validation Service
 */
public class EmailValidationService {
    private static final Log log = LogFactory.getLog(EmailValidationService.class);

    /**
     * Proceed updating the contact email address
     *
     * @param domain          tenant domain
     * @param email           email address
     * @param confirmationKey confirmation key.
     * @throws Exception, RegistryException.
     */
    public void proceedUpdateContact(String domain, String email, String confirmationKey)
            throws Exception {

        TenantManager tenantManager = Util.getTenantManager();
        int tenantId;

        try {
            tenantId = tenantManager.getTenantId(domain);
        } catch (UserStoreException e) {
            String msg = "Error in adding tenant, tenant domain: " + domain + ".";
            log.error(msg);
            throw new RegistryException(msg, e);
        }

        UserRegistry superTenantSystemRegistry = Util.getGovernanceSystemRegistry(
                MultitenantConstants.SUPER_TENANT_ID);
        String emailVerificationPath =
                StratosConstants.ADMIN_EMAIL_VERIFICATION_FLAG_PATH +
                RegistryConstants.PATH_SEPARATOR + tenantId;
        if (!superTenantSystemRegistry.resourceExists(emailVerificationPath)) {
            // the confirmation key should exist,otherwise fail registraion
            String msg = "The confirmationKey doesn't exist in service.";
            log.error(msg);
            throw new RegistryException(msg);
        }
        Resource resource = superTenantSystemRegistry.get(emailVerificationPath);
        String actualConfirmationKey = null;
        Object content = resource.getContent();
        if (content instanceof String) {
            actualConfirmationKey = (String) content;
        } else if (content instanceof byte[]) {
            actualConfirmationKey = new String((byte[]) content);
        }

        if (actualConfirmationKey == null || !actualConfirmationKey.equals(confirmationKey)) {
            // validation will fail.
            String msg = "The email confirmation key is not matching";
            log.error(msg);
            throw new RegistryException(msg);
        }

        resource.setProperty(email, "true");

        // now we will really update the tenant email
        Tenant tenant;
        try {
            tenant = tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg =
                    "Error in retrieving the tenant information for the tenant id: " + tenantId +
                    ".";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }

        // If TenantActivation is moderated, the mail address associated with the validation link
        // would not be the tenant email. Otherwise, the validation mail would be the tenant email.
        if (!CommonUtil.isTenantActivationModerated()) {
            tenant.setEmail(email);
        }

        try {
            tenantManager.updateTenant(tenant);
        } catch (UserStoreException e) {
            String msg =
                    "Error in updating the tenant information for the tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }

        // activate the tenant on successful validation of the email, if it is not already activated.
        if ("false".equals(resource.getProperty(StratosConstants.IS_EMAIL_VALIDATED))) {
            tenantManager.activateTenant(tenantId);
            // set the registry flag
            resource.editPropertyValue(StratosConstants.IS_EMAIL_VALIDATED, "false", "true");

            if (log.isDebugEnabled()) {
                log.debug("Tenant : " + tenantId + " is activated after validating the " +
                          "email of the tenant admin.");
            }
            
            //Notify all the listeners that tenant has been activated for the first time
            Util.alertTenantInitialActivation(tenantId);

            //Activating the usage plan
            try{
                AccountMgtServiceComponent.getBillingService().activateUsagePlan(domain);
            }catch(Exception e){
                log.error("Error occurred while activating the usage plan for tenant: " + domain
                        + " tenant Id: " + tenantId, e);
            }

        }
        
        //This is considered an update. Hence notify the update to all listeners
        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        tenantInfoBean.setTenantId(tenantId);
        tenantInfoBean.setTenantDomain(domain);
        tenantInfoBean.setEmail(email);
        Util.alertTenantUpdate(tenantInfoBean);

        // update the registry
        superTenantSystemRegistry.put(emailVerificationPath, resource);
    }
}
