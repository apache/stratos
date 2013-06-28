/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.theme.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ServerConstants;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeUtil {

    private static final Log log = LogFactory.getLog(ThemeUtil.class);

    private static RegistryService registryService;
    private static RealmService realmService;
    private static final String CURRENT_THEME_KEY = "current-theme";
    private static final String THEME_PATH = "/repository/theme";
    private static final String THEME_ADMIN_PATH = THEME_PATH + "/admin";

    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }
    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static UserRegistry getThemeMgtSystemRegistry(String tenantPass) throws RegistryException {
        if (tenantPass != null && !tenantPass.equals("")) {
            // tenant 0th system registry
            UserRegistry systemRegistry = registryService.getGovernanceSystemRegistry();
            Resource resource = systemRegistry.get(
                    StratosConstants.TENANT_CREATION_THEME_PAGE_TOKEN + "/" + tenantPass);
            String tenantIdStr = resource.getProperty("tenantId");
            int tenantId = Integer.parseInt(tenantIdStr);

            return registryService.getGovernanceSystemRegistry(tenantId);
        }

        return null;
    }

    public static void removeTheUUID(String tenantPass) throws RegistryException {
        if (tenantPass != null && !tenantPass.equals("")) {
            // tenant 0th system registry
            UserRegistry systemRegistry = registryService.getGovernanceSystemRegistry();
            if(systemRegistry.resourceExists(
                    StratosConstants.TENANT_CREATION_THEME_PAGE_TOKEN + "/" + tenantPass)) {
                systemRegistry.delete(
                        StratosConstants.TENANT_CREATION_THEME_PAGE_TOKEN + "/" + tenantPass);
            }
        }
    }

    public static void transferAllThemesToRegistry(File rootDirectory, Registry registry,
                                                String registryPath)
                                                throws RegistryException {
        try {
            // adding the common media types
            Map<String, String> extensionToMediaTypeMap = new HashMap<String, String>();
            extensionToMediaTypeMap.put("gif", "image/gif");
            extensionToMediaTypeMap.put("jpg", "image/jpeg");
            extensionToMediaTypeMap.put("jpe", "image/jpeg");
            extensionToMediaTypeMap.put("jpeg", "image/jpeg");
            extensionToMediaTypeMap.put("png", "image/png");
            extensionToMediaTypeMap.put("css", "text/css");
            
            File[] filesAndDirs = rootDirectory.listFiles();
            if (filesAndDirs == null) {
                return;
            }
            List<File> filesDirs = Arrays.asList(filesAndDirs);

            for (File file : filesDirs) {
                String filename = file.getName();
                String fileRegistryPath = registryPath + RegistryConstants.PATH_SEPARATOR + filename;
                if (!file.isFile()) {
                    // This is a Directory add a new collection
                    // This path is used to store the file resource under registry
                    Collection newCollection = registry.newCollection();
                    registry.put(fileRegistryPath, newCollection);

                    // recur
                    transferAllThemesToRegistry(file, registry, fileRegistryPath);
                } else {
                    // Add the file to registry
                    Resource newResource = registry.newResource();
                    String mediaType = null;
                    if (filename.contains(".")) {
                        String fileExt = filename.substring(filename.lastIndexOf(".") + 1);
                        mediaType = extensionToMediaTypeMap.get(fileExt.toLowerCase());
                    }
                    if (mediaType == null) {
                        mediaType = new MimetypesFileTypeMap().getContentType(file);
                    }
                    newResource.setMediaType(mediaType);
                    newResource.setContentStream(new FileInputStream(file));
                    registry.put(fileRegistryPath, newResource);
                }
            }
        } catch (Exception e) {
            String msg = "Error loading theme to the sytem registry for registry path: " + registryPath;
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }

    }

    public static UserRegistry getThemeRegistryFromTenantPass(String tenantPass) throws RegistryException {
        UserRegistry themeMgtSystemRegistry = getThemeMgtSystemRegistry(tenantPass);
        if (themeMgtSystemRegistry != null) {
            return themeMgtSystemRegistry.getChrootedRegistry(THEME_ADMIN_PATH);
        } else {
            return null;
        }
    }

    public static UserRegistry getThemeRegistry(Registry registry) throws RegistryException {
        if (registry == null) {
            return null;
        }
        return ((UserRegistry)registry).getChrootedRegistry(THEME_ADMIN_PATH);
    }

    public static void loadResourceThemes() throws RegistryException {
        // loads the tenant0's system registry
        UserRegistry systemRegistry = registryService.getGovernanceSystemRegistry();
        // we are not checking whether the theme resources already exists to make sure, the newly
        // added themes can be loaded just at the activation of the component
        String themeRootFileName = System.getProperty(ServerConstants.CARBON_HOME) + File
                .separator + "resources" + File.separator + "allthemes";
        // we are always making this accessible from anyware
        File themeRootFile = new File(themeRootFileName);
        ThemeUtil.transferAllThemesToRegistry(themeRootFile, systemRegistry, StratosConstants.ALL_THEMES_PATH);

        CommonUtil.setAnonAuthorization(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + StratosConstants.ALL_THEMES_PATH,
                systemRegistry.getUserRealm());
    }

    public static String[] getAvailableThemes() throws RegistryException {
        Registry systemRegistry = registryService.getGovernanceSystemRegistry();
        if (!systemRegistry.resourceExists(StratosConstants.ALL_THEMES_PATH)) {
            log.info("The theme root path: " + StratosConstants.ALL_THEMES_PATH + " doesn't exist.");
            return new String[0];
        }
        Collection c = (Collection)systemRegistry.get(StratosConstants.ALL_THEMES_PATH);
        String[] childPaths = c.getChildren();
        for (int i = 0; i < childPaths.length; i ++) {
            childPaths[i] = RegistryUtils.getResourceName(childPaths[i]);
        }
        return childPaths;
    }


    public static void loadTheme(int tenantId) throws RegistryException {
        // get the util functions from the theme
        // this is always the 0th system reigstry
        UserRegistry systemRegistry = registryService.getGovernanceSystemRegistry(tenantId);
        String[] allThemes = getAvailableThemes();
        if (allThemes.length == 0) {
            log.info("No themes found.");
            return;
        }
        int randomNumber = (int)(Math.random() * allThemes.length);
        String ourLuckyTheme = allThemes[randomNumber];
        // anway now we are hard coding the default theme to be loaded here,
        ourLuckyTheme = "Default";
        applyThemeForDomain(ourLuckyTheme, systemRegistry);
    }

    public static void applyTheme(String themeName, String tenantPass, UserRegistry systemTenantRegistry) throws Exception {
        if (systemTenantRegistry == null) {
            systemTenantRegistry = getThemeMgtSystemRegistry(tenantPass);
        }
        applyThemeForDomain(themeName, systemTenantRegistry);
    }

    public static void applyThemeForDomain(String themeName, UserRegistry systemTenantRegistry)
                throws RegistryException {
        String sourcePath = StratosConstants.ALL_THEMES_PATH + "/" + themeName; // tenant 0s path
        String targetPath = THEME_PATH;

        UserRegistry systemZeroRegistry = registryService.getGovernanceSystemRegistry();

        // if the themes doesn't exist we would exclude applying it
        if (!systemZeroRegistry.resourceExists(sourcePath)) {
            log.info("The theme source path: " + sourcePath + " doesn't exist.");
            return;
        }

        // first delete the old one, or we can backup it if required
        // we are anyway getting a backup of the logo
        Resource logoR = null;
        String logoPath = targetPath + "/admin/" + "logo.gif";
        if (systemTenantRegistry.resourceExists(targetPath)) {
            if (systemTenantRegistry.resourceExists(logoPath)) {
                logoR = systemTenantRegistry.get(logoPath);
            }
            if (logoR != null) {
                logoR.getContent(); // we will load the content as well.
            }
            systemTenantRegistry.delete(targetPath);
        }

        // copy theme resources to tenant's registry 
        addResourcesRecursively(sourcePath, targetPath, systemZeroRegistry, systemTenantRegistry);

        // replace the logo
        if (logoR != null) {
            systemTenantRegistry.put(logoPath, logoR);
        }

        // remember the theme name
        Resource tenantThemeCollection = systemTenantRegistry.get(targetPath);
        tenantThemeCollection.setProperty(CURRENT_THEME_KEY, themeName);
        systemTenantRegistry.put(targetPath, tenantThemeCollection);

        try {
            CommonUtil.setAnonAuthorization(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + targetPath,
                    systemTenantRegistry.getUserRealm());
            CommonUtil.setAnonAuthorization(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + 
                    StratosConstants.ALL_THEMES_PATH,
                    systemTenantRegistry.getUserRealm());
        } catch (RegistryException e) {
            String msg = "Error in giving authorizations of the " + targetPath +
                    " to the anonymous user and everyone role.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    private static void addResourcesRecursively(String sourcePath, String targetPath,
                                                Registry superRegistry, Registry tenantRegistry)
            throws RegistryException {
        Resource resource = superRegistry.get(sourcePath);
        tenantRegistry.put(targetPath, resource);

        if (resource instanceof Collection) {
            String[] children = ((Collection) resource).getChildren();
            for (String child : children) {
                String childName = child.substring(child.lastIndexOf("/"), child.length());
                addResourcesRecursively(child, targetPath + childName, superRegistry, tenantRegistry);
            }
        }
    }

    public static String getCurrentTheme(String tenantPass, UserRegistry registry) throws Exception {
        if (registry == null) {
            registry = getThemeMgtSystemRegistry(tenantPass);
        }
        String targetPath = THEME_PATH;

        // remember the theme name
        Resource tenantThemeCollection = registry.get(targetPath);
        if (tenantThemeCollection == null) {
            return null;
        }
        return tenantThemeCollection.getProperty(CURRENT_THEME_KEY);
    }
}
