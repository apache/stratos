/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.stratos.common.util;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for the stratos specific parameters configuration
 */
public class StratosConfiguration {

    // By default email sending is disabled. Changed according to the configuration file.
    private boolean emailsDisabled = true;

    private boolean emailValidationRequired = false;
    private boolean isPublicCloudSetup = true;
    private boolean isTenantActivationModerated = false;
    private boolean chargeOnRegistration = false;
    private String SuperAdminEmail = "";
    private String paypalUrl = "";
    private String paypalAPIUsername = "";
    private String paypalAPIPassword = "";
    private String paypalAPISignature = "";
    private String paypalEnvironment="";
    private String usagePlanURL = "";
    private String paidJIRAUrl = "";
    private String paidJIRAProject = "";
    private String forumUrl = "";
    private String paidUserGroup = "";
    private String nonpaidUserGroup = "";
    private String supportInfoUrl = "";
    private String incidentCustomFieldId = ""; // todo this is a custom field id of JIRA, this need to be dynamically get in jira reporting component
    private String incidentImpactCustomFieldId = "";
    private String stratosEventListenerName ="";
    private Map<String, String> stratosEventProperties = new HashMap<String, String>();
    private String googleAnalyticsURL;
    private String managerServiceUrl = "";
    private String adminUserName = "";
    private String adminPassword = "";
    private String ssoLoadingMessage="";

    /**
     * @return Stratos Manager service url
     */
    public String getManagerServiceUrl() {
        return managerServiceUrl;
    }

    public void setManagerServiceUrl(String managerServiceUrl) {
        this.managerServiceUrl = managerServiceUrl;
    }

    /**
     * @return Super admin User name
     */
    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    /**
     * @return super admin password
     */
    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    //This parameter used to skip usage summary generator
     private boolean skipSummaryGenerator = false;

    public boolean isSkipSummaryGenerator() {
        return skipSummaryGenerator;
    }

    public void setSkipSummaryGenerator(boolean skipSummaryGenerator) {
        this.skipSummaryGenerator = skipSummaryGenerator;
    }
    //This is the url that we pointed users when they need to aware about usage plans

    public String getUsagePlanURL() {
        return usagePlanURL;
    }

    public void setUsagePlanURL(String usagePlanURL) {
        this.usagePlanURL = usagePlanURL;
    }

    //Email address for general notifications
    private String notificationEmail = "";
    //Email address for finance related notifications
    private String financeNotificationEmail = "";

    public boolean isTenantActivationModerated() {
        return isTenantActivationModerated;
    }

    public void setTenantActivationModerated(boolean tenantActivationModerated) {
        isTenantActivationModerated = tenantActivationModerated;
    }

    public String getSuperAdminEmail() {
        return SuperAdminEmail;
    }

    public void setSuperAdminEmail(String superAdminEmail) {
        SuperAdminEmail = superAdminEmail;
    }

    public boolean getEmailValidationRequired() {
        return emailValidationRequired;
    }

    public String getPaypalUrl() {
        return paypalUrl;
    }

    public void setPaypalUrl(String paypalUrl) {
        this.paypalUrl = paypalUrl;
    }

    public void setEmailValidationRequired(boolean emailValidationRequired) {
        this.emailValidationRequired = emailValidationRequired;
    }

    public boolean isPublicCloudSetup() {
        return isPublicCloudSetup;
    }

    public void setPublicCloudSetup(boolean publicCloudSetup) {
        isPublicCloudSetup = publicCloudSetup;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public String getPaypalAPIUsername() {
        return paypalAPIUsername;
    }

    public void setPaypalAPIUsername(String paypalAPIUsername) {
        this.paypalAPIUsername = paypalAPIUsername;
    }

    public String getPaypalAPIPassword() {
        return paypalAPIPassword;
    }

    public void setPaypalAPIPassword(String paypalAPIPassword) {
        this.paypalAPIPassword = paypalAPIPassword;
    }

    public String getPaypalAPISignature() {
        return paypalAPISignature;
    }

    public void setPaypalAPISignature(String paypalAPISignature) {
        this.paypalAPISignature = paypalAPISignature;
    }

    public String getFinanceNotificationEmail() {
        return financeNotificationEmail;
    }

    public void setFinanceNotificationEmail(String financeNotificationEmail) {
        this.financeNotificationEmail = financeNotificationEmail;
    }

    public String getPaidJIRAUrl() {
        return paidJIRAUrl;
    }

    public void setPaidJIRAUrl(String paidJIRAUrl) {
        this.paidJIRAUrl = paidJIRAUrl;
    }

    public String getPaidJIRAProject() {
        return paidJIRAProject;
    }

    public void setPaidJIRAProject(String paidJIRAProject) {
        this.paidJIRAProject = paidJIRAProject;
    }

    public String getForumUrl() {
        return forumUrl;
    }

    public void setForumUrl(String forumUrl) {
        this.forumUrl = forumUrl;
    }

    public String getPaidUserGroup() {
        return paidUserGroup;
    }

    public void setPaidUserGroup(String paidUserGroup) {
        this.paidUserGroup = paidUserGroup;
    }

    public String getNonpaidUserGroup() {
        return nonpaidUserGroup;
    }

    public void setNonpaidUserGroup(String nonpaidUserGroup) {
        this.nonpaidUserGroup = nonpaidUserGroup;
    }

    public String getSupportInfoUrl() {
        return supportInfoUrl;
    }

    public void setSupportInfoUrl(String supportInfoUrl) {
        this.supportInfoUrl = supportInfoUrl;
    }

    public String getIncidentCustomFieldId() {
        return incidentCustomFieldId;
    }

    public void setIncidentCustomFieldId(String incidentCustomFieldId) {
        this.incidentCustomFieldId = incidentCustomFieldId;
    }

    public String getIncidentImpactCustomFieldId() {
        return incidentImpactCustomFieldId;
    }

    public void setIncidentImpactCustomFieldId(String incidentImpactCustomFieldId) {
        this.incidentImpactCustomFieldId = incidentImpactCustomFieldId;
    }
    
    public String getStratosEventListenerName() {
        return stratosEventListenerName;
    }

    public void setStratosEventListenerName(String stratosEventListenerName) {
        this.stratosEventListenerName = stratosEventListenerName;
    }

    public String getStratosEventListenerPropertyValue(String key) {
        return stratosEventProperties.get(key);
    }

    public void setStratosEventListenerProperty(String key, String value) {
        stratosEventProperties.put(key, value);
    }

    public String getPaypalEnvironment() {
        return paypalEnvironment;
    }

    public void setPaypalEnvironment(String paypalEnvironment) {
        this.paypalEnvironment = paypalEnvironment;
    }

    public String getGoogleAnalyticsURL() {
        return googleAnalyticsURL;
    }

    public void setGoogleAnalyticsURL(String googleAnalyticsURL) {
        this.googleAnalyticsURL = googleAnalyticsURL;
    }

    public boolean isEmailsDisabled() {
        return emailsDisabled;
    }

    public void setEmailsDisabled(boolean emailsDisabled) {
        this.emailsDisabled = emailsDisabled;
    }

    public boolean isChargeOnRegistration() {
        return chargeOnRegistration;
    }

    public void setChargeOnRegistration(boolean chargeOnRegistration) {
        this.chargeOnRegistration = chargeOnRegistration;
    }

    public String getSsoLoadingMessage() {
        return ssoLoadingMessage;
    }

    public void setSsoLoadingMessage(String ssoLoadingMessage) {
        this.ssoLoadingMessage = ssoLoadingMessage;
    }
    
}

