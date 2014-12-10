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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
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
    private String cartridgeType;

    public InstanceCreator(MemberContext memberContext, IaasProvider iaasProvider,
                           String cartridgeType) {
        this.memberContext = memberContext;
        this.iaasProvider = iaasProvider;
        this.cartridgeType = cartridgeType;
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
            NodeMetadata node = createInstance(iaas, clusterContext, memberContext);

            // Attach volumes
            attachVolumes(iaas, clusterContext, memberContext);

            // Allocate IP address
            iaas.allocateIpAddress(clusterId, memberContext, partition, cartridgeType, node);


            // Update topology
            TopologyBuilder.handleMemberSpawned(cartridgeType, clusterId,
                    partition.getId(), memberContext.getPrivateIpAddress(), memberContext.getPublicIpAddress(),
                    memberContext);

            // Publish instance creation statistics to BAM
            StatisticsDataPublisher.publish(memberContext.getMemberId(),
                    memberContext.getPartition().getId(),
                    memberContext.getNetworkPartitionId(),
                    memberContext.getClusterId(),
                    cartridgeType,
                    MemberStatus.Created.toString(),
                    node);
        } finally {
            if(lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private NodeMetadata createInstance(Iaas iaas, ClusterContext clusterContext, MemberContext memberContext) {
        NodeMetadata node = iaas.createInstance(clusterContext, memberContext);

        // node id
        String nodeId = node.getId();
        if (nodeId == null) {
            String msg = "Node id of the starting instance is null\n" + memberContext.toString();
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        memberContext.setNodeId(nodeId);
        CloudControllerContext.getInstance().updateMemberContext(memberContext);
        CloudControllerContext.getInstance().persist();

        if (log.isDebugEnabled()) {
            log.debug("Instance created: [node-metadata] " + node.toString());
        }
        return node;
    }

    public void attachVolumes(Iaas iaas, ClusterContext clusterContext, MemberContext memberContext) {
        // attach volumes
        if (clusterContext.isVolumeRequired()) {
            // remove region prefix
            String nodeId = memberContext.getNodeId();
            String instanceId = nodeId.indexOf('/') != -1 ? nodeId
                    .substring(nodeId.indexOf('/') + 1, nodeId.length())
                    : nodeId;
            memberContext.setInstanceId(instanceId);
            if (clusterContext.getVolumes() != null) {
                for (Volume volume : clusterContext.getVolumes()) {
                    try {
                        iaas.attachVolume(instanceId, volume.getId(), volume.getDevice());
                    } catch (Exception e) {
                        // continue without throwing an exception, since
                        // there is an instance already running
                        log.error("Attaching volume to instance [ "
                                + instanceId + " ] failed!", e);
                    }
                }
            }
        }
    }
}