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

package org.apache.stratos.integration.tests;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.integration.tests.rest.RestClient;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.*;

/**
 * Sample application tests.
 */
public class SampleApplicationsTest extends StratosTestServerManager {

    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);

    public static final int APPLICATION_ACTIVATION_TIMEOUT = 600000;
    private ApplicationsEventReceiver applicationsEventReceiver;
    private RestClient restClient = new RestClient();
    private String endpoint = "https://localhost:9443";
    private AutoscalingPolicyTest autoscalingPolicyTest;
    private NetworkPartitionTest networkPartitionTest;
    private CartridgeTest cartridgeTest;
    private DeploymentPolicyTest deploymentPolicyTest;

    @BeforeClass
    public void setUp() {
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
        autoscalingPolicyTest = new AutoscalingPolicyTest();
        networkPartitionTest = new NetworkPartitionTest();
        cartridgeTest = new CartridgeTest();
        deploymentPolicyTest = new DeploymentPolicyTest();
    }

    @Test
    public void testSingleCartridgeApplication() {
        try {
            initializeApplicationEventReceiver();
            //runApplicationTest("simple/single-cartridge-app", "single-cartridge-app");
        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred", false);
        }
    }

    /*@Test
    public void testAutoscalingPolicy() {
        try {
            initializeApplicationEventReceiver();
            boolean added = autoscalingPolicyTest.addAutoscalingPolicy("autoscaling-policy-c0.json",
                    endpoint, restClient);
            assertEquals(added, true);
            AutoscalePolicyBean bean = autoscalingPolicyTest.getAutoscalingPolicy("autoscaling-policy-c0", endpoint,
                    restClient);
            assertEquals(bean.getLoadThresholds().getRequestsInFlight().getThreshold(), 35.0, 0.0);
            assertEquals(bean.getLoadThresholds().getMemoryConsumption().getThreshold(), 45.0, 0.0);
            assertEquals(bean.getLoadThresholds().getLoadAverage().getThreshold(), 25.0, 0.0);

            boolean updated = autoscalingPolicyTest.updateAutoscalingPolicy("autoscaling-policy-c0.json",
                    endpoint, restClient);
            assertEquals(updated, true);
            AutoscalePolicyBean updatedBean = autoscalingPolicyTest.getAutoscalingPolicy("autoscaling-policy-c0", endpoint,
                    restClient);
            assertEquals(updatedBean.getLoadThresholds().getRequestsInFlight().getThreshold(), 30.0, 0.0);
            assertEquals(updatedBean.getLoadThresholds().getMemoryConsumption().getThreshold(), 40.0, 0.0);
            assertEquals(updatedBean.getLoadThresholds().getLoadAverage().getThreshold(), 20.0, 0.0);

            boolean removed = autoscalingPolicyTest.removeAutoscalingPolicy("autoscaling-policy-c0", endpoint,
                    restClient);
            assertEquals(removed, true);

            AutoscalePolicyBean beanRemoved = autoscalingPolicyTest.getAutoscalingPolicy("autoscaling-policy-c0", endpoint,
                    restClient);
            assertEquals(beanRemoved, null);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling autoscaling policy", false);
        }
    }

    @Test
    public void testNetworkPartition() {
        try {
            initializeApplicationEventReceiver();
            boolean added = networkPartitionTest.addNetworkPartition("network-partition-1.json",
                    endpoint, restClient);
            assertEquals(added, true);
            NetworkPartitionBean bean = networkPartitionTest.getNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(bean.getId(), "network-partition-1");
            assertEquals(bean.getPartitions().size(), 1);
            assertEquals(bean.getPartitions().get(0).getId(), "partition-1");
            assertEquals(bean.getPartitions().get(0).getProperty().get(0).getName(), "region");
            assertEquals(bean.getPartitions().get(0).getProperty().get(0).getValue(), "default");

            boolean updated = networkPartitionTest.updateNetworkPartition("network-partition-1.json",
                    endpoint, restClient);
            assertEquals(updated, true);
            NetworkPartitionBean updatedBean = networkPartitionTest.getNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(updatedBean.getId(), "network-partition-1");
            assertEquals(updatedBean.getPartitions().size(), 2);
            assertEquals(updatedBean.getPartitions().get(1).getId(), "partition-2");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(0).getName(), "region");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(0).getValue(), "default1");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(1).getName(), "zone");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(1).getValue(), "z1");

            boolean removed = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(removed, true);

            NetworkPartitionBean beanRemoved = networkPartitionTest.getNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(beanRemoved, null);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling network partitions", false);
        }
    }

    @Test
    public void testDeploymentPolicy() {
        try {
            initializeApplicationEventReceiver();
            boolean addedN1 = networkPartitionTest.addNetworkPartition("network-partition-1.json",
                    endpoint, restClient);
            assertEquals(addedN1, true);

            boolean addedN2 = networkPartitionTest.addNetworkPartition("network-partition-2.json",
                    endpoint, restClient);
            assertEquals(addedN2, true);

            boolean addedDep = deploymentPolicyTest.addDeploymentPolicy("deployment-policy-1.json",
                    endpoint, restClient);
            assertEquals(addedDep, true);

            DeploymentPolicyBean bean = deploymentPolicyTest.getDeploymentPolicy(
                    "deployment-policy-1", endpoint, restClient);
            assertEquals(bean.getId(), "deployment-policy-1");
            assertEquals(bean.getNetworkPartitions().size(), 2);
            assertEquals(bean.getNetworkPartitions().get(0).getId(), "network-partition-1");
            assertEquals(bean.getNetworkPartitions().get(0).getPartitionAlgo(), "one-after-another");
            assertEquals(bean.getNetworkPartitions().get(0).getPartitions().size(), 1);
            assertEquals(bean.getNetworkPartitions().get(0).getPartitions().get(0).getId(), "partition-1");
            assertEquals(bean.getNetworkPartitions().get(0).getPartitions().get(0).getPartitionMax(), 20);

            assertEquals(bean.getNetworkPartitions().get(1).getId(), "network-partition-2");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitionAlgo(), "round-robin");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().size(), 2);
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(0).getId(),
                    "network-partition-2-partition-1");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(0).getPartitionMax(), 10);
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(1).getId(),
                    "network-partition-2-partition-2");
            assertEquals(bean.getNetworkPartitions().get(1).getPartitions().get(1).getPartitionMax(), 9);

            //update network partition
            boolean updated = networkPartitionTest.updateNetworkPartition("network-partition-1.json",
                    endpoint, restClient);
            assertEquals(updated, true);

            //update deployment policy with new partition and max values
            boolean updatedDep = deploymentPolicyTest.updateDeploymentPolicy("deployment-policy-1.json",
                    endpoint, restClient);
            assertEquals(updatedDep, true);

            DeploymentPolicyBean updatedBean = deploymentPolicyTest.getDeploymentPolicy(
                    "deployment-policy-1", endpoint, restClient);
            assertEquals(updatedBean.getId(), "deployment-policy-1");
            assertEquals(updatedBean.getNetworkPartitions().size(), 2);
            assertEquals(updatedBean.getNetworkPartitions().get(0).getId(), "network-partition-1");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitionAlgo(), "one-after-another");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().size(), 2);
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(0).getId(), "partition-1");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(0).getPartitionMax(), 25);
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(1).getId(), "partition-2");
            assertEquals(updatedBean.getNetworkPartitions().get(0).getPartitions().get(1).getPartitionMax(), 20);

            assertEquals(updatedBean.getNetworkPartitions().get(1).getId(), "network-partition-2");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitionAlgo(), "round-robin");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().size(), 2);
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(0).getId(),
                    "network-partition-2-partition-1");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(0).getPartitionMax(), 15);
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(1).getId(),
                    "network-partition-2-partition-2");
            assertEquals(updatedBean.getNetworkPartitions().get(1).getPartitions().get(1).getPartitionMax(), 5);

            boolean removedNet = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = deploymentPolicyTest.removeDeploymentPolicy("deployment-policy-1", endpoint,
                    restClient);
            assertEquals(removedDep, true);

            DeploymentPolicyBean beanRemovedDep = deploymentPolicyTest.getDeploymentPolicy("network-partition-1", endpoint,
                    restClient);
            assertEquals(beanRemovedDep, null);

            boolean removedN1 = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(removedN1, true);

            NetworkPartitionBean beanRemovedN1 = networkPartitionTest.getNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(beanRemovedN1, null);

            boolean removedN2 = networkPartitionTest.removeNetworkPartition("network-partition-2", endpoint,
                    restClient);
            assertEquals(removedN2, true);

            NetworkPartitionBean beanRemovedN2 = networkPartitionTest.getNetworkPartition("network-partition-2", endpoint,
                    restClient);
            assertEquals(beanRemovedN2, null);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling autoscaling policy", false);
        }
    }

    @Test
    public void testCartridge() {
        try {
            initializeApplicationEventReceiver();
            boolean added = cartridgeTest.addCartridge("c0.json", endpoint, restClient);
            assertEquals(added, true);
            CartridgeBean bean = cartridgeTest.getCartridge("c0", endpoint, restClient);
            assertEquals(bean.getType(), "c0");
            assertEquals(bean.getCategory(), "Application");
            assertEquals(bean.getHost(), "qmog.cisco.com");
            for(PropertyBean property : bean.getProperty()) {
                if(property.getName().equals("payload_parameter.CEP_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com");
                } else if(property.getName().equals("payload_parameter.CEP_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com");
                } else if(property.getName().equals("payload_parameter.QTCM_NETWORK_COUNT")) {
                    assertEquals(property.getValue(), "1");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin");
                } else if(property.getName().equals("payload_parameter.QTCM_DNS_SEGMENT")) {
                    assertEquals(property.getValue(), "test");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_SECURE_PORT")) {
                    assertEquals(property.getValue(), "7711");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_PORT")) {
                    assertEquals(property.getValue(), "7611");
                } else if(property.getName().equals("payload_parameter.CEP_PORT")) {
                    assertEquals(property.getValue(), "7611");
                } else if(property.getName().equals("payload_parameter.MB_PORT")) {
                    assertEquals(property.getValue(), "61616");
                }
            }


            boolean updated = cartridgeTest.updateCartridge("c0.json",
                    endpoint, restClient);
            assertEquals(updated, true);
            CartridgeBean updatedBean = cartridgeTest.getCartridge("c0", endpoint,
                    restClient);
            assertEquals(updatedBean.getType(), "c0");
            assertEquals(updatedBean.getCategory(), "Data");
            assertEquals(updatedBean.getHost(), "qmog.cisco.com12");
            for(PropertyBean property : updatedBean.getProperty()) {
                if(property.getName().equals("payload_parameter.CEP_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com123");
                } else if(property.getName().equals("payload_parameter.CEP_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin123");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com123");
                } else if(property.getName().equals("payload_parameter.QTCM_NETWORK_COUNT")) {
                    assertEquals(property.getValue(), "3");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin123");
                } else if(property.getName().equals("payload_parameter.QTCM_DNS_SEGMENT")) {
                    assertEquals(property.getValue(), "test123");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_SECURE_PORT")) {
                    assertEquals(property.getValue(), "7712");
                } else if(property.getName().equals("payload_parameter.MONITORING_SERVER_PORT")) {
                    assertEquals(property.getValue(), "7612");
                } else if(property.getName().equals("payload_parameter.CEP_PORT")) {
                    assertEquals(property.getValue(), "7612");
                } else if(property.getName().equals("payload_parameter.MB_PORT")) {
                    assertEquals(property.getValue(), "61617");
                }
            }

            boolean removed = cartridgeTest.removeCartridge("c0", endpoint,
                    restClient);
            assertEquals(removed, true);

            CartridgeBean beanRemoved = cartridgeTest.getCartridge("c0", endpoint,
                    restClient);
            assertEquals(beanRemoved, null);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling cartridges", false);
        }
    }*/


    private void runApplicationTest(String applicationId) {
        runApplicationTest(applicationId, applicationId);
    }

    private void runApplicationTest(String applicationFolderName, String applicationId) {
        executeCommand(getApplicationsPath() + "/" + applicationFolderName + "/scripts/mock/deploy.sh");
        assertApplicationActivation(applicationId);
        executeCommand(getApplicationsPath() + "/" + applicationFolderName + "/scripts/mock/undeploy.sh");
        assertApplicationNotExists(applicationId);
    }

    /**
     * Initialize application event receiver
     */
    private void initializeApplicationEventReceiver() {
        if (applicationsEventReceiver == null) {
            applicationsEventReceiver = new ApplicationsEventReceiver();
            ExecutorService executorService = StratosThreadPool.getExecutorService("STRATOS_TEST_SERVER", 1);
            applicationsEventReceiver.setExecutorService(executorService);
            applicationsEventReceiver.execute();
        }
    }

    /**
     * Execute shell command
     *
     * @param commandText
     */
    private void executeCommand(String commandText) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            CommandLine commandline = CommandLine.parse(commandText);
            DefaultExecutor exec = new DefaultExecutor();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline);
            log.info(outputStream.toString());
        } catch (Exception e) {
            log.error(outputStream.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertApplicationActivation(String applicationName) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        while (!((application != null) && (application.getStatus() == ApplicationStatus.Active))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplication(applicationName);
            if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                break;
            }
        }

        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        assertEquals(String.format("Application status did not change to active: [application-id] %s", applicationName),
                ApplicationStatus.Active, application.getStatus());
    }

    private void assertApplicationNotExists(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNull(String.format("Application is found in the topology : [application-id] %s", applicationName), application);
    }

    /**
     * Get applications folder path
     *
     * @return
     */
    private String getApplicationsPath() {
        return getResourcesFolderPath() + "/../../../../../../samples/applications";
    }

    /**
     * Get resources folder path
     *
     * @return
     */
    private String getResourcesFolderPath() {
        String path = getClass().getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }

    private String getArtifactsPath() {
        return getResourcesFolderPath() + "/../../src/test/resources";
    }
}
