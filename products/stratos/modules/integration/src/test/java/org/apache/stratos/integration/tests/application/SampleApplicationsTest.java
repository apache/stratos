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
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
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
        try {
            String autoscalingPolicyId = "autoscaling-policy-sample-applications-test";

            boolean addedScalingPolicy = restClient.addEntity(TEST_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-sample-applications-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC1, true);

            boolean addedC2 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c2-sample-applications-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC2, true);

            boolean addedC3 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c3-sample-applications-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC3, true);

            boolean addedG1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "cartrdige-nested-sample-applications-test.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(addedG1, true);

            CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(beanG1.getName(), "G1-sample-applications-test");

            boolean addedN1 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-sample-applications-test-1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-sample-applications-test-2.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-sample-applications-test.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            boolean added = restClient.addEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "g-sc-G123-1-sample-applications-test.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(added, true);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1-sample-applications-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "g-sc-G123-1-sample-applications-test");

            assertEquals(bean.getComponents().getGroups().get(0).getName(), "G1-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getAlias(), "group1-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getType(), "c1-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getAlias(), "group2-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getName(), "G2-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c2-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getAlias(), "group3-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getName(), "G3-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 2);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c3-sample-applications-test");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            boolean updated = restClient.updateEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/g-sc-G123-1-sample-applications-test-v1.json",
                    RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
            assertEquals(updated, true);

            ApplicationBean updatedBean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1-sample-applications-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);

            assertEquals(bean.getApplicationId(), "g-sc-G123-1-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getName(), "G1-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getAlias(), "group1-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getType(), "c1-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 3);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getAlias(), "group2-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getName(), "G2-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c2-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 4);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getAlias(), "group3-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getName(), "G3-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 3);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 2);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c3-sample-applications-test");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 3);


            boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertFalse(removedGroup);

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertFalse(removedAuto);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-sample-applications-test-1",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertFalse(removedNet);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-sample-applications-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertFalse(removedDep);

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-sample-applications-test",
                    RestConstants.APPLICATIONS_NAME);
            assertTrue(removed);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1-sample-applications-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, true);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c1-sample-applications-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "c2-sample-applications-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC2, true);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "c3-sample-applications-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC3, true);

            removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-sample-applications-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-sample-applications-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-sample-applications-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(String.format("[Network partition] network-partition-sample-applications-test-2 " +
                    "could not be removed for [application] g-sc-G123-1-sample-applications-test"),removedN2, true);

            log.info("Ended application test case**************************************");

        } catch (Exception e) {
            log.error("An error occurred while handling application test case", e);
            assertTrue("An error occurred while handling application test case", false);
        }
    }
}
