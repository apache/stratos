package org.wso2.carbon.ui;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.ui.util.CarbonUIAuthenticationUtil;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class BasicAuthUIAuthenticator extends AbstractCarbonUIAuthenticator {

    private static final String AUTHENTICATOR_NAME = "BasicAuthUIAuthenticator";

    @Override
    public boolean canHandle(HttpServletRequest request) {

        String userName = request.getParameter(AbstractCarbonUIAuthenticator.USERNAME);
        String password = request.getParameter(AbstractCarbonUIAuthenticator.PASSWORD);

        if (CarbonUtils.isRunningOnLocalTransportMode()) {
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

    @Override
    public void authenticate(HttpServletRequest request) throws AuthenticationException {

        String userName = request.getParameter(AbstractCarbonUIAuthenticator.USERNAME);
        String password = request.getParameter(AbstractCarbonUIAuthenticator.PASSWORD);
        String value = request.getParameter(AbstractCarbonUIAuthenticator.REMEMBER_ME);

        boolean isRememberMe = false;

        if (userName == null || password == null) {
            throw new AuthenticationException("Invalid username or password provided.");
        }

        if (value != null && value.equals(AbstractCarbonUIAuthenticator.REMEMBER_ME)) {
            isRememberMe = true;
        }

        String userNameWithDomain = userName;
        String domainName = (String) request.getAttribute(MultitenantConstants.TENANT_DOMAIN);
        if (domainName != null) {
            userNameWithDomain += "@" + domainName;
        }
        if (userNameWithDomain != null) {
            // User-name can be null in remember me scenario.
            userNameWithDomain = userNameWithDomain.trim();
        }
        DefaultAuthenticatorCredentials credentials;
        credentials = new DefaultAuthenticatorCredentials(userNameWithDomain, password);

        // No exception means authentication successful
        handleSecurity(credentials, isRememberMe, request);
    }

    @Override
    public void authenticateWithCookie(HttpServletRequest request) throws AuthenticationException {

        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)) {
                DefaultAuthenticatorCredentials credentials;
                credentials = new DefaultAuthenticatorCredentials(null, null);
                // No exception means authentication successful
                handleSecurity(credentials, true, request);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String doAuthentication(Object credentials, boolean isRememberMe, ServiceClient client,
            HttpServletRequest request) throws AuthenticationException {

        DefaultAuthenticatorCredentials defaultCredentials = (DefaultAuthenticatorCredentials) credentials;

        if (isRememberMe && defaultCredentials.getUserName() == null
                && defaultCredentials.getPassword() == null) {
            // This is to login with Remember Me.
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)) {
                        CarbonUIAuthenticationUtil.setCookieHeaders(cookie, client);
                        String cookieValue = cookie.getValue();
                        return getUserNameFromCookie(cookieValue);
                    }
                }
            }
        } else {
            CarbonUtils.setBasicAccessSecurityHeaders(defaultCredentials.getUserName(),
                    defaultCredentials.getPassword(), isRememberMe, client);
            return defaultCredentials.getUserName();
        }

        throw new AuthenticationException("Invalid user credentials.");
    }

    /**
     * 
     * @param serviceClient
     * @param httpServletRequest
     * @throws AxisFault
     */
    @SuppressWarnings("rawtypes")
    public void handleRememberMe(Map transportHeaders, HttpServletRequest httpServletRequest)
            throws AuthenticationException {

        if (transportHeaders != null) {
            String cookieValue = (String) transportHeaders.get("RememberMeCookieValue");
            String cookieAge = (String) transportHeaders.get("RememberMeCookieAge");

            if (cookieValue == null || cookieAge == null) {
                throw new AuthenticationException("Unable to load remember me date from response. "
                        + "Cookie value or cookie age or both are null");
            }

            if (log.isDebugEnabled()) {
                log.debug("Cookie value returned " + cookieValue + " cookie age " + cookieAge);
            }

            httpServletRequest.setAttribute(CarbonConstants.REMEMBER_ME_COOKIE_VALUE, cookieValue);
            httpServletRequest.setAttribute(CarbonConstants.REMEMBER_ME_COOKIE_AGE, cookieAge);
        }
    }

    @Override
    public void unauthenticate(Object object) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

}
