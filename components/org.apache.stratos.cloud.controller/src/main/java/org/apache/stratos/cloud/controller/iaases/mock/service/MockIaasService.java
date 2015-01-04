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

package org.apache.stratos.cloud.controller.iaases.mock.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.mock.MockPartitionValidator;
import org.apache.stratos.cloud.controller.iaases.mock.service.config.MockIaasConfig;
import org.apache.stratos.cloud.controller.iaases.mock.service.statistics.generator.MockHealthStatisticsGenerator;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.common.registry.RegistryManager;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Mock IaaS service implementation. This is a singleton class that simulates a standard Infrastructure as a Service
 * platform by creating mock members and managing their lifecycle states.
 *
 * How does this work:
 * - Mock IaaS starts a Mock Member thread or each instance created
 * - A sample private IP and a public IP will be assigned to the instance
 * - Mock Member will publish Instance Started and Instance Activated events once the thread is started
 * - Afterwards it will start publishing sample health statistics values to CEP
 * - If the Mock IaaS was asked to terminate an instance it will stop the relevant thread
 */
public class MockIaasService {

    private static final Log log = LogFactory.getLog(MockIaasService.class);

    private static final ExecutorService mockMemberExecutorService =
            StratosThreadPool.getExecutorService("MOCK_MEMBER_EXECUTOR_SERVICE", MockConstants.MAX_MOCK_MEMBER_COUNT);
    private static final String MOCK_IAAS_MEMBERS = "/cloud.controller/mock/iaas/members";
    private static volatile MockIaasService instance;

    private MockPartitionValidator partitionValidator;
    // Map<ServiceName, Map<MemberId,MockMember>>
    private Map<String, Map<String, MockMember>> serviceNameToMockMemberMap;

    private MockIaasService() {
        super();
        partitionValidator = new MockPartitionValidator();
        try {
            serviceNameToMockMemberMap = readFromRegistry();
        } catch (RegistryException e) {
            String message = "Could not read service name -> mock member map from registry";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        if(serviceNameToMockMemberMap == null) {
            // No members found in registry, create a new map
            serviceNameToMockMemberMap = new ConcurrentHashMap<String, Map<String, MockMember>>();
        }
    }

    public static MockIaasService getInstance() {
        if (instance == null) {
            synchronized (MockIaasService.class) {
                if (instance == null) {
                    if(!MockIaasConfig.getInstance().isEnabled()) {
                        throw new RuntimeException("Mock IaaS is not enabled");
                    }
                    instance = new MockIaasService();
                }
            }
        }
        return instance;
    }

    /**
     * Start mock members
     */
    public void startMockMembers() {
        if(serviceNameToMockMemberMap != null) {
            for(Map.Entry<String, Map<String, MockMember>> serviceNameEntry : serviceNameToMockMemberMap.entrySet())  {
                // Start mock members
                for(Map.Entry<String, MockMember> memberEntry : serviceNameEntry.getValue().entrySet()) {
                    mockMemberExecutorService.submit(memberEntry.getValue());
                }

                // Schedule statistics updater tasks for service
                if(serviceNameEntry.getValue().entrySet().size() > 0) {
                    MockHealthStatisticsGenerator.getInstance().scheduleStatisticsUpdaterTasks(serviceNameEntry.getKey());
                }
            }
        }
    }

    public MemberContext createInstance(MemberContext memberContext) {
        synchronized (MockIaasService.class) {
            // Create mock member instance
            MockMemberContext mockMemberContext = new MockMemberContext(
                    memberContext.getApplicationId(), memberContext.getCartridgeType(),
                    memberContext.getClusterId(), memberContext.getMemberId(), memberContext.getInstanceId(),
                    memberContext.getClusterInstanceId(), memberContext.getNetworkPartitionId(),
                    memberContext.getPartition().getId());

            MockMember mockMember = new MockMember(mockMemberContext);
            addMemberToMap(mockMember);
            mockMemberExecutorService.submit(mockMember);

            // Generate instance id
            memberContext.setInstanceId(UUID.randomUUID().toString());

            // Persist changes
            persistInRegistry();

            String serviceName = mockMemberContext.getServiceName();
            MockHealthStatisticsGenerator.getInstance().scheduleStatisticsUpdaterTasks(serviceName);

            return memberContext;
        }
    }

    private void addMemberToMap(MockMember mockMember) {
        String serviceName = mockMember.getMockMemberContext().getServiceName();
        Map<String, MockMember> memberMap = serviceNameToMockMemberMap.get(serviceName);
        if(memberMap == null) {
            memberMap = new ConcurrentHashMap<String, MockMember>();
            serviceNameToMockMemberMap.put(serviceName, memberMap);
        }
        memberMap.put(mockMember.getMockMemberContext().getMemberId(), mockMember);
    }

    private void persistInRegistry() {
        try {
            RegistryManager.getInstance().persist(MOCK_IAAS_MEMBERS,
                    (ConcurrentHashMap<String, Map<String, MockMember>>)serviceNameToMockMemberMap);
        } catch (RegistryException e) {
            log.error("Could not persist mock iaas members in registry", e);
        }
    }

    private ConcurrentHashMap<String, Map<String, MockMember>> readFromRegistry() throws RegistryException {
        return (ConcurrentHashMap<String, Map<String, MockMember>>)
                RegistryManager.getInstance().read(MOCK_IAAS_MEMBERS);
    }

    public void allocateIpAddress(String clusterId, MemberContext memberContext, Partition partition) {
        // Allocate mock ip addresses
        memberContext.setDefaultPrivateIP(MockIPAddressPool.getInstance().getNextPrivateIpAddress());
        memberContext.setDefaultPublicIP(MockIPAddressPool.getInstance().getNextPublicIpAddress());
    }

    public void releaseAddress(String ip) {

    }

    public boolean isValidRegion(String region) throws InvalidRegionException {
        return true;
    }

    public boolean isValidZone(String region, String zone) throws InvalidZoneException, InvalidRegionException {
        return true;
    }

    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        return true;
    }

    public PartitionValidator getPartitionValidator() {
        return partitionValidator;
    }

    public String createVolume(int sizeGB, String snapshotId) {
        return null;
    }

    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        return null;
    }

    public void detachVolume(String instanceId, String volumeId) {

    }

    public void deleteVolume(String volumeId) {

    }

    public String getIaasDevice(String device) {
        return null;
    }

    public void setDynamicPayload(byte[] payload) {

    }

    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException, InvalidMemberException {
        synchronized (MockIaasService.class) {
            String serviceName = memberContext.getCartridgeType();
            Map<String, MockMember> memberMap = serviceNameToMockMemberMap.get(serviceName);
            if(memberMap != null) {
                MockMember mockMember = memberMap.get(memberContext.getMemberId());
                if(mockMember != null) {
                    if (mockMember != null) {
                        mockMember.terminate();
                        memberMap.remove(memberContext.getMemberId());
                    }

                    if (memberMap.size() == 0) {
                        MockHealthStatisticsGenerator.getInstance().stopStatisticsUpdaterTasks(serviceName);
                    }
                }
            }
        }
    }
}
