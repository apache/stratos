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
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
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
	public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Partition getPartition(String partitionId) {
		for(Partition par: this.getAllAvailablePartitions()){
			if(par.getId().equals(partitionId)){
				return par;
			}
		}
		return null;
	}

	@Override
	public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId) {
		for(DeploymentPolicy depPol : this.getAllDeploymentPolicies()){
			if(depPol.getId().equals(deploymentPolicyId)){
				return depPol;
			}
		}
		return null;
	}

	@Override
	public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId) {
		for(AutoscalePolicy asPol : this.getAllAutoScalingPolicy()){
			if(asPol.getId().equals(autoscalingPolicyId))
				return asPol;
		}
		return null;
	}

	@Override
	public PartitionGroup[] getPartitionGroups(String deploymentPolicyId) {	
		this.getDeploymentPolicy(deploymentPolicyId).getAllPartitions();
		return null;
	}

	@Override
	public Partition[] getPartitionsOfDeploymentPolicy(String depPolicy, String partitonGroupId) {
		DeploymentPolicy depPol = this.getDeploymentPolicy(depPolicy);
		if(null == depPol)
			return null;
		
		PartitionGroup partGrp = depPol.getPartitionGroup(partitonGroupId);
		if(null == partGrp)
			return null;
		
		return partGrp.getPartitions();
	}

}
