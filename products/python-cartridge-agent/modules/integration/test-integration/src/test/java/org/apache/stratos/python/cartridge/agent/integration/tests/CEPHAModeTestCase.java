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
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.apache.stratos.messaging.listener.instance.status.InstanceActivatedEventListener;
import org.apache.stratos.python.cartridge.agent.integration.common.ThriftTestServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.core.AgentCallback;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CEPHAModeTestCase extends PythonAgentIntegrationTest {

    public CEPHAModeTestCase() throws IOException {
    }

    private static final Log log = LogFactory.getLog(CEPHAModeTestCase.class);
    private static final int STARTUP_TIMEOUT = 5 * 60000;
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
    private boolean startupTestCompleted = false;
    private Topology topology = createTestTopology();
    private static final int ADC_TIMEOUT = 300000;
    private ThriftTestServer secondThriftTestServer;
    private boolean thriftTestCompletedinServerTwo = false;
    private boolean thriftTestCompletedinServerOne = false;
    private boolean failDetected = false;


    @BeforeMethod(alwaysRun = true)
    public void setupADCExtensionTest() throws Exception {
        log.info("Setting up ADCExtensionTestCase");
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getCommonResourcesPath());

        // start Python agent with configurations provided in resource path
        super.setup(ADC_TIMEOUT);

        secondThriftTestServer = new ThriftTestServer();

        File file =
                new File(getResourcesPath() + PATH_SEP + "common" + PATH_SEP + "stratos-health-stream-def.json");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");
        if (str.equals("")) {
            log.warn("Stream definition of health stat stream is empty. Thrift server will not function properly");
        }
        secondThriftTestServer.addStreamDefinition(str, Integer.parseInt(TENANT_ID));
        // start with non-ssl port; test server will automatically bind to ssl port
        secondThriftTestServer.start(cepPort + 1);
        log.info("Started Thrift server with stream definition: " + str);

        // Simulate server socket
        startServerSocket(8080);
    }


    /**
     * TearDown method for test method testPythonCartridgeAgent
     */
    @AfterMethod(alwaysRun = true)
    public void tearDownAgentStartupTest() {
        tearDown();

        try {
            if (secondThriftTestServer != null) {
                secondThriftTestServer.stop();
            }
        } catch (Exception ignore) {
        }

    }

    @Test(timeOut = STARTUP_TIMEOUT, description = "Test PCA initialization, activation, health stat publishing in " +
            "CEP HA mode", groups = {"smoke"})
    public void testPythonCartridgeAgent() {
        startCommunicatorThread();
        subscribeToThriftDatabridge();
        subscribeToSecondThriftDatabridge();
        Thread startupTestThread = new Thread(new Runnable() {
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
                            if (line.contains("Subscribed to 'topology/#'")) {
                                sleep(2000);
                                // Send complete topology event
                                log.info("Publishing complete topology event...");
                                CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                                publishEvent(completeTopologyEvent);
                                log.info("Complete topology event published");

                                // Publish member initialized event
                                log.info("Publishing member initialized event...");
                                MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(
                                        SERVICE_NAME, CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID,
                                        PARTITION_ID, INSTANCE_ID
                                );
                                publishEvent(memberInitializedEvent);
                                log.info("Member initialized event published");
                            }

                            if (line.contains("Published event to thrift stream")) {
                                startupTestCompleted = true;
                            }

                            if (line.contains("Couldn't publish health statistics to CEP. Thrift Receiver offline.")) {
                                failDetected = true;
                            }

                        }
                    }
                    sleep(1000);
                }
            }
        });

        startupTestThread.start();

        instanceStatusEventReceiver.addEventListener(new InstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Publishing complete topology with a new member...");
                Member newMember = new Member(SERVICE_NAME, CLUSTER_ID, "new-member", CLUSTER_INSTANCE_ID,
                        NETWORK_PARTITION_ID, PARTITION_ID, LoadBalancingIPType.Private, System.currentTimeMillis());
                topology.getService(SERVICE_NAME).getCluster(CLUSTER_ID).addMember(newMember);
                CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                publishEvent(completeTopologyEvent);
                log.info("Complete topology event published with new member");
            }
        });

        while (!instanceStarted || !instanceActivated || !startupTestCompleted ||
                !thriftTestCompletedinServerOne || !thriftTestCompletedinServerTwo) {
            // wait until the instance activated event is received.
            // this will assert whether instance got activated within timeout period; no need for explicit assertions
            sleep(2000);
        }

        thriftTestServer.stop();
        startupTestCompleted = false;
        thriftTestCompletedinServerTwo = false;

        while (!startupTestCompleted || !failDetected || !thriftTestCompletedinServerTwo) {
            // wait until PCA publishes health stats to second thrift server
            sleep(2000);
        }

        thriftTestCompletedinServerOne = false;

        try {
            thriftTestServer.start(cepPort);
            subscribeToThriftDatabridge();
        } catch (DataBridgeException e) {
            e.printStackTrace();
        }

        while (!thriftTestCompletedinServerOne) {
            // wait until PCA publishes health stats to started node
            sleep(2000);
        }

    }

    private void subscribeToThriftDatabridge() {
        thriftTestServer.getDatabridge().subscribe(new AgentCallback() {
            @Override
            public void definedStream(StreamDefinition streamDefinition, int tenantId) {
                // ignore
            }

            @Override
            public void removeStream(StreamDefinition streamDefinition, int tenantId) {
                // ignore
            }

            @Override
            public void receive(List<org.wso2.carbon.databridge.commons.Event> eventList, Credentials credentials) {
                log.info("Event list size of thrift server one: " + eventList.size());
                log.info("Recent events received for thrift server one: " + eventList);
                // if the list contains an event that means PCA was able to successfully publish health stats
                if (eventList.size() > 0) {
                    thriftTestCompletedinServerOne = true;
                }
            }
        });
    }

    private void subscribeToSecondThriftDatabridge() {
        secondThriftTestServer.getDatabridge().subscribe(new AgentCallback() {
            @Override
            public void definedStream(StreamDefinition streamDefinition, int tenantId) {
                // ignore
            }

            @Override
            public void removeStream(StreamDefinition streamDefinition, int tenantId) {
                // ignore
            }

            @Override
            public void receive(List<org.wso2.carbon.databridge.commons.Event> eventList, Credentials credentials) {
                log.info("Event list size of thrift server two: " + eventList.size());
                log.info("Recent events received for thrift server two: " + eventList);
                // if the list contains an event that means PCA was able to successfully publish health stats
                if (eventList.size() > 0) {
                    thriftTestCompletedinServerTwo = true;
                }
            }
        });
    }

    /**
     * Create test topology
     *
     * @return Topology object with mock information
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
