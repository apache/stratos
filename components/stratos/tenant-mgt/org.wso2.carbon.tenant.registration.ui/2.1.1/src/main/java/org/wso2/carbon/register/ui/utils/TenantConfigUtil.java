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
package org.wso2.carbon.register.ui.utils;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.Base64;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.register.ui.clients.TenantSelfRegistrationClient;
import org.wso2.carbon.registry.common.ui.UIException;
import org.wso2.carbon.tenant.register.stub.beans.xsd.CaptchaInfoBean;
import org.wso2.carbon.tenant.register.stub.beans.xsd.TenantInfoBean;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Utility methods for tenant configuration
 */
public class TenantConfigUtil {
    private static final Log log = LogFactory.getLog(TenantConfigUtil.class);

    /**
     * Registers the tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return String
     * @throws UIException
     */
    public static String registerTenantConfigBean(HttpServletRequest request,
                                                  ServletConfig config, HttpSession session) throws UIException {
        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        CaptchaInfoBean captchaInfoBean = new CaptchaInfoBean();

        try {
            // filling tenant info.
            tenantInfoBean.setFirstname(request.getParameter("admin-firstname"));
            tenantInfoBean.setLastname(request.getParameter("admin-lastname"));
            tenantInfoBean.setAdmin(request.getParameter("admin"));
            tenantInfoBean.setAdminPassword(request.getParameter("admin-password"));
            tenantInfoBean.setTenantDomain(resolveDomainName(request.getParameter("domain")));
            tenantInfoBean.setEmail(request.getParameter("admin-email"));
            tenantInfoBean.setSuccessKey((String) session.getAttribute("validate-domain-success-key"));
            tenantInfoBean.setUsagePlan(request.getParameter("selectedUsagePlan"));
            tenantInfoBean.setOriginatedService(TenantConfigUtil.base64Decode((String) session.getAttribute(
                    StratosConstants.ORIGINATED_SERVICE)));
            tenantInfoBean.setCreatedDate(Calendar.getInstance());
            // filling captcha info
            captchaInfoBean.setSecretKey(request.getParameter("captcha-secret-key"));
            captchaInfoBean.setUserAnswer(request.getParameter("captcha-user-answer"));

            TenantSelfRegistrationClient selfRegistrationClient =
                    new TenantSelfRegistrationClient(config, session);

            String returnText = selfRegistrationClient.registerTenant(tenantInfoBean, captchaInfoBean);

            return returnText;

        } catch (Exception e) {
            AxisFault fault = new AxisFault(e.getMessage());
            String msg = fault.getReason() + " Failed to add tenant config. tenant-domain: " +
                    tenantInfoBean.getTenantDomain() + ", " +
                    "tenant-admin: " + tenantInfoBean.getAdmin() + ".";
            log.error(msg, e);
            // we are preserving the original message.
            throw new UIException(e.getMessage(), e);
        }
    }


    // do this before the send redirect.

    public static void setSubmissionValuesForSession(HttpServletRequest request) {
        HttpSession session = request.getSession();

        session.setAttribute("submit-domain", resolveDomainName(request.getParameter("domain")));
        session.setAttribute("submit-admin", request.getParameter("admin"));
        session.setAttribute("submit-admin-firstname", request.getParameter("admin-firstname"));
        session.setAttribute("submit-admin-lastname", request.getParameter("admin-lastname"));
        session.setAttribute("submit-admin-email", request.getParameter("admin-email"));
    }


    /**
     * Checks the availability of the domain
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return true, if domain is available to register
     * @throws UIException if failed to check the availability.
     */
    public static boolean checkDomainAvailability(
            HttpServletRequest request, ServletConfig config, HttpSession session)
            throws UIException {
        String tenantDomain = null;
        try {
            tenantDomain = resolveDomainName(request.getParameter("domain"));
            TenantSelfRegistrationClient selfRegistrationClient =
                    new TenantSelfRegistrationClient(config, session);
            return selfRegistrationClient.checkDomainAvailability(tenantDomain);
        } catch (Exception e) {
            String msg = "Failed to check the domain availability:" + tenantDomain + ".";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    /**
     * Validates or suggests the domain
     *
     * @param config  ServletConfig
     * @param session HttpSession
     * @return domain
     * @throws UIException, if validating or suggesting the domain failed.
     */
    public static String validateOrSuggestDomain(ServletConfig config,
                                                 HttpSession session) throws UIException {
        String tempDomainToRegister = resolveDomainName(
                (String) session.getAttribute("temp-domain-to-register"));
        // here successKey can be null, in such cases services will directly go to suggest a name
        String successKey = (String) session.getAttribute("validate-domain-success-key");

        try {
            TenantSelfRegistrationClient selfRegistrationClient =
                    new TenantSelfRegistrationClient(config, session);
            return selfRegistrationClient.validateOrSuggestDomain(tempDomainToRegister, successKey);
        } catch (Exception e) {
            String msg = "Failed to validate or suggest a domain related to :" +
                    tempDomainToRegister + ".";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    /**
     * Generates a random captcha
     *
     * @param config  ServletConfig
     * @param session HttpSession
     * @return CaptchaInfoBean
     * @throws UIException, if generating the random captcha fails.
     */
    public static CaptchaInfoBean generateRandomCaptcha(ServletConfig config,
                                                        HttpSession session) throws UIException {
        try {
            TenantSelfRegistrationClient selfRegistrationClient =
                    new TenantSelfRegistrationClient(config, session);
            return selfRegistrationClient.generateRandomCaptcha();
        } catch (Exception e) {
            String msg = "Error in generating the captcha image.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static HttpServletRequest readIntermediateData(HttpServletRequest request, String data) {
        try {
            XMLStreamReader parser =
                    XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(data));
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement = builder.getDocumentElement();
            Iterator it = documentElement.getChildElements();
            while (it.hasNext()) {
                OMElement element = (OMElement) it.next();
                if ("admin".equals(element.getLocalName())) {
                    request.setAttribute("admin", element.getText());
                } else if ("firstname".equals(element.getText())) {
                    request.setAttribute("firstname", element.getText());
                } else if ("lastname".equals(element.getText())) {
                    request.setAttribute("lastname", element.getText());
                } else if ("email".equals(element.getLocalName())) {
                    request.setAttribute("email", element.getText());
                } else if ("tenantDomain".equals(element.getLocalName())) {
                    request.setAttribute("tenantDomain", element.getText());
                } else if ("confirmationKey".equals(element.getLocalName())) {
                    request.setAttribute("confirmationKey", element.getText());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing xml", e);
        }
        return request;
    }

    /**
     * Resolves the correct domain name in the form of example.com from the user input domain name.
     * Currently strips out "www."and white space. Can add more checks.
     *
     * @param domainNameUserInput the user input domain name
     * @return the domain after removing (if entered) www. from the input.
     */
    public static String resolveDomainName(String domainNameUserInput) {
        if (domainNameUserInput == null) {
            String msg = "Provided domain name is null";
            log.error(msg);
            return "";
        }
        String domainName = domainNameUserInput.trim();
        if (domainName.startsWith("www.")) {
            domainName = domainName.substring(4);
        }
        return domainName;
    }

    /**
     * A basic method to decode the encoded Stratos Service Name
     *
     * @param encodedStr Encoded Stratos Service Name
     * @return Decoded Stratos Service name
     */
    private static String base64Decode(String encodedStr) {
        String decodedStr = null;
        // Check whether this value is null(not set) or set to "null" which is also possible.
        if (encodedStr != null && !"null".equals(encodedStr)) {
            decodedStr = new String(Base64.decode(encodedStr));
        }
        return decodedStr;
    }

}
