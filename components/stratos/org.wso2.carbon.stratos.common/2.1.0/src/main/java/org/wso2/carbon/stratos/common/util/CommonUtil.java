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
package org.wso2.carbon.stratos.common.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.internal.CloudCommonServiceComponent;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.AccessControlConstants;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.AuthorizationManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Common Utility methods for Stratos.
 * Now this class has been growing with several util methods - Should refactor accordingly.
 */
public class CommonUtil {
    private static final Log log = LogFactory.getLog(CommonUtil.class);
    private static StratosConfiguration stratosConfig;
    private static String eula;


    private static final String ILLEGAL_CHARACTERS_FOR_EMAIL =
            ".*[\\(\\)\\<\\>\\,\\;\\:\\\\\\\"\\[\\]].*";
    private static final String EMAIL_FILTER_STRING = "^[^@]+@[^@.]+\\.[^@]*\\w\\w$";
    private static Pattern emailFilterPattern = Pattern.compile(EMAIL_FILTER_STRING);
    private static Pattern illegalCharactersPatternForEmail = Pattern
            .compile(ILLEGAL_CHARACTERS_FOR_EMAIL);

    public static StratosConfiguration getStratosConfig() {
        return stratosConfig;
    }

    public static void setStratosConfig(StratosConfiguration stratosConfig) {
        CommonUtil.stratosConfig = stratosConfig;
    }

    public static String getEula() {
        return eula;
    }

    public static void setEula(String eula) {
        CommonUtil.eula = eula;
    }

    /**
     * Checks whether the email validation is mandatory from the configuration file.
     *
     * @return true, if the email validation is mandatory to login. Default is false.
     */
    public static boolean isEmailValidationMandatory() {
        boolean isEmailValidationMandatory = false; //false by default.
        if (stratosConfig != null) {   //make sure the configuration exists.
            isEmailValidationMandatory = stratosConfig.getEmailValidationRequired();
        }
        return isEmailValidationMandatory;
    }

    /**
     * Checks whether the email sending is enabled from the configuration file.
     *
     * @return true, if the email sending is disabled. By default, this is disabled, and tenant
     * activation is done without any email sending.
     */
    public static boolean isTenantManagementEmailsDisabled() {
        boolean isEmailsDisabled = true; //true by default.
        if (stratosConfig != null) {   //make sure the configuration exists.
            isEmailsDisabled = stratosConfig.isEmailsDisabled();
        }
        return isEmailsDisabled;
    }

    public static String getSuperAdminEmail() {
        return stratosConfig.getSuperAdminEmail();
    }

    public static String getAdminUserName() {
        return stratosConfig.getAdminUserName();
    }
    
    public static String getAdminPassword() {
        return stratosConfig.getAdminPassword();
    }

    public static boolean isTenantActivationModerated() {
        return stratosConfig.isTenantActivationModerated();
    }

    public static boolean isChargedOnRegistration() {
        return stratosConfig.isChargeOnRegistration();
    }

    /**
     * Checks whether it is for the public cloud setup or Mars.
     *
     * @return true, if it is not for the private cloud setups. Default is true.
     */
    public static boolean isPublicCloudSetup() {
        boolean isPublicCloudSetup = true;  // true by default.
        if (stratosConfig != null) { //make sure the configuration exists.
            isPublicCloudSetup = stratosConfig.isPublicCloudSetup();
        }
        return isPublicCloudSetup;
    }

