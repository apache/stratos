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

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.security.AuthenticationUser;
import org.apache.activemq.security.SimpleAuthenticationPlugin;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.instance.status.InstanceActivatedEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceStartedEventListener;
import org.apache.stratos.messaging.message.receiver.initializer.InitializerEventReceiver;
import org.apache.stratos.messaging.message.receiver.instance.status.InstanceStatusEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.apache.stratos.python.cartridge.agent.integration.common.ThriftTestServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class PythonAgentIntegrationTest {

    public static final String PATH_SEP = File.separator;
    public static final String NEW_LINE = System.getProperty("line.separator");

    public static final String ACTIVEMQ_AMQP_BIND_PORTS = "activemq.amqp.bind.ports";
    public static final String ACTIVEMQ_MQTT_BIND_PORTS = "activemq.mqtt.bind.ports";
    public static final String CEP_PORT = "cep.server.one.port";
    public static final String CEP_SSL_PORT = "cep.server.one.ssl.port";
    public static final String DISTRIBUTION_NAME = "distribution.name";

    private static final Log log = LogFactory.getLog(PythonAgentIntegrationTest.class);

    public static final String TEST_THREAD_POOL_SIZE = "test.thread.pool.size";
    protected final UUID PYTHON_AGENT_DIR_NAME = UUID.randomUUID();
    //    protected final String defaultBrokerName = "testBrokerDefault";
    protected final Properties integrationProperties = new Properties();

    protected Map<Integer, ServerSocket> serverSocketMap = new HashMap<>();
    protected Map<String, Executor> executorList = new HashMap<>();

    protected int cepPort;
    protected int cepSSLPort;
    protected String[] amqpBindPorts;
    protected String[] mqttBindPorts;
    protected String distributionName;
    protected int testThreadPoolSize;

    protected boolean eventReceiverInitialized = false;
    protected TopologyEventReceiver topologyEventReceiver;
    protected InstanceStatusEventReceiver instanceStatusEventReceiver;
    protected InitializerEventReceiver initializerEventReceiver;
    protected boolean instanceStarted;
    protected boolean instanceActivated;
    protected ByteArrayOutputStreamLocal outputStream;
    protected ThriftTestServer thriftTestServer;

    private Map<String, BrokerService> messageBrokers;

    /**
     * Setup method for test method testPythonCartridgeAgent
     */
    protected void setup(int timeout) throws Exception {
        messageBrokers = new HashMap<>();

        distributionName = integrationProperties.getProperty(DISTRIBUTION_NAME);

        cepPort = Integer.parseInt(integrationProperties.getProperty(CEP_PORT));
        cepSSLPort = Integer.parseInt(integrationProperties.getProperty(CEP_SSL_PORT));

        Properties jndiProperties = new Properties();
        jndiProperties.load(new FileInputStream(
                new File(System.getProperty("jndi.properties.dir") + PATH_SEP + "jndi.properties")));
        if (!jndiProperties.containsKey(ACTIVEMQ_AMQP_BIND_PORTS) || !jndiProperties
                .containsKey(ACTIVEMQ_MQTT_BIND_PORTS)) {
            amqpBindPorts = integrationProperties.getProperty(ACTIVEMQ_AMQP_BIND_PORTS).split(",");
            mqttBindPorts = integrationProperties.getProperty(ACTIVEMQ_MQTT_BIND_PORTS).split(",");
        } else {
            amqpBindPorts = jndiProperties.getProperty(ACTIVEMQ_AMQP_BIND_PORTS).split(",");
            mqttBindPorts = jndiProperties.getProperty(ACTIVEMQ_MQTT_BIND_PORTS).split(",");
        }

        if (amqpBindPorts.length != mqttBindPorts.length) {
            throw new RuntimeException(
                    "The number of AMQP ports and MQTT ports should be equal in integration-test.properties.");
        }

        // start ActiveMQ test server
        for (int i = 0; i < amqpBindPorts.length; i++) {
            log.info("Starting ActiveMQ instance with AMQP: " + amqpBindPorts[i] + ", MQTT: " + mqttBindPorts[i]);
            startActiveMQInstance(Integer.parseInt(amqpBindPorts[i]), Integer.parseInt(mqttBindPorts[i]), true);
        }

        ExecutorService executorService = StratosThreadPool.getExecutorService("TEST_THREAD_POOL", testThreadPoolSize);
        topologyEventReceiver = new TopologyEventReceiver();
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();

        instanceStatusEventReceiver = new InstanceStatusEventReceiver();
        instanceStatusEventReceiver.setExecutorService(executorService);
        instanceStatusEventReceiver.execute();

        instanceStatusEventReceiver.addEventListener(new InstanceStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Instance started event received");
                instanceStarted = true;
            }
        });

        instanceStatusEventReceiver.addEventListener(new InstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("Instance activated event received");
                instanceActivated = true;
            }
        });

        initializerEventReceiver = new InitializerEventReceiver();
        initializerEventReceiver.setExecutorService(executorService);
        initializerEventReceiver.execute();

        this.eventReceiverInitialized = true;

        // Start CEP Thrift test server
        thriftTestServer = new ThriftTestServer();

        File file = new File(getResourcesPath() + PATH_SEP + "common" + PATH_SEP + "stratos-health-stream-def.json");
        FileInputStream fis = new FileInputStream(file);
        String str = IOUtils.toString(fis, "UTF-8");

        if (str.equals("")) {
            log.warn("Stream definition of health stat stream is empty. Thrift server will not function properly");
        }
        thriftTestServer.addStreamDefinition(str, -1234);
        // start with non-ssl port; test server will automatically bind to ssl port
        thriftTestServer.start(cepPort);
        log.info("Started Thrift server with stream definition: " + str);

        String agentPath = setupPythonAgent();
        log.info("Python agent working directory name: " + PYTHON_AGENT_DIR_NAME);
        log.info("Starting python cartridge agent...");
        this.outputStream = executeCommand("python " + agentPath + PATH_SEP + "agent.py", timeout);
    }

    protected void tearDown() {
        tearDown(null);
    }

    /**
     * TearDown method for test method testPythonCartridgeAgent
     */
    protected void tearDown(String sourcePath) {
        for (Map.Entry<String, Executor> entry : executorList.entrySet()) {
            try {
                String commandText = entry.getKey();
                Executor executor = entry.getValue();
                log.info("Terminating process: " + commandText);
                executor.setExitValue(0);
                executor.getWatchdog().destroyProcess();
            } catch (Exception ignore) {
            }
        }
        // wait until everything cleans up to avoid connection errors
        sleep(1000);
        for (ServerSocket serverSocket : serverSocketMap.values()) {
            try {
                log.info("Stopping socket server: " + serverSocket.getLocalSocketAddress());
                serverSocket.close();
            } catch (IOException ignore) {
            }
        }
        try {
            if (thriftTestServer != null) {
                thriftTestServer.stop();
            }
        } catch (Exception ignore) {
        }

        if (sourcePath != null) {
            try {
                log.info("Deleting source checkout folder...");
                FileUtils.deleteDirectory(new File(sourcePath));
            } catch (Exception ignore) {
            }
        }
        log.info("Terminating event receivers...");
        this.instanceStatusEventReceiver.terminate();
        this.topologyEventReceiver.terminate();
        this.initializerEventReceiver.terminate();

        this.instanceStatusEventReceiver = null;
        this.topologyEventReceiver = null;
        this.initializerEventReceiver = null;

        this.instanceActivated = false;
        this.instanceStarted = false;

        // stop the broker services
        for (Map.Entry<String, BrokerService> entry : this.messageBrokers.entrySet()) {
            try {
                log.debug("Stopping broker service [" + entry.getKey() + "]");
                entry.getValue().stop();
            } catch (Exception ignore) {
            }
        }

        this.messageBrokers = null;

        // TODO: use thread synchronization and assert all connections are properly closed
        // leave some room to clear up active connections
        sleep(1000);
    }

    public PythonAgentIntegrationTest() throws IOException {
        integrationProperties
                .load(PythonAgentIntegrationTest.class.getResourceAsStream(PATH_SEP + "integration-test.properties"));
        distributionName = integrationProperties.getProperty(DISTRIBUTION_NAME);
        cepPort = Integer.parseInt(integrationProperties.getProperty(CEP_PORT));
        cepSSLPort = Integer.parseInt(integrationProperties.getProperty(CEP_SSL_PORT));
        testThreadPoolSize = Integer.parseInt(integrationProperties.getProperty(TEST_THREAD_POOL_SIZE));
        log.info("PCA integration properties: " + integrationProperties.toString());
    }

    protected String startActiveMQInstance(int amqpPort, int mqttPort, boolean secured) throws Exception {

        try {
            ServerSocket serverSocket = new ServerSocket(amqpPort);
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("AMQP port " + amqpPort + " is already in use.", e);
        }

        try {
            ServerSocket serverSocket = new ServerSocket(mqttPort);
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("MQTT port " + mqttPort + " is already in use.", e);
        }

        System.setProperty("mb.username", "system");
        System.setProperty("mb.password", "manager");

        String brokerName = "testBroker-" + amqpPort + "-" + mqttPort;

        log.info("Starting an ActiveMQ instance");
        BrokerService broker = new BrokerService();
        broker.addConnector("tcp://localhost:" + (amqpPort));
        broker.addConnector("mqtt://localhost:" + (mqttPort));

        if (secured) {
            AuthenticationUser authenticationUser = new AuthenticationUser("system", "manager", "users,admins");
            List<AuthenticationUser> authUserList = new ArrayList<>();
            authUserList.add(authenticationUser);
            broker.setPlugins(new BrokerPlugin[] { new SimpleAuthenticationPlugin(authUserList) });
        }

        broker.setBrokerName(brokerName);
        broker.setDataDirectory(
                PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".." + PATH_SEP +
                        PYTHON_AGENT_DIR_NAME + PATH_SEP + "activemq-data-" + brokerName);
        broker.start();
        this.messageBrokers.put(brokerName, broker);
        log.info("ActiveMQ Broker service [" + brokerName + "] started! [AMQP] " + amqpPort + " [MQTT] " + mqttPort);

        return brokerName;
    }

    protected void stopActiveMQInstance(String brokerName) {
        if (this.messageBrokers.containsKey(brokerName)) {
            log.debug("Stopping broker service [" + brokerName + "]");
            BrokerService broker = this.messageBrokers.get(brokerName);
            try {
                broker.stop();
            } catch (Exception ignore) {
            }
        }
    }

    protected void startCommunicatorThread() {
        Thread communicatorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> outputLines = new ArrayList<>();
                while (!outputStream.isClosed()) {
                    List<String> newLines = getNewLines(outputLines, outputStream.toString());
                    if (newLines.size() > 0) {
                        for (String line : newLines) {
                            if (line.contains("Exception in thread") || line.contains("ERROR")) {
                                try {
                                    throw new RuntimeException(line);
                                } catch (Exception e) {
                                    log.error("ERROR found in PCA log", e);
                                }
                            }
                            log.debug("[" + getClassName() + "] [PCA] " + line);
                        }
                    }
                    sleep(100);
                }
            }
        });
        communicatorThread.start();
    }

    /**
     * Return concrete class name
     * @return
     */
    protected abstract String getClassName();

    /**
     * Start server socket
     *
     * @param port Port number of server socket to be started
     */
    protected void startServerSocket(final int port) {
        Thread socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    serverSocketMap.put(port, serverSocket);
                    log.info("Server socket started on port: " + port);
                    Socket socket = serverSocket.accept();
                    log.info("Client connected to [port] " + port);

                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int read;
                    while (!socket.isClosed()) {
                        if ((read = is.read(buffer)) != -1) {
                            String output = new String(buffer, 0, read);
                            log.info("Message received for [port] " + port + ", [message] " + output);
                        }
                    }
                } catch (IOException e) {
                    String message = "Could not start server socket: [port] " + port;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
        });
        socketThread.start();
    }

    public static String getCommonResourcesPath() {
        return PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".." + PATH_SEP +
                ".." + PATH_SEP + "src" + PATH_SEP + "test" + PATH_SEP + "resources" + PATH_SEP + "common";
    }

    public static String getResourcesPath() {
        return PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".." + PATH_SEP +
                ".." + PATH_SEP + "src" + PATH_SEP + "test" + PATH_SEP + "resources";
    }

    protected String getTestCaseResourcesPath() {
        return PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + ".." + PATH_SEP + ".." + PATH_SEP +
                "src" + PATH_SEP + "test" + PATH_SEP + "resources" + PATH_SEP + this.getClass().getSimpleName();
    }

    /**
     * Copy python agent distribution to a new folder, extract it and copy sample configuration files
     *
     * @return Python cartridge agent home directory
     */
    protected String setupPythonAgent() {
        try {
            log.info("Setting up python cartridge agent...");

            String srcAgentPath = PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() +
                    PATH_SEP + ".." + PATH_SEP + ".." + PATH_SEP + ".." + PATH_SEP + ".." + PATH_SEP + "distribution" +
                    PATH_SEP + "target" + PATH_SEP + distributionName + ".zip";
            String unzipDestPath = PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".."
                    + PATH_SEP +
                    PYTHON_AGENT_DIR_NAME + PATH_SEP;
            //FileUtils.copyFile(new File(srcAgentPath), new File(destAgentPath));
            unzip(srcAgentPath, unzipDestPath);
            String destAgentPath = PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".." +
                    PATH_SEP + PYTHON_AGENT_DIR_NAME + PATH_SEP + distributionName;

            String srcAgentConfPath = getTestCaseResourcesPath() + PATH_SEP + "agent.conf";
            String destAgentConfPath = destAgentPath + PATH_SEP + "agent.conf";
            FileUtils.copyFile(new File(srcAgentConfPath), new File(destAgentConfPath));

            String srcLoggingIniPath = getTestCaseResourcesPath() + PATH_SEP + "logging.ini";
            String destLoggingIniPath = destAgentPath + PATH_SEP + "logging.ini";
            FileUtils.copyFile(new File(srcLoggingIniPath), new File(destLoggingIniPath));

            String srcPayloadPath = getTestCaseResourcesPath() + PATH_SEP + "payload";
            String destPayloadPath = destAgentPath + PATH_SEP + "payload";
            FileUtils.copyDirectory(new File(srcPayloadPath), new File(destPayloadPath));

            // copy extensions directory if it exists
            String srcExtensionPath = getTestCaseResourcesPath() + PATH_SEP + "extensions" + PATH_SEP + "bash";
            File extensionsDirFile = new File(srcExtensionPath);
            if (extensionsDirFile.exists()) {
                FileUtils.copyDirectory(extensionsDirFile,
                        new File(destAgentPath + PATH_SEP + "extensions" + PATH_SEP + "bash"));
            }

            // copy plugins directory if it exists
            String srcPluginPath = getTestCaseResourcesPath() + PATH_SEP + "extensions" + PATH_SEP + "py";
            File pluginsDirFile = new File(srcPluginPath);
            if (pluginsDirFile.exists()) {
                FileUtils.copyDirectory(pluginsDirFile, new File(destAgentPath + PATH_SEP + "plugins"));
            }

            File extensionsPath = new File(destAgentPath + PATH_SEP + "extensions" + PATH_SEP + "bash");
            File[] extensions = extensionsPath.listFiles();
            log.info("Changing extension scripts permissions in: " + extensionsPath.getAbsolutePath());
            assert extensions != null;
            for (File extension : extensions) {
                extension.setExecutable(true);
            }

            log.info("Python cartridge agent setup completed");

            return destAgentPath;
        } catch (Exception e) {
            String message = "Could not copy cartridge agent distribution";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    /**
     * Execute shell command
     *
     * @param commandText Command string to be executed
     */
    protected ByteArrayOutputStreamLocal executeCommand(final String commandText, int timeout) {
        final ByteArrayOutputStreamLocal outputStream = new ByteArrayOutputStreamLocal();
        try {
            CommandLine commandline = CommandLine.parse(commandText);
            DefaultExecutor exec = new DefaultExecutor();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setWorkingDirectory(new File(
                    PythonAgentIntegrationTest.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".." + PATH_SEP +
                            PYTHON_AGENT_DIR_NAME));
            exec.setStreamHandler(streamHandler);
            ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
            exec.setWatchdog(watchdog);
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
     * Sleep current thread
     *
     * @param time Time to sleep in milli-seconds
     */
    protected void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Return new lines found in the output
     *
     * @param currentOutputLines current output lines
     * @param output             output
     * @return new lines printed by Python agent process
     */
    protected List<String> getNewLines(List<String> currentOutputLines, String output) {
        List<String> newLines = new ArrayList<>();

        if (StringUtils.isNotBlank(output)) {
            List<String> lines = Arrays.asList(output.split(NEW_LINE));
            if (lines.size() > 0) {
                int readStartIndex = (currentOutputLines.size() > 0) ? (currentOutputLines.size() - 1) : 0;
                for (String line : lines.subList(readStartIndex , lines.size())) {
                    currentOutputLines.add(line);
                    newLines.add(line);
                }
            }
        }
        return newLines;
    }

    /**
     * Publish messaging event
     *
     * @param event Event object to be published to message broker
     */
    protected void publishEvent(Event event) {
        String topicName = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
        eventPublisher.publish(event);
    }

    /**
     * Implements ByteArrayOutputStream.isClosed() method
     */
    protected class ByteArrayOutputStreamLocal extends org.apache.commons.io.output.ByteArrayOutputStream {
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

    /**
     * Create a test topology object
     *
     * @param serviceName
     * @param clusterId
     * @param depPolicyName
     * @param autoscalingPolicyName
     * @param appId
     * @param memberId
     * @param clusterInstanceId
     * @param networkPartitionId
     * @param partitionId
     * @param serviceType
     * @return
     */
    protected static Topology createTestTopology(
            String serviceName,
            String clusterId,
            String depPolicyName,
            String autoscalingPolicyName,
            String appId,
            String memberId,
            String clusterInstanceId,
            String networkPartitionId,
            String partitionId,
            ServiceType serviceType) {


        Topology topology = new Topology();
        Service service = new Service(serviceName, serviceType);
        topology.addService(service);

        Cluster cluster = new Cluster(service.getServiceName(), clusterId, depPolicyName, autoscalingPolicyName, appId);
        service.addCluster(cluster);

        Member member = new Member(
                service.getServiceName(),
                cluster.getClusterId(),
                memberId,
                clusterInstanceId,
                networkPartitionId,
                partitionId,
                LoadBalancingIPType.Private,
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
