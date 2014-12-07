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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.messaging.publisher.CartridgeInstanceDataPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.Template;

import java.util.Set;

/**
 * An abstraction for defining jclouds IaaS features.
 */
public abstract class JcloudsIaas extends Iaas {

    private static final Log log = LogFactory.getLog(JcloudsIaas.class);

    public JcloudsIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
    }

    /**
     * This should build the {@link org.jclouds.compute.ComputeService} object and the {@link org.jclouds.compute.domain.Template} object,
     * using the information from {@link org.apache.stratos.cloud.controller.domain.IaasProvider} and should set the built
     * {@link org.jclouds.compute.ComputeService} object in the {@link org.apache.stratos.cloud.controller.domain.IaasProvider#setComputeService(org.jclouds.compute.ComputeService)}
     * and also should set the built {@link org.jclouds.compute.domain.Template} object in the
     * {@link org.apache.stratos.cloud.controller.domain.IaasProvider#setTemplate(org.jclouds.compute.domain.Template)}.
     */
    public abstract void buildComputeServiceAndTemplate();

    /**
     * Builds only the jclouds {@link org.jclouds.compute.domain.Template}
     */
    public abstract void buildTemplate();

    /**
     * This method should create a Key Pair corresponds to a given public key in the respective region having the name given.
     * Also should override the value of the key pair in the {@link org.jclouds.compute.domain.Template} of this IaaS.
     * @param region region that the key pair will get created.
     * @param keyPairName name of the key pair. NOTE: Jclouds adds a prefix : <code>jclouds#</code>
     * @param publicKey public key, from which the key pair will be created.
     * @return whether the key pair creation is successful or not.
     */
    public abstract boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey);

    /**
     * This will obtain an IP address from the allocated list and associate that IP with this node.
     * @param node Node to be associated with an IP.
     * @return associated public IP.
     */
    public abstract String associateAddress(NodeMetadata node);

    /**
     * This will obtain a predefined IP address and associate that IP with this node, if ip is already in use allocate ip from pool
     * (through associateAddress())
     * @param node Node to be associated with an IP.
     * @ip preallocated floating Ip
     * @return associated public IP.
     */
    public abstract String associatePredefinedAddress(NodeMetadata node, String ip);

    @Override
    public void initialize() {
        try {
            JcloudsIaasUtil.buildComputeServiceAndTemplate(getIaasProvider());
        } catch (InvalidIaasProviderException e) {
            log.error("Could not initialize jclouds IaaS", e);
        }
    }

    @Override
    public NodeMetadata createInstance(ClusterContext clusterContext, MemberContext memberContext) {
        NodeMetadata node = null;
        // generate the group id from domain name and sub domain name.
        // Should have lower-case ASCII letters, numbers, or dashes.
        // Should have a length between 3-15
        String clusterId = clusterContext.getClusterId();
        String str = clusterId.length() > 10 ? clusterId.substring(0, 10) : clusterId.substring(0, clusterId.length());
        String group = str.replaceAll("[^a-z0-9-]", "");

        try {
            ComputeService computeService = getIaasProvider().getComputeService();
            Template template = getIaasProvider().getTemplate();

            if (template == null) {
                String msg = "Could not start an instance, jclouds template is null for iaas provider [type]: " +
                        getIaasProvider().getType();
                log.error(msg);
                throw new InvalidIaasProviderException(msg);
            }

            if (log.isDebugEnabled()) {
                log.debug("Cloud Controller is delegating request to start an instance for "
                        + memberContext + " to Jclouds layer.");
            }
            // create and start a node
            Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(group, 1, template);
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
        } catch (Exception e) {
            String msg = "Failed to start an instance. " + memberContext.toString() + " Cause: " + e.getMessage();
            log.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
        return node;
    }

    public void allocateIpAddress(String clusterId, MemberContext memberContext, Partition partition,
                                  String cartridgeType, NodeMetadata node) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("IP allocation process started for " + memberContext);
            }
            String autoAssignIpProp = getIaasProvider().getProperty(CloudControllerConstants.AUTO_ASSIGN_IP_PROPERTY);
            String preDefinedIp = getIaasProvider().getProperty(CloudControllerConstants.FLOATING_IP_PROPERTY);
            String publicIp = "";
            String ip = "";

            // default behavior is autoIpAssign=false
            if ((autoAssignIpProp == null) || ((autoAssignIpProp != null) && autoAssignIpProp.equals("false"))) {

                // check if floating ip is well defined in cartridge definition
                if (preDefinedIp != null) {
                    if (CloudControllerServiceUtil.isValidIpAddress(preDefinedIp)) {
                        if (log.isDebugEnabled()) {
                            log.debug("CloudControllerServiceImpl:IpAllocator:preDefinedIp: invoking associatePredefinedAddress" + preDefinedIp);
                        }
                        ip = associatePredefinedAddress(node, preDefinedIp);

                        if (ip == null || "".equals(ip) || !preDefinedIp.equals(ip)) {
                            // throw exception and stop instance creation
                            String msg = "Error occurred while allocating predefined floating ip address: " + preDefinedIp +
                                    " / allocated ip:" + ip +
                                    " - terminating node:" + memberContext.toString();
                            log.error(msg);
                            // terminate instance
                            CloudControllerServiceUtil.terminate(getIaasProvider(),
                                    node.getId(), memberContext);
                            throw new CloudControllerException(msg);
                        }
                    } else {
                        String msg = "Invalid floating ip address configured: " + preDefinedIp +
                                " - terminating node:" + memberContext.toString();
                        log.error(msg);
                        // terminate instance
                        CloudControllerServiceUtil.terminate(getIaasProvider(), node.getId(), memberContext);
                        throw new CloudControllerException(msg);
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("CloudControllerServiceImpl:IpAllocator:no (valid) predefined floating ip configured, "
                                + "selecting available one from pool");
                    }
                    // allocate an IP address - manual IP assigning mode
                    ip = associateAddress(node);

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

            CloudControllerContext.getInstance().updateMemberContext(memberContext);

            // persist in registry
            CloudControllerContext.getInstance().persist();

            if (log.isDebugEnabled()) {
                log.debug("IP allocation process ended for " + memberContext);
            }

        } catch (Exception e) {
            String msg = "Error occurred while allocating an ip address. " + memberContext.toString();
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }

    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException, InvalidMemberException {

        String memberId = memberContext.getMemberId();
        String clusterId = memberContext.getClusterId();
        String partitionId = memberContext.getPartition().getId();
        String cartridgeType = memberContext.getCartridgeType();
        String nodeId = memberContext.getNodeId();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);

        log.info("Starting to terminate an instance with member id : " + memberId +
                " in partition id: " + partitionId + " of cluster id: " + clusterId +
                " and of cartridge type: " + cartridgeType);

        if (cartridge == null) {
            String msg = "Termination of Member Id: " + memberId + " failed. " +
                    "Cannot find a matching Cartridge for type: " +
                    cartridgeType;
            log.error(msg);
            throw new InvalidCartridgeTypeException(msg);
        }

        // if no matching node id can be found.
        if (nodeId == null) {
            String msg = "Termination failed. Cannot find a node id for Member Id: " + memberId;

            // log information
            CloudControllerServiceUtil.logTermination(memberContext);
            log.error(msg);
            throw new InvalidMemberException(msg);
        }

        IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);

        // terminate it!
        CloudControllerServiceUtil.terminate(iaasProvider, nodeId, memberContext);

        // log information
        CloudControllerServiceUtil.logTermination(memberContext);
    }
}
