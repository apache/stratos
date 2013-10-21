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
	
	private static PolicyManager instance = null;
	 
    private PolicyManager() {
    }

    public static PolicyManager getInstance() {
            if (instance == null) {
                    instance = new PolicyManager ();
            }
            return instance;
    }
    
	public void addPolicy(AutoscalePolicy policy) throws InvalidPolicyException {
		if (!policyListMap.containsKey(policy.getName())) {
			if(log.isDebugEnabled()){
				log.debug("Adding policy :" + policy.getName());
			}
			policyListMap.put(policy.getName(), policy);
		} else {
			throw new InvalidPolicyException("Specified service [" + policy.getName()
					+ "] already exists");
		}
	}
	
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
	
	public List<AutoscalePolicy> getPolicyList() {
		return Collections.unmodifiableList(new ArrayList<AutoscalePolicy>(policyListMap.values()));
	}


}
