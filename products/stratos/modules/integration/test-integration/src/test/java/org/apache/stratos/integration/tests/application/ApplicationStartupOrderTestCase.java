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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test(timeOut = APPLICATION_TEST_TIMEOUT, groups = {"stratos.application.deployment"})
    public void testApplicationStartupOrder() throws Exception {
    	
    	thrown.expect(RuntimeException.class);
    	thrown.expectMessage(
    			"{\"status\":\"error\",\"message\":\"The startup-order defined in the [application] my-compositeapp is not correct. [startup-order-alias] group.my-dbgroup3333 is not there in the application.\"}");
        String autoscalingPolicyId = "autoscaling-policy-1";

        boolean addedScalingPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH
                        + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "esb.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        boolean addedC2 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "php.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC2);

        boolean addedC3 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "mysql.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC3);

        boolean addedC5 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "postgres.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC5);

        boolean addedC6 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "tomcat.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC6);

        boolean addedG2 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + "app-group.json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG2);

        boolean addedG3 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + "db-group.json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(addedG3);

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-1.json",
                RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);
        
        boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                		"network-partition-2.json",
        RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN2);
        
        boolean addedDep1 = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-1.json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep1);
        
        boolean addedDep2 = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-2.json",
                RestConstants.DEPLOYMENT_POLICIES, RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep2);
            	     			
    	try {
    	  restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        "application.json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        	fail("Should throw an exception if the aliases mentioned in dependency order section are not defined");
    	} catch (Exception e) {
    		assertThat(
    				e.getMessage(),containsString("The startup-order defined in the [application] my-compositeapp is not correct. [startup-order-alias] group.my-dbgroup3333 is not there in the application."));
    	}

    	
    	// Clean up        
        boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                "app-group",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS,
                "db-group",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertTrue(removedGroup);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                autoscalingPolicyId, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);   
                
        boolean removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                "deployment-policy-1", RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);
        
        removedDep = restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES,
                "deployment-policy-2", RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);
        
        boolean removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-1",
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
        
        removedNet = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                "network-partition-2",
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
        
        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "php",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "tomcat",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC2);

        boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "postgres",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC3);

        boolean removedC4 = restClient.removeEntity(RestConstants.CARTRIDGES, "mysql",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC4);

        boolean removedC5 = restClient.removeEntity(RestConstants.CARTRIDGES, "esb",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC5);

        assertTrue(true);
    }
   
}