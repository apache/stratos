/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.stratos.manager.services.mgt.util;

import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.config.CloudServiceConfig;
import org.wso2.carbon.stratos.common.config.CloudServicesDescConfig;
import org.wso2.carbon.stratos.common.config.PermissionConfig;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.CarbonUtils;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    private static final String CONFIG_FILENAME = "cloud-services-desc.xml";

    private static RegistryService registryService;
    private static RealmService realmService;
    private static CloudServicesDescConfig cloudServicesDescConfig = null;

    public static synchronized void setRegistryService(RegistryService service) {
        if ((registryService == null) || (service == null)) {
            registryService = service;
        }
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setRealmService(RealmService service) {
        if ((realmService == null) || (service == null)) {
            realmService = service;
        }
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static RealmConfiguration getBootstrapRealmConfiguration() {
        return realmService.getBootstrapRealmConfiguration();
    }

    public static UserRegistry getTenantZeroSystemGovernanceRegistry() throws RegistryException {
        return registryService.getGovernanceSystemRegistry();
    }

    public static UserRegistry getSystemGovernanceRegistry(int tenantId) throws RegistryException {
        return registryService.getGovernanceSystemRegistry(tenantId);
    }

    public static UserRegistry getSystemConfigRegistry(int tenantId) throws RegistryException {
        return registryService.getConfigSystemRegistry(tenantId);
    }

    public static void loadCloudServicesConfiguration() throws Exception {
        // now load the cloud services configuration
        String configFileName =
                CarbonUtils.getCarbonConfigDirPath() + File.separator + StratosConstants.MULTITENANCY_CONFIG_FOLDER +
                        File.separator+ CONFIG_FILENAME;
        OMElement configElement;
        try {
            configElement = CommonUtil.buildOMElement(new FileInputStream(configFileName));
        } catch (Exception e) {
            String msg = "Error in building the cloud service configuration. config filename: " +
                            configFileName + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        cloudServicesDescConfig = new CloudServicesDescConfig(configElement);
    }

    public static CloudServicesDescConfig getCloudServicesDescConfig() {
        return cloudServicesDescConfig;
    }

    public static CloudServiceConfig getCloudServiceConfig(String cloudServiceName) {
        Map<String, CloudServiceConfig> cloudServiceConfigs =
                cloudServicesDescConfig.getCloudServiceConfigs();
        return cloudServiceConfigs.get(cloudServiceName);
    }

    public static void setCloudServiceActive(boolean active, String cloudServiceName, 
                                             int tenantId)throws Exception {
        CloudServiceConfig cloudServiceConfig = getCloudServiceConfig(cloudServiceName);
        if (cloudServiceConfig.getLabel() == null) {
            // for the non-labeled services, we are not setting/unsetting the service active
            return;
        }

        UserRegistry tenantZeroSystemGovernanceRegistry;
        UserRegistry systemConfigRegistry;
        try {
            tenantZeroSystemGovernanceRegistry = Util.getTenantZeroSystemGovernanceRegistry();
            systemConfigRegistry = Util.getSystemConfigRegistry(tenantId);
        } catch (RegistryException e) {
            String msg = "Error in getting the tenant 0 system config registry";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        String cloudServiceInfoPath =
                Constants.CLOUD_SERVICE_INFO_STORE_PATH + RegistryConstants.PATH_SEPARATOR +
                        tenantId + RegistryConstants.PATH_SEPARATOR + cloudServiceName;
        Resource cloudServiceInfoResource;
        if (tenantZeroSystemGovernanceRegistry.resourceExists(cloudServiceInfoPath)) {
            cloudServiceInfoResource = tenantZeroSystemGovernanceRegistry.get(cloudServiceInfoPath);
        } else {
            cloudServiceInfoResource = tenantZeroSystemGovernanceRegistry.newCollection();
        }
        cloudServiceInfoResource.setProperty(Constants.CLOUD_SERVICE_IS_ACTIVE_PROP_KEY,
                active ? "true" : "false");
        tenantZeroSystemGovernanceRegistry.put(cloudServiceInfoPath, cloudServiceInfoResource);

        // then we will copy the permissions
        List<PermissionConfig> permissionConfigs = cloudServiceConfig.getPermissionConfigs();
        for (PermissionConfig permissionConfig : permissionConfigs) {
            String path = permissionConfig.getPath();
            String name = permissionConfig.getName();
            if (active) {
                if (!systemConfigRegistry.resourceExists(path)) {
                    Collection collection = systemConfigRegistry.newCollection();
                    collection.setProperty(UserMgtConstants.DISPLAY_NAME, name);
                    systemConfigRegistry.put(path, collection);
                }
            } else {
                if (systemConfigRegistry.resourceExists(path)) {
                    systemConfigRegistry.delete(path);
                }
            }
        }
    }

    public static boolean isCloudServiceActive(String cloudServiceName, 
                                               int tenantId)throws Exception {
        UserRegistry systemGovernanceRegistry;
        try {
            systemGovernanceRegistry = Util.getTenantZeroSystemGovernanceRegistry();
        } catch (RegistryException e) {
            String msg = "Error in getting the tenant 0 system config registry";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        String cloudServiceInfoPath =
                Constants.CLOUD_SERVICE_INFO_STORE_PATH + RegistryConstants.PATH_SEPARATOR +
                        tenantId + RegistryConstants.PATH_SEPARATOR + cloudServiceName;
        
        if (systemGovernanceRegistry.resourceExists(cloudServiceInfoPath)) {
            Resource cloudServiceInfoResource = systemGovernanceRegistry.get(cloudServiceInfoPath);
            String isActiveStr = cloudServiceInfoResource.getProperty(
                    Constants.CLOUD_SERVICE_IS_ACTIVE_PROP_KEY);
            return "true".equals(isActiveStr);
        }
        
        return false;
    }

    /**
     * Currently this is not used, as the icons are loaded from the webapps
     * 
     * @throws Exception
     */
    public static void loadServiceIcons() throws Exception {
        String serviceIconDirLocation =
                CarbonUtils.getCarbonHome() + "/resources/cloud-service-icons";
        File serviceIconDirDir = new File(serviceIconDirLocation);
        UserRegistry registry = getTenantZeroSystemGovernanceRegistry();
        try {
            // adding the common media types
            Map<String, String> extensionToMediaTypeMap = new HashMap<String, String>();
            extensionToMediaTypeMap.put("gif", "img/gif");
            extensionToMediaTypeMap.put("jpg", "img/gif");
            extensionToMediaTypeMap.put("png", "img/png");

            File[] filesAndDirs = serviceIconDirDir.listFiles();
            if (filesAndDirs == null) {
                return;
            }
            List<File> filesDirs = Arrays.asList(filesAndDirs);

            for (File file : filesDirs) {
                String filename = file.getName();
                String fileRegistryPath = Constants.CLOUD_SERVICE_ICONS_STORE_PATH +
                                RegistryConstants.PATH_SEPARATOR + filename;

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
        } catch (Exception e) {
            String msg = "Error loading icons to the system registry for registry path: " +
                            Constants.CLOUD_SERVICE_ICONS_STORE_PATH;
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        try {
            CommonUtil.setAnonAuthorization(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + 
                    Constants.CLOUD_SERVICE_ICONS_STORE_PATH, registry.getUserRealm());
        } catch (RegistryException e) {
            String msg = "Setting the annon access enabled for the services icons paths.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

    }

}
