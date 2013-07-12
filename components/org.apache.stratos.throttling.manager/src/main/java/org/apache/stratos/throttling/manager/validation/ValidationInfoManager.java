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
package org.apache.stratos.throttling.manager.validation;

import java.util.Properties;
import java.util.Set;

import org.apache.stratos.throttling.manager.dataobjects.ThrottlingAccessValidation;
import org.apache.stratos.throttling.manager.exception.ThrottlingException;
import org.apache.stratos.throttling.manager.utils.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.MeteringAccessValidationUtils;
import org.apache.stratos.throttling.manager.dataobjects.ThrottlingDataContext;

public class ValidationInfoManager {
    private static final Log log = LogFactory.getLog(ValidationInfoManager.class);

    public static void loadValidationDetails(ThrottlingDataContext throttlingDataContext)
            throws ThrottlingException {
        int tenantId = throttlingDataContext.getTenantId();
        // retrieve validation info for the tenant
        String tenantValidationInfoResourcePath =
                StratosConstants.TENANT_USER_VALIDATION_STORE_PATH +
                        RegistryConstants.PATH_SEPARATOR + tenantId;

        ThrottlingAccessValidation accessValidation = throttlingDataContext.getAccessValidation();
        if (accessValidation == null) {
            accessValidation = new ThrottlingAccessValidation();
            throttlingDataContext.setAccessValidation(accessValidation);
        }
        try {
            Registry governanceSystemRegistry = Util.getSuperTenantGovernanceSystemRegistry();
            if (governanceSystemRegistry.resourceExists(tenantValidationInfoResourcePath)) {
                Resource tenantValidationInfoResource =
                        governanceSystemRegistry.get(tenantValidationInfoResourcePath);
                Properties properties = tenantValidationInfoResource.getProperties();
                Set<String> actions = MeteringAccessValidationUtils.getAvailableActions(properties);

                for (String action : actions) {
                    String blockActionStr =
                            tenantValidationInfoResource.getProperty(MeteringAccessValidationUtils
                                    .generateIsBlockedPropertyKey(action));

                    String blockActionMsg =
                            tenantValidationInfoResource.getProperty(MeteringAccessValidationUtils
                                    .generateErrorMsgPropertyKey(action));
                    accessValidation.setTenantBlocked(action, "true".equals(blockActionStr),
                            blockActionMsg);

                }
            }
        } catch (RegistryException e) {
            String msg =
                    "Error in getting the tenant validation info.  tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
    }

    public static void persistValidationDetails(ThrottlingDataContext throttlingDataContext)
            throws ThrottlingException {
        int tenantId = throttlingDataContext.getTenantId();
        // retrieve validation info for the tenant
        String tenantValidationInfoResourcePath =
                StratosConstants.TENANT_USER_VALIDATION_STORE_PATH +
                        RegistryConstants.PATH_SEPARATOR + tenantId;

        ThrottlingAccessValidation accessValidation = throttlingDataContext.getAccessValidation();
        try {
            Registry governanceSystemRegistry = Util.getSuperTenantGovernanceSystemRegistry();

            Resource tenantValidationInfoResource;
            if (governanceSystemRegistry.resourceExists(tenantValidationInfoResourcePath)) {
                tenantValidationInfoResource =
                        governanceSystemRegistry.get(tenantValidationInfoResourcePath);
            } else {
                tenantValidationInfoResource = governanceSystemRegistry.newResource();
            }

            Set<String> actions = accessValidation.getActions();
            for (String action : actions) {
                boolean blockAction = accessValidation.isTenantBlocked(action);
                String blockActionMsg = accessValidation.getTenantBlockedMsg(action);

                tenantValidationInfoResource.setProperty(MeteringAccessValidationUtils
                        .generateIsBlockedPropertyKey(action), blockAction ? "true" : "false");

                tenantValidationInfoResource.setProperty(MeteringAccessValidationUtils
                        .generateErrorMsgPropertyKey(action), blockActionMsg);
            }
            governanceSystemRegistry.put(tenantValidationInfoResourcePath,
                    tenantValidationInfoResource);
        } catch (RegistryException e) {
            String msg =
                    "Error in storing the tenant validation info.  tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
    }
}
