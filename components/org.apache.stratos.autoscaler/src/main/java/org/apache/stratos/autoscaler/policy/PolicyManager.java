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

package org.apache.stratos.autoscaler.policy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

/**
 * 
 *  Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {
	
	private static final Log log = LogFactory.getLog(PolicyManager.class);

	private static final String asResourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE+ AutoScalerConstants.AS_POLICY_RESOURCE + "/";
	private static final String deploymentPolicyResourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE+ AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/";
	
	private static Map<String,AutoscalePolicy> autoscalePolicyListMap = new HashMap<String, AutoscalePolicy>();
	
	private static Map<String,DeploymentPolicy> deploymentPolicyListMap = new HashMap<String, DeploymentPolicy>();
	
	private static PolicyManager instance = null;
	 
    private PolicyManager() {
    }

    public static PolicyManager getInstance() {
        if (instance == null) {
            synchronized (PolicyManager.class){
                if (instance == null) {
                    instance = new PolicyManager();
                }
            }
        }
        return instance;
    }
    
    // Add the policy to information model and persist.
	public boolean deployAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {	
		this.addASPolicyToInformationModel(policy);
		this.persitASPolicy(asResourcePath+ policy.getId(), policy);	
		
		log.info("AutoScaling policy  :" + policy.getId() + " is deployed successfully.");
		return true;
	}
	
	// Add the deployment policy to information model and persist.
	public boolean deployDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {	
	    try {
	        Partition[] allPartitions = policy.getAllPartitions();
            validateExistenceOfPartions(allPartitions);
            
        } catch (InvalidPartitionException e) {
            String msg = "Deployment Policy is invalid. Policy name: " + policy.getId();
            log.error(msg, e);
            throw new InvalidPolicyException(msg, e);
        }
	    
		this.addDeploymentPolicyToInformationModel(policy);
		this.persitDeploymentPolicy(deploymentPolicyResourcePath+ policy.getId(), policy);
		
		log.info("Deployment policy  :" + policy.getId() + " is deployed successfully.");
		return true;
	}
	
	private static void validateExistenceOfPartions(Partition[] partitions) throws InvalidPartitionException {
        PartitionManager partitionMgr = PartitionManager.getInstance();
        for (Partition partition : partitions) {
            String partitionId = partition.getId();
            if (partitionId == null || !partitionMgr.partitionExist(partitionId)) {
                String msg =
                             "Non existing Partition defined. Partition id: " + partitionId + ". " +
                                     "Please define the partition in the partition definition file.";
                log.error(msg);
                throw new InvalidPartitionException(msg);
            }
            fillPartition(partition, partitionMgr.getPartitionById(partitionId));
        }
    }

    private static void fillPartition(Partition destPartition, Partition srcPartition) {

        if (!destPartition.isProviderSpecified()) {
            destPartition.setProvider(srcPartition.getProvider());
        }
        if (!destPartition.isPartitionMaxSpecified()) {
            destPartition.setPartitionMax(srcPartition.getPartitionMax());
        }
        if (!destPartition.isPartitionMinSpecified()) {
            destPartition.setPartitionMin(srcPartition.getPartitionMin());
        }
        if (!destPartition.isPropertiesSpecified()) {
            destPartition.setProperties(srcPartition.getProperties());
        }
    }
		
	public void addASPolicyToInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException{
		if (!autoscalePolicyListMap.containsKey(asPolicy.getId())) {
			if(log.isDebugEnabled()){
				log.debug("Adding policy :" + asPolicy.getId());
			}			
			autoscalePolicyListMap.put(asPolicy.getId(), asPolicy);
		} else {
			throw new InvalidPolicyException("Specified policy [" + asPolicy.getId()
					+ "] already exists");
		}
	}
	
	private void persitASPolicy(String asResourcePath, AutoscalePolicy policy){		
		try {
			RegistryManager.getInstance().persist(policy, asResourcePath);
		} catch (RegistryException e) {
			throw new AutoScalerException(e);
		}
	}
	
	private void persitDeploymentPolicy(String depResourcePath, DeploymentPolicy policy){		
		try {
			RegistryManager.getInstance().persist(policy, depResourcePath);
		} catch (RegistryException e) {
			throw new AutoScalerException(e);
		}
	}
	
	/**
	 * Removes the specified policy
	 * @param policy
	 * @throws InvalidPolicyException
	 */
	public void removeAutoscalePolicy(String policy) throws InvalidPolicyException {
		if (autoscalePolicyListMap.containsKey(policy)) {
			if(log.isDebugEnabled()){
				log.debug("Removing policy :" + policy);
			}
			autoscalePolicyListMap.remove(policy);
		} else {
			throw new InvalidPolicyException("No such policy [" + policy + "] exists");
		}
	}
	
	/**
	 * Returns a List of the Autoscale policies contained in this manager.
	 * @return
	 */
	public List<AutoscalePolicy> getAutoscalePolicyList() {
		return Collections.unmodifiableList(new ArrayList<AutoscalePolicy>(autoscalePolicyListMap.values()));
	}
	
	/**
	 * Returns the autoscale policy to which the specified id is mapped or null
	 * @param id
	 * @return
	 */
	public AutoscalePolicy getAutoscalePolicy(String id) {
		return autoscalePolicyListMap.get(id);
	}

	// Add the deployment policy to As in memmory information model. Does not persist.
	public void addDeploymentPolicyToInformationModel(DeploymentPolicy policy) throws InvalidPolicyException {
		if (!deploymentPolicyListMap.containsKey(policy.getId())) {
			if(log.isDebugEnabled()){
				log.debug("Adding policy :" + policy.getId());
			}
			PartitionManager.getInstance().deployNewNetworkPartitions(policy);
			deploymentPolicyListMap.put(policy.getId(), policy);
		} else {
			throw new InvalidPolicyException("Specified policy [" + policy.getId()
					+ "] already exists");
		}
	}
		
	/**
	 * Removes the specified policy
	 * @param policy
	 * @throws InvalidPolicyException
	 */
	public void removeDeploymentPolicy(String policy) throws InvalidPolicyException {
		if (deploymentPolicyListMap.containsKey(policy)) {
			if(log.isDebugEnabled()){
				log.debug("Removing policy :" + policy);
			}
			deploymentPolicyListMap.remove(policy);
		} else {
			throw new InvalidPolicyException("No such policy [" + policy + "] exists");
		}
	}
	
	/**
	 * Returns a List of the Deployment policies contained in this manager.
	 * @return
	 */
	public List<DeploymentPolicy> getDeploymentPolicyList() {
		return Collections.unmodifiableList(new ArrayList<DeploymentPolicy>(deploymentPolicyListMap.values()));
	}
	
	/**
	 * Returns the deployment policy to which the specified id is mapped or null
	 * @param id
	 * @return
	 */
	public DeploymentPolicy getDeploymentPolicy(String id) {
		return deploymentPolicyListMap.get(id);
	}

}
