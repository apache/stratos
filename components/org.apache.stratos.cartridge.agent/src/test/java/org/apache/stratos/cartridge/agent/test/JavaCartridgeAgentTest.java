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

package org.apache.stratos.cartridge.agent.test;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.domain.LoadBalancingIPType;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertTrue;

/**
 * An integration test that verifies the functionality of the Java cartridge agent
 */
@RunWith(Parameterized.class)
public class JavaCartridgeAgentTest {

    private static final Log log = LogFactory.getLog(JavaCartridgeAgentTest.class);
    private static final long TIMEOUT = 200000;

    private static final String NEW_LINE = System.getProperty("line.separator");
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
    public static final String AGENT_NAME = "apache-stratos-cartridge-agent-4.1.0";
    private static HashMap<String, Executor> executorList;
    private static ArrayList<ServerSocket> serverSocketList;
    private final ArtifactUpdatedEvent artifactUpdatedEvent;
    private final Boolean expectedResult;
    private boolean instanceStarted;
    private boolean instanceActivated;
    private ByteArrayOutputStreamLocal outputStream;
    private TopologyEventReceiver topologyEventReceiver;
    private InstanceStatusEventReceiver instanceStatusEventReceiver;

    public JavaCartridgeAgentTest(ArtifactUpdatedEvent artifactUpdatedEvent, Boolean expectedResult) {
        this.artifactUpdatedEvent = artifactUpdatedEvent;
        this.expectedResult = expectedResult;
    }

    @BeforeClass
    public static void oneTimeSetUp() {
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
    }

