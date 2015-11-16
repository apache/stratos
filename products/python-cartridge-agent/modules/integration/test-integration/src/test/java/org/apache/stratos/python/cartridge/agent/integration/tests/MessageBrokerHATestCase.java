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

package org.apache.stratos.python.cartridge.agent.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by chamilad on 11/11/15.
 */
public class MessageBrokerHATestCase extends PythonAgentIntegrationTest {
    public MessageBrokerHATestCase() throws IOException {
    }

    private static final Log log = LogFactory.getLog(MessageBrokerHATestCase.class);
    private static final int ADC_TIMEOUT = 300000;
    private static final String CLUSTER_ID = "tomcat.domain";
    private static final String DEPLOYMENT_POLICY_NAME = "deployment-policy-6";
    private static final String AUTOSCALING_POLICY_NAME = "autoscaling-policy-6";
    private static final String APP_ID = "application-6";
    private static final String MEMBER_ID = "tomcat.member-1";
    private static final String INSTANCE_ID = "instance-1";
    private static final String CLUSTER_INSTANCE_ID = "cluster-1-instance-1";
    private static final String NETWORK_PARTITION_ID = "network-partition-1";
    private static final String PARTITION_ID = "partition-1";
    private static final String TENANT_ID = "6";
    private static final String SERVICE_NAME = "tomcat";


    @BeforeMethod(alwaysRun = true)
    public void setup() throws Exception {
        System.setProperty("jndi.properties.dir", getTestCaseResourcesPath());
//        integrationTestPropertiesPath = new FileInputStream(new File(getTestCaseResourcesPath() + PATH_SEP + "integration-test.properties"));

        super.setup(ADC_TIMEOUT);
        startServerSocket(8080);
    }
    
    @AfterMethod(alwaysRun = true)
    public void tearDownBrokerHATest(){
        tearDown();
    }

    @Test(groups = {"test"})
    public void testBrokerFailoverHeartbeat(){
        startCommunicatorThread();
        sleep(10000);
//        assertAgentActivation();

        // take down the default broker
        log.info("Stopping subscribed message broker: DEFAULT");
        stopActiveMQInstance("testBroker-" + amqpBindPorts[0] + "-" + mqttBindPorts[0]);

        List<String> outputLines = new ArrayList<>();
        boolean exit = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Message broker localhost:" + mqttBindPorts[0] + " cannot be reached. Disconnecting client...")) {
                        log.info("Message Broker Heartbeat checker has detected message broker node termination and is trying the next option.");
                        exit = true;
                    }
                }
            }
            sleep(1000);
        }

        sleep(10000);
        log.info("Stopping subscribed message broker");
        stopActiveMQInstance("testBroker-" + amqpBindPorts[1] + "-" + mqttBindPorts[1]);

        exit = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Message broker localhost:" + mqttBindPorts[1] + " cannot be reached. Disconnecting client...")) {
                        log.info("Message Broker Heartbeat checker has detected message broker node termination and is trying the next option.");
                        exit = true;
                    }
                }
            }
            sleep(1000);
        }

        sleep(20000);
        log.info("Stopping subscribed message broker");
        stopActiveMQInstance("testBroker-" + amqpBindPorts[2] + "-" + mqttBindPorts[2]);

        exit = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Message broker localhost:" + mqttBindPorts[2] + " cannot be reached. Disconnecting client...")) {
                        log.info("Message Broker Heartbeat checker has detected message broker node termination and is trying the next option.");
                    }
                    if (line.contains("Could not connect to any of the message brokers provided. Retrying in 2 seconds")) {
                        log.info("Failover went through all the options and will be retrying.");
                        exit = true;
                    }
                }
            }
            sleep(1000);
        }
    }
    
    @Test(groups = {"smoke"})
    public void testBrokerFailoverForPublisher(){
        startCommunicatorThread();


        List<String> outputLines = new ArrayList<>();
        boolean exit = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Subscribed to 'topology/#'")) {
                        // take down the default broker
                        stopActiveMQInstance("testBroker-" + amqpBindPorts[0] + "-" + mqttBindPorts[0]);
                    }

                    if (line.contains("Waiting for complete topology event")) {

                        sleep(4000);

//                        stopActiveMQInstance("testBroker2");
//                        stopActiveMQInstance("testBroker3");
                        // Send complete topology event
                        log.info("Publishing complete topology event...");
                        Topology topology = createTestTopology();
                        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                        publishEvent(completeTopologyEvent);
                        log.info("Complete topology event published");
                    }

                    if (line.contains("Waiting for cartridge agent to be initialized")) {
                        // Publish member initialized event
                        log.info("Publishing member initialized event...");
                        MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(
                                SERVICE_NAME, CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID,
                                PARTITION_ID, INSTANCE_ID
                        );
                        publishEvent(memberInitializedEvent);
                        log.info("Member initialized event published");
                    }


                    // Send artifact updated event to activate the instance first
                    if (line.contains("Artifact repository found")) {
                        publishEvent(getArtifactUpdatedEventForPublicRepo());
                        log.info("Artifact updated event published");
                    }

                    if (line.contains("Could not publish event to message broker localhost:1885.")) {
                        log.info("Event publishing to default message broker failed and the next option is tried.");
                        exit = true;
                    }

//                    if (line.contains("The event will be dropped.")) {
//                        log.info("Event publishing failed after timeout exceeded and the event was dropped.");
//                        exit = true;
//                    }
                }
            }
            sleep(1000);
        }

//        assertAgentActivation();
    }

    private void assertAgentActivation() {
        Thread startupTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!eventReceiverInitiated) {
                    sleep(1000);
                }
                List<String> outputLines = new ArrayList<>();
                boolean completeTopologyPublished = false;
                boolean memberInitPublished = false;
                while (!outputStream.isClosed()) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Waiting for complete topology event") && !completeTopologyPublished) {
                                sleep(2000);
                                // Send complete topology event
                                log.info("Publishing complete topology event...");
                                Topology topology = createTestTopology();
                                CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                                publishEvent(completeTopologyEvent);
                                log.info("Complete topology event published");
                                completeTopologyPublished = true;
                            }

                            if (line.contains("Waiting for cartridge agent to be initialized") && !memberInitPublished) {
                                // Publish member initialized event
                                log.info("Publishing member initialized event...");
                                MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(
                                        SERVICE_NAME, CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID,
                                        PARTITION_ID, INSTANCE_ID
                                );
                                publishEvent(memberInitializedEvent);
                                log.info("Member initialized event published");
                                memberInitPublished = true;
                            }

                            // Send artifact updated event to activate the instance first
                            if (line.contains("Artifact repository found")) {
                                publishEvent(getArtifactUpdatedEventForPublicRepo());
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

    private ArtifactUpdatedEvent getArtifactUpdatedEventForPublicRepo() {
        ArtifactUpdatedEvent publicRepoEvent = createTestArtifactUpdatedEvent();
        publicRepoEvent.setRepoURL("https://bitbucket.org/testapache2211/opentestrepo1.git");
        return publicRepoEvent;
    }

    private static ArtifactUpdatedEvent createTestArtifactUpdatedEvent() {
        ArtifactUpdatedEvent artifactUpdatedEvent = new ArtifactUpdatedEvent();
        artifactUpdatedEvent.setClusterId(CLUSTER_ID);
        artifactUpdatedEvent.setTenantId(TENANT_ID);
        return artifactUpdatedEvent;
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
