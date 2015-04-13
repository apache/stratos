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
import org.apache.stratos.mock.iaas.persistence.PersistenceManager;
import org.apache.stratos.mock.iaas.statistics.generator.MockHealthStatisticsGenerator;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Mock iaas service util.
 */
public class MockIaasServiceUtil {

    private static final Log log = LogFactory.getLog(MockIaasServiceUtil.class);
    private static final String MOCK_IAAS_MEMBERS = "/mock.iaas/instances";

    private PersistenceManager persistenceManager;

    public MockIaasServiceUtil(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void persistInRegistry(ConcurrentHashMap<String, MockInstance> instanceIdToMockMemberMap) {
        try {
            persistenceManager.persist(MOCK_IAAS_MEMBERS, instanceIdToMockMemberMap);
        } catch (RegistryException e) {
            log.error("Could not persist mock iaas instances in registry", e);
        }
    }

    public ConcurrentHashMap<String, MockInstance> readFromRegistry() throws RegistryException {
        return (ConcurrentHashMap<String, MockInstance>) persistenceManager.read(MOCK_IAAS_MEMBERS);
    }

    public void startInstancesPersisted() throws RegistryException {
        Map<String, MockInstance> instanceIdToMockMemberMap = readFromRegistry();
        ExecutorService mockMemberExecutorService =
                StratosThreadPool.getExecutorService(MockConstants.MOCK_MEMBER_THREAD_POOL,
                        MockConstants.MOCK_MEMBER_THREAD_POOL_SIZE);

        if (instanceIdToMockMemberMap != null) {
            log.info("Starting mock instances persisted...");

            Set<String> serviceNameSet = new HashSet<String>();
            for (MockInstance mockInstance : instanceIdToMockMemberMap.values()) {
                mockMemberExecutorService.submit(mockInstance);

                // Schedule statistics updater tasks for service
                String serviceName = mockInstance.getMockInstanceContext().getServiceName();
                if (!serviceNameSet.contains(serviceName)) {
                    MockHealthStatisticsGenerator.getInstance().scheduleStatisticsUpdaterTasks(serviceName);
                    serviceNameSet.add(serviceName);
                }
            }
        }
    }
}
