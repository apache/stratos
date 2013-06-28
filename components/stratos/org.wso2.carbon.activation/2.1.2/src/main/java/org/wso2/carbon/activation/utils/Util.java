/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.activation.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Utilities for the Service Activation Module for Tenants.
 */
public class Util {

    private static RegistryService registryService = null;
    private static RealmService realmService = null;
    private static String serviceName = null;
    private static final Log log = LogFactory.getLog(Util.class);

    private static boolean cloudServiceInfoPathSanityChecked = false;

    /**
     * Stores an instance of the Registry Service that can be used to access the registry.
     *
     * @param service the Registry Service instance.
     */
    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }

    /**
     * Method to retrieve the Registry Service instance.
     *
     * @return the Registry Service instance if it has been stored or null if not.
     */
    @SuppressWarnings("unused")
    public static RegistryService getRegistryService() {
        return registryService;
    }

    /**
     * Stores an instance of the Realm Service that can be used to access the user realm.
     *
     * @param service the Realm Service instance.
     */
    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    /**
     * Method to retrieve the Realm Service instance.
     *
     * @return the Realm Service instance if it has been stored or null if not.
     */
    public static RealmService getRealmService() {
        return realmService;
    }

    private static UserRegistry getSuperTenantGovernanceSystemRegistry() throws RegistryException {
        return registryService.getGovernanceSystemRegistry();
    }

//    /**
//     * Method to determine whether the given cloud service is active.
//     *
//     * @param cloudServiceName the name of the cloud service.
//     * @param tenantId         the tenant identifier.
//     * @param systemRegistry   the super tenant's governance system registry
//     *
//     * @return true if the service is active or false if not.
//     * @throws Exception if the operation failed.
//     */
//    public static boolean isCloudServiceActive(String cloudServiceName,
//                                               int tenantId, UserRegistry systemRegistry)
//            throws Exception {
//        // The cloud manager is always active
//        if (StratosConstants.CLOUD_MANAGER_SERVICE.equals(cloudServiceName)) {
//            return true;
//        }
//
//        if(!cloudServiceInfoPathSanityChecked) {
//            if(!systemRegistry.resourceExists(StratosConstants.CLOUD_SERVICE_INFO_STORE_PATH)) {
//                throw new RuntimeException("Cloud services list resource " +
//                                           StratosConstants.CLOUD_SERVICE_INFO_STORE_PATH + " does not exist");
//            }
//            cloudServiceInfoPathSanityChecked = true;
//        }
//
//        String cloudServiceInfoPath = StratosConstants.CLOUD_SERVICE_INFO_STORE_PATH +
//                RegistryConstants.PATH_SEPARATOR + tenantId +
//                RegistryConstants.PATH_SEPARATOR + cloudServiceName;
//        Resource cloudServiceInfoResource;
//        if (systemRegistry.resourceExists(cloudServiceInfoPath)) {
//            cloudServiceInfoResource = systemRegistry.get(cloudServiceInfoPath);
//            String isActiveStr =
//                cloudServiceInfoResource.getProperty(StratosConstants.CLOUD_SERVICE_IS_ACTIVE_PROP_KEY);
//            return Boolean.toString(true).equals(isActiveStr);
//        }
//        return false;
//    }
//

    /**
     * Method to obtain the name of the cloud service in which this module is running.
     *
     * @return the name of the service as defined in the server configuration.
     */
    public static String getServiceName() {
        if (serviceName == null) {
            serviceName = ServerConfiguration.getInstance().getFirstProperty("Name");
        }
        return serviceName;
    }

}
