/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests.adc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test case to test the /repo/notify endpoint
 */
public class GitHookTestCase extends StratosIntegrationTest {

    private static Log log = LogFactory.getLog(GitHookTestCase.class);
    private static final String RESOURCES_PATH = "/git-hook-test";
    private int artifactUpdateEvents = 0;
    private static final String autoscalePolicyId = "autoscaling-policy-git-hook-test";
    private static final String cartridgeId = "c1-git-hook-test";
    private static final String networkPartitionId = "network-partition-git-hook-test";
    private static final String depPolicyId = "deployment-policy-git-hook-test";
    private static final String applicationId = "git-hook-test";
    private static final String appPolicyId = "application-policy-git-hook-test";

    @Test(timeOut = GLOBAL_TEST_TIMEOUT, groups = {"adc", "smoke"})
    public void sendRepoNotify() throws Exception {
        deployArtifacts();
        ExecutorService eventListenerExecutorService = StratosThreadPool.getExecutorService(
                                                                            "stratos.integration.test.git.thread.pool",
                                                                            5);
        final InstanceNotifierEventReceiver instanceNotifierEventReceiver = new InstanceNotifierEventReceiver();
        eventListenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                instanceNotifierEventReceiver.execute();
            }
        });

        ArtifactUpdateEventListener artifactUpdateEventListener = new ArtifactUpdateEventListener() {
            @Override
            protected void onEvent(Event event) {
                artifactUpdateEvents++;
            }
        };

        instanceNotifierEventReceiver.addEventListener(artifactUpdateEventListener);

        String gitHookFile = "hook-req.json";
        restClient.doPost(
                new URI(stratosBackendURL + RestConstants.REPO_NOTIFY),
                restClient.getJsonStringFromFile(RESOURCES_PATH + "/" + gitHookFile));

        while (artifactUpdateEvents < 2) {
            log.info("Waiting till artifact updated comes in... ");
            Thread.sleep(1000);
        }

        log.info("Waiting for application status to become ACTIVE...");
        TopologyHandler.getInstance().assertApplicationStatus(applicationId, ApplicationStatus.Active);

        undeployArtifacts();
    }

    private void deployArtifacts() throws Exception {
        boolean autoscalePolicyAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalePolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(autoscalePolicyAdded);

        boolean cartridgeAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId + ".json",
                RestConstants.CARTRIDGES,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(cartridgeAdded);

        boolean networkPartitionAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" + networkPartitionId + ".json",
                RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(networkPartitionAdded);

        boolean deploymentPolicyAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" + depPolicyId + ".json",
                RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(deploymentPolicyAdded);

        boolean applicationAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" + applicationId + ".json",
                RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(applicationAdded);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId);

        boolean appPolicyAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" + appPolicyId + ".json",
                RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(appPolicyAdded);

        boolean appDeployed = restClient.deployEntity(
                RestConstants.APPLICATIONS + "/" + applicationId + RestConstants.APPLICATIONS_DEPLOY + "/" + appPolicyId,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(appDeployed);
    }

    private void undeployArtifacts() throws Exception{
        log.info(String.format("Un-deploying the application [application id] %s", applicationId));
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = TopologyHandler.getInstance().assertApplicationUndeploy(applicationId);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for the [application] %s", applicationId));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = TopologyHandler.getInstance().assertApplicationUndeploy(applicationId);
            assertTrue(String.format("Forceful undeployment failed for the application %s", applicationId), forceUndeployed);
        }

        log.info("Removing the application [application id] sample-application-startup-test");
        boolean removedApp = restClient.removeEntity(RestConstants.APPLICATIONS, applicationId,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info(String.format("Removing the application policy [application policy id] %s", appPolicyId));
        boolean removeAppPolicy = restClient
                .removeEntity(RestConstants.APPLICATION_POLICIES, appPolicyId,
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info(String.format("Removing the cartridge [cartridge type] %s", cartridgeId));
        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info(String.format("Removing the autoscaling policy [autoscaling policy id] %s", autoscalePolicyId));
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalePolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info(String.format("Removing the deployment policy [deployment policy id] %s", depPolicyId));
        boolean removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, depPolicyId,
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info(String.format("Removing the network partition [network partition id] %s", networkPartitionId));
        boolean removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
    }
}
