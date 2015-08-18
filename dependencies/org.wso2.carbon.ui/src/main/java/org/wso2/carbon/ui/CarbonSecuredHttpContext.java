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
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.ui.deployment.beans.CarbonUIDefinitions;
import org.wso2.carbon.ui.deployment.beans.Context;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;

public class CarbonSecuredHttpContext extends SecuredComponentEntryHttpContext {

    public static final String LOGGED_USER = CarbonConstants.LOGGED_USER;
    public static final String CARBON_AUTHNETICATOR = "CarbonAuthenticator";

    private static final Log log = LogFactory.getLog(CarbonSecuredHttpContext.class);
    private Bundle bundle = null;

    private HashMap<String, String> httpUrlsToBeByPassed = new HashMap<String, String>();
    private HashMap<String, String> urlsToBeByPassed = new HashMap<String, String>();
    private String defaultHomePage;
    private Context defaultContext;

    /**
     * 
     * @param bundle
     * @param s
     * @param uiResourceRegistry
     * @param registry
     */
    public CarbonSecuredHttpContext(Bundle bundle, String s, UIResourceRegistry uiResourceRegistry,
            Registry registry) {
        super(bundle, s, uiResourceRegistry);
        this.registry = registry;
        this.bundle = bundle;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String requestedURI = request.getRequestURI();
        
        // Get the matching CarbonUIAuthenticator. If no match found for the given request, this
        // will return null.
        CarbonUIAuthenticator authenticator = CarbonUILoginUtil.getAuthenticator(request);

        // This check is required for Single Logout implementation. If the request is not for SSO
        // based authentication page or SSO Servlet, then if the session is invalid redirect the
        // requests to logout_action.jsp.
        CarbonSSOSessionManager ssoSessionManager = CarbonSSOSessionManager.getInstance();
        requestedURI = ssoSessionManager.getRequestedUrl(request,authenticator);

        HttpSession session;
        String sessionId;
        boolean authenticated = false;

        try {
            // Get the user's current authenticated session - if any exists.
            session = request.getSession();
            sessionId = session.getId();
            Boolean authenticatedObj = (Boolean) session.getAttribute("authenticated");
            if (authenticatedObj != null) {
                authenticated = authenticatedObj.booleanValue();
                if(log.isDebugEnabled()){
                	log.debug("Is authenticated " + authenticated);
                }
            }
        } catch (Exception e) {
            log.debug("No session exits");
            return false;
        }

        String context = request.getContextPath();
        if ("/".equals(context)) {
            context = "";
        }

        // We eliminate the /tenant/{tenant-domain} from authentications
        Matcher matcher = CarbonUILoginUtil.getTenantEnabledUriPattern().matcher(requestedURI);
        if (matcher.matches()) {
        	log.debug("Tenant webapp request " + requestedURI);
            return CarbonUILoginUtil.escapeTenantWebAppRequests(authenticated, response,
                    requestedURI, context);
        }

        // TODO: When filtered from a Servlet filter the request uri always contains 2 //, this is a
        // temporary fix
        if (requestedURI.indexOf("//") == 0) {
            requestedURI = requestedURI.substring(1);
        }


        if (httpUrlsToBeByPassed.isEmpty()) {
            // Populates http urls to be by passed.
            populatehttpUrlsToBeByPassed();
        }

        if (requestedURI.equals(context) || requestedURI.equals(context + "/")) {
            return handleRequestOnContext(request, response);
        }

        // Storing intermediate value of requestedURI.
        // This is needed for OpenID authentication later.
        String tempUrl = requestedURI;

        // When war is deployed on top of an existing app server we cannot use root context
        // for deployment. Hence a new context is added.Now url changes from eg:
        // carbon/admin/index.jsp to wso2/carbon/admin/index.jsp In this case before doing anything,
        // we need to remove web app context (eg: wso2) .
        CarbonUILoginUtil.addNewContext(requestedURI);

        // Disabling http access to admin console user guide documents should be allowed to access
        // via http protocol
        int val = -1;
        if ((val = allowNonSecuredContent(requestedURI, request, response, authenticated,
                authenticator)) != CarbonUILoginUtil.CONTINUE) {
            if (val == CarbonUILoginUtil.RETURN_TRUE) {
            	log.debug("Skipping security check for non secured content. " + requestedURI);
                return true;
            } else {
            	log.debug("Security check failed for the resource " + requestedURI);
                return false;
            }
        }
        // We are allowing requests for  .jar/.class resources. Otherwise applets won't get loaded
        // due to session checks. (applet loading happens over https://)
        if(requestedURI.endsWith(".jar") || requestedURI.endsWith(".class")) {
           log.debug("Skipping authentication for .jar files and .class file." + requestedURI);
           return true;
        }


        String resourceURI = requestedURI.replaceFirst("/carbon/", "../");

        if (log.isDebugEnabled()) {
            log.debug("CarbonSecuredHttpContext -> handleSecurity() requestURI:" + requestedURI
                    + " id:" + sessionId + " resourceURI:" + resourceURI);
        }

        if (urlsToBeByPassed.isEmpty()) {
            // retrieve urls that should be by-passed from security check
            populateUrlsToBeBypassed();
        }

        // if the current uri is marked to be by-passed, let it pass through
        if (isCurrentUrlToBePassed(request, session, resourceURI)) {
            return true;
        }

        String indexPageURL = CarbonUIUtil.getIndexPageURL(session.getServletContext(),
                request.getSession());

        // Reading the requestedURL from the cookie to obtain the request made while not
        // authanticated; and setting it as the indexPageURL
        indexPageURL = CarbonUILoginUtil.getIndexPageUrlFromCookie(requestedURI, indexPageURL,
                request);

        // If a custom index page is used send the login request with the indexpage specified
        indexPageURL = CarbonUILoginUtil.getCustomIndexPage(request, indexPageURL);

        // Reading home page set on product.xml
        // If the params in the servletcontext is null get them from the UTIL
        indexPageURL = updateIndexPageWithHomePage(indexPageURL);

        if ((val = CarbonUILoginUtil.handleLoginPageRequest(requestedURI, request, response,
                authenticated, context, indexPageURL)) != CarbonUILoginUtil.CONTINUE) {
            if (val == CarbonUILoginUtil.RETURN_TRUE) {
                return true;
            } else {
                return false;
            }
        }

        // If authenticator defines to skip URL, return true
        if (ssoSessionManager.skipAuthentication(request)) {
        	if(log.isDebugEnabled()){
        		log.debug("Skipping security checks for authenticator defined URL " + requestedURI);
        	}
            return true;
        }

        // If someone signed in from a tenant, try to access a different tenant domain, he should be
        // forced to sign out without any prompt Cloud requirement
        requestedURI = CarbonUILoginUtil.getForcedSignOutRequestedURI(requestedURI, request);

        String contextPath = (request.getContextPath().equals("") || request.getContextPath()
                .equals("/")) ? "" : request.getContextPath();

        String tenantDomain = (String) session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
        if (tenantDomain != null && !tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            contextPath += "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + tenantDomain;
        }

        String httpLogin = request.getParameter("gsHttpRequest");

        boolean skipLoginPage = false;

        // Login page is not required for all the Authenticators.
        if (authenticator != null
                && authenticator.skipLoginPage()
                && requestedURI.indexOf("login_action.jsp") < 0
                && (requestedURI.endsWith("/carbon/") || (requestedURI.indexOf("/registry/atom") == -1 && requestedURI
                        .endsWith("/carbon")))) {
            // Add this to session for future use.
            request.getSession().setAttribute("skipLoginPage", "true");
        }

        if (request.getSession().getAttribute("skipLoginPage") != null) {
            if ("true".equals(((String) request.getSession().getAttribute("skipLoginPage")))) {
                skipLoginPage = true;
            }
        }

        if (requestedURI.indexOf("login_action.jsp") > -1 && authenticator != null) {
            return CarbonUILoginUtil.handleLogin(authenticator, request, response, session,
                    authenticated, contextPath, indexPageURL, httpLogin);
        } else if (requestedURI.indexOf("logout_action.jsp") > 1) {
            return CarbonUILoginUtil.handleLogout(authenticator, request, response, session,
                    authenticated, contextPath, indexPageURL, httpLogin);
        }

        if (requestedURI.endsWith("/carbon/")) {
            if (skipLoginPage) {
                response.sendRedirect(contextPath + indexPageURL + "?skipLoginPage=true");
            } else {
                response.sendRedirect(contextPath + indexPageURL);
            }
            return false;
        } else if (requestedURI.indexOf("/registry/atom") == -1 && requestedURI.endsWith("/carbon")) {
            if (skipLoginPage) {
                response.sendRedirect(contextPath + indexPageURL + "?skipLoginPage=true");
            } else {
                response.sendRedirect(contextPath + indexPageURL);
            }
            return false;
        } else if (CarbonUILoginUtil.letRequestedUrlIn(requestedURI, tempUrl)) {
            return true;
        } else if (requestedURI.endsWith(".jsp") && authenticated) {
            return true;
        }

        if (!authenticated) {
            if (requestedURI.endsWith("ajaxprocessor.jsp")) {
                // Prevent login page appearing
                return true;
            } else {
                return CarbonUILoginUtil.saveOriginalUrl(authenticator, request, response, session,
                        skipLoginPage, contextPath, indexPageURL, requestedURI);
            }
        }
        if (request.getSession().isNew()) {
            if (skipLoginPage) {
                response.sendRedirect(contextPath + "/carbon/admin/login_action.jsp");
            } else {
                response.sendRedirect(contextPath + "/carbon/admin/login.jsp");

            }
            return false;
        }
        return true;
    }

