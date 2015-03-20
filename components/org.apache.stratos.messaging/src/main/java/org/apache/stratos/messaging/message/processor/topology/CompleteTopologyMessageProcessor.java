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
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyMemberFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.ArrayList;
import java.util.List;

public class CompleteTopologyMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompleteTopologyMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (CompleteTopologyEvent.class.getName().equals(type)) {
            // Parse complete message and build event
            CompleteTopologyEvent event = (CompleteTopologyEvent) MessagingUtil.jsonToObject(message, CompleteTopologyEvent.class);

            if (!topology.isInitialized()) {
                TopologyUpdater.acquireWriteLock();

                try {
                    doProcess(event, topology);

                } finally {
                    TopologyUpdater.releaseWriteLock();
                }
            }

            // Notify event listeners
            notifyEventListeners(event);
            return true;

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            }
            return false;
        }
    }

    private void doProcess(CompleteTopologyEvent event, Topology topology) {

        for (Service service : event.getTopology().getServices()) {
            // Apply service filter
            if (TopologyServiceFilter.apply(service.getServiceName())) {
                continue;
            }
            // Add service
            topology.addService(service);
        }

        // Apply cluster filter
        for (Service service : topology.getServices()) {
            List<Cluster> clustersToRemove = new ArrayList<Cluster>();
            for (Cluster cluster : service.getClusters()) {
                if (TopologyClusterFilter.apply(cluster.getClusterId())) {
                    clustersToRemove.add(cluster);
                }else{
                    // Add non filtered clusters to clusterId-cluster map
                    if(!topology.clusterExist(cluster.getClusterId())){
                        topology.addToCluterMap(cluster);
                    }
                }

            }
            for (Cluster cluster : clustersToRemove) {
                service.removeCluster(cluster);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Cluster is excluded: [cluster] %s",
                            cluster.getClusterId()));
                }
            }
        }

        // Apply member filter
        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) {
                List<Member> membersToRemove = new ArrayList<Member>();
                for (Member member : cluster.getMembers()) {
                    if (TopologyMemberFilter.apply(member.getLbClusterId(), member.getNetworkPartitionId())) {
                        membersToRemove.add(member);
                    }
                }
                for (Member member : membersToRemove) {
                    cluster.removeMember(member);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member is excluded: [member] %s [lb-cluster-id] %s",
                                member.getMemberId(),
                                member.getLbClusterId()));
                    }
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Topology initialized");
        }

        // Set topology initialized
        topology.setInitialized(true);
    }
}
