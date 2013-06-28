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

import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * Axis configuration context observer which add and remove lazy loaded/unloaded tenants to throttling information
 * cache.
 */
public class Axis2ConfigurationContextObserverImpl extends AbstractAxis2ConfigurationContextObserver{

    private ThrottlingInfoCache throttlingInfoCache;

    public Axis2ConfigurationContextObserverImpl(ThrottlingInfoCache throttlingInfoCache){
        this.throttlingInfoCache = throttlingInfoCache;
    }

    public void createdConfigurationContext(ConfigurationContext configContext) {
        throttlingInfoCache.addTenant(MultitenantUtils.getTenantId(configContext));
    }

    public void terminatedConfigurationContext(ConfigurationContext configCtx) {
        throttlingInfoCache.deleteTenant(MultitenantUtils.getTenantId(configCtx));
    }
}
