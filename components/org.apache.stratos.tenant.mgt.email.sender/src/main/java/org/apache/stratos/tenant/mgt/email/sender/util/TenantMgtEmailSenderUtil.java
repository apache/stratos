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
package org.apache.stratos.tenant.mgt.email.sender.util;

import org.apache.stratos.tenant.mgt.email.sender.internal.DataHolder;
import org.apache.stratos.email.sender.api.EmailSender;
import org.apache.stratos.email.sender.api.EmailSenderConfiguration;
import org.wso2.carbon.email.verification.util.EmailVerifcationSubscriber;
import org.wso2.carbon.email.verification.util.EmailVerifierConfig;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.AuthenticationObserver;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility methods for the email sender component
 */
public class TenantMgtEmailSenderUtil {

    private static final Log log = LogFactory.getLog(TenantMgtEmailSenderUtil.class);
    
    private static EmailSender successMsgSender;
    private static EmailSender tenantCreationNotifier;
    private static EmailSender tenantActivationNotifier;
    private static EmailSender passwordResetMsgSender;
    private static EmailVerifierConfig emailVerifierConfig;
    private static EmailVerifierConfig superTenantEmailVerifierConfig = null;
    
    public static void init() {
        initTenantActivatedEmailSender();
        initSuperTenantNotificationEmailSender();
        initEmailVerificationSender();
        initPasswordResetEmailSender();
    }
    
    /**
     * Sends validation mail to the tenant admin upon the tenant creation
     *
     * @param tenantInfoBean    - registered tenant's details
     * @throws Exception, if the sending mail failed
     */
    public static void sendTenantCreationVerification(
                                              TenantInfoBean tenantInfoBean) throws Exception {
        String confirmationKey = generateConfirmationKey(tenantInfoBean,
                DataHolder.getRegistryService().getConfigSystemRegistry(
                        MultitenantConstants.SUPER_TENANT_ID));

        if (CommonUtil.isTenantActivationModerated()) {
            requestSuperTenantModeration(tenantInfoBean, confirmationKey);
        } else {
            //request for verification
            requestUserVerification(tenantInfoBean, confirmationKey);
        }
    }
    
    /**
     * Emails the tenant admin notifying the account creation.
     *
     * @param tenantId tenant Id
     */
    public static void notifyTenantInitialActivation(int tenantId) {
        TenantManager tenantManager = DataHolder.getTenantManager();
        String firstName = "";
        String domainName = "";
        String adminName = "";
        String email = "";
        try {
            Tenant tenant = tenantManager.getTenant(tenantId);
            domainName = tenant.getDomain();
            firstName = ClaimsMgtUtil.getFirstName(DataHolder.getRealmService(), tenantId);
            adminName = tenant.getAdminName();
            email = tenant.getEmail(); 
        } catch (Exception e) {
            String msg = "Unable to get the tenant with the tenant domain";
            log.error(msg, e);
            // just catch from here.
        }

        // load the mail configuration
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put("first-name", firstName);
        userParams.put("user-name", adminName);
        userParams.put("domain-name", domainName);

        try {
            successMsgSender.sendEmail(email, userParams);
        } catch (Exception e) {
            // just catch from here..
            String msg = "Error in sending the notification email.";
            log.error(msg, e);
        }
        
        // send the notification message to the super tenant
        notifyTenantActivationToSuperAdmin(domainName, adminName, email);
    }
    
    /**
     * Emails the super admin notifying the account creation for a new tenant.
     *
     * @param tenantInfoBean - tenant details
     */
    public static void notifyTenantCreationToSuperAdmin(TenantInfoBean tenantInfoBean) {
        String notificationEmailAddress = CommonUtil.getNotificationEmailAddress();

        if (notificationEmailAddress.trim().equals("")) {
            if (log.isDebugEnabled()) {
                log.debug("No super-admin notification email address is set to notify upon a" +
                          " tenant registration");
            }
            return;
        }

        Map<String, String> userParams = initializeSuperTenantNotificationParams(
                tenantInfoBean.getTenantDomain(), tenantInfoBean.getAdmin(), 
                tenantInfoBean.getEmail());

        try {
            tenantCreationNotifier.sendEmail(notificationEmailAddress, userParams);
        } catch (Exception e) {
            // just catch from here..
            String msg = "Error in sending the notification email.";
            log.error(msg, e);
        }
    }
    
