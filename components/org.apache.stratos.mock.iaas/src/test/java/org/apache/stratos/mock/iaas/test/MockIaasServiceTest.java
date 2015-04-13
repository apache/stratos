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

package org.apache.stratos.mock.iaas.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.mock.iaas.config.MockIaasConfig;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.apache.stratos.mock.iaas.persistence.PersistenceManagerType;
import org.apache.stratos.mock.iaas.services.impl.MockIaasServiceImpl;
import org.apache.stratos.mock.iaas.services.impl.MockConstants;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Mock iaas service test.
 */
public class MockIaasServiceTest {

    private static Log log = LogFactory.getLog(MockIaasServiceTest.class);
    private static final String CONFIG_FILE_PATH = "/mock-iaas.xml";

    @Test
    public void testStartInstance() {

        try {
            URL configFileUrl = getClass().getResource(CONFIG_FILE_PATH);
            System.setProperty(MockIaasConfig.MOCK_IAAS_CONFIG_FILE_PATH, configFileUrl.getPath());
            System.setProperty(MockConstants.PERSISTENCE_MANAGER_TYPE, PersistenceManagerType.Mock.toString());

            MockIaasServiceImpl mockIaasService = new MockIaasServiceImpl();
            MockInstanceContext mockInstanceContext = new MockInstanceContext("app1", "service1", "cluster1", "member1",
                    "cluster-instance1", "network-p1", "p1");
            MockInstanceMetadata metadata = mockIaasService.startInstance(mockInstanceContext);
            assertNotNull("Could not start mock instance", metadata);
            assertNotNull("Mock instance not found", mockIaasService.getInstance(metadata.getInstanceId()));
        } catch (Exception e) {
            log.error(e);
            assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void testGetInstances() {

        try {
            URL configFileUrl = getClass().getResource(CONFIG_FILE_PATH);
            System.setProperty(MockIaasConfig.MOCK_IAAS_CONFIG_FILE_PATH, configFileUrl.getPath());
            System.setProperty(MockConstants.PERSISTENCE_MANAGER_TYPE, PersistenceManagerType.Mock.toString());

            MockIaasServiceImpl mockIaasService = new MockIaasServiceImpl();
            MockInstanceContext mockInstanceContext = new MockInstanceContext("app1", "service1", "cluster1", "member1",
                    "cluster-instance1", "network-p1", "p1");
            MockInstanceMetadata metadata1 = mockIaasService.startInstance(mockInstanceContext);
            assertNotNull("Could not start mock instance", metadata1);
            assertNotNull("Mock instance not found", mockIaasService.getInstance(metadata1.getInstanceId()));

            mockInstanceContext = new MockInstanceContext("app1", "service1", "cluster1", "member2",
                    "cluster-instance1", "network-p1", "p1");
            MockInstanceMetadata metadata2 = mockIaasService.startInstance(mockInstanceContext);
            assertNotNull("Could not start mock instance", metadata2);
            assertNotNull("Mock instance not found", mockIaasService.getInstance(metadata2.getInstanceId()));

            List<MockInstanceMetadata> instances = mockIaasService.getInstances();
            assertNotNull(instances);
            assertTrue("Mock instance 1 not found in get instances result", instanceExist(instances, metadata1.getInstanceId()));
            assertTrue("Mock instance 2 not found in get instances result", instanceExist(instances, metadata2.getInstanceId()));
        } catch (Exception e) {
            log.error(e);
            assertTrue(e.getMessage(), false);
        }
    }

    private boolean instanceExist(List<MockInstanceMetadata> instances, String instanceId) {
        for (MockInstanceMetadata instance : instances) {
            if (instance.getInstanceId().equals(instanceId)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testTerminateInstance() {

        try {
            URL configFileUrl = getClass().getResource(CONFIG_FILE_PATH);
            System.setProperty(MockIaasConfig.MOCK_IAAS_CONFIG_FILE_PATH, configFileUrl.getPath());
            System.setProperty(MockConstants.PERSISTENCE_MANAGER_TYPE, PersistenceManagerType.Mock.toString());

            MockIaasServiceImpl mockIaasService = new MockIaasServiceImpl();
            MockInstanceContext mockInstanceContext = new MockInstanceContext("app1", "service1", "cluster1", "member1",
                    "cluster-instance1", "network-p1", "p1");
            MockInstanceMetadata metadata = mockIaasService.startInstance(mockInstanceContext);
            assertNotNull("Could not start mock instance", metadata);
            assertNotNull("Mock instance not found", mockIaasService.getInstance(metadata.getInstanceId()));

            mockIaasService.terminateInstance(metadata.getInstanceId());
            assertNull("Could not terminate mock instance", mockIaasService.getInstance(metadata.getInstanceId()));
        } catch (Exception e) {
            log.error(e);
            assertTrue(e.getMessage(), false);
        }
    }
}
