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

package org.apache.stratos.integration.tests.iaas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.IaasProviderInfoBean;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.junit.Assert.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * IaaS provider related test cases
 */

@Test(groups = { "iaas" })
public class IaaSProviderTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(IaaSProviderTestCase.class);

    private static final String RESOURCES_PATH = "/api";
    private static final String IDENTIFIER = "/iaasProviders";

    @Test(timeOut = DEFAULT_APPLICATION_TEST_TIMEOUT)
    public void testListIaaSProviders() throws Exception {
        log.info("Running IaaSProviderTestCase.testListIaaSProviders test method...");
        long startTime = System.currentTimeMillis();
        assertTrue(true);

        IaasProviderInfoBean iaasProviderInfo = (IaasProviderInfoBean) restClient
                .getEntity(RESOURCES_PATH, IDENTIFIER, IaasProviderInfoBean.class, "IaaSProvider");
        assertNotNull(iaasProviderInfo);
        List<String> iaasList = iaasProviderInfo.getIaasProviders();
        for (String iaas : iaasList) {
            assertThat(iaas, either(containsString("kubernetes")).or(containsString("mock")));
        }
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("IaaSProviderTestCase completed in [duration] %s ms", duration));
    }
}
