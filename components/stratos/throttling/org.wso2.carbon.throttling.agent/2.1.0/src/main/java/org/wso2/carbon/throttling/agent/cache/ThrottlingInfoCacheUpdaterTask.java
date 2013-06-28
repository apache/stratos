/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.throttling.agent.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.MeteringAccessValidationUtils;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import java.util.Properties;
import java.util.Set;

public class ThrottlingInfoCacheUpdaterTask implements Runnable {
    private static final Log log = LogFactory.getLog(ThrottlingInfoCacheUpdaterTask.class);

    private ThrottlingInfoCache cache;
    private UserRegistry governanceSystemRegistry;

    public ThrottlingInfoCacheUpdaterTask(ThrottlingInfoCache cache, UserRegistry governanceSystemRegistry) {
        this.cache = cache;
        this.governanceSystemRegistry = governanceSystemRegistry;
    }

    public void run() {
        log.info("Running throttling info cache updater task");
        Set<Integer> activeTenants = cache.getActiveTenants();
        for (Integer tenant : activeTenants) {
            String tenantValidationInfoResourcePath =
                    StratosConstants.TENANT_USER_VALIDATION_STORE_PATH +
                            RegistryConstants.PATH_SEPARATOR + tenant;
            try {
                if (governanceSystemRegistry.resourceExists(tenantValidationInfoResourcePath)) {
                    Resource tenantValidationInfoResource =
                            governanceSystemRegistry.get(tenantValidationInfoResourcePath);
                    Properties properties = tenantValidationInfoResource.getProperties();
                    Set<String> actions = MeteringAccessValidationUtils.getAvailableActions(properties);
                    for (String action : actions) {
                        String blocked =
                                tenantValidationInfoResource.getProperty(MeteringAccessValidationUtils
                                        .generateIsBlockedPropertyKey(action));
                        if(log.isDebugEnabled()){
                            log.debug("Action: " + action + " blocked: " + blocked + " tenant: " + tenant);
                        }

                        String blockMessage =
                                tenantValidationInfoResource.getProperty(MeteringAccessValidationUtils
                                        .generateErrorMsgPropertyKey(action));
                        TenantThrottlingInfo tenantThrottlingInfo = cache.getTenantThrottlingInfo(tenant);

                        tenantThrottlingInfo.updateThrottlingActionInfo(action,
                                new ThrottlingActionInfo("true".equals(blocked), blockMessage));
                    }
                }
            } catch (RegistryException re) {
                String msg =
                        "Error while getting throttling info for tenant " + tenant + ".";
                log.error(msg, re);
            }
        }
    }
}
