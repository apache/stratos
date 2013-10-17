/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.lb.endpoint.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.EventMessageHeader;
import org.apache.stratos.messaging.message.MessageProcessor;
import org.apache.stratos.messaging.message.TopologyEventMessage;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class TopologyEventMessageProcessor extends MessageProcessor implements Runnable {

    private static final Log log = LogFactory.getLog(TopologyEventMessageProcessor.class);

    @Override
    public void run() {
        if (log.isInfoEnabled()) {
            log.info("Topology event message processor started");
            log.info("Waiting for the complete topology event message...");
        }
        while (true) {
            try {
                // First take the complete topology event
                String json = TopologyEventQueue.getInstance().take();

                // Read message header and identify event
                EventMessageHeader header = readHeader(json);
                if (header.getEventClassName().equals(CompleteTopologyEvent.class.getName())) {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Event message received from queue: %s", header.getEventClassName()));
                    }
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    CompleteTopologyEvent event = (CompleteTopologyEvent) jsonToObject(eventMessage.getBody(), CompleteTopologyEvent.class);
                    TopologyManager.getTopology().addServices(event.getTopology().getServices());
                    if (log.isInfoEnabled()) {
                        log.info("Topology initialized");
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (true) {
            try {
                String json = TopologyEventQueue.getInstance().take();

                // Read message header and identify event
                EventMessageHeader header = readHeader(json);
                if (log.isInfoEnabled()) {
                    log.info(String.format("Event message received from queue: %s", header.getEventClassName()));
                }

                if (header.getEventClassName().equals(ServiceCreatedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    ServiceCreatedEvent event = (ServiceCreatedEvent) jsonToObject(eventMessage.getBody(), ServiceCreatedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();
                        if (TopologyManager.getTopology().serviceExists(event.getServiceName())) {
                            throw new RuntimeException(String.format("Service %s already exists", event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        Service service = new Service();
                        service.setServiceName(event.getServiceName());
                        TopologyManager.acquireWriteLock();
                        TopologyManager.getTopology().addService(service);
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Service %s created", event.getServiceName()));
                    }
                } else if (header.getEventClassName().equals(ServiceRemovedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    ServiceRemovedEvent event = (ServiceRemovedEvent) jsonToObject(eventMessage.getBody(), ServiceRemovedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        TopologyManager.getTopology().removeService(service);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Service %s removed", event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                } else if (header.getEventClassName().equals(ClusterCreatedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    ClusterCreatedEvent event = (ClusterCreatedEvent) jsonToObject(eventMessage.getBody(), ClusterCreatedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                        if (service.clusterExists(event.getClusterId())) {
                            throw new RuntimeException(String.format("Cluster %s already exists in service %s", event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();
                        Cluster cluster = new Cluster();
                        cluster.setClusterId(event.getClusterId());
                        cluster.setHostName(event.getHostName());
                        cluster.setTenantRange(event.getTenantRange());

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        service.addCluster(cluster);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Cluster %s created for service %s", event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                } else if (header.getEventClassName().endsWith(ClusterRemovedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    ClusterRemovedEvent event = (ClusterRemovedEvent) jsonToObject(eventMessage.getBody(), ClusterRemovedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                        if (!service.clusterExists(event.getClusterId())) {
                            throw new RuntimeException(String.format("Cluster %s does not exist in service %s", event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        service.removeCluster(event.getClusterId());

                        if (log.isInfoEnabled()) {
                            log.info(String.format("Cluster %s removed from service %s", event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                } else if (header.getEventClassName().endsWith(MemberStartedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    MemberStartedEvent event = (MemberStartedEvent) jsonToObject(eventMessage.getBody(), MemberStartedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                        Cluster cluster = service.getCluster(event.getClusterId());
                        if (cluster == null) {
                            throw new RuntimeException(String.format("Cluster %s does not exist", event.getClusterId()));
                        }
                        if (cluster.memberExists(event.getMemberId())) {
                            throw new RuntimeException(String.format("Member %s already exist in cluster %s of service %s", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();

                        Member member = new Member();
                        member.setServiceName(event.getServiceName());
                        member.setClusterId(event.getClusterId());
                        member.setMemberId(event.getMemberId());
                        member.setHostName(event.getHostName());
                        member.setStatus(MemberStatus.Starting);
                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        Cluster cluster = service.getCluster(event.getClusterId());
                        cluster.addMember(member);

                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member %s started in cluster %s of service %s", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                } else if (header.getEventClassName().endsWith(MemberActivatedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    MemberActivatedEvent event = (MemberActivatedEvent) jsonToObject(eventMessage.getBody(), MemberActivatedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                        Cluster cluster = service.getCluster(event.getClusterId());
                        if (cluster == null) {
                            throw new RuntimeException(String.format("Cluster %s does not exist", event.getClusterId()));
                        }
                        Member member = cluster.getMember(event.getMemberId());
                        if (member == null) {
                            throw new RuntimeException(String.format("Member %s does not exist", event.getMemberId()));
                        }
                        if(member.getStatus() == MemberStatus.Activated) {
                            throw new RuntimeException(String.format("Member %s of cluster %s of service %s is already activated", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        Cluster cluster = service.getCluster(event.getClusterId());
                        Member member = cluster.getMember(event.getMemberId());
                        member.setStatus(MemberStatus.Activated);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member %s activated in cluster %s of service %s", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                } else if (header.getEventClassName().endsWith(MemberSuspendedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    MemberSuspendedEvent event = (MemberSuspendedEvent) jsonToObject(eventMessage.getBody(), MemberSuspendedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                        Cluster cluster = service.getCluster(event.getClusterId());
                        if (cluster == null) {
                            throw new RuntimeException(String.format("Cluster %s does not exist", event.getClusterId()));
                        }
                        Member member = cluster.getMember(event.getMemberId());
                        if (member == null) {
                            throw new RuntimeException(String.format("Member %s does not exist", event.getMemberId()));
                        }
                        if(member.getStatus() == MemberStatus.Suspended) {
                            throw new RuntimeException(String.format("Member %s of cluster %s of service %s is already suspended", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        Cluster cluster = service.getCluster(event.getClusterId());
                        Member member = cluster.getMember(event.getMemberId());
                        member.setStatus(MemberStatus.Suspended);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member %s suspended in cluster %s of service %s", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                } else if (header.getEventClassName().endsWith(MemberTerminatedEvent.class.getName())) {
                    // Parse complete message and build event
                    TopologyEventMessage eventMessage = (TopologyEventMessage) jsonToObject(json, TopologyEventMessage.class);
                    MemberTerminatedEvent event = (MemberTerminatedEvent) jsonToObject(eventMessage.getBody(), MemberTerminatedEvent.class);

                    // Validate event against the existing topology
                    try {
                        TopologyManager.acquireReadLock();

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        if (service == null) {
                            throw new RuntimeException(String.format("Service %s does not exist", event.getServiceName()));
                        }
                        Cluster cluster = service.getCluster(event.getClusterId());
                        if (cluster == null) {
                            throw new RuntimeException(String.format("Cluster %s does not exist", event.getClusterId()));
                        }
                        Member member = cluster.getMember(event.getMemberId());
                        if (member == null) {
                            throw new RuntimeException(String.format("Member %s does not exist", event.getMemberId()));
                        }
                        if(member.getStatus() == MemberStatus.Terminated) {
                            throw new RuntimeException(String.format("Member %s of cluster %s of service %s is already terminated", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }

                    // Apply changes to the topology
                    try {
                        TopologyManager.acquireWriteLock();

                        Service service = TopologyManager.getTopology().getService(event.getServiceName());
                        Cluster cluster = service.getCluster(event.getClusterId());
                        Member member = cluster.getMember(event.getMemberId());
                        member.setStatus(MemberStatus.Terminated);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Member %s terminated in cluster %s of service %s", event.getMemberId(), event.getClusterId(), event.getServiceName()));
                        }
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
