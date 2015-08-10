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
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.apache.stratos.integration.tests.TopologyHandler;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * This will handle the scale-up and scale-down of a particular cluster bursting test cases
 */
public class SingleClusterScalingTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(SampleApplicationsTest.class);
    private static final String TEST_PATH = "/application-bursting-test";


    @Test
    public void testDeployApplication() {
        try {
            log.info("Started application Bursting test case**************************************");

            String autoscalingPolicyId = "autoscaling-policy-2";

            boolean addedScalingPolicy = restClient.addEntity(TEST_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "esb.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC1, true);

            boolean addedC2 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "php.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC2, true);

            boolean addedC3 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "tomcat.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC3, true);

            boolean addedG1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "esb-php-group.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(addedG1, true);

            CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(beanG1.getName(), "esb-php-group");

            boolean addedN1 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-9.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-10.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-4.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            boolean added = restClient.addEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "app-bursting-single-cartriddge-group.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(added, true);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "cartridge-group-app", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "cartridge-group-app");

            boolean addAppPolicy = restClient.addEntity(TEST_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            "application-policy-3.json", RestConstants.APPLICATION_POLICIES,
                    RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(addAppPolicy, true);

            ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient.getEntity(
                    RestConstants.APPLICATION_POLICIES,
                    "application-policy-3", ApplicationPolicyBean.class,
                    RestConstants.APPLICATION_POLICIES_NAME);

            //deploy the application
            String resourcePath = RestConstants.APPLICATIONS + "/" + "cartridge-group-app" +
                    RestConstants.APPLICATIONS_DEPLOY + "/" + "application-policy-3";
            boolean deployed = restClient.deployEntity(resourcePath,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(deployed, true);

            //Application active handling
            TopologyHandler.getInstance().assertApplicationActivation(bean.getApplicationId());

            //Group active handling
            TopologyHandler.getInstance().assertGroupActivation(bean.getApplicationId());

            //Cluster active handling
            TopologyHandler.getInstance().assertClusterActivation(bean.getApplicationId());

           /* boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, false);    */

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, false);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-9",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-4", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, false);

            //Un-deploying the application
            String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + "cartridge-group-app" +
                    RestConstants.APPLICATIONS_UNDEPLOY;

            boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(unDeployed, true);

            boolean undeploy = TopologyHandler.getInstance().assertApplicationUndeploy("cartridge-group-app");
            if (!undeploy) {
                //Need to forcefully undeploy the application
                log.info("Force undeployment is going to start for the [application] " + "cartridge-group-app");

                restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + "cartridge-group-app" +
                        RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

                boolean forceUndeployed = TopologyHandler.getInstance().assertApplicationUndeploy("cartridge-group-app");
                assertEquals(String.format("Forceful undeployment failed for the application %s",
                        "cartridge-group-app"), forceUndeployed, true);

            }

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "cartridge-group-app",
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "cartridge-group-app", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, true);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "esb",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "php",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC2, true);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "tomcat",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC3, true);

            removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-4", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-9", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, false);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-10", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, false);

            boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    "application-policy-3", RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(removeAppPolicy, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-9", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-10", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            log.info("Ended application bursting test case**************************************");

        } catch (Exception e) {
            log.error("An error occurred while handling  application bursting", e);
            assertTrue("An error occurred while handling  application bursting", false);
        }
    }

    @Test(dependsOnMethods = {"testApplication"})
    public void testClusterScalingUp() {

    }



}
