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
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionReferenceBean;
import org.apache.stratos.common.beans.partition.PartitionReferenceBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to handle Deployment policy CRUD operations
 */
public class DeploymentPolicyTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(DeploymentPolicyTest.class);
    private static final String RESOURCES_PATH = "/deployment-policy-test";


    @Test
    public void testDeploymentPolicy() {
        try {
            String deploymentPolicyId = "deployment-policy-deployment-policy-test";
            log.info("-------------------------Started deployment policy test case-------------------------");

            boolean addedN1 = restClientTenant1.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-1" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedN2 = restClientTenant1.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-2" + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN2, true);

            boolean addedDep = restClientTenant1.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            deploymentPolicyId + ".json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            DeploymentPolicyBean bean = (DeploymentPolicyBean) restClientTenant1.
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
            boolean updated = restClientTenant1.updateEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-deployment-policy-test-1-v1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(updated, true);

            //update deployment policy with new partition and max values
            boolean updatedDep = restClientTenant1.updateEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH +
                            "/" + deploymentPolicyId + "-v1.json", RestConstants.DEPLOYMENT_POLICIES,
                    RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(updatedDep, true);

            DeploymentPolicyBean updatedBean = (DeploymentPolicyBean) restClientTenant1.
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

            boolean removedNet = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClientTenant1.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            DeploymentPolicyBean beanRemovedDep = (DeploymentPolicyBean) restClientTenant1.
                    getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                            DeploymentPolicyBean.class, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(beanRemovedDep, null);

            boolean removedN1 = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN1, true);

            NetworkPartitionBean beanRemovedN1 = (NetworkPartitionBean) restClientTenant1.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-deployment-policy-test-1",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemovedN1, null);

            boolean removedN2 = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-deployment-policy-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

            NetworkPartitionBean beanRemovedN2 = (NetworkPartitionBean) restClientTenant1.
                    getEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-deployment-policy-test-2",
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemovedN2, null);

            log.info("-------------------------Ended deployment policy test case-------------------------");

        } catch (Exception e) {
            log.error("An error occurred while handling deployment policy", e);
            assertTrue("An error occurred while handling deployment policy", false);
        }
    }
}
