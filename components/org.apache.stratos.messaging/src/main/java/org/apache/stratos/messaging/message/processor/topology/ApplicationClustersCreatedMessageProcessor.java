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
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ApplicationClustersCreatedEvent;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.List;

/**
 * This will process the clusters and add them to relevant service.
 */
public class ApplicationClustersCreatedMessageProcessor extends MessageProcessor {
    private static final Log log = LogFactory.getLog(ApplicationClustersCreatedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;
        if (ApplicationClustersCreatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            ApplicationClustersCreatedEvent event = (ApplicationClustersCreatedEvent) MessagingUtil.
                    jsonToObject(message, ApplicationClustersCreatedEvent.class);
            return doProcess(event, topology);


        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess(ApplicationClustersCreatedEvent event, Topology topology) {
        List<Cluster> clusters = event.getClusterList();

        for (Cluster cluster : clusters) {
            String serviceName = cluster.getServiceName();
            String clusterId = cluster.getClusterId();
            TopologyUpdater.acquireWriteLockForService(serviceName);
            try {

                // Apply service filter
                if (TopologyServiceFilter.apply(serviceName)) {
                    continue;
                }

                // Apply cluster filter
                if (TopologyClusterFilter.apply(clusterId)) {
                    continue;
                }

                // Validate event against the existing topology
                Service service = topology.getService(serviceName);
                if (service == null) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Service does not exist: [service] %s",
                                serviceName));
                    }
                    return false;
                }
                if (service.clusterExists(clusterId)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Cluster already exists in service: [service] %s " +
                                        "[cluster] %s", serviceName,
                                clusterId));
                    }
                } else {

                    // Apply changes to the topology
                    service.addCluster(cluster);
                    topology.addToCluterMap(cluster);
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Cluster created: %s",
                                cluster.toString()));
                    }
                }

            } finally {
                TopologyUpdater.releaseWriteLockForService(serviceName);
            }
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;
    }
}
