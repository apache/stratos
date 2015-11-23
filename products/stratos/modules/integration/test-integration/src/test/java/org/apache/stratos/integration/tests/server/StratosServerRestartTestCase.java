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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.common.extensions.StratosServerExtension;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.metadata.client.beans.PropertyBean;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Deploy a sample application on mock IaaS and assert whether application instance, cluster instance, member instances
 * are getting activated. Restart the Stratos and check all again.
 */
public class StratosServerRestartTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(StratosServerRestartTestCase.class);
    private static final String RESOURCES_PATH = "/stratos-server-restart-test";

    @Test(timeOut = APPLICATION_TEST_TIMEOUT,
            groups = { "stratos.server.restart", "server" },
            dependsOnGroups = { "stratos.application.deployment" })
    public void stratosServerRestartTest() throws Exception {
        String autoscalingPolicyId = "autoscaling-policy-stratos-server-restart-test";
        TopologyHandler topologyHandler = TopologyHandler.getInstance();

        log.info("Adding autoscaling policy [autoscale policy id] " + autoscalingPolicyId);
        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        log.info("Adding cartridge [cartridge type] c1-stratos-server-restart-test");
        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-stratos-server-restart-test.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        log.info("Adding network partition [network partition id] stratos-server-restart-test");
        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-stratos-server-restart-test.json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        log.info("Adding deployment policy [deployment policy id] deployment-policy-stratos-server-restart-test");
        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-stratos-server-restart-test.json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        log.info("Adding application [application id] stratos-server-restart-test");
        boolean addedApp = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                "stratos-server-restart-test.json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(addedApp, true);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "stratos-server-restart-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), "stratos-server-restart-test");

        log.info(
                "Adding application policy [application policy id] application-policy-stratos-server-restart-test");
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        "application-policy-stratos-server-restart-test.json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient
                .getEntity(RestConstants.APPLICATION_POLICIES, "application-policy-stratos-server-restart-test",
                        ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(policyBean.getId(), "application-policy-stratos-server-restart-test");

        // Used policies/cartridges should not removed...asserting validations when removing policies
        log.info("Trying to remove the used autoscaling policy...");
        boolean removedUsedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedUsedAuto);

        log.info("Trying to remove the used network partition...");
        boolean removedUsedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-stratos-server-restart-test",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertFalse(removedUsedNet);

        log.info("Trying to remove the used deployment policy...");
        boolean removedUsedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-stratos-server-restart-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedUsedDep);

        log.info("Deploying application [application id] stratos-server-restart-test using [application policy id] "
                + "application-policy-stratos-server-restart-test");
        String resourcePath = RestConstants.APPLICATIONS + "/stratos-server-restart-test" +
                RestConstants.APPLICATIONS_DEPLOY + "/application-policy-stratos-server-restart-test";
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        log.info("Trying to remove the used application policy");
        boolean removedUsedAppPolicy = restClient
                .removeEntity(RestConstants.APPLICATION_POLICIES, "application-policy-stratos-server-restart-test",
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertFalse(removedUsedAppPolicy);

        log.info("Trying to remove the deployed application without undeploying first");
        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "stratos-server-restart-test",
                RestConstants.APPLICATIONS_NAME);
        assertFalse(removed);

        log.info("Waiting for application status to become ACTIVE...");
        topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Active);

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        List<Member> memberList = topologyHandler.getMembersForApplication(bean.getApplicationId());
        Assert.assertTrue(memberList.size() == 1,
                String.format("Active member list for application %s is empty", bean.getApplicationId()));

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
        topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Inactive);

        // application should recover itself and become active after spinning more instances
        log.info("Waiting for application status to become ACTIVE...");
        topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Active);

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        // restart stratos server
        StratosServerExtension.restartStratosServer();

        log.info("Waiting for application status to become ACTIVE...");
        topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Active);

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        memberList = topologyHandler.getMembersForApplication(bean.getApplicationId());
        Assert.assertTrue(memberList.size() == 1,
                String.format("Active member list for application %s is empty", bean.getApplicationId()));

        log.info("Un-deploying the application [application id] stratos-server-restart-test");
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/stratos-server-restart-test" +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy("stratos-server-restart-test");
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info("Force undeployment is going to start for the [application] stratos-server-restart-test");

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/stratos-server-restart-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy("stratos-server-restart-test");
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    "stratos-server-restart-test"), forceUndeployed);
        }

        log.info("Removing the application [application id] stratos-server-restart-test");
        boolean removedApp = restClient.removeEntity(RestConstants.APPLICATIONS, "stratos-server-restart-test",
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "stratos-server-restart-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info("Removing the application policy [application policy id] "
                + "application-policy-stratos-server-restart-test");
        boolean removeAppPolicy = restClient
                .removeEntity(RestConstants.APPLICATION_POLICIES, "application-policy-stratos-server-restart-test",
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info("Removing the cartridge [cartridge type] c1-stratos-server-restart-test");
        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c1-stratos-server-restart-test",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info("Removing the autoscaling policy [autoscaling policy id] " + autoscalingPolicyId);
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info("Removing the deployment policy [deployment policy id] "
                + "deployment-policy-stratos-server-restart-test");
        boolean removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-stratos-server-restart-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info("Removing the network partition [network partition id] "
                + "network-partition-stratos-server-restart-test");
        boolean removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-stratos-server-restart-test",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);

        // asserting whether instances are terminated from IaaS
    }
}
