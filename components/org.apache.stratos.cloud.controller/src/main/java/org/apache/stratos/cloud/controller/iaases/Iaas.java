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
package org.apache.stratos.cloud.controller.iaases;

import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.*;

/**
 * All IaaSes that are going to support by Cloud Controller, should extend this abstract class.
 */
public abstract class Iaas {
    /**
     * Reference to the corresponding {@link IaasProvider}
     */
    private IaasProvider iaasProvider;

    public Iaas(IaasProvider iaasProvider) {
        this.setIaasProvider(iaasProvider);
    }

    public IaasProvider getIaasProvider() {
        return iaasProvider;
    }

    /**
     * Set iaas provider.
     * @param iaasProvider
     */
    public void setIaasProvider(IaasProvider iaasProvider) {
        this.iaasProvider = iaasProvider;
    }

    /**
     * Initialize the iaas object.
     */
    public abstract void initialize();

    /**
     * Create vm/container instance.
     *
     * @param memberContext
     * @param payload
     * @return updated memberContext
     */
    public abstract MemberContext startInstance(MemberContext memberContext, byte[] payload) throws CartridgeNotFoundException;

    /**
     * This will deallocate/release the given IP address back to pool.
     *
     * @param ip public IP address to be released.
     */
    public abstract void releaseAddress(String ip);

    /**
     * Validate a given region name against a particular IaaS.
     * If a particular IaaS doesn't have a concept called region, it can simply throw {@link InvalidRegionException}.
     *
     * @param region name of the region.
     * @return whether the region is valid.
     * @throws InvalidRegionException if the region is invalid.
     */
    public abstract boolean isValidRegion(String region) throws InvalidRegionException;

    /**
     * Validate a given zone name against a particular region in an IaaS.
     * If a particular IaaS doesn't have a concept called zone, it can simply throw {@link InvalidZoneException}.
     *
     * @param region region of the IaaS that the zone belongs to.
     * @param zone
     * @return whether the zone is valid in the given region or not.
     * @throws InvalidZoneException if the zone is invalid in a given region.
     */
    public abstract boolean isValidZone(String region, String zone) throws InvalidZoneException, InvalidRegionException;

    /**
     * Validate a given host id against a particular zone in an IaaS.
     * If a particular IaaS doesn't have a concept called hosts, it can simply throw {@link InvalidHostException}.
     *
     * @param zone zone of the IaaS that the host belongs to.
     * @param host
     * @return whether the host is valid in the given zone or not.
     * @throws InvalidHostException if the host is invalid in a given zone.
     */
    public abstract boolean isValidHost(String zone, String host) throws InvalidHostException;

    /**
     * provides the {@link PartitionValidator} corresponds to this particular IaaS.
     *
     * @return {@link PartitionValidator}
     */
    public abstract PartitionValidator getPartitionValidator();

    /**
     * Create a new volume in the respective Iaas.
     *
     * @param sizeGB size of the volume in Giga Bytes.
     * @return Id of the created volume.
     */
    public abstract String createVolume(int sizeGB, String snapshotId);


    /**
     * Attach a given volume to an instance at the specified device path.
     *
     * @param instanceId of the instance.
     * @param volumeId   volume id of the volume to be attached.
     * @param deviceName name of the device that the volume would bind to.
     * @return the status of the attachment.
     */
    public abstract String attachVolume(String instanceId, String volumeId, String deviceName);

    /**
     * Detach a given volume from the given instance.
     *
     * @param instanceId of the instance.
     * @param volumeId   volume id of the volume to be detached.
     */
    public abstract void detachVolume(String instanceId, String volumeId);

    /**
     * Delete a given volume.
     *
     * @param volumeId volume id of the volume to be detached.
     */
    public abstract void deleteVolume(String volumeId);

    /**
     * This returns the device of the volume specified by the user. This is depends on IAAS.
     * For an instance /dev/sdf maps to /dev/xvdf in EC2.
     */
    public abstract String getIaasDevice(String device);

    /**
     * Allocates ip addresses to member.
     * @param clusterId
     * @param memberContext
     * @param partition
     */
    public abstract void allocateIpAddresses(String clusterId, MemberContext memberContext, Partition partition);

    /**
     * This method provides a way to set payload.
     */
    public abstract void setDynamicPayload(byte[] payload);

    /**
     * Terminate an instance.
     * @param memberContext
     * @throws InvalidCartridgeTypeException
     * @throws InvalidMemberException
     */
    public abstract void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException, InvalidMemberException, MemberTerminationFailedException;
}
