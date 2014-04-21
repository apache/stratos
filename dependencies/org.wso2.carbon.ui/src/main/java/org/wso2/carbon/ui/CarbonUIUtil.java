/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.ui.deployment.beans.CarbonUIDefinitions;
import org.wso2.carbon.ui.deployment.beans.Menu;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * Utility class for Carbon UI
 */
public class CarbonUIUtil {
    public static final String QUERY_PARAM_LOCALE = "locale";
    public static final String SESSION_PARAM_LOCALE = "custom_locale";
    private static Log log = LogFactory.getLog(CarbonUIUtil.class);
    private static BundleContext bundleContext = null;

    //To store the product specific params 
    private static HashMap productParams = new HashMap();

    /**
     * Get a proxy object to the business logic implementation class.
     * <p/>
     * This proxy could be a handle to an OSGi service or a Web services client
     *
     * @param clientClassObject Web services client
     * @param osgiObjectClass   OSGi service class
     * @param session           The HTTP Session
     * @return Proxy object
     * @deprecated Do not use this method. Simply use the relevant client.
     */
    public static Object getServerProxy(Object clientClassObject,
                                        Class osgiObjectClass,
                                        HttpSession session) {
        return clientClassObject;
    }

    public static void setBundleContext(BundleContext context) {
        bundleContext = context;
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static String getIndexPageURL(ServerConfigurationService serverConfig) {
        return serverConfig.getFirstProperty(CarbonConstants.INDEX_PAGE_URL);
    }

    public static String getIndexPageURL(ServletContext servletContext, HttpSession httpSession) {
        String url;
        Object obj = httpSession.getAttribute(CarbonConstants.INDEX_PAGE_URL);
        if (obj != null && obj instanceof String) {
            // Index Page URL is present in the servlet session
            url = (String) obj;
        } else {
            url = (String) servletContext.getAttribute(CarbonConstants.INDEX_PAGE_URL);
        }
        return url;
    }

    public static String getServerURL(ServerConfigurationService serverConfig) {
        ConfigurationContext serverCfgCtx =
                CarbonUIServiceComponent.getConfigurationContextService().getServerConfigContext();
        return CarbonUtils.getServerURL(serverConfig, serverCfgCtx);
    }

    public static String getServerURL(ServletContext servletContext, HttpSession httpSession) {
        return CarbonUtils.getServerURL(servletContext, httpSession,
                CarbonUIServiceComponent.
                        getConfigurationContextService().getServerConfigContext());
    }

    public static boolean isSuperTenant(HttpServletRequest request) {
        return request.getSession().getAttribute(MultitenantConstants.IS_SUPER_TENANT) != null &&
                request.getSession().getAttribute(MultitenantConstants.IS_SUPER_TENANT)
                        .equals(Boolean.toString(true));
    }

    public static String https2httpURL(String url) {
        if (url.indexOf("${carbon.https.port}") != -1) {
            String httpPort = CarbonUtils.getTransportPort(CarbonUIServiceComponent
                    .getConfigurationContextService(), "http")
                    + "";
            url = url.replace("${carbon.https.port}", httpPort);
        } else {
            // TODO: This is a hack to gaurd against the above if condition failing.
            // Need to dig into the root of the problem
            url = url.replace("https", "http");
            String httpsPort = CarbonUtils.getTransportPort(CarbonUIServiceComponent
                    .getConfigurationContextService(), "https")
                    + "";
            String httpPort = CarbonUtils.getTransportPort(CarbonUIServiceComponent
                    .getConfigurationContextService(), "http")
                    + "";
            url = url.replace(httpsPort, httpPort);
        }
        return url;
    }

    /**
     * Returns url to admin console. eg: https://192.168.1.201:9443/wso2/carbon
     *
     * @param request The HTTPServletRequest
     * @return The URL of the Admin Console
     */
    public static String getAdminConsoleURL(HttpServletRequest request) {

        // Hostname
        String hostName = "localhost";
        try {
            hostName = NetworkUtils.getMgtHostName();
        } catch (Exception ignored) {
        }

        // HTTPS port
        String mgtConsoleTransport = CarbonUtils.getManagementTransport();
        ConfigurationContextService configContextService = CarbonUIServiceComponent
            .getConfigurationContextService();
        int httpsPort = CarbonUtils.getTransportPort(configContextService, mgtConsoleTransport);
        int httpsProxyPort =
            CarbonUtils.getTransportProxyPort(configContextService.getServerConfigContext(),
                                              mgtConsoleTransport);

        // Context
        String context = request.getContextPath();
        if ("/".equals(context)) {
            context = "";
        }

        if (httpsPort == -1) {
            return null;
        }
        
        String adminConsoleURL = null;
        String enableHTTPAdminConsole = CarbonUIServiceComponent.getServerConfiguration()
                .getFirstProperty(CarbonConstants.ENABLE_HTTP_ADMIN_CONSOLE);

        if (enableHTTPAdminConsole != null
                && "true".equalsIgnoreCase(enableHTTPAdminConsole.trim())) {
            int httpPort = CarbonUtils.getTransportPort(
                    CarbonUIServiceComponent.getConfigurationContextService(), "http");
            adminConsoleURL = "http://" + hostName + ":" + httpPort + context + "/carbon/";
        } else {
            adminConsoleURL = "https://" + hostName + ":"
                    + (httpsProxyPort != -1 ? httpsProxyPort : httpsPort) + context + "/carbon/";
        }

        return adminConsoleURL;
    }

    /**
     * Returns url to admin console.
     *
     * @param context Webapp context root of the Carbon webapp
     * @return The URL of the Admin Console
     */
    public static String getAdminConsoleURL(String context) {
        // Hostname
        String hostName = "localhost";
        try {
            hostName = NetworkUtils.getMgtHostName();
        } catch (Exception ignored) {
        }

        // HTTPS port
        String mgtConsoleTransport = CarbonUtils.getManagementTransport();
        ConfigurationContextService configContextService = CarbonUIServiceComponent
            .getConfigurationContextService();
        int httpsPort = CarbonUtils.getTransportPort(configContextService, mgtConsoleTransport);
        int httpsProxyPort =
            CarbonUtils.getTransportProxyPort(configContextService.getServerConfigContext(),
                                              mgtConsoleTransport);
        // Context
        if ("/".equals(context)) {
            context = "";
        }
        return "https://" + hostName + ":" + (httpsProxyPort != -1? httpsProxyPort : httpsPort) +
            context + "/carbon/";
    }

    /**
     * Get a ServerConfiguration Property
     *
     * @param propertyName Name of the property
     * @return the property
     */
    public static String getServerConfigurationProperty(String propertyName) {
        try {
            ServerConfigurationService serverConfig = CarbonUIServiceComponent.getServerConfiguration();
            return serverConfig.getFirstProperty(propertyName);
        } catch (Exception e) {
            String msg = "ServerConfiguration Service not available";
            log.error(msg, e);
        }

        return null;
    }

    public static boolean isContextRegistered(ServletConfig config, String context) {
        URL url;
        try {
            url = config.getServletContext().getResource(context);
        } catch (MalformedURLException e) {
            return false;
        }
        if (url == null) {
            return false;
        } else if (url.toString().indexOf(context) != -1) {
            return true;
        }
        return false;
    }

    public static Locale toLocale(String localeQuery){
        String localeInfo[] = localeQuery.split("_");
        int size = localeInfo.length;
        Locale locale;
        switch (size){
            case 2:
                locale = new Locale(localeInfo[0],localeInfo[1]);
                break;
            case 3:
                locale = new Locale(localeInfo[0],localeInfo[1],localeInfo[2]);
                break;
            default:
                locale = new Locale(localeInfo[0]);
                break;
        }
        return locale;
    }

    public static void setLocaleToSession (HttpServletRequest request)
    {
        if (request.getParameter(QUERY_PARAM_LOCALE) != null) {
            request.getSession().setAttribute(CarbonUIUtil.SESSION_PARAM_LOCALE, request.getParameter(QUERY_PARAM_LOCALE));
        }
    }

    public static Locale getLocaleFromSession (HttpServletRequest request)
    {
        if (request.getSession().getAttribute(SESSION_PARAM_LOCALE) != null) {
            String custom_locale = request.getSession().getAttribute(SESSION_PARAM_LOCALE).toString();
            return toLocale(custom_locale);

        } else {
            return request.getLocale();
        }
    }

    /**
     * Returns internationalized string for supplied key.
     *
     * @param key        - key to look for
     * @param i18nBundle - resource bundle
     * @param language   - language
     * @return internationalized key value of key, if no value can be derived
     */
    public static String geti18nString(String key, String i18nBundle, String language) {
        Locale locale = new Locale(language);
        String text = geti18nString(key, i18nBundle, locale);
        return text;
    }

    /**
     * Returns internationalized string for supplied key.
     *
     * @param key        - key to look for
     * @param i18nBundle - resource bundle
     * @param locale     - locale
     * @return internationalized key value of key, if no value can be derived
     */
    public static String geti18nString(String key, String i18nBundle, Locale locale) {
        java.util.ResourceBundle resourceBundle = null;
        if (i18nBundle != null) {
            try {
                resourceBundle = java.util.ResourceBundle.getBundle(i18nBundle, locale);
            } catch (java.util.MissingResourceException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot find resource bundle : " + i18nBundle + " for locale : "
                                    + locale);
                }
            }
        }
        String text = key;
        if (resourceBundle != null) {
            String tmp = null;
            try {
                tmp = resourceBundle.getString(key);
            } catch (java.util.MissingResourceException e) {
                // Missing key should not be a blocking factor for UI rendering
                if (log.isDebugEnabled()) {
                    log.debug("Cannot find resource for key :" + key);
                }
            }
            if (tmp != null) {
                text = tmp;
            }
        }
        return text;
    }

    /**
     * Removed menu item from current user's session. Only current user's menu
     * items are effected.
     *
     * @param menuId
     * @param request
     * @see CarbonUIDefinitions#removeMenuDefinition(String)
     */
    public static void removeMenuDefinition(String menuId, HttpServletRequest request) {
        // TODO : consider removing child menu items as well
        ArrayList<Menu> modifiedMenuDefs = new ArrayList<Menu>();
        Menu[] currentMenus = (Menu[]) request.getSession().getAttribute(
                MenuAdminClient.USER_MENU_ITEMS);
        boolean modified = false;
        if (currentMenus != null) {
            if (menuId != null && menuId.trim().length() > 0) {
                for (int a = 0; a < currentMenus.length; a++) {
                    Menu menu = currentMenus[a];
                    if (menu != null) {
                        if (!menuId.equals(menu.getId())) {
                            modifiedMenuDefs.add(menu);
                            modified = true;
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Removing menu item : " + menuId);
                            }
                        }
                    }
                }
                if (modified) {
                    Menu[] newMenuDefs = new Menu[modifiedMenuDefs.size()];
                    newMenuDefs = modifiedMenuDefs.toArray(newMenuDefs);
                    request.getSession().setAttribute(MenuAdminClient.USER_MENU_ITEMS, newMenuDefs);
                }
            }
        }
    }

    public static String getBundleResourcePath(String resourceName) {
        if (resourceName == null || resourceName.length() == 0) {
            return null;
        }

        String resourcePath = resourceName;
        resourcePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        resourcePath = (resourcePath.lastIndexOf('/') != -1) ? resourcePath.substring(0,
                resourcePath.indexOf('/')) : resourcePath;

        return resourcePath;
    }

    /**
     * This method is a helper method for checking UI permissions.
     */
    @SuppressWarnings("unchecked")
    public static boolean isUserAuthorized(HttpServletRequest request, String resource) {
        boolean isAuthorized = false;
        List<String> permissions = (List<String>) request.getSession().getAttribute(
                CarbonConstants.UI_USER_PERMISSIONS);
        if (permissions == null) {
            return false;
        }

        for (String permission : permissions) {
            if (resource.startsWith(permission)) {
                isAuthorized = true;
                break;
            }
        }

        return isAuthorized;
    }

    /**
     * Method is used to retrive product xml params
     *
     * @param key = product xml key
     * @return product xml value
     */
    public static Object getProductParam(String key) {
        return productParams.get(key);
    }

    public static void setProductParam(String key, Object value) {
        productParams.put(key, value);
    }

    /**
     * Returns home page location for "Home" link in Carbon UI menu.
     * If defaultHomePage property is available in product.xml this method will return it and if not it'll return
     * default ../admin/index.jsp
     *
     * @return home page location
     */
    public static String getHomePage() {
        Object homePage;
        if ((homePage = getDefaultHomePageProductParam())
                != null) {
            String homePageLocation = (String) homePage;
            if (!homePageLocation.startsWith("/")) {
                // it is assumed that homepage location is provided as a relative path starting
                // from carbon context. This is to support the re-direction url at the login.
                // Therefore here we fix the location to suit the homepage link of the product.
                homePageLocation = "../../" + homePageLocation;
            }
            return homePageLocation;
        }

        return CarbonConstants.CARBON_UI_DEFAULT_HOME_PAGE;

    }
    
    public static String removeTenantSpecificStringsFromURL(String requestURL) {
		if (requestURL.contains("/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/")) {
			int tenantPrefixIndex = requestURL.lastIndexOf("/" +
			                                               MultitenantConstants.TENANT_AWARE_URL_PREFIX +
			                                               "/");
			requestURL = requestURL.substring(tenantPrefixIndex +
			                                  MultitenantConstants.TENANT_AWARE_URL_PREFIX.length() +
			                                  2);
			// bypassing tenantDomain part
			int pageUrlIndex = requestURL.indexOf('/');
			requestURL = requestURL.substring(pageUrlIndex);
		}
		return requestURL;
	}

    private static Object getDefaultHomePageProductParam() {
        return getProductParam(CarbonConstants.PRODUCT_XML_WSO2CARBON + CarbonConstants.DEFAULT_HOME_PAGE);
    }
}
