/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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
package org.apache.stratos.messaging.message.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.util.Util;

public class ClusterCreatedEventProcessor implements MessageProcessor {

	private static final Log log = LogFactory.getLog(ClusterCreatedEventProcessor.class);
	private MessageProcessor nextMsgProcessor;

	@Override
	public void setNext(MessageProcessor nextProcessor) {
		nextMsgProcessor = nextProcessor;
	}

	@Override
	public boolean process(String type, String message, Topology topology) {
		try {
			if (ClusterCreatedEvent.class.getName().equals(type)) {
				// Parse complete message and build event
				ClusterCreatedEvent event =
				                            (ClusterCreatedEvent) Util.jsonToObject(message,
				                                                                    ClusterCreatedEvent.class);
				// Validate event against the existing topology
				Service service = topology.getService(event.getServiceName());
				if (service == null) {
					throw new RuntimeException(String.format("Service %s does not exist",
					                                         event.getServiceName()));
				}
				if (service.clusterExists(event.getClusterId())) {
					throw new RuntimeException(
					                           String.format("Cluster %s already exists in service %s",
					                                         event.getClusterId(),
					                                         event.getServiceName()));
				}

				// Apply changes to the topology
				Cluster cluster = new Cluster();
				cluster.setClusterId(event.getClusterId());
				cluster.setHostName(event.getHostName());
				cluster.setTenantRange(event.getTenantRange());

				service.addCluster(cluster);
				log.info(String.format("Cluster %s created for service %s", event.getClusterId(),
				                       event.getServiceName()));

				return true;

			} else {
				if (nextMsgProcessor != null) {
					// ask the next processor to take care of the message.
					return nextMsgProcessor.process(type, message, topology);
				}
			}
		} catch (Exception e) {
			if (nextMsgProcessor != null) {
				// ask the next processor to take care of the message.
				return nextMsgProcessor.process(type, message, topology);
			} else {
				throw new RuntimeException(
				                           String.format("Failed to process the message: %s of type %s using any of the available processors.",
				                                         message, type));
			}
		}
		return false;
	}

}
