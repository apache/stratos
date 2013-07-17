/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.theme.mgt.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.common.utils.RegistryUtil;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.CollectionContentBean;
import org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.ContentBean;
import org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.ContentDownloadBean;
import org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.MetadataBean;
import org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.ResourceTreeEntryBean;
import org.apache.stratos.theme.mgt.stub.registry.resource.stub.common.xsd.ResourceData;
import org.apache.stratos.theme.mgt.stub.ThemeMgtServiceStub;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.activation.DataHandler;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class ThemeMgtServiceClient {

    private static final Log log = LogFactory.getLog(ThemeMgtServiceClient.class);

    private ThemeMgtServiceStub stub;
    private String epr;

    public ThemeMgtServiceClient (
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws RegistryException {

        epr = backendServerURL + "ThemeMgtService";

        try {
            stub = new ThemeMgtServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate resource service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }


    public ThemeMgtServiceClient(String cookie, ServletConfig config, HttpSession session)
            throws RegistryException {

        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "ThemeMgtService";

        try {
            stub = new ThemeMgtServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate resource service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public ThemeMgtServiceClient(ServletConfig config, HttpSession session)
            throws RegistryException {

        String cookie = (String)session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "ThemeMgtService";

        try {
            stub = new ThemeMgtServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate resource service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public ResourceTreeEntryBean getResourceTreeEntry(String resourcePath)
            throws Exception {

        ResourceTreeEntryBean entryBean = null;
        try {
            Options options = stub._getServiceClient().getOptions();
            options.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
            entryBean = stub.getResourceTreeEntry(resourcePath);
        } catch (Exception e) {
            String msg = "Failed to get resource tree entry for resource " +
                    resourcePath + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
        if (entryBean == null) {
            throw new ResourceNotFoundException("The resource does not exist");
        }
        return entryBean;
    }

     public ContentBean getContent(HttpServletRequest request) throws Exception {

        String path = RegistryUtil.getPath(request);
        ContentBean bean = null;
        try {
            bean = stub.getContentBean(path);
        } catch (Exception e) {
            String msg = "Failed to get content from the resource service. " +
                    e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return bean;
    }


    public CollectionContentBean getCollectionContent(HttpServletRequest request) throws Exception {

        String path = RegistryUtil.getPath(request);
        CollectionContentBean bean = null;
        try {
            bean = stub.getCollectionContent(path);

        } catch (Exception e) {
            String msg = "Failed to get collection content from the resource service. " +
                    e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return bean;
    }


    public ResourceData[] getResourceData(String[] paths) throws Exception {

        ResourceData[] resourceData;
        try {
            resourceData = stub.getResourceData(paths);
        } catch (Exception e) {
            String msg = "Failed to get resource data from the resource service. " +
                    e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return resourceData;
    }

    public String addCollection(
            String parentPath, String collectionName, String mediaType, String description) throws Exception  {
        try {
            return stub.addCollection(parentPath, collectionName, mediaType, description);
        } catch (Exception e) {
            String msg = "Failed to add collection " + collectionName + " for parent path: " + parentPath + ". " +
                    e.getMessage();
            log.error(msg, e);
            throw e;
        }
    }

    public void addResource(String path, String mediaType, String description, DataHandler content,
                            String symlinkLocation, String tenantPass)
            throws Exception {

        try {
            Options options = stub._getServiceClient().getOptions();
            options.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
            options.setTimeOutInMilliSeconds(300000);
            stub.addResource(path, mediaType, description, content, symlinkLocation, tenantPass);

        } catch (Exception e) {

            String msg = "Failed to add resource " + path + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
    }

    public void importResource(
            String parentPath,
            String resourceName,
            String mediaType,
            String description,
            String fetchURL,
            String symlinkLocation,
            boolean isAsync,
            String tenantPass) throws Exception {

        try {
            // This is used by the add wsdl UI. WSDL validation takes long when there are wsdl
            // imports to prevent this we make a async call.
            if (isAsync) {
                stub._getServiceClient().getOptions().setProperty(
                        MessageContext.CLIENT_API_NON_BLOCKING,Boolean.TRUE);
            }
            stub.importResource(parentPath, resourceName, mediaType, description, fetchURL, symlinkLocation, tenantPass);
        } catch (Exception e) {
            String msg = "Failed to import resource with name " + resourceName +
                    " to the parent collection " + parentPath + ". " + e.getMessage();
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }


    public void addTextResource(
            String parentPath,
            String fileName,
            String mediaType,
            String description,
            String content) throws Exception {

        try {
            stub.addTextResource(parentPath, fileName, mediaType, description, content);
        } catch (Exception e) {
            String msg = "Failed to add new text resource with name " + fileName +
                    " to the parent collection " + parentPath + ". " + e.getMessage();
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public MetadataBean getMetadata(HttpServletRequest request) throws Exception {

        String path = RegistryUtil.getPath(request);
        if (path == null) {
            path = getSessionResourcePath();
            if (path == null) {
                path = RegistryConstants.ROOT_PATH;
            }

            request.setAttribute("path", path);
        }

        MetadataBean bean = null;
        try {
            bean = stub.getMetadata(path);
        } catch (Exception e) {
            String msg = "Failed to get resource metadata from the resource service. " +
                    e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return bean;
    }

    public MetadataBean getMetadata(HttpServletRequest request,String root) throws Exception {

        String path = RegistryConstants.ROOT_PATH;
        request.setAttribute("path", path);
        if (path == null) {
            path = getSessionResourcePath();
            if (path == null) {
                path = RegistryConstants.ROOT_PATH;
            }
        }

        MetadataBean bean = null;
        try {
            bean = stub.getMetadata(path);
        } catch (Exception e) {
            String msg = "Failed to get resource metadata from the resource service. " +
                    e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return bean;
    }

    public String getSessionResourcePath() throws Exception {

        String sessionResourcePath;
        try {
            sessionResourcePath = stub.getSessionResourcePath();
        } catch (Exception e) {

            String msg = "Failed to get the session resource path. " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
        return sessionResourcePath;
    }


    public String getTextContent(HttpServletRequest request) throws Exception {

        String path = RegistryUtil.getPath(request);

        String textContent = null;
        try {
            textContent = stub.getTextContent(path);
        } catch (Exception e) {

            String msg = "Failed get text content of the resource " +
                    path + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
        return textContent;
    }

     public void updateTextContent(String resourcePath, String contentText) throws Exception {

        try {
            stub.updateTextContent(resourcePath, contentText);
        } catch (Exception e) {

            String msg = "Failed to update text content of the resource " +
                    resourcePath + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
    }

    public ContentDownloadBean getContentDownloadBean(String path) throws Exception {

        ContentDownloadBean bean = stub.getContentDownloadBean(path);
        return bean;
    }

    public void renameResource(
            String parentPath, String oldResourcePath, String newResourceName)
            throws Exception {

        try {
            stub.renameResource(parentPath, oldResourcePath, newResourceName);
        } catch (Exception e) {
            String msg = "Failed to rename resource with name " + oldResourcePath +
                    " to the new name " + newResourceName + ". " + e.getMessage();
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public void delete(String pathToDelete) throws Exception {

        try {
            stub.delete(pathToDelete);
        } catch (Exception e) {
            String msg = "Failed to delete " + pathToDelete + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
    }

    public String[] getAllPaths() throws Exception {

        try {
            return  stub.getAllPaths();
        } catch (Exception e) {
            String msg = "Failed to getAllPaths. " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
    }

    public String[] getAllThemes(String tenantPass) throws Exception {
        try {
            return stub.getAllThemes(tenantPass);
        } catch (Exception e) {
            String msg = "Failed to get All Themes.";
            log.error(msg, e);
            throw e;
        }
    }

    public void applyTheme(String themeName, String tenantPass) throws Exception {
        try {
            stub.applyTheme(themeName, tenantPass);
        } catch (Exception e) {
            String msg = "Failed to apply the theme: " + themeName;
            log.error(msg, e);
            throw e;
        }
    }
}
