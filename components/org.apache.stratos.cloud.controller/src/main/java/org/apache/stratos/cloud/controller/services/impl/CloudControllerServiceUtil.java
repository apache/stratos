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

import java.util.Properties;

import com.google.common.net.InetAddresses;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.ContainerClusterContext;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.domain.Volume;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.validators.IaasBasedPartitionValidator;
import org.apache.stratos.cloud.controller.iaases.validators.KubernetesBasedPartitionValidator;
import org.apache.stratos.cloud.controller.messaging.publisher.CartridgeInstanceDataPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.jclouds.rest.ResourceNotFoundException;

/**
 * Cloud controller service utility methods.
 */
public class CloudControllerServiceUtil {

    private static final Log log = LogFactory.getLog(CloudControllerServiceUtil.class);

    public static Iaas buildIaas(IaasProvider iaasProvider) throws InvalidIaasProviderException {
        Iaas iaas = iaasProvider.getIaas();
        iaas.initialize();
        return iaas;
    }

    /**
     * A helper method to terminate an instance.
     *
     * @param iaasProvider
     * @param ctxt
     * @param nodeId
     * @return will return the IaaSProvider
     */
    public static IaasProvider terminate(IaasProvider iaasProvider,
                                   String nodeId, MemberContext ctxt) {
        Iaas iaas = iaasProvider.getIaas();
        if (iaas == null) {

            try {
                iaas = buildIaas(iaasProvider);
            } catch (InvalidIaasProviderException e) {
                String msg =
                        "Instance termination failed. " + ctxt.toString() +
                                ". Cause: Unable to build Iaas of this " + iaasProvider.toString();
                log.error(msg, e);
                throw new CloudControllerException(msg, e);
            }

        }

        //detach volumes if any
        detachVolume(iaasProvider, ctxt);

        // destroy the node
        iaasProvider.getComputeService().destroyNode(nodeId);

        // release allocated IP address
        if (ctxt.getAllocatedIpAddress() != null) {
            iaas.releaseAddress(ctxt.getAllocatedIpAddress());
        }

        if (log.isDebugEnabled()) {
            log.debug("Member is terminated: " + ctxt.toString());
        } else if (log.isInfoEnabled()) {
            log.info("Member with id " + ctxt.getMemberId() + " is terminated");
        }
        return iaasProvider;
    }

    private static void detachVolume(IaasProvider iaasProvider, MemberContext ctxt) {
        String clusterId = ctxt.getClusterId();
        ClusterContext clusterCtxt = CloudControllerContext.getInstance().getClusterContext(clusterId);
        if (clusterCtxt.getVolumes() != null) {
            for (Volume volume : clusterCtxt.getVolumes()) {
                try {
                    String volumeId = volume.getId();
                    if (volumeId == null) {
                        return;
                    }
                    Iaas iaas = iaasProvider.getIaas();
                    iaas.detachVolume(ctxt.getInstanceId(), volumeId);
                } catch (ResourceNotFoundException ignore) {
                    if (log.isDebugEnabled()) {
                        log.debug(ignore);
                    }
                }
            }
        }
    }

    public static void logTermination(MemberContext memberContext) {

        if (memberContext == null) {
            return;
        }

        String partitionId = memberContext.getPartition() == null ? null : memberContext.getPartition().getId();

        //updating the topology
        TopologyBuilder.handleMemberTerminated(memberContext.getCartridgeType(),
                memberContext.getClusterId(), memberContext.getNetworkPartitionId(),
                partitionId, memberContext.getMemberId());

        //publishing data
        CartridgeInstanceDataPublisher.publish(memberContext.getMemberId(),
                partitionId,
                memberContext.getNetworkPartitionId(),
                memberContext.getClusterId(),
                memberContext.getCartridgeType(),
                MemberStatus.Terminated.toString(),
                null);

        // update data holders
        CloudControllerContext.getInstance().removeMemberContext(memberContext.getMemberId(), memberContext.getClusterId());

        // persist
        CloudControllerContext.getInstance().persist();
    }

    public static boolean isValidIpAddress(String ip) {
        boolean isValid = InetAddresses.isInetAddress(ip);
        return isValid;
    }
    
    public static IaasProvider validatePartitionAndGetIaasProvider(Partition partition, IaasProvider iaasProvider) throws InvalidPartitionException {
        String provider = partition.getProvider();
        String partitionId = partition.getId();
        Properties partitionProperties = CloudControllerUtil.toJavaUtilProperties(partition.getProperties());

        if (iaasProvider != null) {
            // if this is a IaaS based partition
            Iaas iaas = iaasProvider.getIaas();

//            if (iaas == null) {
//                try {
//                    iaas = CloudControllerUtil.getIaas(iaasProvider);
//                } catch (InvalidIaasProviderException e) {
//                    String msg =
//                            "Invalid Partition - " + partition.toString()
//                                    + ". Cause: Unable to build Iaas of this IaasProvider [Provider] : " + provider
//                                    + ". " + e.getMessage();
//                    log.error(msg, e);
//                    throw new InvalidPartitionException(msg, e);
//                }
//            }

            IaasBasedPartitionValidator validator = (IaasBasedPartitionValidator) iaas.getPartitionValidator();
            validator.setIaasProvider(iaasProvider);
            iaasProvider = validator.validate(partitionId, partitionProperties);
            return iaasProvider;

        } else if (CloudControllerConstants.DOCKER_PARTITION_PROVIDER.equals(provider)) {
            // if this is a docker based Partition
            KubernetesBasedPartitionValidator validator = new KubernetesBasedPartitionValidator();
            validator.validate(partitionId, partitionProperties);
            return null;

        } else {

            String msg =
                    "Invalid Partition - " + partition.toString() + ". Cause: Cannot identify as a valid partition.";
            log.error(msg);
            throw new InvalidPartitionException(msg);
        }
    }
    
    public static boolean validatePartition(Partition partition, IaasProvider iaasProvider) throws InvalidPartitionException {
        validatePartitionAndGetIaasProvider(partition, iaasProvider);
        return true;
    }
    
}
