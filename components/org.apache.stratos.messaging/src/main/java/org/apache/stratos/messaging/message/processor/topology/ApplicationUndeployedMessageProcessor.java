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
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.topology.ApplicationUndeployedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.Util;

import java.util.Set;

public class ApplicationUndeployedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationUndeployedMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        Topology topology = (Topology) object;

        if (ApplicationUndeployedEvent.class.getName().equals(type)) {
            if (!topology.isInitialized()) {
                return false;
            }

            ApplicationUndeployedEvent event = (ApplicationUndeployedEvent)
                    Util.jsonToObject(message, ApplicationUndeployedEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to ApplicationUndeployedEvent");
                return false;
            }

            // get write lock for the application and relevant Clusters
            TopologyUpdater.acquireWriteLockForApplication(event.getApplicationId());
            Set<ClusterDataHolder> clusterDataHolders = event.getClusterData();
            if (clusterDataHolders != null) {
                for (ClusterDataHolder clusterData : clusterDataHolders) {
                    TopologyUpdater.acquireWriteLockForCluster(clusterData.getServiceType(),
                            clusterData.getClusterId());
                }
            }

            try {
                return doProcess(event, topology);

            } finally {
                // remove locks
                if (clusterDataHolders != null) {
                    for (ClusterDataHolder clusterData : clusterDataHolders) {
                        TopologyUpdater.releaseWriteLockForCluster(clusterData.getServiceType(),
                                clusterData.getClusterId());
                    }
                }
                TopologyUpdater.releaseWriteLockForApplication(event.getApplicationId());
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            } else {
                throw new RuntimeException(String.format
                    ("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess (ApplicationUndeployedEvent event, Topology topology) {

        // update the application status to Terminating
        Application application = topology.getApplication(event.getApplicationId());
        // check and update application status to 'Terminating'
        if (!application.isStateTransitionValid(ApplicationStatus.Terminating)) {
            log.error("Invalid state transfer from " + application.getStatus() + " to " + ApplicationStatus.Terminating);
        }
        // for now anyway update the status forcefully
        application.setStatus(ApplicationStatus.Terminating);
        log.info("Application " + event.getApplicationId() + "'s status updated to " + ApplicationStatus.Terminating);

        // update all the Clusters' statuses to 'Terminating'
        Set<ClusterDataHolder> clusterData = application.getClusterDataRecursively();
        // update the Cluster statuses to Terminating
        for (ClusterDataHolder clusterDataHolder : clusterData) {
            Service service = topology.getService(clusterDataHolder.getServiceType());
            if (service != null) {
                Cluster aCluster = service.getCluster(clusterDataHolder.getClusterId());
                if (aCluster != null) {
                    // validate state transition
                    if (!aCluster.isStateTransitionValid(ClusterStatus.Terminating)) {
                        log.error("Invalid state transfer from " + aCluster.getStatus() + " to "
                                + ClusterStatus.Terminating);
                    }
                    // for now anyway update the status forcefully
                    aCluster.setStatus(ClusterStatus.Terminating);
                    log.info("Cluster " + clusterDataHolder.getClusterId() + "'s status updated to "
                            + ClusterStatus.Terminating + " successfully");

                } else {
                    log.warn("Unable to find Cluster with cluster id " + clusterDataHolder.getClusterId() +
                            " in Topology");
                }

            } else {
                log.warn("Unable to remove cluster with cluster id: " + clusterDataHolder.getClusterId() + " from Topology, " +
                        " associated Service [ " + clusterDataHolder.getServiceType() + " ] npt found");
            }
        }

        notifyEventListeners(event);
        return true;
    }
}
