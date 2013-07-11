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
package org.apache.stratos.status.monitor.core.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.stratos.status.monitor.core.util.StatusMonitorUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.carbon.status.monitor" immediate="true"
 * @scr.reference name="configuration.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"
 */
public class StatusMonitorCoreComponent {
    private static Log log = LogFactory.getLog(
            StatusMonitorCoreComponent.class);

    private static BundleContext bundleContext;
    private static ConfigurationContextService configurationContextService;

    protected void activate(ComponentContext context) {
        try {
            bundleContext = context.getBundleContext();
            if (StatusMonitorUtil.getStatusMonitorConfiguration() == null) {
                StatusMonitorUtil.initStatusMonitor(context.getBundleContext());
                if (log.isDebugEnabled()) {
                    log.debug("Status Monitor Agent initialized");
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("******* Status Monitor core bundle is activated ******* ");
            }
        } catch (Exception e) {
            log.error("******* Status Monitor Core bundle failed activating ****", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Status Monitor core bundle is deactivated ******* ");
    }

    protected void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        log.debug("Receiving ConfigurationContext Service");
        StatusMonitorCoreComponent.
                configurationContextService = configurationContextService;
    }

    protected void unsetConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        log.debug("Unsetting ConfigurationContext Service");
        setConfigurationContextService(null);
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    public static ConfigurationContext getConfigurationContext() {
        if (configurationContextService.getServerConfigContext() == null) {
            return null;
        }
        return configurationContextService.getServerConfigContext();
    }
}
