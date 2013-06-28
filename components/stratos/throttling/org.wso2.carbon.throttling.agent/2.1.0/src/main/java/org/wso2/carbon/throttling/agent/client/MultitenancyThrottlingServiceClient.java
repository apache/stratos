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
package org.wso2.carbon.throttling.agent.client;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.authenticator.proxy.AuthenticationAdminClient;
import org.wso2.carbon.throttling.agent.internal.ThrottlingAgentServiceComponent;
import org.wso2.carbon.throttling.agent.stub.services.MultitenancyThrottlingServiceStub;

public class MultitenancyThrottlingServiceClient implements ThrottlingRuleInvoker {
    MultitenancyThrottlingServiceStub stub;

    public MultitenancyThrottlingServiceClient(String serverUrl, String userName, String password)
            throws Exception {
        stub =
                new MultitenancyThrottlingServiceStub(ThrottlingAgentServiceComponent.getThrottlingAgent().getConfigurationContextService()
                        .getClientConfigContext(), serverUrl + "MultitenancyThrottlingService");
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        String cookie = login(serverUrl, userName, password);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public static String login(String serverUrl, String userName, String password) throws Exception {
        String sessionCookie = null;
        try {
            AuthenticationAdminClient client =
                    new AuthenticationAdminClient(ThrottlingAgentServiceComponent.getThrottlingAgent().getConfigurationContextService()
                            .getClientConfigContext(), serverUrl, null, null, false);
            // TODO : get the correct IP
            boolean isLogin = client.login(userName, password, "127.0.0.1");
            if (isLogin) {
                sessionCookie = client.getAdminCookie();
            }
        } catch (Exception e) {
            throw new Exception("Error in login to throttling manager. server: " + serverUrl +
                    "username: " + userName + ".", e);
        }
        return sessionCookie;
    }


    public void executeThrottlingRules(int tenantId) throws Exception {
        stub.executeThrottlingRules(tenantId);
    }

}
