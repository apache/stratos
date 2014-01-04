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
package org.apache.stratos.tenant.mgt.services;

import org.wso2.carbon.captcha.mgt.beans.CaptchaInfoBean;
import org.wso2.carbon.captcha.mgt.constants.CaptchaMgtConstants;
import org.wso2.carbon.captcha.mgt.util.CaptchaUtil;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.tenant.mgt.internal.TenantMgtServiceComponent;
import org.apache.stratos.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.apache.stratos.tenant.mgt.core.TenantPersistor;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * TenantSelfRegistration - This is the Web service that will be called when
 * tenants register themselves
 */
public class TenantSelfRegistrationService {
    private static final Log log = LogFactory.getLog(TenantSelfRegistrationService.class);

    /**
     * Registers a tenant - Tenant Self Registration
     *
     * @param tenantInfoBean  - tenantInformation
     * @param captchaInfoBean - captchaInformation
     * @return String UUID
     * @throws Exception if the tenant registration fails.
     */
    public String registerTenant(TenantInfoBean tenantInfoBean, CaptchaInfoBean captchaInfoBean)
            throws Exception {
        // validate the email
        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            String msg = "Invalid email is provided.";
            log.error(msg, e);
            throw new AxisFault(msg);
        }
        // validate the domain
        String domainName = tenantInfoBean.getTenantDomain();
        try {
            TenantMgtUtil.validateDomain(domainName);
        } catch (Exception e) {
            String msg = "Domain Validation Failed.";
            log.error(msg, e);
            throw new AxisFault(msg);
        }
        // validate the first/last names
        String firstname = tenantInfoBean.getFirstname();
        String lastname = tenantInfoBean.getLastname();
        try {
            CommonUtil.validateName(firstname, "First Name");
            CommonUtil.validateName(lastname, "Last Name");
        } catch (Exception e) {
            String msg = "First/Last Name Validation Failed.";
            log.error(msg, e);
            throw new AxisFault(msg);
        } // now validate the captcha
        try {
            CaptchaUtil.validateCaptcha(captchaInfoBean);
            if (log.isDebugEnabled()) {
                log.debug("Captcha Successfully Validated.");
            }
        } catch (Exception e) {
            String msg = CaptchaMgtConstants.CAPTCHA_ERROR_MSG;
            log.error(msg, e);
            throw new AxisFault(msg);
        } finally {
            try {
                CaptchaUtil.cleanCaptcha(captchaInfoBean.getSecretKey());
            } catch (Exception e) {
                String msg = "Error in cleaning captcha. ";
                log.error(msg, e);
                // not throwing the exception in finally more up.
            }
        }
        // persists the tenant.
        Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
        TenantPersistor persistor = new TenantPersistor();
        int tenantId = persistor.persistTenant(tenant, true, tenantInfoBean.getSuccessKey(), 
                tenantInfoBean.getOriginatedService(),false);
        tenantInfoBean.setTenantId(tenantId);
        TenantMgtUtil.addClaimsToUserStoreManager(tenant);
        
        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        //adding the subscription entry
        try {
            if (TenantMgtServiceComponent.getBillingService() != null) {
                TenantMgtServiceComponent.getBillingService().addUsagePlan(tenant,
                        tenantInfoBean.getUsagePlan());
                if (log.isDebugEnabled()) {
                    log.debug("Subscription added successfully for the tenant: " +
                              tenantInfoBean.getTenantDomain());
                }
            }
        } catch (Exception e) {
            String msg = "Error occurred while adding the subscription for tenant: " + domainName;
            log.error(msg, e);
        }

        // If Email Validation is made optional, tenant will be activated now.
        if (CommonUtil.isTenantManagementEmailsDisabled() ||
                !CommonUtil.isEmailValidationMandatory()) {
            TenantMgtUtil.activateTenantInitially(tenantInfoBean, tenantId);
        }
        return TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId());
    }

    /**
     * Check if the selected domain is available to register
     *
     * @param domainName domain name
     * @return true, if the domain is available to register
     * @throws Exception, if unable to get the tenant manager, or get the tenant id
     *                    from manager.
     */
    public boolean checkDomainAvailability(String domainName) throws Exception {
        TenantManager tenantManager = TenantMgtServiceComponent.getTenantManager();
        int tenantId = tenantManager.getTenantId(domainName);
        if (log.isDebugEnabled()) {
            log.debug("Tenant Domain is available to register.");
        }
        return tenantId < 0; // no tenant exists with the same tenant domain
    }

    // use a boolean instead of string.

    /**
     * Validates or Suggests a domain.
     *
     * @param domain     tenant domain
     * @param successKey success key
     * @return domain name
     * @throws Exception if exception in validating or suggesting the tenant domain.
     */
    public String validateOrSuggestDomain(String domain, String successKey) throws Exception {
        if (successKey != null && !successKey.equals("")) {
            if (CommonUtil.validateDomainFromSuccessKey(
                    TenantMgtServiceComponent.getGovernanceSystemRegistry(
                            MultitenantConstants.SUPER_TENANT_ID), domain, successKey)) {
                return domain;
            }
        }
        // otherwise domain is not correct

        return "null";
    }

    /**
     * Generates a random Captcha
     *
     * @return captchaInfoBean
     * @throws Exception, if exception in cleaning old captchas or generating new
     *                    captcha image.
     */
    public CaptchaInfoBean generateRandomCaptcha() throws Exception {
        // we will clean the old captchas asynchronously
        CaptchaUtil.cleanOldCaptchas();
        return CaptchaUtil.generateCaptchaImage();
    }
}
