package org.wso2.carbon.lb.endpoint.group.mgt;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.lb.common.group.mgt.SubDomainAwareGroupManagementAgent;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;

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
