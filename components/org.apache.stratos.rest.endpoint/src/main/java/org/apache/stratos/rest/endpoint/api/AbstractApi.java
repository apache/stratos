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
package org.apache.stratos.rest.endpoint.api;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * The abstract class for service beans. If the admin service want to get {@link ConfigurationContext} etc,
 * they should acquire them through this class' methods.
 */
public class AbstractApi {
    private static Log log = LogFactory.getLog(AbstractApi.class);

    protected ConfigurationContext getConfigContext() {

        // If a tenant has been set, then try to get the ConfigurationContext of that tenant
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        ConfigurationContextService configurationContextService =
                (ConfigurationContextService) carbonContext.getOSGiService(ConfigurationContextService.class);
        ConfigurationContext mainConfigContext = configurationContextService.getServerConfigContext();
        String domain = carbonContext.getTenantDomain();
        if (domain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domain)) {
            return TenantAxisUtils.getTenantConfigurationContext(domain, mainConfigContext);
        } else if (carbonContext.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            return mainConfigContext;
        } else {
            throw new UnsupportedOperationException("Tenant domain unidentified. " +
                    "Upstream code needs to identify & set the tenant domain & tenant ID. " +
                    " The TenantDomain SOAP header could be set by the clients or " +
                    "tenant authentication should be carried out.");
        }
    }

    protected String getTenantDomain(){
        return CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    protected String getUsername(){
        return CarbonContext.getThreadLocalCarbonContext().getUsername();
    }
    
    protected int getTenantId(){
        return CarbonContext.getThreadLocalCarbonContext().getTenantId();
    }
}
