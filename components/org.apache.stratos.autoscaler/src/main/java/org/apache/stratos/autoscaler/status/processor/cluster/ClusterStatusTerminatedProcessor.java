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
package org.apache.stratos.autoscaler.status.processor.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
import org.apache.stratos.autoscaler.status.processor.StatusProcessor;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Cluster terminated checking processor.
 */
public class ClusterStatusTerminatedProcessor extends ClusterStatusProcessor {
    private static final Log log = LogFactory.getLog(ClusterStatusInActiveProcessor.class);
    private ClusterStatusProcessor nextProcessor;

    @Override
    public void setNext(StatusProcessor nextProcessor) {
        this.nextProcessor = (ClusterStatusProcessor) nextProcessor;
    }

    @Override
    public boolean process(String type, String clusterId, String instanceId) {
        boolean statusChanged;
        if (type == null || (ClusterStatusTerminatedProcessor.class.getName().equals(type))) {
            statusChanged = doProcess(clusterId, instanceId);
            if (statusChanged) {
                return statusChanged;
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(null, clusterId, instanceId);
            } else {
                throw new RuntimeException(String.format("Failed to process message using " +
                                "available message processors: [type] %s [cluster] %s [instance]",
                        type, clusterId, instanceId));
            }
        }
        return false;
    }

    private boolean doProcess(String clusterId, String instanceId) {
        VMClusterMonitor monitor = (VMClusterMonitor) AutoscalerContext.getInstance().
                getClusterMonitor(clusterId);
        boolean clusterMonitorHasMembers = clusterMonitorHasMembers(monitor);
        boolean clusterTerminated = false;
        try {
            TopologyManager.acquireReadLockForCluster(monitor.getServiceId(), monitor.getClusterId());
            Service service = TopologyManager.getTopology().getService(monitor.getServiceId());
            Cluster cluster;
            String appId = monitor.getAppId();
            if (service != null) {
                cluster = service.getCluster(monitor.getClusterId());
                if (cluster != null) {
                    try {
                        ApplicationHolder.acquireReadLock();
                        Application application = ApplicationHolder.getApplications().getApplication(appId);
                        //if all members removed from the cluster and cluster is in terminating,
                        // either it has to be terminated or Reset
                        if (!clusterMonitorHasMembers && cluster.getStatus(null) == ClusterStatus.Terminating) {
                            if (log.isInfoEnabled()) {
                                log.info("Publishing Cluster terminated event for [application]: " + appId +
                                        " [cluster]: " + clusterId);
                            }
                            ClusterStatusEventPublisher.sendClusterTerminatedEvent(appId, monitor.getServiceId(),
                                    monitor.getClusterId(), instanceId);
                            clusterTerminated = true;

                        } else {
                            log.info("Cluster has non terminated [members] and in the [status] "
                                    + cluster.getInstanceContexts(instanceId).getStatus().toString());
                        }
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                }
            }


        } finally {
            TopologyManager.releaseReadLockForCluster(monitor.getServiceId(), monitor.getClusterId());

        }
        return clusterTerminated;
    }

    /**
     * Find out whether cluster monitor has any non terminated members
     *
     * @param monitor the cluster monitor
     * @return whether has members or not
     */
    private boolean clusterMonitorHasMembers(VMClusterMonitor monitor) {
        boolean hasMember = false;
        for (ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext : monitor.getAllNetworkPartitionCtxts().values()) {
            //minimum check per partition
            for (ClusterLevelPartitionContext partitionContext : clusterLevelNetworkPartitionContext.getPartitionCtxts().values()) {
                if (partitionContext.getNonTerminatedMemberCount() > 0) {
                    hasMember = true;
                } else {
                    hasMember = false;
                }
            }
        }
        return hasMember;
    }
}
