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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

public class ClusterRemovedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ClusterRemovedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (ClusterRemovedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized())
                return false;

            // Parse complete message and build event
            ClusterRemovedEvent event = (ClusterRemovedEvent) MessagingUtil.jsonToObject(message, ClusterRemovedEvent.class);

            TopologyUpdater.acquireWriteLockForService(event.getServiceName());
            try {
                return doProcess(event, topology);

            } finally {
                TopologyUpdater.releaseWriteLockForService(event.getServiceName());
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess(ClusterRemovedEvent event, Topology topology) {

        String serviceName = event.getServiceName();
        String clusterId = event.getClusterId();

        // Apply service filter
        if (TopologyServiceFilter.apply(serviceName)) {
            return false;
        }

        // Apply cluster filter
        if (TopologyClusterFilter.apply(clusterId)) {
            return false;
        }

        // Validate event against the existing topology
        Service service = topology.getService(event.getServiceName());
        if (service == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Service does not exist: [service] %s", event.getServiceName()));
            }
            return false;
        }

        // Notify event listeners before removing the cluster object
        notifyEventListeners(event);

        if (!service.clusterExists(event.getClusterId())) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster does not exist: [service] %s [cluster] %s",
                        event.getServiceName(),
                        event.getClusterId()));
            }
        } else {

            // Apply changes to the topology
            service.removeCluster(event.getClusterId());

            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster removed from service: [service] %s [cluster] %s",
                        event.getServiceName(), event.getClusterId()));
            }
        }

        return true;
    }
}
