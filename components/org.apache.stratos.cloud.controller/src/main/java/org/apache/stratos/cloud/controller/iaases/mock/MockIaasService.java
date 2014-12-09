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

package org.apache.stratos.cloud.controller.iaases.mock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.validators.PartitionValidator;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.jclouds.compute.domain.NodeMetadata;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

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
    private static final String MOCK_IAAS_MEMBERS = "/mock/iaas/members";
    private static volatile MockIaasService instance;

    private MockPartitionValidator partitionValidator;
    private ConcurrentHashMap<String, MockMember> membersMap;

    private MockIaasService() {
        super();
        partitionValidator = new MockPartitionValidator();
        membersMap = readFromRegistry();
        if(membersMap == null) {
            // No members found in registry, create a new map
            membersMap = new ConcurrentHashMap<String, MockMember>();
        }
    }

    public static MockIaasService getInstance() {
        if (instance == null) {
            synchronized (MockIaasService.class) {
                if (instance == null) {
                    instance = new MockIaasService();
                }
            }
        }
        return instance;
    }

    /**
     * Start mock members if present in registry
     */
    public static void startMockMembersIfPresentInRegistry() {
        ConcurrentHashMap<String, MockMember> membersMap = readFromRegistry();
        if(membersMap != null) {
            ExecutorService executorService = StratosThreadPool.getExecutorService("MOCK_IAAS_THREAD_EXECUTOR", 100);
            for (MockMember mockMember : membersMap.values()) {
                executorService.submit(mockMember);
            }
        }
    }

    public NodeMetadata createInstance(ClusterContext clusterContext, MemberContext memberContext) {
        synchronized (MockIaasService.class) {
            // Create mock member instance
            MockMemberContext mockMemberContext = new MockMemberContext(clusterContext.getCartridgeType(),
                    clusterContext.getClusterId(), memberContext.getMemberId(), memberContext.getNetworkPartitionId(),
                    memberContext.getPartition().getId(), memberContext.getInstanceId());
            MockMember mockMember = new MockMember(mockMemberContext);
            membersMap.put(mockMember.getMockMemberContext().getMemberId(), mockMember);
            mockMemberExecutorService.submit(mockMember);

            // Prepare node metadata
            MockNodeMetadata nodeMetadata = new MockNodeMetadata();
            nodeMetadata.setId(UUID.randomUUID().toString());

            // Persist changes
            persistInRegistry();

            return nodeMetadata;
        }
    }

    private void persistInRegistry() {
        try {
            RegistryManager.getInstance().persist(MOCK_IAAS_MEMBERS, membersMap);
        } catch (RegistryException e) {
            log.error("Could not persist mock iaas members in registry", e);
        }
    }

    private static ConcurrentHashMap<String, MockMember> readFromRegistry() {
        return (ConcurrentHashMap<String, MockMember>) RegistryManager.getInstance().read(MOCK_IAAS_MEMBERS);
    }

    public void allocateIpAddress(String clusterId, MemberContext memberContext, Partition partition,
                                  String cartridgeType, NodeMetadata node) {
        // Allocate mock ip addresses
        memberContext.setPrivateIpAddress(MockIPAddressPool.getInstance().getNextPrivateIpAddress());
        memberContext.setPublicIpAddress(MockIPAddressPool.getInstance().getNextPublicIpAddress());
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
            MockMember mockMember = membersMap.get(memberContext.getMemberId());
            if (mockMember != null) {
                //updating the topology
                TopologyBuilder.handleMemberTerminated(memberContext.getCartridgeType(),
                        memberContext.getClusterId(), memberContext.getNetworkPartitionId(),
                        memberContext.getPartition().getId(), memberContext.getMemberId());

                mockMember.terminate();
                membersMap.remove(memberContext.getMemberId());
            }
        }
    }
}
