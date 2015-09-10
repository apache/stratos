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
package org.apache.stratos.integration.tests.policies;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Test to handle autoscaling policy CRUD operations
 */
public class AutoscalingPolicyTest extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(AutoscalingPolicyTest.class);
    private static final String RESOURCES_PATH = "/autoscaling-policy-test";

    @Test(timeOut = GLOBAL_TEST_TIMEOUT)
    public void testAutoscalingPolicy() {
        log.info("-------------------------Started autoscaling policy test case-------------------------");
        String policyId = "autoscaling-policy-autoscaling-policy-test";
        try {
            boolean added = restClient
                    .addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId + ".json",
                            RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertTrue(String.format("Autoscaling policy did not added: [autoscaling-policy-id] %s", policyId), added);
            AutoscalePolicyBean bean = (AutoscalePolicyBean) restClient.
                    getEntity(RestConstants.AUTOSCALING_POLICIES, policyId,
                            AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertEquals(String.format("[autoscaling-policy-id] %s is not correct", bean.getId()),
                    bean.getId(), policyId);
            assertEquals(String.format("[autoscaling-policy-id] %s RIF is not correct", policyId),
                    bean.getLoadThresholds().getRequestsInFlight().getThreshold(), 35.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Memory is not correct", policyId),
                    bean.getLoadThresholds().getMemoryConsumption().getThreshold(), 45.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Load is not correct", policyId),
                    bean.getLoadThresholds().getLoadAverage().getThreshold(), 25.0, 0.0);

            boolean updated = restClient.updateEntity(
                    RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId + "-v1.json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertTrue(String.format("[autoscaling-policy-id] %s update failed", policyId), updated);
            AutoscalePolicyBean updatedBean = (AutoscalePolicyBean) restClient.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(String.format("[autoscaling-policy-id] %s RIF is not correct", policyId),
                    updatedBean.getLoadThresholds().getRequestsInFlight().getThreshold(), 30.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Load is not correct", policyId),
                    updatedBean.getLoadThresholds().getMemoryConsumption().getThreshold(), 40.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Memory is not correct", policyId),
                    updatedBean.getLoadThresholds().getLoadAverage().getThreshold(), 20.0, 0.0);

            boolean removed = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    policyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertTrue(String.format("[autoscaling-policy-id] %s couldn't be removed", policyId),
                    removed);

            AutoscalePolicyBean beanRemoved = (AutoscalePolicyBean) restClient.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertNull(String.format("[autoscaling-policy-id] %s didn't get removed successfully",
                    policyId), beanRemoved);
            log.info("-------------------------Ended autoscaling policy test case---------------------------");
        }
        catch (Exception e) {
            log.error("An error occurred while handling [autoscaling policy] " + policyId, e);
            assertTrue("An error occurred while handling [autoscaling policy] " + policyId, false);
        }
    }

    @Test(timeOut = GLOBAL_TEST_TIMEOUT)
    public void testAutoscalingPolicyList() {
        log.info("-------------------------Started autoscaling policy list test case-------------------------");
        String policyId1 = "autoscaling-policy-autoscaling-policy-test-1";
        String policyId2 = "autoscaling-policy-autoscaling-policy-test-2";
        try {
            boolean added = restClient.addEntity(RESOURCES_PATH +
                            RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId1 + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertTrue(String.format("Autoscaling policy did not added: [autoscaling-policy-id] %s",
                    policyId1), added);

            added = restClient.addEntity(RESOURCES_PATH +
                            RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId2 + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertTrue(String.format("Autoscaling policy did not added: [autoscaling-policy-id] %s",
                    policyId2), added);


            Type listType = new TypeToken<ArrayList<AutoscalePolicyBean>>() {
            }.getType();

            List<AutoscalePolicyBean> autoscalingPolicyList = (List<AutoscalePolicyBean>) restClient.
                    listEntity(RestConstants.AUTOSCALING_POLICIES,
                            listType, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertTrue(autoscalingPolicyList.size() >= 2);

            AutoscalePolicyBean bean1 = null;
            for (AutoscalePolicyBean autoscalePolicyBean : autoscalingPolicyList) {
                if (autoscalePolicyBean.getId().equals(policyId1)) {
                    bean1 = autoscalePolicyBean;
                }
            }
            assertNotNull(bean1);

            AutoscalePolicyBean bean2 = null;
            for (AutoscalePolicyBean autoscalePolicyBean : autoscalingPolicyList) {
                if (autoscalePolicyBean.getId().equals(policyId2)) {
                    bean2 = autoscalePolicyBean;
                }
            }
            assertNotNull(bean2);


            boolean removed = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    policyId1, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertTrue(String.format("[autoscaling-policy-id] %s couldn't be removed", policyId1),
                    removed);

            AutoscalePolicyBean beanRemoved = (AutoscalePolicyBean) restClient.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId1,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertNull(String.format("[autoscaling-policy-id] %s didn't get removed successfully",
                    policyId1), beanRemoved);

            removed = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    policyId2, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertTrue(String.format("[autoscaling-policy-id] %s couldn't be removed", policyId2),
                    removed);

            beanRemoved = (AutoscalePolicyBean) restClient.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId2,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertNull(String.format("[autoscaling-policy-id] %s didn't get removed successfully",
                    policyId2), beanRemoved);

            log.info("-------------------------Ended autoscaling policy list test case---------------------------");
        }
        catch (Exception e) {
            log.error("An error occurred while handling [autoscaling policy] list", e);
            assertTrue("An error occurred while handling [autoscaling policy] list", false);
        }
    }
}
