/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
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

package org.apache.stratos.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.integration.common.StratosTestServerManager;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.common.extensions.StratosServerExtension;
import org.apache.stratos.integration.common.rest.RestClient;
import org.apache.stratos.mock.iaas.client.MockIaasApiClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.List;

public class StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(StratosIntegrationTest.class);
    protected AutomationContext stratosAutomationCtx;
    protected String adminUsername;
    protected String adminPassword;
    protected String stratosBackendURL;
    protected String stratosSecuredBackendURL;
    protected RestClient restClient;
    protected MockIaasApiClient mockIaasApiClient;
    public static final int GLOBAL_TEST_TIMEOUT = 5 * 60 * 1000; // 5 mins
    public static final int APPLICATION_TEST_TIMEOUT = 20 * 60 * 1000; // 20 mins

    public StratosIntegrationTest() {
        try {
            stratosAutomationCtx = new AutomationContext("STRATOS", "stratos-001", TestUserMode.SUPER_TENANT_ADMIN);
            adminUsername = stratosAutomationCtx
                    .getConfigurationValue("/automation/userManagement/superTenant/tenant/admin/user/userName");
            adminPassword = stratosAutomationCtx
                    .getConfigurationValue("/automation/userManagement/superTenant/tenant/admin/user/password");

            // Do not rely on automation context for context URLs since ports are dynamically picked
            stratosBackendURL = StratosServerExtension.getStratosTestServerManager().getWebAppURL();
            stratosSecuredBackendURL = StratosServerExtension.getStratosTestServerManager().getWebAppURLHttps();
            restClient = new RestClient(stratosBackendURL, stratosSecuredBackendURL, adminUsername, adminPassword);
            mockIaasApiClient = new MockIaasApiClient(stratosBackendURL + "/mock-iaas/api");
            // initialize topology handler before running the tests
            TopologyHandler.getInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize StratosIntegrationTest", e);
        }
    }
}
