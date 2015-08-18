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
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.apache.stratos.integration.tests.TopologyHandler;
import org.apache.stratos.integration.tests.rest.RestClient;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Sample application tests with application add, .
 */
public class ApplicationUpdateTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(ApplicationUpdateTest.class);
    private static final String RESOURCES_PATH = "/application-update-test";

    @Test
    public void testDeployApplication() {
        try {

            String autoscalingPolicyId = "autoscaling-policy-application-update-test";
            String applicationId="g-sc-G123-1-application-update-test";

            testApplicationRuntimeForTenant(restClientTenant1,tenant1Id,autoscalingPolicyId)  ;

            TopologyHandler.getInstance().assertApplicationForNonAvailability(applicationId,tenant2Id);

            //Updating application
            boolean updated = restClientTenant1.updateEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            "g-sc-G123-1-application-update-test-v1.json", RestConstants.APPLICATIONS,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(updated, true);

            TopologyHandler.getInstance().assertGroupInstanceCount(applicationId, "group3-application-update-test", 2, tenant1Id);

            TopologyHandler.getInstance().assertClusterMinMemberCount(applicationId, 1, tenant1Id);

            ApplicationBean updatedBean = (ApplicationBean) restClientTenant1.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1-application-update-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(updatedBean.getApplicationId(), "g-sc-G123-1-application-update-test");

            TopologyHandler.getInstance().assertApplicationForNonAvailability(applicationId,tenant2Id);

            boolean removedGroup = restClientTenant1.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-application-update-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, false);

            boolean removedAuto = restClientTenant1.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, false);

            boolean removedNet = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-update-test-1",
                    RestConstants.NETWORK_PARTITIONS_NAME);
            //Trying to remove the used network partition
            assertEquals(removedNet, false);

            boolean removedDep = restClientTenant1.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-application-update-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, false);

           // testApplicationRuntimeForTenant(restClientTenant2,tenant2Id,autoscalingPolicyId);

            //Un-deploying the application
            String resourcePathUndeploy = RestConstants.APPLICATIONS + "/" + "g-sc-G123-1-application-update-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY;

            boolean unDeployed = restClientTenant1.undeployEntity(resourcePathUndeploy,
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(unDeployed, true);


            boolean undeploy = TopologyHandler.getInstance().assertApplicationUndeploy("g-sc-G123-1-application-update-test", tenant1Id);
            if (!undeploy) {
                //Need to forcefully undeploy the application
                log.info("Force undeployment is going to start for the [application] " + "g-sc-G123-1-application-update-test");

                restClientTenant1.undeployEntity(RestConstants.APPLICATIONS + "/" + "g-sc-G123-1-application-update-test" +
                        RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

                boolean forceUndeployed = TopologyHandler.getInstance().assertApplicationUndeploy("g-sc-G123-1-application-update-test", tenant1Id);
                assertEquals(String.format("Forceful undeployment failed for the application %s",
                        "g-sc-G123-1-application-update-test"), forceUndeployed, true);

            }


            boolean removed = restClientTenant1.removeEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-application-update-test",
                    RestConstants.APPLICATIONS_NAME);
            assertEquals(removed, true);

            ApplicationBean beanRemoved = (ApplicationBean) restClientTenant1.getEntity(RestConstants.APPLICATIONS,
                    "g-sc-G123-1-application-update-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
            assertEquals(beanRemoved, null);

            removedGroup = restClientTenant1.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-application-update-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(removedGroup, true);

            boolean removedC1 = restClientTenant1.removeEntity(RestConstants.CARTRIDGES, "c1-application-update-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC1, true);

            boolean removedC2 = restClientTenant1.removeEntity(RestConstants.CARTRIDGES, "c2-application-update-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC2, true);

            boolean removedC3 = restClientTenant1.removeEntity(RestConstants.CARTRIDGES, "c3-application-update-test",
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removedC3, true);

            removedAuto = restClientTenant1.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(removedAuto, true);

            removedDep = restClientTenant1.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                    "deployment-policy-application-update-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
            assertEquals(removedDep, true);

            removedNet = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-update-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, false);

            boolean removedN2 = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-update-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, false);

            boolean removeAppPolicy = restClientTenant1.removeEntity(RestConstants.APPLICATION_POLICIES,
                    "application-policy-application-update-test", RestConstants.APPLICATION_POLICIES_NAME);
            assertEquals(removeAppPolicy, true);

            removedNet = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-update-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedNet, true);

            removedN2 = restClientTenant1.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    "network-partition-application-update-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removedN2, true);

           // testApplicationUndeplymentForTenant(restClientTenant2,tenant2Id,autoscalingPolicyId);

            log.info("-------------------------Ended application runtime update test case-------------------------");

        } catch (Exception e) {
            log.error("An error occurred while handling application deployment/undeployment and update", e);
            assertTrue("An error occurred while handling application deployment/undeployment and update", false);
        }
    }

    private void testApplicationRuntimeForTenant(RestClient restClientTenant,int tenantId, String autoscalingPolicyId ){

        boolean addedScalingPolicy = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(addedScalingPolicy, true);

        boolean addedC1 = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-application-update-test.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC1, true);

        boolean addedC2 = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c2-application-update-test.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC2, true);

        boolean addedC3 = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c3-application-update-test.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC3, true);

        boolean addedG1 = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + "cartrdige-nested-application-update-test.json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(addedG1, true);

        CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClientTenant.
                getEntity(RestConstants.CARTRIDGE_GROUPS, "G1-application-update-test",
                        CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(beanG1.getName(), "G1-application-update-test");

        boolean addedN1 = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-application-update-test-1.json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN1, true);

        boolean addedN2 = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-application-update-test-2.json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN2, true);

        boolean addedDep = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-application-update-test.json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(addedDep, true);

        boolean added = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        "g-sc-G123-1-application-update-test.json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(added, true);

        ApplicationBean bean = (ApplicationBean) restClientTenant.getEntity(RestConstants.APPLICATIONS,
                "g-sc-G123-1-application-update-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), "g-sc-G123-1-application-update-test");

        boolean addAppPolicy = restClientTenant.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        "application-policy-application-update-test.json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(addAppPolicy, true);

        ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClientTenant.getEntity(
                RestConstants.APPLICATION_POLICIES,
                "application-policy-application-update-test", ApplicationPolicyBean.class,
                RestConstants.APPLICATION_POLICIES_NAME);

        //deploy the application
        String resourcePath = RestConstants.APPLICATIONS + "/" + "g-sc-G123-1-application-update-test" +
                RestConstants.APPLICATIONS_DEPLOY + "/" + "application-policy-application-update-test";
        boolean deployed = restClientTenant.deployEntity(resourcePath,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(deployed, true);

        //Application active handling
        TopologyHandler.getInstance().assertApplicationStatus(bean.getApplicationId(),
                ApplicationStatus.Active, tenantId);

        //Group active handling
        TopologyHandler.getInstance().assertGroupActivation(bean.getApplicationId(), tenantId);

        //Cluster active handling
        TopologyHandler.getInstance().assertClusterActivation(bean.getApplicationId(), tenantId);
    }

    private void testApplicationUndeplymentForTenant(RestClient restClientTenant,int tenantId,String autoscalingPolicyId){
        boolean removed = restClientTenant.removeEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-application-update-test",
                RestConstants.APPLICATIONS_NAME);
        assertEquals(removed, true);

        ApplicationBean beanRemoved = (ApplicationBean) restClientTenant.getEntity(RestConstants.APPLICATIONS,
                "g-sc-G123-1-application-update-test", ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertEquals(beanRemoved, null);

        boolean removedGroup = restClientTenant.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-application-update-test",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(removedGroup, true);

        boolean removedC1 = restClientTenant.removeEntity(RestConstants.CARTRIDGES, "c1-application-update-test",
                RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC1, true);

        boolean removedC2 = restClientTenant.removeEntity(RestConstants.CARTRIDGES, "c2-application-update-test",
                RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC2, true);

        boolean removedC3 = restClientTenant.removeEntity(RestConstants.CARTRIDGES, "c3-application-update-test",
                RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC3, true);

        boolean removedAuto = restClientTenant.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(removedAuto, true);

        boolean removedDep = restClientTenant.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                "deployment-policy-application-update-test", RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(removedDep, true);

        boolean removedNet = restClientTenant.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-application-update-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, false);

        boolean removedN2 = restClientTenant.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-application-update-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedN2, false);

        boolean removeAppPolicy = restClientTenant.removeEntity(RestConstants.APPLICATION_POLICIES,
                "application-policy-application-update-test", RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(removeAppPolicy, true);

        removedNet = restClientTenant.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-application-update-test-1", RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, true);

        removedN2 = restClientTenant.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-application-update-test-2", RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedN2, true);
    }
}
