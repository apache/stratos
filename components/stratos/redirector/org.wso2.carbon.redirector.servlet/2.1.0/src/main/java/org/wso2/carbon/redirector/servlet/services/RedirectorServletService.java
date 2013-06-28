/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.redirector.servlet.services;

import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.redirector.servlet.util.Util;

public class RedirectorServletService {
    public String validateTenant(String tenantDomain) throws Exception {
        TenantManager tenantManager = Util.getTenantManager();
        int tenantId = tenantManager.getTenantId(tenantDomain);
        if (tenantId <= 0) {
            return StratosConstants.INVALID_TENANT;
        } else if (Util.getActivationService() != null &&
                !Util.getActivationService().isActive(tenantId)) {
            return StratosConstants.INACTIVE_TENANT;
        }
        return StratosConstants.ACTIVE_TENANT;
    }
}
