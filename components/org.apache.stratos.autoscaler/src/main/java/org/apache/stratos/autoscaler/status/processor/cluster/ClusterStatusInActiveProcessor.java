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
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
import org.apache.stratos.autoscaler.status.processor.StatusProcessor;

/**
 * Cluster inActive checking processor
 */
public class ClusterStatusInActiveProcessor extends ClusterStatusProcessor {
    private static final Log log = LogFactory.getLog(ClusterStatusInActiveProcessor.class);
    private ClusterStatusProcessor nextProcessor;

    @Override
    public void setNext(StatusProcessor nextProcessor) {
        this.nextProcessor = (ClusterStatusProcessor) nextProcessor;
    }
    @Override
    public boolean process(String type, String clusterId, String instanceId) {
        boolean statusChanged;
        if (type == null || (ClusterStatusInActiveProcessor.class.getName().equals(type))) {
            statusChanged = doProcess(clusterId, instanceId);
            if (statusChanged) {
                return statusChanged;
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, clusterId, instanceId);
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

        boolean clusterInActive;
        clusterInActive = getClusterInactive(monitor);
        if(clusterInActive) {
            //if the monitor is dependent, temporarily pausing it
            if (monitor.hasStartupDependents()) {
                monitor.setHasFaultyMember(true);
            }
            if (log.isInfoEnabled()) {
                log.info("Publishing Cluster in-activate event for [application]: "
                        + monitor.getAppId() + " [cluster]: " + clusterId);
            }
            //send cluster In-Active event to cluster status topic
            ClusterStatusEventPublisher.sendClusterInActivateEvent(monitor.getAppId(),
                    monitor.getServiceId(), clusterId, instanceId);
        } else {
            if (log.isInfoEnabled()) {
                log.info("Publishing Cluster active event for [application]: "
                        + monitor.getAppId() + " [cluster]: " + clusterId);
            }
            ClusterStatusEventPublisher.sendClusterActivatedEvent(monitor.getAppId(),
                                                        monitor.getServiceId(), clusterId);
        }
        return clusterInActive;
    }

    private boolean getClusterInactive(VMClusterMonitor monitor) {
        boolean clusterInActive = false;
        for (NetworkPartitionContext networkPartitionContext : monitor.getAllNetworkPartitionCtxts().values()) {
            for (ClusterLevelPartitionContext partition : networkPartitionContext.getPartitionCtxts().values()) {
                if (partition.getActiveMemberCount() <= partition.getMinimumMemberCount()) {
                    clusterInActive = true;
                    return clusterInActive;
                }
            }

        }
        return clusterInActive;
    }
}
