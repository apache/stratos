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

package org.apache.stratos.manager.lb.category;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.manager.behaviour.CartridgeMgtBehaviour;
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;

import java.util.Map;

public abstract class LoadBalancerCategory extends CartridgeMgtBehaviour {

    private String loadBalancedServiceType;
	private boolean isLoadBalancedServiceMultiTenant;
    private String deploymentPolicyName;
	private static Log log = LogFactory.getLog(LoadBalancerCategory.class);

    public String getLoadBalancedServiceType() {
        return loadBalancedServiceType;
    }

    public void setLoadBalancedServiceType(String loadBalancedServiceType) {
        this.loadBalancedServiceType = loadBalancedServiceType;
    }
    
	public PayloadData create(String alias, Cluster cluster, Subscriber subscriber, Repository repository, CartridgeInfo cartridgeInfo,
                              String subscriptionKey, Map<String, String> customPayloadEntries)
            throws ADCException, AlreadySubscribedException {

		String clusterId;

		if (isLoadBalancedServiceMultiTenant) {
			// the load balancer should be already up and running from service
			// cluster deployment

			// get the cluster domain and host name from deployed Service

			Service deployedLBService;
			try {
				deployedLBService = new DataInsertionAndRetrievalManager()
						.getService(cartridgeInfo.getType());

			} catch (PersistenceManagerException e) {
				String errorMsg = "Error in checking if Service is available is PersistenceManager";
				log.error(errorMsg, e);
				throw new ADCException(errorMsg, e);
			}

			if (deployedLBService == null) {
				String errorMsg = "There is no deployed Service for type "
						+ cartridgeInfo.getType();
				log.error(errorMsg);
				throw new ADCException(errorMsg);
			}

			if(log.isDebugEnabled()){
				log.debug(" Setting cluster Domain : " + deployedLBService.getClusterId());
				log.debug(" Setting Host Name : " + deployedLBService.getHostName());
			}
			
			// set the cluster and hostname
			cluster.setClusterDomain(deployedLBService.getClusterId());
			cluster.setHostName(deployedLBService.getHostName());

			return null;
		} else {
            // set cluster domain
			cluster.setClusterDomain(generateClusterId(alias, cartridgeInfo.getType()));
			// set hostname
			cluster.setHostName(generateHostName(alias, cartridgeInfo.getHostName()));
			
			return createPayload(cartridgeInfo, subscriptionKey, subscriber,
					cluster, repository, alias, customPayloadEntries);
		}

		
	}

	public boolean isLoadBalancedServiceMultiTenant() {
		return isLoadBalancedServiceMultiTenant;
	}

	public void setLoadBalancedServiceMultiTenant(
			boolean isLoadBalancedServiceMultiTenant) {
		this.isLoadBalancedServiceMultiTenant = isLoadBalancedServiceMultiTenant;
	}


    public String getDeploymentPolicyName() {
        return deploymentPolicyName;
    }

    public void setDeploymentPolicyName(String deploymentPolicyName) {
        this.deploymentPolicyName = deploymentPolicyName;
    }
}
