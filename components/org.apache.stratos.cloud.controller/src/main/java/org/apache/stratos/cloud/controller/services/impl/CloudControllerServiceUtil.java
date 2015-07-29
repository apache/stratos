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

import com.google.common.net.InetAddresses;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.statistics.publisher.BAMUsageDataPublisher;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.domain.topology.MemberStatus;

import java.util.Properties;

/**
 * Cloud controller service utility methods.
 */
public class CloudControllerServiceUtil {

    private static final Log log = LogFactory.getLog(CloudControllerServiceUtil.class);

    public static Iaas buildIaas(IaasProvider iaasProvider) throws InvalidIaasProviderException {
        return iaasProvider.getIaas();
    }

    /**
     * Update the topology, publish statistics to BAM, remove member context
     * and persist cloud controller context.
     *
     * @param memberContext
     */
    public static void executeMemberTerminationPostProcess(MemberContext memberContext) {
        if (memberContext == null) {
            return;
        }

        String partitionId = memberContext.getPartition() == null ? null : memberContext.getPartition().getId();

        // Update the topology
        TopologyBuilder.handleMemberTerminated(memberContext.getCartridgeType(),
                memberContext.getClusterId(), memberContext.getNetworkPartitionId(),
                partitionId, memberContext.getMemberId());
        //member terminated time
        Long timeStamp = System.currentTimeMillis();
        // Publish statistics to BAM
        BAMUsageDataPublisher.publish(memberContext.getMemberId(),
                partitionId,
                memberContext.getNetworkPartitionId(),
                memberContext.getClusterInstanceId(),
                memberContext.getClusterId(),
                memberContext.getCartridgeType(),
                MemberStatus.Terminated.toString(),
                timeStamp, null, null, null);

        // Remove member context
        CloudControllerContext.getInstance().removeMemberContext(memberContext.getClusterId(),
                memberContext.getMemberId());

        // Persist cloud controller context
        CloudControllerContext.getInstance().persist();
    }

    public static boolean isValidIpAddress(String ip) {
        boolean isValid = InetAddresses.isInetAddress(ip);
        return isValid;
    }

    public static IaasProvider validatePartitionAndGetIaasProvider(Partition partition, IaasProvider iaasProvider)
            throws InvalidPartitionException {
        if (iaasProvider != null) {
            // if this is a IaaS based partition
            Iaas iaas = iaasProvider.getIaas();
            PartitionValidator validator = iaas.getPartitionValidator();
            validator.setIaasProvider(iaasProvider);
            Properties partitionProperties = CloudControllerUtil.toJavaUtilProperties(partition.getProperties());
            iaasProvider = validator.validate(partition, partitionProperties);
            return iaasProvider;

        } else {
            String msg = "Partition is not valid: [partition-id] " + partition.getId();
            log.error(msg);
            throw new InvalidPartitionException(msg);
        }
    }

    public static boolean validatePartition(Partition partition, IaasProvider iaasProvider)
            throws InvalidPartitionException {
        validatePartitionAndGetIaasProvider(partition, iaasProvider);
        return true;
    }
}
