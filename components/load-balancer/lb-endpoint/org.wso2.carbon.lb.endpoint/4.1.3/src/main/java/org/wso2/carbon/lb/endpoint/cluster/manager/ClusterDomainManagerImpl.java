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
package org.wso2.carbon.lb.endpoint.cluster.manager;

import java.util.ArrayList;
import java.util.List;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.cartridge.messages.ClusterDomainManager;
import org.wso2.carbon.cartridge.messages.ClusterDomain;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.lb.common.conf.util.Constants;
import org.wso2.carbon.lb.common.conf.util.HostContext;
import org.wso2.carbon.lb.common.conf.util.TenantDomainContext;
import org.wso2.carbon.lb.common.group.mgt.SubDomainAwareGroupManagementAgent;
import org.wso2.carbon.lb.endpoint.TenantLoadBalanceMembershipHandler;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;

/**
 * Bridge between the ELB and the Stratos2 Agent.
 */
public class ClusterDomainManagerImpl implements ClusterDomainManager {

    private static final Log log = LogFactory.getLog(ClusterDomainManagerImpl.class);

    @Override
    public void addClusterDomain(ClusterDomain cluster) {

        // create group management agent, if one doesn't exist already.
        HostContext hostCtxt = createGroupMgtAgentIfNotExists(cluster);

        // we should only update if the above step is successful.
        if (hostCtxt != null) {
            // create / update Service Configuration
            createOrUpdateServiceConfig(cluster, hostCtxt);
        }
        
    }

    @Override
	public void removeClusterDomain(String domain, String subDomain, String hostName) {

		TenantLoadBalanceMembershipHandler handler =
		                                             ConfigHolder.getInstance()
		                                                         .getTenantLoadBalanceMembershipHandler();

		if (handler == null) {
			String msg = "TenantLoadBalanceMembershipHandler is null. Thus, We cannot proceed.";
			log.error(msg);
			throw new SynapseException(msg);
		}

		handler.removeHostContext(hostName);
		
		LoadBalancerConfiguration lbConfig = ConfigHolder.getInstance().getLbConfig();
		
		lbConfig.removeServiceConfiguration(domain, subDomain);
		
	}
    
    private void createOrUpdateServiceConfig(ClusterDomain cluster, HostContext ctxt) {
        LoadBalancerConfiguration lbConfig = ConfigHolder.getInstance().getLbConfig();
        
        String domain = cluster.getDomain();
        String subDomain = cluster.getSubDomain();
        
        if (subDomain == null || subDomain.isEmpty()) {
            // uses default sub domain
            subDomain = Constants.DEFAULT_SUB_DOMAIN;
        }
        
        String hostName = cluster.getHostName();
        String tenantRange = cluster.getTenantRange();
        int minInstances = cluster.getMinInstances();
        int maxInstances = cluster.getMaxInstances();
        String service = cluster.getServiceName();
        int maxRequestsPerSecond = cluster.getMaxRequestsPerSecond();
    	int roundsToAverage = cluster.getRoundsToAverage(); 
    	double alarmingUpperRate = cluster.getAlarmingUpperRate();
    	double alarmingLowerRate = cluster.getAlarmingLowerRate();
    	double scaleDownFactor = cluster.getScaleDownFactor();
        
        ServiceConfiguration serviceConfig ;
        
        if((serviceConfig = lbConfig.getServiceConfig(domain, subDomain)) == null){
            serviceConfig = lbConfig.new ServiceConfiguration();
        }
        
        // we simply override the attributes with new values
        serviceConfig.setDomain(domain);
        serviceConfig.setSub_domain(subDomain);
        serviceConfig.setTenant_range(tenantRange);
        serviceConfig.setHosts(hostName);
        serviceConfig.setMin_app_instances(minInstances);
        serviceConfig.setMax_app_instances(maxInstances);
        serviceConfig.setMax_requests_per_second(maxRequestsPerSecond);
        serviceConfig.setRounds_to_average(roundsToAverage);
        serviceConfig.setAlarming_upper_rate(alarmingUpperRate);
        serviceConfig.setAlarming_lower_rate(alarmingLowerRate);
        serviceConfig.setScale_down_factor(scaleDownFactor);
        
        // add to host name tracker
        lbConfig.addToHostNameTrackerMap(service, serviceConfig.getHosts());
        
        // add to host contexts
        lbConfig.addToHostContextMap(hostName, ctxt);
        
        // finally add this service config
        lbConfig.addServiceConfiguration(serviceConfig);
    }

    protected HostContext createGroupMgtAgentIfNotExists(ClusterDomain cluster) {
        
        String domain = cluster.getDomain();
        String subDomain = cluster.getSubDomain();
        String hostName = cluster.getHostName();
        String tenantRange = cluster.getTenantRange();

        // sub domain can be null, but others can't
        if (domain == null || hostName == null || tenantRange == null) {
            String msg =
                         "Invalid value/s detected - domain: " + domain + "\n host name: " +
                                 hostName + "\n tenant range: " + tenantRange;
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (subDomain == null || subDomain.isEmpty()) {
            // uses default sub domain
            subDomain = Constants.DEFAULT_SUB_DOMAIN;
        }

        ClusteringAgent clusteringAgent = null;

        try {
            clusteringAgent =
                              ConfigHolder.getInstance().getAxisConfiguration()
                                          .getClusteringAgent();

        } catch (Exception e) {
            String msg = "Failed to retrieve Clustering Agent.";
            log.error(msg, e);
            throw new SynapseException(msg, e);

        }

        if (clusteringAgent == null) {
            String msg = "Clustering Agent is null.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        /*
         * Add Group Management Agent if one is not already present for this domain and sub
         * domain
         */

        if (clusteringAgent.getGroupManagementAgent(domain, subDomain) == null) {
            clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(subDomain),
                                                    domain, subDomain,-1);

            if (log.isDebugEnabled()) {
                log.debug("Group management agent added to cluster domain: " + domain +
                          " and sub domain: " + subDomain);
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Group management agent is already available for cluster domain: " +
                          domain + " and sub domain: " + subDomain);
            }
        }

        TenantLoadBalanceMembershipHandler handler =
                                                     ConfigHolder.getInstance()
                                                                 .getTenantLoadBalanceMembershipHandler();

        if (handler == null) {
            String msg = "TenantLoadBalanceMembershipHandler is null. Thus, We cannot proceed.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        HostContext hostCtxt;

        // if there's an already registered HostContext use it
        if((hostCtxt = handler.getHostContext(hostName)) == null){
            hostCtxt = new HostContext(hostName);
        }
        
        List<TenantDomainContext> ctxts;
        ctxts = new ArrayList<TenantDomainContext>(hostCtxt.getTenantDomainContexts());

        // default value is super tenant mode - which is defined by tenant id 0, in this context
        int tenantId = 0;
        if(!"*".equals(tenantRange)){
        	tenantId = Integer.parseInt(tenantRange);
        }
                
        ctxts.add(new TenantDomainContext(tenantId, domain, subDomain));

        hostCtxt.addTenantDomainContexts(ctxts);

        handler.addHostContext(hostCtxt);

        return hostCtxt;
    }

}
