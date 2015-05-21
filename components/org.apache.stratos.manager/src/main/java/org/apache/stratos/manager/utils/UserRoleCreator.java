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
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;

import java.util.ArrayList;
import java.util.List;

public class UserRoleCreator {

    private static final Log log = LogFactory.getLog(UserRoleCreator.class);

    /**
     * Creating Internal/user Role at Carbon Server Start-up
     */
    public static void createInternalUserRole(UserStoreManager userStoreManager) throws UserManagerException {
        String userRole = "Internal/user";
        try {
            if (!userStoreManager.isExistingRole(userRole)) {
                log.info("Creating internal user role: " + userRole);

                //Set permissions to the Internal/user role
                List<Permission> permissions = new ArrayList<Permission>();
                for (String permissionResourceId : PermissionConstants.STRATOS_PERMISSIONS) {
                    Permission permission = new Permission(permissionResourceId, UserMgtConstants.EXECUTE_ACTION);
                    permissions.add(permission);
                }
                String[] userList = new String[]{};
                userStoreManager.addRole(userRole, userList, permissions.toArray(new Permission[permissions.size()]));
            }
        } catch (UserStoreException e) {
            String msg = "Error while creating the role: " + userRole;
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }
    }
}
