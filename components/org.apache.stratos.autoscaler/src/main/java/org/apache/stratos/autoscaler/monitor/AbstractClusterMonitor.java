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
package org.apache.stratos.autoscaler.monitor;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import java.util.Map;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
abstract public class AbstractClusterMonitor extends Monitor implements Runnable {

    private static final Log log = LogFactory.getLog(AbstractClusterMonitor.class);
    // Map<NetworkpartitionId, Network Partition Context>
    protected Map<String, NetworkPartitionContext> networkPartitionCtxts;
    protected DeploymentPolicy deploymentPolicy;
    protected AutoscalePolicy autoscalePolicy;


    protected FactHandle minCheckFactHandle;
    protected FactHandle scaleCheckFactHandle;
    protected FactHandle terminateDependencyFactHandle;
    protected FactHandle terminateAllFactHandle;

    protected StatefulKnowledgeSession minCheckKnowledgeSession;
    protected StatefulKnowledgeSession scaleCheckKnowledgeSession;
    protected StatefulKnowledgeSession terminateAllKnowledgeSession;
    protected boolean isDestroyed;

    protected String clusterId;
    protected String serviceId;
    protected String appId;

    protected boolean hasFaultyMember = false;

    protected ClusterStatus status;

    //protected ParentComponentMonitor parent;

    protected AutoscalerRuleEvaluator autoscalerRuleEvaluator;

    // time intereval between two runs of the Monitor. Default is 90000ms.
    protected int monitorInterval;

    public AbstractClusterMonitor() {
        readConfigurations();
        status = ClusterStatus.Created;
    }

    private void readConfigurations() {

        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        monitorInterval = conf.getInt(AutoScalerConstants.AUTOSCALER_MONITOR_INTERVAL, 90000);
        if (log.isDebugEnabled()) {
            log.debug("Cluster Monitor task interval: " + getMonitorInterval());
        }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
    }


    public NetworkPartitionContext getNetworkPartitionCtxt(Member member) {
        log.info("***** getNetworkPartitionCtxt " + member.getNetworkPartitionId());
        String networkPartitionId = member.getNetworkPartitionId();
        if (networkPartitionCtxts.containsKey(networkPartitionId)) {
            log.info("returnnig network partition context " + networkPartitionCtxts.get(networkPartitionId));
            return networkPartitionCtxts.get(networkPartitionId);
        }
        log.info("returning null getNetworkPartitionCtxt");
        return null;
    }

    public String getPartitionOfMember(String memberId) {
        for (Service service : TopologyManager.getTopology().getServices()) {
            for (Cluster cluster : service.getClusters()) {
                if (cluster.memberExists(memberId)) {
                    return cluster.getMember(memberId).getPartitionId();
                }
            }
        }
        return null;
    }

    public void destroy() {
        minCheckKnowledgeSession.dispose();
        scaleCheckKnowledgeSession.dispose();
        terminateAllKnowledgeSession.dispose();
        setDestroyed(true);
        if (log.isDebugEnabled()) {
            log.debug("Cluster Monitor Drools session has been disposed. " + this.toString());
        }
    }

    public abstract void terminateAllMembers ();

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public DeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public AutoscalePolicy getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public void setAutoscalePolicy(AutoscalePolicy autoscalePolicy) {
        this.autoscalePolicy = autoscalePolicy;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Map<String, NetworkPartitionContext> getNetworkPartitionCtxts() {
        return networkPartitionCtxts;
    }

    public NetworkPartitionContext getNetworkPartitionCtxt(String networkPartitionId) {
        return networkPartitionCtxts.get(networkPartitionId);
    }

    public void setPartitionCtxt(Map<String, NetworkPartitionContext> partitionCtxt) {
        this.networkPartitionCtxts = partitionCtxt;
    }

    public boolean partitionCtxtAvailable(String partitionId) {
        return networkPartitionCtxts.containsKey(partitionId);
    }

    public void addNetworkPartitionCtxt(NetworkPartitionContext ctxt) {
        this.networkPartitionCtxts.put(ctxt.getId(), ctxt);
    }

    public NetworkPartitionContext getPartitionCtxt(String id) {
        return this.networkPartitionCtxts.get(id);
    }

    public StatefulKnowledgeSession getMinCheckKnowledgeSession() {
        return minCheckKnowledgeSession;
    }

    public void setMinCheckKnowledgeSession(StatefulKnowledgeSession minCheckKnowledgeSession) {
        this.minCheckKnowledgeSession = minCheckKnowledgeSession;
    }

    public FactHandle getMinCheckFactHandle() {
        return minCheckFactHandle;
    }

    public void setMinCheckFactHandle(FactHandle minCheckFactHandle) {
        this.minCheckFactHandle = minCheckFactHandle;
    }


    public int getMonitorInterval() {
        return monitorInterval;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterStatus status) {

        //if(this.status != status) {
            this.status = status;
            /**
             * notifying the parent monitor about the state change
             * If the cluster in_active and if it is a in_dependent cluster,
             * then won't send the notification to parent.
             */
            if (status == ClusterStatus.Inactive && !this.hasDependent) {
                log.info("[Cluster] " + clusterId + "is not notifying the parent, " +
                        "since it is identified as the independent unit");

            } else if (status == ClusterStatus.Terminating) {
                // notify parent
                log.info("[Cluster] " + clusterId + " is not notifying the parent, " +
                        "since it is in Terminating State");

            } else {
                MonitorStatusEventBuilder.handleClusterStatusEvent(this.parent, this.status, this.clusterId);
            }
        //}

    }

    @Override
    public void onChildEvent(MonitorStatusEvent statusEvent) {

    }

    @Override
    public void onEvent(MonitorTerminateAllEvent terminateAllEvent) {

    }

    @Override
    public void onEvent(MonitorScalingEvent scalingEvent) {

    }

    public boolean isHasFaultyMember() {
        return hasFaultyMember;
    }

    public void setHasFaultyMember(boolean hasFaultyMember) {
        this.hasFaultyMember = hasFaultyMember;
    }
}