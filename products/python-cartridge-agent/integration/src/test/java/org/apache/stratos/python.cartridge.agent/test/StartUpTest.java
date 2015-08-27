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

package org.apache.stratos.python.cartridge.agent.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertTrue;

public class StartUpTest extends PythonTestManager {
    private static final Log log = LogFactory.getLog(StartUpTest.class);
    private static final int STARTUP_TIMEOUT = 30000;
    private static final String RESOURCES_PATH = "/suite-1";
    private static final String CLUSTER_ID = "php.php.domain";
    private static final String DEPLOYMENT_POLICY_NAME = "deployment-policy-1";
    private static final String AUTOSCALING_POLICY_NAME = "autoscaling-policy-1";
    private static final String APP_ID = "application-1";
    private static final String MEMBER_ID = "php.member-1";
    private static final String CLUSTER_INSTANCE_ID = "cluster-1-instance-1";
    private static final String NETWORK_PARTITION_ID = "network-partition-1";
    private static final String PARTITION_ID = "partition-1";
    private static final String TENANT_ID = "-1234";
    private static final String SERVICE_NAME = "php";
    private static final String SOURCE_PATH = "/tmp/stratos-pca-startup-test-app-path/";


    @BeforeSuite
    public void setupStartUpTest() {
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getResourcesPath(RESOURCES_PATH));

        // start Python agent with configurations provided in resource path
        setup(RESOURCES_PATH);
    }


    /**
     * TearDown method for test method testPythonCartridgeAgent
     */
    @AfterSuite
    public void tearDownStartUpTest() {
        tearDown();
    }

    @Test(timeOut = STARTUP_TIMEOUT)
    public void testPythonCartridgeAgent() {
        Thread communicatorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!eventReceiverInitiated) {
                    sleep(2000);
                }
                List<String> outputLines = new ArrayList<String>();
                while (!outputStream.isClosed()) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Exception in thread") || line.contains("ERROR")) {
                                try {
                                    throw new RuntimeException(line);
                                }
                                catch (Exception e) {
                                    log.error("ERROR found in PCA log", e);
                                }
                            }
                            if (line.contains("Subscribed to 'topology/#'")) {
                                sleep(2000);
                                // Send complete topology event
                                log.info("Publishing complete topology event...");
                                Topology topology = createTestTopology();
                                CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                                publishEvent(completeTopologyEvent);
                                log.info("Complete topology event published");

                                // Publish member initialized event
                                log.info("Publishing member initialized event...");
                                MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(
                                        SERVICE_NAME, CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID,
                                        PARTITION_ID
                                );
                                publishEvent(memberInitializedEvent);
                                log.info("Member initialized event published");

                                // Simulate server socket
                                startServerSocket(8080);
                            }
                            /*
                            if (line.contains("Artifact repository found")) {
                                // Send artifact updated event
                                ArrayList<ArtifactUpdatedEvent> list = getArtifactUpdatedEventsAsParams();
                                for (ArtifactUpdatedEvent artifactUpdatedEvent : list) {
                                    publishEvent(artifactUpdatedEvent);
                                }
                            }*/
                            log.info(line);
                        }
                    }
                    sleep(100);
                }
            }
        });

        communicatorThread.start();

        while (!instanceActivated) {
            // wait until the instance activated event is received.
            sleep(2000);
        }

        assertTrue("Instance started event was not received", instanceStarted);
        assertTrue("Instance activated event was not received", instanceActivated);
    }


    /**
     * Create test topology
     *
     * @return
     */
    private Topology createTestTopology() {
        Topology topology = new Topology();
        Service service = new Service(SERVICE_NAME, ServiceType.SingleTenant);
        topology.addService(service);

        Cluster cluster = new Cluster(service.getServiceName(), CLUSTER_ID, DEPLOYMENT_POLICY_NAME,
                AUTOSCALING_POLICY_NAME, APP_ID);
        service.addCluster(cluster);

        Member member = new Member(service.getServiceName(), cluster.getClusterId(), MEMBER_ID,
                CLUSTER_INSTANCE_ID, NETWORK_PARTITION_ID, PARTITION_ID, LoadBalancingIPType.Private,
                System.currentTimeMillis());

        member.setDefaultPrivateIP("10.0.0.1");
        member.setDefaultPublicIP("20.0.0.1");
        Properties properties = new Properties();
        properties.setProperty("prop1", "value1");
        member.setProperties(properties);
        member.setStatus(MemberStatus.Created);
        cluster.addMember(member);

        return topology;
    }
}