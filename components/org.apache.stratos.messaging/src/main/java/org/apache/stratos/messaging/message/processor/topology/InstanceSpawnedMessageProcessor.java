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
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.topology.InstanceSpawnedEvent;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyMemberFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.Util;

public class InstanceSpawnedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(InstanceSpawnedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (InstanceSpawnedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized())
                return false;

            // Parse complete message and build event
            InstanceSpawnedEvent event = (InstanceSpawnedEvent) Util.jsonToObject(message, InstanceSpawnedEvent.class);

            TopologyUpdater.acquireWriteLockForCluster(event.getServiceName(), event.getClusterId());
            try {
                return doProcess(event, topology);

            } finally {
                TopologyUpdater.releaseWriteLockForCluster(event.getServiceName(), event.getClusterId());
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

    private boolean doProcess (InstanceSpawnedEvent event,Topology topology){

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

        // Apply member filter
        if(TopologyMemberFilter.getInstance().isActive()) {
            if(TopologyMemberFilter.getInstance().lbClusterIdExcluded(event.getLbClusterId())) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Member is excluded: [lb-cluster-id] %s", event.getLbClusterId()));
                }
                return false;
            }
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
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cluster does not exist: [service] %s [cluster] %s",
                        event.getServiceName(), event.getClusterId()));
            }
            return false;
        }
        if (cluster.memberExists(event.getMemberId())) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Member already exists: [service] %s [cluster] %s [member] %s",
                        event.getServiceName(),
                        event.getClusterId(),
                        event.getMemberId()));
            }
        } else {

            // Apply changes to the topology
            Member member = new Member(event.getServiceName(), event.getClusterId(), event.getNetworkPartitionId(), event.getPartitionId(), event.getMemberId(), event.getInitTime());
            //member.setStatus(MemberStatus.Created);
            member.setMemberPublicIp(event.getMemberPublicIp());
            member.setMemberIp(event.getMemberIp());
            member.setLbClusterId(event.getLbClusterId());
            member.setProperties(event.getProperties());
            cluster.addMember(member);

            if (log.isInfoEnabled()) {
                log.info(String.format("Member created: [service] %s [cluster] %s [member] %s",
                        event.getServiceName(),
                        event.getClusterId(),
                        event.getMemberId()));
            }
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;
    }
}
