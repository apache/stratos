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
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Sample application tests with application add, .
 */
public class ApplicationUpdateTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(ApplicationUpdateTestCase.class);
    private static final String RESOURCES_PATH = "/application-update-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-application-update-test";
    private static final String cartridgeId1 = "c1-application-update-test";
    private static final String cartridgeId2 = "c2-application-update-test";
    private static final String cartridgeId3 = "c3-application-update-test";
    private static final String networkPartitionId1 = "network-partition-application-update-test-1";
    private static final String networkPartitionId2 = "network-partition-application-update-test-2";
    private static final String deploymentPolicyId = "deployment-policy-application-update-test";
    private static final String cartridgeGroupId = "cartridge-nested-application-update-test";
    private static final String applicationId1 = "g-sc-G123-1-application-update-test";
    private static final String applicationPolicyId = "application-policy-application-update-test";
    private static final String applicationId2 = "g-sc-G123-1-application-update-test-v1";

    @Test(timeOut = APPLICATION_TEST_TIMEOUT, groups = {"stratos.application.deployment"})
    public void testDeployApplication() throws Exception {
        TopologyHandler topologyHandler = TopologyHandler.getInstance();

        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(addedScalingPolicy, true);

        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId1 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC1, true);

        boolean addedC2 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId2 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC2, true);

        boolean addedC3 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId3 + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC3, true);

        boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + cartridgeGroupId + ".json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(addedG1, true);

        CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                getEntity(RestConstants.CARTRIDGE_GROUPS, cartridgeGroupId,
                        CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(beanG1.getName(), cartridgeGroupId);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        networkPartitionId1 + ".json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN1, true);

        boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        networkPartitionId2 + ".json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN2, true);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(addedDep, true);

        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        applicationId1 + ".json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(added, true);

        ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId1, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId1);

        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(addAppPolicy, true);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId1 +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(deployed, true);

        //Application active handling
        topologyHandler.assertApplicationStatus(applicationId1, ApplicationStatus.Active);

        //Group active handling
        topologyHandler.assertGroupActivation(applicationId1);

        //Cluster active handling
        topologyHandler.assertClusterActivation(applicationId1);

        //Updating application
        boolean updated = restClient.updateEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        applicationId2 + ".json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(updated, true);

        ApplicationBean updatedBean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId1, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(updatedBean.getApplicationId(), applicationId1);

        //Need to validate whether the updated taken into the applications Topology
        Application application = ApplicationManager.getApplications().
                getApplication(bean.getApplicationId());

        Group group = application.getGroupRecursively("group3-application-update-test");
        assertEquals(group.getGroupMaxInstances(), 3);
        assertEquals(group.getGroupMinInstances(), 2);
        log.info("Application update is successfully done for [application] " +
                bean.getApplicationId() + " [group] " + group.getUniqueIdentifier());

        ClusterDataHolder clusterDataHolder = application.
                getClusterDataHolderRecursivelyByAlias("c3-1x0-application-update-test");
        assertEquals(clusterDataHolder.getMaxInstances(), 3);
        assertEquals(clusterDataHolder.getMinInstances(), 2);
        log.info("Application update is successfully done for [application] " +
                bean.getApplicationId() + " [Cluster] " + clusterDataHolder.getClusterId());

        topologyHandler.assertGroupInstanceCount(bean.getApplicationId(), "group3-application-update-test", 2);

        topologyHandler.assertClusterMinMemberCount(bean.getApplicationId(), 2);

        boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, cartridgeGroupId,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(removedGroup, false);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(removedAuto, false);

        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, false);

        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(removedDep, false);

        //Un-deploying the application
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId1 +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(unDeployed, true);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(applicationId1);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for the [application] %s", applicationId1));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId1 +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed =
                    topologyHandler.assertApplicationUndeploy(applicationId1);
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    applicationId1), forceUndeployed);

        }

        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, applicationId1,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(removed, true);

        ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId1, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(beanRemoved, null);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, cartridgeGroupId,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(removedGroup, true);

        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId1,
                RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC1, true);

        boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId2,
                RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC2, true);

        boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId3,
                RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC3, true);

        removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(removedAuto, true);

        removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(removedDep, true);

        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, false);

        boolean removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId2, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedN2, false);

        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                applicationPolicyId, RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(removeAppPolicy, true);

        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, true);

        removedN2 = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId2, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedN2, true);
    }
}
