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
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;

/**
 * 
 *  Manager class for the purpose of managing Autoscale-policy definitions.
 */
public class PolicyManager {
	
	private static final Log log = LogFactory.getLog(PolicyManager.class);
	
	private static Map<String,AutoscalePolicy> policyListMap = new HashMap<String, AutoscalePolicy>();
	private static Map<File,String> fileNameMap = new HashMap<File, String>();
	
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
	public void addPolicy(File policyFile,AutoscalePolicy policy) throws InvalidPolicyException {
		if(fileNameMap.containsKey(policyFile)){
			removePolicy(fileNameMap.get(policyFile));
			fileNameMap.remove(policyFile);
		} else{
			fileNameMap.put(policyFile, policy.getId());
		}
		if (!policyListMap.containsKey(policy.getId())) {
			if(log.isDebugEnabled()){
				log.debug("Adding policy :" + policy.getId());
			}
			policyListMap.put(policy.getId(), policy);
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
	public void addPolicy(AutoscalePolicy policy) throws InvalidPolicyException {
		addPolicy(new File(policy.getId().concat(".xml")), policy);
	}
	
	/**
	 * Removes the specified policy
	 * @param policy
	 * @throws InvalidPolicyException
	 */
	public void removePolicy(String policy) throws InvalidPolicyException {
		if (policyListMap.containsKey(policy)) {
			if(log.isDebugEnabled()){
				log.debug("Removing policy :" + policy);
			}
			policyListMap.remove(policy);
		} else {
			throw new InvalidPolicyException("No such policy [" + policy + "] exists");
		}
	}
	
	/**
	 * Removes the specified policy
	 * @param policyFile
	 * @throws InvalidPolicyException
	 */
	public void removePolicy(File policyFile) throws InvalidPolicyException {
		if(fileNameMap.containsKey(policyFile)){
			removePolicy(fileNameMap.get(policyFile));
			fileNameMap.remove(policyFile);
		} 
	}
	
	/**
	 * Returns a List of the policies contained in this manager.
	 * @return
	 */
	public List<AutoscalePolicy> getPolicyList() {
		return Collections.unmodifiableList(new ArrayList<AutoscalePolicy>(policyListMap.values()));
	}
	
	/**
	 * Returns the policy to which the specified id is mapped or null
	 * @param id
	 * @return
	 */
	public AutoscalePolicy getPolicy(String id) {
		return policyListMap.get(id);
	}


}
