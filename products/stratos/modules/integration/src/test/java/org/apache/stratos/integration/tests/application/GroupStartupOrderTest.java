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
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Handling the startup order of the group
 */
public class GroupStartupOrderTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(SampleApplicationsTest.class);
    private static final String RESOURCES_PATH = "/application-bursting-test";

    @Test
    public void testApplicationBusting() {
        try {
            log.info("-------------------------------Started application Bursting test case-------------------------------");

            String autoscalingPolicyId = "autoscaling-policy-application-bursting-test";

            boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "esb-application-bursting-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC1, true);

            boolean addedC2 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "php-application-bursting-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC2, true);

            boolean addedC3 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "tomcat-application-bursting-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC3, true);

            boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "esb-php-group-application-bursting-test.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(addedG1, true);

            CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group-application-bursting-test",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(beanG1.getName(), "esb-php-group-application-bursting-test");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-bursting-test-1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-bursting-test-2.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-application-bursting-test.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "app-bursting-single-cartriddge-group.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(added, true);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "application-bursting-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "application-bursting-test");

            boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            "application-policy-application-bursting-test.json", RestConstants.APPLICATION_POLICIES,
                    RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(addAppPolicy, true);

            ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient.getEntity(
                    RestConstants.APPLICATION_POLICIES,
                    "application-policy-application-bursting-test", ApplicationPolicyBean.class,
                    RestConstants.APPLICATION_POLICIES_NAME);

            //deploy the application
            String resourcePath = RestConstants.APPLICATIONS + "/" + "application-bursting-test" +
                    RestConstants.APPLICATIONS_DEPLOY + "/" + "application-policy-application-bursting-test";
            boolean deployed = restClient.deployEntity(resourcePath,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(deployed, true);

            //Application active handling
            TopologyHandler.getInstance().assertApplicationStatus(bean.getApplicationId(),
                    ApplicationStatus.Active);

            //Group active handling
            TopologyHandler.getInstance().assertGroupActivation(bean.getApplicationId());

            //Cluster active handling
            TopologyHandler.getInstance().assertClusterActivation(bean.getApplicationId());

            boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group-application-bursting-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, false);

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, false);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-1",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-application-bursting-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, false);

            //Un-deploying the application
            String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + "application-bursting-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY;

            boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(unDeployed, true);

            boolean undeploy = TopologyHandler.getInstance().assertApplicationUndeploy("application-bursting-test");
            if (!undeploy) {
                //Need to forcefully undeploy the application
                log.info("Force undeployment is going to start for the [application] " + "application-bursting-test");

                restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + "application-bursting-test" +
                        RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

                boolean forceUndeployed = TopologyHandler.getInstance().assertApplicationUndeploy("application-bursting-test");
                assertEquals(String.format("Forceful undeployment failed for the application %s",
                        "application-bursting-test"), forceUndeployed, true);

            }

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "application-bursting-test",
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "application-bursting-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "esb-php-group-application-bursting-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, true);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "esb-application-bursting-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "php-application-bursting-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC2, true);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "tomcat-application-bursting-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC3, true);

            removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-application-bursting-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, false);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, false);

            boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    "application-policy-application-bursting-test", RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(removeAppPolicy, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-bursting-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            log.info("-------------------------------Ended application bursting test case-------------------------------");

        } catch (Exception e) {
            log.error("An error occurred while handling  application bursting", e);
            assertTrue("An error occurred while handling  application bursting", false);
        }
    }
}
