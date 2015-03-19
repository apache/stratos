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
package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.topology.ClusterInstanceActivatedEvent;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.List;

/**
 * This processor will act upon the cluster activated event
 */
public class ClusterInstanceActivatedProcessor extends MessageProcessor {
    private static final Log log = LogFactory.getLog(ClusterInstanceActivatedProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        Topology topology = (Topology) object;

        if (ClusterInstanceActivatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            ClusterInstanceActivatedEvent event = (ClusterInstanceActivatedEvent) MessagingUtil.
                    jsonToObject(message, ClusterInstanceActivatedEvent.class);

            String clusterId = event.getClusterId();
            TopologyUpdater.acquireWriteLockForCluster(event.getServiceName(), clusterId);
            try {
                return doProcess(event, topology);

            } finally {
                TopologyUpdater.releaseWriteLockForCluster(event.getServiceName(), clusterId);
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

    private boolean doProcess(ClusterInstanceActivatedEvent event, Topology topology) {

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
                log.warn(String.format("Service does not exist: [service] %s",
                        event.getServiceName()));
            }
            return false;
        }
        Cluster cluster = service.getCluster(event.getClusterId());

        if (cluster == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cluster not exists in service: [service] %s [cluster] %s", event.getServiceName(),
                        event.getClusterId()));
                return false;
            }
        } else {
            // Apply changes to the topology
            List<KubernetesService> kubernetesServices = event.getKubernetesServices();
            if (kubernetesServices != null) {
                // Set kubernetes services
                cluster.setKubernetesServices(kubernetesServices);
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster instance context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return false;
            }
            ClusterStatus status = ClusterStatus.Active;
            if (!context.isStateTransitionValid(status)) {
                log.error("Invalid state transition from " + context.getStatus() + " to " + status);
            }
            context.setStatus(status);
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;
    }

}
