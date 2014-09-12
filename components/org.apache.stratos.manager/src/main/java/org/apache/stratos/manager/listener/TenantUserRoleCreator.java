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

package org.apache.stratos.manager.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.listeners.TenantMgtListener;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;


public class TenantUserRoleCreator implements TenantMgtListener {

    private transient static final Log log = LogFactory.getLog(TenantUserRoleCreator.class);
    private static String role = "Internal/user";

    /**
     * Create an 'user' role at tenant creation time
     * @param tenantInfo
     * @throws StratosException
     */
    @Override
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {

            try {

                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantDomain(tenantInfo.getTenantDomain());
                carbonContext.setTenantId(tenantInfo.getTenantId());

                UserRealm userRealm = DataHolder.getRealmService().getTenantUserRealm(tenantInfo.getTenantId());
                UserStoreManager manager = userRealm.getUserStoreManager();

                if (!manager.isExistingRole(role)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating new role: " + role);
                    }

                    Permission[] TenantUserPermissions = new Permission[]{  new Permission(CartridgeConstants.Permissions.VIEW_AUTOSCALING_POLICY, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_DEPLOYMENT_POLICY, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_CARTRIDGE, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_SERVICE, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_SUBSCRIPTION, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_DOMAIN, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_CLUSTER, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_INSTANCE, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.VIEW_KUBERNETES, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.ADD_GIT_SYNC, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.ADD_SUBSCRIPTION, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.ADD_DOMAIN, UserMgtConstants.EXECUTE_ACTION),
                                                                            new Permission(CartridgeConstants.Permissions.REST_LOGIN, UserMgtConstants.EXECUTE_ACTION),
                    };

                    String[] userList = new String[]{};
                    manager.addRole(role, userList, TenantUserPermissions);
                }

            } catch (UserStoreException e) {
                log.error("Error while creating the role: " + role + " - " +
                          e.getMessage());
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }

        }


    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {

    }

    @Override
    public void onTenantDelete(int tenantId) {

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
