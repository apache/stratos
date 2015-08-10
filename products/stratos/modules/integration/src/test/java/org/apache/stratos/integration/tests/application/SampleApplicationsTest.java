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
 * Sample application tests with application add, .
 */
public class SampleApplicationsTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(SampleApplicationsTest.class);
    private static final String TEST_PATH = "/sample-applications-test";

    @Test
    public void testApplication() {
        log.info("Started application test case**************************************");
        String autoscalingPolicyId = "autoscaling-policy-1";

        try {
            boolean addedScalingPolicy = restClient.addEntity(TEST_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC1, true);

            boolean addedC2 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c2.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC2, true);

            boolean addedC3 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c3.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC3, true);

            boolean addedG1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "cartrdige-nested.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(addedG1, true);

            CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "G1",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(beanG1.getName(), "G1");

            boolean addedN1 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-2.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-1.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            boolean added = restClient.addEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "g-sc-G123-1.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(added, true);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "g-sc-G123-1");

            assertEquals(bean.getComponents().getGroups().get(0).getName(), "G1");
            assertEquals(bean.getComponents().getGroups().get(0).getAlias(), "group1");
            assertEquals(bean.getComponents().getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getType(), "c1");
            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getAlias(), "group2");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getName(), "G2");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c2");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getAlias(), "group3");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getName(), "G3");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 2);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c3");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            boolean updated = restClient.updateEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/g-sc-G123-1-v1.json",
                    RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
            assertEquals(updated, true);

            ApplicationBean updatedBean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);

            assertEquals(bean.getApplicationId(), "g-sc-G123-1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getName(), "G1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getAlias(), "group1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getType(), "c1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 3);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getAlias(), "group2");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getName(), "G2");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c2");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 4);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getAlias(), "group3");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getName(), "G3");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 3);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 2);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c3");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 3);


          /*  boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, false);

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, false);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-1",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-1", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, false);   */

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "g-sc-G123-1",
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, true);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c1",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "c2",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC2, true);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "c3",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC3, true);

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-1", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            log.info("Ended application test case**************************************");

        } catch (Exception e) {
            log.error("An error occurred while handling application test case", e);
            assertTrue("An error occurred while handling application test case", false);
        }
    }

    @Test(dependsOnMethods = {"testApplication"})
    public void testDeployApplication() {
        try {
            log.info("Started application deploy/undeploy test case**************************************");

            String autoscalingPolicyId = "autoscaling-policy-1";

            boolean addedScalingPolicy = restClient.addEntity(TEST_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC1, true);

            boolean addedC2 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c2.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC2, true);

            boolean addedC3 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c3.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC3, true);

            boolean addedG1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "cartrdige-nested.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(addedG1, true);

            CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "G1",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(beanG1.getName(), "G1");

            boolean addedN1 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-2.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-1.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            boolean added = restClient.addEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "g-sc-G123-1.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(added, true);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "g-sc-G123-1");

            boolean addAppPolicy = restClient.addEntity(TEST_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            "application-policy-1.json", RestConstants.APPLICATION_POLICIES,
                    RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(addAppPolicy, true);

            ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient.getEntity(
                    RestConstants.APPLICATION_POLICIES,
                    "application-policy-1", ApplicationPolicyBean.class,
                    RestConstants.APPLICATION_POLICIES_NAME);

            //deploy the application
            String resourcePath = RestConstants.APPLICATIONS + "/" + "g-sc-G123-1" +
                    RestConstants.APPLICATIONS_DEPLOY + "/" + "application-policy-1";
            boolean deployed = restClient.deployEntity(resourcePath,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(deployed, true);

            //Application active handling
            TopologyHandler.getInstance().assertApplicationActivation(bean.getApplicationId());

            //Group active handling
            TopologyHandler.getInstance().assertGroupActivation(bean.getApplicationId());

            //Cluster active handling
            TopologyHandler.getInstance().assertClusterActivation(bean.getApplicationId());

            //Updating application
            boolean updated = restClient.updateEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "g-sc-G123-1-v1.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(updated, true);

            TopologyHandler.getInstance().assertGroupInstanceCount(bean.getApplicationId(), "group3", 2);

            TopologyHandler.getInstance().assertClusterMinMemberCount(bean.getApplicationId(), 2);

            ApplicationBean updatedBean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(updatedBean.getApplicationId(), "g-sc-G123-1");

          /*  boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, false);      */

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, false);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-1",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-1", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, false);

            //Un-deploying the application
            String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + "g-sc-G123-1" +
                    RestConstants.APPLICATIONS_UNDEPLOY;

            boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(unDeployed, true);

            boolean undeploy = TopologyHandler.getInstance().assertApplicationUndeploy("g-sc-G123-1");
            if (!undeploy) {
                //Need to forcefully undeploy the application
                log.info("Force undeployment is going to start for the [application] " + "g-sc-G123-1");

                restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + "g-sc-G123-1" +
                        RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

                boolean forceUndeployed = TopologyHandler.getInstance().assertApplicationUndeploy("g-sc-G123-1");
                assertEquals(String.format("Forceful undeployment failed for the application %s",
                        "g-sc-G123-1"), forceUndeployed, true);

            }

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "g-sc-G123-1",
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, true);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c1",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "c2",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC2, true);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "c3",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC3, true);

            removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-1", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, false);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, false);

            boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    "application-policy-1", RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(removeAppPolicy, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            log.info("Ended application deploy/undeploy test case**************************************");

        } catch (Exception e) {
            log.error("An error occurred while handling application deployment/undeployment", e);
            assertTrue("An error occurred while handling application deployment/undeployment", false);
        }
    }


}
