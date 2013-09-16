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
package org.apache.stratos.lb.endpoint.builder;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.stratos.lb.endpoint.util.ConfigHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.stratos.lb.common.conf.LoadBalancerConfiguration;
import org.apache.stratos.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.apache.stratos.lb.common.conf.structure.Node;
import org.apache.stratos.lb.common.conf.structure.NodeBuilder;
import org.apache.stratos.lb.common.conf.util.HostContext;
import org.apache.stratos.lb.common.conf.util.TenantDomainContext;
import org.apache.stratos.lb.endpoint.TenantLoadBalanceMembershipHandler;
import org.apache.stratos.lb.endpoint.group.mgt.GroupMgtAgentBuilder;

/**
 * A Thread, which is responsible for making a sense out of a message received for
 * ELB via topology synchronization.
 */
public class TopologySyncher implements Runnable {

    private static final Log log = LogFactory.getLog(TopologySyncher.class);

    /*
     * This is a reference to sharedTopologyQueue ConfigHolder.
     */
    private BlockingQueue<String> sharedQueue;

    public TopologySyncher(BlockingQueue<String> queue) {

        sharedQueue = queue;

    }

    @Override
	public void run() {
		// grab the lb configuration instance
		LoadBalancerConfiguration lbconfig = LoadBalancerConfiguration
				.getInstance();

		// FIXME Currently there has to be at least one dummy cluster defined in
		// the loadbalancer
		// conf
		// in order to proper initialization of TribesClusteringAgent.
		generateGroupMgtAgents(lbconfig);

		// this thread should run for ever, untill terminated.
		while (true) {
			try {

				// grabs a message or waits till the queue is non-empty
				String message = sharedQueue.take();

				// build the nginx format of this message, and get the Node
				// object
				Node topologyNode = NodeBuilder.buildNode(message);

				// create new service configurations
				List<ServiceConfiguration> currentServiceConfigs = lbconfig
						.createServicesConfig(topologyNode);

				generateGroupMgtAgents(lbconfig);

				removeGroupMgtAgents(lbconfig, currentServiceConfigs);

			} catch (InterruptedException ignore) {
			}
		}

	}

    private void removeGroupMgtAgents(LoadBalancerConfiguration lbConfig, List<ServiceConfiguration> currentServiceConfigs) {

        for (Iterator iterator = lbConfig.getServiceConfigurations().values().iterator(); iterator.hasNext();) {
            Map<String, ServiceConfiguration> valuesMap = (Map<String, ServiceConfiguration>) iterator.next();
            
            for (Iterator iterator2 = valuesMap.values().iterator(); iterator2.hasNext();) {
                ServiceConfiguration oldServiceConfig = (ServiceConfiguration) iterator2.next();
                
                if(!currentServiceConfigs.contains(oldServiceConfig)){
                    // if the ServiceConfiguration is not there any more in the latest topology
                    lbConfig.removeServiceConfiguration(oldServiceConfig.getDomain(), oldServiceConfig.getSubDomain());
                    GroupMgtAgentBuilder.resetGroupMgtAgent(oldServiceConfig.getDomain(), oldServiceConfig.getSubDomain());
                }
            }
        }
    }

    /**
     * @param lbconfig
     */
    private void generateGroupMgtAgents(LoadBalancerConfiguration lbconfig) {
        TenantLoadBalanceMembershipHandler handler =
            ConfigHolder.getInstance()
                .getTenantLoadBalanceMembershipHandler();

        if (handler == null) {
            String msg =
                "TenantLoadBalanceMembershipHandler is null. Thus, We cannot proceed.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        Map<String, HostContext> hostContexts = lbconfig.getHostContextMap();

        // Add the Axis2 GroupManagement agents
        if (hostContexts != null) {
            // iterate through each host context
            for (HostContext hostCtxt : hostContexts.values()) {
                // each host can has multiple Tenant Contexts, iterate through them
                for (TenantDomainContext tenantCtxt : hostCtxt
                    .getTenantDomainContexts()) {

                    String domain = tenantCtxt.getDomain();
                    String subDomain = tenantCtxt.getSubDomain();

                    // creates the group management agent
                    GroupMgtAgentBuilder.createGroupMgtAgent(domain,
                        subDomain);
                }

                // add to the handler
                handler.addHostContext(hostCtxt);
            }
        }
    }

}
