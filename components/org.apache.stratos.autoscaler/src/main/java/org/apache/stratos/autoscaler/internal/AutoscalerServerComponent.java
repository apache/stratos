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
import org.apache.stratos.autoscaler.message.receiver.TopologyManager;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageDelegator;
import org.apache.stratos.autoscaler.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.autoscaler.rule.ExecutorTaskScheduler;
import org.osgi.service.component.ComponentContext;

/**
 *
 * @scr.component name=
 *                "org.apache.stratos.autoscaler.internal.AutoscalerServerComponent"
 *                immediate="true"
 */

public class AutoscalerServerComponent {
	
    private static final Log log = LogFactory.getLog(AutoscalerServerComponent.class);

	protected void activate(ComponentContext componentContext) throws Exception {

		log.info("Autoscaler Server Component activated");

		Thread executorThread = new Thread() {
			public void run() {

				// Subscribe to all topics
				TopologyManager topologyManager = new TopologyManager();
				topologyManager.subscribeAllTopics();
				if (log.isDebugEnabled()) {
					log.debug("Topology event message receiver thread started");
				}
				TopologyEventMessageDelegator tropologyEventMessageDelegator = new TopologyEventMessageDelegator();
				Thread tropologyDelegatorThread = new Thread(tropologyEventMessageDelegator);
				tropologyDelegatorThread.start();
				
				if (log.isDebugEnabled()) {
		               log.debug("Topology message processor thread started");
		         }
				
				HealthEventMessageDelegator healthEventMessageDelegator = new HealthEventMessageDelegator();
				Thread healthDelegatorThread = new Thread(healthEventMessageDelegator);
				healthDelegatorThread.start();
				
				if (log.isDebugEnabled()) {
		               log.debug("Health message processor thread started");
		         }

				// Start scheduler for running rules
				ExecutorTaskScheduler executor = new ExecutorTaskScheduler();
				executor.start();
				log.info("Rules executor started");

			};
		};
		executorThread.start();
		
	}


}