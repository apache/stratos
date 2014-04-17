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

package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.ui.tracker.AuthenticatorRegistry;
import org.wso2.carbon.utils.UserUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.regex.Pattern;

public final class CarbonUILoginUtil {

    private static Log log = LogFactory.getLog(CarbonUILoginUtil.class);
    private static Pattern tenantEnabledUriPattern;
    private static final String TENANT_ENABLED_URI_PATTERN = "(/.*/|/)"
            + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/[^/]*($|/.*)";
    protected static final int RETURN_FALSE = 0;
    protected static final int RETURN_TRUE = 1;
    protected static final int CONTINUE = 2;

    static {
        tenantEnabledUriPattern = Pattern.compile(TENANT_ENABLED_URI_PATTERN);
    }

    /**
     * 
     * @return
     */
    protected static Pattern getTenantEnabledUriPattern() {
        return tenantEnabledUriPattern;
    }

    /**
     * Returns the corresponding CarbonAuthenticator based on the request.
     * 
     * @param request
     * @return
     */
    protected static CarbonUIAuthenticator getAuthenticator(HttpServletRequest request) {
        return AuthenticatorRegistry.getCarbonAuthenticator(request);
    }

    /**
     * 
     * @param authenticator
     * @param request
     * @param response
     * @param session
     * @param skipLoginPage
     * @param contextPath
     * @param indexPageURL
     * @param requestedURI
     * @return
     * @throws IOException
     */
    protected static boolean saveOriginalUrl(CarbonUIAuthenticator authenticator,
            HttpServletRequest request, HttpServletResponse response, HttpSession session,
            boolean skipLoginPage, String contextPath, String indexPageURL, String requestedURI)
            throws IOException {

        // Saving originally requested url should not forward to error page after login
        if (!requestedURI.endsWith("admin/error.jsp")) {
            String queryString = request.getQueryString();
            String tmpURI;
            if (queryString != null) {
                tmpURI = requestedURI + "?" + queryString;
            } else {
                tmpURI = requestedURI;
            }
            tmpURI = "../.." + tmpURI;
            request.getSession(false).setAttribute("requestedUri", tmpURI);
            if (!tmpURI.contains("session-validate.jsp") && !("/null").equals(requestedURI)) {
                // Also setting it in a cookie, for non-remember-me cases
                Cookie cookie = new Cookie("requestedURI", tmpURI);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }

        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)
                            && authenticator != null) {
                        try {
                            authenticator.authenticateWithCookie(request);
                            return true;
                        } catch (AuthenticationException ignored) {
                            // We can ignore here and proceed with normal login.
                            if (log.isDebugEnabled()) {
                                log.debug(ignored);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("error occurred while login", e);
        }

        if (request.getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
            if (skipLoginPage) {
                response.sendRedirect("../admin/login_action.jsp");
            } else {
                response.sendRedirect("../admin/login.jsp");
            }
        } else {
            if (skipLoginPage) {
                response.sendRedirect(contextPath + "/carbon/admin/login_action.jsp");
            } else {
                response.sendRedirect(contextPath + "/carbon/admin/login.jsp");

            }
        }
        return false;
    }

    /**
     * 
     * @param request
     * @param indexPageURL
     * @return
     */
    protected static String getCustomIndexPage(HttpServletRequest request, String indexPageURL) {
        // If a custom index page is used send the login request with the index page specified
        if (request.getParameter(CarbonConstants.INDEX_PAGE_URL) != null) {
            return request.getParameter(CarbonConstants.INDEX_PAGE_URL);
        } else if (indexPageURL == null) {
            return "/carbon/admin/index.jsp";
        }

        return indexPageURL;
    }

    /**
     * 
     * @param requestedURI
     * @param indexPageURL
     * @param request
     * @return
     */
    protected static String getIndexPageUrlFromCookie(String requestedURI, String indexPageURL,
            HttpServletRequest request) {
        if (requestedURI.equals("/carbon/admin/login_action.jsp")) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("requestedURI")) {
                        indexPageURL = cookie.getValue();
                    }
                }
                // Removing any tenant specific strings from the cookie value for the indexPageURL
                if (tenantEnabledUriPattern.matcher(indexPageURL).matches()) {
                    indexPageURL = CarbonUIUtil.removeTenantSpecificStringsFromURL(indexPageURL);
                }
            }
        }
        return indexPageURL;
    }

    /**
     * 
     * @param requestedURI
     * @return
     */
    protected static boolean letRequestedUrlIn(String requestedURI, String tempUrl) {
        if (requestedURI.endsWith(".css") || requestedURI.endsWith(".gif")
                || requestedURI.endsWith(".GIF") || requestedURI.endsWith(".jpg")
                || requestedURI.endsWith(".JPG") || requestedURI.endsWith(".png")
                || requestedURI.endsWith(".PNG") || requestedURI.endsWith(".xsl")
                || requestedURI.endsWith(".xslt") || requestedURI.endsWith(".js")
                || requestedURI.startsWith("/registry") || requestedURI.endsWith(".html")
                || requestedURI.endsWith(".ico") || requestedURI.startsWith("/openid/")
                || requestedURI.indexOf("/openid/") > -1
                || requestedURI.indexOf("/openidserver") > -1
                || requestedURI.indexOf("/gadgets") > -1 || requestedURI.indexOf("/samlsso") > -1) {
            return true;
        }
        return false;
    }

    /**
     * 
     * @param authenticator
     * @param request
     * @param response
     * @param session
     * @param authenticated
     * @param contextPath
     * @param indexPageURL
     * @param httpLogin
     * @return
     * @throws IOException
     */
    protected static boolean handleLogout(CarbonUIAuthenticator authenticator,
            HttpServletRequest request, HttpServletResponse response, HttpSession session,
            boolean authenticated, String contextPath, String indexPageURL, String httpLogin)
            throws IOException {
    	log.debug("Handling Logout..");
        // Logout the user from the back-end
        try {
            authenticator = (CarbonUIAuthenticator) session
                    .getAttribute(CarbonSecuredHttpContext.CARBON_AUTHNETICATOR);
            if (authenticator != null) {
                authenticator.unauthenticate(request);
                log.debug("Backend session invalidated");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.sendRedirect("../admin/login.jsp");
            return false;
        }

        // Only applicable if this is SAML2 based SSO. Complete the logout action after receiving
        // the Logout response.
        if ("true".equals(request.getParameter("logoutcomplete"))) {
            HttpSession currentSession = request.getSession(false);
            if (currentSession != null) {
                // check if current session has expired
                session.removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
                session.getServletContext().removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
                try {
                    session.invalidate();
                } catch (Exception ignored) { // Ignore exception when
                    // invalidating and
                    // invalidated session
                }
                log.debug("Frontend session invalidated");
            }
            response.sendRedirect("../../carbon/admin/login.jsp");
            return false;
        }
        
		if (request.getAttribute("ExternalLogoutPage") != null) {
			HttpSession currentSession = request.getSession(false);
			if (currentSession != null) {
				session.removeAttribute("logged-user");
				session.getServletContext().removeAttribute("logged-user");
				try {
					session.invalidate();
				} catch (Exception ignored) {
				}
				log.debug("Frontend session invalidated");
			}

			response.sendRedirect((String) request.getAttribute("ExternalLogoutPage"));
			return false;
		}

        CarbonSSOSessionManager ssoSessionManager = CarbonSSOSessionManager.getInstance();

        if (!ssoSessionManager.skipSSOSessionInvalidation(request, authenticator)
                && !ssoSessionManager.isSessionValid(request.getSession().getId())) {
            HttpSession currentSession = request.getSession(false);
            if (currentSession != null) {
                // check if current session has expired
                session.removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
                session.getServletContext().removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
                try {
                    session.invalidate();
                    log.debug("SSO session session invalidated ");
                } catch (Exception ignored) { // Ignore exception when
                    // Invalidating and invalidated session
                    if (log.isDebugEnabled()) {
                        log.debug("Ignore exception when invalidating session", ignored);
                    }
                }
            }
            response.sendRedirect("../.." + indexPageURL);
            return false;
        }

        // Memory clean up : remove invalid session from the invalid session list.
        ssoSessionManager.removeInvalidSession(request.getSession().getId());

        // This condition is evaluated when users are logged out in SAML2 based SSO
        if (request.getAttribute("logoutRequest") != null) {
        	log.debug("Loging out from SSO session");
            response.sendRedirect("../../carbon/sso-acs/redirect_ajaxprocessor.jsp?logout=true");
            return false;
        }

        HttpSession currentSession = request.getSession(false);
        if (currentSession != null) {
            // Check if current session has expired
            session.removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
            session.getServletContext().removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
            try {
                session.invalidate();
                log.debug("Frontend session invalidated");
            } catch (Exception ignored) {
                // Ignore exception when invalidating and invalidated session
            }
        }

        Cookie rmeCookie = new Cookie(CarbonConstants.REMEMBER_ME_COOKE_NAME, null);
        rmeCookie.setPath("/");
        rmeCookie.setSecure(true);
        rmeCookie.setMaxAge(0);
        response.addCookie(rmeCookie);
        response.sendRedirect(contextPath + indexPageURL);
        return false;
    }

    /**
     * 
     * @param authenticator
     * @param request
     * @param response
     * @param session
     * @param authenticated
     * @param contextPath
     * @param indexPageURL
     * @param httpLogin
     * @return
     * @throws IOException
     */
    protected static boolean handleLogin(CarbonUIAuthenticator authenticator,
            HttpServletRequest request, HttpServletResponse response, HttpSession session,
            boolean authenticated, String contextPath, String indexPageURL, String httpLogin)
            throws IOException {
        try {

            // commenting out this method as it is not required
//        	String[] username = (String[])request.getParameterMap().get(AbstractCarbonUIAuthenticator.USERNAME);
//        	if(username != null && !username[0].contains("/") && UserUtils.hasMultipleUserStores()){
//            	response.sendRedirect("../../carbon/admin/login.jsp?loginStatus=false&errorCode=domain.not.specified");
//            	return false;
//        	}
        	
            authenticator.authenticate(request);
            session = request.getSession();
            session.setAttribute(CarbonSecuredHttpContext.CARBON_AUTHNETICATOR, authenticator);

            // Check if the username is of type bob@acme.com if so, this is a login from a
            // multi-tenant deployment
            // The tenant id part(i.e. acme.com) should be set into http session for further UI
            // related processing
            String userName = (String) request.getAttribute(AbstractCarbonUIAuthenticator.USERNAME);
            
            if(log.isDebugEnabled()) {
            	log.debug("Login request from " + userName);
            }
            String tenantDomain = null;
            if (request.getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
                tenantDomain = (String) request.getAttribute(MultitenantConstants.TENANT_DOMAIN);

            }
            if (tenantDomain == null) {
                tenantDomain = MultitenantUtils.getTenantDomain(userName);
            }
            if (tenantDomain != null
                    && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                // we will add it to the context
                contextPath += "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/"
                        + tenantDomain;
            }

            String value = request.getParameter("rememberMe");
            boolean isRememberMe = false;
            if (value != null && value.equals("rememberMe")) {
                isRememberMe = true;
            }

            try {
                if (isRememberMe) {
                    String rememberMeCookieValue = (String) request
                            .getAttribute(CarbonConstants.REMEMBER_ME_COOKIE_VALUE);
                    int age = Integer.parseInt((String) request
                            .getAttribute(CarbonConstants.REMEMBER_ME_COOKIE_AGE));

                    Cookie rmeCookie = new Cookie(CarbonConstants.REMEMBER_ME_COOKE_NAME,
                            rememberMeCookieValue);
                    rmeCookie.setPath("/");
                    rmeCookie.setSecure(true);
                    rmeCookie.setMaxAge(age);
                    response.addCookie(rmeCookie);
                }
            } catch (Exception e) {
                response.sendRedirect(contextPath + indexPageURL
                        + (indexPageURL.indexOf('?') == -1 ? "?" : "&") + "loginStatus=false");
				if (log.isDebugEnabled()) {
					log.debug("Security check failed for login request for " + userName);
				}
                return false;
            }
            if (contextPath != null) {
                if (indexPageURL.startsWith("../..")) {
                    indexPageURL = indexPageURL.substring(5);
                }

                response.sendRedirect(contextPath + indexPageURL
                        + (indexPageURL.indexOf('?') == -1 ? "?" : "&") + "loginStatus=true");
            }

        } catch (AuthenticationException e) {
            log.debug("Authentication failure ...", e);
            try {
                request.getSession().invalidate();
                getAuthenticator(request).unauthenticate(request);
                if (httpLogin != null) {
                    response.sendRedirect(httpLogin + "?loginStatus=false");
                    return false;
                } else {
                    response.sendRedirect("/carbon/admin/login.jsp?loginStatus=false");
                    return false;
                }
            } catch (Exception e1) {
                // ignore exception 
            }

        } catch (Exception e) {
            log.error("error occurred while login", e);
            response.sendRedirect("../../carbon/admin/login.jsp?loginStatus=failed");
        }

        return false;
    }

    /**
     * 
     * @param requestedURI
     * @param request
     * @return
     */
    protected static String getForcedSignOutRequestedURI(String requestedURI,
            HttpServletRequest request) {
        if (requestedURI.endsWith(".jsp")
                && !requestedURI.endsWith("ajaxprocessor.jsp")
                && !requestedURI.endsWith("session_validate.jsp")
                && (request.getSession().getAttribute("authenticated")) != null
                && ((Boolean) (request.getSession().getAttribute("authenticated"))).booleanValue()
                && ((request.getSession().getAttribute(MultitenantConstants.TENANT_DOMAIN) == null && request
                        .getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) || ((request
                        .getSession().getAttribute(MultitenantConstants.TENANT_DOMAIN) != null && request
                        .getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) && !request
                        .getSession().getAttribute(MultitenantConstants.TENANT_DOMAIN)
                        .equals(request.getAttribute(MultitenantConstants.TENANT_DOMAIN))))) {
            // If someone signed in from a tenant, try to access a different tenant domain, he
            // should be forced to sign out without any prompt Cloud requirement
            requestedURI = "../admin/logout_action.jsp";
        }

        return requestedURI;
    }

    /**
     * 
     * @param requestedURI
     * @param request
     * @param response
     * @param authenticated
     * @param context
     * @param indexPageURL
     * @return
     * @throws IOException
     */
    protected static int handleLoginPageRequest(String requestedURI, HttpServletRequest request,
            HttpServletResponse response, boolean authenticated, String context, String indexPageURL)
            throws IOException {
        if (requestedURI.indexOf("login.jsp") > -1
                || requestedURI.indexOf("login_ajaxprocessor.jsp") > -1
                || requestedURI.indexOf("admin/layout/template.jsp") > -1
                || requestedURI.endsWith("/filedownload") || requestedURI.endsWith("/fileupload")
                || requestedURI.indexOf("/fileupload/") > -1
                || requestedURI.indexOf("login_action.jsp") > -1
                || requestedURI.indexOf("admin/jsp/WSRequestXSSproxy_ajaxprocessor.jsp") > -1) {

            if ((requestedURI.indexOf("login.jsp") > -1
                    || requestedURI.indexOf("login_ajaxprocessor.jsp") > -1 || requestedURI
                    .indexOf("login_action.jsp") > -1) && authenticated) {
                // User has typed the login page url, while being logged in
                if (request.getSession().getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
                    String tenantDomain = (String) request.getSession().getAttribute(
                            MultitenantConstants.TENANT_DOMAIN);
                    if (tenantDomain != null
                            && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                        context += "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/"
                                + tenantDomain;
                    }
                }
                if(log.isDebugEnabled()){
                	log.debug("User already authenticated. Redirecting to " + indexPageURL);
                }
                // redirect relative to the servlet container root
                response.sendRedirect(context + "/carbon/admin/index.jsp");
                return RETURN_FALSE;
            } else if (requestedURI.indexOf("login_action.jsp") > -1 && !authenticated) {
                // User is not yet authenticated and now trying to get authenticated
                // do nothing, leave for authentication at the end
                if (log.isDebugEnabled()) {
                    log.debug("User is not yet authenticated and now trying to get authenticated;"
                            + "do nothing, leave for authentication at the end");
                }
                return CONTINUE;
            } else {
				if (log.isDebugEnabled()) {
					log.debug("Skipping security checks for " + requestedURI);
				}
                return RETURN_TRUE;
            }
        }

        return CONTINUE;
    }

    /**
     * 
     * @param authenticated
     * @param response
     * @param requestedURI
     * @param context
     * @return
     * @throws IOException
     */
    protected static boolean escapeTenantWebAppRequests(boolean authenticated,
            HttpServletResponse response, String requestedURI, String context) throws IOException {
        // Tenant webapp requests should never reach Carbon. It can happen
        // if Carbon is deployed at / context and requests for non-existent tenant webapps is made.
        if (requestedURI.contains("/webapps/")) {
            response.sendError(404, "Web application not found. Request URI: " + requestedURI);
            return false;
        } else if (requestedURI.contains("/carbon/admin/login.jsp") && !authenticated) {
            // a tenant requesting login.jsp while not being authenticated
            // redirecting the tenant login page request to the root /carbon/admin/login.jsp
            // instead of tenant-aware login page
            response.sendRedirect(context + "/carbon/admin/login.jsp");
           	log.debug("Redirecting to /carbon/admin/login.jsp");
            return false;
        }
        log.debug("Skipping security checks");
        return true;
    }

    /**
     * 
     * @param requestedURI
     * @return
     */
    protected static String addNewContext(String requestedURI) {
        String tmp = requestedURI;
        String customWarContext = "";
        if (requestedURI.startsWith("/carbon") && !(requestedURI.startsWith("/carbon/carbon/"))) {
            // one can name the folder as 'carbon'
            requestedURI = tmp;
        } else if (requestedURI.indexOf("filedownload") == -1
                && requestedURI.indexOf("fileupload") == -1) {
            // replace first context
            String tmp1 = tmp.replaceFirst("/", "");
            int end = tmp1.indexOf('/');
            if (end > -1) {
                customWarContext = tmp1.substring(0, end);
                // one can rename the war file as 'registry'.
                // This will conflict with our internal 'registry' context
                if (!(requestedURI.startsWith("/registry/registry/"))
                        && !(requestedURI.startsWith("/registry/carbon/"))
                        && (customWarContext.equals("registry")
                                || customWarContext.equals("gadgets") || customWarContext
                                .equals("social"))) {
                    requestedURI = tmp;
                } else {
                    requestedURI = tmp.substring(end + 1);
                }
            }
        }

        return requestedURI;
    }
}
