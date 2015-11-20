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
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Test case to test the /repo/notify endpoint
 */
public class GitHookTestCase extends StratosIntegrationTest {

    private static Log log = LogFactory.getLog(GitHookTestCase.class);
    private static final String RESOURCES_PATH = "/git-hook-test";
    private int artifactUpdateEvents = 0;

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
            log.debug("Waiting till artifact updated comes in... ");
            Thread.sleep(1000);
        }
    }

    private void deployArtifacts() throws Exception {
        String autoscalingPolicyFile = "autoscaling-policy-1";
        boolean autoscalingPolicyAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyFile + ".json",
                RestConstants.AUTOSCALING_POLICIES,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(autoscalingPolicyAdded);

        String cartridgeFile = "php";
        boolean cartridgeAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeFile + ".json",
                RestConstants.CARTRIDGES,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(cartridgeAdded);

        String networkPartitionFiile = "network-partition-1";
        boolean networkPartitionAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" + networkPartitionFiile + ".json",
                RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(networkPartitionAdded);

        String depPolicyFile = "deployment-policy-1";
        boolean deploymentPolicyAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" + depPolicyFile + ".json",
                RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(deploymentPolicyAdded);

        String applicationFile = "application";
        boolean applicationAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" + applicationFile + ".json",
                RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(applicationAdded);

        String appPolicyFile = "application-policy-1";
        boolean appPolicyAdded = restClient.addEntity(
                RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" + appPolicyFile + ".json",
                RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(appPolicyAdded);


        boolean appDeployed = restClient.deployEntity(
                RestConstants.APPLICATIONS + "/" + "single-cartridge-app" + RestConstants.APPLICATIONS_DEPLOY + "/" + appPolicyFile,
                RestConstants.APPLICATIONS_NAME);
        assertTrue(appDeployed);
    }
}
