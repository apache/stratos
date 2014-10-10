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

package org.apache.stratos.manager.utils;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.user.mgt.exception.UserManagerException;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;

public class UserRoleCreator {

    private static final Log log = LogFactory.getLog(UserRoleCreator.class);

    /**
     * Creating a Internal/user Role at Carbon Server Start-up
     */
    public static void createTenantUserRole(UserStoreManager manager) throws UserManagerException {

        String userRole = "Internal/user";

        try {
            if (!manager.isExistingRole(userRole)) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating new role: " + userRole);
                }
                //Set permissions to the Internal/user role
                Permission[] tenantUserPermissions = new Permission[]{new Permission(PermissionConstants.VIEW_AUTOSCALING_POLICY, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_DEPLOYMENT_POLICY, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_CARTRIDGE, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_SERVICE, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_SUBSCRIPTION, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_DOMAIN, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_CLUSTER, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_INSTANCE, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.VIEW_KUBERNETES, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.ADD_GIT_SYNC, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.ADD_SUBSCRIPTION, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.ADD_DOMAIN, UserMgtConstants.EXECUTE_ACTION),
                                                                      new Permission(PermissionConstants.REST_LOGIN, UserMgtConstants.EXECUTE_ACTION),
                };

                String[] userList = new String[]{};
                manager.addRole(userRole, userList, tenantUserPermissions);
            }
        } catch (UserStoreException e) {
            String msg = "Error while creating the role: " + userRole;
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }
    }
}
