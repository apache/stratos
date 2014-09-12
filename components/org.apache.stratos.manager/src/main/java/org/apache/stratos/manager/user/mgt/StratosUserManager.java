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

package org.apache.stratos.manager.user.mgt;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.user.mgt.beans.UserInfoBean;
import org.apache.stratos.manager.user.mgt.exception.UserManagementException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StratosUserManager {

    private transient static final Log log = LogFactory.getLog(StratosUserManager.class);
    private UserStoreManager userStoreManager;


    public StratosUserManager(UserStoreManager userStoreManager) {
        this.userStoreManager = userStoreManager;

    }

    /**
     * Add a user to the user store
     *
     * @param userInfoBean
     * @throws UserStoreException
     */
    public void addUser(UserInfoBean userInfoBean) throws UserManagementException {

        try {

            if (!userStoreManager.isExistingUser(userInfoBean.getUserName())) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating new User: " + userInfoBean.getUserName());
                }
            }
            String[] roles = new String[1];
            roles[0] = userInfoBean.getRole();
            Map<String, String> claims = new HashMap<String, String>();

            claims.put(UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, userInfoBean.getEmail());
            claims.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, userInfoBean.getFirstName());
            claims.put(UserCoreConstants.ClaimTypeURIs.SURNAME, userInfoBean.getLastName());
            userStoreManager.addUser(userInfoBean.getUserName(), userInfoBean.getCredential(), roles, claims, userInfoBean.getProfileName());

        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new UserManagementException(e.getMessage(), e);
        }

    }

    /**
     * Delete the user with the given user name
     *
     * @param userName The user name
     * @throws UserStoreException
     */
    public void deleteUser(String userName) throws UserManagementException {

        try {
            userStoreManager.deleteUser(userName);
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new UserManagementException(e.getMessage(), e);
        }
    }


    /**
     * Updates the user info
     *
     * @param userInfoBean
     */
    public void updateUser(UserInfoBean userInfoBean) throws UserManagementException {

        try {
            if (userStoreManager.isExistingUser(userInfoBean.getUserName())) {
                String[] newRoles = new String[1];
                newRoles[0] = userInfoBean.getRole();

                //String[] rolesToDelete = new String[1];
                ArrayList<String> rolesToDelete = new ArrayList<String>();
                String[] currentRolesList = userStoreManager.getRoleListOfUser(userInfoBean.getUserName());
                for(String role: currentRolesList){
                    if(!role.equals("Internal/everyone")){
                        rolesToDelete.add(role);
                    }
                }
                String[] rolesToDeleteArray = new String[rolesToDelete.size()];
                Map<String, String> claims = new HashMap<String, String>();

                claims.put(UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, userInfoBean.getEmail());
                claims.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, userInfoBean.getFirstName());
                claims.put(UserCoreConstants.ClaimTypeURIs.SURNAME, userInfoBean.getLastName());

                userStoreManager.updateRoleListOfUser(userInfoBean.getUserName(), rolesToDelete.toArray(rolesToDeleteArray), newRoles);
                userStoreManager.setUserClaimValue(userInfoBean.getUserName(),UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, userInfoBean.getEmail(),userInfoBean.getProfileName());
                userStoreManager.setUserClaimValue(userInfoBean.getUserName(),UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, userInfoBean.getFirstName(),userInfoBean.getProfileName());
                userStoreManager.setUserClaimValue(userInfoBean.getUserName(),UserCoreConstants.ClaimTypeURIs.SURNAME, userInfoBean.getLastName(),userInfoBean.getProfileName());
                userStoreManager.updateCredentialByAdmin(userInfoBean.getUserName(), userInfoBean.getCredential());

            }
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new UserManagementException(e.getMessage(), e);
        }

    }

}