    public static void notifyResetPassword(TenantInfoBean tenantInfoBean) throws Exception {
        int tenantId = tenantInfoBean.getTenantId();
        String firstName = ClaimsMgtUtil.getFirstName(DataHolder.getRealmService(), tenantId);

        // load the mail configuration
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put("user-name", tenantInfoBean.getAdmin());
        userParams.put("first-name", firstName);
        userParams.put("domain-name", tenantInfoBean.getTenantDomain());
        userParams.put("password", tenantInfoBean.getAdminPassword());

        try {
            passwordResetMsgSender.sendEmail(tenantInfoBean.getEmail(), userParams);
        } catch (Exception e) {
            // just catch from here..
            String msg = "Error in sending the notification email.";
            log.error(msg, e);
        }
    }
    
    /**
     * Emails the super admin notifying the account activation for an unactivated tenant.
     *
     * @param domainName tenant domain
     * @param adminName  tenant admin
     * @param email      tenant's email address
     */
    private static void notifyTenantActivationToSuperAdmin(String domainName, String adminName,
                                                          String email) {
        String notificationEmailAddress = CommonUtil.getNotificationEmailAddress();

        if (notificationEmailAddress.trim().equals("")) {
            if (log.isDebugEnabled()) {
                log.debug("No super-admin notification email address is set to notify upon a"
                          + " tenant activation");
            }
            return;
        }

        Map<String, String> userParams =
                initializeSuperTenantNotificationParams(domainName, adminName, email);

        try {
            tenantActivationNotifier.sendEmail(notificationEmailAddress, userParams);
        } catch (Exception e) {
            // just catch from here..
            String msg = "Error in sending the notification email.";
            log.error(msg, e);
        }
    }
    
