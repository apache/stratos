/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.stratos.common.test.TestLogAppender;
import org.apache.stratos.integration.tests.rest.IntegrationMockClient;
import org.apache.stratos.integration.tests.rest.RestClient;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.wso2.carbon.integration.framework.TestServerManager;
import org.wso2.carbon.integration.framework.utils.FrameworkSettings;
import org.wso2.carbon.integration.framework.utils.ServerUtils;
import org.wso2.carbon.integration.framework.utils.TestUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Prepare activemq, Stratos server for tests, enables mock iaas, starts servers and stop them after the tests.
 */
public class StratosTestServerManager extends TestServerManager {
    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);
    private static Properties integrationProperties;
    public static final String BASE_PATH = StratosTestServerManager.class.getResource("/").getPath();
    public static final String STRATOS_DISTRIBUTION_NAME = "distribution.path";
    public final static String PORT_OFFSET = "carbon.port.offset";
    public static final String ACTIVEMQ_BIND_ADDRESS = "activemq.bind.address";
    public static final String STRATOS_ENDPOINT = "stratos.endpoint";
    public static final String ADMIN_USERNAME = "stratos.admin.username";
    public static final String ADMIN_PASSWORD = "stratos.admin.password";
    private static final String TENANT1_USER_NAME = "stratos.tenant1.username";
    private static final String TENANT1_PASSWD = "stratos.tenant1.password";
    private static final String TENANT2_USER_NAME = "stratos.tenant2.username";
    private static final String TENANT2_PASSWD = "stratos.tenant2.password";
    public static final String MOCK_IAAS_XML_FILE = "mock-iaas.xml";
    public static final String SCALING_DROOL_FILE = "scaling.drl";
    public static final String JNDI_PROPERTIES_FILE = "jndi.properties";
    public static final String JMS_OUTPUT_ADAPTER_FILE = "JMSOutputAdaptor.xml";

    protected String distributionName;
    protected int portOffset;
    protected String adminUsername;
    protected String adminPassword;
    protected String tenant1UserName;
    protected String tenant1Password;
    protected String tenant2UserName;
    protected String tenant2Password;
    protected String stratosEndpoint;
    protected String activemqBindAddress;
    protected RestClient restClientAdmin;
    private BrokerService broker = new BrokerService();
    private TestLogAppender testLogAppender = new TestLogAppender();
    private ServerUtils serverUtils;
    private String carbonHome;
    protected IntegrationMockClient mockIaasApiClient;
    protected RestClient restClientTenant1;
    protected RestClient restClientTenant2;
    protected int tenant1Id;
    protected int tenant2Id;

    public StratosTestServerManager() {
        super(BASE_PATH + getIntegrationTestProperty(STRATOS_DISTRIBUTION_NAME),
                Integer.parseInt(getIntegrationTestProperty(PORT_OFFSET)));

        distributionName = integrationProperties.getProperty(STRATOS_DISTRIBUTION_NAME);
        portOffset = Integer.parseInt(integrationProperties.getProperty(PORT_OFFSET));
        adminUsername = integrationProperties.getProperty(ADMIN_USERNAME);
        adminPassword = integrationProperties.getProperty(ADMIN_PASSWORD);
        tenant1UserName = integrationProperties.getProperty(TENANT1_USER_NAME);
        tenant1Password = integrationProperties.getProperty(TENANT1_PASSWD);
        tenant2UserName = integrationProperties.getProperty(TENANT2_USER_NAME);
        tenant2Password = integrationProperties.getProperty(TENANT2_PASSWD);
        stratosEndpoint = integrationProperties.getProperty(STRATOS_ENDPOINT);
        activemqBindAddress = integrationProperties.getProperty(ACTIVEMQ_BIND_ADDRESS);
        serverUtils = new ServerUtils();
        mockIaasApiClient = new IntegrationMockClient(stratosEndpoint + "/mock-iaas/api");
        restClientAdmin = new RestClient(stratosEndpoint, adminUsername, adminPassword);
        restClientTenant1 = new RestClient(stratosEndpoint, tenant1UserName, tenant1Password);
        restClientTenant2 = new RestClient(stratosEndpoint, tenant2UserName, tenant2Password);
    }

    private static String getIntegrationTestProperty(String key) {
        if (integrationProperties == null) {
            integrationProperties = new Properties();
            try {
                integrationProperties
                        .load(StratosTestServerManager.class.getResourceAsStream("/integration-test.properties"));
                log.info("Stratos integration properties: " + integrationProperties.toString());
            }
            catch (IOException e) {
                log.error("Error loading integration-test.properties file from classpath. Please make sure that file " +
                        "exists in classpath.", e);
            }
        }
        return integrationProperties.getProperty(key);
    }

    @Override
    @BeforeSuite(timeOut = 600000)
    public String startServer() throws IOException {
        Logger.getRootLogger().addAppender(testLogAppender);
        Logger.getRootLogger().setLevel(Level.INFO);

        try {
            // Start ActiveMQ
            long time1 = System.currentTimeMillis();
            log.info("Starting ActiveMQ...");
            broker.setDataDirectory(StratosTestServerManager.class.getResource("/").getPath() +
                    File.separator + ".." + File.separator + "activemq-data");
            broker.setBrokerName("testBroker");
            broker.addConnector(activemqBindAddress);
            broker.start();
            long time2 = System.currentTimeMillis();
            log.info(String.format("ActiveMQ started in %d sec", (time2 - time1) / 1000));
        }
        catch (Exception e) {
            throw new RuntimeException("Could not start ActiveMQ", e);
        }

        try {
            log.info("Setting up Stratos server...");
            long time3 = System.currentTimeMillis();
            String carbonZip = getCarbonZip();
            if (carbonZip == null) {
                carbonZip = System.getProperty("carbon.zip");
            }

            if (carbonZip == null) {
                throw new IllegalArgumentException("carbon zip file is null");
            } else {
                carbonHome = this.serverUtils.setUpCarbonHome(carbonZip);
                TestUtil.copySecurityVerificationService(carbonHome);
                this.copyArtifacts(carbonHome);
                log.info("Stratos server setup completed");

                log.info("Starting Stratos server...");
                this.serverUtils.startServerUsingCarbonHome(carbonHome, carbonHome, "stratos", portOffset, null);
                FrameworkSettings.init();

                while (!serverStarted()) {
                    log.info("Waiting for topology to be initialized...");
                    Thread.sleep(5000);
                }

                while (!mockServiceStarted()) {
                    log.info("Waiting for mock service to be initialized...");
                    Thread.sleep(1000);
                }

                long time4 = System.currentTimeMillis();
                log.info(String.format("Stratos server started in %d sec", (time4 - time3) / 1000));
                createTenants();
                return carbonHome;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not start Stratos server", e);
        }
    }

    private void createTenants() {
        log.info("Added tenants to the testing suit");
        boolean addedTenant1 = restClientAdmin
                .addEntity(RestConstants.TENANT1_RESOURCE, RestConstants.TENANT_API, RestConstants.TENANTS_NAME);
        assertEquals(addedTenant1, true);
        boolean addedTenant2 = restClientAdmin
                .addEntity(RestConstants.TENANT2_RESOURCE, RestConstants.TENANT_API, RestConstants.TENANTS_NAME);
        assertEquals(addedTenant2, true);
    }


    @BeforeClass
    public void getTenantDetails() {
        Tenant tenant1 = (Tenant) restClientAdmin
                .getEntity(RestConstants.TENANT_API, RestConstants.TENANT1_GET_RESOURCE, Tenant.class,
                        RestConstants.TENANTS_NAME);
        tenant1Id = tenant1.getTenantId();
        Tenant tenant2 = (Tenant) restClientAdmin
                .getEntity(RestConstants.TENANT_API, RestConstants.TENANT2_GET_RESOURCE, Tenant.class,
                        RestConstants.TENANTS_NAME);
        tenant2Id = tenant2.getTenantId();
    }

    private boolean mockServiceStarted() {
        for (String message : testLogAppender.getMessages()) {
            if (message.contains("Mock IaaS service component activated")) {
                return true;
            }
        }
        return false;
    }

    @Override
    @AfterSuite(timeOut = 600000)
    public void stopServer() throws Exception {
        super.stopServer();
       /*
       while (!serverStopped()) {
            log.info("Waiting for server to be shutdown...");
            Thread.sleep(1000);
        }
        log.info("Stopped Apache Stratos server.");
        */
        broker.stop();
        log.info("Stopped ActiveMQ server.");
    }

    protected void copyArtifacts(String carbonHome) throws IOException {
        copyConfigFile(carbonHome, MOCK_IAAS_XML_FILE);
        copyConfigFile(carbonHome, JNDI_PROPERTIES_FILE);
        copyConfigFile(carbonHome, SCALING_DROOL_FILE, "repository/conf/drools");
        copyConfigFile(carbonHome, JMS_OUTPUT_ADAPTER_FILE, "repository/deployment/server/outputeventadaptors");
    }

    private void copyConfigFile(String carbonHome, String sourceFilePath) throws IOException {
        copyConfigFile(carbonHome, sourceFilePath, "repository/conf");
    }

    private void copyConfigFile(String carbonHome, String sourceFilePath, String destinationFolder) throws IOException {
        log.info("Copying file: " + sourceFilePath);
        URL fileURL = getClass().getResource("/" + sourceFilePath);
        assertNotNull(fileURL);
        File srcFile = new File(fileURL.getFile());
        File destFile = new File(carbonHome + "/" + destinationFolder + "/" + sourceFilePath);
        FileUtils.copyFile(srcFile, destFile);
        log.info(sourceFilePath + " file copied");
    }

    private boolean serverStopped() {
        for (String message : testLogAppender.getMessages()) {
            if (message.contains("Halting JVM")) {
                return true;
            }
        }
        return false;
    }


    private boolean serverStarted() {
        for (String message : testLogAppender.getMessages()) {
            if (message.contains("Topology initialized")) {
                return true;
            }
        }
        return false;
    }
}
