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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ADCExtensionTestCase extends PythonAgentIntegrationTest {

    public ADCExtensionTestCase() throws IOException {
    }

    private static final Log log = LogFactory.getLog(ADCExtensionTestCase.class);
    private static final int ADC_TIMEOUT = 300000;
    private static final String APPLICATION_PATH = "/tmp/ADCExtensionTestCase";
    private static final String CLUSTER_ID = "tomcat.domain";
    private static final String DEPLOYMENT_POLICY_NAME = "deployment-policy-2";
    private static final String AUTOSCALING_POLICY_NAME = "autoscaling-policy-2";
    private static final String APP_ID = "application-2";
    private static final String MEMBER_ID = "tomcat.member-1";
    private static final String CLUSTER_INSTANCE_ID = "cluster-1-instance-1";
    private static final String NETWORK_PARTITION_ID = "network-partition-1";
    private static final String PARTITION_ID = "partition-1";
    private static final String TENANT_ID = "-1234";
    private static final String SERVICE_NAME = "tomcat";
    private boolean hasADCExtensionTestCompleted = false;

    @BeforeMethod(alwaysRun = true)
    public void setupADCExtensionTest() throws Exception {
        log.info("Setting up ADCExtensionTestCase");
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getCommonResourcesPath());

        // start Python agent with configurations provided in resource path
        super.setup(ADC_TIMEOUT);

        // Simulate server socket
        startServerSocket(8080);
    }

    @Override
    protected String getClassName() {
        return this.getClass().getSimpleName();
    }

    /**
     * TearDown method for test method testPythonCartridgeAgent
     */
    @AfterMethod(alwaysRun = true)
    public void tearDownADCExtensionTest() {
        // TODO: app path is duplicated in Java test and payload
        tearDown(APPLICATION_PATH);
    }

    @Test(timeOut = ADC_TIMEOUT)
    public void testADC() throws Exception {
        startCommunicatorThread();
        Thread adcExtensionTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Running ADC Extension Test thread...");
                // Send artifact updated event
                publishEvent(getArtifactUpdatedEventForPrivateRepo());
                log.info("Publishing artifact updated event for repo: " + getArtifactUpdatedEventForPrivateRepo()
                        .getRepoURL());

                List<String> outputLines = new ArrayList<String>();
                while (!outputStream.isClosed() && !hasADCExtensionTestCompleted) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains(
                                    "Multiple plugins registered for artifact checkout. Stratos agent failed to "
                                            + "start")) {
                                hasADCExtensionTestCompleted = true;
                            }
                        }
                    }
                    sleep(1000);
                }
            }
        });
        adcExtensionTestThread.start();

        while (!hasADCExtensionTestCompleted) {
            // wait until the instance activated event is received.
            sleep(1000);
        }
        log.info("ADC Extension Test completed");
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