    @Before
    public void setup() {
        serverSocketList = new ArrayList<ServerSocket>();
        executorList = new HashMap<String, Executor>();

        String agentHome = setupJavaAgent();

        ExecutorService executorService = StratosThreadPool.getExecutorService("TEST_THREAD_POOL", 5);
        topologyEventReceiver = new TopologyEventReceiver();
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();

        instanceStatusEventReceiver = new InstanceStatusEventReceiver();
        instanceStatusEventReceiver.setExecutorService(executorService);
        instanceStatusEventReceiver.execute();

        instanceStarted = false;
        instanceStatusEventReceiver.addEventListener(new InstanceStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Instance started event received");
                instanceStarted = true;
            }
        });


        instanceActivated = false;
        instanceStatusEventReceiver.addEventListener(new InstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Instance activated event received");
                instanceActivated = true;
            }
        });

        startServerSocket(7711);

        log.info("Starting Java cartridge agent...");
        String binPath = agentHome + "/bin";
        outputStream = executeCommand("bash stratos.sh", new File(binPath));

    }

    @After
    public void tearDown() {
        for (Map.Entry<String, Executor> entry : executorList.entrySet()) {
            try {
                String commandText = entry.getKey();
                Executor executor = entry.getValue();
                ExecuteWatchdog watchdog = executor.getWatchdog();
                if (watchdog != null) {
                    log.info("Terminating process: " + commandText);
                    watchdog.destroyProcess();
                }
//                File workingDirectory = executor.getWorkingDirectory();
//                if (workingDirectory != null) {
//                    log.info("Cleaning working directory: " + workingDirectory.getAbsolutePath());
//                    FileUtils.deleteDirectory(workingDirectory);
//                }
            } catch (Exception ignore) {
            }
        }
        for (ServerSocket serverSocket : serverSocketList) {
            try {
                log.info("Stopping socket server: " + serverSocket.getLocalSocketAddress());
                serverSocket.close();
            } catch (IOException e) {
                log.info("Couldn't stop socket server " + serverSocket.getLocalSocketAddress() + ", " + e.getMessage());
            }
        }

        try {
            log.info("Deleting source checkout folder...");
            FileUtils.deleteDirectory(new File("/tmp/test-jca-source"));
        } catch (Exception ignore) {
        }

        this.instanceStatusEventReceiver.terminate();
        this.topologyEventReceiver.terminate();

        this.instanceActivated = false;
        this.instanceStarted = false;
    }

    /**
     * This method returns a collection of {@link org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent}
     * objects as parameters to the test
     *
     * @return
     */
    @Parameterized.Parameters
    public static Collection getArtifactUpdatedEventsAsParams() {
        ArtifactUpdatedEvent publicRepoEvent = createTestArtifactUpdatedEvent();

        ArtifactUpdatedEvent privateRepoEvent = createTestArtifactUpdatedEvent();
        privateRepoEvent.setRepoURL("https://bitbucket.org/testapache2211/testrepo.git");
        privateRepoEvent.setRepoUserName("testapache2211");
        privateRepoEvent.setRepoPassword("RExPDGa4GkPJj4kJDzSROQ==");

        ArtifactUpdatedEvent privateRepoEvent2 = createTestArtifactUpdatedEvent();
        privateRepoEvent2.setRepoURL("https://testapache2211@bitbucket.org/testapache2211/testrepo.git");
        privateRepoEvent2.setRepoUserName("testapache2211");
        privateRepoEvent2.setRepoPassword("RExPDGa4GkPJj4kJDzSROQ==");

        return Arrays.asList(new Object[][]{
                {publicRepoEvent, true},
                {privateRepoEvent, true},
                {privateRepoEvent2, true}
        });

    }

    /**
     * Creates an {@link org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent} object with a public
     * repository URL
     *
     * @return
     */
    private static ArtifactUpdatedEvent createTestArtifactUpdatedEvent() {
        ArtifactUpdatedEvent publicRepoEvent = new ArtifactUpdatedEvent();
        publicRepoEvent.setClusterId(CLUSTER_ID);
        publicRepoEvent.setTenantId(TENANT_ID);
        publicRepoEvent.setRepoURL("https://bitbucket.org/testapache2211/opentestrepo1.git");
        return publicRepoEvent;
    }

    /**
     * Setup the JCA test path, copy test configurations
     *
     * @return
     */
    private String setupJavaAgent() {
        try {
            log.info("Setting up Java cartridge agent test setup");
            String jcaZipSource = getResourcesFolderPath() + "/../../../../products/cartridge-agent/modules/distribution/target/" + AGENT_NAME + ".zip";
            String testHome = getResourcesFolderPath() + "/../" + UUID.randomUUID() + "/";
            File agentHome = new File(testHome + AGENT_NAME);
            log.info("Extracting Java Cartridge Agent to test folder");
            ZipFile agentZip = new ZipFile(jcaZipSource);
            ProgressMonitor zipProgresMonitor = agentZip.getProgressMonitor();
            agentZip.extractAll(testHome);
            while (zipProgresMonitor.getPercentDone() < 100) {
                log.info("Extracting: " + zipProgresMonitor.getPercentDone());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            log.info("Copying agent jar");
            String agentJar = "org.apache.stratos.cartridge.agent-4.1.0.jar";
            String agentJarSource = getResourcesFolderPath() + "/../" + agentJar;
            String agentJarDest = agentHome.getCanonicalPath() + "/lib/" + agentJar;
            FileUtils.copyFile(new File(agentJarSource), new File(agentJarDest));

            log.info("Copying test payload file");
            String srcPayloadPath = getResourcesFolderPath() + "/../../src/test/resources/payload";
            String destPayloadPath = agentHome + "/payload";
            FileUtils.copyDirectory(new File(srcPayloadPath), new File(destPayloadPath));

            log.info("Copying test conf files");
            String srcConf = getResourcesFolderPath() + "/../../src/test/resources/conf";
            String destConf = agentHome + "/conf";
            FileUtils.copyDirectory(new File(srcConf), new File(destConf));

            log.info("Copying test stratos.sh script");
            String srcBin = getResourcesFolderPath() + "/../../src/test/resources/bin";
            String destBin = agentHome + "/bin";
            FileUtils.copyDirectory(new File(srcBin), new File(destBin));

            log.info("Changing stratos.sh permissions");
            new File(agentHome.getCanonicalPath() + "/bin/stratos.sh").setExecutable(true);
            log.info("Changed permissions for stratos.sh");

            log.info("Changing extension scripts permissions");
            File extensionsPath = new File(agentHome.getCanonicalPath() + "/extensions/");
            File[] extensions = extensionsPath.listFiles();
            for (File extension : extensions) {
                extension.setExecutable(true);
            }
            log.info("Changed permissions for extensions : " + outputStream);

            log.info("Java cartridge agent setup complete.");

            return agentHome.getCanonicalPath();
        } catch (IOException e) {
            String message = "Could not copy cartridge agent distribution";
            log.error(message, e);
            throw new RuntimeException(message, e);
        } catch (ZipException e) {
            String message = "Could not unzip cartridge agent distribution. Please make sure to build <STRATOS_HOME>/products/cartridge-agent first.";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Get current folder path
     *
     * @return
     */
    private static String getResourcesFolderPath() {
        return StringUtils.removeEnd(JavaCartridgeAgentTest.class.getResource("/").getPath(), File.separator);
    }

    @Test(timeout = TIMEOUT)
    public void testJavaCartridgeAgent() throws Exception {
        Thread communicatorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> outputLines = new ArrayList<String>();
                while (!outputStream.isClosed()) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Cartridge agent topology receiver thread started, waiting for event messages")) {
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
                                publishEvent(artifactUpdatedEvent);
                            }
                            if (line.contains("Exception in thread") || line.contains("ERROR")) {
                                //throw new RuntimeException(line);
                            }
                            log.info(line);
                        }
                    }

                    if (instanceActivated) {
                        break;
                    }
                    sleep(500);
                }
            }
        });

        communicatorThread.start();

        while (!instanceActivated) {
            sleep(2000);
        }

        assertTrue("Instance started event was not received", instanceStarted);
        assertTrue("Instance activated event was not received", instanceActivated == expectedResult);
    }

    /**
     * Publish messaging event
     *
     * @param event
     */
    private void publishEvent(Event event) {
        String topicName = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
        eventPublisher.publish(event);
    }

    /**
     * Start server socket
     *
     * @param port
     */
    private void startServerSocket(final int port) {
        Thread socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    serverSocketList.add(serverSocket);
                    serverSocket.accept();
                } catch (IOException e) {
                    String message = "Could not start server socket: [port] " + port;
                    log.error(message, e);
//                    throw new RuntimeException(message, e);
                }
            }
        });
        socketThread.start();
    }

    /**
     * Execute shell command
     *
     * @param commandText
     */
    private ByteArrayOutputStreamLocal executeCommand(final String commandText, File workingDir) {
        final ByteArrayOutputStreamLocal outputStream = new ByteArrayOutputStreamLocal();
        try {
            CommandLine commandline = CommandLine.parse(commandText);
            DefaultExecutor exec = new DefaultExecutor();
            if (workingDir != null) {
                exec.setWorkingDirectory(workingDir);
            }
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setStreamHandler(streamHandler);
            ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT);
            exec.setWatchdog(watchdog);
            log.info("Executing command: " + commandText + (workingDir == null ? "" : " at " + workingDir.getCanonicalPath()));
            exec.execute(commandline, new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int i) {
                    log.info(commandText + " process completed");
                }

                @Override
                public void onProcessFailed(ExecuteException e) {
                    log.error(commandText + " process failed", e);
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
