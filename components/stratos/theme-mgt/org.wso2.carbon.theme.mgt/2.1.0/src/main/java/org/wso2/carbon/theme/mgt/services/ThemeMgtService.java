/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.theme.mgt.services;

import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.theme.mgt.util.ThemeUtil;
import org.wso2.carbon.registry.resource.beans.*;
import org.wso2.carbon.registry.resource.services.utils.*;
import org.wso2.carbon.registry.common.ResourceData;
import org.wso2.carbon.registry.common.utils.RegistryUtil;

import javax.activation.DataHandler;
import java.util.ArrayList;
import java.util.Stack;

public class ThemeMgtService extends AbstractAdmin {
    public ResourceTreeEntryBean getResourceTreeEntry(String resourcePath) throws Exception {
        UserRegistry themeRegistry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return GetResourceTreeEntryUtil.getResourceTreeEntry(resourcePath, themeRegistry);
    }

    public ContentBean getContentBean(String path) throws Exception {
        UserRegistry themeRegistry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return ContentUtil.getContent(path, themeRegistry);
    }


    public CollectionContentBean getCollectionContent(String path) throws Exception {
        UserRegistry themeRegistry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return ContentUtil.getCollectionContent(path, themeRegistry);
    }

    public ResourceData[] getResourceData(String[] paths) throws Exception {
        UserRegistry themeRegistry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return ContentUtil.getResourceData(paths, themeRegistry);
    }
    
    public String addCollection(
            String parentPath, String collectionName, String mediaType, String description)
            throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return AddCollectionUtil.process(parentPath, collectionName, mediaType, description, registry);
    }

    public void addResource(String path, String mediaType, String description, DataHandler content,
                            String symlinkLocation, String tenantPass)
            throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        if (registry == null) {
            registry = ThemeUtil.getThemeRegistryFromTenantPass(tenantPass);
        }
        AddResourceUtil.addResource(path, mediaType, description, content, symlinkLocation, registry,new String[0][0]);
    }

    public void importResource(
            String parentPath,
            String resourceName,
            String mediaType,
            String description,
            String fetchURL,
            String symlinkLocation,
            String tenantPass) throws Exception {

        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        if (registry == null) {
            registry = ThemeUtil.getThemeRegistryFromTenantPass(tenantPass);
        }
        ImportResourceUtil.importResource(parentPath, resourceName, mediaType, description, fetchURL,
                        symlinkLocation, registry,new String[0][0]);
    }

    public void addTextResource(
            String parentPath,
            String fileName,
            String mediaType,
            String description,
            String content) throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        AddTextResourceUtil.addTextResource(parentPath, fileName, mediaType, description, content, registry);
    }

    public MetadataBean getMetadata(String path) throws Exception {
        RegistryUtil.setSessionResourcePath(path);
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return MetadataPopulator.populate(path, registry);
    }

    public String getSessionResourcePath() throws Exception {
        return RegistryUtil.getSessionResourcePath();
    }

    public String getTextContent(String path) throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return GetTextContentUtil.getTextContent(path, registry);
    }

    public void updateTextContent(String resourcePath, String contentText) throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        UpdateTextContentUtil.updateTextContent(resourcePath, contentText, registry);
    }

    public ContentDownloadBean getContentDownloadBean(String path) throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        return GetDownloadContentUtil.getContentDownloadBean(path, registry);
    }

    public void renameResource(
            String parentPath, String oldResourcePath, String newResourceName)
            throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        RenameResourceUtil.renameResource(parentPath, oldResourcePath, newResourceName, registry);
    }

    public void delete(String pathToDelete) throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        DeleteUtil.process(pathToDelete, registry);
    }

    public String[] getAllPaths() throws Exception {
        UserRegistry registry = ThemeUtil.getThemeRegistry(getGovernanceSystemRegistry());
        // will use a stack in place of calling recurssion

        
        ArrayList<String> paths = new ArrayList<String>();
        Stack<Collection> parentCollections = new Stack<Collection>();
        Collection rootCollection = (Collection)registry.get("/");
        parentCollections.push(rootCollection);
        while (!parentCollections.empty()) {
            Collection parentCollection = parentCollections.pop();
            String[] childs = parentCollection.getChildren();
            for (String childPath: childs) {
                String pathToAdd = childPath.substring(1);
                paths.add(pathToAdd);
                Resource resource = registry.get(childPath);
                if (resource instanceof Collection) {
                    Collection c = (Collection)resource;
                    parentCollections.push(c);
                }
            }
        }
        return paths.toArray(new String[paths.size()]);
    }

    public String[] getAllThemes(String tenantPass) throws Exception {
        String[] allThemes = ThemeUtil.getAvailableThemes();
        //we are readding the selected theme as the first element
        String currentTheme = ThemeUtil.getCurrentTheme(tenantPass, (UserRegistry) getGovernanceSystemRegistryIfLoggedIn());
        String[] returnVal = new String[allThemes.length + 1];
        returnVal[0] = currentTheme;
        for (int i = 0; i < allThemes.length; i ++) {
            returnVal[i + 1] = allThemes[i];
        }
        return returnVal;
    }

    public void applyTheme(String themeName, String tenantPass) throws Exception {
        ThemeUtil.applyTheme(themeName, tenantPass, (UserRegistry) getGovernanceSystemRegistryIfLoggedIn());
        ThemeUtil.removeTheUUID(tenantPass);
    }

    private Registry getGovernanceSystemRegistryIfLoggedIn() {
        UserRegistry tempRegistry = (UserRegistry)getConfigUserRegistry();
        if (tempRegistry != null) {
            try {
                return ThemeUtil.getRegistryService().getGovernanceSystemRegistry(
                        tempRegistry.getTenantId());
            } catch (Exception ignored) {
                // The Registry service should not fail if the above if condition holds.
                return null;
            }
        }
        return null;
    }

}
