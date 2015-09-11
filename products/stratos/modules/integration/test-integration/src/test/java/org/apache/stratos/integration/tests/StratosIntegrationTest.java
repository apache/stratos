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
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.extensions.StratosServerExtension;
import org.apache.stratos.integration.common.rest.IntegrationMockClient;
import org.apache.stratos.integration.common.rest.RestClient;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import static org.testng.Assert.assertEquals;

public class StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(StratosIntegrationTest.class);
    protected AutomationContext stratosAutomationCtx;
    protected String adminUsername;
    protected String adminPassword;
    protected String tenant1UserName;
    protected String tenant1Password;
    protected String tenant2UserName;
    protected String tenant2Password;
    protected String stratosBackendURL;
    protected RestClient restClientAdmin;
    protected RestClient restClientTenant1;
    protected RestClient restClientTenant2;
    protected int tenant1Id;
    protected int tenant2Id;
    protected IntegrationMockClient mockIaasApiClient;
    public static final int GLOBAL_TEST_TIMEOUT = 5 * 60 * 1000; // 5 mins
    public static final int APPLICATION_TEST_TIMEOUT = 20 * 60 * 1000; // 20 mins
    private static volatile Boolean tenantsInitialized = false;

    public StratosIntegrationTest() {
        init();
        if (!tenantsInitialized) {
            synchronized (StratosIntegrationTest.class) {
                if (!tenantsInitialized) {
                    populateTenants();
                    tenantsInitialized = true;
                }
            }
        }

        Tenant tenant1 = (Tenant) restClientAdmin
                .getEntity(RestConstants.TENANT_API, RestConstants.TENANT1_GET_RESOURCE, Tenant.class,
                        RestConstants.TENANTS_NAME);
        tenant1Id = tenant1.getTenantId();
        Tenant tenant2 = (Tenant) restClientAdmin
                .getEntity(RestConstants.TENANT_API, RestConstants.TENANT2_GET_RESOURCE, Tenant.class,
                        RestConstants.TENANTS_NAME);
        tenant2Id = tenant2.getTenantId();
    }

    private void init() {
        try {
            log.info("Initializing StratosIntegrationTest...");
            stratosAutomationCtx = new AutomationContext("STRATOS", "stratos-001", TestUserMode.SUPER_TENANT_ADMIN);
            adminUsername = stratosAutomationCtx.getConfigurationValue
                    ("/automation/userManagement/superTenant/tenant/admin/user/userName");
            adminPassword = stratosAutomationCtx.getConfigurationValue
                    ("/automation/userManagement/superTenant/tenant/admin/user/password");

            // Do not rely on automation context for context URLs since ports are dynamically picked
            stratosBackendURL = StratosServerExtension.getStratosTestServerManager().getWebAppURL();
            restClientAdmin = new RestClient(stratosBackendURL, adminUsername, adminPassword);
            mockIaasApiClient = new IntegrationMockClient(stratosBackendURL + "/mock-iaas/api");

            tenant1UserName = "admin@test1.com";
            tenant1Password = "admin123";
            tenant2UserName = "admin@test2.com";
            tenant2Password = "admin123";

            restClientTenant1 = new RestClient(stratosBackendURL, tenant1UserName, tenant1Password);
            restClientTenant2 = new RestClient(stratosBackendURL, tenant2UserName, tenant2Password);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not initialize StratosIntegrationTest", e);
        }
    }

    private void populateTenants() {
        log.info("Creating tenants in Stratos server...");
        boolean addedTenant1 = restClientAdmin
                .addEntity(RestConstants.TENANT1_RESOURCE, RestConstants.TENANT_API, RestConstants.TENANTS_NAME);
        assertEquals(addedTenant1, true);
        boolean addedTenant2 = restClientAdmin
                .addEntity(RestConstants.TENANT2_RESOURCE, RestConstants.TENANT_API, RestConstants.TENANTS_NAME);
        assertEquals(addedTenant2, true);
    }
}