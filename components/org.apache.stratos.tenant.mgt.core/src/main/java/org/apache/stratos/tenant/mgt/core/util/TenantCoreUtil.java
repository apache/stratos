package org.apache.stratos.tenant.mgt.core.util;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.apache.stratos.tenant.mgt.core.internal.TenantMgtCoreServiceComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.AuthenticationObserver;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.apache.stratos.common.constants.StratosConstants;


/**
 * Tenant Core Util class - used by any service that needs to create a tenant.
 */
public class TenantCoreUtil {
    
    private static final Log log = LogFactory.getLog(TenantCoreUtil.class);

    /**
     * Initializes the registry for the tenant.
     * 
     * @param tenantId
     *            tenant id.
     */
    public static void initializeRegistry(int tenantId) {
        BundleContext bundleContext = TenantMgtCoreServiceComponent.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                                     new ServiceTracker(bundleContext,
                                                        AuthenticationObserver.class.getName(),
                                                        null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).startedAuthentication(tenantId);
                }
            }
            tracker.close();
        }
    }

    /**
     * Setting the Originated
     * @param tenantId - tenant Id
     * @param originatedService - The Service from where the tenant registration was originated.
     * @throws Exception, Registry Exception, if error in putting the originated Service resource
     * to the governance registry.
     */
    public static void setOriginatedService(int tenantId,
                                            String originatedService) throws Exception {
        if (originatedService != null) { 
            String originatedServicePath =
                                           StratosConstants.ORIGINATED_SERVICE_PATH +
                                                   StratosConstants.PATH_SEPARATOR +
                                                   StratosConstants.ORIGINATED_SERVICE +
                                                   StratosConstants.PATH_SEPARATOR + tenantId;
            try {
                Resource origServiceRes = TenantMgtCoreServiceComponent.
                        getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID).newResource();
                origServiceRes.setContent(originatedService);
                TenantMgtCoreServiceComponent.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID).
                        put(originatedServicePath, origServiceRes);
            } catch (RegistryException e) {
                String msg = "Error in putting the originated service resource " +
                             "to the governance registry";
                log.error(msg, e);
                throw new RegistryException(msg, e);
            }
        }
    }

}
