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
package org.wso2.stratos.manager.services.mgt.services;

import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.config.CloudServiceConfig;
import org.wso2.carbon.stratos.common.config.CloudServicesDescConfig;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.stratos.manager.services.mgt.beans.CloudService;
import org.wso2.stratos.manager.services.mgt.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CloudManagerService extends AbstractAdmin {
    
    public CloudService[] retrieveCloudServiceInfo() throws Exception {
        UserRegistry registry = (UserRegistry)getConfigUserRegistry();
        int tenantId = registry.getTenantId();

        CloudServicesDescConfig cloudServicesDesc = Util.getCloudServicesDescConfig();
        Map<String, CloudServiceConfig> cloudServiceConfigs =
                cloudServicesDesc.getCloudServiceConfigs();
        List<CloudService> cloudServices = new ArrayList<CloudService>();
        if (cloudServiceConfigs != null) {
            Set<String> configKeys = cloudServiceConfigs.keySet();
            for (String configKey : configKeys) {
                CloudServiceConfig cloudServiceConfig = cloudServiceConfigs.get(configKey);
                String label = cloudServiceConfig.getLabel();
                if (label == null) {
                    // we are only returning display-able services
                    continue;
                }
                CloudService cloudService = new CloudService();
                String name = cloudServiceConfig.getName();
                cloudService.setName(name);
                cloudService.setLabel(label);
                cloudService.setLink(cloudServiceConfig.getLink());
                cloudService.setIcon(cloudServiceConfig.getIcon());
                cloudService.setDescription(cloudServiceConfig.getDescription());
                cloudService.setProductPageURL(cloudServiceConfig.getProductPageURL());
                boolean active = Util.isCloudServiceActive(name, tenantId);
                cloudService.setActive(tenantId == 0 || active);

                cloudServices.add(cloudService);
            }
        }
        return cloudServices.toArray(new CloudService[cloudServices.size()]);
    }

    public void saveCloudServicesActivity(String[] activeServiceNames) throws Exception {
        UserRegistry registry = (UserRegistry)getConfigUserRegistry();
        int tenantId = registry.getTenantId();

        CloudServicesDescConfig cloudServicesDesc = Util.getCloudServicesDescConfig();
        Map<String, CloudServiceConfig> cloudServiceConfigMap =
                cloudServicesDesc.getCloudServiceConfigs();

        List<String> activeServiceNamesList = Arrays.asList(activeServiceNames);
        if (cloudServiceConfigMap != null) {
            for (String cloudServiceName : cloudServiceConfigMap.keySet()) {
                if (activeServiceNamesList.contains(cloudServiceName)) {
                    // this should be made active
                    if (!Util.isCloudServiceActive(cloudServiceName, tenantId)) {
                        Util.setCloudServiceActive(true, cloudServiceName, tenantId);
                    }
                } else {
                    // this should be made inactive
                    if (Util.isCloudServiceActive(cloudServiceName, tenantId)) {
                        Util.setCloudServiceActive(false, cloudServiceName, tenantId);
                    }

                }
            }
        }
        Util.setCloudServiceActive(true, StratosConstants.CLOUD_IDENTITY_SERVICE, tenantId);
        Util.setCloudServiceActive(true, StratosConstants.CLOUD_GOVERNANCE_SERVICE, tenantId);
    }

    public void activate(String cloudServiceName) throws Exception {
        UserRegistry registry = (UserRegistry) getConfigUserRegistry();
        int tenantId = registry.getTenantId();
        if (!Util.isCloudServiceActive(cloudServiceName, tenantId)) {
            Util.setCloudServiceActive(true, cloudServiceName, tenantId);
        }
    }

    public void deactivate(String cloudServiceName) throws Exception {
        if (StratosConstants.CLOUD_IDENTITY_SERVICE.equals(cloudServiceName) ||
            StratosConstants.CLOUD_GOVERNANCE_SERVICE.equals(cloudServiceName)) {
            // cloud identity and governance services cannot be deactivated..
            return;
        }
        UserRegistry registry = (UserRegistry) getConfigUserRegistry();
        int tenantId = registry.getTenantId();
        if (Util.isCloudServiceActive(cloudServiceName, tenantId)) {
            Util.setCloudServiceActive(false, cloudServiceName, tenantId);
        }
    }
}


