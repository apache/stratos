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

package org.apache.stratos.mock.iaas.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.apache.stratos.mock.iaas.exceptions.MockIaasException;
import org.apache.stratos.mock.iaas.persistence.PersistenceManager;
import org.apache.stratos.mock.iaas.persistence.PersistenceManagerFactory;
import org.apache.stratos.mock.iaas.persistence.PersistenceManagerType;
import org.apache.stratos.mock.iaas.services.MockIaasService;
import org.apache.stratos.mock.iaas.statistics.generator.MockHealthStatisticsGenerator;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Mock IaaS service implementation. This is a singleton class that simulates a standard Infrastructure as a Service
 * platform by creating mock instances and managing their lifecycle states.
 * <p/>
 * How does this work:
 * - Mock IaaS starts a Mock Member thread or each instance created
 * - A sample private IP and a public IP will be assigned to the instance
 * - Mock Member will publish Instance Started and Instance Activated events once the thread is started
 * - Afterwards it will start publishing sample health statistics values to CEP
 * - If the Mock IaaS was asked to terminate an instance it will stop the relevant thread
 */
public class MockIaasServiceImpl implements MockIaasService {

    private static final Log log = LogFactory.getLog(MockIaasServiceImpl.class);

    private static final ExecutorService mockMemberExecutorService =
            StratosThreadPool.getExecutorService(MockConstants.MOCK_MEMBER_THREAD_POOL,
                    MockConstants.MOCK_MEMBER_THREAD_POOL_SIZE);
    private static volatile MockIaasServiceImpl instance;

    private PersistenceManager persistenceManager;
    private MockIaasServiceUtil mockIaasServiceUtil;
    private Map<String, MockInstance> instanceIdToMockInstanceMap; // Map<InstanceId,MockInstance>

