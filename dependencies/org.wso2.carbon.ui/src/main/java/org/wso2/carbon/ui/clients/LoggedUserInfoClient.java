/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.common.LoggedUserInfo;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfoAdminStub;

public class LoggedUserInfoClient {

    private static final Log log = LogFactory.getLog(LoggedUserInfoClient.class);
    private LoggedUserInfoAdminStub stub;

    public LoggedUserInfoClient(ConfigurationContext ctx, String serverURL, String cookie)
            throws AxisFault {
        String serviceEPR = serverURL + "LoggedUserInfoAdmin";
        stub = new LoggedUserInfoAdminStub(ctx, serviceEPR);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        if (cookie != null) {
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        }
    }

    public LoggedUserInfo getUserInfo() throws Exception {
        try {
            return getLoggedUserInfo(stub.getUserInfo());
        } catch (Exception e) {
            String msg = "Error occurred while getting system permissions of user";
            log.error(msg, e);
            throw new AuthenticationException(msg, e);
        }
    }

    private LoggedUserInfo getLoggedUserInfo(
            org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfo userInfo) {
        LoggedUserInfo loggedUserInfo = new LoggedUserInfo();
        loggedUserInfo.setUIPermissionOfUser(userInfo.getUIPermissionOfUser());
        loggedUserInfo.setPasswordExpiration(userInfo.getPasswordExpiration());
        return loggedUserInfo;
    }

}
