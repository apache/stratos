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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;

import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.testng.annotations.Test;

/**
 * Handling the startup order of the application
 */
public class ApplicationStartupOrderTestCase extends StratosIntegrationTest {
    private static final String RESOURCES_PATH = "/application-startup-order-test";
    private static final String autoscalingPolicyId = "autoscaling-policy-application-startup-order-test";
    private static final String cartridgeId1 = "esb-application-startup-order-test";
    private static final String cartridgeId2 = "php-application-startup-order-test";
    private static final String cartridgeId3 = "mysql-application-startup-order-test";
    private static final String cartridgeId4 = "postgres-application-startup-order-test";
    private static final String cartridgeId5 = "tomcat-application-startup-order-test";
    private static final String cartridgeGroupId1 = "app-group-application-startup-order-test";
    private static final String cartridgeGroupId2 = "db-group-application-startup-order-test";
    private static final String networkPartitionId1 = "network-partition-1-application-startup-order-test";
    private static final String networkPartitionId2 = "network-partition-2-application-startup-order-test";
    private static final String deploymentPolicyId1 = "deployment-policy-1-application-startup-order-test";
    private static final String deploymentPolicyId2 = "deployment-policy-2-application-startup-order-test";
    private static final String applicationId = "application-startup-order-test";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test(timeOut = APPLICATION_TEST_TIMEOUT, groups = {"stratos.application.deployment"})
    public void testApplicationStartupOrder() throws Exception {
    	
    	thrown.expect(RuntimeException.class);
    	thrown.expectMessage(
    			"{\"status\":\"error\",\"message\":\"The startup-order defined in the [application] application-startup-order-test is not correct. [startup-order-alias] group.my-dbgroup3333 is not there in the application.\"}");

        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId1 + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        boolean addedC2 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId2 + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC2);

        boolean addedC3 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId3 + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC3);

        boolean addedC4 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId4 + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC4);

        boolean addedC5 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + cartridgeId5 + ".json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC5);

        boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + cartridgeGroupId1 + ".json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG1);

        boolean addedG2 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + cartridgeGroupId2 + ".json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG2);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        networkPartitionId1 + ".json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        networkPartitionId2 + ".json",
        RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN2);

        boolean addedDep1 = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId1 + ".json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep1);

        boolean addedDep2 = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        deploymentPolicyId2 + ".json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep2);
            	     			
    	try {
            restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                            applicationId + ".json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        	fail("Should throw an exception if the aliases mentioned in dependency order section are not defined");
    	} catch (Exception e) {
    		assertThat(
    				e.getMessage(),containsString("The startup-order defined in the [application] application-startup-order-test is not correct. [startup-order-alias] group.my-dbgroup3333 is not there in the application."));
    	}
    	// Clean up        
        boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                cartridgeGroupId1,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                cartridgeGroupId2,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);   
                
        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId1, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);
        
        removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                deploymentPolicyId2, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);
        
        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId1,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
        
        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                networkPartitionId2,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);

        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId1,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);
        
        boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId2,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC2);

        boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId3,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC3);

        boolean removedC4 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId4,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC4);

        boolean removedC5 = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeId5,
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC5);
    }
}