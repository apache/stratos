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
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ApplicationClustersRemovedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.Util;

import java.util.Set;

public class ApplicationClustersRemovedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationClustersRemovedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        Topology topology = (Topology) object;

        if (ApplicationClustersRemovedEvent.class.getName().equals(type)) {
            if (!topology.isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            ApplicationClustersRemovedEvent event = (ApplicationClustersRemovedEvent) Util.
                    jsonToObject(message, ApplicationClustersRemovedEvent.class);

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

    private boolean doProcess (ApplicationClustersRemovedEvent event,Topology topology) {

        Set<ClusterDataHolder> clusterData = event.getClusterData();
        if (clusterData != null) {
            for (ClusterDataHolder aClusterData : clusterData) {
                TopologyUpdater.acquireWriteLockForService(aClusterData.getServiceType());

                try {
                    Service aService = topology.getService(aClusterData.getServiceType());
                    if (aService != null) {
                        aService.removeCluster(aClusterData.getClusterId());
                    } else {
                        log.warn("Service " + aClusterData.getServiceType() + " not found, unable to remove Cluster " + aClusterData.getClusterId());
                    }

                } finally {
                    TopologyUpdater.releaseWriteLockForService(aClusterData.getServiceType());
                }
            }

        } else {
            if(log.isDebugEnabled()) {
                log.debug("No cluster data found in application " + event.getAppId() + " to remove from Topology");
            }
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;
    }
}