    /**
     * Gets the notification email address of the admin upon the tenant creation.
     *
     * @return notification email address for the tenant creations.
     */
    public static String getNotificationEmailAddress() {
        String notificationEmailAddress = "";
        if (stratosConfig != null) {
            notificationEmailAddress = stratosConfig.getNotificationEmail();
            try {
                validateEmail(notificationEmailAddress);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Invalid Email Address provided for tenant creation notification. " +
                             "Please check whether the parameter NotificationEmail" +
                             " is set properly in " +
                             StratosConstants.STRATOS_CONF_FILE, e);
                }
                return "";
            }
        }
        return notificationEmailAddress;
    }

    public static String getMonthString(Calendar calendar) {
        int currentMonth = calendar.get(Calendar.MONTH);

        String[] monthArr = new DateFormatSymbols().getMonths();
        String month = monthArr[currentMonth];
        return calendar.get(Calendar.YEAR) + "-" + month;
    }

    public static String getMonthString(int relativeMonth) {
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.add(Calendar.MONTH, relativeMonth);

        return CommonUtil.getMonthString(newCalendar);
    }

    public static Date getDateFromMonthString(String yearMonth) throws ParseException {
        DateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MMM");
        return yearMonthFormat.parse(yearMonth);
    }

    public static String getCurrentMonthString() {
        Calendar newCalendar = Calendar.getInstance();

        return CommonUtil.getMonthString(newCalendar);
    }


    public static void setAnonAuthorization(String path, UserRealm userRealm)
            throws RegistryException {

        if (userRealm == null) {
            return;
        }

        try {
            AuthorizationManager accessControlAdmin = userRealm.getAuthorizationManager();
            String everyoneRole = CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME;

            accessControlAdmin.authorizeRole(everyoneRole, path, ActionConstants.GET);
            accessControlAdmin.denyRole(everyoneRole, path, ActionConstants.PUT);
            accessControlAdmin.denyRole(everyoneRole, path, ActionConstants.DELETE);
            accessControlAdmin.denyRole(everyoneRole, path, AccessControlConstants.AUTHORIZE);

        } catch (UserStoreException e) {
            String msg = "Could not set authorizations for the " + path + ".";
            log.error(msg, e);
            throw new RegistryException(msg);
        }
    }

    public static void denyAnonAuthorization(String path, UserRealm userRealm)
            throws RegistryException {
        if (userRealm == null) {
            return;
        }

        try {
            AuthorizationManager accessControlAdmin = userRealm.getAuthorizationManager();
            RealmConfiguration realmConfig;
            try {
                realmConfig = userRealm.getRealmConfiguration();
            } catch (UserStoreException e) {
                String msg = "Failed to retrieve realm configuration.";
                log.error(msg, e);
                throw new RegistryException(msg, e);
            }

            String everyoneRole = realmConfig.getEveryOneRoleName();

            accessControlAdmin.denyRole(everyoneRole, path, ActionConstants.GET);
            accessControlAdmin.denyRole(everyoneRole, path, ActionConstants.PUT);
            accessControlAdmin.denyRole(everyoneRole, path, ActionConstants.DELETE);
            accessControlAdmin.denyRole(everyoneRole, path, AccessControlConstants.AUTHORIZE);

        } catch (UserStoreException e) {
            String msg = "Could not clear authorizations for the " + path + ".";
            log.error(msg, e);
            throw new RegistryException(msg);
        }
    }

    /**
     * builds the OMElement from the given inputStream
     *
     * @param inputStream, given input - inputStream
     * @return OMElement
     * @throws Exception, if building OMElement from the inputStream failed.
     */
    public static OMElement buildOMElement(InputStream inputStream) throws Exception {
        XMLStreamReader parser;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            String msg = "Error in initializing the parser to build the OMElement.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        // create the builder
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        // get the root element (in this case the envelope)

        return builder.getDocumentElement();
    }


    /**
     * validates the email
     *
     * @param email - email address
     * @throws Exception, if validation failed
     */
    public static void validateEmail(String email) throws Exception {
        if (email == null) {
            String msg = "Provided email value is null.";
            log.warn(msg);
            throw new Exception(msg);
        }
        email = email.trim();
        if ("".equals(email)) {
            String msg = "Provided email value is empty.";
            log.warn(msg);
            throw new Exception(msg);
        }
        if (illegalCharactersPatternForEmail.matcher(email).matches()) {
            String msg = "Wrong characters in the email.";
            log.error(msg);
            throw new Exception(msg);
        }
        if (!emailFilterPattern.matcher(email).matches()) {
            String msg = "Invalid email address is provided.";
            log.error(msg);
            throw new Exception(msg);
        }
    }

    /**
     * @param name     validate the name.
     * @param variable entry name.
     * @throws Exception if empty
     */
    public static void validateName(String name, String variable) throws Exception {
        if (name.trim().equals("")) {
            String msg = variable + " is not provided.";
            log.error(msg);
            throw new Exception(msg);
        }
    }

    /**
     * validates domain from the successkey
     *
     * @param governanceSystemRegistry - The governance system registry
     * @param domain                   - tenant domain
     * @param successKey               - successkey
     * @return true, if successfully validated
     * @throws RegistryException, if validation failed
     */
    public static boolean validateDomainFromSuccessKey(UserRegistry governanceSystemRegistry,
                                                       String domain, String successKey)
            throws RegistryException {
        String domainValidatorInfoPath =
                StratosConstants.DOMAIN_VALIDATOR_INFO_PATH + RegistryConstants.PATH_SEPARATOR +
                domain + RegistryConstants.PATH_SEPARATOR +
                StratosConstants.VALIDATION_KEY_RESOURCE_NAME;
        if (governanceSystemRegistry.resourceExists(domainValidatorInfoPath)) {
            Resource resource = governanceSystemRegistry.get(domainValidatorInfoPath);
            String actualSuccessKey = resource.getProperty("successKey");
            if (actualSuccessKey != null && successKey != null &&
                actualSuccessKey.trim().equals(successKey.trim())) {
                // the domain is correct
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the configurations from the stratos configuration file.
     *
     * @return stratos configurations
     */
    public static StratosConfiguration loadStratosConfiguration() {
        // gets the configuration file name from the StratosConstants.
        String StratosConfigFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator + 
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator +
                StratosConstants.STRATOS_CONF_FILE;
        return loadStratosConfiguration(StratosConfigFileName);
    }

    /**
     * Loads the given Stratos Configuration file.
     *
     * @param configFilename Name of the configuration file
     * @return the stratos configuration data.
     */
    private static StratosConfiguration loadStratosConfiguration(String configFilename) {
        StratosConfiguration config = new StratosConfiguration();
        File configFile = new File(configFilename);
        if (configFile.exists()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(configFile);
                XMLStreamReader parser =
                        XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement documentElement = builder.getDocumentElement();
                Iterator it = documentElement.getChildElements();
                while (it.hasNext()) {
                    OMElement element = (OMElement) it.next();

                    if ("DisableTenantManagementEmails".equals(element.getLocalName())) {
                        String disableEmails = element.getText();
                        // by default, make the email validation mandatory.
                        boolean isEmailsDisabled = true;
                        if (disableEmails.trim().equalsIgnoreCase("false")) {
                            isEmailsDisabled = false;
                        }
                        config.setEmailsDisabled(isEmailsDisabled);
                    }
                    // Checks whether Email Validation is mandatory to log in and use the registered
                    // tenants.
                    else if ("EmailValidationMandatoryForLogin".equals(element.getLocalName())) {
                        String emailValidation = element.getText();
                        //by default, make the email validation not mandatory.
                        boolean isEmailValidationRequired = false;
                        if (emailValidation.trim().equalsIgnoreCase("true")) {
                            isEmailValidationRequired = true;
                        }
                        config.setEmailValidationRequired(isEmailValidationRequired);
                    } else if ("ChargeOnRegistration".equals(element.getLocalName())) {
                        String chargeOnRegistration = element.getText();
                        boolean isChargedOnRegistration = false;
                        if (chargeOnRegistration.trim().equalsIgnoreCase("true")) {
                            isChargedOnRegistration = true;
                        }
                        config.setChargeOnRegistration(isChargedOnRegistration);
                    } else if ("NotificationEmail".equals(element.getLocalName())) {
                        config.setNotificationEmail(element.getText());
                    } else if ("SuperAdminEmail".equals(element.getLocalName())) {
                        config.setSuperAdminEmail(element.getText());
                    } else if ("TenantActivationModerated".equals(element.getLocalName())){
                        String isTenantActivationModerated = element.getText();
                        boolean tenantActivationModerated = false;
                        if (isTenantActivationModerated.trim().equalsIgnoreCase("true")) {
                            tenantActivationModerated = true;
                        }
                        config.setTenantActivationModerated(tenantActivationModerated);
                    }
                    //Checks whether it is public cloud deployment.
                    else if ("StratosPublicCloudSetup".equals(element.getLocalName())) {
                        String cloudSetup = element.getText();
                        //by default, make the email validation mandatory.
                        boolean isStratosPublicCloudSetup = true;
                        if (cloudSetup.trim().equalsIgnoreCase("false")) {
                            isStratosPublicCloudSetup = false;
                        }
                        config.setPublicCloudSetup(isStratosPublicCloudSetup);
                        //Setting the paypal url
                    } else if ("PaypalUrl".equals(element.getLocalName())) {
                        String paypalUrl = element.getText();
                        config.setPaypalUrl(paypalUrl);
                    } else if ("SkipSummaryGenerator".equals(element.getLocalName())) {
                        String summaryGenerator = element.getText();
                        boolean skipSummaryGenerator = false;
                        if (summaryGenerator.trim().equalsIgnoreCase("true")) {
                            skipSummaryGenerator = true;
                        }
                        config.setSkipSummaryGenerator(skipSummaryGenerator);
                    }
                    else if ("PaypalAPIUsername".equals(element.getLocalName())) {
                        config.setPaypalAPIUsername(element.getText());
                    } else if ("PaypalAPIPassword".equals(element.getLocalName())) {
                        config.setPaypalAPIPassword(element.getText());
                    } else if ("PaypalAPISignature".equals(element.getLocalName())) {
                        config.setPaypalAPISignature(element.getText());
                    }else if ("PaypalEnvironment".equals(element.getLocalName())){
                        config.setPaypalEnvironment(element.getText());
                    }else if("FinanceNotificationEmail".equals(element.getLocalName())){
                        config.setFinanceNotificationEmail(element.getText());    
                    }else if("UsagePlanUrl".equals(element.getLocalName())){
                        config.setUsagePlanURL(element.getText());
                    }else if("PaidJIRAUrl".equals(element.getLocalName())) {
                        config.setPaidJIRAUrl(element.getText());
                    }else if("PaidJIRAProject".equals(element.getLocalName())) {
                        config.setPaidJIRAProject(element.getText());
                    }else if("ForumUrl".equals(element.getLocalName())) {
                        config.setForumUrl(element.getText());
                    }else if("PaidUserGroup".equals(element.getLocalName())) {
                        config.setPaidUserGroup(element.getText());
                    }else if("NonpaidUserGroup".equals(element.getLocalName())) {
                        config.setNonpaidUserGroup(element.getText());
                    } else if("SupportInfoUrl".equals(element.getLocalName())) {
                        config.setSupportInfoUrl(element.getText());
                    }else if("IncidentCustomFieldId".equals(element.getLocalName())) {
                        config.setIncidentCustomFieldId(element.getText());
                    } else if("IncidentImpactCustomFieldId".equals(element.getLocalName())) {
                        config.setIncidentImpactCustomFieldId(element.getText());
                    } else if ("GoogleAnalyticsURL".equals(element.getLocalName())) {
                        config.setGoogleAnalyticsURL(element.getText());
                    } else if("StratosEventListener".equals(element.getLocalName())) {
                        populateEventListenerProperties(config, element);
                    } else if ("managerServiceUrl".equals(element.getLocalName())) {
                        config.setManagerServiceUrl(element.getText());
                    } else if ("adminUserName".equals(element.getLocalName())) {
                        config.setAdminUserName(element.getText());
                    } else if ("adminPassword".equals(element.getLocalName())) {
                        config.setAdminPassword(element.getText());
                    } else if("SSOLoadingMessage".equals(element.getLocalName())){
                        config.setSsoLoadingMessage(element.getText());
                    }
                }
                return config;
            } catch (Exception e) {
                String msg = "Error in loading Stratos Configurations File: " + configFilename + ".";
                log.error(msg, e);
                return config; //returns the default configurations, if the file could not be loaded.
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Could not close the Configuration File " + configFilename);
                    }
                }
            }
        }
        log.error("Unable to locate the stratos configurations file. " +
                  "Default Settings will be used.");
        return config; // return the default configuratiosn, if the file not found.
    }
    
    
    private static void populateEventListenerProperties(StratosConfiguration config,
                                                        OMElement element) throws RegistryException {
        config.setStratosEventListenerName(element.getAttributeValue(new QName("class")));
        Iterator<?> ite =
                          element.getChildrenWithName(new QName("Property"));
        while (ite.hasNext()) {
            OMElement propElem = (OMElement) ite.next();
            String propName = propElem.getAttributeValue(new QName("name"));
            String propValue = propElem.getText();
            config.setStratosEventListenerProperty(propName, propValue);
        }
    }

    /**
     * Loading the EULA.
     * ultimately we shall be loading the eula from the web page itself.
     * But loading from file should be there to customize the EULA based on the private deployments,
     * etc.
     *
     * @return eula
     */
    public static String loadTermsOfUsage() {
        // currently loads from the file; gets the eula file name from the StratosConstants.
        String StratosEULAFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator +
                StratosConstants.STRATOS_EULA;
        return loadTermsOfUsage(StratosEULAFileName);
    }

    private static String loadTermsOfUsage(String eulaFile) {
        String eula = StratosConstants.STRATOS_EULA_DEFAULT_TEXT;
        File configFile = new File(eulaFile);
        if (configFile.exists()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(configFile);
                XMLStreamReader parser =
                        XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement documentElement = builder.getDocumentElement();
                Iterator it = documentElement.getChildElements();
                while (it.hasNext()) {
                    OMElement element = (OMElement) it.next();

                    //Checks whether Email Validation is mandatory for tenant registration complete.
                    if ("EULA".equalsIgnoreCase(element.getLocalName())) {
                        eula = element.getText();
                    }
                }
                return eula;
            } catch (Exception e) {
                String msg = "Error in loading Stratos Terms and Conditions File.";
                log.error(msg, e);
                return eula; //returns the default text, if the file could not be loaded.
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Could not close the EULA File " + eulaFile);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Unable to locate the stratos EULA file. Default value will be used.");
        }
        return eula; // return the default, if the file not found.
    }


    /**
     * method to check whether a domain name is available to register given a domain name
     * @param tenantDomain, domain name
     * @return true, if the domain is available to register
     * @throws Exception, if checking the existence of the tenant is failed.
     */
    public static boolean isDomainNameAvailable(String tenantDomain) throws Exception {

        TenantManager tenantManager = CloudCommonServiceComponent.getTenantManager();
          // The registry reserved words are checked first.
          if (tenantDomain.equals("atom") || tenantDomain.equals("registry")
                  || tenantDomain.equals("resource")) {
              String msg = "You can not use a registry reserved word:" + tenantDomain +
                           ":as a tenant domain. Please choose a different one.";
              log.error(msg);
              throw new Exception(msg);
          }

          int tenantId;
          try {
              tenantId = tenantManager.getTenantId(tenantDomain);
          } catch (UserStoreException e) {
              String msg = "Error in getting the tenant id for the given domain  " +
                           tenantDomain + ".";
              log.error(msg);
              throw new Exception(msg, e);
          }

          // check a tenant with same domain exist.
          if ((tenantId != MultitenantConstants.INVALID_TENANT_ID && tenantId != MultitenantConstants.SUPER_TENANT_ID) ||
                  tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
              String msg =
                           "A tenant with same domain already exist. " +
                                   "Please use a different domain name. tenant domain: " +
                                   tenantDomain + ".";
              log.info(msg);
              return false;
          }
          return true;
      }
}



