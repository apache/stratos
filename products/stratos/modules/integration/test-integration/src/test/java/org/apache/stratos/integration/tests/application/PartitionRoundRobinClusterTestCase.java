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
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * This will handle the scale-up and scale-down of a particular cluster bursting test cases
 */
@Test(groups = { "application", "round-robin" })
public class PartitionRoundRobinClusterTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(PartitionRoundRobinClusterTestCase.class);
    private static final String RESOURCES_PATH = "/partition-round-robin-cluster-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-partition-round-robin-test";
    private static final String cartridgeId = "c7-partition-round-robin-test";
    private static final String networkPartitionId = "network-partition-partition-round-robin-test";
    private static final String deploymentPolicyId = "deployment-policy-partition-round-robin-test";
    private static final String applicationId = "partition-round-robin-test";
    private static final String applicationPolicyId = "application-policy-partition-round-robin-test";
    private TopologyHandler topologyHandler = TopologyHandler.getInstance();

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT)
    public void testDeployApplication() throws Exception {
        log.info("Running PartitionRoundRobinClusterTestCase.testDeployApplication test method...");
        long startTime = System.currentTimeMillis();

        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        Assert.assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId + ".json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        Assert.assertTrue(addedC1);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                networkPartitionId + ".json", RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        Assert.assertTrue(addedN1);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId + ".json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        Assert.assertTrue(addedDep);

        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                applicationId + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(added);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), applicationId);

        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        applicationPolicyId + ".json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        Assert.assertTrue(addAppPolicy);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId;
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(deployed);

        //Application active handling
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        //Cluster active handling
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        //Verifying whether members got created using round robin algorithm
        assertClusterWithRoundRobinAlgorithm(bean.getApplicationId());

        //Un-deploying the application
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy(applicationId);
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info(String.format("Force undeployment is going to start for [application-id] %s", applicationId));

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy(applicationId);
            assertTrue(String.format("Forceful undeployment failed for the application %s", applicationId),
                    forceUndeployed);

        }

        boolean removed = restClient
                .removeEntity(RestConstants.APPLICATIONS, applicationId, RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, applicationId, ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeId, RestConstants.CARTRIDGES_NAME);
        Assert.assertTrue(removedC1);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        Assert.assertTrue(removedAuto);

        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        Assert.assertTrue(removedDep);

        boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                RestConstants.APPLICATION_POLICIES_NAME);
        Assert.assertTrue(removeAppPolicy);

        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                RestConstants.NETWORK_PARTITIONS_NAME);
        Assert.assertTrue(removedNet);
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("PartitionRoundRobinClusterTestCase completed in [duration] %s ms", duration));
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    private void assertClusterWithRoundRobinAlgorithm(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceName);
            assertNotNull(String.format("Service is not found: [application-id] %s [service] %s", applicationName,
                    serviceName), service);

            Cluster cluster = service.getCluster(clusterId);
            assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                    applicationName, serviceName, clusterId), cluster);

            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                List<String> partitionsUsedInMembers = new ArrayList<String>();
                Map<String, List<Long>> partitionIdToMembersMap = new HashMap<String, List<Long>>();
                for (Member member : cluster.getMembers()) {
                    String partitionId = member.getPartitionId();
                    if (!partitionIdToMembersMap.containsKey(partitionId)) {
                        List<Long> members = new ArrayList<Long>();
                        members.add(member.getInitTime());
                        partitionIdToMembersMap.put(partitionId, members);
                    } else {
                        partitionIdToMembersMap.get(partitionId).add(member.getInitTime());
                    }
                    if (!partitionsUsedInMembers.contains(partitionId)) {
                        partitionsUsedInMembers.add(partitionId);
                    }
                }
                String p1 = "network-partition-11-partition-1";
                String p2 = "network-partition-11-partition-2";
                List<Long> p1InitTime = partitionIdToMembersMap.get(p1);
                Collections.sort(p1InitTime);

                List<Long> p2InitTime = partitionIdToMembersMap.get(p2);
                Collections.sort(p2InitTime);

                List<Long> allInitTime = new ArrayList<>();
                allInitTime.addAll(p1InitTime);
                allInitTime.addAll(p2InitTime);
                Collections.sort(allInitTime);

                int p1Index = -1;
                int p2Index = -1;
                String previousPartition = null;
                for (int i = 0; i < allInitTime.size(); i++) {
                    if (previousPartition == null) {
                        if (Objects.equals(p1InitTime.get(0), allInitTime.get(i))) {
                            previousPartition = p1;
                            p1Index++;
                        } else if (Objects.equals(p2InitTime.get(0), allInitTime.get(i))) {
                            previousPartition = p2;
                            p2Index++;

                        }
                    } else if (previousPartition.equals(p1)) {
                        p2Index++;
                        previousPartition = p2;
                        assertEquals(allInitTime.get(i), p2InitTime.get(p2Index),
                                "Partition-2 doesn't not contain correct values in current iteration");
                        if (p1Index >= 0) {
                            assertEquals(allInitTime.get(i - 1), p1InitTime.get(p1Index),
                                    "Partition-1 doesn't not contain correct values in the previous iteration");
                            if (p1Index + 1 <= (p1InitTime.size() - 1) && i + 1 <= (allInitTime.size() - 1)) {
                                assertEquals(allInitTime.get(i + 1), p1InitTime.get(p1Index + 1),
                                        "Partition-1 doesn't not contain correct values in the next iteration");
                            }
                        }
                    } else {
                        p1Index++;
                        previousPartition = p1;
                        assertEquals(allInitTime.get(i), p1InitTime.get(p1Index),
                                "Partition-1 doesn't not contain correct values in current iteration");
                        if (p2Index >= 0) {
                            assertEquals(allInitTime.get(i - 1), p2InitTime.get(p2Index),
                                    "Partition-2 doesn't not contain correct values in the previous iteration");
                            if ((p2Index + 1) <= (p2InitTime.size() - 1) && (i + 1) <= (allInitTime.size() - 1)) {
                                assertEquals(allInitTime.get(i + 1), p2InitTime.get(p2Index + 1),
                                        "Partition-2 doesn't not contain correct values in the next iteration");
                            }
                        }
                    }
                }
            }
        }
    }
}
