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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.testng.annotations.Test;

import static junit.framework.Assert.*;

/**
 * Test to handle autoscaling policy CRUD operations
 */
public class AutoscalingPolicyTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(AutoscalingPolicyTest.class);
    private static final String RESOURCES_PATH = "/autoscaling-policy-test";


    @Test
    public void testAutoscalingPolicy() {
        log.info("-------------------------Started autoscaling policy test case-------------------------");
        String policyId = "autoscaling-policy-autoscaling-policy-test";
        try {
            boolean added = restClientTenant1.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertEquals(String.format("Autoscaling policy did not added: [autoscaling-policy-id] %s", policyId), added, true);
            AutoscalePolicyBean bean = (AutoscalePolicyBean) restClientTenant1.
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

            bean = (AutoscalePolicyBean) restClientTenant2.
                    getEntity(RestConstants.AUTOSCALING_POLICIES, policyId,
                            AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertNull("Auto scale policy exists for other tenant",bean);

            boolean addedTenant2 = restClientTenant2.addEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId + ".json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertEquals(String.format("Autoscaling policy did not added: [autoscaling-policy-id] %s", policyId), addedTenant2, true);
            bean = (AutoscalePolicyBean) restClientTenant2.
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

            boolean updated = restClientTenant1.updateEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId + "-v1.json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertEquals(String.format("[autoscaling-policy-id] %s update failed", policyId), updated, true);
            AutoscalePolicyBean updatedBean = (AutoscalePolicyBean) restClientTenant1.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(String.format("[autoscaling-policy-id] %s RIF is not correct", policyId),
                    updatedBean.getLoadThresholds().getRequestsInFlight().getThreshold(), 30.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Load is not correct", policyId),
                    updatedBean.getLoadThresholds().getMemoryConsumption().getThreshold(), 40.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Memory is not correct", policyId),
                    updatedBean.getLoadThresholds().getLoadAverage().getThreshold(), 20.0, 0.0);

            boolean updatedTenant2 = restClientTenant2.updateEntity(RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + policyId + "-v1.json",
                    RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);

            assertEquals(String.format("[autoscaling-policy-id] %s update failed", policyId), updatedTenant2, true);
            AutoscalePolicyBean updatedTenant2Bean = (AutoscalePolicyBean) restClientTenant2.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(String.format("[autoscaling-policy-id] %s RIF is not correct", policyId),
                    updatedTenant2Bean.getLoadThresholds().getRequestsInFlight().getThreshold(), 30.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Load is not correct", policyId),
                    updatedTenant2Bean.getLoadThresholds().getMemoryConsumption().getThreshold(), 40.0, 0.0);
            assertEquals(String.format("[autoscaling-policy-id] %s Memory is not correct", policyId),
                    updatedTenant2Bean.getLoadThresholds().getLoadAverage().getThreshold(), 20.0, 0.0);


            boolean removed = restClientTenant1.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    policyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(String.format("[autoscaling-policy-id] %s couldn't be removed", policyId),
                    removed, true);

            AutoscalePolicyBean beanRemoved = (AutoscalePolicyBean) restClientTenant1.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(String.format("[autoscaling-policy-id] %s didn't get removed successfully",
                    policyId), beanRemoved, null);

            beanRemoved = (AutoscalePolicyBean) restClientTenant2.getEntity(
                    RestConstants.AUTOSCALING_POLICIES, policyId,
                    AutoscalePolicyBean.class, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertNotNull("Auto scale policy not exist in other tenant",beanRemoved);

            removed = restClientTenant2.removeEntity(RestConstants.AUTOSCALING_POLICIES,
                    policyId, RestConstants.AUTOSCALING_POLICIES_NAME);
            assertEquals(String.format("[autoscaling-policy-id] %s couldn't be removed", policyId),
                    removed, true);

            log.info("-------------------------Ended autoscaling policy test case---------------------------");
        } catch (Exception e) {
            log.error("An error occurred while handling [autoscaling policy] " + policyId, e);
            assertTrue("An error occurred while handling [autoscaling policy] " + policyId, false);
        }
    }
}
