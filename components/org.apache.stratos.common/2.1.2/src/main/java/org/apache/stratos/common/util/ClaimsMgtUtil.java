/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.stratos.common.util;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/*
 * This class handles the parameters that are input during the registration
 * which later are
 * stored as claims.
 * 
 * Current claims are:
 * First Name - GIVEN_NAME
 * Last Name - SURNAME
 */
public class ClaimsMgtUtil {

    private static final Log log = LogFactory.getLog(ClaimsMgtUtil.class);

    /* Methods to get the first and last names from the getNames() */
    /**
     * gets first name
     * 
     * @param realmService realmService
     * @param tenantId
     *            tenant id
     * @return first name
     * @throws Exception
     *             if error in getting the first name
     */
    public static String getFirstName(RealmService realmService, int tenantId)throws Exception {
        String names[] = getNames(realmService, tenantId);
        return names[0];
    }

    /**
     * gets last name
     * 
     * @param tenant
     *            tenant
     * @param tenantId
     *            tenant id
     * @return last name
     * @throws Exception
     *             if error in getting the last name
     */
    public static String getLastName(RealmService realmService, int tenantId) throws Exception {
        String names[] = getNames(realmService, tenantId);
        return names[1];
    }

    /**
     * Gets the first name of a tenant admin to address him/her in the
     * notifications
     * 
     * @param tenant
     *            tenant
     * @param tenantId
     *            tenant Id
     * @return first name / calling name
     * @throws Exception
     *             if unable to retrieve the admin name
     */
    public static String[] getNames(RealmService realmService, int tenantId) throws Exception {
        String[] names = new String[2];
        String firstname = "", lastname = "";
        try {
            firstname = getFirstNamefromUserStoreManager(realmService, tenantId);
        } catch (Exception ignore) {
            if (log.isDebugEnabled()) {
                // Not exceptions,due to the existence of tenants with no full name.
                String msg = "Unable to get the firstname from the user store manager";
                log.debug(msg, ignore);
            }
        }
        if (firstname != null && !firstname.trim().equals("")) {
            lastname = getLastNamefromUserStoreManager(realmService, tenantId);
            if ((lastname != null) && (!lastname.trim().equals(""))) {
                names[0] = firstname;
                names[1] = lastname;
            } else {
                // no last name - fullname was considered givenname;
                names = getNamesfromFullName(realmService, firstname);
            }
        } else { 
            // Work around for old tenants - where even full name is not input.
            if (log.isDebugEnabled()) {
                log.debug("First name is not available");
            }
            try {
                firstname = getAdminUserNameFromTenantId(realmService, tenantId);
                names[0] = firstname;
                names[1] = lastname;
            } catch (Exception e) {
                String msg = "Unable to get the admin Name from the user store manager";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }
        return names;
    }

    /**
     * Method to get the name of the admin user given the tenant id
     * 
     * @param tenantId
     *            tenant id
     * @return admin user name
     * @throws Exception
     *             UserStoreException
     */
    public static String getAdminUserNameFromTenantId(RealmService realmService, int tenantId)
                                                                                              throws Exception {
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            return realmService.getBootstrapRealmConfiguration().getAdminUserName();
        }
        String tenantAdminName ="";
        try {
            if (realmService.getTenantManager().getTenant(tenantId) != null) {
                tenantAdminName = realmService.getTenantManager().getTenant(tenantId).getAdminName();
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Unable to retrieve the admin name for the tenant with the tenant Id: " +
                         tenantId;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        return tenantAdminName;
    }

    /**
     * Let's split the givenname into two.
     * 
     * @param fullName
     *            full name of the tenant admin
     * @return first name
     */
    public static String[] getNamesfromFullName(RealmService realmService, String fullName) {
        String[] names = new String[0];

        if (!fullName.trim().equals("")) {
            names = fullName.split(" ", 2); // split by space.
        }
        return names; // first entry as the calling name.
    }

    /**
     * Get the claims from the user store manager
     * 
     * @param tenant
     *            tenant information
     * @param tenantId
     *            tenantId
     * @param claim
     *            claim name
     * @return claim value
     * @throws org.wso2.carbon.user.core.UserStoreException
     *             exception in getting the user store manager
     */
    public static String getClaimfromUserStoreManager(RealmService realmService, int tenantId,
                                                      String claim) throws UserStoreException {
        UserStoreManager userStoreManager = null;
        String claimValue = "";
        try {
            if (realmService.getTenantUserRealm(tenantId) != null) {
                userStoreManager =
                        (UserStoreManager) realmService.getTenantUserRealm(tenantId)
                                .getUserStoreManager();
            }

        } catch (Exception e) {
            String msg = "Error retrieving the user store manager for the tenant";
            log.error(msg, e);
            throw new UserStoreException(msg, e);
        }
        try {
            if (userStoreManager != null) {
                claimValue =
                        userStoreManager.getUserClaimValue(
                                getAdminUserNameFromTenantId(realmService, tenantId), claim,
                                UserCoreConstants.DEFAULT_PROFILE);
            }
            return claimValue;
        } catch (Exception e) {
            String msg = "Unable to retrieve the claim for the given tenant";
            log.error(msg, e);
            throw new UserStoreException(msg, e);
        }
    }

    /**
     * Gets first name from the user store manager
     * 
     * @param tenant
     *            tenant
     * @param tenantId
     *            tenant id
     * @return first name
     * @throws UserStoreException
     *             , if error in getting the claim GIVEN_NAME
     */
    public static String getFirstNamefromUserStoreManager(RealmService realmService,
                                                          int tenantId) throws UserStoreException {
        try {
            return getClaimfromUserStoreManager(realmService, tenantId,
                                                UserCoreConstants.ClaimTypeURIs.GIVEN_NAME);
        } catch (Exception e) {
            String msg = "First Name not found for the tenant";
            log.debug(msg, e);
            return ""; // returns empty string
        }
    }

    /**
     * Gets last name from the user store manager
     * 
     * @param tenant
     *            tenant
     * @param tenantId
     *            tenant id
     * @return last name
     * @throws UserStoreException
     *             , if error in getting the claim SURNAME
     */
    public static String getLastNamefromUserStoreManager(RealmService realmService,
                                                         int tenantId) throws UserStoreException {
        String lastname = "";
        try {
            lastname = getClaimfromUserStoreManager(realmService, tenantId,
                                                    UserCoreConstants.ClaimTypeURIs.SURNAME);
        } catch (Exception e) {
            String msg = "Last Name not found for the tenant";
            log.debug(msg, e);
        }
        return lastname; // returns empty string, if couldn't get last name from
                         // userStore manager
    }

}

