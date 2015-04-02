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

package org.apache.stratos.cloud.controller.services.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.CartridgeNotFoundException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.statistics.publisher.BAMUsageDataPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.messaging.domain.topology.MemberStatus;

import java.util.concurrent.locks.Lock;

/**
 * Instance creator runnable.
 */
public class InstanceCreator implements Runnable {

    private static final Log log = LogFactory.getLog(InstanceCreator.class);

    private MemberContext memberContext;
    private IaasProvider iaasProvider;
    private byte[] payload;

    public InstanceCreator(MemberContext memberContext, IaasProvider iaasProvider, byte[] payload) {
        this.memberContext = memberContext;
        this.iaasProvider = iaasProvider;
        this.payload = payload;
    }

    @Override
    public void run() {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();

            String clusterId = memberContext.getClusterId();
            Partition partition = memberContext.getPartition();
            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            Iaas iaas = iaasProvider.getIaas();

            if(log.isDebugEnabled()){

                log.debug(String.format("Payload passed to instance created, [member] %s [payload] %s",
                        memberContext.getMemberId(),  new String(payload)));
            }
            memberContext = startInstance(iaas, memberContext, payload);

            if (log.isInfoEnabled()) {
                log.info(String.format("Instance started successfully: [cartridge-type] %s [cluster-id] %s [instance-id] %s " +
                                "[default-private-ip] %s [default-public-ip] %s",
                        memberContext.getCartridgeType(), memberContext.getClusterId(),
                        memberContext.getInstanceId(), memberContext.getDefaultPrivateIP(),
                        memberContext.getDefaultPublicIP()));
            }

            if(clusterContext.isVolumeRequired()) {
                attachVolumes(iaas, clusterContext, memberContext);
            }

            // Allocate IP addresses
            iaas.allocateIpAddresses(clusterId, memberContext, partition);

            // Update topology
            TopologyBuilder.handleMemberInitializedEvent(memberContext);

            // Publish instance creation statistics to BAM
            BAMUsageDataPublisher.publish(
                    memberContext.getMemberId(),
                    memberContext.getPartition().getId(),
                    memberContext.getNetworkPartitionId(),
                    memberContext.getClusterId(),
                    memberContext.getCartridgeType(),
                    MemberStatus.Initialized.toString(),
                    memberContext.getInstanceMetadata());
        } catch (Exception e) {
            String message = String.format("Could not start instance: [cartridge-type] %s [cluster-id] %s",
                    memberContext.getCartridgeType(), memberContext.getClusterId());
            log.error(message, e);
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private MemberContext startInstance(Iaas iaas, MemberContext memberContext, byte[] payload) throws CartridgeNotFoundException {
        memberContext = iaas.startInstance(memberContext, payload);

        // Validate instance id
        String instanceId = memberContext.getInstanceId();
        if (StringUtils.isBlank(instanceId)) {
            String msg = String.format("Instance id not found in started member: [cartridge-type] %s [member-id] %s",
                    memberContext.getCartridgeType(), memberContext.getMemberId());
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // Update member context and persist changes
        CloudControllerContext.getInstance().updateMemberContext(memberContext);
        CloudControllerContext.getInstance().persist();

        if(log.isDebugEnabled()) {
            log.debug(String.format("Member context updated: [application] %s [cartridge] %s [member] %s",
                    memberContext.getApplicationId(), memberContext.getCartridgeType(), memberContext.getMemberId()));
        }

        return memberContext;
    }

    public void attachVolumes(Iaas iaas, ClusterContext clusterContext, MemberContext memberContext) {
        // attach volumes
        if (clusterContext.isVolumeRequired()) {
            // remove region prefix
            if (clusterContext.getVolumes() != null) {
                for (Volume volume : clusterContext.getVolumes()) {
                    try {
                        iaas.attachVolume(memberContext.getInstanceId(), volume.getId(), volume.getDevice());
                    } catch (Exception e) {
                        // continue without throwing an exception, since
                        // there is an instance already running
                        log.error(String.format("Could not attach volume, [instance] %s [volume] %s ",
                                memberContext.getInstanceId(), volume.toString()), e);
                    }
                }
            }
        }
    }
}