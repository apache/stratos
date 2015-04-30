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
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Class provides the operations related to adding/updating/deleting and listing users using
 * the carbon UserStoreManager in the particular tenant space
 */
public class StratosUserManagerUtils {

    private transient static final Log log = LogFactory.getLog(StratosUserManagerUtils.class);
    private static final String INTERNAL_EVERYONE_ROLE = "Internal/everyone";
    private static final String GET_ALL_USERS_WILD_CARD = "*";

    /**
     * Add a user to the user-store of the particular tenant
     *
     * @param userStoreManager UserStoreManager
     * @param userInfoBean     UserInfoBean
     * @throws UserManagerException
     */
    public static void addUser(UserStoreManager userStoreManager, UserInfoBean userInfoBean)
            throws UserManagerException {

        if (log.isDebugEnabled()) {
            log.debug("Creating new User: " + userInfoBean.getUserName());
        }

        String[] roles = new String[1];
        roles[0] = userInfoBean.getRole();
        Map<String, String> claims = new HashMap<String, String>();

        //set firstname, lastname and email as user claims
        claims.put(UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, userInfoBean.getEmail());
        claims.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, userInfoBean.getFirstName());
        claims.put(UserCoreConstants.ClaimTypeURIs.SURNAME, userInfoBean.getLastName());

        try {
            userStoreManager.addUser(userInfoBean.getUserName(), userInfoBean.getCredential(), roles, claims, userInfoBean.getProfileName());
        } catch (UserStoreException e) {
            String msg = "Error in adding user " + userInfoBean.getUserName() + " to User Store";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }

    }

    /**
     * Delete the user with the given username in the relevant tenant space
     *
     * @param userStoreManager UserStoreManager
     * @param userName         UserName
     * @throws UserManagerException
     */
    public static void removeUser(UserStoreManager userStoreManager, String userName)
            throws UserManagerException {

        try {
            if (userStoreManager.isExistingUser(userName)) {
                userStoreManager.deleteUser(userName);
            } else {
                String msg = "Requested user " + userName + " does not exist";
                throw new UserManagerException(msg);
            }
        } catch (UserStoreException e) {
            String msg = "Error in deleting the user " + userName + " from User Store";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }
    }


    /**
     * Updates the user info given the new UserInfoBean
     *
     * @param userStoreManager UserStoreManager
     * @param userInfoBean     UserInfoBean
     * @throws UserManagerException
     */
    public static void updateUser(UserStoreManager userStoreManager, UserInfoBean userInfoBean)
            throws UserManagerException {

        try {
            if (userStoreManager.isExistingUser(userInfoBean.getUserName())) {
                if (log.isDebugEnabled()) {
                    log.debug("Updating User " + userInfoBean.getUserName());
                }

                String[] newRoles = new String[1];
                newRoles[0] = userInfoBean.getRole();

                userStoreManager.updateRoleListOfUser(userInfoBean.getUserName(), getRefinedListOfRolesOfUser(userStoreManager, userInfoBean.getUserName()), newRoles);
                userStoreManager.setUserClaimValue(userInfoBean.getUserName(), UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, userInfoBean.getEmail(), userInfoBean.getProfileName());
                userStoreManager.setUserClaimValue(userInfoBean.getUserName(), UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, userInfoBean.getFirstName(), userInfoBean.getProfileName());
                userStoreManager.setUserClaimValue(userInfoBean.getUserName(), UserCoreConstants.ClaimTypeURIs.SURNAME, userInfoBean.getLastName(), userInfoBean.getProfileName());
                userStoreManager.updateCredentialByAdmin(userInfoBean.getUserName(), userInfoBean.getCredential());
            } else {
                String msg = "Requested user " + userInfoBean.getUserName() + " does not exist";
                throw new UserManagerException(msg);
            }
        } catch (UserStoreException e) {
            String msg = "Error in updating the user " + userInfoBean.getUserName() + " in User Store";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }
    }

    /**
     * Get a List of usernames and associated Roles as a UserInfoBean
     *
     * @param userStoreManager UserStoreManager
     * @return List<UserInfoBean>
     * @throws UserManagerException
     */
    public static List<UserInfoBean> getAllUsers(UserStoreManager userStoreManager)
            throws UserManagerException {

        String[] users;
        List<UserInfoBean> userList = new ArrayList<UserInfoBean>();

        try {
            users = userStoreManager.listUsers(GET_ALL_USERS_WILD_CARD, -1);
        } catch (UserStoreException e) {
            String msg = "Error in listing the users in User Store";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }

        //Iterate through the list of users and retrieve their roles
        for (String user : users) {
            UserInfoBean userInfoBean = new UserInfoBean();
            userInfoBean.setUserName(user);
            userInfoBean.setRole(getRefinedListOfRolesOfUser(userStoreManager, user)[0]);
            userList.add(userInfoBean);
        }

        return userList;
    }

    /**
     * Get the List of userRoles except the Internal/everyone role
     *
     * @param userStoreManager UserStoreManager
     * @param username         Username of the user
     * @return String[]
     * @throws UserManagerException
     */
    private static String[] getRefinedListOfRolesOfUser(UserStoreManager userStoreManager, String username)
            throws UserManagerException {

        ArrayList<String> rolesWithoutEveryoneRole = new ArrayList<String>();

        try {
            String[] allUserRoles = userStoreManager.getRoleListOfUser(username);

            for (String role : allUserRoles) {
                if (!role.equals(INTERNAL_EVERYONE_ROLE)) {
                    rolesWithoutEveryoneRole.add(role);
                }
            }
            String[] rolesWithoutEveryoneRoleArray = new String[rolesWithoutEveryoneRole.size()];
            return rolesWithoutEveryoneRole.toArray(rolesWithoutEveryoneRoleArray);

        } catch (UserStoreException e) {
            String msg = "Error in listing the roles of user " + username + " in User Store";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }
    }
}
