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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.mock.iaas.client.MockIaasApiClient;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock IaaS client for invoking mock IaaS service.
 */
public class MockIaas extends Iaas {
    private static final Log log = LogFactory.getLog(MockIaas.class);
    private MockIaasApiClient apiClient;
    private PartitionValidator partitionValidator;
    private Map<String, String> payloadMap;
    private static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    private static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";

    public MockIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
        String endpoint = iaasProvider.getProperty("api.endpoint");
        if (StringUtils.isBlank(endpoint)) {
            throw new CloudControllerException(
                    "api.endpoint property not found in mock iaas provider in" + "cloud-controller.xml file");
        }
        apiClient = new MockIaasApiClient(endpoint);
        partitionValidator = new MockIaasPartitionValidator();
        payloadMap = new HashMap<>();
    }

    @Override
    public void initialize() {
    }

    @Override
    public MemberContext startInstance(MemberContext memberContext, byte[] payloadByteArray) {
        MockInstanceContext mockInstanceContext = new MockInstanceContext(memberContext.getApplicationId(),
                memberContext.getCartridgeType(), memberContext.getClusterId(), memberContext.getMemberId(),
                memberContext.getClusterInstanceId(), memberContext.getNetworkPartitionId(),
                memberContext.getPartition().getId());
        setDynamicPayload(payloadByteArray);
        mockInstanceContext.setPayload(new String(payloadByteArray));
        MockInstanceMetadata mockInstanceMetadata = apiClient.startInstance(mockInstanceContext);
        memberContext.setInstanceId(mockInstanceMetadata.getInstanceId());
        return memberContext;
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
    public void allocateIpAddresses(String clusterId, MemberContext memberContext, Partition partition) {
        MockInstanceMetadata mockInstanceMetadata = apiClient.allocateIpAddress(memberContext.getInstanceId());
        if (mockInstanceMetadata != null) {
            memberContext.setDefaultPrivateIP(mockInstanceMetadata.getDefaultPrivateIp());
            memberContext.setDefaultPublicIP(mockInstanceMetadata.getDefaultPublicIp());

            String[] privateIPs = new String[] { mockInstanceMetadata.getDefaultPrivateIp() };
            String[] publicIPs = new String[] { mockInstanceMetadata.getDefaultPublicIp() };
            memberContext.setPrivateIPs(privateIPs);
            memberContext.setPublicIPs(publicIPs);
        }
    }

    @Override
    public void setDynamicPayload(byte[] payloadByteArray) {
        // Clear existing payloadMap parameters
        payloadMap.clear();

        if (payloadByteArray != null) {
            String payloadString = new String(payloadByteArray);
            String[] parameterArray = payloadString.split(PAYLOAD_PARAMETER_SEPARATOR);
            if (parameterArray.length > 0) {
                for (String parameter : parameterArray) {
                    if (parameter != null) {
                        String[] nameValueArray = parameter.split(PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR, 2);
                        if ((nameValueArray.length == 2)) {
                            payloadMap.put(nameValueArray[0], nameValueArray[1]);
                        }
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Dynamic payloadMap is set: " + payloadMap.values());
                }
            }
        }
    }

    @Override
    public void terminateInstance(MemberContext memberContext)
            throws InvalidCartridgeTypeException, InvalidMemberException {
        boolean terminated = apiClient.terminateInstance(memberContext.getInstanceId());
        if (terminated) {
            log.info("Mock instance terminated via MockIaaSApiClient [member-id] " + memberContext.getMemberId());
        }
    }
}
