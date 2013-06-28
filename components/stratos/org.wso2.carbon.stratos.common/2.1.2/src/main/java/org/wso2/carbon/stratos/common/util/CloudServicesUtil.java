package org.wso2.carbon.stratos.common.util;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.config.CloudServiceConfig;
import org.wso2.carbon.stratos.common.config.CloudServicesDescConfig;
import org.wso2.carbon.stratos.common.config.PermissionConfig;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.internal.CloudCommonServiceComponent;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class CloudServicesUtil {

    private static final Log log = LogFactory.getLog(CloudServicesUtil.class);
    // TODO protect using Java security

    public static void activateAllServices(CloudServicesDescConfig cloudServicesDesc, int tenantId) throws Exception {

        java.util.Collection<CloudServiceConfig> cloudServiceConfigList =
                                                                          cloudServicesDesc.getCloudServiceConfigs().
                                                                                            values();
        if (cloudServiceConfigList != null) {
            for (CloudServiceConfig cloudServiceConfig : cloudServiceConfigList) {
                if (cloudServiceConfig.isDefaultActive()) {
                    String cloudServiceName = cloudServiceConfig.getName();
                    try {
                        if (!CloudServicesUtil.isCloudServiceActive(cloudServiceName, tenantId)) {
                            CloudServicesUtil.setCloudServiceActive(true,
                                                                    cloudServiceName,
                                                                    tenantId,
                                                                    cloudServicesDesc.getCloudServiceConfigs().
                                                                                      get(cloudServiceName));
                        }
                    } catch (Exception e) {
                        String msg = "Error in activating the cloud service at the tenant" +
                                     "creation. tenant id: " + tenantId + ", service name: " +
                                     cloudServiceName;
                        log.error(msg, e);
                        throw new UserStoreException(msg, e);
                    }
                }
            }
        }     
    }

    public static void activateOriginalAndCompulsoryServices(CloudServicesDescConfig cloudServicesDesc,
                                                             String originalService,
                                                             int tenantId) throws Exception {

        Map<String, CloudServiceConfig> cloudServiceConfigs =
                                                              cloudServicesDesc.getCloudServiceConfigs();
        if (CloudServicesUtil.isServiceNameValid(cloudServicesDesc, originalService)) {
            if (!CloudServicesUtil.isCloudServiceActive(originalService, tenantId)) {
                CloudServicesUtil.setCloudServiceActive(true, originalService, tenantId,
                                                        cloudServiceConfigs.get(originalService));
                log.info("Successfully activated the " + originalService + " for the tenant " +
                         tenantId);
            }
            // register the compulsory services
            if (!CloudServicesUtil.isCloudServiceActive(StratosConstants.CLOUD_IDENTITY_SERVICE,
                                                        tenantId)) {
                CloudServicesUtil.setCloudServiceActive(true,
                                                        StratosConstants.CLOUD_IDENTITY_SERVICE,
                                                        tenantId,
                                                        cloudServiceConfigs.get(StratosConstants.CLOUD_IDENTITY_SERVICE));
            }
            if (!CloudServicesUtil.isCloudServiceActive(StratosConstants.CLOUD_GOVERNANCE_SERVICE,
                                                        tenantId)) {
                CloudServicesUtil.setCloudServiceActive(true,
                                                        StratosConstants.CLOUD_GOVERNANCE_SERVICE,
                                                        tenantId,
                                                        cloudServiceConfigs.get(StratosConstants.CLOUD_GOVERNANCE_SERVICE));
            }
        } else {
            log.warn("Unable to activate the " + originalService + " for the tenant " + tenantId);
        }

    }

    public static void setCloudServiceActive(boolean active,
                                             String cloudServiceName,
                                             int tenantId, CloudServiceConfig cloudServiceConfig)
                                                                                                 throws Exception {
        if (cloudServiceConfig.getLabel() == null) {
            // for the non-labled services, we are not setting/unsetting the
            // service active
            return;
        }

        UserRegistry govRegistry =
                CloudCommonServiceComponent.getGovernanceSystemRegistry(
                        MultitenantConstants.SUPER_TENANT_ID);
        UserRegistry configRegistry = CloudCommonServiceComponent.getConfigSystemRegistry(tenantId);
        String cloudServiceInfoPath = StratosConstants.CLOUD_SERVICE_INFO_STORE_PATH +
                                      RegistryConstants.PATH_SEPARATOR + tenantId +
                                      RegistryConstants.PATH_SEPARATOR + cloudServiceName;
        
        Resource cloudServiceInfoResource;
        if (govRegistry.resourceExists(cloudServiceInfoPath)) {
            cloudServiceInfoResource = govRegistry.get(cloudServiceInfoPath);
        } else {
            cloudServiceInfoResource = govRegistry.newCollection();
        }
        cloudServiceInfoResource.setProperty(StratosConstants.CLOUD_SERVICE_IS_ACTIVE_PROP_KEY,
                                             active ? "true" : "false");
        govRegistry.put(cloudServiceInfoPath, cloudServiceInfoResource);

        // then we will copy the permissions
        List<PermissionConfig> permissionConfigs = cloudServiceConfig.getPermissionConfigs();
        for (PermissionConfig permissionConfig : permissionConfigs) {
            String path = permissionConfig.getPath();
            String name = permissionConfig.getName();
            if (active) {
                if (!configRegistry.resourceExists(path)) {
                    Collection collection = configRegistry.newCollection();
                    collection.setProperty(UserMgtConstants.DISPLAY_NAME, name);
                    configRegistry.put(path, collection);
                }
            } else {
                if (configRegistry.resourceExists(path)) {
                    configRegistry.delete(path);
                }
            }
        }
    }

    public static boolean isCloudServiceActive(String cloudServiceName,
                                               int tenantId) throws Exception {
        UserRegistry govRegistry = CloudCommonServiceComponent.getGovernanceSystemRegistry(
                                                                                           MultitenantConstants.SUPER_TENANT_ID);
        return isCloudServiceActive(cloudServiceName, tenantId, govRegistry);
    }

    public static boolean isCloudServiceActive(String cloudServiceName,
                                               int tenantId, UserRegistry govRegistry)
                                                                                      throws Exception {
        // The cloud manager is always active
        if (StratosConstants.CLOUD_MANAGER_SERVICE.equals(cloudServiceName)) {
            return true;
        }

        String cloudServiceInfoPath = StratosConstants.CLOUD_SERVICE_INFO_STORE_PATH +
                                      RegistryConstants.PATH_SEPARATOR + tenantId +
                                      RegistryConstants.PATH_SEPARATOR + cloudServiceName;
        Resource cloudServiceInfoResource;
        if (govRegistry.resourceExists(cloudServiceInfoPath)) {
            cloudServiceInfoResource = govRegistry.get(cloudServiceInfoPath);
            String isActiveStr =
                                 cloudServiceInfoResource.getProperty(
                                                         StratosConstants.CLOUD_SERVICE_IS_ACTIVE_PROP_KEY);
            return "true".equals(isActiveStr);
        }
        return false;
    }

    public static boolean isServiceNameValid(CloudServicesDescConfig cloudServicesDesc,
                                               String cloudServiceName) {
        if(cloudServiceName == null) {
            return false;
        }
        java.util.Collection<CloudServiceConfig> cloudServiceConfigList =
                cloudServicesDesc.getCloudServiceConfigs().values();
        if (cloudServiceName.equals(StratosConstants.CLOUD_MANAGER_SERVICE)) {
            return false;
        }
        for (CloudServiceConfig cloudServiceConfig : cloudServiceConfigList) {
            if (cloudServiceConfig.getName().equals(cloudServiceName)) {
                return true;
            }
        }
        return false;
    }
}