    /**
     * 
     * @param indexPageURL
     * @return
     */
    private String updateIndexPageWithHomePage(String indexPageURL) {
        // If the params in the servletcontext is null get them from the UTIL
        if (defaultHomePage == null) {
            defaultHomePage = (String) CarbonUIUtil
                    .getProductParam(CarbonConstants.PRODUCT_XML_WSO2CARBON
                            + CarbonConstants.DEFAULT_HOME_PAGE);
        }

        if (defaultHomePage != null && defaultHomePage.trim().length() > 0
                && indexPageURL.contains("/carbon/admin/index.jsp")) {
            indexPageURL = defaultHomePage;
            if (!indexPageURL.startsWith("/")) {
                indexPageURL = "/" + indexPageURL;
            }
        }

        return indexPageURL;
    }

    /**
     * 
     * @param request
     * @param session
     * @param resourceURI
     * @return
     */
    private boolean isCurrentUrlToBePassed(HttpServletRequest request, HttpSession session,
            String resourceURI) {

        if (!urlsToBeByPassed.isEmpty() && urlsToBeByPassed.containsKey(resourceURI)) {
            if (log.isDebugEnabled()) {
                log.debug("By passing authentication check for URI : " + resourceURI);
            }
            // Before bypassing, set the backendURL properly so that it doesn't fail
            String contextPath = request.getContextPath();
            String backendServerURL = request.getParameter("backendURL");
            if (backendServerURL == null) {
                backendServerURL = CarbonUIUtil.getServerURL(session.getServletContext(),
                        request.getSession());
            }
            if ("/".equals(contextPath)) {
                contextPath = "";
            }
            backendServerURL = backendServerURL.replace("${carbon.context}", contextPath);
            session.setAttribute(CarbonConstants.SERVER_URL, backendServerURL);
            return true;
        }

        return false;
    }

