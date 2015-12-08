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
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Test case to test the message broker connection resilience in the Python Cartridge Agent
 */
public class MessageBrokerHATestCase extends PythonAgentIntegrationTest {
    public MessageBrokerHATestCase() throws IOException {
    }

    @Override
    protected String getClassName() {
        return this.getClass().getSimpleName();
    }

    private static final Log log = LogFactory.getLog(MessageBrokerHATestCase.class);
    private static final int HA_TEST_TIMEOUT = 300000;
    private static final String CLUSTER_ID = "php.php.domain";
    private static final String DEPLOYMENT_POLICY_NAME = "deployment-policy-1";
    private static final String AUTOSCALING_POLICY_NAME = "autoscaling-policy-1";
    private static final String APP_ID = "application-1";
    private static final String MEMBER_ID = "php.member-1";
    private static final String INSTANCE_ID = "instance-1";
    private static final String CLUSTER_INSTANCE_ID = "cluster-1-instance-1";
    private static final String NETWORK_PARTITION_ID = "network-partition-1";
    private static final String PARTITION_ID = "partition-1";
    private static final String TENANT_ID = "-1234";
    private static final String SERVICE_NAME = "php";
    boolean pcaActivated = false;

    @BeforeMethod(alwaysRun = true)
    public void setup() throws Exception {
        System.setProperty("jndi.properties.dir", getTestCaseResourcesPath());
        super.setup(HA_TEST_TIMEOUT);
        startServerSocket(8080);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownBrokerHATest() {
        tearDown();
    }

    @Test(timeOut = HA_TEST_TIMEOUT,
          groups = { "ha" },
          priority = 1)
    public void testBrokerFailoverHeartbeat() {
        log.info("Running MessageBrokerHATestCase subscriber failover test...");
        startCommunicatorThread();
        assertAgentActivation();
        sleep(10000);

        // take down the default broker
        log.info("Stopping subscribed message broker: DEFAULT");
        stopActiveMQInstance("testBroker-" + amqpBindPorts[0] + "-" + mqttBindPorts[0]);

        List<String> outputLines = new ArrayList<>();
        boolean exit = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Message broker localhost:" + mqttBindPorts[0]
                            + " cannot be reached. Disconnecting client...")) {
                        log.info("Message Broker Heartbeat checker has detected message broker node termination and is"
                                + " trying the next option.");
                        exit = true;
                    }
                }
            }
            log.info("Waiting for message broker subscriber failover detection for the 1st time.");
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
                    if (line.contains("Message broker localhost:" + mqttBindPorts[1]
                            + " cannot be reached. Disconnecting client...")) {
                        log.info("Message Broker Heartbeat checker has detected message broker node termination and is"
                                + " trying the next option.");
                        exit = true;
                    }
                }
            }
            log.info("Waiting for message broker subscriber failover detection for the 2nd time.");
            sleep(1000);
        }

        log.info("Stopping subscribed message broker");
        stopActiveMQInstance("testBroker-" + amqpBindPorts[2] + "-" + mqttBindPorts[2]);

        exit = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Message broker localhost:" + mqttBindPorts[2]
                            + " cannot be reached. Disconnecting client...")) {
                        log.info("Message Broker Heartbeat checker has detected message broker node termination and is"
                                + " trying the next option.");
                    }
                    if (line.contains(
                            "Could not connect to any of the message brokers provided. Retrying in 2 seconds")) {
                        log.info("Failover went through all the options and will be retrying.");
                        exit = true;
                    }
                }
            }
            log.info("Waiting for message broker subscriber failover detection for the 3rd time.");
            sleep(1000);
        }
        log.info("MessageBrokerHATestCase subscriber test completed successfully.");
    }

    @Test(timeOut = HA_TEST_TIMEOUT,
          groups = { "ha" },
          priority = 2)
    public void testBrokerFailoverForPublisher() {
        log.info("Running MessageBrokerHATestCase publisher failover test...");
        startCommunicatorThread();
        assertAgentActivation();
        List<String> outputLines = new ArrayList<>();
        boolean exit = false;
        boolean publishCleanupEvent = false;
        while (!exit) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (!publishCleanupEvent) {
                        log.info("Publishing instance cleanup member event and shutting down first MB instance...");

                        // publish instance cleanup event to trigger an ready to shutdown event being published from PCA
                        InstanceCleanupMemberEvent instanceCleanupMemberEvent = new InstanceCleanupMemberEvent(
                                MEMBER_ID);
                        publishEvent(instanceCleanupMemberEvent);
                        publishCleanupEvent = true;

                        stopActiveMQInstance("testBroker-" + amqpBindPorts[0] + "-" + mqttBindPorts[0]);
                    }

                    if (line.contains("Could not publish [event] ")) {
                        log.info("Event publishing to default message broker failed and the next option is tried.");
                        exit = true;
                    }
                }
            }
            log.info("Waiting for message broker publisher failover detection.");
            sleep(1000);
        }

        //        assertAgentActivation();
        log.info("MessageBrokerHATestCase publisher test completed successfully.");
    }

    private void assertAgentActivation() {
        pcaActivated = false;
        instanceActivated = false;
        instanceStarted = false;
        Thread startupTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!eventReceiverInitialized) {
                    log.info("Waiting until event receiver is initialized...");
                    sleep(1000);
                }
                List<String> outputLines = new ArrayList<String>();
                while (!outputStream.isClosed() && !pcaActivated) {
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
                                sleep(2000);

                                // Publish member initialized event
                                log.info("Publishing member initialized event...");
                                MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(SERVICE_NAME,
                                        CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID, PARTITION_ID,
                                        INSTANCE_ID);
                                publishEvent(memberInitializedEvent);
                                log.info("Member initialized event published");
                            }
                        }
                    }
                    sleep(1000);
                }
                log.info("Startup test thread finished.");
            }
        });
        startupTestThread.start();

        while (!instanceStarted || !instanceActivated) {
            // wait until the instance activated event is received.
            // this will assert whether instance got activated within timeout period; no need for explicit assertions
            log.info("Waiting for agent activation...");
            sleep(2000);
        }
        pcaActivated = true;
        log.info("PCA activation assertion passed.");
    }


}
