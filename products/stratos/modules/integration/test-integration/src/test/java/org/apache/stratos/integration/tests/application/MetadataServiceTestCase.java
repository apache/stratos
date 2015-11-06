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

package org.apache.stratos.integration.tests.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.metadata.client.beans.PropertyBean;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Deploy a sample application on mock IaaS and load test metadata service with high load of concurrent read/write
 * operations from multiple clients
 */
public class MetadataServiceTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(MetadataServiceTestCase.class);
    private static final String RESOURCES_PATH = "/metadata-service-test";
    private static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    private static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";
    private static final String PAYLOAD_PARAMETER_TOKEN_KEY = "TOKEN";
    private static final String PAYLOAD_PARAMETER_APPLICATION_ID_KEY = "APPLICATION_ID";
    private static final String PAYLOAD_PARAMETER_CLUSTER_ID_KEY = "CLUSTER_ID";
    private GsonBuilder gsonBuilder = new GsonBuilder();
    private Gson gson = gsonBuilder.create();
    private TopologyHandler topologyHandler = TopologyHandler.getInstance();
    private String app1AccessToken;
    private String app2AccessToken;
    private String app1ClusterId;
    private long startTime;

    public static final String NETWORK_PARTITION_ID = "network-partition-metadata-service-test";
    public static final String AUTOSCALE_POLICY_ID = "autoscaling-policy-metadata-service-test";
    public static final String CARTRIDGE_TYPE = "c1-application-metadata-service-test";
    public static final String APPLICATION_1_ID = "application-metadata-service-test-1";
    public static final String APPLICATION_2_ID = "application-metadata-service-test-2";
    public static final String APPLICATION_POLICY_ID = "application-policy-metadata-service-test";
    public static final String DEPLOYMENT_POLICY_ID = "deployment-policy-metadata-service-test";

    @BeforeTest(timeOut = APPLICATION_TEST_TIMEOUT,
                groups = { "stratos.application.deployment" })
    public void deployApplications() throws Exception {
        startTime = System.currentTimeMillis();
        log.info("Adding autoscaling policy [autoscale policy id] " + AUTOSCALE_POLICY_ID);
        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + AUTOSCALE_POLICY_ID + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        log.info("Adding cartridge [cartridge type] " + CARTRIDGE_TYPE);
        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + CARTRIDGE_TYPE + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        log.info("Adding network partition [network partition id] " + APPLICATION_1_ID);
        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        NETWORK_PARTITION_ID + ".json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        log.info("Adding deployment policy [deployment policy id] " + DEPLOYMENT_POLICY_ID);
        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        DEPLOYMENT_POLICY_ID + ".json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        // add app-1
        log.info("Adding application [application id] " + APPLICATION_1_ID);
        boolean addedApp1 = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                APPLICATION_1_ID + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(addedApp1, true);

        ApplicationBean bean1 = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, APPLICATION_1_ID, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean1.getApplicationId(), APPLICATION_1_ID);

        // add app-2
        log.info("Adding application [application id] " + APPLICATION_2_ID);
        boolean addedApp2 = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                APPLICATION_2_ID + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(addedApp2, true);

        ApplicationBean bean2 = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, APPLICATION_2_ID, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean2.getApplicationId(), APPLICATION_2_ID);

        log.info("Adding application policy [application policy id] " + APPLICATION_POLICY_ID);
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        APPLICATION_POLICY_ID + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient
                .getEntity(RestConstants.APPLICATION_POLICIES, APPLICATION_POLICY_ID, ApplicationPolicyBean.class,
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(policyBean.getId(), APPLICATION_POLICY_ID);

        // deploy app-1
        log.info("Deploying application [application id] " + APPLICATION_1_ID + " using [application policy " + "id] "
                + APPLICATION_POLICY_ID);
        String resourcePath = RestConstants.APPLICATIONS + "/" + APPLICATION_1_ID +
                RestConstants.APPLICATIONS_DEPLOY + "/" + APPLICATION_POLICY_ID;
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        // deploy app-2
        log.info("Deploying application [application id] " + APPLICATION_2_ID + " using [application policy " + "id] "
                + APPLICATION_POLICY_ID);
        resourcePath = RestConstants.APPLICATIONS + "/" + APPLICATION_2_ID +
                RestConstants.APPLICATIONS_DEPLOY + "/" + APPLICATION_POLICY_ID;
        deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        log.info("Waiting for application-1 status to become ACTIVE...");
        topologyHandler.assertApplicationStatus(bean1.getApplicationId(), ApplicationStatus.Active);

        log.info("Waiting for cluster status of application-1 to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean1.getApplicationId());

        log.info("Waiting for application-2 status to become ACTIVE...");
        topologyHandler.assertApplicationStatus(bean2.getApplicationId(), ApplicationStatus.Active);

        log.info("Waiting for cluster status of application-2 to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean2.getApplicationId());

        Properties app1PayloadProperties = getPayloadProperties(APPLICATION_1_ID);
        app1AccessToken = app1PayloadProperties.getProperty(PAYLOAD_PARAMETER_TOKEN_KEY);
        String app1Id = app1PayloadProperties.getProperty(PAYLOAD_PARAMETER_APPLICATION_ID_KEY);
        Assert.assertEquals(app1Id, APPLICATION_1_ID, "Payload application id is not equal to deployed application id");
        app1ClusterId = app1PayloadProperties.getProperty(PAYLOAD_PARAMETER_CLUSTER_ID_KEY);
        assertNotNull(app1AccessToken, "Access token is null in member payload");

        Properties app2PayloadProperties = getPayloadProperties(APPLICATION_2_ID);
        app2AccessToken = app2PayloadProperties.getProperty(PAYLOAD_PARAMETER_TOKEN_KEY);
        String app2Id = app2PayloadProperties.getProperty(PAYLOAD_PARAMETER_APPLICATION_ID_KEY);
        Assert.assertEquals(app2Id, APPLICATION_2_ID, "Payload application id is not equal to deployed application id");
        assertNotNull(app2AccessToken, "Access token is null in member payload");
    }

    private Properties getPayloadProperties(String applicationId) {
        List<Member> memberList = topologyHandler.getMembersForApplication(applicationId);
        Assert.assertTrue(memberList.size() > 0,
                String.format("Active member list for application %s is empty", applicationId));
        MockInstanceMetadata mockInstanceMetadata = mockIaasApiClient.getInstance(memberList.get(0).getMemberId());
        String payloadString = mockInstanceMetadata.getPayload();
        log.info("Mock instance payload properties: " + payloadString);

        Properties payloadProperties = new Properties();
        String[] parameterArray = payloadString.split(PAYLOAD_PARAMETER_SEPARATOR);
        for (String parameter : parameterArray) {
            if (parameter != null) {
                String[] nameValueArray = parameter.split(PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR, 2);
                if ((nameValueArray.length == 2)) {
                    payloadProperties.put(nameValueArray[0], nameValueArray[1]);
                }
            }
        }
        return payloadProperties;
    }

    @Test(timeOut = APPLICATION_TEST_TIMEOUT,
          description = "Application startup, activation and metadata service basic test",
          groups = { "stratos.application.deployment" },
          priority = 1)
    public void testBasicOperations() throws Exception {
        String key = "mykey";
        String val1 = "myval1";
        String val2 = "myval2";
        String val3 = "myval3";

        log.info("Trying to add metadata for application:" + APPLICATION_1_ID + ", with app1AccessToken: "
                + app1AccessToken);
        boolean hasProperty1Added = restClient.addPropertyToApplication(APPLICATION_1_ID, key, val1, app1AccessToken);
        Assert.assertTrue(hasProperty1Added, "Could not add metadata property1 to application: " + APPLICATION_1_ID);

        PropertyBean propertyBean1 = restClient.getApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean1));
        Assert.assertTrue(propertyBean1 != null && propertyBean1.getValues().size() > 0, "Empty property list");

        boolean hasPropertiesAdded1 = propertyBean1.getValues().contains(val1);
        Assert.assertTrue(hasPropertiesAdded1, "Metadata properties retrieved are not correct");

        log.info("Trying to add metadata for application:" + APPLICATION_1_ID + ", with app1AccessToken: "
                + app1AccessToken);
        boolean hasProperty2Added = restClient.addPropertyToApplication(APPLICATION_1_ID, key, val2, app1AccessToken);
        Assert.assertTrue(hasProperty2Added, "Could not add metadata property2 to application: " + APPLICATION_1_ID);

        PropertyBean propertyBean2 = restClient.getApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean2));
        Assert.assertTrue(propertyBean2 != null && propertyBean2.getValues().size() > 0, "Empty property list");

        List<String> addedValues = new ArrayList<>(Arrays.asList(val1, val2));
        boolean hasPropertiesAdded2 = propertyBean2.getValues().containsAll(addedValues);
        Assert.assertTrue(hasPropertiesAdded2, "Metadata properties retrieved are not correct");

        log.info("Trying to add metadata for application:" + APPLICATION_1_ID + ", cluster:" + app1ClusterId
                + ", with app1AccessToken: " + app1AccessToken);
        restClient.addPropertyToCluster(APPLICATION_1_ID, app1ClusterId, key, val3, app1AccessToken);
        PropertyBean propertyBean3 = restClient
                .getClusterProperty(APPLICATION_1_ID, app1ClusterId, key, app1AccessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean3));
        Assert.assertTrue(propertyBean3 != null && propertyBean3.getValues().size() > 0, "Empty property list");

        boolean hasPropertiesAdded3 = propertyBean3.getValues().contains(val3);
        Assert.assertTrue(hasPropertiesAdded3, "Metadata properties retrieved are not correct");

        // clean up property value
        log.info("Trying to remove application property value");
        restClient.deleteApplicationPropertyValue(APPLICATION_1_ID, key, val2, app1AccessToken);
        PropertyBean propertyBean4 = restClient.getApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        boolean hasPropertyValueRemoved = !propertyBean4.getValues().contains(val2);
        Assert.assertTrue(hasPropertyValueRemoved,
                String.format("Application [property] %s, [value] %s was not removed", key, val2));

        log.info("Trying to remove application property");
        restClient.deleteApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        PropertyBean propertyBean5 = restClient.getApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        Assert.assertNull(propertyBean5, "Metadata properties have not been cleaned properly");

        // clean up app metadata
        restClient.deleteApplicationProperties(APPLICATION_1_ID, app1AccessToken);
        PropertyBean propertyBeanCleaned = restClient.getApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBeanCleaned));
        Assert.assertNull(propertyBeanCleaned, "Metadata properties have not been cleaned properly");

        log.info("Metadata service basic test completed successfully");
    }

    @Test(timeOut = APPLICATION_TEST_TIMEOUT,
          description = "Application startup, activation and metadata service concurrency test",
          groups = { "stratos.application.deployment" },
          priority = 2)
    public void metadataConcurrencyTest() throws Exception {
        log.info("Starting multiple clients to add properties");
        ExecutorService taskExecutor = Executors.newFixedThreadPool(5);
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(getInstanceOfCallable(APPLICATION_1_ID, "t1"));
        tasks.add(getInstanceOfCallable(APPLICATION_1_ID, "t2"));
        tasks.add(getInstanceOfCallable(APPLICATION_1_ID, "t3"));
        taskExecutor.invokeAll(tasks);

        for (int i = 0; i < 50; i++) {
            PropertyBean propertyBean = restClient
                    .getApplicationProperty(APPLICATION_1_ID, Integer.toString(i), app1AccessToken);
            log.info("Retrieved metadata property: " + gson.toJson(propertyBean));
            List<String> addedValues = new ArrayList<>(Arrays.asList("t1", "t2", "t3"));
            boolean hasPropertiesAdded = propertyBean.getValues().containsAll(addedValues);
            Assert.assertTrue(hasPropertiesAdded, String.format("Property values have not been added for [key] %d", i));
        }
        log.info("Metadata service concurrency test completed successfully");
    }

    @Test(timeOut = APPLICATION_TEST_TIMEOUT,
          description = "Application startup, activation and metadata service security test",
          groups = { "stratos.application.deployment" },
          priority = 3)
    public void metadataSecurityTest() throws Exception {
        String key = "mykey";
        String val1 = "myval1";

        log.info("Trying to add metadata for application:" + APPLICATION_1_ID + ", with app1AccessToken: "
                + app1AccessToken);
        boolean hasProperty1Added = restClient.addPropertyToApplication(APPLICATION_1_ID, key, val1, app1AccessToken);
        Assert.assertTrue(hasProperty1Added, "Could not add metadata property1 to application: " + APPLICATION_1_ID);

        PropertyBean propertyBean1 = restClient.getApplicationProperty(APPLICATION_1_ID, key, app1AccessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean1));
        Assert.assertTrue(propertyBean1 != null && propertyBean1.getValues().size() > 0, "Empty property list");

        boolean hasPropertiesAdded1 = propertyBean1.getValues().contains(val1);
        Assert.assertTrue(hasPropertiesAdded1, "Metadata properties retrieved are not correct");

        log.info("Trying to access metadata with an empty access token. This should fail");
        propertyBean1 = restClient.getApplicationProperty(APPLICATION_1_ID, key, "");
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean1));
        Assert.assertNull(propertyBean1, "Metadata service returned value for an empty access token");

        log.info("Trying to access metadata with an access token of another app. This should fail");
        propertyBean1 = restClient.getApplicationProperty(APPLICATION_1_ID, key, app2AccessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean1));
        Assert.assertNull(propertyBean1, "Metadata service returned value for an access token of another application");

        log.info("Metadata service security test completed successfully");
    }

    @Test(timeOut = APPLICATION_TEST_TIMEOUT,
          description = "Application startup, activation and metadata service concurrency test",
          groups = { "stratos.application.deployment" },
          priority = 10)
    public void cleanupAfterUndeployingAppTest() throws Exception {
        // undeploy the app and check whether metadata entries are cleared
        log.info("Un-deploying the application [application id] " + APPLICATION_1_ID);
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + APPLICATION_1_ID +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(APPLICATION_1_ID);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info("Force undeployment is going to start for the [application] " + APPLICATION_1_ID);

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + APPLICATION_1_ID +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(APPLICATION_1_ID);
            assertTrue(String.format("Forceful undeployment failed for the application %s", APPLICATION_1_ID),
                    forceUndeployed);
        }

        for (int i = 0; i < 50; i++) {
            PropertyBean propertyBeanCleaned = restClient
                    .getApplicationProperty(APPLICATION_1_ID, Integer.toString(i), app1AccessToken);
            log.info("Retrieved metadata property: " + gson.toJson(propertyBeanCleaned));
            Assert.assertNull(propertyBeanCleaned, "Metadata properties have not been cleaned properly");
        }
        log.info("Metadata service cleanup test completed successfully");
    }

    private Callable<Void> getInstanceOfCallable(final String appId, final String propertyValue) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 0; i < 50; i++) {
                    restClient.addPropertyToApplication(appId, Integer.toString(i), propertyValue, app1AccessToken);
                }
                return null;
            }
        };
    }

    @AfterTest(timeOut = APPLICATION_TEST_TIMEOUT,
               groups = { "stratos.application.deployment" })
    public void cleanup() throws Exception {
        // remove app-1
        log.info("Removing the application [application id] " + APPLICATION_1_ID);
        boolean removedApp = restClient
                .removeEntity(RestConstants.APPLICATIONS, APPLICATION_1_ID, RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, APPLICATION_1_ID, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        // undeploy and remove app-2
        log.info("Un-deploying the application [application id] " + APPLICATION_2_ID);
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + APPLICATION_2_ID +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(APPLICATION_2_ID);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info("Force undeployment is going to start for the [application] " + APPLICATION_2_ID);

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + APPLICATION_2_ID +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(APPLICATION_2_ID);
            assertTrue(String.format("Forceful undeployment failed for the application %s", APPLICATION_2_ID),
                    forceUndeployed);
        }

        log.info("Removing the application [application id] " + APPLICATION_2_ID);
        removedApp = restClient
                .removeEntity(RestConstants.APPLICATIONS, APPLICATION_2_ID, RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, APPLICATION_2_ID, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info("Removing the application policy [application policy id] " + APPLICATION_POLICY_ID);
        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES, APPLICATION_POLICY_ID,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info("Removing the cartridge [cartridge type] " + CARTRIDGE_TYPE);
        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, CARTRIDGE_TYPE, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info("Removing the autoscaling policy [autoscaling policy id] " + AUTOSCALE_POLICY_ID);
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, AUTOSCALE_POLICY_ID,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info("Removing the deployment policy [deployment policy id] " + DEPLOYMENT_POLICY_ID);
        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES, DEPLOYMENT_POLICY_ID,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info("Removing the network partition [network partition id] " + NETWORK_PARTITION_ID);
        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, NETWORK_PARTITION_ID,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Metadata test completed in " + duration + " ms");
    }
}
