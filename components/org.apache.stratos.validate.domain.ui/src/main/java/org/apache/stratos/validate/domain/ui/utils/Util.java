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
package org.apache.stratos.validate.domain.ui.utils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.apache.stratos.validate.domain.ui.clients.ValidateDomainAdminClient;
import org.apache.stratos.validate.domain.ui.clients.ValidateDomainClient;
import org.apache.stratos.validate.domain.ui.clients.ValidateDomainNonAdminClient;

public class Util {
    public static ValidateDomainClient getValidateDomainClient(ServletRequest request,
            ServletConfig config, HttpSession session) throws RegistryException {
        // this doesn't make any security hole, as even a not-logged-in user try
        // manually put status parameter to logged_in, still the back-end service
        // try to validate him and he will fail.
        
        String status = request.getParameter("status");
        if ("logged_in".equals(status)) {
            return new ValidateDomainAdminClient(config, session);
        }
        return new ValidateDomainNonAdminClient(config, session);
    }
}
