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
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.integration.tests.rest.RestClient;
import org.apache.stratos.messaging.domain.application.*;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.*;

/**
 * Sample application tests.
 */
public class SampleApplicationsTest extends StratosTestServerManager {

    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);

    public static final int APPLICATION_ACTIVATION_TIMEOUT = 120000;
    public static final String APPLICATION_STATUS_CREATED = "Created";
    public static final String APPLICATION_STATUS_UNDEPLOYING = "Undeploying";
    private String endpoint = "https://localhost:9443";

    private ApplicationsEventReceiver applicationsEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    private RestClient restClient = new RestClient();
    private AutoscalingPolicyTest autoscalingPolicyTest;
    private NetworkPartitionTest networkPartitionTest;
    private CartridgeTest cartridgeTest;
    private DeploymentPolicyTest deploymentPolicyTest;
    private CartridgeGroupTest cartridgeGroupTest;
    private ApplicationTest applicationTest;
    private ApplicationPolicyTest applicationPolicyTest;


    @BeforeClass
    public void setUp() {
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
        System.setProperty("autoscaler.service.url", "https://localhost:9443/services/AutoscalerService");
        autoscalingPolicyTest = new AutoscalingPolicyTest();
        networkPartitionTest = new NetworkPartitionTest();
        cartridgeTest = new CartridgeTest();
        deploymentPolicyTest = new DeploymentPolicyTest();
        cartridgeGroupTest = new CartridgeGroupTest();
        applicationTest = new ApplicationTest();
        applicationPolicyTest = new ApplicationPolicyTest();
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

    @Test
    public void testAutoscalingPolicy() {
        try {
            boolean added = autoscalingPolicyTest.addAutoscalingPolicy("autoscaling-policy-c0.json",
                    endpoint, restClient);
            assertEquals(added, true);
            AutoscalePolicyBean bean = autoscalingPolicyTest.getAutoscalingPolicy("autoscaling-policy-c0", endpoint,
                    restClient);
            assertEquals(bean.getId(), "autoscaling-policy-c0");
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
    public void testCartridgeGroup() {
        try {
            boolean addedC1 = cartridgeTest.addCartridge("c1.json", endpoint, restClient);
            assertEquals(addedC1, true);

            boolean addedC2 = cartridgeTest.addCartridge("c2.json", endpoint, restClient);
            assertEquals(addedC2, true);

            boolean addedC3 = cartridgeTest.addCartridge("c3.json", endpoint, restClient);
            assertEquals(addedC3, true);

            boolean added = cartridgeGroupTest.addCartridgeGroup("cartrdige-nested.json",
                    endpoint, restClient);
            assertEquals(added, true);
            CartridgeGroupBean bean = cartridgeGroupTest.getCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(bean.getName(), "G1");

            boolean updated = cartridgeGroupTest.updateCartridgeGroup("cartrdige-nested.json",
                    endpoint, restClient);
            assertEquals(updated, true);
            CartridgeGroupBean updatedBean = cartridgeGroupTest.getCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(updatedBean.getName(), "G1");

            boolean removedC1 = cartridgeTest.removeCartridge("c1", endpoint,
                    restClient);
            assertEquals(removedC1, false);

            boolean removedC2 = cartridgeTest.removeCartridge("c2", endpoint,
                    restClient);
            assertEquals(removedC2, false);

            boolean removedC3 = cartridgeTest.removeCartridge("c3", endpoint,
                    restClient);
            assertEquals(removedC3, false);

            boolean removed = cartridgeGroupTest.removeCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(removed, true);

            CartridgeGroupBean beanRemoved = cartridgeGroupTest.getCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(beanRemoved, null);

            removedC1 = cartridgeTest.removeCartridge("c1", endpoint,
                    restClient);
            assertEquals(removedC1, true);

            removedC2 = cartridgeTest.removeCartridge("c2", endpoint,
                    restClient);
            assertEquals(removedC2, true);

            removedC3 = cartridgeTest.removeCartridge("c3", endpoint,
                    restClient);
            assertEquals(removedC3, true);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling autoscaling policy", false);
        }
    }

    @Test
    public void testApplication() {
        try {
            boolean addedScalingPolicy = autoscalingPolicyTest.addAutoscalingPolicy("autoscaling-policy-1.json",
                    endpoint, restClient);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = cartridgeTest.addCartridge("c1.json", endpoint, restClient);
            assertEquals(addedC1, true);

            boolean addedC2 = cartridgeTest.addCartridge("c2.json", endpoint, restClient);
            assertEquals(addedC2, true);

            boolean addedC3 = cartridgeTest.addCartridge("c3.json", endpoint, restClient);
            assertEquals(addedC3, true);

            boolean addedG1 = cartridgeGroupTest.addCartridgeGroup("cartrdige-nested.json",
                    endpoint, restClient);
            assertEquals(addedG1, true);
            CartridgeGroupBean beanG1 = cartridgeGroupTest.getCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(beanG1.getName(), "G1");

            boolean addedN1 = networkPartitionTest.addNetworkPartition("network-partition-1.json",
                    endpoint, restClient);
            assertEquals(addedN1, true);

            boolean addedN2 = networkPartitionTest.addNetworkPartition("network-partition-2.json",
                    endpoint, restClient);
            assertEquals(addedN2, true);

            boolean addedDep = deploymentPolicyTest.addDeploymentPolicy("deployment-policy-1.json",
                    endpoint, restClient);
            assertEquals(addedDep, true);

            boolean added = applicationTest.addApplication("g-sc-G123-1.json",
                    endpoint, restClient);
            assertEquals(added, true);
            ApplicationBean bean = applicationTest.getApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(bean.getApplicationId(), "g-sc-G123-1");

            assertEquals(bean.getComponents().getGroups().get(0).getName(), "G1");
            assertEquals(bean.getComponents().getGroups().get(0).getAlias(), "group1");
            assertEquals(bean.getComponents().getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getType(), "c1");
            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getAlias(), "group2");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getName(), "G2");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c2");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getAlias(), "group3");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getName(), "G3");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 2);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c3");
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 1);
            assertEquals(bean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 2);

            boolean updated = applicationTest.updateApplication("g-sc-G123-1.json",
                    endpoint, restClient);
            assertEquals(updated, true);

            ApplicationBean updatedBean = applicationTest.getApplication("g-sc-G123-1", endpoint,
                    restClient);

            assertEquals(bean.getApplicationId(), "g-sc-G123-1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getName(), "G1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getAlias(), "group1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getType(), "c1");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 3);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getAlias(), "group2");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getName(), "G2");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 1);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 1);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c2");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 4);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getAlias(), "group3");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getName(), "G3");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMaxInstances(), 3);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getGroupMinInstances(), 2);

            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getType(), "c3");
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMin(), 2);
            assertEquals(updatedBean.getComponents().getGroups().get(0).getGroups().get(0).getGroups().get(0).getCartridges().get(0).getCartridgeMax(), 3);


            boolean removedGroup = cartridgeGroupTest.removeCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(removedGroup, false);

            boolean removedAuto = autoscalingPolicyTest.removeAutoscalingPolicy("autoscaling-policy-1", endpoint,
                    restClient);
            assertEquals(removedAuto, false);

            boolean removedNet = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = deploymentPolicyTest.removeDeploymentPolicy("deployment-policy-1", endpoint,
                    restClient);
            assertEquals(removedDep, false);

            boolean removed = applicationTest.removeApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = applicationTest.getApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(beanRemoved, null);

            removedGroup = cartridgeGroupTest.removeCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(removedGroup, true);

            boolean removedC1 = cartridgeTest.removeCartridge("c1", endpoint,
                    restClient);
            assertEquals(removedC1, true);

            boolean removedC2 = cartridgeTest.removeCartridge("c2", endpoint,
                    restClient);
            assertEquals(removedC2, true);

            boolean removedC3 = cartridgeTest.removeCartridge("c3", endpoint,
                    restClient);
            assertEquals(removedC3, true);

            removedAuto = autoscalingPolicyTest.removeAutoscalingPolicy("autoscaling-policy-1", endpoint,
                    restClient);
            assertEquals(removedAuto, true);

            removedDep = deploymentPolicyTest.removeDeploymentPolicy("deployment-policy-1", endpoint,
                    restClient);
            assertEquals(removedDep, true);

            removedNet = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(removedNet, true);

            boolean removedN2 = networkPartitionTest.removeNetworkPartition("network-partition-2", endpoint,
                    restClient);
            assertEquals(removedN2, true);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling application", false);
        }
    }

    @Test
    public void testDeployApplication() {
        try {
            //Initializing event Receivers
            initializeApplicationEventReceiver();
            initializeTopologyEventReceiver();

            boolean addedScalingPolicy = autoscalingPolicyTest.addAutoscalingPolicy("autoscaling-policy-1.json",
                    endpoint, restClient);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = cartridgeTest.addCartridge("c1.json", endpoint, restClient);
            assertEquals(addedC1, true);

            boolean addedC2 = cartridgeTest.addCartridge("c2.json", endpoint, restClient);
            assertEquals(addedC2, true);

            boolean addedC3 = cartridgeTest.addCartridge("c3.json", endpoint, restClient);
            assertEquals(addedC3, true);

            boolean addedG1 = cartridgeGroupTest.addCartridgeGroup("cartrdige-nested.json",
                    endpoint, restClient);
            assertEquals(addedG1, true);
            CartridgeGroupBean beanG1 = cartridgeGroupTest.getCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(beanG1.getName(), "G1");

            boolean addedN1 = networkPartitionTest.addNetworkPartition("network-partition-1.json",
                    endpoint, restClient);
            assertEquals(addedN1, true);

            boolean addedN2 = networkPartitionTest.addNetworkPartition("network-partition-2.json",
                    endpoint, restClient);
            assertEquals(addedN2, true);

            boolean addedDep = deploymentPolicyTest.addDeploymentPolicy("deployment-policy-1.json",
                    endpoint, restClient);
            assertEquals(addedDep, true);

            boolean added = applicationTest.addApplication("g-sc-G123-1.json",
                    endpoint, restClient);
            assertEquals(added, true);
            ApplicationBean bean = applicationTest.getApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(bean.getApplicationId(), "g-sc-G123-1");

            boolean addAppPolicy = applicationPolicyTest.addApplicationPolicy(
                    "application-policy-1.json", endpoint, restClient);
            assertEquals(addAppPolicy, true);

            ApplicationPolicyBean policyBean = applicationPolicyTest.getApplicationPolicy(
                    "application-policy-1", endpoint, restClient);

            //deploy the application
            boolean deployed = applicationTest.deployApplication(bean.getApplicationId(),
                    policyBean.getId(), endpoint, restClient);
            assertEquals(deployed, true);

            //Application active handling
            assertApplicationActivation(bean.getApplicationId());

            //Group active handling
            assertGroupActivation(bean.getApplicationId());

            //Cluster active handling
            assertClusterActivation(bean.getApplicationId());

            //Updating application
            boolean updated = applicationTest.updateApplication("g-sc-G123-1.json",
                    endpoint, restClient);
            assertEquals(updated, true);

            assertGroupInstanceCount(bean.getApplicationId(), "group3", 2);
            ApplicationBean updatedBean = applicationTest.getApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(updatedBean.getApplicationId(), "g-sc-G123-1");

            boolean removedGroup = cartridgeGroupTest.removeCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(removedGroup, false);

            boolean removedAuto = autoscalingPolicyTest.removeAutoscalingPolicy("autoscaling-policy-1", endpoint,
                    restClient);
            assertEquals(removedAuto, false);

            boolean removedNet = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = deploymentPolicyTest.removeDeploymentPolicy("deployment-policy-1", endpoint,
                    restClient);
            assertEquals(removedDep, false);

            boolean unDeployed = applicationTest.undeployApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(unDeployed, true);

            assertApplicationUndeploy("g-sc-G123-1");

            boolean removed = applicationTest.removeApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = applicationTest.getApplication("g-sc-G123-1", endpoint,
                    restClient);
            assertEquals(beanRemoved, null);

            removedGroup = cartridgeGroupTest.removeCartridgeGroup("G1", endpoint,
                    restClient);
            assertEquals(removedGroup, true);

            boolean removedC1 = cartridgeTest.removeCartridge("c1", endpoint,
                    restClient);
            assertEquals(removedC1, true);

            boolean removedC2 = cartridgeTest.removeCartridge("c2", endpoint,
                    restClient);
            assertEquals(removedC2, true);

            boolean removedC3 = cartridgeTest.removeCartridge("c3", endpoint,
                    restClient);
            assertEquals(removedC3, true);

            removedAuto = autoscalingPolicyTest.removeAutoscalingPolicy("autoscaling-policy-1", endpoint,
                    restClient);
            assertEquals(removedAuto, true);

            removedDep = deploymentPolicyTest.removeDeploymentPolicy("deployment-policy-1", endpoint,
                    restClient);
            assertEquals(removedDep, true);

            //Remove network partition used by application policy
            removedNet = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(removedNet, false);

            boolean removedN2 = networkPartitionTest.removeNetworkPartition("network-partition-2", endpoint,
                    restClient);
            assertEquals(removedN2, false);

            boolean removeAppPolicy = applicationPolicyTest.removeApplicationPolicy("application-policy-1", endpoint,
                    restClient);
            assertEquals(removeAppPolicy, true);

            removedNet = networkPartitionTest.removeNetworkPartition("network-partition-1", endpoint,
                    restClient);
            assertEquals(removedNet, true);

            removedN2 = networkPartitionTest.removeNetworkPartition("network-partition-2", endpoint,
                    restClient);
            assertEquals(removedN2, true);

        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred while handling autoscaling policy", false);
        }
    }

    @Test
    public void testNetworkPartition() {
        try {
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

            DeploymentPolicyBean beanRemovedDep = deploymentPolicyTest.getDeploymentPolicy("deployment-policy-1", endpoint,
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
            boolean added = cartridgeTest.addCartridge("c0.json", endpoint, restClient);
            assertEquals(added, true);
            CartridgeBean bean = cartridgeTest.getCartridge("c0", endpoint, restClient);
            assertEquals(bean.getType(), "c0");
            assertEquals(bean.getCategory(), "Application");
            assertEquals(bean.getHost(), "qmog.cisco.com");
            for (PropertyBean property : bean.getProperty()) {
                if (property.getName().equals("payload_parameter.CEP_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com");
                } else if (property.getName().equals("payload_parameter.CEP_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com");
                } else if (property.getName().equals("payload_parameter.QTCM_NETWORK_COUNT")) {
                    assertEquals(property.getValue(), "1");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin");
                } else if (property.getName().equals("payload_parameter.QTCM_DNS_SEGMENT")) {
                    assertEquals(property.getValue(), "test");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_SECURE_PORT")) {
                    assertEquals(property.getValue(), "7711");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_PORT")) {
                    assertEquals(property.getValue(), "7611");
                } else if (property.getName().equals("payload_parameter.CEP_PORT")) {
                    assertEquals(property.getValue(), "7611");
                } else if (property.getName().equals("payload_parameter.MB_PORT")) {
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
            for (PropertyBean property : updatedBean.getProperty()) {
                if (property.getName().equals("payload_parameter.CEP_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com123");
                } else if (property.getName().equals("payload_parameter.CEP_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin123");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_IP")) {
                    assertEquals(property.getValue(), "octl.qmog.cisco.com123");
                } else if (property.getName().equals("payload_parameter.QTCM_NETWORK_COUNT")) {
                    assertEquals(property.getValue(), "3");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_ADMIN_PASSWORD")) {
                    assertEquals(property.getValue(), "admin123");
                } else if (property.getName().equals("payload_parameter.QTCM_DNS_SEGMENT")) {
                    assertEquals(property.getValue(), "test123");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_SECURE_PORT")) {
                    assertEquals(property.getValue(), "7712");
                } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_PORT")) {
                    assertEquals(property.getValue(), "7612");
                } else if (property.getName().equals("payload_parameter.CEP_PORT")) {
                    assertEquals(property.getValue(), "7612");
                } else if (property.getName().equals("payload_parameter.MB_PORT")) {
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
    }


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
     * Initialize Topology event receiver
     */
    private void initializeTopologyEventReceiver() {
        if (topologyEventReceiver == null) {
            topologyEventReceiver = new TopologyEventReceiver();
            ExecutorService executorService = StratosThreadPool.getExecutorService("STRATOS_TEST_SERVER1", 1);
            topologyEventReceiver.setExecutorService(executorService);
            topologyEventReceiver.execute();
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

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertGroupActivation(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Collection<Group> groups = application.getAllGroupsRecursively();
        for(Group group : groups) {
            assertEquals(group.getInstanceContextCount() >= group.getGroupMinInstances(), true);
        }
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertClusterActivation(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for(ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceName);
            assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                    applicationName, serviceName), service);

            Cluster cluster = service.getCluster(clusterId);
            assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                    applicationName, serviceName, clusterId), cluster);
            boolean clusterActive = false;

            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                int activeInstances = 0;
                for (Member member : cluster.getMembers()) {
                    if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                        if (member.getStatus().equals(MemberStatus.Active)) {
                            activeInstances++;
                        }
                    }
                }
                clusterActive = activeInstances >= clusterDataHolder.getMinInstances();

                if (!clusterActive) {
                    break;
                }
            }
            assertEquals(String.format("Cluster status did not change to active: [cluster-id] %s", clusterId),
                    clusterActive, true);
        }

    }


    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertApplicationUndeploy(String applicationName) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        ApplicationContext applicationContext = null;
        try {
            applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationName);
        } catch (RemoteException e) {
            log.error("Error while getting the application context for [application] " + applicationName);
        }
        while (((application != null) && application.getInstanceContextCount() > 0) ||
                (applicationContext == null || applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplication(applicationName);
            try {
                applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationName);
            } catch (RemoteException e) {
                log.error("Error while getting the application context for [application] " + applicationName);
            }
            if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                break;
            }
        }

        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);
        assertNotNull(String.format("Application Context is not found: [application-id] %s",
                applicationName), applicationContext);

        //Force undeployment after the graceful deployment
        if (application.getInstanceContextCount() > 0 ||
                applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING)) {
            log.info("Force undeployment is going to start for the [application] " + applicationName);

            applicationTest.forceUndeployApplication(applicationName, endpoint, restClient);
            while (application.getInstanceContextCount() > 0 ||
                    applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                application = ApplicationManager.getApplications().getApplication(applicationName);
                if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                    break;
                }
            }
        }
        assertEquals(String.format("Application status did not change to Created: [application-id] %s", applicationName),
                APPLICATION_STATUS_CREATED, applicationContext.getStatus());

    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertGroupInstanceCount(String applicationName, String groupAlias, int count) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        if (application != null) {
            Group group = application.getGroupRecursively(groupAlias);
            while (group.getInstanceContextCount() != count) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                    break;
                }
            }
            for (GroupInstance instance : group.getInstanceIdToInstanceContextMap().values()) {
                while (!instance.getStatus().equals(GroupStatus.Active)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        break;
                    }
                }
            }
            assertEquals(String.format("Application status did not change to active: [application-id] %s", applicationName),
                    group.getInstanceContextCount(), count);
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

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
