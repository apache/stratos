/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.integration.tests.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * This will handle the scale-up and scale-down of a particular cluster bursting test cases
 */
public class SingleClusterScalingTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(SingleClusterScalingTestCase.class);
    private static final String RESOURCES_PATH = "/single-cluster-scaling-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-single-cluster-scaling-test";
    private static final String cartridgeId = "c7-single-cluster-scaling-test";
    private static final String networkPartitionId = "network-partition-single-cluster-scaling-test";
    private static final String deploymentPolicyId = "deployment-policy-single-cluster-scaling-test";
    private static final String applicationPolicyId = "application-policy-single-cluster-scaling-test";
    private static final String applicationId = "single-cluster-scaling-test";
    private static final int CLUSTER_SCALE_UP_TIMEOUT = 180000;
    private static final int CLUSTER_SCALE_DOWN_TIMEOUT = 360000;
    private int activeInstancesAfterScaleup = 0;

    @Test(timeOut = APPLICATION_TEST_TIMEOUT, groups = {"stratos.application.deployment"})
    public void testDeployApplication() throws Exception {
        TopologyHandler topologyHandler = TopologyHandler.getInstance();

        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        Assert.assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        Assert.assertTrue(addedC1);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        networkPartitionId + ".json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        Assert.assertTrue(addedN1);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        Assert.assertTrue(addedDep);

        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        applicationId + ".json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(added);

        ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId);

        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        Assert.assertTrue(addAppPolicy);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath,
                RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(deployed);

        //Application active handling
        topologyHandler.assertApplicationStatus(bean.getApplicationId()
                , ApplicationStatus.Active);

        //Cluster active handling
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        //Verifying whether members got created using round robin algorithm
        assertClusterWithScalingup(bean.getApplicationId());

        //assert scale-down
        assertClusterWithScaleDown(bean.getApplicationId());

        //Check whether cluster could scale-down upto the minimum
        assertClusterScaleDownToMinimumCount(bean.getApplicationId());

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedAuto);

        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId,
                RestConstants.NETWORK_PARTITIONS_NAME);
        //Trying to remove the used network partition
        assertFalse(removedNet);

        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedDep);

        //Un-deploying the application
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(applicationId);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for the [application] %s", applicationId));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId+
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(applicationId);
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    applicationId), forceUndeployed);
        }

        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, applicationId,
                RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                applicationId, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId,
                RestConstants.CARTRIDGES_NAME);
        Assert.assertTrue(removedC1);

        removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        Assert.assertTrue(removedAuto);

        removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId, RestConstants.DEPLOYMENT_POLICIES_NAME);
        Assert.assertTrue(removedDep);

        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId, RestConstants.NETWORK_PARTITIONS_NAME);
        assertFalse(removedNet);

        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                applicationPolicyId, RestConstants.APPLICATION_POLICIES_NAME);
        Assert.assertTrue(removeAppPolicy);

        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId, RestConstants.NETWORK_PARTITIONS_NAME);
        Assert.assertTrue(removedNet);
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertClusterWithScalingup(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);
        boolean clusterScaleup = false;
        String clusterId = null;
        long startTime = System.currentTimeMillis();
        while (!clusterScaleup) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignore) {
            }
            Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
            for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
                String serviceName = clusterDataHolder.getServiceType();
                clusterId = clusterDataHolder.getClusterId();
                Service service = TopologyManager.getTopology().getService(serviceName);
                assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                        applicationName, serviceName), service);

                Cluster cluster = service.getCluster(clusterId);
                assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                        applicationName, serviceName, clusterId), cluster);
                for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                    int activeInstances = 0;
                    for (Member member : cluster.getMembers()) {
                        if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                            if (member.getStatus().equals(MemberStatus.Active)) {
                                activeInstances++;
                            }
                        }
                    }

                    clusterScaleup = activeInstances > clusterDataHolder.getMinInstances();
                    if (clusterScaleup) {
                        activeInstancesAfterScaleup = activeInstances;
                        break;
                    }
                }
                application = ApplicationManager.getApplications().getApplication(applicationName);
                if ((System.currentTimeMillis() - startTime) > CLUSTER_SCALE_UP_TIMEOUT) {
                    break;
                }
            }
        }
        assertEquals(true, clusterScaleup, String.format("Cluster did not get scaled up: [cluster-id] %s", clusterId));
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertClusterWithScaleDown(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);
        boolean clusterScaleDown = false;
        String clusterId = null;
        long startTime = System.currentTimeMillis();
        while (!clusterScaleDown) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignore) {
            }
            Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
            for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
                String serviceName = clusterDataHolder.getServiceType();
                clusterId = clusterDataHolder.getClusterId();
                Service service = TopologyManager.getTopology().getService(serviceName);
                assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                        applicationName, serviceName), service);

                Cluster cluster = service.getCluster(clusterId);
                assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                        applicationName, serviceName, clusterId), cluster);
                for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                    int activeInstances = 0;
                    for (Member member : cluster.getMembers()) {
                        if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                            if (member.getStatus().equals(MemberStatus.Active)) {
                                activeInstances++;
                            }
                        }
                    }

                    if (activeInstances > activeInstancesAfterScaleup) {
                        activeInstancesAfterScaleup = activeInstances;
                    }

                    clusterScaleDown = activeInstancesAfterScaleup - 1 == activeInstances;
                    if (clusterScaleDown) {
                        break;
                    }

                }

                application = ApplicationManager.getApplications().getApplication(applicationName);
                if ((System.currentTimeMillis() - startTime) > CLUSTER_SCALE_DOWN_TIMEOUT) {
                    break;
                }
            }
        }
        assertEquals(clusterScaleDown, true,
                String.format("Cluster did not get scaled up: [cluster-id] %s", clusterId));
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertClusterScaleDownToMinimumCount(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);
        boolean clusterScaleDown = false;
        String clusterId = null;
        long startTime = System.currentTimeMillis();
        while (!clusterScaleDown) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignore) {
            }
            Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
            for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
                String serviceName = clusterDataHolder.getServiceType();
                clusterId = clusterDataHolder.getClusterId();
                Service service = TopologyManager.getTopology().getService(serviceName);
                assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                        applicationName, serviceName), service);

                Cluster cluster = service.getCluster(clusterId);
                assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                        applicationName, serviceName, clusterId), cluster);
                for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                    int activeInstances = 0;
                    for (Member member : cluster.getMembers()) {
                        if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                            if (member.getStatus().equals(MemberStatus.Active)) {
                                activeInstances++;
                            }
                        }
                    }
                    clusterScaleDown = activeInstances == clusterDataHolder.getMinInstances();
                    if (clusterScaleDown) {
                        break;
                    }
                }
                application = ApplicationManager.getApplications().getApplication(applicationName);
                if ((System.currentTimeMillis() - startTime) > CLUSTER_SCALE_DOWN_TIMEOUT) {
                    break;
                }
            }
        }
        assertEquals(clusterScaleDown, true,
                String.format("Cluster did not get scaled up: [cluster-id] %s", clusterId));
    }
}