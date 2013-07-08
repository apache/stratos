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
package org.apache.stratos.activation.activation.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.activation.activation.utils.ActivationManager;
import org.apache.stratos.activation.activation.utils.Util;
import org.apache.stratos.common.config.CloudServiceConfigParser;
import org.apache.stratos.common.config.CloudServicesDescConfig;
import org.apache.stratos.common.util.CloudServicesUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * Admin Service to handle activation of cloud services used by tenants.
 */
public class ActivationService {

    private static final Log log = LogFactory.getLog(ActivationService.class);

    /**
     * Method to update an activation record.
     *
     * @param tenantId the tenant identifier.
     *
     * @throws Exception if the operation failed.
     */
    public static void updateActivation(int tenantId) throws Exception {
        if (tenantId != 0) {
            String serviceName = Util.getServiceName();
            boolean isActive = CloudServicesUtil.isCloudServiceActive(serviceName, tenantId);
            CloudServicesDescConfig cloudServicesDesc =
                                                        CloudServiceConfigParser.loadCloudServicesConfiguration();
            CloudServicesUtil.setCloudServiceActive(!isActive,
                                                    serviceName,
                                                    tenantId,
                                                    cloudServicesDesc.getCloudServiceConfigs()
                                                                     .get(serviceName));
            ActivationManager.setActivation(tenantId, !isActive);
        }
    }

    /**
     * Method to determine whether a service is active for the given tenant.
     *
     * @param tenantId tenantId the tenant identifier.
     *
     * @return whether the service is active.
     * @throws Exception if the operation failed.
     */
    public boolean isActive(int tenantId) throws Exception {
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            return true;
        }
        if (ActivationManager.activationRecorded(tenantId)) {
            return ActivationManager.getActivation(tenantId);
        }
        String serviceName = Util.getServiceName();
        if (CloudServicesUtil.isCloudServiceActive(serviceName, tenantId)) {
            log.debug("Successful attempt to access " + serviceName + " by tenant " + tenantId);
            ActivationManager.setActivation(tenantId, true);
            return true;
        }
        log.warn("Failed attempt to access " + serviceName + " by tenant " + tenantId);
        ActivationManager.setActivation(tenantId, false);
        return false;
    }

}
