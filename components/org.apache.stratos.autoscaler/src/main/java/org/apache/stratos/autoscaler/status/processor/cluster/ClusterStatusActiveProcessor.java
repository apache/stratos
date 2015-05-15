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
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.status.processor.StatusProcessor;

/**
 * Cluster active status processor
 */
public class ClusterStatusActiveProcessor extends ClusterStatusProcessor {
    private static final Log log = LogFactory.getLog(ClusterStatusActiveProcessor.class);
    private ClusterStatusProcessor nextProcessor;

    @Override
    public void setNext(StatusProcessor nextProcessor) {
        this.nextProcessor = (ClusterStatusProcessor) nextProcessor;
    }

    @Override
    public boolean process(String type, String clusterId, String instanceId) {
        boolean statusChanged;
        if (type == null || (ClusterStatusActiveProcessor.class.getName().equals(type))) {
            statusChanged = doProcess(clusterId, instanceId);
            if (statusChanged) {
                return true;
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, clusterId, instanceId);
            } else {
                log.warn(String.format("No possible state change found for [type] %s [cluster] %s " +
                        "[instance] %s", type, clusterId, instanceId));
            }
        }
        return false;
    }

    private boolean doProcess(String clusterId, String instanceId) {
        ClusterMonitor monitor = AutoscalerContext.getInstance().
                getClusterMonitor(clusterId);
        boolean clusterActive = false;
        for (ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext :
                monitor.getNetworkPartitionCtxts()) {
            //minimum check per partition
            ClusterInstanceContext instanceContext =
                    (ClusterInstanceContext) clusterLevelNetworkPartitionContext.
                            getInstanceContext(instanceId);
            if (instanceContext != null) {
                if (instanceContext.getActiveMembers() >= instanceContext.getMinInstanceCount()) {
                    clusterActive = true;
                }
                break;
            }
        }
        if (clusterActive) {
            if (log.isInfoEnabled()) {
                log.info("Publishing cluster activated event for [application]: "
                        + monitor.getAppId() + " [cluster]: " + clusterId);
            }
            //TODO service call
            ClusterStatusEventPublisher.sendClusterActivatedEvent(monitor.getAppId(),
                    monitor.getServiceId(), monitor.getClusterId(), instanceId);
        }
        return clusterActive;
    }
}
