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
package org.wso2.carbon.ui.deployment;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.ui.deployment.beans.Component;
import org.wso2.carbon.ui.deployment.beans.Context;
import org.wso2.carbon.ui.deployment.beans.FileUploadExecutorConfig;
import org.wso2.carbon.ui.deployment.beans.Menu;
import org.wso2.carbon.ui.deployment.beans.Servlet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.wso2.carbon.CarbonConstants.AUTHENTICATION;
import static org.wso2.carbon.CarbonConstants.BYPASS;
import static org.wso2.carbon.CarbonConstants.CONTEXT;
import static org.wso2.carbon.CarbonConstants.CONTEXTS;
import static org.wso2.carbon.CarbonConstants.CONTEXT_ID;
import static org.wso2.carbon.CarbonConstants.CONTEXT_NAME;
import static org.wso2.carbon.CarbonConstants.DESCRIPTION;
import static org.wso2.carbon.CarbonConstants.FRAMEWORK_CONFIG;
import static org.wso2.carbon.CarbonConstants.HTTP_URLS;
import static org.wso2.carbon.CarbonConstants.LINK;
import static org.wso2.carbon.CarbonConstants.MENUE_ELE;
import static org.wso2.carbon.CarbonConstants.MENUS_ELE;
import static org.wso2.carbon.CarbonConstants.PROTOCOL;
import static org.wso2.carbon.CarbonConstants.REQUIRE_NOT_LOGGED_IN;
import static org.wso2.carbon.CarbonConstants.REQUIRE_NOT_SUPER_TENANT;
import static org.wso2.carbon.CarbonConstants.REQUIRE_PERMISSION;
import static org.wso2.carbon.CarbonConstants.REQUIRE_SUPER_TENANT;
import static org.wso2.carbon.CarbonConstants.SERVLET;
import static org.wso2.carbon.CarbonConstants.SERVLETS;
import static org.wso2.carbon.CarbonConstants.SERVLET_CLASS;
import static org.wso2.carbon.CarbonConstants.SERVLET_DISPLAY_NAME;
import static org.wso2.carbon.CarbonConstants.SERVLET_ID;
import static org.wso2.carbon.CarbonConstants.SERVLET_NAME;
import static org.wso2.carbon.CarbonConstants.SERVLET_URL_PATTERN;
import static org.wso2.carbon.CarbonConstants.TILES;
import static org.wso2.carbon.CarbonConstants.WSO2CARBON_NS;

/**
 * XML Builder for building the UI related portions of the component.xml file included
 * in Carbon components
 */
public final class ComponentBuilder {
    private static Log log = LogFactory.getLog(ComponentBuilder.class);

    private ComponentBuilder() {
    }

    /*TODO : use registry to store this */
//    private static Map<String, Action> actionMap = new HashMap<String, Action>();

