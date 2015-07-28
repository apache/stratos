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

package org.apache.stratos.manager.user.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.apache.stratos.manager.internal.ServiceReferenceHolder;
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.apache.stratos.manager.utils.UserRoleCreator;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;

/**
 * Listener for Tenant create event to create a new Role
 */
public class TenantUserRoleManager implements TenantMgtListener {

    private static final Log log = LogFactory.getLog(TenantUserRoleManager.class);

    /**
     * Create an 'user' role at tenant creation time
     *
     * @param tenantInfo TenantInfoBean
     * @throws org.wso2.carbon.stratos.common.exception.StratosException
     */
    @Override
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {

        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(tenantInfo.getTenantDomain());
            carbonContext.setTenantId(tenantInfo.getTenantId());
            //Get tenant UserRealm & UserStoreManager
            UserRealm userRealm = ServiceReferenceHolder.getRealmService().getTenantUserRealm(tenantInfo.getTenantId());
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            //Call Util class to create the user role

            UserRoleCreator.createInternalUserRole(userStoreManager);

        } catch (UserStoreException e) {
            String msg = "Error while retrieving the user store for tenant: " + tenantInfo.getTenantDomain();
            log.error(msg, e);
            throw new StratosException(e.getMessage(), e);
        } catch (UserManagerException e) {
            String msg = "Error while creating the user role in tenant: " + tenantInfo.getTenantDomain();
            log.error(msg, e);
            throw new StratosException(e.getMessage(), e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }


    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {

    }

    @Override
    public void onTenantRename(int tenantId, String oldDomainName, String newDomainName)
            throws StratosException {

    }

    @Override
    public void onTenantInitialActivation(int tenantId) throws StratosException {

    }

    @Override
    public void onTenantActivation(int tenantId) throws StratosException {

    }

    @Override
    public void onTenantDeactivation(int tenantId) throws StratosException {

    }

    @Override
    public void onSubscriptionPlanChange(int tenentId, String oldPlan, String newPlan)
            throws StratosException {

    }

    @Override
    public int getListenerOrder() {
        return 0;
    }
}
