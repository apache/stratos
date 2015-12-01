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

package org.apache.stratos.python.cartridge.agent.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static junit.framework.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

public class ADCTestCase extends PythonAgentIntegrationTest {
    private static final Log log = LogFactory.getLog(ADCTestCase.class);
    private static final int ADC_TIMEOUT = 300000;
    private static final String APPLICATION_PATH = "/tmp/ADCTestCase";
    private static final String CLUSTER_ID = "tomcat.domain";
    private static final String DEPLOYMENT_POLICY_NAME = "deployment-policy-2";
    private static final String AUTOSCALING_POLICY_NAME = "autoscaling-policy-2";
    private static final String APP_ID = "application-2";
    private static final String MEMBER_ID = "tomcat.member-1";
    private static final String INSTANCE_ID = "instance-1";
    private static final String CLUSTER_INSTANCE_ID = "cluster-1-instance-1";
    private static final String NETWORK_PARTITION_ID = "network-partition-1";
    private static final String PARTITION_ID = "partition-1";
    private static final String TENANT_ID = "-1234";
    private static final String SERVICE_NAME = "tomcat";

    private boolean hasADCTestCompleted = false;

    public ADCTestCase() throws IOException {
    }

    @Override
    protected String getClassName() {
        return this.getClass().getSimpleName();
    }

    @BeforeMethod(alwaysRun = true)
    public void setupADCTest() throws Exception {
        log.info("Setting up ADCTestCase");
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getCommonResourcesPath());

        // create a dummy directory structure in Git clone path to create a conflicting default artifacts scenario
        File tempWorkDir = new File(APPLICATION_PATH);
        if (!tempWorkDir.mkdir()) {
            throw new Exception("Could not create dir at " + tempWorkDir.getAbsolutePath());
        }
        File dummyDir = new File(APPLICATION_PATH + File.separator + "do_not_delete");
        File dummyFile = new File(APPLICATION_PATH + File.separator + "do_not_delete" + File.separator +
                "do_not_delete");

        if (!dummyDir.mkdir()) {
            throw new Exception("Could not create dir at " + dummyDir.getAbsolutePath());
        }
        if (!dummyFile.createNewFile()) {
            throw new Exception("Could not create file at " + dummyFile.getAbsolutePath());
        }
        // write some dummy string which should get replaced by remote repo's version
        String msg = "This should not be visible";
        Files.write(Paths.get(dummyFile.getAbsolutePath()), msg.getBytes());

        // start Python agent with configurations provided in resource path
        super.setup(ADC_TIMEOUT);

