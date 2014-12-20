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
import org.apache.stratos.cloud.controller.messaging.publisher.StatisticsDataPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.jclouds.compute.domain.NodeMetadata;

import java.util.concurrent.locks.Lock;

/**
 * Instance creator runnable.
 */
public class InstanceCreator implements Runnable {

    private static final Log log = LogFactory.getLog(InstanceCreator.class);

    private MemberContext memberContext;
    private IaasProvider iaasProvider;

    public InstanceCreator(MemberContext memberContext, IaasProvider iaasProvider) {
        this.memberContext = memberContext;
        this.iaasProvider = iaasProvider;
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

            // Create instance
            memberContext = createInstance(iaas, memberContext);

            if(log.isInfoEnabled()) {
                log.info(String.format("Instance is starting up: [cartridge-type] %s [cluster-id] %s [instance-id] %s"
                        , memberContext.getCartridgeType(), memberContext.getClusterId(), memberContext.getInstanceId()));
            }

            // Attach volumes
            attachVolumes(iaas, clusterContext, memberContext);

            // Allocate IP address
            iaas.allocateIpAddress(clusterId, memberContext, partition);


            // Update topology
            TopologyBuilder.handleMemberSpawned(memberContext);

            // Publish instance creation statistics to BAM
            StatisticsDataPublisher.publish(
                    memberContext.getMemberId(),
                    memberContext.getPartition().getId(),
                    memberContext.getNetworkPartitionId(),
                    memberContext.getClusterId(),
                    memberContext.getCartridgeType(),
                    MemberStatus.Created.toString(),
                    memberContext.getInstanceMetadata());
        } catch (Exception e) {
            String message = String.format("Could not start instance: [cartridge-type] %s [cluster-id] %s",
                    memberContext.getCartridgeType(), memberContext.getClusterId());
            log.error(message, e);
        } finally {
            if(lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private MemberContext createInstance(Iaas iaas, MemberContext memberContext) throws CartridgeNotFoundException {
        memberContext = iaas.createInstance(memberContext);

        // Validate node id
        String instanceId = memberContext.getInstanceId();
        if (StringUtils.isBlank(instanceId)) {
            String msg = "Instance id of the starting instance is null\n" + memberContext.toString();
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // Update member context and persist changes
        CloudControllerContext.getInstance().updateMemberContext(memberContext);
        CloudControllerContext.getInstance().persist();

        if (log.isDebugEnabled()) {
            log.debug("Instance created: [member-context] " + memberContext.toString());
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
                        log.error("Attaching volume to instance [ "
                                + memberContext.getInstanceId() + " ] failed!", e);
                    }
                }
            }
        }
    }
}