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
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.integration.common.Util;
import org.apache.stratos.integration.common.rest.IntegrationMockClient;
import org.apache.stratos.integration.common.rest.RestClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.URL;

public class StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(StratosIntegrationTest.class);
    protected AutomationContext stratosAutomationCtx;
    protected String adminUsername;
    protected String adminPassword;
    protected String stratosBackendURL;
    protected RestClient restClient;
    protected IntegrationMockClient mockIaasApiClient;
    public static final int GLOBAL_TEST_TIMEOUT = 5 * 60 * 1000;
    public static final int APPLICATION_TEST_TIMEOUT = 20 * 60 * 1000;

    public StratosIntegrationTest() {
        try {
            stratosAutomationCtx = new AutomationContext("STRATOS", "stratos-001", TestUserMode.SUPER_TENANT_ADMIN);
            adminUsername = stratosAutomationCtx.getConfigurationValue
                    ("/automation/userManagement/superTenant/tenant/admin/user/userName");
            adminPassword = stratosAutomationCtx.getConfigurationValue
                    ("/automation/userManagement/superTenant/tenant/admin/user/password");
            stratosBackendURL = stratosAutomationCtx.getContextUrls().getWebAppURL();
            restClient = new RestClient(stratosBackendURL, adminUsername, adminPassword);
            mockIaasApiClient = new IntegrationMockClient(stratosBackendURL + "/mock-iaas/api");
            setSystemproperties();
        }
        catch (Exception e) {
            log.error("Could not initialize StratosIntegrationTest base parameters");
        }
    }

    public void setSystemproperties() {
        URL resourceUrl = getClass().getResource(File.separator + "keystores" + File.separator
                + "products" + File.separator + "wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStore", resourceUrl.getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        log.info("trustStore set to " + resourceUrl.getPath());

        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", Util.getCommonResourcesFolderPath());
        try {
            String autoscalerServiceURL = stratosAutomationCtx.getContextUrls().getSecureServiceUrl() +
                    "/AutoscalerService";
            System.setProperty(StratosConstants.AUTOSCALER_SERVICE_URL, autoscalerServiceURL);
            log.info("Autoscaler service URL set to " + autoscalerServiceURL);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not set autoscaler service URL system property");
        }
    }
}