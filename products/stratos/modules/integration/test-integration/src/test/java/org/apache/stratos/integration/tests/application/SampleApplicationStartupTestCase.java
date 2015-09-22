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

package org.apache.stratos.integration.tests.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Deploy a sample application on mock IaaS and assert whether application instance, cluster instance, member instances
 * are getting activated. Kill the mock instance and check whether
 */
public class SampleApplicationStartupTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(SampleApplicationStartupTestCase.class);
    private static final String RESOURCES_PATH = "/sample-application-startup-test";

    @Test(timeOut = APPLICATION_TEST_TIMEOUT, description = "Application startup, activation and faulty member " +
            "detection", groups = {"stratos.application.startup", "smoke"})
    public void testApplication() throws Exception {
        String autoscalingPolicyId = "autoscaling-policy-sample-applications-test";
        TopologyHandler topologyHandler = TopologyHandler.getInstance();

        log.info("Adding autoscaling policy [autoscale policy id] " + autoscalingPolicyId);
        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        log.info("Adding cartridge [cartridge type] c1-sample-applications-test");
        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-sample-applications-test.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        log.info("Adding network partition [network partition id] network-partition-sample-applications-test-1");
        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-sample-applications-test-1.json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        log.info("Adding deployment policy [deployment policy id] deployment-policy-sample-applications-test");
        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-sample-applications-test.json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        log.info("Adding application [application id] sample-applications-test-1");
        boolean addedApp = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        "sample-applications-test-1.json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(addedApp, true);

        ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                "sample-applications-test-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), "sample-applications-test-1");

        log.info("Adding application policy [application policy id] application-policy-sample-applications-test");
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        "application-policy-sample-applications-test.json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient.getEntity(
                RestConstants.APPLICATION_POLICIES, "application-policy-sample-applications-test",
                ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(policyBean.getId(), "application-policy-sample-applications-test");

        // Used policies/cartridges should not removed...asserting validations when removing policies
        log.info("Trying to remove the used autoscaling policy...");
        boolean removedUsedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedUsedAuto);

        log.info("Trying to remove the used network partition...");
        boolean removedUsedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-sample-applications-test-1",
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertFalse(removedUsedNet);

        log.info("Trying to remove the used deployment policy...");
        boolean removedUsedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                "deployment-policy-sample-applications-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedUsedDep);

        log.info("Deploying application [application id] sample-applications-test-1 using [application policy id] " +
                "application-policy-sample-applications-test");
        String resourcePath = RestConstants.APPLICATIONS + "/sample-applications-test-1" +
                RestConstants.APPLICATIONS_DEPLOY + "/application-policy-sample-applications-test";
        boolean deployed = restClient.deployEntity(resourcePath,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        log.info("Trying to remove the used application policy");
        boolean removedUsedAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                "application-policy-sample-applications-test", RestConstants.APPLICATION_POLICIES_NAME);
        assertFalse(removedUsedAppPolicy);

        log.info("Trying to remove the deployed application without undeploying first");
        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "sample-applications-test-1",
                RestConstants.APPLICATIONS_NAME);
        assertFalse(removed);

        log.info("Waiting for application status to become ACTIVE...");
        topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Active);

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        log.info("Terminating members in [cluster id] c1-sample-applications-test in mock IaaS directly to simulate " +
                "faulty members...");
        Map<String, Member> memberMap = TopologyHandler.getInstance().getMembersForCluster
                ("c1-sample-applications-test", bean.getApplicationId());
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

        log.info("Un-deploying the application [application id] sample-applications-test-1");
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/sample-applications-test-1" +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy("sample-applications-test-1");
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info("Force undeployment is going to start for the [application] sample-applications-test-1");

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/sample-applications-test-1" +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy("sample-applications-test-1");
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    "sample-applications-test-1"), forceUndeployed);
        }

        log.info("Removing the application [application id] sample-applications-test-1");
        boolean removedApp = restClient.removeEntity(RestConstants.APPLICATIONS, "sample-applications-test-1",
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                "sample-applications-test-1", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info("Removing the application policy [application policy id] application-policy-sample-applications-test");
        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                "application-policy-sample-applications-test", RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info("Removing the cartridge [cartridge type] c1-sample-applications-test");
        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c1-sample-applications-test",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info("Removing the autoscaling policy [autoscaling policy id] " + autoscalingPolicyId);
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info("Removing the deployment policy [deployment policy id] deployment-policy-sample-applications-test");
        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                "deployment-policy-sample-applications-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info("Removing the network partition [network partition id] network-partition-sample-applications-test-1");
        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-sample-applications-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
    }
}
