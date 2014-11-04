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

package org.apache.stratos.autoscaler.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
 
public class DeploymentPolicyCommands implements CommandProvider{
 
    public String getHelp() {
    	return "\nlistDeploymentPolicies - List Deployment policies deployed to AutoScaler. \n"
        		+ "\t parameters : \n"
        		+ "\t\t String   policyID : ID of the deployment policy.\n";
    }
 
    public void _listDeploymentPolicies (CommandInterpreter ci){
    	String policyId = ci.nextArgument();
    	
    	PolicyManager pm = PolicyManager.getInstance();
    	
    	if(StringUtils.isBlank(policyId)){
    		DeploymentPolicy[] depPolicyArr = pm.getDeploymentPolicyList();
        	for(DeploymentPolicy depPoolicy : depPolicyArr){
        		ci.println(depPoolicy.toString());
        	}
    	}else{
    		DeploymentPolicy asPolicy = pm.getDeploymentPolicy(policyId);
    		if(asPolicy != null){
    			ci.println(asPolicy);
    		}
    	}
    }
}