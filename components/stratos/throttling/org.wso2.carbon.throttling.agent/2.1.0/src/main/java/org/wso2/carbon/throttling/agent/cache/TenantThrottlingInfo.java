/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.throttling.agent.cache;

import org.wso2.carbon.stratos.common.constants.StratosConstants;

import java.util.HashMap;
import java.util.Map;

public class TenantThrottlingInfo {
    private Map<String, ThrottlingActionInfo> throttlingActionInfoMap = new HashMap<String, ThrottlingActionInfo>();

    public ThrottlingActionInfo getThrottlingActionInfo(String action) {
        if(throttlingActionInfoMap.get(action) != null){
            return throttlingActionInfoMap.get(action);
        }
        return new ThrottlingActionInfo(false, "");
    }

    public ThrottlingActionInfo getThrottlingActionInfo(String[] actions) {
        ThrottlingActionInfo actionInfo = throttlingActionInfoMap.get(StratosConstants.THROTTLING_ALL_ACTION);
        if (actionInfo != null && actionInfo.isBlocked()) {
            return actionInfo;
        }

        for (String action : actions) {
            actionInfo = throttlingActionInfoMap.get(action);
            if (actionInfo != null && actionInfo.isBlocked()) {
                return actionInfo;
            }
        }

        return new ThrottlingActionInfo(false, "");
    }

    public void updateThrottlingActionInfo(String action, ThrottlingActionInfo throttlingActionInfo) {
        throttlingActionInfoMap.put(action, throttlingActionInfo);
    }
}
