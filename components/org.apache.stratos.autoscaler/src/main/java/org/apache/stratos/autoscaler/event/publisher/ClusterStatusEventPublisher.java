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
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

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
                if (cluster.isStateTransitionValid(ClusterStatus.Created)) {
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

    public static void sendClusterResetEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Created)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster created event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    ClusterStatusClusterResetEvent clusterCreatedEvent =
                            new ClusterStatusClusterResetEvent(appId, serviceName, clusterId);

                    publishEvent(clusterCreatedEvent);
                } else {
                    log.warn("Created is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterActivatedEvent(String appId, String serviceName, String clusterId) {
        TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
        try {
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster activated event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    ClusterStatusClusterActivatedEvent clusterActivatedEvent =
                            new ClusterStatusClusterActivatedEvent(appId, serviceName, clusterId);

                    publishEvent(clusterActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterInActivateEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Inactive)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster in-activate event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    ClusterStatusClusterInactivateEvent clusterInActivateEvent =
                            new ClusterStatusClusterInactivateEvent(appId, serviceName, clusterId);

                    publishEvent(clusterInActivateEvent);
                } else {
                    log.warn("In-active is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }
    }

    public static void sendClusterTerminatingEvent(String appId, String serviceName, String clusterId) {

        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Terminating)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster Terminating event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    ClusterStatusClusterTerminatingEvent appStatusClusterTerminatingEvent =
                            new ClusterStatusClusterTerminatingEvent(appId, serviceName, clusterId);

                    publishEvent(appStatusClusterTerminatingEvent);
                } else {
                    log.warn("Terminating is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }

    }

    public static void sendClusterTerminatedEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Terminated)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster terminated event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    ClusterStatusClusterTerminatedEvent appStatusClusterTerminatedEvent =
                            new ClusterStatusClusterTerminatedEvent(appId, serviceName, clusterId);

                    publishEvent(appStatusClusterTerminatedEvent);
                } else {
                    log.warn("Terminated is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }
    }


    public static void publishEvent(Event event) {
        //publishing events to application status topic
        String topic = Util.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }
}
