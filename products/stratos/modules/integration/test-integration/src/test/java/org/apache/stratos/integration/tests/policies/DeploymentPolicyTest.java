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
import org.apache.stratos.common.beans.partition.NetworkPartitionReferenceBean;
import org.apache.stratos.common.beans.partition.PartitionReferenceBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test to handle Deployment policy CRUD operations
 */
public class DeploymentPolicyTest extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(DeploymentPolicyTest.class);
    private static final String RESOURCES_PATH = "/deployment-policy-test";

    @Test(timeOut = GLOBAL_TEST_TIMEOUT)
    public void testDeploymentPolicy() {
        try {
            String deploymentPolicyId = "deployment-policy-deployment-policy-test";
            log.info("-------------------------Started deployment policy test case-------------------------");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-1" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN1);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-2" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN2);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            deploymentPolicyId + ".json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(addedDep);

            DeploymentPolicyBean bean = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);

            NetworkPartitionReferenceBean nw1 = bean.getNetworkPartitions().get(0);
            NetworkPartitionReferenceBean nw2 = bean.getNetworkPartitions().get(1);
            PartitionReferenceBean nw1P1 = nw1.getPartitions().get(0);
            PartitionReferenceBean nw2P1 = nw2.getPartitions().get(0);
            PartitionReferenceBean nw2P2 = nw2.getPartitions().get(1);

            assertEquals(bean.getId(), "deployment-policy-deployment-policy-test");
            assertEquals(bean.getNetworkPartitions().size(), 2);
            assertEquals(nw1.getId(), "network-partition-deployment-policy-test-1");
            assertEquals(nw1.getPartitionAlgo(), "one-after-another");
            assertEquals(nw1.getPartitions().size(), 1);
            assertEquals(nw1P1.getId(), "partition-1");
            assertEquals(nw1P1.getPartitionMax(), 20);

            assertEquals(nw2.getId(), "network-partition-deployment-policy-test-2");
            assertEquals(nw2.getPartitionAlgo(), "round-robin");
            assertEquals(nw2.getPartitions().size(), 2);
            assertEquals(nw2P1.getId(),
                    "network-partition-6-partition-1");
            assertEquals(nw2P1.getPartitionMax(), 10);
            assertEquals(nw2P2.getId(),
                    "network-partition-6-partition-2");
            assertEquals(nw2P2.getPartitionMax(), 9);

            //update network partition
            boolean updated = restClient.updateEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-1-v1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(updated);

            //update deployment policy with new partition and max values
            boolean updatedDep = restClient.updateEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH +
                            "/" + deploymentPolicyId + "-v1.json", RestConstants.DEPLOYMENT_POLICIES,
                    RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(updatedDep);

            DeploymentPolicyBean updatedBean = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);

            nw1 = updatedBean.getNetworkPartitions().get(0);
            nw2 = updatedBean.getNetworkPartitions().get(1);
            nw1P1 = nw1.getPartitions().get(0);
            PartitionReferenceBean nw1P2 = nw1.getPartitions().get(1);
            nw2P1 = nw2.getPartitions().get(0);
            nw2P2 = nw2.getPartitions().get(1);

            assertEquals(updatedBean.getId(), "deployment-policy-deployment-policy-test");
            assertEquals(updatedBean.getNetworkPartitions().size(), 2);
            assertEquals(nw1.getId(), "network-partition-deployment-policy-test-1");
            assertEquals(nw1.getPartitionAlgo(), "one-after-another");
            assertEquals(nw1.getPartitions().size(), 2);
            assertEquals(nw1P1.getId(), "partition-1");
            assertEquals(nw1P1.getPartitionMax(), 25);
            assertEquals(nw1P2.getId(), "partition-2");
            assertEquals(nw1P2.getPartitionMax(), 20);

            assertEquals(nw2.getId(), "network-partition-deployment-policy-test-2");
            assertEquals(nw2.getPartitionAlgo(), "round-robin");
            assertEquals(nw2.getPartitions().size(), 2);
            assertEquals(nw2P1.getId(),
                    "network-partition-6-partition-1");
            assertEquals(nw2P1.getPartitionMax(), 15);
            assertEquals(nw2P2.getId(),
                    "network-partition-6-partition-2");
            assertEquals(nw2P2.getPartitionMax(), 5);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertFalse(removedNet);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(removedDep);

            DeploymentPolicyBean beanRemovedDep = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertNull(beanRemovedDep);

            boolean removedN1 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN1);

            DeploymentPolicyBean beanRemovedN1 = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-deployment-policy-test-1",
                            DeploymentPolicyBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN1);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN2);

            DeploymentPolicyBean beanRemovedN2 = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-deployment-policy-test-2",
                            DeploymentPolicyBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN2);

            log.info("-------------------------Ended deployment policy test case-------------------------");

        }
        catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }

    @Test(timeOut = GLOBAL_TEST_TIMEOUT)
    public void testDeploymentPolicyList() {
        try {
            String deploymentPolicyId1 = "deployment-policy-deployment-policy-test-1";
            String deploymentPolicyId2 = "deployment-policy-deployment-policy-test-2";

            log.info("-------------------------Started deployment policy list test case-------------------------");

            boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-1" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN1);

            boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-2" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(addedN2);

            boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            deploymentPolicyId1 + ".json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(addedDep);


            addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            deploymentPolicyId2 + ".json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(addedDep);


            Type listType = new TypeToken<ArrayList<DeploymentPolicyBean>>() {
            }.getType();

            List<DeploymentPolicyBean> cartridgeList = (List<DeploymentPolicyBean>) restClient.
                    listEntity(RestConstants.DEPLOYMENT_POLICIES,
                            listType, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(cartridgeList.size() >= 2);

            DeploymentPolicyBean bean1 = null;
            for (DeploymentPolicyBean deploymentPolicyBean : cartridgeList) {
                if (deploymentPolicyBean.getId().equals(deploymentPolicyId1)) {
                    bean1 = deploymentPolicyBean;
                }
            }
            assertNotNull(bean1);

            DeploymentPolicyBean bean2 = null;
            for (DeploymentPolicyBean deploymentPolicyBean : cartridgeList) {
                if (deploymentPolicyBean.getId().equals(deploymentPolicyId2)) {
                    bean2 = deploymentPolicyBean;
                }
            }
            assertNotNull(bean2);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    deploymentPolicyId1, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(removedDep);

            DeploymentPolicyBean beanRemovedDep = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId1,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertNull(beanRemovedDep);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertFalse(removedNet);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    deploymentPolicyId2, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertTrue(removedDep);

            beanRemovedDep = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId2,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertNull(beanRemovedDep);

            boolean removedN1 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN1);

            DeploymentPolicyBean beanRemovedN1 = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-deployment-policy-test-1",
                            DeploymentPolicyBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN1);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertTrue(removedN2);

            DeploymentPolicyBean beanRemovedN2 = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-deployment-policy-test-2",
                            DeploymentPolicyBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertNull(beanRemovedN2);

            log.info("-------------------------Ended deployment policy list test case-------------------------");

        }
        catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }
}