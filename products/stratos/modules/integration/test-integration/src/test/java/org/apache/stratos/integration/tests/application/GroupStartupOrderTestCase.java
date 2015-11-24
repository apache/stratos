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
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Handling the startup order of the group
 */
public class GroupStartupOrderTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(GroupStartupOrderTestCase.class);
    private static final String RESOURCES_PATH = "/group-startup-order-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-group-startup-order-test";
    private static final String esbCartridgeId = "esb-group-startup-order-test";
    private static final String phpCartridgeId = "php-group-startup-order-test";
    private static final String stratosLbCartridgeId = "stratos-lb-group-startup-order-test";
    private static final String tomcat1CartridgeId = "tomcat1-group-startup-order-test";
    private static final String tomcat2CartridgeId = "tomcat2-group-startup-order-test";
    private static final String tomcat3CartridgeId = "tomcat3-group-startup-order-test";
    private static final String tomcatCartridgeId = "tomcat-group-startup-order-test";
    private static final String cartridgeGroupId1 = "group6-group-startup-order-test";
    private static final String cartridgeGroupId2 = "group8-group-startup-order-test";
    private static final String networkPartitionId1 = "network-partition-group-startup-order-test-1";
    private static final String deploymentPolicyId = "deployment-policy-group-startup-order-test";
    private static final String applicationPolicyId = "application-policy-group-startup-order-test";
    private static final String applicationId = "group-startup-order-test";
    private static final int GROUP_ACTIVE_TIMEOUT = 300000;
    private static final int NODES_START_PARALLEL_TIMEOUT = 30000;


    @Test(timeOut = APPLICATION_TEST_TIMEOUT, groups = {"stratos.application.deployment"})
    public void testTerminationBehavior() throws Exception {

        TopologyHandler topologyHandler = TopologyHandler.getInstance();
        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + esbCartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        boolean addedC2 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + phpCartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC2);

        boolean addedC3 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + stratosLbCartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC3);

        boolean addedC5 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + tomcat1CartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC5);

        boolean addedC6 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + tomcat2CartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC6);

        boolean addedC7 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + tomcat3CartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC7);

        boolean addedC8 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + tomcatCartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC8);

        boolean addedG2 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + cartridgeGroupId1 + ".json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG2);

        boolean addedG3 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + cartridgeGroupId2 + ".json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG3);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        networkPartitionId1 + ".json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        applicationId + ".json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(added);

        ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId);

        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        String group6 = topologyHandler.generateId(bean.getApplicationId(),
                "my-group6-group-startup-order-test", bean.getApplicationId() + "-1");

        String group8 = topologyHandler.generateId(bean.getApplicationId(),
                "my-group8-group-startup-order-test", bean.getApplicationId() + "-1");

        String lb = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-stratos-lb-group-startup-order-test");

        String tomcat = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-tomcat-group-startup-order-test");

        assertCreationOfNodes(lb, tomcat);

        assertCreationOfNodes(tomcat, group6);

        assertCreationOfNodesInParallel(group6, group8);

        assertCreationOfNodes(tomcat, group8);

        String group7 = topologyHandler.generateId(bean.getApplicationId(),
                "my-group7-group-startup-order-test", bean.getApplicationId() + "-1");

        String groupTom2 = topologyHandler.generateId(bean.getApplicationId(),
                "my-group6-group-tom2-group-startup-order-test", bean.getApplicationId() + "-1");

        assertCreationOfNodesInParallel(group7, groupTom2);

        String group7Tomcat = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-group7-tomcat-group-startup-order-test");

        String group7Tomcat1 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-group7-tomcat1-group-startup-order-test");

        assertCreationOfNodes(group7Tomcat, group7Tomcat1);

        String groupTom2Tomcat2 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-group-tom2-tomcat2-group-startup-order-test");

        String groupTom2Tomcat3 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-group-tom2-tomcat3-group-startup-order-test");

        assertCreationOfNodes(groupTom2Tomcat2, groupTom2Tomcat3);

        String group8Tomcat2 = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-tomcat2-group8-group-startup-order-test");

        String group8Tomcat = topologyHandler.
                getClusterIdFromAlias(bean.getApplicationId(),
                        "my-tomcat-group8-group-startup-order-test");

        assertCreationOfNodesInParallel(group8Tomcat2, group8Tomcat);

        //Application active handling
        topologyHandler.assertApplicationStatus(bean.getApplicationId(),
                ApplicationStatus.Active);

        //Group active handling
        topologyHandler.assertGroupActivation(bean.getApplicationId());

        //Cluster active handling
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                cartridgeGroupId1,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertFalse(removedGroup);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                cartridgeGroupId2,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertFalse(removedGroup);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedAuto);

        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1,
                RestConstants.NETWORK_PARTITIONS_NAME);
        //Trying to remove the used network partition
        assertFalse(removedNet);

        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedDep);

        //Un-deploying the application
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(applicationId);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for the [application] %s", applicationId));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(applicationId);
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    applicationId), forceUndeployed);

        }

        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, applicationId,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                cartridgeGroupId1, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                cartridgeGroupId2, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, stratosLbCartridgeId,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES,tomcatCartridgeId,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC2);

        boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, tomcat1CartridgeId,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC3);

        boolean removedC4 = restClient.removeEntity(RestConstants.CARTRIDGES, tomcat2CartridgeId,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC4);

        boolean removedC5 = restClient.removeEntity(RestConstants.CARTRIDGES, tomcat3CartridgeId,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC5);

        removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1, RestConstants.NETWORK_PARTITIONS_NAME);
        assertFalse(removedNet);

        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                applicationPolicyId, RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
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
            }
            catch (InterruptedException ignored) {
            }
            activeMembers = TopologyHandler.getInstance().getActivateddMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_ACTIVE_TIMEOUT) {
                break;
            }
        }
        assertTrue(activeMembers.containsKey(firstNodeId));

        while (!createdMembers.containsKey(secondNodeId)) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignore) {
            }
            createdMembers = TopologyHandler.getInstance().getCreatedMembers();
            if ((System.currentTimeMillis() - startTime) > GROUP_ACTIVE_TIMEOUT) {
                break;
            }
        }

        assertTrue(createdMembers.containsKey(secondNodeId));

        assertTrue(createdMembers.get(secondNodeId) > activeMembers.get(firstNodeId));
    }

    private void assertCreationOfNodesInParallel(String firstNodeId, String secondNodeId) {
        //group1 started first, then cluster started later
        long startTime = System.currentTimeMillis();
        Map<String, Long> createdMembers = TopologyHandler.getInstance().getCreatedMembers();
        //Active member should be available at the time cluster is started to create.

        while (!(createdMembers.containsKey(firstNodeId) && createdMembers.containsKey(firstNodeId))) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignored) {
            }
            createdMembers = TopologyHandler.getInstance().getCreatedMembers();
            if ((System.currentTimeMillis() - startTime) > NODES_START_PARALLEL_TIMEOUT) {
                break;
            }
        }
        assertTrue(createdMembers.containsKey(firstNodeId));
        assertTrue(createdMembers.containsKey(firstNodeId));
    }
}