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
package org.apache.stratos.autoscaler.event.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * This will publish cluster status events to cluster-status topic
 */
public class ClusterStatusEventPublisher {
    private static final Log log = LogFactory.getLog(ClusterStatusEventPublisher.class);


    public static void sendClusterCreatedEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Created, null)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster created event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    /*ClusterStatusClusterCreatedEvent clusterCreatedEvent =
                            new ClusterStatusClusterCreatedEvent(appId, serviceName, clusterId);

                    publishEvent(clusterCreatedEvent);*/
                } else {
                    log.warn("Created is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterResetEvent(String appId, String serviceName,
                                             String clusterId, String instanceId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Created, null)){
                    if(cluster.getStatus(null) != ClusterStatus.Created) {
                        ClusterStatusClusterResetEvent clusterCreatedEvent =
                                new ClusterStatusClusterResetEvent(appId, serviceName, clusterId, instanceId);

                        publishEvent(clusterCreatedEvent);
                    } else {
                        if(log.isDebugEnabled()){
                            log.warn("Cluster is already created, [cluster] " + clusterId);
                        }
                    }
                } else {
                    log.warn("Created is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterInstanceCreatedEvent(String alias, String serviceName,
                                                       String clusterId, String instanceId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if(cluster.getInstanceContexts(instanceId) == null) {
                   log.warn("The Instance context for the cluster already exists for [cluster] " +
                   clusterId + " [instance-id] " + instanceId);
                    return;
                }
                ClusterStatusClusterInstanceCreatedEvent clusterInstanceCreatedEvent =
                        new ClusterStatusClusterInstanceCreatedEvent(alias, serviceName,
                                                                    clusterId, instanceId);

                publishEvent(clusterInstanceCreatedEvent);
            } else {
                log.warn("Created is not in the possible state list of [cluster] " + clusterId);
            }

        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }


    public static void sendClusterActivatedEvent(String appId, String serviceName, String clusterId,
                                                 String instanceId) {
        TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
        try {
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
                if (clusterInstance.isStateTransitionValid(ClusterStatus.Active)){
                    if(clusterInstance.getStatus() != ClusterStatus.Active) {
                        ClusterStatusClusterActivatedEvent clusterActivatedEvent =
                                new ClusterStatusClusterActivatedEvent(appId, serviceName,
                                        clusterId, instanceId);

                        publishEvent(clusterActivatedEvent);
                    } else {
                        if(log.isDebugEnabled()){
                            log.warn("Cluster is already active [cluster] " + clusterId);
                        }
                    }
                } else {
                    log.warn("Active is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterInactivateEvent(String appId, String serviceName,
                                                  String clusterId, String instanceId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
                if (clusterInstance.isStateTransitionValid(ClusterStatus.Inactive)){
                        if(clusterInstance.getStatus() != ClusterStatus.Inactive) {
                            ClusterStatusClusterInactivateEvent clusterInactivateEvent =
                                    new ClusterStatusClusterInactivateEvent(appId, serviceName, clusterId, instanceId);

                            publishEvent(clusterInactivateEvent);
                        } else {
                            if(log.isDebugEnabled()){
                                log.warn("Cluster is already inactive [cluster] " + clusterId);
                            }
                        }
                } else {
                    log.warn("Inactive is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }
    }

    public static void sendClusterTerminatingEvent(String appId, String serviceName,
                                                   String clusterId, String instanceId) {

        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);

            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
                if (clusterInstance.isStateTransitionValid(ClusterStatus.Terminating)){
                    if (clusterInstance.getStatus() != ClusterStatus.Terminating) {
                        ClusterStatusClusterTerminatingEvent appStatusClusterTerminatingEvent =
                                new ClusterStatusClusterTerminatingEvent(appId, serviceName, clusterId, instanceId);

                        publishEvent(appStatusClusterTerminatingEvent);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.warn("Cluster is already terminating, [cluster] " + clusterId);
                        }
                    }
                }
            } else {
                log.warn("Terminating is not in the possible state list of [cluster] " + clusterId);
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }

    }

    public static void sendClusterTerminatedEvent(String appId, String serviceName,
                                                  String clusterId, String instanceId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                ClusterInstance clusterInstance = cluster.getInstanceContexts(instanceId);
                if (clusterInstance.isStateTransitionValid(ClusterStatus.Terminated)){
                    if(clusterInstance.getStatus() != ClusterStatus.Terminated) {
                        ClusterStatusClusterTerminatedEvent appStatusClusterTerminatedEvent =
                                new ClusterStatusClusterTerminatedEvent(appId, serviceName, clusterId, instanceId);

                        publishEvent(appStatusClusterTerminatedEvent);
                    } else {
                        if(log.isDebugEnabled()){
                            log.warn("Cluster is already terminated, [cluster] " + clusterId);
                        }
                    }
                } else {
                    log.warn("Terminated is not in the possible state list for [ClusterInstance] " +
                            clusterInstance.getInstanceId() + " of [cluster] " +
                            clusterId + " as it is current state is " + clusterInstance.getStatus());
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }
    }


    public static void publishEvent(Event event) {
        //publishing events to application status topic
        String topic = MessagingUtil.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }
}
