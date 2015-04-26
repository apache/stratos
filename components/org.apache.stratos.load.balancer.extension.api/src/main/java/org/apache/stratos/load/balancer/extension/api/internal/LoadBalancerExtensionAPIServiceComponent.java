/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.load.balancer.extension.api.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.apache.stratos.load.balancer.extension.api.internal.LoadBalancerExtensionAPIServiceComponent" immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class LoadBalancerExtensionAPIServiceComponent {

    private static final Log log = LogFactory.getLog(LoadBalancerExtensionAPIServiceComponent.class);

    /**
     * Service component activate method.
     * @param context component context
     */
    protected void activate(ComponentContext context) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Load Balancer Extension API Service bundle activated");
            }
        } catch (Exception e) {
            log.error("Could not activate Load Balancer Extension API Service bundle", e);
        }
    }

    /**
     * Set configuration context service.
     * @param contextService
     */
    protected void setConfigurationContextService(ConfigurationContextService contextService) {
    }

    /**
     * Unset configuration context service.
     * @param contextService
     */
    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
    }
}
