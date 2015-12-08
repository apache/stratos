/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.integration.tests.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * This will handle the application bursting test cases
 */
@Test(groups = { "application", "app-burst" })
public class ApplicationBurstingTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(ApplicationBurstingTestCase.class);
    private TopologyHandler topologyHandler = TopologyHandler.getInstance();
    private static final String RESOURCES_PATH = "/application-bursting-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-application-bursting-test";
    private static final String cartridgeId1 = "esb-application-bursting-test";
    private static final String cartridgeId2 = "php-application-bursting-test";
    private static final String cartridgeId3 = "tomcat-application-bursting-test";
    private static final String cartridgeGroupId = "esb-php-group-application-bursting-test";
    private static final String networkPartition1 = "network-partition-application-bursting-test-1";
    private static final String networkPartition2 = "network-partition-application-bursting-test-2";
    private static final String deploymentPolicyId = "deployment-policy-application-bursting-test";
    private static final String applicationId = "application-bursting-test";
    private static final String applicationPolicyId = "application-policy-application-bursting-test";

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT)
    public void testApplicationBusting() throws Exception {
        log.info("Running ApplicationBurstingTestCase.testApplicationBusting test method...");
        long startTime = System.currentTimeMillis();

        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId1 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        boolean addedC2 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId2 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC2);

        boolean addedC3 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId3 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC3);

        boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                "/" + cartridgeGroupId + ".json", RestConstants.CARTRIDGE_GROUPS, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG1);

        CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                getEntity(RestConstants.CARTRIDGE_GROUPS, cartridgeGroupId, CartridgeGroupBean.class,
                        RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(beanG1.getName(), cartridgeGroupId);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                networkPartition1 + ".json", RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                networkPartition2 + ".json", RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN2);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                applicationId + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertTrue(added);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId);

        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        //Application active handling
        TopologyHandler.getInstance().assertApplicationActiveStatus(applicationId);

        //Group active handling
        topologyHandler.assertGroupActivation(bean.getApplicationId());

        //Cluster active handling
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        //Un-deploying the application
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(applicationId);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for the [application] %s ", applicationId));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(applicationId);
            assertTrue(String.format("Forceful undeployment failed for the application %s", applicationId),
                    forceUndeployed);

        }

        boolean removed = restClient
                .removeEntity(RestConstants.APPLICATIONS, applicationId, RestConstants.APPLICATIONS_NAME);
        assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        boolean removedGroup = restClient
                .removeEntity(RestConstants.CARTRIDGE_GROUPS, cartridgeGroupId, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId1, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        boolean removedC2 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId2, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC2);

        boolean removedC3 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId3, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC3);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartition1,
                RestConstants.NETWORK_PARTITIONS_NAME);
        Assert.assertTrue(removedNet);

        boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartition2,
                RestConstants.NETWORK_PARTITIONS_NAME);
        Assert.assertTrue(removedN2);
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("ApplicationBurstingTestCase completed in [duration] %s ms", duration));
    }
}
