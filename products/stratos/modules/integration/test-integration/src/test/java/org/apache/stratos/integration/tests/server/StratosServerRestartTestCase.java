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

package org.apache.stratos.integration.tests.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.common.extensions.StratosServerExtension;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.topology.Member;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Deploy a sample application on mock IaaS and assert whether application instance, cluster instance, member instances
 * are getting activated. Restart the Stratos and check all again.
 */
@Test(groups = { "server" },
      dependsOnGroups = { "adc", "application", "cartridge", "iaas", "policies", "users" },
      alwaysRun = true)
public class StratosServerRestartTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(StratosServerRestartTestCase.class);
    private static final String RESOURCES_PATH = "/stratos-server-restart-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-stratos-server-restart-test";
    private static final String cartridgeId = "c1-stratos-server-restart-test";
    private static final String networkPartitionId = "network-partition-stratos-server-restart-test";
    private static final String deploymentPolicyId = "deployment-policy-stratos-server-restart-test";
    private static final String applicationId = "stratos-server-restart-test";
    private static final String applicationPolicyId = "application-policy-stratos-server-restart-test";
    private TopologyHandler topologyHandler = TopologyHandler.getInstance();

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT)
    public void stratosServerRestartTest() throws Exception {
        log.info("Running StratosServerRestartTestCase.stratosServerRestartTest test method...");
        long startTime = System.currentTimeMillis();

        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        log.info(String.format("Adding cartridge [cartridge type] %s", cartridgeId));
        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        log.info(String.format("Adding network partition [network partition id] %s", networkPartitionId));
        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                networkPartitionId + ".json", RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        log.info(String.format("Adding deployment policy [deployment policy id] %s", deploymentPolicyId));
        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        log.info(String.format("Adding application [application id] %s", applicationId));
        boolean addedApp = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                applicationId + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(addedApp, true);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId);

        log.info(String.format("Adding application policy [application policy id] %s", applicationPolicyId));
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient
                .getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId, ApplicationPolicyBean.class,
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(policyBean.getId(), applicationPolicyId);

        log.info(String.format("Deploying application [application id] %s using [application policy id] %s",
                applicationId, applicationPolicyId));
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        log.info("Waiting for application status to become ACTIVE...");
        TopologyHandler.getInstance().assertApplicationActiveStatus(applicationId);

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(applicationId);

        List<Member> memberList = topologyHandler.getMembersForApplication(applicationId);
        Assert.assertTrue(memberList.size() == 1,
                String.format("Active member list for application %s is empty", applicationId));

        /*
        * Restarting Stratos server
        */
        StratosServerExtension.restartStratosServer();

        /*
        * Assert whether cluster monitors were re-created by terminating mock instances. Application status should
        * become inactive
        */
        log.info("Terminating members in [cluster id] c1-stratos-server-restart-test in mock IaaS directly to "
                + "simulate faulty members...");
        Map<String, Member> memberMap = TopologyHandler.getInstance()
                .getMembersForCluster("c1-stratos-server-restart-test", bean.getApplicationId());
        for (Map.Entry<String, Member> entry : memberMap.entrySet()) {
            String memberId = entry.getValue().getMemberId();
            TopologyHandler.getInstance().terminateMemberInMockIaas(memberId, mockIaasApiClient);
            TopologyHandler.getInstance().assertMemberTermination(memberId);
        }
        // application status should be marked as inactive since some members are faulty
        log.info("Waiting for application status to become INACTIVE");
        TopologyHandler.getInstance().assertApplicationInActiveStatus(bean.getApplicationId());

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        // application should recover itself and become active after spinning more instances
        log.info("Waiting for application status to become ACTIVE...");
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        memberList = topologyHandler.getMembersForApplication(bean.getApplicationId());
        Assert.assertTrue(memberList.size() == 1,
                String.format("Active member list for application %s is empty", bean.getApplicationId()));

        log.info(String.format("Un-deploying the application [application id] %s", applicationId));
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

        log.info(String.format("Removing the application [application id] %s", applicationId));
        boolean removedApp = restClient
                .removeEntity(RestConstants.APPLICATIONS, applicationId, RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info(String.format("Removing the application policy [application policy id] %s", applicationPolicyId));
        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info(String.format("Removing the cartridge [cartridge type] %s", cartridgeId));
        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId, RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info(String.format("Removing the autoscaling policy [autoscaling policy id] %s", autoscalingPolicyId));
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info(String.format("Removing the deployment policy [deployment policy id] %s", deploymentPolicyId));
        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info(String.format("Removing the network partition [network partition id] %s", networkPartitionId));
        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);

        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("StratosServerRestartTestCase completed in [duration] %s ms", duration));
    }
}
