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
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;

/**
 * 
 *  Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {
	
	private static final Log log = LogFactory.getLog(PolicyManager.class);
	
	private static Map<String,AutoscalePolicy> autoscalePolicyListMap = new HashMap<String, AutoscalePolicy>();
	private static Map<File,String> autoscalePolicyfileNameMap = new HashMap<File, String>();
	
	private static Map<String,DeploymentPolicy> deploymentPolicyListMap = new HashMap<String, DeploymentPolicy>();
	private static Map<File,String> deploymentPolicyfileNameMap = new HashMap<File, String>();
	
	private static PolicyManager instance = null;
	 
    private PolicyManager() {
    }

    public static PolicyManager getInstance() {
            if (instance == null) {
                    instance = new PolicyManager ();
            }
            return instance;
    }
    
    /**
     * Appends the specified policy
     * @param policyFile
     * @param policy
     * @throws InvalidPolicyException
     */
	public void addAutoscalePolicy(File policyFile,AutoscalePolicy policy) throws InvalidPolicyException {
		if(autoscalePolicyfileNameMap.containsKey(policyFile)){
			removeAutoscalePolicy(autoscalePolicyfileNameMap.get(policyFile));
			autoscalePolicyfileNameMap.remove(policyFile);
		} else{
			autoscalePolicyfileNameMap.put(policyFile, policy.getId());
		}
		if (!autoscalePolicyListMap.containsKey(policy.getId())) {
			if(log.isDebugEnabled()){
				log.debug("Adding policy :" + policy.getId());
			}
			autoscalePolicyListMap.put(policy.getId(), policy);
		} else {
			throw new InvalidPolicyException("Specified policy [" + policy.getId()
					+ "] already exists");
		}
	}
	
	/**
	 * Appends the specified policy
	 * @param policy
	 * @throws InvalidPolicyException
	 */
	public void addAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
		addAutoscalePolicy(new File(policy.getId().concat(".xml")), policy);
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
	 * Removes the specified policy
	 * @param policyFile
	 * @throws InvalidPolicyException
	 */
	public void removeAutoscalePolicy(File policyFile) throws InvalidPolicyException {
		if(autoscalePolicyfileNameMap.containsKey(policyFile)){
			removeAutoscalePolicy(autoscalePolicyfileNameMap.get(policyFile));
			autoscalePolicyfileNameMap.remove(policyFile);
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

	/**
     * Appends the specified policy
     * @param policyFile
     * @param policy
     * @throws InvalidPolicyException
     */
	public void addDeploymentPolicy(File policyFile,DeploymentPolicy policy) throws InvalidPolicyException {
		if(deploymentPolicyfileNameMap.containsKey(policyFile)){
			removeDeploymentPolicy(deploymentPolicyfileNameMap.get(policyFile));
			deploymentPolicyfileNameMap.remove(policyFile);
		} else{
			deploymentPolicyfileNameMap.put(policyFile, policy.getId());
		}
		if (!deploymentPolicyListMap.containsKey(policy.getId())) {
			if(log.isDebugEnabled()){
				log.debug("Adding policy :" + policy.getId());
			}
			deploymentPolicyListMap.put(policy.getId(), policy);
		} else {
			throw new InvalidPolicyException("Specified policy [" + policy.getId()
					+ "] already exists");
		}
	}
	
	/**
	 * Appends the specified policy
	 * @param policy
	 * @throws InvalidPolicyException
	 */
	public void addDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
		addDeploymentPolicy(new File(policy.getId().concat(".xml")), policy);
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
	 * Removes the specified policy
	 * @param policyFile
	 * @throws InvalidPolicyException
	 */
	public void removeDeploymentPolicy(File policyFile) throws InvalidPolicyException {
		if(deploymentPolicyfileNameMap.containsKey(policyFile)){
			removeDeploymentPolicy(deploymentPolicyfileNameMap.get(policyFile));
			deploymentPolicyfileNameMap.remove(policyFile);
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
