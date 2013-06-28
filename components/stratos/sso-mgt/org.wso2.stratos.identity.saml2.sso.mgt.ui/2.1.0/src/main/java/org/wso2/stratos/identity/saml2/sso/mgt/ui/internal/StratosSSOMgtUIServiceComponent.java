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
package org.wso2.stratos.identity.saml2.sso.mgt.ui.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.stratos.identity.saml2.sso.mgt.ui.SSOLoginPageFilter;
import org.wso2.stratos.identity.saml2.sso.mgt.ui.SSORedirectPageFilter;
import org.wso2.stratos.identity.saml2.sso.mgt.ui.Util;

/**
 * @scr.component name="org.wso2.stratos.identity.saml2.sso.mgt.ui"
 * immediate="true"
 * @scr.reference name="identity.sso.sp.config.manager"
 * interface="org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager"
 * cardinality="1..1" policy="dynamic" bind="setSSOServiceProviderConfigManager" unbind="unsetSSOServiceProviderConfigManager"
 */

public class StratosSSOMgtUIServiceComponent {

    private static Log log = LogFactory.getLog(StratosSSOMgtUIServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        // register a servlet filter for SSO login page
        HttpServlet loginPageRedirectorServlet = new HttpServlet() {
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
            }
        };

        Filter ssoLoginPageFilter = new SSOLoginPageFilter();
        Dictionary loginPageFilterAttrs = new Hashtable(2);
        Dictionary loginPageFilterParams = new Hashtable(2);
        loginPageFilterParams.put("url-pattern", "/carbon/sso-saml/login.jsp");
        loginPageFilterParams.put("associated-filter", ssoLoginPageFilter);
        loginPageFilterParams.put("servlet-attributes", loginPageFilterAttrs);
        ctxt.getBundleContext().registerService(Servlet.class.getName(),
                                                loginPageRedirectorServlet, loginPageFilterParams);

        HttpServlet redirectJSPRedirectorServlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
            }
        };

        // register a servlet filter for SSO redirect page
        Filter redirectPageFilter = new SSORedirectPageFilter();
        Dictionary redirectorPageFilterAttrs = new Hashtable(2);
        Dictionary redirectorPageFilterParams = new Hashtable(2);
        redirectorPageFilterParams.put("url-pattern", "/carbon/sso-saml/redirect_ajaxprocessor.jsp");
        redirectorPageFilterParams.put("associated-filter", redirectPageFilter);
        redirectorPageFilterParams.put("servlet-attributes", redirectorPageFilterAttrs);
        ctxt.getBundleContext().registerService(Servlet.class.getName(), redirectJSPRedirectorServlet,
                                                redirectorPageFilterParams);

        // Read the config parameters from sso-idp-config.xml
        Util.populateLoginPageConfigParams();
    }

    protected void deactivate(ComponentContext ctxt) {
        log.debug("SAML2 SSO Authenticator FE Bundle is deactivated ");
    }

    protected void setSSOServiceProviderConfigManager(
            SSOServiceProviderConfigManager configManager) {
        Util.setSsoServiceProviderConfigManager(configManager);
        if (log.isDebugEnabled()) {
            log.debug("SSOServiceProviderConfigManager is set for Stratos SAML2 SSO Management Service Component.");
        }
    }

    protected void unsetSSOServiceProviderConfigManager(
            SSOServiceProviderConfigManager configManager) {
        Util.setSsoServiceProviderConfigManager(null);
        if (log.isDebugEnabled()) {
            log.debug("SSOServiceProviderConfigManager is unset for Stratos SAML2 SSO Management Service Component.");
        }
    }
}
