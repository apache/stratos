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
package org.apache.stratos.autoscaler.api;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.NonExistingLBException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.interfaces.AutoScalerServiceInterface;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

/**
 * Auto Scaler Service API is responsible getting Partitions and Policies.
 */
public class AutoScalerServiceImpl implements AutoScalerServiceInterface{

	private static final Log log = LogFactory.getLog(AutoScalerServiceImpl.class);
	PartitionManager partitionManager = PartitionManager.getInstance();
	
	public Partition[] getAllAvailablePartitions(){
		return PartitionManager.getInstance().getAllPartitions();
	}
	
	public DeploymentPolicy[] getAllDeploymentPolicies(){
		return PolicyManager.getInstance().getDeploymentPolicyList().toArray(new DeploymentPolicy[0]);
	}
	
	public AutoscalePolicy[] getAllAutoScalingPolicy(){
		return PolicyManager.getInstance().getAutoscalePolicyList().toArray(new AutoscalePolicy[0]);
	}

	@Override
	public DeploymentPolicy[] getValidDeploymentPoliciesforCartridge(String cartridgeType) throws PartitionValidationException {
		ArrayList<DeploymentPolicy> validPolicies = new ArrayList<DeploymentPolicy>();
		
		for(DeploymentPolicy deploymentPolicy : this.getAllDeploymentPolicies()){
			Partition[] policyPartitions = deploymentPolicy.getAllPartitions();;
			boolean isValid = CloudControllerClient.getInstance().validatePartitionsOfPolicy(cartridgeType, policyPartitions);
			if(isValid)
				validPolicies.add(deploymentPolicy);			
		}
		return validPolicies.toArray(new DeploymentPolicy[0]);
	}

	@Override
	public boolean addPartition(Partition partition) {
        return PartitionManager.getInstance().deployNewPartitoion(partition);
	}

	@Override
	public boolean addDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException {
		return PolicyManager.getInstance().deployDeploymentscalePolicy(depPolicy);
	}

	@Override
	public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException {
		return PolicyManager.getInstance().deployAutoscalePolicy(aspolicy);
	}

	@Override
	public Partition getPartition(String partitionId) {
		return PartitionManager.getInstance().getPartitionById(partitionId);		
	}

	@Override
	public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId) {
		return PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId);
	}

	@Override
	public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId) {
		return PolicyManager.getInstance().getAutoscalePolicy(autoscalingPolicyId);
	}

	@Override
	public PartitionGroup[] getPartitionGroups(String deploymentPolicyId) {	
		return PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId).getPartitionGroups();
	}

	public Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId) {
		DeploymentPolicy depPol = this.getDeploymentPolicy(deploymentPolicyId);
		if(null == depPol) {
			return null;
		}
		
		return depPol.getAllPartitions();
	}
	
	@Override
    public Partition[] getPartitionsOfGroup(String deploymentPolicyId, String groupId) {
        DeploymentPolicy depPol = this.getDeploymentPolicy(deploymentPolicyId);
        if(null == depPol) {
            return null;
        }
        
        PartitionGroup group = depPol.getPartitionGroup(groupId);
        
        if(group == null) {
            return null;
        }
        
        return group.getPartitions();
    }
	
	public void checkLBExistenceAgainstPolicy(String lbClusterId, String deploymentPolicyId) throws NonExistingLBException {
	    
	    boolean exist = false;
	    Partition[] partitions = getPartitionsOfDeploymentPolicy(deploymentPolicyId);
        
        for (Partition partition : partitions) {
            if (partition != null) {
                NetworkPartitionContext nwPartitionCtxt =
                                                          partitionManager.getNetworkPartitionOfPartition(partition.getId());
                if (nwPartitionCtxt.isLBExist(lbClusterId)) {
                    exist = true;
                    break;
                }
            }
        }
	    
        if(!exist) {
            String msg = "LB with [cluster id] "+lbClusterId+
                    " does not exist in any network partition of [Deployment Policy] "+deploymentPolicyId;
            log.error(msg);
            throw new NonExistingLBException(msg);
        }
	}
	
	public boolean checkDefaultLBExistenceAgainstPolicy(String deploymentPolicyId) {
	    Partition[] partitions = getPartitionsOfDeploymentPolicy(deploymentPolicyId);
	    
        for (Partition partition : partitions) {
            if (partition != null) {
                NetworkPartitionContext nwPartitionCtxt =
                                                          partitionManager.getNetworkPartitionOfPartition(partition.getId());
                if (!nwPartitionCtxt.isDefaultLBExist()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Default LB does not exist in [network partition] " +
                                  nwPartitionCtxt.getId() + " of [Deployment Policy] " +
                                  deploymentPolicyId);

                    }
                    return false;
                }
            }
        }
        
        return true;
	    
    }
	
	public boolean checkServiceLBExistenceAgainstPolicy(String serviceName, String deploymentPolicyId) {
        Partition[] partitions = getPartitionsOfDeploymentPolicy(deploymentPolicyId);
        
        for (Partition partition : partitions) {
            if (partition != null) {
                NetworkPartitionContext nwPartitionCtxt =
                                                          partitionManager.getNetworkPartitionOfPartition(partition.getId());
                if (!nwPartitionCtxt.isServiceLBExist(serviceName)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Service LB [service name] "+serviceName+" does not exist in [network partition] " +
                                  nwPartitionCtxt.getId() + " of [Deployment Policy] " +
                                  deploymentPolicyId);

                    }
                    return false;
                }
            }
        }
        
        return true;
        
    }
	
	public boolean checkClusterLBExistenceAgainstPolicy(String clusterId, String deploymentPolicyId) {
        Partition[] partitions = getPartitionsOfDeploymentPolicy(deploymentPolicyId);
        
        for (Partition partition : partitions) {
            if (partition != null) {
                NetworkPartitionContext nwPartitionCtxt =
                                                          partitionManager.getNetworkPartitionOfPartition(partition.getId());
                if (!nwPartitionCtxt.isClusterLBExist(clusterId)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cluster LB [cluster id] "+clusterId+" does not exist in [network partition] " +
                                  nwPartitionCtxt.getId() + " of [Deployment Policy] " +
                                  deploymentPolicyId);

                    }
                    return false;
                }
            }
        }
        
        return true;
        
    }


}
