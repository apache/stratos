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
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.apache.stratos.integration.tests.TopologyHandler;
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
import org.testng.annotations.Test;

import java.util.Set;

import static junit.framework.Assert.*;

/**
 * This will handle the scale-up and scale-down of a particular cluster bursting test cases
 */
public class SingleClusterScalingTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(SampleApplicationsTest.class);
    private static final String TEST_PATH = "/single-cluster-scaling-test";
    private static final int CLUSTER_SCALE_UP_TIMEOUT = 180000;


    @Test
    public void testDeployApplication() {
        try {
            log.info("Started application Bursting test case**************************************");

            String autoscalingPolicyId = "autoscaling-policy-single-cluster-scaling-test";

            boolean addedScalingPolicy = restClient.addEntity(TEST_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                            + "/" + autoscalingPolicyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(addedScalingPolicy, true);

            boolean addedC1 = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c7-single-cluster-scaling-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(addedC1, true);

            boolean addedN1 = restClient.addEntity(TEST_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            "network-partition-single-cluster-scaling-test.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(addedN1, true);

            boolean addedDep = restClient.addEntity(TEST_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                            "deployment-policy-single-cluster-scaling-test.json",
                    RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(addedDep, true);

            boolean added = restClient.addEntity(TEST_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "single-cluster-scaling-test.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(added, true);

            ApplicationBean bean = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "single-cluster-scaling-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(bean.getApplicationId(), "single-cluster-scaling-test");

            boolean addAppPolicy = restClient.addEntity(TEST_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                            "application-policy-single-cluster-scaling-test.json", RestConstants.APPLICATION_POLICIES,
                    RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(addAppPolicy, true);

            ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient.getEntity(
                    RestConstants.APPLICATION_POLICIES,
                    "application-policy-single-cluster-scaling-test", ApplicationPolicyBean.class,
                    RestConstants.APPLICATION_POLICIES_NAME);

            //deploy the application
            String resourcePath = RestConstants.APPLICATIONS + "/" + "single-cluster-scaling-test" +
                    RestConstants.APPLICATIONS_DEPLOY + "/" + "application-policy-single-cluster-scaling-test";
            boolean deployed = restClient.deployEntity(resourcePath,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(deployed, true);

            //Application active handling
            TopologyHandler.getInstance().assertApplicationStatus(bean.getApplicationId()
                    , ApplicationStatus.Active);

            //Cluster active handling
            TopologyHandler.getInstance().assertClusterActivation(bean.getApplicationId());

            //Verifying whether members got created using round robin algorithm
            assertClusterWithScalingup(bean.getApplicationId());

            boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, false);

            boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-single-cluster-scaling-test",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-single-cluster-scaling-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, false);

            //Un-deploying the application
            String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + "single-cluster-scaling-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY;

            boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(unDeployed, true);

            boolean undeploy = TopologyHandler.getInstance().assertApplicationUndeploy("single-cluster-scaling-test");
            if (!undeploy) {
                //Need to forcefully undeploy the application
                log.info("Force undeployment is going to start for the [application] " + "single-cluster-scaling-test");

                restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + "single-cluster-scaling-test" +
                        RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

                boolean forceUndeployed = TopologyHandler.getInstance().assertApplicationUndeploy("single-cluster-scaling-test");
                assertEquals(String.format("Forceful undeployment failed for the application %s",
                        "single-cluster-scaling-test"), forceUndeployed, true);

            }

            boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "single-cluster-scaling-test",
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = (ApplicationBean) restClient.getEntity(RestConstants.APPLICATIONS,
                    "single-cluster-scaling-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c7-single-cluster-scaling-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);


            removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-single-cluster-scaling-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-single-cluster-scaling-test", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, false);


            boolean removeAppPolicy = restClient.removeEntity(RestConstants.APPLICATION_POLICIES,
                    "application-policy-single-cluster-scaling-test", RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(removeAppPolicy, true);

            removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-single-cluster-scaling-test", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            log.info("Ended application bursting test case**************************************");

        } catch (Exception e) {
            log.error("An error occurred while handling  application bursting", e);
            assertTrue("An error occurred while handling  application bursting", false);
        }
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
            } catch (InterruptedException ignore) {
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
                    if(clusterScaleup) {
                        break;
                    }
                }
                application = ApplicationManager.getApplications().getApplication(applicationName);
                if ((System.currentTimeMillis() - startTime) > CLUSTER_SCALE_UP_TIMEOUT) {
                    break;
                }
            }
        }
        assertEquals(String.format("Cluster did not get scaled up: [cluster-id] %s", clusterId),
                clusterScaleup, true);
    }
}
