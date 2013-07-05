/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.lb.endpoint.group.mgt;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.stratos.lb.endpoint.util.ConfigHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.stratos.lb.common.group.mgt.SubDomainAwareGroupManagementAgent;

/**
 * Responsible for building {@link GroupManagementAgent}s.
 */
public class GroupMgtAgentBuilder {
    
    private static final Log log = LogFactory.getLog(GroupMgtAgentBuilder.class);
	
    /**
     * Creates a {@link SubDomainAwareGroupManagementAgent} corresponds to the given 
     * parameters, if and only if there's no existing agent.
     * @param domain clustering domain.
     * @param subDomain clustering sub domain.
     */
    public static void createGroupMgtAgent(String domain, String subDomain) {

        ClusteringAgent clusteringAgent =
            ConfigHolder.getInstance().getAxisConfiguration().getClusteringAgent();
        
        if (clusteringAgent == null) {
            throw new SynapseException("Axis2 Clustering Agent not defined in axis2.xml");
        }

        // checks the existence. 
        if (clusteringAgent.getGroupManagementAgent(domain, subDomain) == null) {
            
            clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(subDomain),
                domain, subDomain,-1);
            
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
            
            clusteringAgent.resetGroupManagementAgent(domain, subDomain);
            
            log.info("Group management agent of cluster domain: " +
                domain + " and sub domain: " + subDomain+" is removed.");
        }
    }
}
