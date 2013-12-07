/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.theme.mgt.util;

import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.listeners.TenantMgtListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ThemeLoadingListener implements TenantMgtListener {
    private static final Log log = LogFactory.getLog(ThemeLoadingListener.class);
    private static final int EXEC_ORDER = 10;
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
        try {
            ThemeUtil.loadTheme(tenantInfo.getTenantId());
        } catch (RegistryException e) {
            String msg = "Error in loading the theme for the tenant: " 
                + tenantInfo.getTenantDomain() + ".";
            log.error(msg, e);
            throw new StratosException(msg, e);
        }
    }
    
    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
        // do nothing
    }

    public void onTenantDelete(int tenantId) {
        // do nothing
    }

    public void onTenantRename(int tenantId, String oldDomainName,
                             String newDomainName) throws StratosException {
        // do nothing
    }

    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
        // do nothing
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // do nothing
        
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // do nothing
        
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, 
                                         String newPlan) throws StratosException {
        // do nothing
    }
}
