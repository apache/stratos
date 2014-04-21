/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.ui;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.authenticator.proxy.AuthenticationAdminClient;
import org.wso2.carbon.authenticator.stub.RememberMeData;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ServerConstants;

/**
 * Default implementation of CarbonUIAuthenticator.
 */
public class DefaultCarbonAuthenticator extends BasicAuthUIAuthenticator {

    protected static final Log log = LogFactory.getLog(DefaultCarbonAuthenticator.class);

    private static final String AUTHENTICATOR_NAME = "DefaultCarbonAuthenticator";

    /**
     * {@inheritDoc}
     */
    public boolean canHandle(HttpServletRequest request) {
        // try to authenticate any request that comes
        // least priority authenticator
        String userName = request.getParameter(AbstractCarbonUIAuthenticator.USERNAME);
        String password = request.getParameter(AbstractCarbonUIAuthenticator.PASSWORD);

        if (!CarbonUtils.isRunningOnLocalTransportMode()) {
            return false;
        }

        if (userName != null && password != null) {
            return true;
        }

        // This is to login with Remember Me.
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String doAuthentication(Object credentials, boolean isRememberMe, ServiceClient client,
            HttpServletRequest request) throws AuthenticationException {

        DefaultAuthenticatorCredentials defaultCredentials = (DefaultAuthenticatorCredentials) credentials;
        // call AuthenticationAdmin, since BasicAuth are not validated for LocalTransport
        AuthenticationAdminClient authClient;
        try {
            authClient = getAuthenticationAdminCient(request);
            boolean isAutenticated = false;
            if (isRememberMe && defaultCredentials.getUserName() != null
                    && defaultCredentials.getPassword() != null) {
                RememberMeData rememberMe;
                rememberMe = authClient.loginWithRememberMeOption(defaultCredentials.getUserName(),
                        defaultCredentials.getPassword(), "127.0.0.1");
                isAutenticated = rememberMe.getAuthenticated();
                if (isAutenticated) {
                    request.setAttribute(CarbonConstants.REMEMBER_ME_COOKIE_VALUE,
                            rememberMe.getValue());
                    request.setAttribute(CarbonConstants.REMEMBER_ME_COOKIE_AGE, new Integer(
                            rememberMe.getMaxAge()).toString());
                    return defaultCredentials.getUserName();
                }
            } else if (isRememberMe) {
                // This is to login with Remember Me.
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)) {
                            isAutenticated = authClient
                                    .loginWithRememberMeCookie(cookie.getValue());
                            if (isAutenticated) {
                                String cookieValue = cookie.getValue();
                                return getUserNameFromCookie(cookieValue);
                            }
                        }
                    }
                }
            } else {
                isAutenticated = authClient.login(defaultCredentials.getUserName(),
                        defaultCredentials.getPassword(), "127.0.0.1");
                if (isAutenticated) {
                    return defaultCredentials.getUserName();
                }
            }

            throw new AuthenticationException("Invalid user credentials.");

        } catch (AxisFault e) {
            throw new AuthenticationException(e.getMessage(), e);
        }

    }

    /**
     *
     */
    public void unauthenticate(Object object) throws Exception {
        try {
            getAuthenticationAdminCient(((HttpServletRequest) object)).logout();
        } catch (Exception ignored) {
            String msg = "Configuration context is null.";
            log.error(msg);
            throw new Exception(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

    /**
     * 
     * @param request
     * @return
     * @throws AxisFault
     */
    private AuthenticationAdminClient getAuthenticationAdminCient(HttpServletRequest request)
            throws AxisFault {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String backendServerURL = request.getParameter("backendURL");
        if (backendServerURL == null) {
            backendServerURL = CarbonUIUtil.getServerURL(servletContext, request.getSession());
        }
        session.setAttribute(CarbonConstants.SERVER_URL, backendServerURL);

        ConfigurationContext configContext = (ConfigurationContext) servletContext
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN);

        return new AuthenticationAdminClient(configContext, backendServerURL, cookie, session, true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void handleRememberMe(Map transportHeaders, HttpServletRequest httpServletRequest)
            throws AuthenticationException {
        // Do nothing here. Already done.

    }

}
