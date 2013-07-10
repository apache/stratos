/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.packages.PackageInfoHolder;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.common.util.StratosConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;

/**
 * @scr.component name="apache.stratos.common" immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 */
public class CloudCommonServiceComponent {

    private static Log log = LogFactory.getLog(CloudCommonServiceComponent.class);

    private static BundleContext bundleContext;
    private static RealmService realmService;
    private static RegistryService registryService;
    private static PackageInfoHolder packageInfos;

    protected void activate(ComponentContext context) {
        try {
            bundleContext = context.getBundleContext();
            if (CommonUtil.getStratosConfig() == null) {
                StratosConfiguration stratosConfig = CommonUtil.loadStratosConfiguration();
                CommonUtil.setStratosConfig(stratosConfig);
            }

            // Loading the EULA
            if (CommonUtil.getEula() == null) {
                String eula = CommonUtil.loadTermsOfUsage();
                CommonUtil.setEula(eula);
            }
            
			packageInfos = new PackageInfoHolder();
			context.getBundleContext().registerService(
					PackageInfoHolder.class.getName(), packageInfos, null);

            //Register manager configuration OSGI service
            try {
                StratosConfiguration stratosConfiguration = CommonUtil.loadStratosConfiguration();
                bundleContext.registerService(StratosConfiguration.class.getName(), stratosConfiguration, null);
                if (log.isDebugEnabled()) {
                    log.debug("******* Cloud Common Service bundle is activated ******* ");
                }
            } catch (Exception ex) {
                String msg = "An error occurred while initializing Cloud Common Service as an OSGi Service";
                log.error(msg, ex);
            }
        } catch (Throwable e) {
            log.error("Error in activating Cloud Common Service Component" + e.toString());
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Tenant Core bundle is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        CloudCommonServiceComponent.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {
        setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        CloudCommonServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        setRealmService(null);
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }


    public static RealmService getRealmService() {
        return realmService;
    }

    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static UserRegistry getGovernanceSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getGovernanceSystemRegistry(tenantId);
    }

    public static UserRegistry getConfigSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getConfigSystemRegistry(tenantId);
    }

	public static PackageInfoHolder getPackageInfos() {
		return packageInfos;
	}

    

}
