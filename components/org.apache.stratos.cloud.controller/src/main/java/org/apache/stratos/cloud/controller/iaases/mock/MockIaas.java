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

import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.iaases.mock.service.MockIaasService;

/**
 * Mock IaaS client for invoking mock IaaS service.
 */
public class MockIaas extends Iaas {

    public MockIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
    }

    @Override
    public void initialize() {
    }

    @Override
    public MemberContext startInstance(MemberContext memberContext) {
        return MockIaasService.getInstance().createInstance(memberContext);
    }

    @Override
    public void releaseAddress(String ip) {
        MockIaasService.getInstance().releaseAddress(ip);
    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
        return MockIaasService.getInstance().isValidRegion(region);
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException, InvalidRegionException {
        return MockIaasService.getInstance().isValidZone(region, zone);
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        return MockIaasService.getInstance().isValidHost(zone, host);
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return MockIaasService.getInstance().getPartitionValidator();
    }

    @Override
    public String createVolume(int sizeGB, String snapshotId) {
        return MockIaasService.getInstance().createVolume(sizeGB, snapshotId);
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        return MockIaasService.getInstance().attachVolume(instanceId, volumeId, deviceName);
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {
        MockIaasService.getInstance().detachVolume(instanceId, volumeId);
    }

    @Override
    public void deleteVolume(String volumeId) {
        MockIaasService.getInstance().deleteVolume(volumeId);
    }

    @Override
    public String getIaasDevice(String device) {
        return MockIaasService.getInstance().getIaasDevice(device);
    }

    @Override
    public void allocateIpAddresses(String clusterId, MemberContext memberContext, Partition partition) {
        MockIaasService.getInstance().allocateIpAddress(clusterId, memberContext, partition);
    }

    @Override
    public void setDynamicPayload(byte[] payload) {
        MockIaasService.getInstance().setDynamicPayload(payload);
    }

    @Override
    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException, InvalidMemberException {
        MockIaasService.getInstance().terminateInstance(memberContext);
    }
}
