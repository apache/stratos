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

import java.util.List;
import java.util.Properties;

import com.google.common.net.InetAddresses;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.DeploymentPolicy;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.NetworkPartition;
import org.apache.stratos.cloud.controller.domain.NetworkPartitionRef;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.domain.PartitionRef;
import org.apache.stratos.cloud.controller.exception.InvalidDeploymentPolicyException;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.messaging.publisher.StatisticsDataPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.MemberStatus;

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

        // Publish statistics to BAM
        StatisticsDataPublisher.publish(memberContext.getMemberId(),
                partitionId,
                memberContext.getNetworkPartitionId(),
                memberContext.getClusterId(),
                memberContext.getCartridgeType(),
                MemberStatus.Terminated.toString(),
                null);

        // Remove member context
        CloudControllerContext.getInstance().removeMemberContext(memberContext.getClusterId(), memberContext.getMemberId());

        // Persist cloud controller context
        CloudControllerContext.getInstance().persist();
    }

    public static boolean isValidIpAddress(String ip) {
        boolean isValid = InetAddresses.isInetAddress(ip);
        return isValid;
    }
    
    public static IaasProvider validatePartitionAndGetIaasProvider(Partition partition, IaasProvider iaasProvider) throws InvalidPartitionException {
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
    
    public static boolean validatePartition(Partition partition, IaasProvider iaasProvider) throws InvalidPartitionException {
        validatePartitionAndGetIaasProvider(partition, iaasProvider);
        return true;
    }
    
    /**
     * Validates deployment policy. This method will not validate whether the deployment policy is already deployed or not.
     * Reason is this method has to be used in add/update deployment policy APIs which have different view on "already deployed deployment policies".
     * It is the caller's responsibility to validate whether the deployment policy is already deployed or not.
     * @param deploymentPolicy the {@link DeploymentPolicy} to be validated
     * @throws InvalidDeploymentPolicyException will be thrown if the given {@link DeploymentPolicy} is not valid
     */
    public static void validateDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws InvalidDeploymentPolicyException {
    	
    	// deployment policy can't be null
    	if (null == deploymentPolicy) {
			String msg = "Invalid deployment policy. Cause -> Deployment policy is null";
			log.error(msg);
			throw new InvalidDeploymentPolicyException(msg);
		}
    	
    	if (log.isInfoEnabled()) {
    		log.info(String.format("Validating deployment policy %s", deploymentPolicy.toString()));
		}
    	
    	// deployment policy id can't be null or empty
    	if (null == deploymentPolicy.getDeploymentPolicyID() || deploymentPolicy.getDeploymentPolicyID().isEmpty()) {
			String msg = String.format("Invalid deployment policy. Cause -> Invalid deployment policy id [deployment-policy-id] %s", 
					deploymentPolicy.getDeploymentPolicyID());
			log.error(msg);
			throw new InvalidDeploymentPolicyException(msg);
		}
    	
    	// deployment policy should contain at least one network partition reference
    	if (null == deploymentPolicy.getNetworkPartitionsRef() || deploymentPolicy.getNetworkPartitionsRef().length == 0) {
			String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
					+ "Cause -> Deployment policy doesn't have at least one network partition reference", 
					deploymentPolicy.getDeploymentPolicyID());
			log.error(msg);
			throw new InvalidDeploymentPolicyException(msg);
		}
    	
    	// validate each network partition references
    	for (NetworkPartitionRef networkPartitionRef : deploymentPolicy.getNetworkPartitionsRef()) {
    		
			// network partition id can't be null or empty
    		if (null == networkPartitionRef.getId() || networkPartitionRef.getId().isEmpty()) {
				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
						+ "Cause -> Invalid network partition id in network partition references section", 
						deploymentPolicy.getDeploymentPolicyID());
				log.error(msg);
				throw new InvalidDeploymentPolicyException(msg);
			}
    		
    		// network partitions should be already added
    		if (null == CloudControllerContext.getInstance().getNetworkPartition(networkPartitionRef.getId())) {
				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
						+ "Cause -> Network partition is not added - [network-partition-id] %s", 
						deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId());
				log.error(msg);
				throw new InvalidDeploymentPolicyException(msg);
			}
    		
    		// partition algorithm can't be null or empty
    		if (null == networkPartitionRef.getPartitionAlgo() || networkPartitionRef.getPartitionAlgo().isEmpty()) {
				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
						+ "Cause -> Invalid partition algorithm - [network-partition-id] %s [partition-algo] %s", 
						deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId(), networkPartitionRef.getPartitionAlgo());
				log.error(msg);
				throw new InvalidDeploymentPolicyException(msg);
			}
    		
    		// partition algorithm should be either one-after-another or round-robin
    		if (!StratosConstants.ROUND_ROBIN_ALGORITHM_ID.equals(networkPartitionRef.getPartitionAlgo())
    				&& !StratosConstants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(networkPartitionRef.getPartitionAlgo())) {
				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
						+ "Cause -> Invalid partition algorithm - [network-partition-id] %s [partition-algo] %s", 
						deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId(), networkPartitionRef.getPartitionAlgo());
				log.error(msg);
				throw new InvalidDeploymentPolicyException(msg);
			}
    		
    		// a network partition reference should contain at least one partition reference
    		if (null == networkPartitionRef.getPartitions() || networkPartitionRef.getPartitions().length == 0) {
				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
						+ "Cause -> Network partition reference doesn't have at lease one partition reference - "
						+ "[network-partition-id] %s", deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId());
				log.error(msg);
				throw new InvalidDeploymentPolicyException(msg);
			}
    		
    		// validate each partition reference defined in network partition reference
    		for (PartitionRef partitionRef : networkPartitionRef.getPartitions()) {
				
    			// partition id can't be null or empty
    			if (null == partitionRef.getId() || partitionRef.getId().isEmpty()) {
    				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
    						+ "Cause -> Invalid partition id for a partition defined in the netwok partition - [network-partition-id] %s", 
    						deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId());
    				log.error(msg);
    				throw new InvalidDeploymentPolicyException(msg);
				}
    			
    			// partition should be defined in the relevant network partitions (on network partition deployment)
    			NetworkPartition networkPartition = CloudControllerContext.getInstance().getNetworkPartition(networkPartitionRef.getId());
    			if (null == networkPartition.getPartition(partitionRef.getId())) {
    				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
    						+ "Cause -> Partition not found in the network partition definition - [network-partition-id] %s [partition-id] %s", 
    						deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId(), partitionRef.getId());
    				log.error(msg);
    				throw new InvalidDeploymentPolicyException(msg);
				}
    			
    			// partition reference should have a valid max value
    			if (partitionRef.getMax() <= 0) {
    				String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
    						+ "Cause -> Partition max value is not valid - [network-partition-id] %s [partition-id] %s [max-value] %s", 
    						deploymentPolicy.getDeploymentPolicyID(), networkPartitionRef.getId(), partitionRef.getId(), partitionRef.getMax());
    				log.error(msg);
    				throw new InvalidDeploymentPolicyException(msg);
				}
			}
		}
    }
    
    /**
     * Overwrites partition's kubernetes cluster ids with network partition's kubernetes cluster ids.
     * @param networkPartitions
     */
    public static void overwritesPartitionsKubernetesClusterIdsWithNetworkPartitionKubernetesClusterId(NetworkPartition networkPartition) {

    	if(StringUtils.isNotBlank(networkPartition.getKubernetesClusterId())) {
    		Partition[] partitions = networkPartition.getPartitions();
    		if(partitions != null) {
    			for(Partition partition : partitions) {
    				if(partition != null) {
    					if(log.isInfoEnabled()) {
    						log.info(String.format("Overwriting partition's kubernetes cluster id: " +
    								"[network-partition-id] %s [partition-id] %s [kubernetes-cluster-id] %s", 
    								networkPartition.getId(), partition.getId(), networkPartition.getKubernetesClusterId()));
    					}
    					partition.setKubernetesClusterId(networkPartition.getKubernetesClusterId());
    				}
    			}
    		}
    	}
    }
}
