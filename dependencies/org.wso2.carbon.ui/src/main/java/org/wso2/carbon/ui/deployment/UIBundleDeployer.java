/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.ui.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.eclipse.equinox.http.helper.FilterServletAdaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.ui.BundleBasedUIResourceProvider;
import org.wso2.carbon.ui.deployment.beans.CarbonUIDefinitions;
import org.wso2.carbon.ui.deployment.beans.Component;
import org.wso2.carbon.ui.deployment.beans.CustomUIDefenitions;
import org.wso2.carbon.ui.deployment.beans.FileUploadExecutorConfig;
import org.wso2.carbon.ui.deployment.beans.Menu;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;
import org.wso2.carbon.ui.transports.fileupload.FileUploadExecutorManager;
import org.wso2.carbon.ui.util.UIResourceProvider;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class UIBundleDeployer implements SynchronousBundleListener {

    private static Log log = LogFactory.getLog(UIBundleDeployer.class);
    private String bundleResourcePath = "/web";
    private BundleContext bundleContext;
    private HttpContext httpContext;
    private ServiceTracker fileUploadExecManagerTracker;
    private BundleBasedUIResourceProvider bundleBasedUIResourceProvider =
            new BundleBasedUIResourceProvider(bundleResourcePath);
    /*
     * Used to hide the menus in the management console.
     */
    private ArrayList<String> hideMenuIds = new ArrayList<String>();

    public UIResourceProvider getBundleBasedUIResourcePrvider() {
        return bundleBasedUIResourceProvider;
    }

    public void deploy(BundleContext bundleContext, HttpContext context) {
        this.bundleContext = bundleContext;
        this.httpContext = context;

        fileUploadExecManagerTracker = new ServiceTracker(bundleContext,
                FileUploadExecutorManager.class.getName(), null);
        fileUploadExecManagerTracker.open();

        hideMenuIds.addAll(Arrays.asList(ServerConfiguration
                                         .getInstance().getProperties("HideMenuItemIds.HideMenuItemId")));
         
        //When Carbon starts up with existing set of bundles which contain component.xmls,
        //the bundleChanged() method does not get called. So calling processComponentXML()
        //method here seems to be the only solution.
        //TODO fork a new thread to do this task since this needs some processing time.
        for (Bundle bundle : bundleContext.getBundles()) {
            if ((bundle.getState() & (Bundle.UNINSTALLED | Bundle.INSTALLED)) == 0) {
                try {
                    processUIBundle(bundle, CarbonConstants.ADD_UI_COMPONENT);
                } catch (Exception e) {
                    log.error("Error occured when processing ui bundle " + bundle.getSymbolicName(), e);
                }
            }
        }

        try {
            bundleContext.addServiceListener(new ServletServiceListener(),
                    "(objectClass=" + Servlet.class.getName() + ")");
        } catch (InvalidSyntaxException e) {
            log.error(e);
        }
    }
    /*
    INSTALLED = 1;
    STARTED = 2;
    STOPPED = 4;
    UPDATED = 8;
    UNINSTALLED = 16;
    RESOLVED = 32;
    UNRESOLVED = 64;
    STARTING = 128;
    STOPPING = 256;
    LAZY_ACTIVATION = 512;
     */
    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if(log.isDebugEnabled()){
            log.debug("Received new bundle event  : " + event.getType());
        }
        try {
            switch (event.getType()) {
                //TODO need to fix here.
                case BundleEvent.RESOLVED:
                    if(log.isDebugEnabled()){
                        log.debug("Add ui component event received....");
                    }
                    processUIBundle(bundle, CarbonConstants.ADD_UI_COMPONENT);
                    break;

                case BundleEvent.UNRESOLVED:
                    if(log.isDebugEnabled()){
                    log.debug("Remove ui component event received....");
                    }
                    processUIBundle(bundle, CarbonConstants.REMOVE_UI_COMPONENT);
                    break;
            }
        } catch (Exception e) {
            log.error("Error occured when processing component xml in bundle " + bundle.getSymbolicName(), e);
            e.printStackTrace();
        }
    }

    /**
     * 1)Search for the UIBundle header
     * 2)Check for UI bundle fragments - for backward compatibility
     *
     * @param bundle
     * @param action
     * @throws CarbonException
     */
    private void processUIBundle(Bundle bundle, String action) throws CarbonException {
        Dictionary headers = bundle.getHeaders();

        String value = (String) headers.get("Carbon-Component");
        if (value != null && "UIBundle".equals(value)) { //this is a UI Bundle
            if (CarbonConstants.ADD_UI_COMPONENT.equals(action)) {
                if(log.isDebugEnabled()){
                    log.debug("UI component add action received in UIBundleDeployer  : "+action);
                }
                if(log.isDebugEnabled()){
                    log.debug("Adding bundle resource paths  : "+ bundle );
                }
                bundleBasedUIResourceProvider.addBundleResourcePaths(bundle);
                if(log.isDebugEnabled()){
                    log.debug("processComponentXML in   : "+ bundle +"   "+action);
                }
                processComponentXML(bundle, action);
            } else if (CarbonConstants.REMOVE_UI_COMPONENT.equals(action)){
                if(log.isDebugEnabled()){
                    log.debug("UI component add action received in UIBundleDeployer  : "+action);
                }
                if(log.isDebugEnabled()){
                    log.debug("Removing bundle resource paths  : "+ bundle );
                }
                bundleBasedUIResourceProvider.removeBundleResourcePaths(bundle);
                if(log.isDebugEnabled()){
                    log.debug("processComponentXML in   : "+ bundle +"   "+action);
                }
                processComponentXML(bundle, action);
            }

        }
    }

    /**
     * Read component.xml(if any) and invoke suitable actions
     *
     * @param bundle
     * @param action - used to determine if a menu item should be added or removed from UI. Value for action
     *               is determined by the bundle's lifecycle method(RESOLVED,...,etc).
     */
    private void processComponentXML(Bundle bundle, String action) throws CarbonException {
        if (bundleContext.getBundle() == bundle) {
            return;
        }
        if(log.isDebugEnabled()){
                    log.debug("Processing component xml in the bundle...");
                }
        Component component = ComponentBuilder.build(bundle, bundleContext);
        if (component == null) {
            return;
        } else {
            ServiceReference reference = bundleContext.getServiceReference(CarbonUIDefinitions.class.getName());
            CarbonUIDefinitions carbonUIDefinitions = null;
            if (reference != null) {
                carbonUIDefinitions = (CarbonUIDefinitions) bundleContext.getService(reference);
            }
            if (carbonUIDefinitions != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Found carbonUIDefinitions in OSGi context");
                }
                if (CarbonConstants.ADD_UI_COMPONENT.equals(action)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding UI component using existing Carbon Definition");
                    }
                    ArrayList<Menu> menusToAdd = new ArrayList<Menu>();
					for (Menu menu : component.getMenus()) {
						
						// Prevent adding the menu if it is defined to be hidden.
						if (!(hideMenuIds.contains(menu.getId()))) {
							menusToAdd.add(menu);
						}
					}
					if(menusToAdd.size() > 0) {
						Menu[] menus = new Menu[menusToAdd.size()];
						menus = menusToAdd.toArray(menus);
						carbonUIDefinitions.addMenuItems(menus);
					}

                    carbonUIDefinitions.addServletItems(component.getServlets());
                    carbonUIDefinitions.addUnauthenticatedUrls(component.getUnauthenticatedUrlList());
                    carbonUIDefinitions.addSkipTilesUrls(component.getSkipTilesUrlList());
                    carbonUIDefinitions.addHttpUrls(component.getSkipHttpsUrlList());
                    carbonUIDefinitions.addContexts(component.getContextsList());
                } else if (CarbonConstants.REMOVE_UI_COMPONENT.equals(action)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing UI component using existing carbon definition");
                    }
                    carbonUIDefinitions.removeMenuItems(component.getMenus());
                    carbonUIDefinitions.removeServletItems(component.getServlets());
                    carbonUIDefinitions.removeUnauthenticatedUrls(component.getUnauthenticatedUrlList());
                    carbonUIDefinitions.removeSkipTilesUrls(component.getSkipTilesUrlList());
                    carbonUIDefinitions.removeHttpUrls(component.getSkipHttpsUrlList());
                    carbonUIDefinitions.removeContexts(component.getContextsList());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("CarbonUIDefinitions is NULL. Registering new...");
                }
                carbonUIDefinitions = new CarbonUIDefinitions();
                carbonUIDefinitions.addMenuItems(component.getMenus());
                carbonUIDefinitions.addServletItems(component.getServlets());
                carbonUIDefinitions.addUnauthenticatedUrls(component.getUnauthenticatedUrlList());
                carbonUIDefinitions.addSkipTilesUrls(component.getSkipTilesUrlList());
                carbonUIDefinitions.addHttpUrls(component.getSkipHttpsUrlList());
                carbonUIDefinitions.addContexts(component.getContextsList());
                bundleContext.registerService(CarbonUIDefinitions.class.getName(), carbonUIDefinitions, null);
            }
        }
        //processing servlet definitions
        processServletDefinitions(component,action);
        // processing the custom UI definitions required by Registry
        processCustomRegistryDefinitions(component,action);
        //processing file upload executor entries in the UI component
        processFileUploadExecutorDefinitions(component,action);

    }



    private void processFileUploadExecutorDefinitions(Component component , String action) throws
            CarbonException{
        if (component.getFileUploadExecutorConfigs() != null
                && component.getFileUploadExecutorConfigs().length > 0) {
            FileUploadExecutorManager executorManager =
                    (FileUploadExecutorManager) fileUploadExecManagerTracker.getService();
            if (executorManager == null) {
                log.error("FileUploadExecutorManager service is not available");
                return;
            }
            FileUploadExecutorConfig[] executorConfigs = component.getFileUploadExecutorConfigs();
            for (FileUploadExecutorConfig executorConfig : executorConfigs) {
                String[] mappingActions = executorConfig.getMappingActionList();
                for (String mappingAction : mappingActions) {
                    if (CarbonConstants.ADD_UI_COMPONENT.equals(action)) {
                        executorManager.addExecutor(mappingAction,
                                executorConfig.getFUploadExecClass());
                    } else if (CarbonConstants.REMOVE_UI_COMPONENT.equals(action)) {
                        executorManager.removeExecutor(mappingAction);
                    }
                }
            }
        }
    }


    private void processServletDefinitions(Component component, String action) throws
            CarbonException{
        if (component != null
                && component.getServlets() != null
                && component.getServlets().length > 0) {
            HttpService httpService;
            try {
                httpService = CarbonUIServiceComponent.getHttpService();
            } catch (Exception e) {
                throw new CarbonException("An instance of HttpService is not available");
            }
            org.wso2.carbon.ui.deployment.beans.Servlet[] servletDefinitions = component.getServlets();
            for (int a = 0; a < servletDefinitions.length; a++) {
                org.wso2.carbon.ui.deployment.beans.Servlet servlet = servletDefinitions[a];
                if (CarbonConstants.ADD_UI_COMPONENT.equals(action)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Registering sevlet : " + servlet);
                    }
                    try {
                        Class clazz = Class.forName(servlet.getServletClass());
                        //TODO : allow servlet parameters to be passed
                        Dictionary params = new Hashtable();
                        httpService.registerServlet(servlet.getUrlPatten(),
                                (Servlet) clazz.newInstance(), params,
                                httpContext);
                    } catch (ClassNotFoundException e) {
                        log.error("Servlet class : " + servlet.getServletClass() + " not found.", e);
                    } catch (ServletException e) {
                        log.error("Problem registering Servlet class : " +
                                servlet.getServletClass() + ".", e);
                    } catch (NamespaceException e) {
                        log.error("Problem registering Servlet class : " +
                                servlet.getServletClass() + ".", e);
                    } catch (InstantiationException e) {
                        log.error("Problem registering Servlet class : " +
                                servlet.getServletClass() + ".", e);
                    } catch (IllegalAccessException e) {
                        log.error("Problem registering Servlet class : " +
                                servlet.getServletClass() + ".", e);
                    }
                } else if (CarbonConstants.REMOVE_UI_COMPONENT.equals(action)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Unregistering sevlet : " + servlet);
                    }
                    httpService.unregister(servlet.getUrlPatten());
                }
            }

        }


    }

    private void processCustomRegistryDefinitions(Component component, String action) throws
            CarbonException{
          if (component != null) {

            ServiceReference reference =
                    bundleContext.getServiceReference(CustomUIDefenitions.class.getName());
            CustomUIDefenitions customUIDefinitions = null;
            if (reference != null) {
                customUIDefinitions = (CustomUIDefenitions) bundleContext.getService(reference);
            }
            if (customUIDefinitions == null) {
                String msg = "Custom UI defenitions service is not available.";
                log.error(msg);
                throw new CarbonException(msg);
            }

            Iterator<String> viewMediaTypes = component.getCustomViewUIMap().keySet().iterator();
            while (viewMediaTypes.hasNext()) {
                String mediaType = viewMediaTypes.next();
                String uiPath = component.getCustomViewUIMap().get(mediaType);
                if (CarbonConstants.ADD_UI_COMPONENT.equals(action)) {
                    if (customUIDefinitions.getCustomViewUI(mediaType) == null) {
                        customUIDefinitions.addCustomViewUI(mediaType, uiPath);
                        if (log.isDebugEnabled()) {
                            log.debug("Registered the custom view UI media type: " + mediaType + ", UI path: " + uiPath);
                        }
                    } else {
                        String msg = "Custom view UI is already registered for media type: " + mediaType +
                                ". Custom UI with media type: " + mediaType + " and UI path: " +
                                uiPath + " will not be registered.";
                        log.error(msg);
                    }
                }else if (CarbonConstants.REMOVE_UI_COMPONENT.equals(action)){
                    //TODO
                }

            }

            Iterator<String> addMediaTypes = component.getCustomAddUIMap().keySet().iterator();
            while (addMediaTypes.hasNext()) {
                String mediaType = addMediaTypes.next();
                String uiPath = component.getCustomAddUIMap().get(mediaType);
                if (CarbonConstants.ADD_UI_COMPONENT.equals(action)) {
                    if (customUIDefinitions.getCustomAddUI(mediaType) == null) {
                        customUIDefinitions.addCustomAddUI(mediaType, uiPath);
                        if (log.isDebugEnabled()) {
                            log.debug("Registered the custom add UI media type: " + mediaType + ", UI path: " + uiPath);
                        }
                    } else {
                        String msg = "Custom add UI is already registered for media type: " + mediaType +
                                ". Custom UI with media type: " + mediaType + " and UI path: " +
                                uiPath + " will not be registered.";
                        log.error(msg);
                    }
                } else if (CarbonConstants.REMOVE_UI_COMPONENT.equals(action)) {
                    //TODO

                }
            }
        }
    }

    public void registerServlet(Servlet servlet, String urlPattern, Dictionary params,
                                Dictionary servletAttrs, int event, javax.servlet.Filter associatedFilter) throws CarbonException {

        HttpService httpService;
        try {
            httpService = CarbonUIServiceComponent.getHttpService();
        } catch (Exception e) {
            throw new CarbonException("An instance of HttpService is not available");
        }
        try {
            if (event == ServiceEvent.REGISTERED) {
                Servlet adaptedJspServlet = new ContextPathServletAdaptor(servlet, urlPattern);
                if (associatedFilter == null) {
                    httpService.registerServlet(urlPattern, adaptedJspServlet, params, httpContext);
                } else {
                    httpService.registerServlet(urlPattern,
                            new FilterServletAdaptor(associatedFilter, null, adaptedJspServlet), params, httpContext);
                }
                if (servletAttrs != null) {
                    for (Enumeration enm = servletAttrs.keys(); enm.hasMoreElements();) {
                        String key = (String) enm.nextElement();
                        adaptedJspServlet.getServletConfig().getServletContext().setAttribute(key, servletAttrs.get(key));
                    }
                }
            } else if (event == ServiceEvent.UNREGISTERING) {
                httpService.unregister(urlPattern);
            }

        } catch (Exception e) {
            log.error("Error occurred while registering servlet",e);
        }
    }

    public class ServletServiceListener implements ServiceListener {

        public ServletServiceListener() {
            try {
                ServiceReference[] servletSRs = bundleContext.getServiceReferences((String)null,
                        "(objectClass=" + Servlet.class.getName() + ")");

                if (servletSRs != null) {
                    for (ServiceReference sr : servletSRs) {
                        registerServletFromSR(sr, ServiceEvent.REGISTERED);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to obtain registerd services. Invalid filter Syntax.", e);
            }
        }

        public void serviceChanged(ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED) {
                registerServletFromSR(event.getServiceReference(), event.getType());
            }
        }

        public void registerServletFromSR(ServiceReference sr, int event) {
            Servlet servlet = (Servlet) bundleContext.getService(sr);
            if (servlet == null) {
                log.error("Servlet instance cannot be null");
                return;
            }

            Object urlPatternObj = sr.getProperty(CarbonConstants.SERVLET_URL_PATTERN);
            if (urlPatternObj == null || !(urlPatternObj instanceof String) || urlPatternObj.equals("")) {
                log.error("URL pattern should not be null");
                return;
            }
            String urlPattern = (String) urlPatternObj;

            Object paramsObj = sr.getProperty(CarbonConstants.SERVLET_PARAMS);
            if (paramsObj != null && !(paramsObj instanceof Dictionary)) {
                log.error("Servlet params instances should be type of Dictionary");
                return;
            }
            Dictionary params = (Dictionary) paramsObj;

            Object attributesObj = sr.getProperty(CarbonConstants.SERVLET_ATTRIBUTES);
            if (attributesObj != null && !(attributesObj instanceof Dictionary)) {
                log.error("Servlet attributes instances should be type of Dictionary");
                return;
            }
            Dictionary attributes = (Dictionary) attributesObj;

            Object associatedFilterObj = sr.getProperty(CarbonConstants.ASSOCIATED_FILTER);

            // use the qualified name for the Filter as it will by default conflicted with the OSGI class
            javax.servlet.Filter associatedFilter = null;
            if (associatedFilterObj != null) {
                associatedFilter = (javax.servlet.Filter) associatedFilterObj;
            }

            try {
                registerServlet(servlet, urlPattern, params, attributes, event, associatedFilter);
            } catch (CarbonException e) {
                log.error(e);
            }
        }
    }
}
