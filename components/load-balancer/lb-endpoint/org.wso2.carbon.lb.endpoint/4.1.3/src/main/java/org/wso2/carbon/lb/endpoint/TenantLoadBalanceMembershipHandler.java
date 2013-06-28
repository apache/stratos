package org.wso2.carbon.lb.endpoint;


import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.LoadBalanceMembershipHandler;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.wso2.carbon.lb.common.conf.util.HostContext;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Bridge between Axis2 membership notification and Synapse load balancing
 */
public class TenantLoadBalanceMembershipHandler implements LoadBalanceMembershipHandler {
    private static final Log log = LogFactory.getLog(TenantLoadBalanceMembershipHandler.class);

    private ConfigurationContext configCtx;

    private LoadbalanceAlgorithm lbAlgo;
    
    /**
     * Key - Host, Value - HostContext
     */
    private static Map<String, HostContext> hostContextsMap =
            new HashMap<String, HostContext>();
    
    private ClusteringAgent clusteringAgent;
    
    private boolean isClusteringEnabled;
    private String endpointName;

    public TenantLoadBalanceMembershipHandler(Map<String, HostContext> hostContexts,
                                              LoadbalanceAlgorithm algorithm,
                                              ConfigurationContext configCtx,
                                              boolean isClusteringEnabled,
                                              String endpointName) {

        lbAlgo = algorithm;
        this.isClusteringEnabled = isClusteringEnabled;
        this.endpointName = endpointName;
        this.configCtx = configCtx;
        
        for (HostContext host : hostContexts.values()) {
            
            addHostContext(host);

        }
    }
    
    /**
     * This will be used to add new {@link HostContext}s.
     * @param host {@link HostContext}
     */
    public void addHostContext(HostContext host){
        
        String hostName = host.getHostName();

        AlgorithmContext algorithmContext =
                                            new AlgorithmContext(isClusteringEnabled,
                                                                 configCtx, endpointName + "." +
                                                                            hostName);

        host.setAlgorithm(lbAlgo.clone());
        host.setAlgorithmContext(algorithmContext);

        hostContextsMap.put(hostName, host);
        
    }
    
    /**
     * This will be used to remove an existing {@link HostContext}s.
     * @param host {@link HostContext}
     */
    public void removeHostContext(String host){

        hostContextsMap.remove(host);
        
    }

    public void init(Properties props, LoadbalanceAlgorithm algorithm) {
        // Nothing to do
    }

    public void setConfigurationContext(ConfigurationContext configCtx) {
        this.configCtx = configCtx;

        // The following code does the bridging between Axis2 and Synapse load balancing
        clusteringAgent = configCtx.getAxisConfiguration().getClusteringAgent();
        if (clusteringAgent == null) {
            String msg = "In order to enable load balancing across an Axis2 cluster, " +
                         "the cluster entry should be enabled in the axis2.xml file";
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    public ConfigurationContext getConfigurationContext() {
        return configCtx;
    }

    /**
     * Getting the next member to which the request has to be sent in a round-robin fashion
     *
     * @param context The AlgorithmContext
     * @return The current member
     * @deprecated Use {@link #getNextApplicationMember(String, int)}
     */
    public Member getNextApplicationMember(AlgorithmContext context) {
        throw new UnsupportedOperationException("This operation is invalid. " +
                                                "Call getNextApplicationMember(String host)");
    }

    public boolean isAValidHostName(String host){
        if(getHostContext(host) != null){
            return true;
        }
        return false;
    }

    public Member getNextApplicationMember(String host, int tenantId) {
        HostContext hostContext = getHostContext(host);

        if(hostContext == null){
            String msg = "Invalid host name : " + host;
            log.error(msg);
            throw new SynapseException(msg);
        }

        // here we have to pass tenant id to get domain from hostContext
        String domain = hostContext.getDomainFromTenantId(tenantId);
        String subDomain = hostContext.getSubDomainFromTenantId(tenantId);

        LoadbalanceAlgorithm algorithm = hostContext.getAlgorithm();
        GroupManagementAgent groupMgtAgent = clusteringAgent.getGroupManagementAgent(domain, subDomain);
        
        if (groupMgtAgent == null) {
        	String tenantDomain;
            try {
	            tenantDomain = ConfigHolder.getInstance().getRealmService().getTenantManager().getDomain(tenantId);
            } catch (UserStoreException ignore) {
            	tenantDomain = ""+tenantId;
            }
        	
            String msg =
                    "No Group Management Agent found for the domain: " + domain + ", subDomain: "
                    		+ subDomain + ", host: " + host+ " and for tenant: "
                    		+  tenantDomain;
            log.error(msg); 
            throw new SynapseException(msg);
        }
        algorithm.setApplicationMembers(groupMgtAgent.getMembers());
        AlgorithmContext context = hostContext.getAlgorithmContext();
        return algorithm.getNextApplicationMember(context);
    }

    public HostContext getHostContext(String host) {
        HostContext hostContext = hostContextsMap.get(host);
        if (hostContext == null) {
            int indexOfDot;
            if ((indexOfDot = host.indexOf(".")) != -1) {
                hostContext = getHostContext(host.substring(indexOfDot + 1));
            } 
        }
        return hostContext;
    }

    public LoadbalanceAlgorithm getLoadbalanceAlgorithm() {
        return lbAlgo;
    }

    public Properties getProperties() {
        return null;
    }
    
    public ClusteringAgent getClusteringAgent() {
        return clusteringAgent;
    }
    
}
