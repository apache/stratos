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
package org.apache.carbon.throttling.test;

import org.apache.stratos.throttling.manager.scheduling.ThrottlingJob;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.utils.CarbonUtils;
import org.apache.stratos.throttling.manager.tasks.Task;
import org.apache.carbon.throttling.test.utils.BaseTestCase;

public class ThrottlingTest extends BaseTestCase {
    public void testLimitResourceSizeRules() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setAdminName("admin");
        tenant.setDomain("abc1.com");
        tenant.setAdminPassword("admin");
        tenant.setEmail("admin@abc1.com");
        tenant.setActive(true);

        int tenantId = realmService.getTenantManager().addTenant(tenant);
        realmService.getTenantManager().activateTenant(tenantId);
        registry = registryService.getGovernanceUserRegistry("admin", tenantId);

        String configFile = CarbonUtils.getCarbonConfigDirPath() + "/throttling-config1.xml";
        String ruleFile = CarbonUtils.getCarbonConfigDirPath() + "/throttling-rules1.drl";
        // the configuration init will initialize task objects.
        //ThrottlingConfiguration ignore = new ThrottlingConfiguration(configFile);
        Task task = getThrottlingTask(configFile, ruleFile);

        Resource r1 = registry.newResource();
        r1.setContent("12345678");   // 8 byte is ok
        registry.put("/bang", r1);

        new ThrottlingJob().executeTask(task);

        try {
            putEmptyResource();
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        r1.setContent("123456789");   // 9 byte is not ok
        registry.put("/bang", r1);

        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("Your data is big. Bigger than 8", e.getMessage());
        }

        registry.delete("/bang");
        new ThrottlingJob().executeTask(task);

        r1.setContent("1234567891011");   // > 10 byte is not ok
        registry.put("/bang", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
            e.printStackTrace();
        }
        registry.delete("/bang");
        new ThrottlingJob().executeTask(task);

        r1.setContent("1234567890123456");
        registry.put("/bang", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("the strange condition for test1 is met", e.getMessage());
        }
        registry.delete("/bang");
        new ThrottlingJob().executeTask(task);

        r1.setContent("12345678901234567");
        registry.put("/bang", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("the strange condition for test2 is met", e.getMessage());
        }
    }

    public void testBandwidthRules() throws Exception {

        Tenant tenant = new Tenant();
        tenant.setAdminName("admin");
        tenant.setDomain("abc2.com");
        tenant.setAdminPassword("admin");
        tenant.setEmail("admin@abc2.com");
        tenant.setActive(true);

        int tenantId = realmService.getTenantManager().addTenant(tenant);
        realmService.getTenantManager().activateTenant(tenantId);
        registry = registryService.getGovernanceUserRegistry("admin", tenantId);

        String configFile = CarbonUtils.getCarbonConfigDirPath() + "/throttling-config2.xml";
        String ruleFile = CarbonUtils.getCarbonConfigDirPath() + "/throttling-rules2.drl";
        // the configuration init will initialize task objects.
        //ThrottlingConfiguration ignore = new ThrottlingConfiguration(configFile);
        Task task = getThrottlingTask(configFile, ruleFile);

        Resource r1 = registry.newResource();
        r1.setContent("12345678");   // 8 byte in
        registry.put("/bang", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
            e.printStackTrace();
        }

        r1.setContent("12345678");   // another 8 byte in
        registry.put("/bang", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("Your incoming bw is big. Bigger than 12. your value: 16", e.getMessage());
        }

        // setting another 8 bytes, this should give the early message rather than a new one

        new ThrottlingJob().executeTask(task);
        r1.setContent("12345678");   // another 8 byte in
        try {
            registry.put("/bang", r1);
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            // and same exception message (no bigger number than 16)
            assertEquals("Your incoming bw is big. Bigger than 12. your value: 16", e.getMessage());
        }
    }

    public void testDBVolume() throws Exception {

        Tenant tenant = new Tenant();
        tenant.setAdminName("admin");
        tenant.setDomain("abc3.com");
        tenant.setAdminPassword("admin");
        tenant.setEmail("admin@abc3.com");
        tenant.setActive(true);

        int tenantId = realmService.getTenantManager().addTenant(tenant);
        realmService.getTenantManager().activateTenant(tenantId);
        registry = registryService.getGovernanceUserRegistry("admin", tenantId);

        String configFile = CarbonUtils.getCarbonConfigDirPath() + "/throttling-config3.xml";
        String ruleFile = CarbonUtils.getCarbonConfigDirPath() + "/throttling-rules3.drl";
        // the configuration init will initialize task objects.
        //ThrottlingConfiguration ignore = new ThrottlingConfiguration(configFile);
        Task task = getThrottlingTask(configFile, ruleFile);

        Resource r1 = registry.newResource();
        r1.setContent("12345678");   // 8 byte in

        registry.put("/bang", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
            e.printStackTrace();
        }

        r1.setContent("12345678");   // another 8 byte in
        registry.put("/bang2", r1);
        new ThrottlingJob().executeTask(task);
        try {
            putEmptyResource();
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("Your resource volume is big. Bigger than 12. your value: 16", e.getMessage());
        }

        // setting another 8 bytes, this should give the early message rather than a new one
        // if the pre handlers are doing the job. because the value 16 will be increased only
        // it goes through the pre dataProviderConfigs

        new ThrottlingJob().executeTask(task);
        r1.setContent("12345678");   // another 8 byte in
        try {
            registry.put("/bang3", r1);
            assertTrue(false); // we expect the exception
        } catch (Exception e) {
            assertTrue(true);
            // and same exception message (no bigger number than 16)
            assertEquals("Your resource volume is big. Bigger than 12. your value: 16", e.getMessage());
        }
    }
}
