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

package org.apache.stratos.adc.topology.mgt.builder;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.topology.mgt.group.mgt.GroupMgtAgentBuilder;
import org.apache.stratos.adc.topology.mgt.util.ConfigHolder;
import org.apache.stratos.lb.common.conf.LoadBalancerConfiguration;
import org.apache.stratos.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.apache.stratos.lb.common.conf.structure.Node;
import org.apache.stratos.lb.common.conf.structure.NodeBuilder;


public class TopologySyncher implements Runnable {

	@SuppressWarnings("rawtypes")
    private BlockingQueue sharedQueue;
	private static final Log log = LogFactory.getLog(TopologySyncher.class);
	
	public TopologySyncher(@SuppressWarnings("rawtypes") BlockingQueue queue){
		
		sharedQueue = queue;
		
	}
	
	public void run() {

	    LoadBalancerConfiguration lbconfig = LoadBalancerConfiguration.getInstance();
	    
	    //FIXME Currently there has to be at least one dummy cluster defined in the loadbalancer conf
	    // in order to proper initialization of TribesClusteringAgent.
	    generateGroupMgtAgents(lbconfig);
	    
        while (true) {
            try {

                Object obj;
                String msg = null;

                obj = sharedQueue.take();
                msg = (String) obj;

                ConfigHolder data = ConfigHolder.getInstance();

                Node topologyNode = NodeBuilder.buildNode(msg);

                List<ServiceConfiguration> currentServiceConfigs = lbconfig.createServicesConfig(topologyNode);

                data.setServiceConfigs(lbconfig.getServiceNameToServiceConfigurations());
                generateGroupMgtAgents(lbconfig);
                resetGroupMgtAgents(lbconfig, currentServiceConfigs);

            } catch (InterruptedException ignore) {
            }
        }

	}

    /**
     * @param lbconfig
     */
    private void generateGroupMgtAgents(LoadBalancerConfiguration lbconfig) {
        for (List<ServiceConfiguration> serviceConfigsList : lbconfig.getServiceNameToServiceConfigurations()
                                                                     .values()) {

        	for (ServiceConfiguration serviceConfiguration : serviceConfigsList) {
        		GroupMgtAgentBuilder.createGroupMgtAgent(serviceConfiguration.getDomain(),
        		                                         serviceConfiguration.getSubDomain());
        	}
        }
    }
    
    private void resetGroupMgtAgents(LoadBalancerConfiguration lbConfig,
        List<ServiceConfiguration> currentServiceConfigs) {

        for (Iterator<?> iterator = lbConfig.getServiceConfigurations().values().iterator(); iterator.hasNext();) {
            @SuppressWarnings("unchecked")
            Map<String, ServiceConfiguration> valuesMap = (Map<String, ServiceConfiguration>) iterator.next();

            for (Iterator<ServiceConfiguration> iterator2 = valuesMap.values().iterator(); iterator2.hasNext();) {
                ServiceConfiguration oldServiceConfig = (ServiceConfiguration) iterator2.next();

                if (!currentServiceConfigs.contains(oldServiceConfig)) {
                    // if the ServiceConfiguration is not there any more in the latest topology
                    lbConfig.removeServiceConfiguration(oldServiceConfig.getDomain(), oldServiceConfig.getSubDomain());
                    GroupMgtAgentBuilder.resetGroupMgtAgent(oldServiceConfig.getDomain(), oldServiceConfig.getSubDomain());
                }
            }
        }
    }

}
