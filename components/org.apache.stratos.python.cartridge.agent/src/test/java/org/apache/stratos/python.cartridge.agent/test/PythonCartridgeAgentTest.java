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
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static junit.framework.Assert.assertTrue;

public class PythonCartridgeAgentTest {

    private static final Log log = LogFactory.getLog(PythonCartridgeAgentTest.class);

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

    @BeforeClass
    public static void setUp(){
        // Set jndi.properties.dir system property for initializing event publishers and receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
    }

    private static String getResourcesFolderPath() {
        String path = PythonCartridgeAgentTest.class.getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }

    @Test(timeout = 300000)
    public void testPythonCartridgeAgent() {

        // Simulate CEP server socket
        startServerSocket(7711);

        String agentPath = setupPythonAgent();
        log.info("Starting python cartridge agent...");
        ByteArrayOutputStreamLocal outputStream = executeCommand("python " + agentPath + "/agent.py");

        List<String> outputLines = new ArrayList<String>();
        while (!outputStream.isClosed()) {
            List<String> newLines = getNewLines(outputLines, outputStream.toString());
            if(newLines.size() > 0) {
                for(String line : newLines) {
                    if(line.contains("Subscribed to 'topology/#'")) {
                        Topology topology = createTestTopology();
                        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);
                        String topicName = MessagingUtil.getMessageTopicName(completeTopologyEvent);
                        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
                        eventPublisher.publish(completeTopologyEvent);

                        // Simulate server socket
                        startServerSocket(9080);
                    }
                    if(line.contains("Artifact repository found")) {
                        ArtifactUpdatedEvent artifactUpdatedEvent = new ArtifactUpdatedEvent();
                        artifactUpdatedEvent.setClusterId(CLUSTER_ID);
                        artifactUpdatedEvent.setTenantId(TENANT_ID);
                        artifactUpdatedEvent.setRepoURL("https://github.com/imesh/stratos-php-applications.git");
                        String topicName = MessagingUtil.getMessageTopicName(artifactUpdatedEvent);
                        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topicName);
                        eventPublisher.publish(artifactUpdatedEvent);
                    }
                    if (line.contains("Exception in thread") || line.contains("ERROR")) {
                        break;
                    }

                    if(line.contains("Git clone operation for tenant u'" + TENANT_ID + "' successful")) {
                        assertTrue(true);
                        return;
                    }

                    log.info(line);
                }
            }
            sleep(500);
        }
    }

    private void startServerSocket(final int port) {
        Thread socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket cep = new ServerSocket(port);
                    cep.accept();
                } catch (IOException e) {
                    log.error("Could not start server socket", e);
                }
            }
        });
        socketThread.start();
    }

    /**
     * Create test topology
     * @return
     */
    private Topology createTestTopology() {
        Topology topology = new Topology();
        Service service = new Service("php", ServiceType.SingleTenant);
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
        member.setStatus(MemberStatus.Initialized);
        cluster.addMember(member);

        return topology;
    }

    /**
     * Return new lines found in the output
     * @param currentOutputLines current output lines
     * @param output output
     * @return
     */
    private List<String> getNewLines(List<String> currentOutputLines, String output) {
        List<String> newLines = new ArrayList<String>();

        if(StringUtils.isNotBlank(output)) {
            String [] lines = output.split(NEW_LINE);
            if(lines != null) {
                for(String line : lines) {
                    if(!currentOutputLines.contains(line)) {
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
     * @return
     */
    private String setupPythonAgent() {
        try {
            log.info("Setting up python cartridge agent...");
            String srcAgentPath = getResourcesFolderPath() + "/../../src/main/python/cartridge.agent/cartridge.agent";
            String destAgentPath = getResourcesFolderPath() + "/../" + UUID.randomUUID() + "/cartridge.agent";
            FileUtils.copyDirectory(new File(srcAgentPath), new File(destAgentPath));

            List<File> extensionFiles = (List<File>) FileUtils.listFiles(new File(srcAgentPath + "/extensions"), new String[] { "sh"}, false);
            for(File extensionFile : extensionFiles) {
                executeCommand("chmod +x " + extensionFile.getAbsolutePath());
            }

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
     * @param commandText
     */
    private ByteArrayOutputStreamLocal executeCommand(String commandText) {
        final ByteArrayOutputStreamLocal outputStream = new ByteArrayOutputStreamLocal();
        try {
            CommandLine commandline = CommandLine.parse(commandText);
            DefaultExecutor exec = new DefaultExecutor();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline, new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int i) {
                }

                @Override
                public void onProcessFailed(ExecuteException e) {
                    log.error("Process failed", e);
                }
            });
            return outputStream;
        } catch (Exception e) {
            log.error(outputStream.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Implements isClosed() method
     */
    private class ByteArrayOutputStreamLocal extends ByteArrayOutputStream  {
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