    /**
     * 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void populateUrlsToBeBypassed() {
        if (bundle != null && urlsToBeByPassed.isEmpty()) {
            ServiceReference reference = bundle.getBundleContext().getServiceReference(
                    CarbonUIDefinitions.class.getName());
            CarbonUIDefinitions carbonUIDefinitions;
            if (reference != null) {
                carbonUIDefinitions = (CarbonUIDefinitions) bundle.getBundleContext().getService(
                        reference);
                if (carbonUIDefinitions != null) {
                    urlsToBeByPassed = carbonUIDefinitions.getUnauthenticatedUrls();
                }
            }
        }
    }

    /**
     * 
     * @param requestedURI
     * @param request
     * @param response
     * @param authenticated
     * @param authenticator
     * @return
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    private int allowNonSecuredContent(String requestedURI, HttpServletRequest request,
            HttpServletResponse response, boolean authenticated, CarbonUIAuthenticator authenticator)
            throws IOException {
        if (!request.isSecure() && !(requestedURI.endsWith(".html"))) {

            // By passing items required for try-it & IDE plugins
            if (requestedURI.endsWith(".css") || requestedURI.endsWith(".gif")
                    || requestedURI.endsWith(".GIF") || requestedURI.endsWith(".jpg")
                    || requestedURI.endsWith(".JPG") || requestedURI.endsWith(".png")
                    || requestedURI.endsWith(".PNG") || requestedURI.endsWith(".xsl")
                    || requestedURI.endsWith(".xslt") || requestedURI.endsWith(".js")
                    || requestedURI.endsWith(".ico") || requestedURI.endsWith("/filedownload")
                    || requestedURI.endsWith("/fileupload")
                    || requestedURI.contains("/fileupload/")
                    || requestedURI.contains("admin/jsp/WSRequestXSSproxy_ajaxprocessor.jsp")
                    || requestedURI.contains("registry/atom")
                    || requestedURI.contains("registry/tags") || requestedURI.contains("gadgets/")
                    || requestedURI.contains("registry/resource")) {
                return CarbonUILoginUtil.RETURN_TRUE;
            }

            String resourceURI = requestedURI.replaceFirst("/carbon/", "../");

            // By passing the pages which are specified as bypass https
            if (httpUrlsToBeByPassed.containsKey(resourceURI)) {
                if (!authenticated) {
                    try {
                        Cookie[] cookies = request.getCookies();
                        if (cookies != null) {
                            for (Cookie cookie : cookies) {
                                if (cookie.getName().equals(CarbonConstants.REMEMBER_ME_COOKE_NAME)
                                        && authenticator != null) {
                                    try {
                                        authenticator.authenticateWithCookie(request);
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
                        log.error(e.getMessage(), e);
                        throw new IOException(e.getMessage(), e);
                    }
                }
                return CarbonUILoginUtil.RETURN_TRUE;
            }
            
            String enableHTTPAdminConsole = CarbonUIServiceComponent.getServerConfiguration()
                  .getFirstProperty(CarbonConstants.ENABLE_HTTP_ADMIN_CONSOLE);
            
            if (enableHTTPAdminConsole == null || "false".equalsIgnoreCase(enableHTTPAdminConsole.trim())) {
                String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
                if (adminConsoleURL != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Request came to admin console via http.Forwarding to : "
                                + adminConsoleURL);
                    }
                    response.sendRedirect(adminConsoleURL);
                    return CarbonUILoginUtil.RETURN_FALSE;
                }
            }
        }

        return CarbonUILoginUtil.CONTINUE;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    private boolean handleRequestOnContext(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
    	log.debug("Handling request on context");
        if (defaultContext != null && !"".equals(defaultContext.getContextName())
                && !"null".equals(defaultContext.getContextName())) {
            String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
            int index = adminConsoleURL.lastIndexOf("carbon");
            String defaultContextUrl = adminConsoleURL.substring(0, index)
                    + defaultContext.getContextName() + "/";
            response.sendRedirect(defaultContextUrl);
        } else {
            response.sendRedirect("carbon");
        }
        return false;
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    private void populatehttpUrlsToBeByPassed() {
        if (bundle != null && httpUrlsToBeByPassed.isEmpty() && defaultContext == null) {
            @SuppressWarnings("rawtypes")
            ServiceReference reference = bundle.getBundleContext().getServiceReference(
                    CarbonUIDefinitions.class.getName());
            CarbonUIDefinitions carbonUIDefinitions;
            if (reference != null) {
                carbonUIDefinitions = (CarbonUIDefinitions) bundle.getBundleContext().getService(
                        reference);
                if (carbonUIDefinitions != null) {
                    httpUrlsToBeByPassed = carbonUIDefinitions.getHttpUrls();
                    if (carbonUIDefinitions.getContexts().containsKey("default-context")) {
                        defaultContext = carbonUIDefinitions.getContexts().get("default-context");
                    }

                }
            }
        }
    }

}
