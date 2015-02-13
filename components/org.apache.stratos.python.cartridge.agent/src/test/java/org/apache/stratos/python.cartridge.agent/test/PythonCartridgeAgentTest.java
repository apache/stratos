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

import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.apache.stratos.messaging.listener.instance.status.InstanceActivatedEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceStartedEventListener;
import org.apache.stratos.messaging.message.receiver.instance.status.InstanceStatusEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertTrue;

public class PythonCartridgeAgentTest {

    private static final Log log = LogFactory.getLog(PythonCartridgeAgentTest.class);

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final long TIMEOUT = 200000;
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

    private static List<ServerSocket> serverSocketList;
    private static Map<String, Executor> executorList;

    @BeforeClass
    public static void setUp() {
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
        serverSocketList = new ArrayList<ServerSocket>();
        executorList = new HashMap<String, Executor>();
    }

    @AfterClass
    public static void tearDown() {
        for (Map.Entry<String, Executor> entry : executorList.entrySet()) {
            try {
                String commandText = entry.getKey();
                Executor executor = entry.getValue();
                ExecuteWatchdog watchdog = executor.getWatchdog();
                if (watchdog != null) {
                    log.info("Terminating process: " + commandText);
                    watchdog.destroyProcess();
                }
                File workingDirectory = executor.getWorkingDirectory();
                if (workingDirectory != null) {
                    log.info("Cleaning working directory: " + workingDirectory.getAbsolutePath());
                    FileUtils.deleteDirectory(workingDirectory);
                }
            } catch (Exception ignore) {
            }
        }
        for (ServerSocket serverSocket : serverSocketList) {
            try {
                log.info("Stopping socket server: " + serverSocket.getLocalSocketAddress());
                serverSocket.close();
            } catch (IOException ignore) {
            }
        }
    }

