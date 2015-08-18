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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.ExceptionException;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfo;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfoAdminStub;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * An abstract implementation if CarbonUIAuthenticator.
 */
public abstract class AbstractCarbonUIAuthenticator implements CarbonUIAuthenticator {

    private static final int DEFAULT_PRIORITY_LEVEL = 5;

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String REMEMBER_ME = "rememberMe";

    protected static final Log log = LogFactory.getLog(AbstractCarbonUIAuthenticator.class);
    private static Log audit = CarbonConstants.AUDIT_LOG;

    /**
     * In default implementation this will read the authenticator configuration and will return true
     * if authenticator is disabled in the configuration.
     * 
     * @return <code>true</code> if authenticator is disabled, else <code>false</code>.
     */
    public boolean isDisabled() {
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = getAuthenticatorConfig();
        return authenticatorConfig != null && authenticatorConfig.isDisabled();
    }

    /**
     * In default implementation this will read the priority from authenticator configuration.
     * 
     * @return The priority value.
     */
    public int getPriority() {
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getPriority() > 0) {
            return authenticatorConfig.getPriority();
        }

        return DEFAULT_PRIORITY_LEVEL;
    }

    /**
     * In default implementation this will return some SSO links to be skipped. TODO : check whether
     * we can move this t SSO authenticators.
     * 
     * @return A list with following urls.
     */
    public List<String> getSessionValidationSkippingUrls() {
        List<String> skippingUrls = new ArrayList<String>(Arrays.asList("/samlsso",
                "sso-saml/login.jsp", "stratos-sso/login_ajaxprocessor.jsp",
                "sso-saml/redirect_ajaxprocessor.jsp", "stratos-sso/redirect_ajaxprocessor.jsp",
                "sso-acs/redirect_ajaxprocessor.jsp", "stratos-auth/redirect_ajaxprocessor.jsp"));

        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getPriority() > 0) {
            skippingUrls.addAll(authenticatorConfig.getSessionValidationSkippingUrls());
        }

        return skippingUrls;
    }

    /**
     * In default implementation this will return an empty list.
     * 
     * @return An empty list.
     */
    public List<String> getAuthenticationSkippingUrls() {

        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = getAuthenticatorConfig();

        if (authenticatorConfig != null) {
            return authenticatorConfig.getAuthenticationSkippingUrls();
        } else {
            return new ArrayList<String>(0);
        }
    }

    /**
     * 
     * @param credentials
     * @param rememberMe
     * @param client
     * @param request
     * @throws AuthenticationException
     */
    public abstract String doAuthentication(Object credentials, boolean isRememberMe,
            ServiceClient client, HttpServletRequest request) throws AuthenticationException;

    /**
     * 
     * @param serviceClient
     * @param httpServletRequest
     * @throws AxisFault
     */
    @SuppressWarnings("rawtypes")
    public abstract void handleRememberMe(Map transportHeaders,
            HttpServletRequest httpServletRequest) throws AuthenticationException;

    /**
     * 
     */
    protected boolean isAdminCookieSet() {
        return false;
    }
    
    
    /**
     * Regenerates session id after each login attempt.
     * @param request
     */
	private void regenrateSession(HttpServletRequest request) {

		HttpSession oldSession = request.getSession();

		Enumeration attrNames = oldSession.getAttributeNames();
		Properties props = new Properties();

		while (attrNames != null && attrNames.hasMoreElements()) {
			String key = (String) attrNames.nextElement();
			props.put(key, oldSession.getAttribute(key));
		}

		oldSession.invalidate();
		HttpSession newSession = request.getSession(true);
		attrNames = props.keys();

		while (attrNames != null && attrNames.hasMoreElements()) {
			String key = (String) attrNames.nextElement();
			newSession.setAttribute(key, props.get(key));
		}
	}
    
    /**
     * 
     * @param credentials
     * @param rememberMe
     * @param request
     * @throws AxisFault
     */
    @SuppressWarnings("rawtypes")
    public void handleSecurity(Object credentials, boolean rememberMe, HttpServletRequest request)
            throws AuthenticationException {
    	
    	regenrateSession(request);

        String backendServerURL = getBackendUrl(request);
        HttpSession session = request.getSession();
        LoggedUserInfoAdminStub stub;
        String loggedinUser = null;

        if (backendServerURL == null) {
            throw new AuthenticationException("Server not initialized properly.");
        }

        try {

            stub = getLoggedUserInfoAdminStub(backendServerURL, session);
            ServiceClient client = stub._getServiceClient();

            // In side this method - Authenticators should complete the authentication with the
            // back-end service Or - it should set the required authentication headers, there are
            // required in future service calls. Also each Authenticator should know how to handle
            // the Remember Me logic.
            loggedinUser = doAuthentication(credentials, rememberMe, client, request);

            if (isAdminCookieSet()) {
                // If the UI Authenticator takes the responsibility of setting the Admin Cookie,it
                // has to set the value in the session with the key
                // ServerConstants.ADMIN_SERVICE_AUTH_TOKEN.
                client.getServiceContext().setProperty(HTTPConstants.COOKIE_STRING,
                        session.getAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN));
            }

            // By now, user is authenticated or proper authentication headers been set. This call
            // set retrieve user authorization information from the back-end.
            setUserAuthorizationInfo(stub, session);

            if (!isAdminCookieSet()) {
                // If authentication successful set the cookie.
                // Authenticators them selves have not set the cookie.
                setAdminCookie(session, client, null);
            }

            // Process remember me data in reply
            if (rememberMe) {
                OperationContext operationContext = client.getLastOperationContext();
                MessageContext inMessageContext;
                Map transportHeaders;
                inMessageContext = operationContext
                        .getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
                transportHeaders = (Map) inMessageContext
                        .getProperty(MessageContext.TRANSPORT_HEADERS);
                handleRememberMe(transportHeaders, request);
            }

            onSuccessAdminLogin(request, loggedinUser);
            
        } catch (RemoteException e) {
            throw new AuthenticationException(e.getMessage(), e);
        } catch (Exception e) {
            throw new AuthenticationException(
                    "Exception occurred while accessing user authorization info", e);
        }
    }

    /**
     * 
     */
    public boolean skipLoginPage() {
        return false;
    }

    /**
     * 
     * @param request
     * @param userName
     * @throws Exception
     */
    public void onSuccessAdminLogin(HttpServletRequest request, String userName) throws Exception {

		HttpSession session = request.getSession();
		
    	String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        if (tenantDomain != null && tenantDomain.trim().length() > 0) {
            session.setAttribute(MultitenantConstants.TENANT_DOMAIN, tenantDomain);
            // we will make it an attribute on request as well
            if (request.getAttribute(MultitenantConstants.TENANT_DOMAIN) == null) {
                request.setAttribute(MultitenantConstants.TENANT_DOMAIN, tenantDomain);
            }
        } else {
            audit.info("User with null domain tried to login.");
            return;
        }
        
		if (session.getAttribute(CarbonConstants.LOGGED_USER) != null) {
			userName = (String) session
					.getAttribute(CarbonConstants.LOGGED_USER);
		}
		request.setAttribute(AbstractCarbonUIAuthenticator.USERNAME, userName);

        String serverURL = getBackendUrl(request);
        if (serverURL == null) {
            throw new AuthenticationException("Server not initialized properly.");
        }

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        // For local transport, cookie might be null.
        if ((serverURL == null || cookie == null) && (!CarbonUtils.isRunningOnLocalTransportMode())) {
            throw new Exception("Cannot proceed logging in. The server URL and/or Cookie is null");
        }

        if (tenantDomain != null
                && MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain.trim())) {
            request.getSession().setAttribute(MultitenantConstants.IS_SUPER_TENANT, "true");
        } else if (tenantDomain != null && tenantDomain.trim().length() > 0) {
            session.setAttribute(MultitenantConstants.TENANT_DOMAIN, tenantDomain);
            // we will make it an attribute on request as well
            if (request.getAttribute(MultitenantConstants.TENANT_DOMAIN) == null) {
                request.setAttribute(MultitenantConstants.TENANT_DOMAIN, tenantDomain);
            }
        } else {
            audit.info("User with null domain tried to login.");
            return;
        }

        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(userName);

        setUserInformation(cookie, serverURL, session);
       
		session.setAttribute(CarbonConstants.LOGGED_USER, tenantAwareUserName);
        session.getServletContext().setAttribute(CarbonConstants.LOGGED_USER, tenantAwareUserName);
        session.setAttribute("authenticated", Boolean.parseBoolean("true"));

        UIAuthenticationExtender[] uiAuthenticationExtenders = CarbonUIServiceComponent
                .getUIAuthenticationExtenders();
        for (UIAuthenticationExtender uiAuthenticationExtender : uiAuthenticationExtenders) {
            uiAuthenticationExtender.onSuccessAdminLogin(request, tenantAwareUserName,
                    tenantDomain, serverURL);
        }
    }

    /**
     * 
     * @param cookie
     * @param backendServerURL
     * @param session
     * @throws RemoteException
     */
    protected void setUserInformation(String cookie, String backendServerURL, HttpSession session)
            throws RemoteException {
        try {

            if (session.getAttribute(ServerConstants.USER_PERMISSIONS) != null) {
                return;
            }

            ServletContext servletContext = session.getServletContext();
            ConfigurationContext configContext = (ConfigurationContext) servletContext
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

            LoggedUserInfoAdminStub stub = new LoggedUserInfoAdminStub(configContext,
                    backendServerURL + "LoggedUserInfoAdmin");
            ServiceClient client = stub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
            org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfo userInfo = stub
                    .getUserInfo();

            String[] permissionArray = userInfo.getUIPermissionOfUser();
            ArrayList<String> list = new ArrayList<String>();
            for (String permission : permissionArray) {
                list.add(permission);
            }

            session.setAttribute(ServerConstants.USER_PERMISSIONS, list);
            if (userInfo.getPasswordExpiration() != null) {
                session.setAttribute(ServerConstants.PASSWORD_EXPIRATION,
                        userInfo.getPasswordExpiration());
            }
        } catch (AxisFault e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new AxisFault("Exception occured", e);
        }
    }

    /**
     * 
     * @param cookieValue
     * @return
     */
    protected static String getUserNameFromCookie(String cookieValue) {
        int index = cookieValue.indexOf('-');
        return cookieValue.substring(0, index);
    }

    /**
     * 
     * @param session
     * @param serviceClient
     * @param rememberMeCookie
     * @throws AxisFault
     */
    protected void setAdminCookie(HttpSession session, ServiceClient serviceClient,
            String rememberMeCookie) throws AxisFault {
        String cookie = (String) serviceClient.getServiceContext().getProperty(
                HTTPConstants.COOKIE_STRING);

        if (cookie == null) {
            // For local transport - the cookie will be null.
            // This generated cookie cannot be used for any form authentication with the backend.
            // This is done to be backward compatible.
            cookie = UUIDGenerator.generateUUID();
        }

        if (rememberMeCookie != null) {
            cookie = cookie + "; " + rememberMeCookie;
        }

        if (session != null) {
            session.setAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN, cookie);
        }
    }

    /**
     * 
     * @param backendServerURL
     * @param session
     * @return
     * @throws AxisFault
     */
    private LoggedUserInfoAdminStub getLoggedUserInfoAdminStub(String backendServerURL,
            HttpSession session) throws AxisFault {

        ServletContext servletContext = session.getServletContext();
        ConfigurationContext configContext = (ConfigurationContext) servletContext
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

        if (configContext == null) {
            String msg = "Configuration context is null.";
            log.error(msg);
            throw new AxisFault(msg);
        }

        return new LoggedUserInfoAdminStub(configContext, backendServerURL + "LoggedUserInfoAdmin");
    }

    /**
     * 
     * @param loggedUserInfoAdminStub
     * @param session
     * @throws ExceptionException
     * @throws RemoteException
     */
    private void setUserAuthorizationInfo(LoggedUserInfoAdminStub loggedUserInfoAdminStub,
            HttpSession session) throws ExceptionException, RemoteException {

        ServiceClient client = loggedUserInfoAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);

        LoggedUserInfo userInfo = loggedUserInfoAdminStub.getUserInfo();
        
        String[] permissionArray = userInfo.getUIPermissionOfUser();
        ArrayList<String> list = new ArrayList<String>();

        Collections.addAll(list, permissionArray);

        session.setAttribute(ServerConstants.USER_PERMISSIONS, list);
        if (userInfo.getPasswordExpiration() != null) {
            session.setAttribute(ServerConstants.PASSWORD_EXPIRATION,
                    userInfo.getPasswordExpiration());
        }
        
		if (session.getAttribute(CarbonConstants.LOGGED_USER) == null) {
			session.setAttribute(CarbonConstants.LOGGED_USER, userInfo.getUserName());
		}

    }

    /**
     * 
     * @param request
     * @return
     */
    private String getBackendUrl(HttpServletRequest request) {

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        String backendServerURL = request.getParameter("backendURL");

        if (backendServerURL == null) {
            backendServerURL = CarbonUIUtil.getServerURL(servletContext, request.getSession());
        }

        if (backendServerURL != null) {
            session.setAttribute(CarbonConstants.SERVER_URL, backendServerURL);
        }

        return backendServerURL;
    }

    /**
     * 
     * @return
     */
    private AuthenticatorsConfiguration.AuthenticatorConfig getAuthenticatorConfig() {

        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration
                .getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = authenticatorsConfiguration
                .getAuthenticatorConfig(getAuthenticatorName());

        return authenticatorConfig;
    }

}