        // Simulate server socket
        startServerSocket(8080);
    }

    /**
     * TearDown method for test method testPythonCartridgeAgent
     */
    @AfterMethod(alwaysRun = true)
    public void tearDownADCTest() {
        // TODO: app path is duplicated in Java test and payload
        tearDown(APPLICATION_PATH);
    }

    @Test(timeOut = ADC_TIMEOUT,
          groups = { "smoke" })
    public void testADC() throws Exception {
        startCommunicatorThread();
        assertAgentActivation();
        Thread adcTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String artifactFileName = "pca-live-" + UUID.randomUUID();
                File file = new File(APPLICATION_PATH + File.separator + artifactFileName);
                boolean fileCreated = false;
                boolean fileDeleted = false;
                boolean repoCloned = false;
                log.info("Running ADC Test thread...");
                // Send artifact updated event
                publishEvent(getArtifactUpdatedEventForPrivateRepo());
                log.info("Publishing artifact updated event for repo: " + getArtifactUpdatedEventForPrivateRepo()
                        .getRepoURL());

                List<String> outputLines = new ArrayList<>();
                while (!outputStream.isClosed() && !hasADCTestCompleted) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Git clone executed")) {
                                log.info("Agent has completed git clone. Asserting the operation...");
                                assertRepoClone(getArtifactUpdatedEventForPrivateRepo());
                                repoCloned = true;
                            }
                            if (repoCloned && !fileCreated) {
                                try {
                                    if (!file.createNewFile()) {
                                        throw new RuntimeException("Could not create [file] " + file.getAbsolutePath());
                                    }
                                    fileCreated = true;
                                    continue;
                                } catch (IOException e) {
                                    log.error("Could not create file", e);
                                }
                            }
                            if (fileCreated && line.contains("ArtifactUpdateTask end of iteration") && !fileDeleted) {
                                if (!file.delete()) {
                                    throw new RuntimeException("Could not delete [file] " + file.getAbsolutePath());
                                }
                                fileDeleted = true;
                                continue;
                            }
                            // assert whether file deletion commit was pushed to remote repo
                            if (fileDeleted && line.contains("ArtifactUpdateTask end of iteration")
                                    && !hasADCTestCompleted) {
                                boolean fileExists = new File(file.getAbsolutePath()).exists();
                                assertFalse("Deleted file has not been pushed to remote repo", fileExists);
                                if (!fileExists) {
                                    hasADCTestCompleted = true;
                                }
                            }
                        }
                    }
                    sleep(1000);
                }
            }
        });
        adcTestThread.start();

        while (!hasADCTestCompleted) {
            // wait until the instance activated event is received.
            sleep(1000);
        }
        log.info("ADC Test completed");
    }

    private void assertAgentActivation() {
        Thread startupTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!eventReceiverInitialized) {
                    sleep(1000);
                }
                List<String> outputLines = new ArrayList<>();
                while (!outputStream.isClosed()) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Subscribed to 'topology/#'")) {
                                sleep(2000);
                                // Send complete topology event
                                log.info("Publishing complete topology event...");
                                Topology topology = PythonAgentIntegrationTest.createTestTopology(
                                        SERVICE_NAME,
                                        CLUSTER_ID,
                                        DEPLOYMENT_POLICY_NAME,
                                        AUTOSCALING_POLICY_NAME,
                                        APP_ID,
                                        MEMBER_ID,
                                        CLUSTER_INSTANCE_ID,
                                        NETWORK_PARTITION_ID,
                                        PARTITION_ID,
                                        ServiceType.SingleTenant);
                                CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                                publishEvent(completeTopologyEvent);
                                log.info("Complete topology event published");

                                // Publish member initialized event
                                log.info("Publishing member initialized event...");
                                MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(SERVICE_NAME,
                                        CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID, PARTITION_ID,
                                        INSTANCE_ID);
                                publishEvent(memberInitializedEvent);
                                log.info("Member initialized event published");
                            }

                            // Send artifact updated event to activate the instance first
                            if (line.contains("Artifact repository found")) {
                                publishEvent(getArtifactUpdatedEventForPrivateRepo());
                                log.info("Artifact updated event published");
                            }
                        }
                    }
                    sleep(1000);
                }
            }
        });
        startupTestThread.start();

        while (!instanceStarted || !instanceActivated) {
            // wait until the instance activated event is received.
            // this will assert whether instance got activated within timeout period; no need for explicit assertions
            sleep(2000);
        }
    }

    private void assertRepoClone(ArtifactUpdatedEvent artifactUpdatedEvent) {
        File file = new File(APPLICATION_PATH + "/README.text");
        assertTrue("Git clone failed for repo [url] " + artifactUpdatedEvent.getRepoURL(), file.exists());
    }

    public static ArtifactUpdatedEvent getArtifactUpdatedEventForPublicRepo() {
        ArtifactUpdatedEvent publicRepoEvent = createTestArtifactUpdatedEvent();
        publicRepoEvent.setRepoURL("https://bitbucket.org/testapache2211/opentestrepo1.git");
        return publicRepoEvent;
    }

    public static ArtifactUpdatedEvent getArtifactUpdatedEventForPrivateRepo() {
        ArtifactUpdatedEvent privateRepoEvent = createTestArtifactUpdatedEvent();
        privateRepoEvent.setRepoURL("https://bitbucket.org/testapache2211/testrepo.git");
        privateRepoEvent.setRepoUserName("testapache2211");
        privateRepoEvent.setRepoPassword("+to2qVW16jzy+Xb/zuafQQ==");
        return privateRepoEvent;
    }

    private static ArtifactUpdatedEvent createTestArtifactUpdatedEvent() {
        ArtifactUpdatedEvent artifactUpdatedEvent = new ArtifactUpdatedEvent();
        artifactUpdatedEvent.setClusterId(CLUSTER_ID);
        artifactUpdatedEvent.setTenantId(TENANT_ID);
        return artifactUpdatedEvent;
    }
}
