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

package org.apache.stratos.integration.tests.iaas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.cartridge.IaasProviderBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.ServerLogClient;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.util.List;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertTrue;

@Test(groups = { "iaas" })
public class IaasProviderAttributeTestCase extends StratosIntegrationTest {

    private static final Log log = LogFactory.getLog(IaasProviderAttributeTestCase.class);
    private static final String RESOURCES_PATH = "/cartridge-iaas-attribute-test";
    private static final String AUTOSCALING_POLICY = "autoscaling-policy-iaasprovider-attribute-test";
    private static final String NETWORK_PARTITION = "network-partition-iaasprovider-attribute-test";
    private static final String DEPLOYMENT_POLICY = "deployment-policy-iaasprovider-attribute-test";
    private static final String APPLICATION_POLICY = "application-policy-iaasprovider-attribute-test";
    private static final String CARTRIDGE = "cartridge-iaasprovider-attribute-test";
    private static final String UPDATED_CARTRIDGE = "cartridge-iaasprovider-attribute-test-updated";
    private static final String APPLICATION = "app-iaasprovider-attribute-test";
    private ServerLogClient serverLogClient;
    private long startTime;

    @BeforeClass
    public void setup() throws Exception {
        serverLogClient = new ServerLogClient(stratosSecuredBackendURL + "/services/", adminUsername, adminPassword);
    }

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT)
    public void testIaasProviderAttributes() throws Exception {
        log.info("Running IaasProviderAttributeTestCase.testIaasProviderAttributes test method...");
        startTime = System.currentTimeMillis();

        // add autoscaling policy
        log.info("Adding autoscaling policy [autoscale policy id] " + AUTOSCALING_POLICY);
        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.
                        AUTOSCALING_POLICIES_PATH + "/" + AUTOSCALING_POLICY + ".json", RestConstants
                .AUTOSCALING_POLICIES,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        // add network partition
        log.info("Adding network partition [network partition id] " + NETWORK_PARTITION);
        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + "/network-partitions" + "/" +
                NETWORK_PARTITION + ".json", RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        // add deployment policy
        log.info("Adding deployment policy [deployment policy id] " + DEPLOYMENT_POLICY);
        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                DEPLOYMENT_POLICY + ".json", RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        // add application policy
        log.info("Adding application policy [application policy id] " + APPLICATION_POLICY);
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        APPLICATION_POLICY + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        // deploy a default cartridge
        boolean defaultCartridgeAdded = restClient
                .addEntity(RESOURCES_PATH + "/cartridges/" + CARTRIDGE + ".json", RestConstants.CARTRIDGES,
                        RestConstants.CARTRIDGES_NAME);
        assertTrue("Default cartridge not deployed properly", defaultCartridgeAdded);

        // deploy application
        log.info("Adding application [application id] " + APPLICATION);
        boolean addedApp = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                APPLICATION + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(addedApp, true);

        // Test Iaas Provider attributes
        CartridgeBean defaultCartridgeBean = (CartridgeBean) restClient
                .getEntity(RestConstants.CARTRIDGES, CARTRIDGE, CartridgeBean.class, RestConstants.CARTRIDGES_NAME);

        assertEquals(CARTRIDGE, defaultCartridgeBean.getType());
        List<IaasProviderBean> iaasProviders = defaultCartridgeBean.getIaasProvider();
        assertNotNull(iaasProviders, "No Iaas Providers found in default cartridge definition");
        IaasProviderBean mockIaasProvider = getMockIaasProvider(iaasProviders);
        assertNotNull(mockIaasProvider, "Mock Iaas Provider not found in default cartridge definition");
        assertNotNull(mockIaasProvider.getProperty(),
                "No properties found in Iaas Provider " + "config of default cartridge definition");

        ///applications/{applicationId}/deploy/{applicationPolicyId}
        log.info("Deploying application [application id] app-iaasprovider-attribute-test using [application policy id] "
                + "application-policy-iaasprovider-attribute-test");
        String resourcePath = RestConstants.APPLICATIONS + "/app-iaasprovider-attribute-test" +
                RestConstants.APPLICATIONS_DEPLOY + "/" + APPLICATION_POLICY;
        boolean appDeployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(appDeployed);

        ApplicationBean applicationBean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, APPLICATION, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(applicationBean.getApplicationId(), APPLICATION);

        TopologyHandler topologyHandler = TopologyHandler.getInstance();

        log.info("Waiting for application status to become ACTIVE...");
        TopologyHandler.getInstance().assertApplicationActiveStatus(applicationBean.getApplicationId());

        // create a ServerLogClientInstance and get logs
        boolean found = false;
        LogEvent[] logEvents = serverLogClient.getAllLogLines();
        if (logEvents.length > 0) {
            for (LogEvent log : logEvents) {
                if (!log.getMessage().contains("cartridge_property_value_1") && log.getMessage()
                        .contains("cc_property_value_1")) {
                    found = true;
                    break;
                }
            }
        }

        assertTrue("Property 'property1' not found | value not equal to 'cc_property_value_1'", found);

        // undeploy application
        log.info("Un-deploying the application [application id] app-iaasprovider-attribute-test");
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/app-iaasprovider-attribute-test" +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean undeployedApp = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(undeployedApp);
        log.info("Undeployed application 'app-iaasprovider-attribute-test'");

        // force undeploy to make sure its undeployed
        log.info("Force undeployment is going to start for the [application] app-iaasprovider-attribute-test");
        restClient.undeployEntity(RestConstants.APPLICATIONS + "/app-iaasprovider-attribute-test" +
                RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

        boolean forceUndeployed = topologyHandler.assertApplicationUndeploy("app-iaasprovider-attribute-test");
        assertTrue(
                String.format("Forceful undeployment failed for the application %s", "app-iaasprovider-attribute-test"),
                forceUndeployed);

        // update cartridge
        boolean updated = restClient
                .updateEntity(RESOURCES_PATH + "/cartridges/" + UPDATED_CARTRIDGE + ".json", RestConstants.CARTRIDGES,
                        RestConstants.CARTRIDGES_NAME);
        assertTrue(updated);
        log.info("Updated cartridge 'cartridge-iaasprovider-attribute-test'");

        // re-deplpoy the application
        resourcePath = RestConstants.APPLICATIONS + "/app-iaasprovider-attribute-test" +
                RestConstants.APPLICATIONS_DEPLOY + "/" + APPLICATION_POLICY;
        appDeployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(appDeployed);
        log.info("Re-deployed application 'app-iaasprovider-attribute-test'");

        log.info("Waiting for application status to become ACTIVE...");
        TopologyHandler.getInstance().assertApplicationActiveStatus(applicationBean.getApplicationId());

        logEvents = serverLogClient.getAllLogLines();
        found = false;
        if (logEvents.length > 0) {
            for (LogEvent log : logEvents) {
                if (log.getMessage().contains("cartridge_property_value_1")) {
                    found = true;
                    break;
                }
            }
        }

        assertTrue("Property 'property1' not found | value not equal to 'cartridge_property_value_1'", found);
    }

    @AfterClass
    public void tearDown() throws Exception {
        terminateAndRemoveAllArtifacts();
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("IaasProviderAttributeTestCase completed in [duration] %s ms", duration));
    }

    private IaasProviderBean getMockIaasProvider(List<IaasProviderBean> iaasProviders) {
        for (IaasProviderBean iaasProvider : iaasProviders) {
            if ("mock".equals(iaasProvider.getType())) {
                return iaasProvider;
            }
        }
        return null;
    }

    private void terminateAndRemoveAllArtifacts() throws Exception {

        TopologyHandler topologyHandler = TopologyHandler.getInstance();

        log.info("Un-deploying the application [application id] app-iaasprovider-attribute-test");
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/app-iaasprovider-attribute-test" +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy("app-iaasprovider-attribute-test");
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info("Force undeployment is going to start for the [application] app-iaasprovider-attribute-test");

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/app-iaasprovider-attribute-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy("app-iaasprovider-attribute-test");
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    "app-iaasprovider-attribute-test"), forceUndeployed);
        }

        log.info("Removing the application [application id] app-iaasprovider-attribute-test");
        boolean removedApp = restClient.removeEntity(RestConstants.APPLICATIONS, "app-iaasprovider-attribute-test",
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "app-iaasprovider-attribute-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info("Removing the application policy [application policy id] "
                + "application-policy-iaasprovider-attribute-test");
        boolean removeAppPolicy = restClient
                .removeEntity(RestConstants.APPLICATION_POLICIES, "application-policy-iaasprovider-attribute-test",
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info("Removing the cartridge [cartridge type] cartridge-iaasprovider-attribute-test");
        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "cartridge-iaasprovider-attribute-test",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info("Removing the autoscaling policy [autoscaling policy id] " + AUTOSCALING_POLICY);
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, AUTOSCALING_POLICY,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info("Removing the deployment policy [deployment policy id] deployment-policy-iaasprovider-attribute-test");
        boolean removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-iaasprovider-attribute-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info("Removing the network partition [network partition id] network-partition-iaasprovider-attribute-test");
        boolean removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-iaasprovider-attribute-test",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
    }
}
