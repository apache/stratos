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
package org.wso2.carbon.throttling.agent.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.throttling.agent.cache.TenantThrottlingInfo;
import org.wso2.carbon.throttling.agent.cache.ThrottlingActionInfo;
import org.wso2.carbon.throttling.agent.internal.ThrottlingAgentServiceComponent;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManagerListener;
import org.wso2.carbon.user.core.listener.AuthorizationManagerListener;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Map;

public class PerUserAddListener extends AbstractUserStoreManagerListener {
    private static final Log log = LogFactory.getLog(PerUserAddListener.class);

    public int getExecutionOrderId() {
        return AuthorizationManagerListener.MULTITENANCY_USER_RESTRICTION_HANDLER;
    }

    @Override
    public boolean addUser(String userName, Object credential, String[] roleList,
                           Map<String, String> claims, String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {

        //If this is not a cloud deployment there is no way to run the throttling rules
        //This means the product is being used in the tenant mode
        //Therefore we can ommit running the throttling rules
        if("false".equals(ServerConfiguration.getInstance().getFirstProperty(CarbonConstants.IS_CLOUD_DEPLOYMENT))){
            log.info("Omitting executing throttling rules becasue this is not a cloud deployment.");
            return true;
        }
        int tenantId = userStoreManager.getTenantId();
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            return true;
        }
        // running the rules invoking the remote throttling manager.
        String[] users = userStoreManager.listUsers("*", -1);
        if (users.length <= 1) {
            // no filtering if the users count < 1
            return true;
        }

        try {
            ThrottlingAgentServiceComponent.getThrottlingAgent().executeManagerThrottlingRules(tenantId);
            ThrottlingAgentServiceComponent.getThrottlingAgent().updateThrottlingCacheForTenant();
        } catch (Exception e1) {
            String msg = "Error in executing the throttling rules in manager.";
            log.error(msg + " tenantId: " + tenantId + ".", e1);
            throw new UserStoreException(msg, e1);
        }
        TenantThrottlingInfo throttlingInfo = ThrottlingAgentServiceComponent.getThrottlingAgent()
                .getThrottlingInfoCache().getTenantThrottlingInfo(tenantId);
        if(throttlingInfo!=null){
            ThrottlingActionInfo actionInfo = throttlingInfo.getThrottlingActionInfo(StratosConstants.THROTTLING_ADD_USER_ACTION);

            if (actionInfo!=null && actionInfo.isBlocked()) {
                String blockedMsg = actionInfo.getMessage();
                String msg = "The add user action is blocked. message: " + blockedMsg + ".";
                log.error(msg);
                // we are only throwing the blocked exception, as it is a error message for the user
                throw new UserStoreException(blockedMsg);
            }
        }
        return true;
    }
}
