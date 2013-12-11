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
package org.apache.stratos.autoscaler.internal;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageDelegator;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageReceiver;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.rule.ExecutorTaskScheduler;
import org.apache.stratos.autoscaler.topology.AutoscalerTopologyReceiver;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

/**
* @scr.component name=org.apache.stratos.autoscaler.internal.AutoscalerServerComponent"
* immediate="true"
*
* @scr.reference name="registry.service"
*                interface=
*                "org.wso2.carbon.registry.core.service.RegistryService"
*                cardinality="1..1" policy="dynamic" bind="setRegistryService"
*                unbind="unsetRegistryService"
*/

public class AutoscalerServerComponent {

    private static final Log log = LogFactory.getLog(AutoscalerServerComponent.class);
    private RegistryManager registryManager;

    protected void activate(ComponentContext componentContext) throws Exception {

        // Subscribe to all topics
//        TopicSubscriber topologyTopicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
//        topologyTopicSubscriber.setMessageListener(new TopologyEventMessageReceiver());
//        Thread topologyTopicSubscriberThread = new Thread(topologyTopicSubscriber);
//        topologyTopicSubscriberThread.start();
//        if (log.isDebugEnabled()) {
//            log.debug("Topology event message receiver thread started");
//        }
//
//        TopologyEventMessageDelegator tropologyEventMessageDelegator = new TopologyEventMessageDelegator();
//        Thread tropologyDelegatorThread = new Thread(tropologyEventMessageDelegator);
//        tropologyDelegatorThread.start();

        
        
        // Adding the registry stored AS policies to the information model.
        
        try {
			Thread th = new Thread(new AutoscalerTopologyReceiver());
			th.start();
			if (log.isDebugEnabled()) {
				log.debug("Topology message processor thread started");
			}
			TopicSubscriber healthStatTopicSubscriber = new TopicSubscriber(
					Constants.HEALTH_STAT_TOPIC);
			healthStatTopicSubscriber
					.setMessageListener(new HealthEventMessageReceiver());
			Thread healthStatTopicSubscriberThread = new Thread(
					healthStatTopicSubscriber);
			healthStatTopicSubscriberThread.start();
			if (log.isDebugEnabled()) {
				log.debug("Health Stat event message receiver thread started");
			}
			HealthEventMessageDelegator healthEventMessageDelegator = new HealthEventMessageDelegator();
			Thread healthDelegatorThread = new Thread(
					healthEventMessageDelegator);
			healthDelegatorThread.start();
			if (log.isDebugEnabled()) {
				log.debug("Health message processor thread started");
			}
			// Start scheduler for running rules
			ExecutorTaskScheduler executor = new ExecutorTaskScheduler();
			Thread executorThread = new Thread(executor);
			executorThread.start();
			if (log.isDebugEnabled()) {
				log.debug("Rules executor thread started");
			}
			this.registryManager = RegistryManager.getInstance();
			// Adding the registry stored partitions to the information model.
			ArrayList<Partition> partitions = this.retreivePartitions();
			Iterator<Partition> it = partitions.iterator();
			while (it.hasNext()) {
				Partition par = it.next();
				PartitionManager.getInstance().addPartitionToInformationModel(
						par);
			}
			ArrayList<AutoscalePolicy> asPolicies = this.retreiveASPolicies();
			Iterator<AutoscalePolicy> asItr = asPolicies.iterator();
			while (asItr.hasNext()) {
				AutoscalePolicy asPolicy = asItr.next();
				PolicyManager.getInstance().addASPolicyToInformationModel(asPolicy);
			}
			
			ArrayList<DeploymentPolicy> depPolicies = this.retreiveDeploymentPolicies();
			Iterator<DeploymentPolicy> depItr = depPolicies.iterator();
			while (depItr.hasNext()) {
				DeploymentPolicy depPolicy = depItr.next();
				PolicyManager.getInstance().addDeploymentPolicyToInformationModel(depPolicy);
			}
			
			if (log.isInfoEnabled()) {
				log.info("Autoscaler Server Component activated");
			}
		} catch (Throwable e) {
			log.error("Error in Activating the AS component " + e.getStackTrace());
		}
    }
    
    protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Registry Service");
		}
		try {
	        ServiceReferenceHolder.getInstance().setRegistry(registryService.getGovernanceSystemRegistry());
        } catch (RegistryException e) {
        	String msg = "Failed when retrieving Governance System Registry.";
        	log.error(msg, e);
        	throw new AutoScalerException(msg, e);
        }
	 }

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
            log.debug("Unsetting the Registry Service");
        }
        ServiceReferenceHolder.getInstance().setRegistry(null);
	}
	
	private ArrayList<Partition> retreivePartitions(){
		ArrayList<Partition> partitionList = new ArrayList<Partition>();
		 String [] partitionsResourceList = (String [])registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE);
		 
	        if (partitionsResourceList != null) {
				for (String resourcePath : partitionsResourceList) {
					Object partition = registryManager.retrieve(resourcePath);

					if (partition != null) {
						try {

							Object dataObj = Deserializer
									.deserializeFromByteArray((byte[]) partition);
							if (dataObj instanceof Partition) {
								partitionList.add((Partition) dataObj);
							} else {
								return null;
							}
						} catch (Exception e) {
							String msg = "Unable to retrieve data from Registry. Hence, any historical partitions will not get reflected.";
							log.warn(msg, e);
						}
					}
				}
			}
			return partitionList;	        
	}
	
	private ArrayList<AutoscalePolicy> retreiveASPolicies(){
		ArrayList<AutoscalePolicy> asPolicyList = new ArrayList<AutoscalePolicy>();
		 String [] partitionsResourceList = (String [])registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE);
		 
	        if (partitionsResourceList != null) {
				for (String resourcePath : partitionsResourceList) {
					Object asPolicy = registryManager.retrieve(resourcePath);

					if (asPolicy != null) {
						try {

							Object dataObj = Deserializer
									.deserializeFromByteArray((byte[]) asPolicy);
							if (dataObj instanceof AutoscalePolicy) {
								asPolicyList.add((AutoscalePolicy) dataObj);
							} else {
								return null;
							}
						} catch (Exception e) {
							String msg = "Unable to retrieve data from Registry. Hence, any historical autoscaler policies will not get reflected.";
							log.warn(msg, e);
						}
					}
				}
			}
			return asPolicyList;	        
	}
	
	private ArrayList<DeploymentPolicy> retreiveDeploymentPolicies(){
		ArrayList<DeploymentPolicy> depPolicyList = new ArrayList<DeploymentPolicy>();
		 String [] depPolicyResourceList = (String [])registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE);
		 
	        if (depPolicyResourceList != null) {
				for (String resourcePath : depPolicyResourceList) {
					Object asPolicy = registryManager.retrieve(resourcePath);

					if (asPolicy != null) {
						try {

							Object dataObj = Deserializer
									.deserializeFromByteArray((byte[]) asPolicy);
							if (dataObj instanceof DeploymentPolicy) {
								depPolicyList.add((DeploymentPolicy) dataObj);
							} else {
								return null;
							}
						} catch (Exception e) {
							String msg = "Unable to retrieve data from Registry. Hence, any historical deployment policies will not get reflected.";
							log.warn(msg, e);
						}
					}
				}
			}
			return depPolicyList;	        
	}
	
	
	
}