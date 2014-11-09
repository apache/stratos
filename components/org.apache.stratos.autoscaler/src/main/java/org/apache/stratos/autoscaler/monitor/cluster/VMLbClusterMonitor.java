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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.VMClusterContext;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class VMLbClusterMonitor extends VMClusterMonitor {

    private static final Log log = LogFactory.getLog(VMLbClusterMonitor.class);

    public VMLbClusterMonitor(String clusterId, String serviceId, VMClusterContext vmClusterContext) {
        super(clusterId, new AutoscalerRuleEvaluator( StratosConstants.VM_MIN_CHECK_DROOL_FILE,
                StratosConstants.VM_OBSOLETE_CHECK_DROOL_FILE, StratosConstants.VM_SCALE_CHECK_DROOL_FILE),
                vmClusterContext);
        readConfigurations();
    }

    @Override
    public void run() {

        while (!isDestroyed()) {
            if (log.isDebugEnabled()) {
                log.debug("Cluster monitor is running.. " + this.toString());
            }
            try {
                if (!ClusterStatus.Inactive.equals(getStatus())) {
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
                Thread.sleep(getMonitorIntervalMilliseconds());
            } catch (InterruptedException ignore) {
            }
        }
    }

    @Override
    protected void monitor() {
        // TODO make this concurrent
        for (NetworkPartitionContext networkPartitionContext : getNetworkPartitionCtxts().values()) {

            // minimum check per partition
            for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts()
                    .values()) {

                if (partitionContext != null) {
                    getMinCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                    getMinCheckKnowledgeSession().setGlobal("isPrimary", false);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Running minimum check for partition %s ",
                                                partitionContext.getPartitionId()));
                    }

                    minCheckFactHandle =
                            AutoscalerRuleEvaluator.evaluateMinCheck(getMinCheckKnowledgeSession(),
                                                                     minCheckFactHandle,
                                                                     partitionContext);
                    obsoleteCheckFactHandle = 
                    		AutoscalerRuleEvaluator.evaluateObsoleteCheck(getObsoleteCheckKnowledgeSession(), 
                    				obsoleteCheckFactHandle, partitionContext);
                    // start only in the first partition context
                    break;
                }

            }

        }
    }

    @Override
    public void destroy() {
        getMinCheckKnowledgeSession().dispose();
        getObsoleteCheckKnowledgeSession().dispose();
        getMinCheckKnowledgeSession().dispose();
        setDestroyed(true);
        stopScheduler();
        if (log.isDebugEnabled()) {
            log.debug("VMLbClusterMonitor Drools session has been disposed. " + this.toString());
        }
    }

    @Override
    protected void readConfigurations() {
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        int monitorInterval = conf.getInt(AutoScalerConstants.VMLb_Cluster_MONITOR_INTERVAL, 90000);
        setMonitorIntervalMilliseconds(monitorInterval);
        if (log.isDebugEnabled()) {
            log.debug("VMLbClusterMonitor task interval set to : " + getMonitorIntervalMilliseconds());
        }
    }

    @Override
    public void handleClusterRemovedEvent(
            ClusterRemovedEvent clusterRemovedEvent) {

        String deploymentPolicy = clusterRemovedEvent.getDeploymentPolicy();
        String clusterId = clusterRemovedEvent.getClusterId();
        DeploymentPolicy depPolicy = PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicy);
        if (depPolicy != null) {
            List<NetworkPartitionLbHolder> lbHolders = PartitionManager.getInstance()
                    .getNetworkPartitionLbHolders(depPolicy);

            for (NetworkPartitionLbHolder networkPartitionLbHolder : lbHolders) {
                // removes lb cluster ids
                boolean isRemoved = networkPartitionLbHolder.removeLbClusterId(clusterId);
                if (isRemoved) {
                    log.info("Removed the lb cluster [id]:"
                             + clusterId
                             + " reference from Network Partition [id]: "
                             + networkPartitionLbHolder
                            .getNetworkPartitionId());

                }
                if (log.isDebugEnabled()) {
                    log.debug(networkPartitionLbHolder);
                }

            }
        }
    }

    @Override
    public String toString() {
        return "VMLbClusterMonitor [clusterId=" + getClusterId() + "]";
    }

}
