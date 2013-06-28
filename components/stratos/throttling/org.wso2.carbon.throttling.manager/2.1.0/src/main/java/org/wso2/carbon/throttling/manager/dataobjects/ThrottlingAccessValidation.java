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
package org.wso2.carbon.throttling.manager.dataobjects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ThrottlingAccessValidation {

    Map<String, Boolean> userBlockedActions = new HashMap<String, Boolean>();
    Map<String, Boolean> tenantBlockedActions = new HashMap<String, Boolean>();
    Map<String, String> userBlockedMsgs = new HashMap<String, String>();
    Map<String, String> tenantBlockedMsgs = new HashMap<String, String>();

    boolean persistValidationInfo = true;

    public boolean isPersistValidationInfo() {
        return persistValidationInfo;
    }

    public void setPersistValidationInfo(boolean persistValidationInfo) {
        this.persistValidationInfo = persistValidationInfo;
    }

    public boolean isUserBlocked(String action) {
        Boolean result = userBlockedActions.get(action);
        return result == null? false: result;
    }

    public String getUserBlockedMsg(String action) {
        return userBlockedMsgs.get(action);
    }

    public void setUserBlocked(String action, boolean block, String msg) {
        userBlockedActions.put(action, block);
        userBlockedMsgs.put(action, msg);
    }

    public boolean isTenantBlocked(String action) {
        Boolean result = tenantBlockedActions.get(action);
        return result == null? false: result;
    }

    public String getTenantBlockedMsg(String action) {
        return tenantBlockedMsgs.get(action);
    }

    public void setTenantBlocked(String action, boolean block, String msg) {
        tenantBlockedActions.put(action, block);
        tenantBlockedMsgs.put(action, msg);
    }

    public Set<String> getActions() {
    	return tenantBlockedActions.keySet();
    }
}
