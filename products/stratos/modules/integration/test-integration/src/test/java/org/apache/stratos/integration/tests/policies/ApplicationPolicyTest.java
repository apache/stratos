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

package org.apache.stratos.integration.tests.policies;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test to handle Network partition CRUD operations
 */
public class ApplicationPolicyTest extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(ApplicationPolicyTest.class);
    private static final String RESOURCES_PATH = "/application-policy-test";

    @Test(timeOut = GLOBAL_TEST_TIMEOUT)
    public void testApplicationPolicy() {
        try {
            String applicationPolicyId = "application-policy-application-policy-test";
            log.info("-------------------------Started Application policy test case-------------------------");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-policy-test-1" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN1);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-policy-test-2" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN2);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            applicationPolicyId + ".json",
                    RestConstants.APPLICATION_POLICIES, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(addedDep);

            ApplicationPolicyBean bean = (ApplicationPolicyBean) restClient.
                    getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                            ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(bean.getId(), applicationPolicyId);
            assertEquals(bean.getAlgorithm(), "one-after-another",
                    String.format("The expected algorithm %s is not found in %s", "one-after-another",
                            applicationPolicyId));
            assertEquals(bean.getId(), applicationPolicyId,
                    String.format("The expected id %s is not found", applicationPolicyId));
            assertEquals(bean.getNetworkPartitions().length, 2,
                    String.format("The expected networkpartitions size %s is not found in %s", 2, applicationPolicyId));
            assertEquals(bean.getNetworkPartitions()[0], "network-partition-application-policy-test-1",
                    String.format("The first network partition is not %s in %s",
                            "network-partition-application-policy-test-1", applicationPolicyId));
            assertEquals(bean.getNetworkPartitions()[1], "network-partition-application-policy-test-2",
                    String.format("The Second network partition is not %s in %s",
                            "network-partition-application-policy-test-2", applicationPolicyId));
            boolean algoFound = false;
            for (PropertyBean propertyBean : bean.getProperties()) {
                if (propertyBean.getName().equals("networkPartitionGroups")) {
                    assertEquals(propertyBean.getValue(),
                            "network-partition-application-policy-test-1,network-partition-application-policy-test-2",
                            String.format("The networkPartitionGroups algorithm %s is not found in %s",
                                    "network-partition-application-policy-test-1, network-partition-application-policy-test-2",
                                    applicationPolicyId));
                    algoFound = true;

                }
            }
            if (!algoFound) {
                assertNull(String.format("The networkPartitionGroups property is not found in %s",
                        applicationPolicyId));
            }

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertFalse(removedNet);

            boolean removedDep = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    applicationPolicyId, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(removedDep);

            ApplicationPolicyBean beanRemovedDep = (ApplicationPolicyBean) restClient.
                    getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                            ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
            assertNull(beanRemovedDep);

            boolean removedN1 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN1);

            NetworkPartitionBean beanRemovedN1 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-application-policy-test-1",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN1);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN2);

            NetworkPartitionBean beanRemovedN2 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-application-policy-test-2",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN2);

            log.info("-------------------------Ended deployment policy test case-------------------------");

        }
        catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }

    @Test(timeOut = GLOBAL_TEST_TIMEOUT)
    public void testApplicationPolicyList() {
        try {
            String applicationPolicyId1 = "application-policy-application-policy-test-1";
            String applicationPolicyId2 = "application-policy-application-policy-test-2";
            log.info("-------------------------Started Application policy list test case-------------------------");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-policy-test-1" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN1);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-policy-test-2" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN2);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            applicationPolicyId1 + ".json",
                    RestConstants.APPLICATION_POLICIES, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(addedDep);

            addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            applicationPolicyId2 + ".json",
                    RestConstants.APPLICATION_POLICIES, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(addedDep);

            Type listType = new TypeToken<ArrayList<ApplicationPolicyBean>>() {
            }.getType();

            List<ApplicationPolicyBean> applicationPolicyList = (List<ApplicationPolicyBean>) restClient.
                    listEntity(RestConstants.APPLICATION_POLICIES,
                            listType, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(applicationPolicyList.size() >= 2);

            ApplicationPolicyBean bean1 = null;
            for (ApplicationPolicyBean applicationPolicyBean : applicationPolicyList) {
                if (applicationPolicyBean.getId().equals(applicationPolicyId1)) {
                    bean1 = applicationPolicyBean;
                }
            }
            assertNotNull(bean1);

            ApplicationPolicyBean bean2 = null;
            for (ApplicationPolicyBean applicationPolicyBean : applicationPolicyList) {
                if (applicationPolicyBean.getId().equals(applicationPolicyId2)) {
                    bean2 = applicationPolicyBean;
                }
            }
            assertNotNull(bean2);

            boolean removedDep = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    applicationPolicyId1, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(removedDep);

            ApplicationPolicyBean beanRemovedDep = (ApplicationPolicyBean) restClient.
                    getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId1,
                            ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
            assertNull(beanRemovedDep);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertFalse(removedNet);

            removedDep = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    applicationPolicyId2, RestConstants.APPLICATION_POLICIES_NAME);
            assertTrue(removedDep);

            beanRemovedDep = (ApplicationPolicyBean) restClient.
                    getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId2,
                            ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
            assertNull(beanRemovedDep);

            boolean removedN1 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN1);

            NetworkPartitionBean beanRemovedN1 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-application-policy-test-1",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN1);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN2);

            NetworkPartitionBean beanRemovedN2 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-application-policy-test-2",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN2);

            log.info("-------------------------Ended deployment policy test case-------------------------");

        }
        catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }
}