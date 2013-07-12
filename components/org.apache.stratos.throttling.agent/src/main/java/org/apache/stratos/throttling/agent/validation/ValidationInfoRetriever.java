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
package org.apache.stratos.throttling.agent.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.MeteringAccessValidationUtils;

public class ValidationInfoRetriever {
    private static final Log log = LogFactory.getLog(ValidationInfoRetriever.class);
    UserRegistry governanceSystemRegistry;
    
    public ValidationInfoRetriever(UserRegistry governanceSystemRegistry) {
        this.governanceSystemRegistry = governanceSystemRegistry;
    }

    public ValidationInfo getValidationInfo(String action, int tenantId) throws ValidationException {
        ValidationInfo validationInfo = new ValidationInfo();
        Resource validationInfoResource = getResource(tenantId);
        if(validationInfoResource == null){
            //this means, the user is allowed to proceed
            return validationInfo;
        }
        
        // first get the validation info for all actions
        checkAction(StratosConstants.THROTTLING_ALL_ACTION, validationInfoResource, validationInfo);
        if (validationInfo.isActionBlocked()) {
            return validationInfo;
        }
        checkAction(action, validationInfoResource, validationInfo);
        return validationInfo;
    }
    
    public ValidationInfo getValidationInfo(String[] actions, int tenantId) throws ValidationException {
        ValidationInfo validationInfo = new ValidationInfo();
        Resource validationInfoResource = getResource(tenantId);
        if(validationInfoResource == null){
            //this means, the user is allowed to proceed
            return validationInfo;
        }
        
     // first get the validation info for all actions
        checkAction(StratosConstants.THROTTLING_ALL_ACTION, validationInfoResource, validationInfo);
        if (validationInfo.isActionBlocked()) {
            return validationInfo;
        }
        
        for(String action : actions){
            checkAction(action, validationInfoResource, validationInfo);
            if (validationInfo.isActionBlocked()) {
                return validationInfo;
            }
        }
        return validationInfo;
    }
    
    private Resource getResource (int tenantId) throws ValidationException{
     // first retrieve validation info for the tenant
        String tenantValidationInfoResourcePath = 
            StratosConstants.TENANT_USER_VALIDATION_STORE_PATH +
                        RegistryConstants.PATH_SEPARATOR + tenantId;
        Resource tenantValidationInfoResource = null;
        try {
            if (governanceSystemRegistry.resourceExists(tenantValidationInfoResourcePath)) {
                tenantValidationInfoResource =
                        governanceSystemRegistry.get(tenantValidationInfoResourcePath);
            }
        } catch (RegistryException e) {
            String msg = "Error in getting the tenant validation info for tenant:" + tenantId;
            log.error(msg, e);
            throw new ValidationException(msg, e);
        }
        return tenantValidationInfoResource;
    }

    private void checkAction(String action, Resource tenantValidationInfoResource, 
                             ValidationInfo validationInfo){
        String blockActionStr =
            tenantValidationInfoResource.getProperty(
                    MeteringAccessValidationUtils.generateIsBlockedPropertyKey(action));

        if ("true".equals(blockActionStr)) {
            validationInfo.setActionBlocked(true);

            String blockActionMsg =
                tenantValidationInfoResource.getProperty(
                        MeteringAccessValidationUtils.generateErrorMsgPropertyKey(action));
            validationInfo.setBlockedActionMsg(blockActionMsg);
        }
    }
}
