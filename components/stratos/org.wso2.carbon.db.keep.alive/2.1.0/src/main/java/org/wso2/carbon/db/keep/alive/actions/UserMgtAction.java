/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.db.keep.alive.actions;

import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.AuthorizationManager;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

public class UserMgtAction {
    UserRealm realm;
    public UserMgtAction(RegistryService registryService) throws RegistryException {
        realm = registryService.getUserRealm(0);
    }
    public void execute() throws Exception {
        UserStoreManager userStoreManager = realm.getUserStoreManager();
        if (!userStoreManager.isExistingUser("tracker")) {
            userStoreManager.addUser("tracker", "tracker123", null, null, null, false);
            Permission[] permisions = new Permission[] { new Permission("high security", "read")};
            userStoreManager.addRole("tracker_role", new String[] {"tracker"}, permisions);
        }
        userStoreManager.updateCredentialByAdmin("tracker", "tracker123");
        // do some authorizations
        AuthorizationManager authManager = realm.getAuthorizationManager();
        authManager.authorizeRole("tracker_role", "tracker_obj", "read");
    }
}