    /**
     * generates the confirmation key for the tenant
     *
     * @param tenantInfoBean            - tenant details
     * @param superTenantConfigSystemRegistry
     *                          - super tenant config system registry.
     * @return confirmation key
     * @throws RegistryException if generation of the confirmation key failed.
     */
    private static String generateConfirmationKey(TenantInfoBean tenantInfoBean,
                                                  UserRegistry superTenantConfigSystemRegistry
                                                  ) throws RegistryException {
        // generating the confirmation key
        String confirmationKey = UUIDGenerator.generateUUID();
        UserRegistry superTenantGovernanceSystemRegistry;
        try {
            superTenantGovernanceSystemRegistry =
                DataHolder.getRegistryService().getGovernanceSystemRegistry(
                        MultitenantConstants.SUPER_TENANT_ID);
        } catch (RegistryException e) {
            String msg = "Exception in getting the governance system registry for the super tenant";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        Resource resource;
        String emailVerificationPath = StratosConstants.ADMIN_EMAIL_VERIFICATION_FLAG_PATH +
                                       RegistryConstants.PATH_SEPARATOR + 
                                       tenantInfoBean.getTenantId();
        try {
            if (superTenantGovernanceSystemRegistry.resourceExists(emailVerificationPath)) {
                resource = superTenantGovernanceSystemRegistry.get(emailVerificationPath);
            } else {
                resource = superTenantGovernanceSystemRegistry.newResource();
            }
            resource.setContent(confirmationKey);
        } catch (RegistryException e) {
            String msg = "Error in creating the resource or getting the resource" +
                         "from the email verification path";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        // email is not validated yet, this prop is used to activate the tenant later.
        resource.addProperty(StratosConstants.IS_EMAIL_VALIDATED, "false");
        resource.addProperty(StratosConstants.TENANT_ADMIN, tenantInfoBean.getAdmin());
        try {
            superTenantGovernanceSystemRegistry.put(emailVerificationPath, resource);
        } catch (RegistryException e) {
            String msg = "Error in putting the resource to the super tenant registry" +
                         " for the email verification path";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }

        // Used for * as a Service impl.
        // Store the cloud service from which the register req. is originated.
        if (tenantInfoBean.getOriginatedService() != null) {
            String originatedServicePath =
                    StratosConstants.ORIGINATED_SERVICE_PATH +
                    StratosConstants.PATH_SEPARATOR +
                    StratosConstants.ORIGINATED_SERVICE +
                    StratosConstants.PATH_SEPARATOR + tenantInfoBean.getTenantId();
            try {
                Resource origServiceRes = superTenantConfigSystemRegistry.newResource();
                origServiceRes.setContent(tenantInfoBean.getOriginatedService());
                superTenantGovernanceSystemRegistry.put(originatedServicePath, origServiceRes);
            } catch (RegistryException e) {
                String msg = "Error in putting the originated service resource "
                             + "to the governance registry";
                log.error(msg, e);
                throw new RegistryException(msg, e);
            }
        }
        initializeRegistry(tenantInfoBean.getTenantId());
        if (log.isDebugEnabled()) {
            log.debug("Successfully generated the confirmation key.");
        }
        return confirmationKey;
    }
    
    /**
     * Sends mail for the super tenant for the account moderation. Once super tenant clicks the
     * link provided in the email, the tenant will be activated.
     *
     * @param tenantInfoBean      - the tenant who registered an account
     * @param confirmationKey confirmation key.
     * @throws Exception if an exception is thrown from EmailVerificationSubscriber.
     */
    private static void requestSuperTenantModeration(TenantInfoBean tenantInfoBean, 
                                                       String confirmationKey) throws Exception {
        try {
            Map<String, String> dataToStore = new HashMap<String, String>();
            dataToStore.put("email", CommonUtil.getSuperAdminEmail());
            dataToStore.put("first-name", tenantInfoBean.getFirstname());
            dataToStore.put("userName", tenantInfoBean.getAdmin());
            dataToStore.put("tenantDomain", tenantInfoBean.getTenantDomain());
            dataToStore.put("confirmationKey", confirmationKey);

            DataHolder.getEmailVerificationService().requestUserVerification(
                    dataToStore, superTenantEmailVerifierConfig);
            if (log.isDebugEnabled()) {
                log.debug("Email verification for the tenant registration.");
            }
        } catch (Exception e) {
            String msg = "Error in notifying the super tenant on the account creation for " +
                         "the domain: " + tenantInfoBean.getTenantDomain();
            log.error(msg);
            throw new Exception(msg, e);
        }
    }
    
    /**
     * request email verification from the user.
     *
     * @param tenantInfoBean - Tenant information
     * @param confirmationKey confirmation key.
     * @throws Exception if an exception is thrown from EmailVerificationSubscriber.
     */
    private static void requestUserVerification(TenantInfoBean tenantInfoBean, 
                                                String confirmationKey) throws Exception {
        try {
            Map<String, String> dataToStore = new HashMap<String, String>();
            dataToStore.put("email", tenantInfoBean.getEmail());
            dataToStore.put("first-name", tenantInfoBean.getFirstname());
            dataToStore.put("userName", tenantInfoBean.getAdmin());
            dataToStore.put("tenantDomain", tenantInfoBean.getTenantDomain());
            dataToStore.put("confirmationKey", confirmationKey);

            EmailVerifcationSubscriber emailVerifier = DataHolder.getEmailVerificationService();
            emailVerifier.requestUserVerification(dataToStore, emailVerifierConfig);
            if (log.isDebugEnabled()) {
                log.debug("Email verification for the tenant registration.");
            }
        } catch (Exception e) {
            String msg = "Error in notifying tenant of domain: " + tenantInfoBean.getTenantDomain();
            log.error(msg);
            throw new Exception(msg, e);
        }
    }
    
    /**
     * Initializes the registry for the tenant.
     *
     * @param tenantId tenant id.
     */
    private static void initializeRegistry(int tenantId) {
        BundleContext bundleContext = DataHolder.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                    new ServiceTracker(bundleContext,
                                       AuthenticationObserver.class.getName(),
                                       null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).startedAuthentication(tenantId);
                }
            }
            tracker.close();
        }
    }
    
    /**
     * loads the notification configurations for the mail to super tenant for account creations
     * and activations.
     */
    private static void initSuperTenantNotificationEmailSender() {
        // Tenant Registration Email Configurations
        String tenantRegistrationEmailConfFile =
                CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        StratosConstants.EMAIL_CONFIG + File.separator +
                        "email-new-tenant-registration.xml";
        EmailSenderConfiguration newTenantRegistrationEmailConf =
                EmailSenderConfiguration.loadEmailSenderConfiguration(
                        tenantRegistrationEmailConfFile);
        tenantCreationNotifier = new EmailSender(newTenantRegistrationEmailConf);

        // Tenant Activation Email Configurations
        String tenantActivationEmailConfFile =
                CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        StratosConstants.EMAIL_CONFIG + File.separator +
                        "email-new-tenant-activation.xml";
        EmailSenderConfiguration newTenantActivationEmailConf =
                EmailSenderConfiguration
                        .loadEmailSenderConfiguration(tenantActivationEmailConfFile);
        tenantActivationNotifier = new EmailSender(newTenantActivationEmailConf);
    }
    
    /**
     * loads the Email configuration files to be sent on the tenant registrations.
     */
    private static void initEmailVerificationSender() {
        String confXml =
                CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        StratosConstants.EMAIL_CONFIG + File.separator + "email-registration.xml";
        try {
            emailVerifierConfig = org.wso2.carbon.email.verification.util.Util
                            .loadeMailVerificationConfig(confXml);
        } catch (Exception e) {
            String msg = "Email Registration Configuration file not found. "
                            + "Pls check the repository/conf/email folder.";
            log.error(msg);
        }
        String superTenantConfXml =
                CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        StratosConstants.EMAIL_CONFIG + File.separator +
                        "email-registration-moderation.xml";
        try {
            superTenantEmailVerifierConfig = org.wso2.carbon.email.verification.util.Util
                            .loadeMailVerificationConfig(superTenantConfXml);
        } catch (Exception e) {
            String msg =
                    "Email Moderation Configuration file not found. "
                            + "Pls check the repository/conf/email folder.";
            log.error(msg);
        }
    }

    /**
     * loads the Email configuration files to be sent on the tenant activations.
     */
    private static void initTenantActivatedEmailSender() {
        String confFilename =
                CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        StratosConstants.EMAIL_CONFIG + File.separator +
                        "email-registration-complete.xml";
        EmailSenderConfiguration successMsgConfig =
                EmailSenderConfiguration.loadEmailSenderConfiguration(confFilename);
        successMsgSender = new EmailSender(successMsgConfig);
    }

    private static void initPasswordResetEmailSender() {
        String passwordResetConfigFileName = CarbonUtils.getCarbonConfigDirPath()+ File.separator + 
                StratosConstants.EMAIL_CONFIG + File.separator + "email-password-reset.xml";
        EmailSenderConfiguration passwordResetMsgConfig =
            EmailSenderConfiguration.loadEmailSenderConfiguration(passwordResetConfigFileName);
        passwordResetMsgSender = new EmailSender(passwordResetMsgConfig);
    }

    /**
     * Initializes the super tenant notification parameters
     *
     * @param domainName - tenant domain
     * @param adminName  - tenant admin
     * @param email      - tenant email
     * @return the parameters
     */
    private static Map<String, String> initializeSuperTenantNotificationParams(
            String domainName, String adminName, String email) {
        TenantManager tenantManager = DataHolder.getTenantManager();
        String firstName = "";
        String lastName = "";
        try {
            int tenantId = tenantManager.getTenantId(domainName);
            firstName = ClaimsMgtUtil.getFirstName(DataHolder.getRealmService(), tenantId);
            lastName = ClaimsMgtUtil.getLastName(DataHolder.getRealmService(), tenantId);

        } catch (Exception e) {
            String msg = "Unable to get the tenant with the tenant domain";
            log.error(msg, e);
            // just catch from here.
        }

        // load the mail configuration
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put("user-name", adminName);
        userParams.put("domain-name", domainName);
        userParams.put("email-address", email);
        userParams.put("first-name", firstName);
        userParams.put("last-name", lastName);
        return userParams;
    }
}
