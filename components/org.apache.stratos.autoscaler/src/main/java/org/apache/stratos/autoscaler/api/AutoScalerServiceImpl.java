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
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.interfaces.AutoScalerServiceInterface;
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
		return PartitionManager.getInstance().getAllPartitions().toArray(new Partition[0]);		
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
			Partition[] policyPartitions = deploymentPolicy.getAllPartitions().toArray(new Partition[0]);
			boolean isValid = CloudControllerClient.getInstance().validatePartitionsOfPolicy(cartridgeType, policyPartitions);
			if(isValid)
				validPolicies.add(deploymentPolicy);			
		}
		return validPolicies.toArray(new DeploymentPolicy[0]);
	}

}
