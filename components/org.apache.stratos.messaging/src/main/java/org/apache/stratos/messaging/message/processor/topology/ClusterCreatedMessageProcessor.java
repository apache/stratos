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
package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.Util;

public class ClusterCreatedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ClusterCreatedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (ClusterCreatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized())
                return false;

            // Parse complete message and build event
            ClusterCreatedEvent event = (ClusterCreatedEvent) Util.jsonToObject(message, ClusterCreatedEvent.class);

            // Apply service filter
            if (TopologyServiceFilter.getInstance().isActive()) {
                if (TopologyServiceFilter.getInstance().serviceNameExcluded(event.getServiceName())) {
                    // Service is excluded, do not update topology or fire event
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Service is excluded: [service] %s", event.getServiceName()));
                    }
                    return false;
                }
            }

            // Apply cluster filter
            if (TopologyClusterFilter.getInstance().isActive()) {
                if (TopologyClusterFilter.getInstance().clusterIdExcluded(event.getClusterId())) {
                    // Cluster is excluded, do not update topology or fire event
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Cluster is excluded: [cluster] %s", event.getClusterId()));
                    }
                    return false;
                }
            }
            
            Cluster cluster = event.getCluster();
            
            if(cluster == null) {
                String msg = "Cluster object of Cluster Created Event is null.";
                log.error(msg);
                throw new RuntimeException(msg);
            }

            // Validate event properties
            if (cluster.getHostNames().isEmpty()) {
                throw new RuntimeException("Host name/s not found in cluster created event");
            }
            // Validate event against the existing topology
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Service does not exist: [service] %s",
                            event.getServiceName()));
                }
                return false;
            }
            if (service.clusterExists(event.getClusterId())) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Cluster already exists in service: [service] %s [cluster] %s", event.getServiceName(),
                            event.getClusterId()));
                }
                return false;
            }

            // Apply changes to the topology
            service.addCluster(cluster);
            if (log.isInfoEnabled()) {
                log.info("Cluster created: "+cluster.toString());
            }

            // Notify event listeners
            notifyEventListeners(event);
            return true;

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }
}
