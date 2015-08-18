/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.ui;

import javax.servlet.http.HttpServletRequest;

/**
 * An instance of this interface can be registered as an OSGi service, which will allow the Carbon
 * UI to extend its authentication process.
 */
public interface UIAuthenticationExtender {

    /**
     * Method that will be executed if the login was successful.
     *
     * @param request   the HTTP Request.
     * @param userName  the name of the logged in user.
     * @param domain    the tenant domain.
     * @param serverURL the URL of the BE server.
     */
    void onSuccessAdminLogin(HttpServletRequest request, String userName, String domain,
                                    String serverURL);

}