    /**
     * reads component.xml from given bundle & returns an object representation  of it
     *
     * @param registeredBundle The bundle that is being registered
     * @param bundleContext    The bundle context of the UI bundles
     * @return Component
     */
    public static Component build(Bundle registeredBundle, BundleContext bundleContext) {
        Component component = null;
        Dictionary headers = registeredBundle.getHeaders();
        try {
            URL url = registeredBundle.getEntry("META-INF/component.xml");
            if (url != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Found component.xml in bundle : " + registeredBundle.getSymbolicName());
                }
                //found a Carbon OSGi bundle that should amend for admin UI
                String bundleVersion = (String) headers.get("Bundle-Version");
                String bundleName = (String) headers.get("Bundle-Name");
                InputStream inputStream = url.openStream();
                component = build(inputStream, bundleName, bundleVersion,
                        bundleContext);
            }
        } catch (Exception e) {
            log.error("Cannot build component.xml for " + registeredBundle.getSymbolicName(), e);
        }
        return component;
    }


    public static Component build(InputStream componentIn,
                                  String componentName,
                                  String componentVersion,
                                  BundleContext bundleContext) throws CarbonException,
            XMLStreamException {

        XMLStreamReader streamReader =
                XMLInputFactory.newInstance().createXMLStreamReader(componentIn);
        StAXOMBuilder builder = new StAXOMBuilder(streamReader);
        OMElement document = builder.getDocumentElement();
        Component component = new Component();
        component.setName(componentName);
        component.setVersion(componentVersion);

        processMenus(componentName, document, component);
        processServlets(document, component);
        processFileUploadConfigs(document, component);
        processCustomUIs(document, component);
        processOSGiServices(document, bundleContext);
        processFrameworkConfiguration(document, component);
        processContextConfiguration(componentName, document, component);

        return component;
    }

    /**
     * Processes the following XML segment
     * <p/>
     * <osgiServices>
     * <service>
     * <classes>
     * <class>org.wso2.carbon.ui.UIExtender</class>
     * </classes>
     * <object>
     * org.wso2.carbon.service.mgt.ui.ServiceManagementUIExtender
     * </object>
     * <properties>
     * <property name="service-mgt">true</property>
     * </properties>
     * </service>
     * </osgiServices>
     *
     * @param document      The document
     * @param bundleContext The OSGi bundle context
     * @throws CarbonException If an error occurs while instantiating a service object
     */
    private static void processOSGiServices(OMElement document,
                                            BundleContext bundleContext) throws CarbonException {
        OMElement osgiServiceEle =
                document.getFirstChildWithName(new QName(WSO2CARBON_NS, "osgiServices"));
        if (osgiServiceEle == null) {
            return;
        }
        for (Iterator services =
                osgiServiceEle.getChildrenWithName(new QName(WSO2CARBON_NS, "service"));
             services.hasNext();) {
            OMElement service = (OMElement) services.next();
            OMElement objectEle =
                    service.getFirstChildWithName(new QName(WSO2CARBON_NS, "object"));
            Object obj;
            String objClazz = objectEle.getText().trim();
            try {
                Class objectClazz = Class.forName(objClazz, true, ComponentBuilder.class.getClassLoader());
                obj = objectClazz.newInstance();
            } catch (Exception e) {
                String msg = "Cannot instantiate OSGi service class " + objClazz;
                log.error(msg, e);
                throw new CarbonException(msg, e);
            }

            OMElement classesEle =
                    service.getFirstChildWithName(new QName(WSO2CARBON_NS, "classes"));
            List<String> classList = new ArrayList<String>();
            for (Iterator classes = classesEle.getChildElements(); classes.hasNext();) {
                OMElement clazz = (OMElement) classes.next();
                classList.add(clazz.getText().trim());
            }

            OMElement propertiesEle =
                    service.getFirstChildWithName(new QName(WSO2CARBON_NS, "properties"));
            Dictionary<String, String> props = new Hashtable<String, String>();
            for (Iterator properties = propertiesEle.getChildElements(); properties.hasNext();) {
                OMElement prop = (OMElement) properties.next();
                props.put(prop.getAttribute(new QName("name")).getAttributeValue().trim(),
                        prop.getText().trim());
            }
            bundleContext.registerService(classList.toArray(new String[classList.size()]),
                    obj, props);

            if (log.isDebugEnabled()) {
                log.debug("Registered OSGi service " + objClazz);
            }
        }
    }

    public static void processCustomUIs(OMElement document, Component component) {
        Iterator customUIElements =
                document.getChildrenWithName(new QName(WSO2CARBON_NS, "customUI"));
        while (customUIElements.hasNext()) {

            OMElement customUIElement = (OMElement) customUIElements.next();

            OMElement uiTypeElement =
                    customUIElement.getFirstChildWithName(new QName(WSO2CARBON_NS, "uiType"));
            String type = (uiTypeElement != null) ? uiTypeElement.getText() : "view";

            OMElement mediaTypeElement =
                    customUIElement.getFirstChildWithName(new QName(WSO2CARBON_NS, "mediaType"));
            String mediaType = (mediaTypeElement != null) ? mediaTypeElement.getText() : null;

            OMElement uiPathElement =
                    customUIElement.getFirstChildWithName(new QName(WSO2CARBON_NS, "uiPath"));
            String uiPath = (uiPathElement != null) ? uiPathElement.getText() : null;

            if (log.isDebugEnabled()) {
                log.debug("Read the custom UI configuration. Media type: " +
                        mediaType + ", UI path: " + uiPath + ", Type: " + type);
            }

            if (mediaType != null && uiPath != null) {
                if ("view".equals(type)) {
                    component.addCustomViewUI(mediaType, uiPath);
                } else if ("add".equals(type)) {
                    component.addCustomAddUI(mediaType, uiPath);
                } else {
                    String msg = "Unknown custom UI type for media type " + mediaType + " and UI path " +
                            uiPath +
                            ". This custom UI will not be enabled. Custom UI type should be 'view' or 'add'.";
                    log.error(msg);
                }
            } else {
                String msg = "Required information missing in custom UI configuration. " +
                        "Media type and UI path should contain a valid value.";
                log.error(msg);
            }
        }
    }

    private static void processFileUploadConfigs(OMElement document,
                                                 Component component) throws CarbonException {
        OMElement fileUploadConfigElement =
                document.getFirstChildWithName(new QName(CarbonConstants.WSO2CARBON_NS,
                        CarbonConstants.FILE_UPLOAD_CONFIG));
        if (fileUploadConfigElement != null) {

//            //Getting ConfigurationContext service
//            ServiceReference configCtxServiceRef = UIBundleDeployer.getBundleContext().getServiceReference(
//                    ConfigurationContextService.class.getName());
//            if (configCtxServiceRef == null) {
//                throw new CarbonException("ConfigurationContext Service is not found");
//            }
//            ConfigurationContext configContext = ((ConfigurationContextService)
//                    UIBundleDeployer.getBundleContext().getService(configCtxServiceRef)).getServerConfigContext();

//            //Getting FileUploadExecutorManager service
//            ServiceReference executorManagerServiceRef = bundle.getBundleContext().getServiceReference(
//                    ConfigurationContext.class.getName());
//            if (executorManagerServiceRef == null) {
//                throw new CarbonException("FileUploadExecutorManager Service is not found");
//            }
//
//            FileUploadExecutorManager executorManager = ((FileUploadExecutorManager) bundle.getBundleContext().getService(
//                    executorManagerServiceRef));

            for (Iterator iterator = fileUploadConfigElement.getChildElements(); iterator.hasNext();) {
                OMElement mapppingElement = (OMElement) iterator.next();
                if (mapppingElement.getLocalName().equalsIgnoreCase("Mapping")) {
                    OMElement actionsElement =
                            mapppingElement.getFirstChildWithName(
                                    new QName(CarbonConstants.WSO2CARBON_NS, CarbonConstants.ACTIONS));

                    if (actionsElement == null) {
                        String msg = "The mandatory FileUploadConfig/Actions entry " +
                                "does not exist or is empty in the CARBON_HOME/conf/carbon.xml " +
                                "file. Please fix this error in the  carbon.xml file and restart.";
                        log.error(msg);
                        throw new CarbonException(msg);
                    }
                    Iterator actionElementIterator =
                            actionsElement.getChildrenWithName(
                                    new QName(CarbonConstants.WSO2CARBON_NS, CarbonConstants.ACTION));

                    if (!actionElementIterator.hasNext()) {
                        String msg = "A FileUploadConfig/Mapping entry in the " +
                                "CARBON_HOME/conf/carbon.xml should have at least on Action " +
                                "defined. Please fix this error in the carbon.xml file and " +
                                "restart.";
                        log.error(msg);
                        throw new CarbonException(msg);
                    }

                    OMElement classElement = mapppingElement.getFirstChildWithName(
                            new QName(CarbonConstants.WSO2CARBON_NS, CarbonConstants.CLASS));

                    if (classElement == null || classElement.getText() == null) {
                        String msg = "The mandatory FileUploadConfig/Mapping/Class entry " +
                                "does not exist or is empty in the CARBON_HOME/conf/carbon.xml " +
                                "file. Please fix this error in the  carbon.xml file and restart.";
                        log.error(msg);
                        throw new CarbonException(msg);
                    }

                    FileUploadExecutorConfig executorConfig = new FileUploadExecutorConfig();
                    String className = classElement.getText().trim();
                    executorConfig.setFUploadExecClass(className);
//                    try {
//                        Class clazz = bundle.loadClass(className);
//                        Constructor constructor =
//                                clazz.getConstructor(new Class[]{ConfigurationContext.class});
//                        object = (FileUploadExecutor)constructor
//                                .newInstance(new Object[]{configContext});
//                        executorConfig.setFUploadExecObject(object);
//                    } catch (Exception e) {
//                        String msg = "Error occurred while trying to instantiate the " + className +
//                                " class specified as a FileUploadConfig/Mapping/class element in " +
//                                "the CARBON_HOME/conf/carbon.xml file. Please fix this error in " +
//                                "the carbon.xml file and restart.";
//                        log.error(msg, e);
//                        throw new CarbonException(msg, e);
//                    }

                    while (actionElementIterator.hasNext()) {
                        OMElement actionElement = (OMElement) actionElementIterator.next();
                        if (actionElement.getText() == null) {
                            String msg = "A FileUploadConfig/Mapping/Actions/Action element in the " +
                                    "CARBON_HOME/conf/carbon.xml file is empty. Please include " +
                                    "the correct value in this file and restart.";
                            log.error(msg);
                            throw new CarbonException(msg);
                        }
                        executorConfig.addMappingAction(actionElement.getText().trim());
                    }
                    component.addFileUploadExecutorConfig(executorConfig);
                }
            }
        }
    }

    private static void processServlets(OMElement document,
                                        Component component) {
        //Reading servlet definitions
        OMElement servletsEle =
                document.getFirstChildWithName(new QName(WSO2CARBON_NS, SERVLETS));
        if (servletsEle != null) {
            for (Iterator iterator =
                    servletsEle.getChildrenWithName(new QName(WSO2CARBON_NS, SERVLET));
                 iterator.hasNext();) {

                OMElement servletEle = (OMElement) iterator.next();
                Servlet servlet = new Servlet();

                OMAttribute attrib = servletEle.getAttribute(new QName(SERVLET_ID));
                if (attrib != null) {
                    servlet.setId(attrib.getAttributeValue());
                }


                Iterator nameEles =
                        servletEle.getChildrenWithName(new QName(WSO2CARBON_NS, SERVLET_NAME));
                if (nameEles.hasNext()) {
                    OMElement nameEle = (OMElement) nameEles.next();
                    servlet.setName(nameEle.getText());
                }
                Iterator displayNameEles =
                        servletEle.getChildrenWithName(new QName(WSO2CARBON_NS, SERVLET_DISPLAY_NAME));
                if (displayNameEles.hasNext()) {
                    OMElement displayNameEle = (OMElement) displayNameEles.next();
                    servlet.setDisplayName(displayNameEle.getText().trim());
                }
                Iterator servletClassEles =
                        servletEle.getChildrenWithName(new QName(WSO2CARBON_NS, SERVLET_CLASS));
                if (servletClassEles.hasNext()) {
                    OMElement servletClassEle = (OMElement) servletClassEles.next();
                    servlet.setServletClass(servletClassEle.getText().trim());
                }
                Iterator urlPatternEles =
                        servletEle.getChildrenWithName(new QName(WSO2CARBON_NS, SERVLET_URL_PATTERN));
                if (urlPatternEles.hasNext()) {
                    OMElement urlPatternEle = (OMElement) urlPatternEles.next();
                    servlet.setUrlPatten(urlPatternEle.getText().trim());
                }
                component.addServlet(servlet);
            }

        }
    }

    private static void processFrameworkConfiguration(OMElement document, Component component) {
        OMElement bypassesEle =
                document.getFirstChildWithName(new QName(WSO2CARBON_NS, FRAMEWORK_CONFIG));

        if (bypassesEle != null) {
            for (Iterator iterator =
                    bypassesEle
                            .getChildrenWithName(new QName(WSO2CARBON_NS, BYPASS));
                 iterator.hasNext();) {

                OMElement bypassEle = (OMElement) iterator.next();
                Iterator requireAuthenticationEles =
                        bypassEle.getChildrenWithName(
                                new QName(WSO2CARBON_NS, AUTHENTICATION));
                if (requireAuthenticationEles.hasNext()) {
                    OMElement skipAuthElement = (OMElement) requireAuthenticationEles.next();
                    Iterator skipLinkEles =
                            skipAuthElement
                                    .getChildrenWithName(new QName(WSO2CARBON_NS, LINK));

                    while (skipLinkEles.hasNext()) {
                        OMElement skipLinkElement = (OMElement) skipLinkEles.next();
                        if (skipLinkElement.getLocalName().equalsIgnoreCase(LINK)) {
                            if (skipLinkElement.getText() != null) {
                                component.addUnauthenticatedUrl(skipLinkElement.getText());
                            }
                        }
                    }
                }

                Iterator requireSkipTilesEles =
                        bypassEle.getChildrenWithName(new QName(WSO2CARBON_NS, TILES));
                if (requireSkipTilesEles.hasNext()) {
                    OMElement skipTilesElement = (OMElement) requireSkipTilesEles.next();
                    Iterator skipLinkEles =
                            skipTilesElement
                                    .getChildrenWithName(new QName(WSO2CARBON_NS, LINK));
                    while (skipLinkEles.hasNext()) {
                        OMElement skipLinkElement = (OMElement) skipLinkEles.next();
                        if (skipLinkElement.getLocalName().equalsIgnoreCase(LINK)) {
                            if (skipLinkElement.getText() != null) {
                                component.addSkipTilesUrl(skipLinkElement.getText());
                            }
                        }
                    }
                }

                Iterator requireSkipHttpUrlEles =
                        bypassEle.getChildrenWithName(new QName(WSO2CARBON_NS, HTTP_URLS));
                if (requireSkipHttpUrlEles.hasNext()) {
                    OMElement SkipHttpUrlElement = (OMElement) requireSkipHttpUrlEles.next();
                    Iterator skipLinkEles =
                            SkipHttpUrlElement
                                    .getChildrenWithName(new QName(WSO2CARBON_NS, LINK));
                    while (skipLinkEles.hasNext()) {
                        OMElement skipLinkElement = (OMElement) skipLinkEles.next();
                        if (skipLinkElement.getLocalName().equalsIgnoreCase(LINK)) {
                            if (skipLinkElement.getText() != null) {
                                component.addSkipHttpsUrlList(skipLinkElement.getText());
                            }
                        }
                    }
                }
            }
        }


    }

    private static void processContextConfiguration(String componentName, OMElement document, Component component) {
        OMElement contextsEle =
                document.getFirstChildWithName(new QName(WSO2CARBON_NS, CONTEXTS));
        if (contextsEle != null) {
            for (Iterator iterator =
                    contextsEle.getChildrenWithName(new QName(WSO2CARBON_NS, CONTEXT));
                 iterator.hasNext();) {
                OMElement contextEle = (OMElement) iterator.next();

                Context context = new Context();

                Iterator contextIdEles = contextEle.getChildrenWithName(new QName(WSO2CARBON_NS, CONTEXT_ID));
                if (contextIdEles.hasNext()) {
                    OMElement idEle = (OMElement) contextIdEles.next();
                    context.setContextId(idEle.getText());
                } else {
                    //found context without an Id
                    log.warn(componentName + " contains a component.xml with empty context id");
                }

                Iterator contextNameEles = contextEle.getChildrenWithName(new QName(WSO2CARBON_NS, CONTEXT_NAME));
                if (contextNameEles.hasNext()) {
                    OMElement nameEle = (OMElement) contextNameEles.next();
                    context.setContextName(nameEle.getText());
                } else {
                    //found context without a context name
                    log.warn(componentName + " contains a component.xml with empty context name");
                }

                Iterator contextProtocolEles = contextEle.getChildrenWithName(new QName(WSO2CARBON_NS, PROTOCOL));
                if (contextProtocolEles.hasNext()) {
                    OMElement protocolEle = (OMElement) contextProtocolEles.next();
                    context.setProtocol(protocolEle.getText());
                } else {
                    //found context without a context name
                    log.warn(componentName + " contains a component.xml with empty protocol");
                }

                Iterator contextDescEles = contextEle.getChildrenWithName(new QName(WSO2CARBON_NS, DESCRIPTION));
                if (contextDescEles.hasNext()) {
                    OMElement descEle = (OMElement) contextDescEles.next();
                    context.setDescription(descEle.getText());
                } else {
                    //found context without a context description
                    log.warn(componentName + " contains a component.xml with empty context description");
                }

                component.addContext(context);

            }
        }

    }

    public static void processMenus(String componentName, OMElement document,
                                     Component component) {
        OMElement menusEle =
                document.getFirstChildWithName(new QName(WSO2CARBON_NS, MENUS_ELE));
        if (menusEle != null) {
            for (Iterator iterator =
                    menusEle.getChildrenWithName(new QName(WSO2CARBON_NS, MENUE_ELE));
                 iterator.hasNext();) {
                OMElement menuEle = (OMElement) iterator.next();

                Menu menu = new Menu();

                Iterator idEles = menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "id"));
                if (idEles.hasNext()) {
                    OMElement idEle = (OMElement) idEles.next();
                    menu.setId(idEle.getText());
                } else {
                    //found menu without an Id
                    log.warn(componentName + " contains a component.xml with empty menu id");
                }
                Iterator i18nKeyEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "i18n-key"));
                if (i18nKeyEles.hasNext()) {
                    OMElement i18nKeyEle = (OMElement) i18nKeyEles.next();
                    menu.setI18nKey(i18nKeyEle.getText());
                }
                Iterator i18nBundleEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "i18n-bundle"));
                if (i18nBundleEles.hasNext()) {
                    OMElement i18nBundleEle = (OMElement) i18nBundleEles.next();
                    menu.setI18nBundle(i18nBundleEle.getText());
                }
                Iterator parentMenuEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "parent-menu"));
                if (parentMenuEles.hasNext()) {
                    OMElement parentMenuEle = (OMElement) parentMenuEles.next();
                    menu.setParentMenu(parentMenuEle.getText());
                }
                Iterator regionEles = menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "region"));
                if (regionEles.hasNext()) {
                    OMElement regionEle = (OMElement) regionEles.next();
                    menu.setRegion(regionEle.getText());
                }
                Iterator iconEles = menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "icon"));
                if (iconEles.hasNext()) {
                    OMElement iconEle = (OMElement) iconEles.next();
                    menu.setIcon(iconEle.getText());
                }
                Iterator linkEles = menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "link"));
                if (linkEles.hasNext()) {
                    OMElement linkEle = (OMElement) linkEles.next();
                    menu.setLink(linkEle.getText());
                }
                Iterator orderEles = menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "order"));
                if (orderEles.hasNext()) {
                    OMElement orderEle = (OMElement) orderEles.next();
                    menu.setOrder(orderEle.getText());
                }
                Iterator styleClassEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "style-class"));
                if (styleClassEles.hasNext()) {
                    OMElement styleEle = (OMElement) styleClassEles.next();
                    menu.setStyleClass(styleEle.getText());
                }

                Iterator requireAuthenticationEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "skip-authentication"));
                if (requireAuthenticationEles.hasNext()) {
                    menu.setRequireAuthentication(false);
                    component.addUnauthenticatedUrl(menu.getLink());

                    OMElement skipAuthElement = (OMElement) requireAuthenticationEles.next();
                    Iterator skipLinkEles =
                            skipAuthElement.getChildrenWithName(new QName(WSO2CARBON_NS, "skip-link"));

                    while (skipLinkEles.hasNext()) {
                        OMElement skipLinkElement = (OMElement) skipLinkEles.next();
                        if (skipLinkElement.getLocalName().equalsIgnoreCase("skip-link")) {
                            if (skipLinkElement.getText() != null) {
                                component.addUnauthenticatedUrl(skipLinkElement.getText());
                            }
                        }
                    }
                }

                Iterator requirePermissionElements =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, REQUIRE_PERMISSION));
                List<String> permissions = new LinkedList<String>();
                while (requirePermissionElements.hasNext()) {
                    OMElement permissionEle = (OMElement) requirePermissionElements.next();
                    permissions.add(permissionEle.getText());
                }
                if (permissions.size() > 0) {
                    menu.setRequirePermission(permissions.toArray(new String[permissions.size()]));
                } else {
                    Iterator permissionElements =
                            menuEle.getChildrenWithName(
                                    new QName(WSO2CARBON_NS, "all"));
                    if (permissionElements.hasNext()) {
                        menu.setAllPermissionsRequired(true);
                    } else {
                        permissionElements =
                            menuEle.getChildrenWithName(
                                    new QName(WSO2CARBON_NS, "at-least"));
                        if (permissionElements.hasNext()) {
                            menu.setAtLeastOnePermissionsRequired(true);
                        }
                    }
                    if (permissionElements.hasNext()) {
                        OMElement permissionsEle = (OMElement) permissionElements.next();
                        requirePermissionElements =
                            permissionsEle.getChildrenWithName(
                                    new QName(WSO2CARBON_NS, REQUIRE_PERMISSION));
                        while (requirePermissionElements.hasNext()) {
                            OMElement permissionEle = (OMElement) requirePermissionElements.next();
                            permissions.add(permissionEle.getText());
                        }
                        if (permissions.size() > 0) {
                            menu.setRequirePermission(
                                    permissions.toArray(new String[permissions.size()]));
                        }
                    }

                }
                // checking require master tenant flag
                Iterator requireSuperTenantEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, REQUIRE_SUPER_TENANT));
                if (requireSuperTenantEles.hasNext()) {
                    OMElement requireSuperTenantEle = (OMElement) requireSuperTenantEles.next();
                    if ("true".equalsIgnoreCase(requireSuperTenantEle.getText())) {
                        menu.setRequireSuperTenant(true);
                    }
                }
                // checking require master tenant flag
                Iterator requireNotSuperTenantEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, REQUIRE_NOT_SUPER_TENANT));
                if (requireNotSuperTenantEles.hasNext()) {
                    OMElement requireNotSuperTenantEle = (OMElement) requireNotSuperTenantEles.next();
                    if ("true".equalsIgnoreCase(requireNotSuperTenantEle.getText())) {
                        menu.setRequireNotSuperTenant(true);
                    }
                }
                // checking the require not logged in
                Iterator requireNotLoggedInEles =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, REQUIRE_NOT_LOGGED_IN));
                if (requireNotLoggedInEles.hasNext()) {
                    OMElement requireNotLoggedInEle = (OMElement) requireNotLoggedInEles.next();
                    if ("true".equalsIgnoreCase(requireNotLoggedInEle.getText())) {
                        menu.setRequireNotLoggedIn(true);
                    }
                }

                Iterator requireServiceModeEls =
                        menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, CarbonConstants.REQUIRE_CLOUD_DEPLOYMENT));
                if (requireServiceModeEls.hasNext()) {
                    OMElement requireServiceModeEle = (OMElement) requireServiceModeEls.next();
                    if ("true".equalsIgnoreCase(requireServiceModeEle.getText())) {
                        menu.setRequireCloudDeployment(true);
                    }
                }

                //url parameters
                Iterator<OMElement> urlParamsEles = menuEle.getChildrenWithName(new QName(WSO2CARBON_NS, "url-params"));
                if (urlParamsEles.hasNext()) {
                    OMElement urlParamsEle = (OMElement) urlParamsEles.next();
                    menu.setUrlParameters(urlParamsEle.getText());
                }
                component.addMenu(menu);

            }
        }
    }
}
