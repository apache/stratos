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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageDelegator;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageReceiver;
import org.apache.stratos.autoscaler.rule.ExecutorTaskScheduler;
import org.apache.stratos.autoscaler.topology.AutoscalerTopologyReceiver;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

/**
 * @scr.component name=
 * "org.apache.stratos.autoscaler.internal.AutoscalerServerComponent"
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

        Thread th = new Thread(new AutoscalerTopologyReceiver());
        th.start();
        if (log.isDebugEnabled()) {
            log.debug("Topology message processor thread started");
        }

        TopicSubscriber healthStatTopicSubscriber = new TopicSubscriber(Constants.HEALTH_STAT_TOPIC);
        healthStatTopicSubscriber.setMessageListener(new HealthEventMessageReceiver());
        Thread healthStatTopicSubscriberThread = new Thread(healthStatTopicSubscriber);
        healthStatTopicSubscriberThread.start();
        if (log.isDebugEnabled()) {
            log.debug("Health Stat event message receiver thread started");
        }

        HealthEventMessageDelegator healthEventMessageDelegator = new HealthEventMessageDelegator();
        Thread healthDelegatorThread = new Thread(healthEventMessageDelegator);
        healthDelegatorThread.start();

        if (log.isDebugEnabled()) {
            log.debug("Health message processor thread started");
        }

        // Start scheduler for running rules
        ExecutorTaskScheduler executor = new ExecutorTaskScheduler();
        Thread executorThread = new Thread(executor);
        executorThread.start();
        if(log.isDebugEnabled()) {
            log.debug("Rules executor thread started");
        }

        if(log.isInfoEnabled()) {
            log.info("Autoscaler Server Component activated");
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
        	//throw new CloudControllerException(msg, e);
        }
	 }

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
            log.debug("Unsetting the Registry Service");
        }
        ServiceReferenceHolder.getInstance().setRegistry(null);
	}
}