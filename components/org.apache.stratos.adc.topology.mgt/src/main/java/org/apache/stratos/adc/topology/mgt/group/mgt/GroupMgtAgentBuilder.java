/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.adc.topology.mgt.group.mgt;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.topology.mgt.util.ConfigHolder;
import org.apache.synapse.SynapseException;
import org.apache.stratos.lb.common.conf.LoadBalancerConfiguration;
import org.apache.stratos.lb.common.conf.util.HostContext;
import org.apache.stratos.lb.common.conf.util.TenantDomainContext;

public class GroupMgtAgentBuilder {

	private static LoadBalancerConfiguration lbConfig;
	/**
     * Key - host name 
     * Value - {@link HostContext}
     */
    private static Map<String, HostContext> hostContexts = new HashMap<String, HostContext>();
    
    private static final Log log = LogFactory.getLog(GroupMgtAgentBuilder.class);
	
	public static void createGroupMgtAgents() {
		lbConfig = ConfigHolder.getInstance().getLbConfig();
		hostContexts = lbConfig.getHostContextMap();
		
		ClusteringAgent clusteringAgent = ConfigHolder.getInstance().getAxisConfiguration().getClusteringAgent();
        if (clusteringAgent == null) {
            throw new SynapseException("Axis2 ClusteringAgent not defined in axis2.xml");
        }

        // Add the Axis2 GroupManagement agents
        if (hostContexts != null) {
            // iterate through each host context
            for (HostContext hostCtxt : hostContexts.values()) {
                // each host can has multiple Tenant Contexts, iterate through them
                for (TenantDomainContext tenantCtxt : hostCtxt.getTenantDomainContexts()) {

                    String domain = tenantCtxt.getDomain();
                    String subDomain = tenantCtxt.getSubDomain();

                    if (clusteringAgent.getGroupManagementAgent(domain, subDomain) == null) {
                        clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(
                            subDomain),
                            domain, subDomain, -1);
                        log.info("Group management agent added to cluster domain: " +
                            domain + " and sub domain: " + subDomain);
                    }
                }
            }
        }
    }
	
	public static void createGroupMgtAgent(String domain, String subDomain) {
		
		ClusteringAgent clusteringAgent = ConfigHolder.getInstance().getAxisConfiguration().getClusteringAgent();
        if (clusteringAgent == null) {
            throw new SynapseException("Axis2 ClusteringAgent not defined in axis2.xml");
        }
		
        if (clusteringAgent.getGroupManagementAgent(domain, subDomain) == null) {
            clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(
                subDomain),
                domain, subDomain, -1);
            log.info("Group management agent added to cluster domain: " +
                domain + " and sub domain: " + subDomain);
        }
	}
	
	public static void resetGroupMgtAgent(String domain, String subDomain) {

        ClusteringAgent clusteringAgent =
            ConfigHolder.getInstance().getAxisConfiguration().getClusteringAgent();
        
        if (clusteringAgent == null) {
            throw new SynapseException("Axis2 Clustering Agent not defined in axis2.xml");
        }

        // checks the existence. 
        if (clusteringAgent.getGroupManagementAgent(domain, subDomain) != null) {
           
	    // FIX THIS IN PROPER WAY
	    // This has changed some API of axis2 and we need to send upstream before using it	  
            //clusteringAgent.resetGroupManagementAgent(domain, subDomain);
            
            log.info("Group management agent of cluster domain: " +
                domain + " and sub domain: " + subDomain+" is removed.");
        }
    }
}
