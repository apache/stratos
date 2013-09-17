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
package org.apache.stratos.tenant.activity.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.apache.stratos.common.util.StratosConfiguration;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.stratos.tenant.activity.ui" immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.stratos.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="stratos.config.service"
 * interface="org.apache.stratos.common.util.StratosConfiguration" cardinality="1..1"
 * policy="dynamic" bind="setStratosConfigurationService" unbind="unsetStratosConfigurationService"
 */
public class TenantActivityUIServiceComponent {
    private static Log log = LogFactory.getLog(TenantActivityUIServiceComponent.class);
    public static ConfigurationContextService contextService;
    public static StratosConfiguration stratosConfiguration;

    protected void activate(ComponentContext context) {
        try {
            if (log.isDebugEnabled()) {
                log.error("******* Tenant Activity UI bundle is activated ******* ");
            }
        } catch (Throwable e) {
            log.error("******* Error in activating Tenant Activity UI bundle ******* ", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("******* Tenant Activity UI bundle is deactivated ******* ");
        }
    }

    protected void setStratosConfigurationService(StratosConfiguration stratosConfigService) {
        TenantActivityUIServiceComponent.stratosConfiguration = stratosConfigService;
    }

    protected void unsetStratosConfigurationService(StratosConfiguration ccService) {
        TenantActivityUIServiceComponent.stratosConfiguration = null;
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        TenantActivityUIServiceComponent.contextService = contextService;
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        TenantActivityUIServiceComponent.contextService = null;
    }
}
