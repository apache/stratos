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
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.apache.stratos.messaging.listener.initializer.CompleteTenantRequestEventListener;
import org.apache.stratos.messaging.listener.initializer.CompleteTopologyRequestEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceActivatedEventListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.core.AgentCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AgentStartupTestCase extends PythonAgentIntegrationTest {
    private static final Log log = LogFactory.getLog(AgentStartupTestCase.class);
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
    private boolean topologyContextTestCompleted = false;
    private boolean completeTenantInitialized = false;
    private boolean thriftTestCompleted = false;
    private Topology topology = PythonAgentIntegrationTest.createTestTopology(
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

    public AgentStartupTestCase() throws IOException {
    }

    @BeforeMethod(alwaysRun = true)
    public void setupAgentStartupTest() throws Exception {
        log.info("Setting up AgentStartupTestCase");
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getCommonResourcesPath());

        // start Python agent with configurations provided in resource path
        super.setup(STARTUP_TIMEOUT);

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
    public void tearDownAgentStartupTest() {
        tearDown();
    }

    @Test(timeOut = STARTUP_TIMEOUT,
          description = "Test PCA initialization, activation, health stat publishing and " + "topology context update",
          groups = { "smoke" })
    public void testPythonCartridgeAgent() {
        startCommunicatorThread();
        subscribeToThriftDatabridge();
        Thread startupTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!eventReceiverInitialized) {
                    sleep(2000);
                }
                List<String> outputLines = new ArrayList<String>();
                while (!outputStream.isClosed()) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Published event to thrift stream")) {
                                startupTestCompleted = true;
                            }

                            // assert topology context update
                            if (line.contains("Topology context update test passed!")) {
                                topologyContextTestCompleted = true;
                            }

                            // assert complete tenant initialization
                            if (line.contains("Tenant context updated with")){
                                completeTenantInitialized = true;
                            }
                        }
                    }
                    sleep(1000);
                }
            }
        });

        startupTestThread.start();

        initializerEventReceiver.addEventListener(new CompleteTopologyRequestEventListener() {
            @Override
            protected void onEvent(Event event) {
                // Send complete topology event
                log.info("CompleteTopologyRequestEvent received. Publishing complete topology event...");
                CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                publishEvent(completeTopologyEvent);
                log.info("Complete topology event published");

                // Publish member initialized event
                log.info("Publishing member initialized event...");
                MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(SERVICE_NAME, CLUSTER_ID,
                        CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID, PARTITION_ID, INSTANCE_ID);
                publishEvent(memberInitializedEvent);
                log.info("Member initialized event published");
            }
        });

        initializerEventReceiver.addEventListener(new CompleteTenantRequestEventListener() {
            @Override
            protected void onEvent(Event event) {
                // Send complete tenant event
                log.info("CompleteTenantRequestEvent received. Publishing complete tenant event...");
                CompleteTenantEvent completeTenantEvent = new CompleteTenantEvent(createTestTenantList());
                publishEvent(completeTenantEvent);
                log.info("Complete tenant event published");
            }
        });

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

        while (!instanceStarted || !instanceActivated || !startupTestCompleted || !topologyContextTestCompleted ||
                !thriftTestCompleted || !completeTenantInitialized) {
            // wait until the instance activated event is received.
            // this will assert whether instance got activated within timeout period; no need for explicit assertions
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
                log.info("Event list size: " + eventList.size());
                log.info("Recent events received: " + eventList);
                // if the list contains an event that means PCA was able to successfully publish health stats
                if (eventList.size() > 0) {
                    thriftTestCompleted = true;
                }
            }
        });
    }

    /**
     * Create test tenant list
     *
     * @return List of tenant objects with mock information
     */
    private List<Tenant> createTestTenantList() {
        List<Tenant> tenantList = new ArrayList<>();
        tenantList.add(new Tenant(1, "test.one.domain"));
        tenantList.add(new Tenant(2, "test.two.domain"));
        tenantList.add(new Tenant(3, "test.three.domain"));
        return tenantList;
    }
}
