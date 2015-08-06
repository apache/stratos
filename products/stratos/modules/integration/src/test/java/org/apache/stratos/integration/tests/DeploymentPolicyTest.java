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

package org.apache.stratos.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to handle Deployment policy CRUD operations
 */
public class DeploymentPolicyTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(DeploymentPolicyTest.class);
    private static final String TEST_PATH = "/deployment-policy-test";


    @Test
    public void testDeploymentPolicy() {
        try {
            String deploymentPolicyId = "deployment-policy-2";
            log.info("Started deployment policy test case**************************************");

            boolean addedN1 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-5" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-6" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClient.addEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            deploymentPolicyId + ".json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            DeploymentPolicyBean bean = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(bean.getId(), "deployment-policy-2");
            assertEquals(bean.getNetworkPartitions().size(), 2);
            assertEquals(bean.getNetworkPartitions().get(0).getId(), "network-partition-5");
            assertEquals(bean.getNetworkPartitions().get(0).getPartitionAlgo(), "one-after-another");
            assertEquals(bean.getNetworkPartitions().get(0).getPartitions().size(), 1);
            assertEquals(bean.getNetworkPartitions().get(0).getPartitions().get(0).getId(), "partition-1");
            assertEquals(bean.getNetworkPartitions().get(0).getPartitions().get(0).getPartitionMax(), 20);

            assertEquals(bean.getNetworkPartitions().get(1).getId(), "network-partition-6");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitionAlgo(), "round-robin");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().size(), 2);
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(0).getId(),
                    "network-partition-6-partition-1");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(0).getPartitionMax(), 10);
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(1).getId(),
                    "network-partition-6-partition-2");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(1).getPartitionMax(), 9);

            //update network partition
            boolean updated = restClient.updateEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-5-v1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(updated, true);

            //update deployment policy with new partition and max values
            boolean updatedDep = restClient.updateEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH +
                            "/" + deploymentPolicyId + "-v1.json", RestConstants.DEPLOYMENT_POLICIES,
                    RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(updatedDep, true);

            DeploymentPolicyBean updatedBean = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(updatedBean.getId(), "deployment-policy-2");
            assertEquals(updatedBean.getNetworkPartitions().size(), 2);
            assertEquals(updatedBean.getNetworkPartitions().get(0).getId(), "network-partition-5");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitionAlgo(), "one-after-another");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().size(), 2);
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(0).getId(), "partition-1");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(0).getPartitionMax(), 25);
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(1).getId(), "partition-2");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(1).getPartitionMax(), 20);

            assertEquals(updatedBean.getNetworkPartitions().get(1).getId(), "network-partition-6");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitionAlgo(), "round-robin");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().size(), 2);
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(0).getId(),
                    "network-partition-6-partition-1");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(0).getPartitionMax(), 15);
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(1).getId(),
                    "network-partition-6-partition-2");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(1).getPartitionMax(), 5);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-5", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            DeploymentPolicyBean beanRemovedDep = (DeploymentPolicyBean) restClient.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(beanRemovedDep, null);

            boolean removedN1 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-5", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN1, true);

            NetworkPartitionBean beanRemovedN1 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-5",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemovedN1, null);

            boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-6", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            NetworkPartitionBean beanRemovedN2 = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-6",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemovedN2, null);

            log.info("Ended deployment policy test case**************************************");

        } catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }
}
