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

package org.apache.stratos.integration.tests.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.common.TopologyHandler;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.metadata.client.beans.PropertyBean;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Deploy a sample application on mock IaaS and assert whether application instance, cluster instance, member instances
 * are getting activated. Kill the mock instance and check whether
 */
@Test(groups = { "application", "smoke" })
public class SampleApplicationStartupTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(SampleApplicationStartupTestCase.class);
    private static final String RESOURCES_PATH = "/sample-application-startup-test";
    private TopologyHandler topologyHandler = TopologyHandler.getInstance();
    private static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    private static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";
    private static final String PAYLOAD_PARAMETER_TOKEN_KEY = "TOKEN";
    private static final String PAYLOAD_PARAMETER_APPLICATION_ID_KEY = "APPLICATION_ID";
    private GsonBuilder gsonBuilder = new GsonBuilder();
    private Gson gson = gsonBuilder.create();

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT,
          description = "Application startup, activation and faulty member detection")
    public void testApplication() throws Exception {
        log.info("Running SampleApplicationStartupTestCase.testApplication test method...");
        long startTime = System.currentTimeMillis();

        String autoscalingPolicyId = "autoscaling-policy-sample-application-startup-test";
        log.info("Adding autoscaling policy [autoscale policy id] " + autoscalingPolicyId);
        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(addedScalingPolicy);

        log.info("Adding cartridge [cartridge type] c1-sample-application-startup-test");
        boolean addedC1 = restClient.addEntity(
                RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-sample-application-startup-test.json",
                RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(addedC1);

        log.info("Adding network partition [network partition id] sample-application-startup-test");
        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-sample-application-startup-test.json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(addedN1);

        log.info("Adding deployment policy [deployment policy id] deployment-policy-sample-application-startup-test");
        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-sample-application-startup-test.json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(addedDep);

        log.info("Adding application [application id] sample-application-startup-test");
        boolean addedApp = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                "sample-application-startup-test.json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        Assert.assertTrue(addedApp);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "sample-application-startup-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), "sample-application-startup-test");

        log.info(
                "Adding application policy [application policy id] application-policy-sample-application-startup-test");
        boolean addAppPolicy = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATION_POLICIES_PATH + "/" +
                        "application-policy-sample-application-startup-test.json", RestConstants.APPLICATION_POLICIES,
                RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(addAppPolicy);

        ApplicationPolicyBean policyBean = (ApplicationPolicyBean) restClient
                .getEntity(RestConstants.APPLICATION_POLICIES, "application-policy-sample-application-startup-test",
                        ApplicationPolicyBean.class, RestConstants.APPLICATION_POLICIES_NAME);
        assertEquals(policyBean.getId(), "application-policy-sample-application-startup-test");

        // Used policies/cartridges should not removed...asserting validations when removing policies
        log.info("Trying to remove used autoscaling policy...");
        boolean removedUsedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedUsedAuto);

        log.info("Trying to remove used network partition...");
        boolean removedUsedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-application-startup-test",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertFalse(removedUsedNet);

        log.info("Trying to remove used deployment policy...");
        boolean removedUsedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-sample-application-startup-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedUsedDep);

        log.info("Deploying application [application id] sample-application-startup-test using [application policy id] "
                + "application-policy-sample-application-startup-test");
        String resourcePath = RestConstants.APPLICATIONS + "/sample-application-startup-test" +
                RestConstants.APPLICATIONS_DEPLOY + "/application-policy-sample-application-startup-test";
        boolean deployed = restClient.deployEntity(resourcePath, RestConstants.APPLICATIONS_NAME);
        assertTrue(deployed);

        log.info("Trying to remove the used application policy");
        boolean removedUsedAppPolicy = restClient
                .removeEntity(RestConstants.APPLICATION_POLICIES, "application-policy-sample-application-startup-test",
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertFalse(removedUsedAppPolicy);

        log.info("Trying to remove the deployed application without undeploying first");
        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "sample-application-startup-test",
                RestConstants.APPLICATIONS_NAME);
        assertFalse(removed);

        log.info("Waiting for application status to become ACTIVE...");
        //topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Active);
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        List<Member> memberList = topologyHandler.getMembersForApplication(bean.getApplicationId());
        Assert.assertTrue(memberList.size() > 1,
                String.format("Active member list for application %s is empty", bean.getApplicationId()));
        MockInstanceMetadata mockInstanceMetadata = mockIaasApiClient.getInstance(memberList.get(0).getMemberId());
        String payloadString = mockInstanceMetadata.getPayload();
        log.info("Mock instance payload properties: " + payloadString);

        Properties payloadProperties = new Properties();
        String[] parameterArray = payloadString.split(PAYLOAD_PARAMETER_SEPARATOR);
        for (String parameter : parameterArray) {
            if (parameter != null) {
                String[] nameValueArray = parameter.split(PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR, 2);
                if ((nameValueArray.length == 2)) {
                    payloadProperties.put(nameValueArray[0], nameValueArray[1]);
                }
            }
        }

        String key = "mykey";
        String val1 = "myval1";
        String val2 = "myval2";
        String accessToken = payloadProperties.getProperty(PAYLOAD_PARAMETER_TOKEN_KEY);
        String appId = payloadProperties.getProperty(PAYLOAD_PARAMETER_APPLICATION_ID_KEY);
        assertNotNull(accessToken, "Access token is null in member payload");

        log.info("Trying to add metadata for application:" + appId + ", with accessToken: " + accessToken);
        boolean hasProperty1Added = restClient.addPropertyToApplication(appId, key, val1, accessToken);
        Assert.assertTrue(hasProperty1Added, "Could not add metadata property1 to application: " + appId);

        boolean hasProperty2Added = restClient.addPropertyToApplication(appId, key, val2, accessToken);
        Assert.assertTrue(hasProperty2Added, "Could not add metadata property2 to application: " + appId);

        PropertyBean propertyBean = restClient.getApplicationProperty(appId, key, accessToken);
        log.info("Retrieved metadata property: " + gson.toJson(propertyBean));
        Assert.assertTrue(propertyBean != null && propertyBean.getValues().size() > 0, "Empty property list");
        List<String> addedValues = new ArrayList<>(Arrays.asList(val1, val2));
        boolean hasPropertiesAdded = propertyBean.getValues().containsAll(addedValues);
        Assert.assertTrue(hasPropertiesAdded, "Metadata properties retrieved are not correct");

        log.info("Terminating members in [cluster id] c1-sample-application-startup-test in mock IaaS directly to "
                + "simulate faulty members...");
        Map<String, Member> memberMap = TopologyHandler.getInstance()
                .getMembersForCluster("c1-sample-application-startup-test", bean.getApplicationId());
        for (Map.Entry<String, Member> entry : memberMap.entrySet()) {
            String memberId = entry.getValue().getMemberId();
            TopologyHandler.getInstance().terminateMemberInMockIaas(memberId, mockIaasApiClient);
            TopologyHandler.getInstance().assertMemberTermination(memberId);
        }
        // application status should be marked as inactive since some members are faulty
        log.info("Waiting for application status to become INACTIVE");
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        // application should recover itself and become active after spinning more instances
        log.info("Waiting for application status to become ACTIVE...");
        //topologyHandler.assertApplicationStatus(bean.getApplicationId(), ApplicationStatus.Active);
        TopologyHandler.getInstance().assertApplicationActiveStatus(bean.getApplicationId());

        log.info("Waiting for cluster status to become ACTIVE...");
        topologyHandler.assertClusterActivation(bean.getApplicationId());

        log.info("Un-deploying the application [application id] sample-application-startup-test");
        String resourcePathUndeploy = RestConstants.APPLICATIONS + "/sample-application-startup-test" +
                RestConstants.APPLICATIONS_UNDEPLOY;

        boolean unDeployed = restClient.undeployEntity(resourcePathUndeploy, RestConstants.APPLICATIONS_NAME);
        assertTrue(unDeployed);

        boolean undeploy = topologyHandler.assertApplicationUndeploy("sample-application-startup-test");
        if (!undeploy) {
            //Need to forcefully undeploy the application
            log.info("Force undeployment is going to start for the [application] sample-application-startup-test");

            restClient.undeployEntity(RestConstants.APPLICATIONS + "/sample-application-startup-test" +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", RestConstants.APPLICATIONS);

            boolean forceUndeployed = topologyHandler.assertApplicationUndeploy("sample-application-startup-test");
            assertTrue(String.format("Forceful undeployment failed for the application %s",
                    "sample-application-startup-test"), forceUndeployed);
        }

        log.info("Removing application [application id] sample-application-startup-test");
        boolean removedApp = restClient.removeEntity(RestConstants.APPLICATIONS, "sample-application-startup-test",
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removedApp);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "sample-application-startup-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        log.info("Removing application policy [application policy id] "
                + "application-policy-sample-application-startup-test");
        boolean removeAppPolicy = restClient
                .removeEntity(RestConstants.APPLICATION_POLICIES, "application-policy-sample-application-startup-test",
                        RestConstants.APPLICATION_POLICIES_NAME);
        assertTrue(removeAppPolicy);

        log.info("Removing cartridge [cartridge type] c1-sample-application-startup-test");
        boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c1-sample-application-startup-test",
                RestConstants.CARTRIDGES_NAME);
        assertTrue(removedC1);

        log.info("Removing autoscaling policy [autoscaling policy id] " + autoscalingPolicyId);
        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertTrue(removedAuto);

        log.info("Removing deployment policy [deployment policy id] "
                + "deployment-policy-sample-application-startup-test");
        boolean removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-sample-application-startup-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertTrue(removedDep);

        log.info("Removing network partition [network partition id] network-partition-sample-application-startup-test");
        boolean removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-application-startup-test",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertTrue(removedNet);
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("SampleApplicationStartupTestCase completed in [duration] %s ms", duration));
    }
}
