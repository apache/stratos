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
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.topology.Member;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Handling the termination behavior of the group
 */
@Test(groups = { "application", "failed" })
public class GroupTerminationBehaviorTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(GroupTerminationBehaviorTestCase.class);
    private TopologyHandler topologyHandler = TopologyHandler.getInstance();
    private static final String RESOURCES_PATH = "/group-termination-behavior-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-group-termination-behavior-test";
    private static final String cartridgeId1 = "c1-group-termination-behavior-test";
    private static final String cartridgeId2 = "c2-group-termination-behavior-test";
    private static final String cartridgeId3 = "c3-group-termination-behavior-test";
    private static final String cartridgeId4 = "c4-group-termination-behavior-test";
    private static final String cartridgeGroupId = "cartridge-groups-group-termination-behavior-test";
    private static final String networkPartitionId1 = "network-partition-group-termination-behavior-test-1";
    private static final String deploymentPolicyId = "deployment-policy-group-termination-behavior-test";
    private static final int GROUP_INACTIVE_TIMEOUT = 180000;

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT)
    public void testTerminationBehavior() throws Exception {
        log.info("Running GroupTerminationBehaviorTestCase.testTerminationBehavior test method...");
        long startTime = System.currentTimeMillis();

        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId1 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        boolean addedC2 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId2 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC2);

        boolean addedC3 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId3 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC3);

        boolean addedC4 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId4 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC4);

        boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                "/" + cartridgeGroupId + ".json", RestConstants.CARTRIDGE_GROUPS, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG1);

        CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                getEntity(RestConstants.CARTRIDGE_GROUPS, "g-sc-G4-group-termination-behavior-test",
                        CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(beanG1.getName(), "g-sc-G4-group-termination-behavior-test");

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                networkPartitionId1 + ".json", RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        final String applicationId = "group-termination-behavior-test";
        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                applicationId + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertTrue(added);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), "group-termination-behavior-test");

        final String applicationPolicyId = "application-policy-group-termination-behavior-test";
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        String groupId = topologyHandler.generateId(bean.getApplicationId(), "g-G1-1x0-group-termination-behavior-test",
                bean.getApplicationId() + "-1");

        String clusterIdC3 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(), "c3-1x0-group-termination-behavior-test");

        String clusterIdC4 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(), "c4-1x0-group-termination-behavior-test");

        String clusterIdC2 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(), "c2-1x0-group-termination-behavior-test");

        assertCreationOfNodes(groupId, clusterIdC2);

        assertCreationOfNodes(clusterIdC3, clusterIdC4);

        //Application active handling
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        //Group active handling
        topologyHandler.assertGroupActivation(bean.getApplicationId());

        //Cluster active handling
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        Map<String, Member> memberMap = TopologyHandler.getInstance()
                .getMembersForCluster("c3-group-termination-behavior-test", bean.getApplicationId());

        //Terminate members in the cluster
        for (Map.Entry<String, Member> entry : memberMap.entrySet()) {
            String memberId = entry.getValue().getMemberId();
            TopologyHandler.getInstance().terminateMemberInMockIaas(memberId, mockIaasApiClient);
            TopologyHandler.getInstance().assertMemberTermination(memberId);
        }

        List<String> clusterIds = new ArrayList<String>();
        clusterIds.add(clusterIdC3);
        clusterIds.add(clusterIdC4);
        clusterIds.add(clusterIdC2);

        assertGroupInactive(groupId, clusterIdC3);

        assertTerminatingOfNodes(groupId, clusterIds);

        assertTerminationOfNodes(groupId, clusterIds);

        assertCreationOfNodes(groupId, clusterIdC2);

        assertCreationOfNodes(clusterIdC3, clusterIdC4);

        //Application active handling
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        //Group active handling
        topologyHandler.assertGroupActivation(bean.getApplicationId());

        //Cluster active handling
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        //Un-deploying the application
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(applicationId);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for the [application] %s", applicationId));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(applicationId);
            assertTrue(String.format("Forceful undeployment failed for the application %s", applicationId),
                    forceUndeployed);

        }

        boolean removed = restClient
                .removeEntity(RestConstants.APPLICATIONS, applicationId, RestConstants.APPLICATIONS_NAME);
        assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        boolean removedGroup = restClient
                .removeEntity(RestConstants.CARTRIDGE_GROUPS, "g-sc-G4-group-termination-behavior-test",
                        RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId1, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        boolean removedC2 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId2, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC2);

        boolean removedC3 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId3, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC3);

        boolean removedC4 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId4, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC4);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId1,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);

        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("GroupTerminationBehaviorTestCase completed in [duration] %s ms", duration));
    }

    private void assertGroupInactive(String groupId, String clusterId) {
        long startTime = System.currentTimeMillis();
        Map<String, Long> inActiveMap = TopologyHandler.getInstance().getInActiveMembers();

        while (!inActiveMap.containsKey(clusterId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            inActiveMap = TopologyHandler.getInstance().getInActiveMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                break;
            }
        }
        assertTrue(inActiveMap.containsKey(clusterId));

        while (!inActiveMap.containsKey(groupId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            inActiveMap = TopologyHandler.getInstance().getInActiveMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                break;
            }
        }
        assertTrue(inActiveMap.containsKey(groupId));

    }

    private void assertTerminatingOfNodes(String groupId, List<String> clusterIds) {
        Map<String, Long> terminatingMembers = TopologyHandler.getInstance().getTerminatingMembers();
        for (String clusterId : clusterIds) {
            long startTime = System.currentTimeMillis();
            while (!terminatingMembers.containsKey(clusterId)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                terminatingMembers = TopologyHandler.getInstance().getTerminatingMembers();
                if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                    break;
                }
            }
            assertTrue(terminatingMembers.containsKey(groupId));
        }
        long startTime = System.currentTimeMillis();
        while (!terminatingMembers.containsKey(groupId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            terminatingMembers = TopologyHandler.getInstance().getTerminatingMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                break;
            }
        }
        assertTrue(terminatingMembers.containsKey(groupId));

    }

    private void assertTerminationOfNodes(String groupId, List<String> clusterIds) {
        long startTime = System.currentTimeMillis();
        Map<String, Long> terminatedMembers = TopologyHandler.getInstance().getTerminatedMembers();

        for (String clusterId : clusterIds) {
            while (!terminatedMembers.containsKey(clusterId)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                terminatedMembers = TopologyHandler.getInstance().getTerminatedMembers();
                if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                    break;
                }
            }
            assertTrue(terminatedMembers.containsKey(clusterId));
        }

        while (!terminatedMembers.containsKey(groupId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            terminatedMembers = TopologyHandler.getInstance().getTerminatedMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                break;
            }
        }

        assertTrue(terminatedMembers.containsKey(groupId));
    }

    private void assertCreationOfNodes(String firstNodeId, String secondNodeId) {
        //group1 started first, then cluster started later
        long startTime = System.currentTimeMillis();
        Map<String, Long> activeMembers = TopologyHandler.getInstance().getActivateddMembers();
        Map<String, Long> createdMembers = TopologyHandler.getInstance().getCreatedMembers();
        //Active member should be available at the time cluster is started to create.
        while (!activeMembers.containsKey(firstNodeId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            activeMembers = TopologyHandler.getInstance().getActivateddMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                break;
            }
        }
        assertTrue(activeMembers.containsKey(firstNodeId));

        while (!createdMembers.containsKey(secondNodeId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            createdMembers = TopologyHandler.getInstance().getCreatedMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_INACTIVE_TIMEOUT) {
                break;
            }
        }
        assertTrue(createdMembers.containsKey(secondNodeId));
        assertTrue(createdMembers.get(secondNodeId) > activeMembers.get(firstNodeId));

        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("GroupTerminationBehaviorTestCase completed in [duration] %s ms", duration));
    }
}
