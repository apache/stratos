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
import org.apache.stratos.messaging.message.filter.topology.ClusterFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.filter.topology.ServiceFilter;
import org.apache.stratos.messaging.util.Util;

public class ClusterCreatedEventProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ClusterCreatedEventProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology)object;

        if (ClusterCreatedEvent.class.getName().equals(type)) {
            // Parse complete message and build event
            ClusterCreatedEvent event = (ClusterCreatedEvent) Util.jsonToObject(message, ClusterCreatedEvent.class);

            // Apply service filter
            if(ServiceFilter.getInstance().isActive()) {
                if(ServiceFilter.getInstance().excluded(event.getServiceName())) {
                    // Service is excluded, do not update topology or fire event
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Service is excluded: [service] %s", event.getServiceName()));
                    }
                    return true;
                }
            }

            // Apply cluster filter
            if(ClusterFilter.getInstance().isActive()) {
                if(ClusterFilter.getInstance().excluded(event.getClusterId())) {
                    // Cluster is excluded, do not update topology or fire event
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Cluster is excluded: [cluster] %s", event.getClusterId()));
                    }
                    return true;
                }
            }

            // Validate event properties
            if(StringUtils.isBlank(event.getHostName())) {
                throw new RuntimeException("Hostname not found in cluster created event");
            }
            // Validate event against the existing topology
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                throw new RuntimeException(String.format("Service does not exist: [service] %s",
                        event.getServiceName()));
            }
            if (service.clusterExists(event.getClusterId())) {
                throw new RuntimeException(String.format("Cluster already exists in service: [service] %s [cluster] %s",
                        event.getServiceName(),
                        event.getClusterId()));
            }

            // Apply changes to the topology
            Cluster cluster = new Cluster(event.getServiceName(), event.getClusterId(), event.getAutoscalingPolicyName());
            cluster.setHostName(event.getHostName());
            cluster.setTenantRange(event.getTenantRange());

            service.addCluster(cluster);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster created: [service] %s [cluster] %s [hostname] %s",
                         event.getServiceName(), event.getClusterId(), event.getHostName()));
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
