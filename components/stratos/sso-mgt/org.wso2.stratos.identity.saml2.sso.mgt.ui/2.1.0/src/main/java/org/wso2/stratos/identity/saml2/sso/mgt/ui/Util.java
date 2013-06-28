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

package org.wso2.stratos.identity.saml2.sso.mgt.ui;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Enumeration;

public class Util {
    // constants used to read the config parameters from sso-idp-config.xml
    private static final String TENANT_REGISTRATION_PAGE = "TenantRegistrationPage";
    private static final String BANNER_ADDS_BASE_URL = "LoginPageBannerBaseURL";

    private static String loginPageBannerBaseURL;
    private static String tenantRegistrationPageURL;

    private static SSOServiceProviderConfigManager ssoServiceProviderConfigManager;

    private static Log log = LogFactory.getLog(Util.class);

    public static void setSsoServiceProviderConfigManager(
            SSOServiceProviderConfigManager ssoServiceProviderConfigManager) {
        Util.ssoServiceProviderConfigManager = ssoServiceProviderConfigManager;
    }

    /**
     * Get the Corresponding Stratos Service Name, used in * as a Service impl.
     *
     * @param requestPath request path for the custom login page JSP
     * @return Stratos Service name
     */
    public static String getStratosServiceName(String requestPath) {
        String context = calculateWebContextFromContextPath(requestPath);
        String stratosServiceName = null;
        Enumeration<SAMLSSOServiceProviderDO> serviceProviders =
                Util.ssoServiceProviderConfigManager.getAllServiceProviders();
        while (serviceProviders.hasMoreElements()) {
            SAMLSSOServiceProviderDO serviceProvider = serviceProviders.nextElement();
            if (context.equals(serviceProvider.getLoginPageURL())) {
                stratosServiceName = serviceProvider.getIssuer();
                break;
            }
        }
        return base64Encode(stratosServiceName);
    }

    /**
     * Read the SP info from the sso-idp-config.xml and create an array of SAMLSSOServiceProviderDO
     * beans
     *
     * @return An array of SAMLSSOServiceProviderDO beans
     */
    public static void populateLoginPageConfigParams() {
        Document document = null;
        String configFilePath = null;
        try {
            configFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator + "sso-idp-config.xml";
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(configFilePath);
        } catch (Exception e) {
            log.error("Error reading Service Providers from sso-idp-config.xml", e);
        }

        // read the tenant registration page URL
        Element element = document.getDocumentElement();
        NodeList tenantRegNodeSet = element.getElementsByTagName(TENANT_REGISTRATION_PAGE);
        if (tenantRegNodeSet.getLength() > 0) {
            Element elem = (Element) tenantRegNodeSet.item(0);
            tenantRegistrationPageURL = elem.getFirstChild().getNodeValue();
        } else {
            log.error("The configuration element '" + TENANT_REGISTRATION_PAGE + "' not found in '" +
                    configFilePath + "'");
        }

        // read the base URL of the banner adds
        NodeList bannerAddBaseURLNodeSet = element.getElementsByTagName(BANNER_ADDS_BASE_URL);
        if (bannerAddBaseURLNodeSet.getLength() > 0) {
            Element elem = (Element) bannerAddBaseURLNodeSet.item(0);
            loginPageBannerBaseURL = elem.getFirstChild().getNodeValue();
        }
    }

    /**
     * Return the tenant registration page URL included in <TenantRegistrationPage>
     *
     * @return tenant registration page URL
     */
    public static String getTenantRegistrationPageURL() {
        return tenantRegistrationPageURL;
    }

    /**
     * Return the base URL for banners displayed in the login page as included in <LoginPageBannerBaseURL>
     *
     * @return base URL for banners
     */
    public static String getBannerAddsBaseURL() {
        return loginPageBannerBaseURL;
    }

    /**
     * calculate the CustomLoginPage parameter from the request path
     *
     * @param requestPath request path
     * @return loginPage URL
     */
    private static String calculateWebContextFromContextPath(String requestPath) {
        requestPath = requestPath.replace("//", "/");
        String subStr = requestPath.subSequence(0, requestPath.lastIndexOf("/")).toString();
        String context = requestPath.subSequence(subStr.lastIndexOf("/") + 1,
                                                 requestPath.length()).toString();
        return context.trim();
    }

    private static String base64Encode(String plainTxt) {
        if (plainTxt != null) {
            return Base64.encode(plainTxt.getBytes());
        }
        return null;
    }

    public static String getForgetPasswordLink(){
        String registerPageURL = Util.getTenantRegistrationPageURL();
        return registerPageURL.replace("select_domain.jsp", "../admin-mgt/forgot_password.jsp");
    }

}
