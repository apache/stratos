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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to handle Network partition CRUD operations
 */
public class ApplicationPolicyTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(ApplicationPolicyTest.class);
    private static final String RESOURCES_PATH = "/application-policy-test";


    @Test
    public void testApplicationPolicy() {
        try {
            String applicationPolicyId = "application-policy-application-policy-test";
            log.info("-------------------------Started Application policy test case-------------------------");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-policy-test-1" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-application-policy-test-2" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            applicationPolicyId + ".json",
                    RestConstants.APPLICATION_POLICIES, RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(addedDep, true);

            ApplicationPolicyBean bean = (ApplicationPolicyBean) restClient.
                    getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                            ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(bean.getId(), applicationPolicyId);
            assertEquals(String.format("The expected algorithm %s is not found in %s",
                    "one-after-another", applicationPolicyId), bean.getAlgorithm(), "one-after-another");
            assertEquals(String.format("The expected id %s is not found",
                    applicationPolicyId), bean.getId(), applicationPolicyId);
            assertEquals(String.format("The expected networkpartitions size %s is not found in %s",
                    2, applicationPolicyId), bean.getNetworkPartitions().length, 2);
            assertEquals(String.format("The first network partition is not %s in %s",
                            "network-partition-application-policy-test-1", applicationPolicyId), bean.getNetworkPartitions()[0],
                    "network-partition-application-policy-test-1");
            assertEquals(String.format("The Second network partition is not %s in %s",
                            "network-partition-application-policy-test-2", applicationPolicyId), bean.getNetworkPartitions()[1],
                    "network-partition-application-policy-test-2");
            boolean algoFound = false;
            for (PropertyBean propertyBean : bean.getProperties()) {
                if (propertyBean.getName().equals("networkPartitionGroups")) {
                    assertEquals(String.format("The networkPartitionGroups algorithm %s is not found in %s",
                                    "network-partition-application-policy-test-1,network-partition-application-policy-test-2", applicationPolicyId),
                            propertyBean.getValue(), "network-partition-application-policy-test-1,network-partition-application-policy-test-2");
                    algoFound = true;

                }
            }
            if (!algoFound) {
                assertTrue(String.format("The networkPartitionGroups property is not found in %s",
                        applicationPolicyId), false);
            }

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    applicationPolicyId, RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(removedDep, true);

            ApplicationPolicyBean beanRemovedDep = (ApplicationPolicyBean) restClient.
                    getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                            ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(beanRemovedDep, null);

            boolean removedN1 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN1, true);

            NetworkPartitionBean beanRemovedN1 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-application-policy-test-1",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemovedN1, null);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-policy-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            NetworkPartitionBean beanRemovedN2 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-application-policy-test-2",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemovedN2, null);

            log.info("-------------------------Ended deployment policy test case-------------------------");

        } catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }
}