    /**
     * Default public constructor
     */
    public MockIaasServiceImpl() {
        try {
            String persistenceManagerTypeStr = System.getProperty(MockConstants.PERSISTENCE_MANAGER_TYPE,
                    PersistenceManagerType.Registry.toString());
            PersistenceManagerType persistenceManagerType = PersistenceManagerType.valueOf(persistenceManagerTypeStr);
            persistenceManager = PersistenceManagerFactory.getPersistenceManager(persistenceManagerType);
            mockIaasServiceUtil = new MockIaasServiceUtil(persistenceManager);

            instanceIdToMockInstanceMap = mockIaasServiceUtil.readFromRegistry();
        } catch (RegistryException e) {
            String message = "Could not read service name -> mock member map from registry";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        if (instanceIdToMockInstanceMap == null) {
            // No instances found in registry, create a new map
            instanceIdToMockInstanceMap = new ConcurrentHashMap<String, MockInstance>();
        }
    }

    /**
     * Start mock instance.
     *
     * @param mockInstanceContext mock instance context containing instance properties
     * @return mock instance metadata
     * @throws MockIaasException is thrown if mock instance context is null
     */
    @Override
    public MockInstanceMetadata startInstance(MockInstanceContext mockInstanceContext) throws MockIaasException {
        synchronized (MockIaasServiceImpl.class) {

            if (mockInstanceContext == null) {
                throw new MockIaasException("Mock instance context is null");
            }

            // Generate instance id
            String instanceId = UUID.randomUUID().toString();
            mockInstanceContext.setInstanceId(instanceId);

            MockInstance mockInstance = new MockInstance(mockInstanceContext);
            instanceIdToMockInstanceMap.put(instanceId, mockInstance);
            mockMemberExecutorService.submit(mockInstance);

            // Persist changes
            mockIaasServiceUtil.persistInRegistry((ConcurrentHashMap<String, MockInstance>) instanceIdToMockInstanceMap);

            String serviceName = mockInstanceContext.getServiceName();
            MockHealthStatisticsGenerator.getInstance().scheduleStatisticsUpdaterTasks(serviceName);

            // Simulate instance creation time
            sleep(2000);

            return new MockInstanceMetadata(mockInstanceContext);
        }
    }

    /**
     * Sleep the current thread for a given period of time
     *
     * @param time time in milliseconds
     */
    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Get mock instances.
     *
     * @return a list of mock instance metadata objects
     */
    @Override
    public List<MockInstanceMetadata> getInstances() {
        List<MockInstanceMetadata> mockInstanceMetadataList = new ArrayList<MockInstanceMetadata>();
        for (Map.Entry<String, MockInstance> entry : instanceIdToMockInstanceMap.entrySet()) {
            MockInstanceContext mockMemberContext = entry.getValue().getMockInstanceContext();
            mockInstanceMetadataList.add(new MockInstanceMetadata(mockMemberContext));
        }
        return mockInstanceMetadataList;
    }

    /**
     * Get mock instance context by instance id.
     *
     * @param instanceId mock instance id
     * @return mock instance metadata of the mock instance
     */
    @Override
    public MockInstanceMetadata getInstance(String instanceId) {
        if (instanceIdToMockInstanceMap.containsKey(instanceId)) {
            MockInstanceContext mockInstanceContext = instanceIdToMockInstanceMap.get(instanceId).getMockInstanceContext();
            return new MockInstanceMetadata(mockInstanceContext);
        }
        return null;
    }

    /**
     * Allocate ip address to mock instance.
     *
     * @param instanceId mock instance id
     * @return mock instance metadata including ip addresses
     * @throws MockIaasException is thrown if instance is not found
     */
    @Override
    public MockInstanceMetadata allocateIpAddress(String instanceId) throws MockIaasException {
        MockInstance mockInstance = instanceIdToMockInstanceMap.get(instanceId);
        if (mockInstance == null) {
            throw new MockIaasException(String.format("Mock instance not found: [instance-id] %s", instanceId));
        }

        MockInstanceContext mockInstanceContext = mockInstance.getMockInstanceContext();
        mockInstanceContext.setDefaultPrivateIP(MockIPAddressPool.getInstance().getNextPrivateIpAddress());
        mockInstanceContext.setDefaultPublicIP(MockIPAddressPool.getInstance().getNextPublicIpAddress());
        mockIaasServiceUtil.persistInRegistry((ConcurrentHashMap<String, MockInstance>) instanceIdToMockInstanceMap);

        return new MockInstanceMetadata(mockInstanceContext);
    }

    /**
     * Terminate mock instance by instance id.
     *
     * @param instanceId mock instance id
     */
    @Override
    public void terminateInstance(String instanceId) {
        synchronized (MockIaasServiceImpl.class) {
            log.info(String.format("Terminating instance: [instance-id] %s", instanceId));

            MockInstance mockInstance = instanceIdToMockInstanceMap.get(instanceId);
            if (mockInstance != null) {
                String serviceName = mockInstance.getMockInstanceContext().getServiceName();

                mockInstance.terminate();
                instanceIdToMockInstanceMap.remove(instanceId);
                mockIaasServiceUtil.persistInRegistry((ConcurrentHashMap<String, MockInstance>) instanceIdToMockInstanceMap);

                if (getMemberCount(serviceName) == 0) {
                    MockHealthStatisticsGenerator.getInstance().stopStatisticsUpdaterTasks(serviceName);
                }

                log.info(String.format("Instance terminated successfully: [instance-id] %s", instanceId));
            } else {
                log.warn(String.format("Instance not found: [instance-id] %s", instanceId));
            }
        }
    }

    /**
     * Find number of instances available for service type.
     *
     * @param serviceName service name/cartridge type
     * @return the member count
     */
    private int getMemberCount(String serviceName) {
        int count = 0;
        for (Map.Entry<String, MockInstance> entry : instanceIdToMockInstanceMap.entrySet()) {
            if (serviceName.equals(entry.getValue().getMockInstanceContext().getServiceName())) {
                count++;
            }
        }
        return count;
    }
}
