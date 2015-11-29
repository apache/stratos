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

import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupReferenceBean;
import org.apache.stratos.common.beans.cartridge.CartridgeReferenceBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Sample application tests with application add, .
 */
@Test(groups = { "application" })
public class SampleApplicationsTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(SampleApplicationsTestCase.class);
    private static final String RESOURCES_PATH = "/sample-applications-test";
    long startTime;

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT,
          priority = 1)
    public void testApplication() throws Exception {
        startTime = System.currentTimeMillis();
        log.info("Running SampleApplicationsTestCase.testApplication test method...");

        String autoscalingPolicyId = "autoscaling-policy-sample-applications-test";
        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(addedScalingPolicy, true);

        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-sample-applications-test.json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC1, true);

        boolean addedC2 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c2-sample-applications-test.json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC2, true);

        boolean addedC3 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c3-sample-applications-test.json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC3, true);

        boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + "cartridge-nested-sample-applications-test.json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(addedG1, true);

        CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                getEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test", CartridgeGroupBean.class,
                        RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(beanG1.getName(), "G1-sample-applications-test");

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-sample-applications-test-1.json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN1, true);

        boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-sample-applications-test-2.json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN2, true);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-sample-applications-test.json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(addedDep, true);

        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                        "g-sc-G123-1-sample-applications-test.json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(added, true);

        ApplicationBean bean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-sample-applications-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertEquals(bean.getApplicationId(), "g-sc-G123-1-sample-applications-test");

        CartridgeGroupReferenceBean group1 = bean.getComponents().getGroups().get(0);
        CartridgeGroupReferenceBean group2 = group1.getGroups().get(0);
        CartridgeGroupReferenceBean group3 = group2.getGroups().get(0);
        CartridgeReferenceBean c1 = group1.getCartridges().get(0);
        CartridgeReferenceBean c2 = group2.getCartridges().get(0);
        CartridgeReferenceBean c3 = group3.getCartridges().get(0);

        assertEquals(group1.getName(), "G1-sample-applications-test");
        assertEquals(group1.getAlias(), "group1-sample-applications-test");
        assertEquals(group1.getGroupMaxInstances(), 1);
        assertEquals(group1.getGroupMinInstances(), 1);

        assertEquals(c1.getType(), "c1-sample-applications-test");
        assertEquals(c1.getCartridgeMin(), 1);
        assertEquals(c1.getCartridgeMax(), 2);

        assertEquals(group2.getAlias(), "group2-sample-applications-test");
        assertEquals(group2.getName(), "G2-sample-applications-test");
        assertEquals(group2.getGroupMaxInstances(), 1);
        assertEquals(group2.getGroupMinInstances(), 1);

        assertEquals(c2.getType(), "c2-sample-applications-test");
        assertEquals(c2.getCartridgeMin(), 1);
        assertEquals(c2.getCartridgeMax(), 2);

        assertEquals(group3.getAlias(), "group3-sample-applications-test");
        assertEquals(group3.getName(), "G3-sample-applications-test");
        assertEquals(group3.getGroupMaxInstances(), 2);
        assertEquals(group3.getGroupMinInstances(), 1);

        assertEquals(c3.getType(), "c3-sample-applications-test");
        assertEquals(c3.getCartridgeMin(), 1);
        assertEquals(c3.getCartridgeMax(), 2);

        boolean updated = restClient.updateEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH +
                        "/g-sc-G123-1-sample-applications-test-v1.json", RestConstants.APPLICATIONS,
                RestConstants.APPLICATIONS_NAME);
        assertEquals(updated, true);

        ApplicationBean updatedBean = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-sample-applications-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);

        assertEquals(bean.getApplicationId(), "g-sc-G123-1-sample-applications-test");

        group1 = updatedBean.getComponents().getGroups().get(0);
        group2 = group1.getGroups().get(0);
        group3 = group2.getGroups().get(0);
        c1 = group1.getCartridges().get(0);
        c2 = group2.getCartridges().get(0);
        c3 = group3.getCartridges().get(0);

        assertEquals(group1.getName(), "G1-sample-applications-test");
        assertEquals(group1.getAlias(), "group1-sample-applications-test");
        assertEquals(group1.getGroupMaxInstances(), 1);
        assertEquals(group1.getGroupMinInstances(), 1);

        assertEquals(c1.getType(), "c1-sample-applications-test");
        assertEquals(c1.getCartridgeMin(), 2);
        assertEquals(c1.getCartridgeMax(), 3);

        assertEquals(group2.getAlias(), "group2-sample-applications-test");
        assertEquals(group2.getName(), "G2-sample-applications-test");
        assertEquals(group2.getGroupMaxInstances(), 1);
        assertEquals(group2.getGroupMinInstances(), 1);

        assertEquals(c2.getType(), "c2-sample-applications-test");
        assertEquals(c2.getCartridgeMin(), 2);
        assertEquals(c2.getCartridgeMax(), 4);

        assertEquals(group3.getAlias(), "group3-sample-applications-test");
        assertEquals(group3.getName(), "G3-sample-applications-test");
        assertEquals(group3.getGroupMaxInstances(), 3);
        assertEquals(group3.getGroupMinInstances(), 2);

        assertEquals(c3.getType(), "c3-sample-applications-test");
        assertEquals(c3.getCartridgeMin(), 2);
        assertEquals(c3.getCartridgeMax(), 3);

        boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertFalse(removedGroup);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedAuto);

        boolean removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-applications-test-1",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        //Trying to remove the used network partition
        assertFalse(removedNet);

        boolean removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-sample-applications-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedDep);

        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-sample-applications-test",
                RestConstants.APPLICATIONS_NAME);
        assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, "g-sc-G123-1-sample-applications-test", ApplicationBean.class,
                        RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(removedGroup, true);

        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, "c1-sample-applications-test", RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC1, true);

        boolean removedC2 = restClient
                .removeEntity(RestConstants.CARTRIDGES, "c2-sample-applications-test", RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC2, true);

        boolean removedC3 = restClient
                .removeEntity(RestConstants.CARTRIDGES, "c3-sample-applications-test", RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC3, true);

        removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(removedAuto, true);

        removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-sample-applications-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(removedDep, true);

        removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-applications-test-1",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, true);

        boolean removedN2 = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-applications-test-2",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedN2, true,
                "[Network partition] network-partition-sample-applications-test-2 could not be removed for "
                        + "[application] g-sc-G123-1-sample-applications-test");
    }

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT,
          priority = 2)
    public void testApplicationList() throws Exception {
        log.info("Running SampleApplicationsTestCase.testApplicationList test method...");

        String autoscalingPolicyId = "autoscaling-policy-sample-applications-test";
        boolean addedScalingPolicy = restClient.addEntity(
                RESOURCES_PATH + RestConstants.AUTOSCALING_POLICIES_PATH + "/" + autoscalingPolicyId + ".json",
                RestConstants.AUTOSCALING_POLICIES, RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(addedScalingPolicy, true);

        boolean addedC1 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c1-sample-applications-test.json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC1, true);

        boolean addedC2 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c2-sample-applications-test.json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC2, true);

        boolean addedC3 = restClient
                .addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" + "c3-sample-applications-test.json",
                        RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertEquals(addedC3, true);

        boolean addedG1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                        "/" + "cartridge-nested-sample-applications-test.json", RestConstants.CARTRIDGE_GROUPS,
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(addedG1, true);

        CartridgeGroupBean beanG1 = (CartridgeGroupBean) restClient.
                getEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test", CartridgeGroupBean.class,
                        RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(beanG1.getName(), "G1-sample-applications-test");

        boolean addedN1 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-sample-applications-test-1.json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN1, true);

        boolean addedN2 = restClient.addEntity(RESOURCES_PATH + RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                        "network-partition-sample-applications-test-2.json", RestConstants.NETWORK_PARTITIONS,
                RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(addedN2, true);

        boolean addedDep = restClient.addEntity(RESOURCES_PATH + RestConstants.DEPLOYMENT_POLICIES_PATH + "/" +
                        "deployment-policy-sample-applications-test.json", RestConstants.DEPLOYMENT_POLICIES,
                RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(addedDep, true);

        String app1 = "sample-applications-test-1";
        String app2 = "sample-applications-test-2";
        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                app1 + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(added, true);

        added = restClient.addEntity(RESOURCES_PATH + RestConstants.APPLICATIONS_PATH + "/" +
                app2 + ".json", RestConstants.APPLICATIONS, RestConstants.APPLICATIONS_NAME);
        assertEquals(added, true);

        Type listType = new TypeToken<ArrayList<ApplicationBean>>() {
        }.getType();

        List<ApplicationBean> applicationList = (List<ApplicationBean>) restClient.
                listEntity(RestConstants.APPLICATIONS, listType, RestConstants.APPLICATIONS_NAME);
        assertTrue(applicationList.size() >= 2);

        ApplicationBean bean1 = null;
        for (ApplicationBean applicationBean : applicationList) {
            if (applicationBean.getApplicationId().equals(app1)) {
                bean1 = applicationBean;
            }
        }
        assertNotNull(bean1);

        ApplicationBean bean2 = null;
        for (ApplicationBean applicationBean : applicationList) {
            if (applicationBean.getApplicationId().equals(app2)) {
                bean2 = applicationBean;
            }
        }
        assertNotNull(bean2);

        boolean removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertFalse(removedGroup);

        boolean removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertFalse(removedAuto);

        boolean removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-applications-test-1",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        //Trying to remove the used network partition
        assertFalse(removedNet);

        boolean removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-sample-applications-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertFalse(removedDep);

        boolean removed = restClient.removeEntity(RestConstants.APPLICATIONS, app1, RestConstants.APPLICATIONS_NAME);
        assertTrue(removed);

        ApplicationBean beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, app1, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        removed = restClient.removeEntity(RestConstants.APPLICATIONS, app2, RestConstants.APPLICATIONS_NAME);
        assertTrue(removed);

        beanRemoved = (ApplicationBean) restClient
                .getEntity(RestConstants.APPLICATIONS, app2, ApplicationBean.class, RestConstants.APPLICATIONS_NAME);
        assertNull(beanRemoved);

        removedGroup = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G1-sample-applications-test",
                RestConstants.CARTRIDGE_GROUPS_NAME);
        assertEquals(removedGroup, true);

        boolean removedC1 = restClient
                .removeEntity(RestConstants.CARTRIDGES, "c1-sample-applications-test", RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC1, true);

        boolean removedC2 = restClient
                .removeEntity(RestConstants.CARTRIDGES, "c2-sample-applications-test", RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC2, true);

        boolean removedC3 = restClient
                .removeEntity(RestConstants.CARTRIDGES, "c3-sample-applications-test", RestConstants.CARTRIDGES_NAME);
        assertEquals(removedC3, true);

        removedAuto = restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyId,
                RestConstants.AUTOSCALING_POLICIES_NAME);
        assertEquals(removedAuto, true);

        removedDep = restClient
                .removeEntity(RestConstants.DEPLOYMENT_POLICIES, "deployment-policy-sample-applications-test",
                        RestConstants.DEPLOYMENT_POLICIES_NAME);
        assertEquals(removedDep, true);

        removedNet = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-applications-test-1",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedNet, true);

        boolean removedN2 = restClient
                .removeEntity(RestConstants.NETWORK_PARTITIONS, "network-partition-sample-applications-test-2",
                        RestConstants.NETWORK_PARTITIONS_NAME);
        assertEquals(removedN2, true,
                "[Network partition] network-partition-sample-applications-test-2 could not be removed for "
                        + "[application] g-sc-G123-1-sample-applications-test");
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("SampleApplicationsTestCase completed in [duration] %s ms", duration));
    }
}
