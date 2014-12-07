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
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.validators.PartitionValidator;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.jclouds.compute.domain.NodeMetadata;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Created by imesh on 12/6/14.
 */
public class MockIaas extends Iaas {

    private static final Log log = LogFactory.getLog(MockIaas.class);
    private static final String MOCK_IAAS_MEMBERS = "/mock/iaas/members";

    private ExecutorService executorService;
    private MockPartitionValidator partitionValidator;
    private ConcurrentHashMap<String, MockMember> membersMap;

    public MockIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
        executorService = StratosThreadPool.getExecutorService("MOCK_IAAS_THREAD_EXECUTOR", 100);
        partitionValidator = new MockPartitionValidator();
        membersMap = readFromRegistry();
        if(membersMap != null) {
            // Start existing members
            for(MockMember mockMember : membersMap.values()) {
                executorService.submit(mockMember);
            }
        } else {
            // No members found in registry, create new map
            membersMap = new ConcurrentHashMap<String, MockMember>();
        }
    }

    @Override
    public void initialize() {
    }

    @Override
    public NodeMetadata createInstance(ClusterContext clusterContext, MemberContext memberContext) {
        // Create mock member instance
        MockMemberContext mockMemberContext = new MockMemberContext(clusterContext.getCartridgeType(),
                clusterContext.getClusterId(), memberContext.getMemberId(), memberContext.getNetworkPartitionId(),
                memberContext.getPartition().getId(), memberContext.getInstanceId());
        MockMember mockMember = new MockMember(mockMemberContext);
        membersMap.put(mockMember.getMockMemberContext().getMemberId(), mockMember);
        executorService.submit(mockMember);

        // Prepare node metadata
        MockNodeMetadata nodeMetadata = new MockNodeMetadata();
        nodeMetadata.setId(UUID.randomUUID().toString());

        // Persist changes
        persistInRegistry();

        return nodeMetadata;
    }

    private void persistInRegistry() {
        try {
            RegistryManager.getInstance().persist(MOCK_IAAS_MEMBERS, membersMap);
        } catch (RegistryException e) {
            log.error("Could not persist mock iaas members in registry", e);
        };
    }

    private ConcurrentHashMap<String, MockMember> readFromRegistry() {
        return (ConcurrentHashMap<String, MockMember>) RegistryManager.getInstance().read(MOCK_IAAS_MEMBERS);
    }

    @Override
    public void allocateIpAddress(String clusterId, MemberContext memberContext, Partition partition,
                                  String cartridgeType, NodeMetadata node) {
        // Allocate mock ip addresses
        memberContext.setPrivateIpAddress(MockIPAddressPool.getInstance().getNextPrivateIpAddress());
        memberContext.setPublicIpAddress(MockIPAddressPool.getInstance().getNextPublicIpAddress());
    }

    @Override
    public void releaseAddress(String ip) {

    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
        return true;
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException, InvalidRegionException {
        return true;
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        return true;
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return partitionValidator;
    }

    @Override
    public String createVolume(int sizeGB, String snapshotId) {
        return null;
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        return null;
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {

    }

    @Override
    public void deleteVolume(String volumeId) {

    }

    @Override
    public String getIaasDevice(String device) {
        return null;
    }

    @Override
    public void setDynamicPayload(byte[] payload) {

    }

    @Override
    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException, InvalidMemberException {
        MockMember mockMember = membersMap.get(memberContext.getMemberId());
        if(mockMember != null) {
            mockMember.terminate();
            membersMap.remove(memberContext.getMemberId());
        }
    }
}
