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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.messaging.publisher.CartridgeInstanceDataPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.Template;

import java.util.Set;
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
            ClusterContext ctxt = CloudControllerContext.getInstance().getClusterContext(clusterId);
            Iaas iaas = iaasProvider.getIaas();
            String publicIp = null;

            NodeMetadata node = null;
            // generate the group id from domain name and sub domain name.
            // Should have lower-case ASCII letters, numbers, or dashes.
            // Should have a length between 3-15
            String str = clusterId.length() > 10 ? clusterId.substring(0, 10) : clusterId.substring(0, clusterId.length());
            String group = str.replaceAll("[^a-z0-9-]", "");

            try {
                ComputeService computeService = iaasProvider.getComputeService();
                Template template = iaasProvider.getTemplate();

                if (log.isDebugEnabled()) {
                    log.debug("Cloud Controller is delegating request to start an instance for "
                            + memberContext + " to Jclouds layer.");
                }
                // create and start a node
                Set<? extends NodeMetadata> nodes = computeService
                        .createNodesInGroup(group, 1, template);
                node = nodes.iterator().next();
                if (log.isDebugEnabled()) {
                    log.debug("Cloud Controller received a response for the request to start "
                            + memberContext + " from Jclouds layer.");
                }

                if (node == null) {
                    String msg = "Null response received for instance start-up request to Jclouds.\n"
                            + memberContext.toString();
                    log.error(msg);
                    throw new IllegalStateException(msg);
                }

                // node id
                String nodeId = node.getId();
                if (nodeId == null) {
                    String msg = "Node id of the starting instance is null.\n"
                            + memberContext.toString();
                    log.fatal(msg);
                    throw new IllegalStateException(msg);
                }

                memberContext.setNodeId(nodeId);
                if (log.isDebugEnabled()) {
                    log.debug("Node id was set. " + memberContext.toString());
                }

                // attach volumes
                if (ctxt.isVolumeRequired()) {
                    // remove region prefix
                    String instanceId = nodeId.indexOf('/') != -1 ? nodeId
                            .substring(nodeId.indexOf('/') + 1, nodeId.length())
                            : nodeId;
                    memberContext.setInstanceId(instanceId);
                    if (ctxt.getVolumes() != null) {
                        for (Volume volume : ctxt.getVolumes()) {
                            try {
                                iaas.attachVolume(instanceId, volume.getId(),
                                        volume.getDevice());
                            } catch (Exception e) {
                                // continue without throwing an exception, since
                                // there is an instance already running
                                log.error("Attaching Volume to Instance [ "
                                        + instanceId + " ] failed!", e);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                String msg = "Failed to start an instance. " + memberContext.toString() + " Cause: " + e.getMessage();
                log.error(msg, e);
                throw new IllegalStateException(msg, e);
            }

            try {
                if (log.isDebugEnabled()) {
                    log.debug("IP allocation process started for " + memberContext);
                }
                String autoAssignIpProp =
                        iaasProvider.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP_PROPERTY);

                String pre_defined_ip =
                        iaasProvider.getProperty(CloudControllerConstants.FLOATING_IP_PROPERTY);

                // reset ip
                String ip = "";

                // default behavior is autoIpAssign=false
                if (autoAssignIpProp == null ||
                        (autoAssignIpProp != null && autoAssignIpProp.equals("false"))) {

                    // check if floating ip is well defined in cartridge definition
                    if (pre_defined_ip != null) {
                        if (CloudControllerServiceUtil.isValidIpAddress(pre_defined_ip)) {
                            if (log.isDebugEnabled()) {
                                log.debug("CloudControllerServiceImpl:IpAllocator:pre_defined_ip: invoking associatePredefinedAddress" + pre_defined_ip);
                            }
                            ip = iaas.associatePredefinedAddress(node, pre_defined_ip);

                            if (ip == null || "".equals(ip) || !pre_defined_ip.equals(ip)) {
                                // throw exception and stop instance creation
                                String msg = "Error occurred while allocating predefined floating ip address: " + pre_defined_ip +
                                        " / allocated ip:" + ip +
                                        " - terminating node:" + memberContext.toString();
                                log.error(msg);
                                // terminate instance
                                CloudControllerServiceUtil.terminate(iaasProvider,
                                        node.getId(), memberContext);
                                throw new CloudControllerException(msg);
                            }
                        } else {
                            String msg = "Invalid floating ip address configured: " + pre_defined_ip +
                                    " - terminating node:" + memberContext.toString();
                            log.error(msg);
                            // terminate instance
                            CloudControllerServiceUtil.terminate(iaasProvider,
                                    node.getId(), memberContext);
                            throw new CloudControllerException(msg);
                        }

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("CloudControllerServiceImpl:IpAllocator:no (valid) predefined floating ip configured, "
                                    + "selecting available one from pool");
                        }
                        // allocate an IP address - manual IP assigning mode
                        ip = iaas.associateAddress(node);

                        if (ip != null) {
                            memberContext.setAllocatedIpAddress(ip);
                            if (log.isDebugEnabled()) {
                                log.debug("Allocated an ip address: "
                                        + memberContext.toString());
                            } else if (log.isInfoEnabled()) {
                                log.info("Allocated ip address [ " + memberContext.getAllocatedIpAddress() +
                                        " ] to member with id: " + memberContext.getMemberId());
                            }
                        }
                    }

                    if (ip == null) {
                        String msg = "No IP address found. IP allocation failed for " + memberContext;
                        log.error(msg);
                        throw new CloudControllerException(msg);
                    }

                    // build the node with the new ip
                    node = NodeMetadataBuilder.fromNodeMetadata(node)
                            .publicAddresses(ImmutableSet.of(ip)).build();
                }


                // public ip
                if (node.getPublicAddresses() != null &&
                        node.getPublicAddresses().iterator().hasNext()) {
                    ip = node.getPublicAddresses().iterator().next();
                    publicIp = ip;
                    memberContext.setPublicIpAddress(ip);
                    if (log.isDebugEnabled()) {
                        log.debug("Retrieving Public IP Address : " + memberContext.toString());
                    } else if (log.isInfoEnabled()) {
                        log.info("Retrieving Public IP Address: " + memberContext.getPublicIpAddress() +
                                ", member id: " + memberContext.getMemberId());
                    }
                }

                // private IP
                if (node.getPrivateAddresses() != null &&
                        node.getPrivateAddresses().iterator().hasNext()) {
                    ip = node.getPrivateAddresses().iterator().next();
                    memberContext.setPrivateIpAddress(ip);
                    if (log.isDebugEnabled()) {
                        log.debug("Retrieving Private IP Address. " + memberContext.toString());
                    } else if (log.isInfoEnabled()) {
                        log.info("Retrieving Private IP Address: " + memberContext.getPrivateIpAddress() +
                                ", member id: " + memberContext.getMemberId());
                    }
                }

                CloudControllerContext.getInstance().addMemberContext(memberContext);

                // persist in registry
                CloudControllerContext.getInstance().persist();


                // trigger topology
                TopologyBuilder.handleMemberSpawned(cartridgeType, clusterId,
                        partition.getId(), ip, publicIp, memberContext);

                String memberID = memberContext.getMemberId();

                // update the topology with the newly spawned member
                // publish data
                CartridgeInstanceDataPublisher.publish(memberID,
                        memberContext.getPartition().getId(),
                        memberContext.getNetworkPartitionId(),
                        memberContext.getClusterId(),
                        cartridgeType,
                        MemberStatus.Created.toString(),
                        node);
                if (log.isDebugEnabled()) {
                    log.debug("Node details: " + node.toString());
                }

                if (log.isDebugEnabled()) {
                    log.debug("IP allocation process ended for " + memberContext);
                }

            } catch (Exception e) {
                String msg = "Error occurred while allocating an ip address. " + memberContext.toString();
                log.error(msg, e);
                throw new CloudControllerException(msg, e);
            }
        } finally {
            if(lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }
}