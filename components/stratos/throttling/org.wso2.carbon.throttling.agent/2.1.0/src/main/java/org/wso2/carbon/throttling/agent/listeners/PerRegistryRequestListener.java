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
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.Handler;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerManager;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.Filter;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.URLMatcher;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.throttling.agent.cache.TenantThrottlingInfo;
import org.wso2.carbon.throttling.agent.cache.ThrottlingActionInfo;
import org.wso2.carbon.throttling.agent.internal.ThrottlingAgentServiceComponent;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class PerRegistryRequestListener extends Handler {

    private static final Log log = LogFactory.getLog(PerRegistryRequestListener.class);

    @Override
    public void put(RequestContext context) throws RegistryException {
        validateRegistryAction(StratosConstants.THROTTLING_IN_DATA_ACTION);
    }

    @Override
    public void importResource(RequestContext context) throws RegistryException {
        validateRegistryAction(StratosConstants.THROTTLING_IN_DATA_ACTION);
    }

    @Override
    public Resource get(RequestContext context) throws RegistryException {
        validateRegistryAction(StratosConstants.THROTTLING_OUT_DATA_ACTION);
        return null;
    }

    @Override
    public void dump(RequestContext requestContext) throws RegistryException {
        validateRegistryAction(StratosConstants.THROTTLING_OUT_DATA_ACTION);
    }

    @Override
    public void restore(RequestContext requestContext) throws RegistryException {
        validateRegistryAction(StratosConstants.THROTTLING_IN_DATA_ACTION);
    }

    private void validateRegistryAction(String action) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID
                || CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return;
        }

        // called only once per request..
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_ACTION_VALIDATED_SESSION_ATTR) != null) {
            return;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_ACTION_VALIDATED_SESSION_ATTR, true);

        int tenantId = CurrentSession.getTenantId();

        TenantThrottlingInfo tenantThrottlingInfo =
                ThrottlingAgentServiceComponent.getThrottlingAgent().getThrottlingInfoCache()
                        .getTenantThrottlingInfo(tenantId);
        if(tenantThrottlingInfo!=null){
            ThrottlingActionInfo actionInfo = tenantThrottlingInfo.getThrottlingActionInfo(action);

            if (actionInfo != null && actionInfo.isBlocked()) {
                String blockedMsg = actionInfo.getMessage();
                String msg =
                        "The throttling action is blocked. message: " + blockedMsg + ", action: " +
                                action + ".";
                log.error(msg);
                // we are only throwing the blocked exception, as it is a error
                // message for the user
                throw new RegistryException(blockedMsg);
            }
        }
    }

    public static void registerPerRegistryRequestListener(RegistryContext registryContext) {
        HandlerManager handlerManager = registryContext.getHandlerManager();
        PerRegistryRequestListener storeBandwidthHandler = new PerRegistryRequestListener();
        URLMatcher anyUrlMatcher = new URLMatcher();
        anyUrlMatcher.setPattern(".*");
        String[] applyingFilters =
                new String[] { Filter.PUT, Filter.IMPORT, Filter.GET, Filter.DUMP, Filter.RESTORE, };

        handlerManager.addHandlerWithPriority(
                applyingFilters, anyUrlMatcher, storeBandwidthHandler);
    }
}
