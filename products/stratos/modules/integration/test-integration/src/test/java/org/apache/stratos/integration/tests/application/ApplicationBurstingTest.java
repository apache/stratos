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
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * This will handle the application bursting test cases
 */
public class ApplicationBurstingTest extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(SampleApplicationsTest.class);
    private static final String RESOURCES_PATH = "/application-bursting-test";


    @Test(timeOut = APPLICATION_TEST_TIMEOUT, groups = {"stratos.application.deployment"})
    public void testApplicationBusting() {
        try {
            log.info("----------------------------Started application Bursting test case----------------------------");
            TopologyHandler topologyHandler = TopologyHandler.getInstance();
            String autoscalingPolicyId = "autoscaling-policy-application-bursting-test";

            boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertTrue(addedScalingPolicy);

            boolean addedC1 = restClient.addEntity(
                    RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "esb-application-bursting-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertTrue(addedC1);

            boolean addedC2 = restClient.addEntity(
                    RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "php-application-bursting-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertTrue(addedC2);

            boolean addedC3 = restClient.addEntity(
                    RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "tomcat-application-bursting-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertTrue(addedC3);

            boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "esb-php-group-application-bursting-test.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertTrue(addedG1);

            CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group-application-bursting-test",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(beanG1.getName(), "esb-php-group-application-bursting-test");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-bursting-test-1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN1);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-bursting-test-2.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN2);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-application-bursting-test.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(addedDep);

            boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "app-bursting-single-cartriddge-group.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertTrue(added);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "application-bursting-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "application-bursting-test");

            boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            "application-policy-application-bursting-test.json", RestConstants.APPLICATION_POLICIES,
                    RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(addAppPolicy);

            ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient.getEntity(
                    RestConstants.APPLICATION_POLICIES,
                    "application-policy-application-bursting-test", ApplicationPolicyBean.class,
                    RestConstants.APPLICATION_POLICIES_NAME);

            //deploy the application
            String resourcePath = RestConstants.APPLICATIONS + "/" + "application-bursting-test" +
                    RestConstants.APPLICATIONS_DEPLOY + "/" + "application-policy-application-bursting-test";
            boolean deployed = restClient.deployEntity(resourcePath,
                    RestConstants.APPLICATIONS_NAME);
            assertTrue(deployed);

            //Application active handling
            topologyHandler.assertApplicationStatus(bean.getApplicationId(),
                    ApplicationStatus.Active);

            //Group active handling
            topologyHandler.assertGroupActivation(bean.getApplicationId());

            //Cluster active handling
            topologyHandler.assertClusterActivation(bean.getApplicationId());

            boolean removedGroup =
                    restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group-application-bursting-test",
                            RestConstants.CARTRIDGE_GROUPS_NAME);
            assertFalse(removedGroup);

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertFalse(removedAuto);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-1",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertFalse(removedNet);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-application-bursting-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertFalse(removedDep);

            //Un-deploying the application
            String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + "application-bursting-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY;

            boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                    RestConstants.APPLICATIONS_NAME);
            assertTrue(unDeployed);

            boolean undeploy = topologyHandler.assertApplicationUndeploy("application-bursting-test");
            if (!undeploy) {
                //Need to forcefully undeploy the application
                log.info("Force undeployment is going to start for the [application] " + "application-bursting-test");

                restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + "application-bursting-test" +
                        RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

                boolean forceUndeployed = topologyHandler.assertApplicationUndeploy("application-bursting-test");
                assertEquals(String.format("Forceful undeployment failed for the application %s",
                        "application-bursting-test"), forceUndeployed);

            }

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "application-bursting-test",
                    RestConstants.APPLICATIONS_NAME);
            assertTrue(removed);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "application-bursting-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertNull(beanRemoved);

            removedGroup =
                    restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group-application-bursting-test",
                            RestConstants.CARTRIDGE_GROUPS_NAME);
            assertTrue(removedGroup);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "esb-application-bursting-test",
                    RestConstants.CARTRIDGES_NAME);
            assertTrue(removedC1);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "php-application-bursting-test",
                    RestConstants.CARTRIDGES_NAME);
            assertTrue(removedC2);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "tomcat-application-bursting-test",
                    RestConstants.CARTRIDGES_NAME);
            assertTrue(removedC3);

            removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertTrue(removedAuto);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-application-bursting-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(removedDep);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertFalse(removedNet);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertFalse(removedN2);

            boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    "application-policy-application-bursting-test", RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(removeAppPolicy);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedNet);

            removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN2);

            log.info("----------------------------Ended application bursting test case----------------------------");

        }
        catch (Exception e) {
            log.error("An error occurred while handling  application bursting", e);
            assertTrue("An error occurred while handling  application bursting", false);
        }
    }
}