    @Test(timeout = TIMEOUT)
    public void testPythonCartridgeAgent() {

        ExecutorService executorService = StratosThreadPool.getExecutorService("TEST_THREAD_POOL", 5);
        TopologyEventReceiver topologyEventReceiver = new TopologyEventReceiver();
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();

        InstanceStatusEventReceiver instanceStatusEventReceiver = new InstanceStatusEventReceiver();
        instanceStatusEventReceiver.setExecutorService(executorService);
        instanceStatusEventReceiver.execute();

        final boolean[] instanceStarted = new boolean[1];
        instanceStatusEventReceiver.addEventListener(new InstanceStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Instance started event received");
                instanceStarted[0] = true;
            }
        });


        final boolean[] instanceActivated = new boolean[1];
        instanceStatusEventReceiver.addEventListener(new InstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Instance activated event received");
                instanceActivated[0] = true;
            }
        });

        // Simulate CEP server socket
        startServerSocket(7711);

        String agentPath = setupPythonAgent();
        log.info("Starting python cartridge agent...");
        ByteArrayOutputStreamLocal outputStream = executeCommand("python " + agentPath + "/agent.py");

        List<String> outputLines = new ArrayList<String>();
        while (!outputStream.isClosed()) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if (newLines.size() > 0) {
                for (String line : newLines) {
                    if (line.contains("Subscribed to 'topology/#'")) {
                        sleep(2000);
                        // Send complete topology event
                        log.info("Publishing complete topology event...");
                        Topology topology = createTestTopology();
                        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                        publishEvent(completeTopologyEvent);
                        log.info("Complete topology event published");

                        sleep(5000);
                        // Publish member initialized event
                        log.info("Publishing member initialized event...");
                        MemberInitializedEvent memberInitializedEvent = new MemberInitializedEvent(
                                SERVICE_NAME, CLUSTER_ID, CLUSTER_INSTANCE_ID, MEMBER_ID, NETWORK_PARTITION_ID, PARTITION_ID
                        );
                        publishEvent(memberInitializedEvent);
                        log.info("Member initialized event published");

                        // Simulate server socket
                        startServerSocket(9080);
                    }
                    if (line.contains("Artifact repository found")) {
                        // Send artifact updated event
                        ArtifactUpdatedEvent artifactUpdatedEvent = new ArtifactUpdatedEvent();
                        artifactUpdatedEvent.setClusterId(CLUSTER_ID);
                        artifactUpdatedEvent.setTenantId(TENANT_ID);
                        artifactUpdatedEvent.setRepoURL("https://github.com/imesh/stratos-php-applications.git");
                        String topicName = MessagingUtil.getMessageTopicName(artifactUpdatedEvent);
                        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
                        eventPublisher.publish(artifactUpdatedEvent);
                    }
                    if (line.contains("Exception in thread") || line.contains("ERROR")) {
                        //throw new RuntimeException(line);
                    }
                    log.info(line);
                }
            }

            if (instanceActivated[0]) {
                break;
            }
            sleep(500);
        }

        assertTrue("Instance started event was not received", instanceStarted[0]);
        assertTrue("Instance activated event was not received", instanceActivated[0]);
    }

    /**
     * Publish messaging event
     * @param event
     */
    private void publishEvent(Event event) {
        String topicName = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
        eventPublisher.publish(event);
    }

    /**
     * Start server socket
     * @param port
     */
    private void startServerSocket(final int port) {
        Thread socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    serverSocket.accept();
                    serverSocketList.add(serverSocket);
                } catch (IOException e) {
                    String message = "Could not start server socket: [port] " + port;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
        });
        socketThread.start();
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
                CLUSTER_INSTANCE_ID, NETWORK_PARTITION_ID, PARTITION_ID, System.currentTimeMillis());
        member.setDefaultPrivateIP("10.0.0.1");
        member.setDefaultPublicIP("20.0.0.1");
        Properties properties = new Properties();
        properties.setProperty("prop1", "value1");
        member.setProperties(properties);
        member.setStatus(MemberStatus.Created);
        cluster.addMember(member);

        return topology;
    }

    /**
     * Return new lines found in the output
     *
     * @param currentOutputLines current output lines
     * @param output             output
     * @return
     */
    private List<String> getNewLines(List<String> currentOutputLines, String output) {
        List<String> newLines = new ArrayList<String>();

        if (StringUtils.isNotBlank(output)) {
            String[] lines = output.split(NEW_LINE);
            if (lines != null) {
                for (String line : lines) {
                    if (!currentOutputLines.contains(line)) {
                        currentOutputLines.add(line);
                        newLines.add(line);
                    }
                }
            }
        }
        return newLines;
    }

    /**
     * Sleep current thread
     *
     * @param time
     */
    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Copy python agent distribution to a new folder, extract it and copy sample configuration files
     *
     * @return
     */
    private String setupPythonAgent() {
        try {
            log.info("Setting up python cartridge agent...");
            String srcAgentPath = getResourcesFolderPath() + "/../../src/main/python/cartridge.agent/cartridge.agent";
            String destAgentPath = getResourcesFolderPath() + "/../" + UUID.randomUUID() + "/cartridge.agent";
            FileUtils.copyDirectory(new File(srcAgentPath), new File(destAgentPath));

            String srcAgentConfPath = getResourcesFolderPath() + "/agent.conf";
            String destAgentConfPath = destAgentPath + "/agent.conf";
            FileUtils.copyFile(new File(srcAgentConfPath), new File(destAgentConfPath));

            String srcLoggingIniPath = getResourcesFolderPath() + "/logging.ini";
            String destLoggingIniPath = destAgentPath + "/logging.ini";
            FileUtils.copyFile(new File(srcLoggingIniPath), new File(destLoggingIniPath));

            String srcPayloadPath = getResourcesFolderPath() + "/payload";
            String destPayloadPath = destAgentPath + "/payload";
            FileUtils.copyDirectory(new File(srcPayloadPath), new File(destPayloadPath));
            log.info("Python cartridge agent setup completed");

            return destAgentPath;
        } catch (Exception e) {
            String message = "Could not copy cartridge agent distribution";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Execute shell command
     *
     * @param commandText
     */
    private ByteArrayOutputStreamLocal executeCommand(String commandText) {
        final ByteArrayOutputStreamLocal outputStream = new ByteArrayOutputStreamLocal();
        try {
            CommandLine commandline = CommandLine.parse(commandText);
            DefaultExecutor exec = new DefaultExecutor();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setStreamHandler(streamHandler);
            ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT);
            exec.setWatchdog(watchdog);
            exec.execute(commandline, new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int i) {
                    log.info("Agent process completed");
                }

                @Override
                public void onProcessFailed(ExecuteException e) {
                    log.error("Agent process failed", e);
                }
            });
            executorList.put(commandText, exec);
            return outputStream;
        } catch (Exception e) {
            log.error(outputStream.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get resources folder path
     * @return
     */
    private static String getResourcesFolderPath() {
        String path = PythonCartridgeAgentTest.class.getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }

    /**
     * Implements ByteArrayOutputStream.isClosed() method
     */
    private class ByteArrayOutputStreamLocal extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
