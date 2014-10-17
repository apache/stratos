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
package org.apache.stratos.autoscaler.monitor.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Status;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class LbClusterMonitor extends AbstractClusterMonitor {

    private static final Log log = LogFactory.getLog(LbClusterMonitor.class);
    private Status status;

    public LbClusterMonitor(String clusterId, String serviceId, DeploymentPolicy deploymentPolicy,
                            AutoscalePolicy autoscalePolicy) {
        this.clusterId = clusterId;
        this.serviceId = serviceId;

        this.autoscalerRuleEvaluator = new AutoscalerRuleEvaluator();
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getScaleCheckStatefulSession();
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getMinCheckStatefulSession();

        this.deploymentPolicy = deploymentPolicy;
        this.deploymentPolicy = deploymentPolicy;
        networkPartitionCtxts = new ConcurrentHashMap<String, NetworkPartitionContext>();
    }

    @Override
    public void run() {

        while (!isDestroyed()) {
            if (log.isDebugEnabled()) {
                log.debug("Cluster monitor is running.. " + this.toString());
            }
            try {
                if( !ClusterStatus.Inactive.equals(status)) {
                    monitor();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("LB Cluster monitor is suspended as the cluster is in " +
                                    ClusterStatus.Inactive + " mode......");
                    }
                }
            } catch (Exception e) {
                log.error("Cluster monitor: Monitor failed. " + this.toString(), e);
            }
            try {
                Thread.sleep(monitorInterval);
            } catch (InterruptedException ignore) {
            }
        }
    }

    private void monitor() {
        // TODO make this concurrent
        for (NetworkPartitionContext networkPartitionContext : networkPartitionCtxts.values()) {

            // minimum check per partition
            for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts()
                    .values()) {

                if (partitionContext != null) {
                    minCheckKnowledgeSession.setGlobal("clusterId", clusterId);
                    minCheckKnowledgeSession.setGlobal("isPrimary", false);
                    
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Running minimum check for partition %s ",
                                partitionContext.getPartitionId()));
                    }

                    minCheckFactHandle =
                            AutoscalerRuleEvaluator.evaluateMinCheck(minCheckKnowledgeSession,
                                    minCheckFactHandle,
                                    partitionContext);
                    // start only in the first partition context
                    break;
                }

            }

        }
    }

    @Override
    public String toString() {
        return "LbClusterMonitor [clusterId=" + clusterId + ", serviceId=" + serviceId + "]";
    }



}
