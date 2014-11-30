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

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.cluster.VMClusterContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.GroupStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Is responsible for monitoring a service cluster which is based on VMs. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class VMServiceClusterMonitor extends VMClusterMonitor {

    private static final Log log = LogFactory.getLog(VMServiceClusterMonitor.class);
    private String lbReferenceType;
    private boolean hasPrimary;
    private float scalingFactorBasedOnDependencies = 1.0f;

    public VMServiceClusterMonitor(String serviceType, String clusterId) {
        super(serviceType, clusterId, new AutoscalerRuleEvaluator(
                StratosConstants.VM_MIN_CHECK_DROOL_FILE,
                StratosConstants.VM_OBSOLETE_CHECK_DROOL_FILE,
                StratosConstants.VM_SCALE_CHECK_DROOL_FILE));
        readConfigurations();
    }

    @Override
    public void run() {
        while (!isDestroyed()) {
            try {
                /* TODO ***********if  (((getStatus().getCode() <= ClusterStatus.Active.getCode()) ||
                        (getStatus() == ClusterStatus.Inactive && !hasStartupDependents)) && !this.hasFaultyMember
                        && !stop) {*/
                if (log.isDebugEnabled()) {
                    log.debug("Cluster monitor is running.. " + this.toString());
                }
                monitor();
                /*} else {
                    if (log.isDebugEnabled()) {
                        log.debug("Cluster monitor is suspended as the cluster is in " +
                                ClusterStatus.Inactive + " mode......");
                    }
                }*/
            } catch (Exception e) {
                log.error("Cluster monitor: Monitor failed." + this.toString(), e);
            }
            try {
                Thread.sleep(getMonitorIntervalMilliseconds());
            } catch (InterruptedException ignore) {
            }
        }


    }

    private boolean isPrimaryMember(MemberContext memberContext) {
        Properties props = memberContext.getProperties();
        if (log.isDebugEnabled()) {
            log.debug(" Properties [" + props + "] ");
        }
        if (props != null && props.getProperties() != null) {
            for (Property prop : props.getProperties()) {
                if (prop.getName().equals("PRIMARY")) {
                    if (Boolean.parseBoolean(prop.getValue())) {
                        log.debug("Adding member id [" + memberContext.getMemberId() + "] " +
                                "member instance id [" + memberContext.getInstanceId() + "] as a primary member");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void monitor() {
        final Collection<ClusterLevelNetworkPartitionContext> clusterLevelNetworkPartitionContexts =
                ((VMClusterContext) this.clusterContext).getNetworkPartitionCtxts().values();
        Runnable monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                for (ClusterLevelNetworkPartitionContext networkPartitionContext :
                        clusterLevelNetworkPartitionContexts) {
                    // store primary members in the network partition context
                    List<String> primaryMemberListInNetworkPartition = new ArrayList<String>();
                    //minimum check per partition
                    for (ClusterInstanceContext instanceContext : networkPartitionContext.
                            getClusterInstanceContextMap().values()) {
                        //FIXME to check the status of the instance
                        if (true) {
                            for (ClusterLevelPartitionContext partitionContext :
                                    instanceContext.getPartitionCtxts().values()) {
                                // store primary members in the partition context
                                List<String> primaryMemberListInPartition = new ArrayList<String>();
                                // get active primary members in this partition context
                                for (MemberContext memberContext : partitionContext.getActiveMembers()) {
                                    if (isPrimaryMember(memberContext)) {
                                        primaryMemberListInPartition.add(memberContext.getMemberId());
                                    }
                                }

                                // get pending primary members in this partition context
                                for (MemberContext memberContext : partitionContext.getPendingMembers()) {
                                    if (isPrimaryMember(memberContext)) {
                                        primaryMemberListInPartition.add(memberContext.getMemberId());
                                    }
                                }
                                primaryMemberListInNetworkPartition.addAll(primaryMemberListInPartition);
                                getMinCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                                getMinCheckKnowledgeSession().setGlobal("lbRef", lbReferenceType);
                                getMinCheckKnowledgeSession().setGlobal("isPrimary", hasPrimary);
                                getMinCheckKnowledgeSession().setGlobal("instanceId",
                                        instanceContext.getId());

                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Running minimum check for partition %s ",
                                            partitionContext.getPartitionId()));
                                }

                                minCheckFactHandle = AutoscalerRuleEvaluator.
                                        evaluateMinCheck(getMinCheckKnowledgeSession()
                                                , minCheckFactHandle, partitionContext);

                                obsoleteCheckFactHandle = AutoscalerRuleEvaluator.
                                        evaluateObsoleteCheck(getObsoleteCheckKnowledgeSession(),
                                                obsoleteCheckFactHandle, partitionContext);

                                //checking the status of the cluster

                                boolean rifReset = instanceContext.isRifReset();
                                boolean memoryConsumptionReset = instanceContext.isMemoryConsumptionReset();
                                boolean loadAverageReset = instanceContext.isLoadAverageReset();

                                if (log.isDebugEnabled()) {
                                    log.debug("flag of rifReset: " + rifReset + " flag of memoryConsumptionReset" + memoryConsumptionReset
                                            + " flag of loadAverageReset" + loadAverageReset);
                                }
                                if (rifReset || memoryConsumptionReset || loadAverageReset) {


                                    VMClusterContext vmClusterContext = (VMClusterContext) clusterContext;

                                    getScaleCheckKnowledgeSession().setGlobal("instance",
                                            instanceContext);
                                    getScaleCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                                    getScaleCheckKnowledgeSession().setGlobal("autoscalePolicy", vmClusterContext.getAutoscalePolicy());
                                    getScaleCheckKnowledgeSession().setGlobal("rifReset", rifReset);
                                    getScaleCheckKnowledgeSession().setGlobal("mcReset", memoryConsumptionReset);
                                    getScaleCheckKnowledgeSession().setGlobal("laReset", loadAverageReset);
                                    getScaleCheckKnowledgeSession().setGlobal("lbRef", lbReferenceType);
                                    getScaleCheckKnowledgeSession().setGlobal("isPrimary", false);
                                    getScaleCheckKnowledgeSession().setGlobal("primaryMembers", primaryMemberListInNetworkPartition);

                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("Running scale check for network partition %s ", networkPartitionContext.getId()));
                                        log.debug(" Primary members : " + primaryMemberListInNetworkPartition);
                                    }

                                    scaleCheckFactHandle = AutoscalerRuleEvaluator.evaluateScaleCheck(getScaleCheckKnowledgeSession()
                                            , scaleCheckFactHandle, networkPartitionContext);

                                    instanceContext.setRifReset(false);
                                    instanceContext.setMemoryConsumptionReset(false);
                                    instanceContext.setLoadAverageReset(false);
                                } else if (log.isDebugEnabled()) {
                                    log.debug(String.format("Scale rule will not run since the LB statistics have not received before this " +
                                            "cycle for network partition %s", networkPartitionContext.getId()));
                                }
                            }

                        }
                    }
                }

            }
        };
        monitoringRunnable.run();
    }

    @Override
    protected void readConfigurations() {
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        int monitorInterval = conf.getInt(AutoScalerConstants.VMService_Cluster_MONITOR_INTERVAL, 90000);
        setMonitorIntervalMilliseconds(monitorInterval);
        if (log.isDebugEnabled()) {
            log.debug("VMServiceClusterMonitor task interval set to : " + getMonitorIntervalMilliseconds());
        }
    }

    @Override
    public void destroy() {
        getMinCheckKnowledgeSession().dispose();
        getObsoleteCheckKnowledgeSession().dispose();
        getScaleCheckKnowledgeSession().dispose();
        setDestroyed(true);
        stopScheduler();
        if (log.isDebugEnabled()) {
            log.debug("VMServiceClusterMonitor Drools session has been disposed. " + this.toString());
        }
    }

    @Override
    public String toString() {
        return "VMServiceClusterMonitor [clusterId=" + getClusterId() +
                ", lbReferenceType=" + lbReferenceType +
                ", hasPrimary=" + hasPrimary + " ]";
    }

    public String getLbReferenceType() {
        return lbReferenceType;
    }

    public void setLbReferenceType(String lbReferenceType) {
        this.lbReferenceType = lbReferenceType;
    }

    public boolean isHasPrimary() {
        return hasPrimary;
    }

    public void setHasPrimary(boolean hasPrimary) {
        this.hasPrimary = hasPrimary;
    }

    @Override
    public void onChildStatusEvent(MonitorStatusEvent statusEvent) {

    }

    @Override
    public void onParentStatusEvent(MonitorStatusEvent statusEvent) {
        String instanceId = statusEvent.getInstanceId();
        // send the ClusterTerminating event
        if (statusEvent.getStatus() == GroupStatus.Terminating || statusEvent.getStatus() ==
                ApplicationStatus.Terminating) {
            if (log.isInfoEnabled()) {
                log.info("Publishing Cluster terminating event for [application]: " + appId +
                        " [cluster]: " + this.getClusterId());
            }
            ClusterStatusEventPublisher.sendClusterTerminatingEvent(getAppId(), getServiceId(), getClusterId(), instanceId);
        }
    }

    @Override
    public void onChildScalingEvent(MonitorScalingEvent scalingEvent) {

    }

    @Override
    public void onParentScalingEvent(MonitorScalingEvent scalingEvent) {

        if (log.isDebugEnabled()) {
            log.debug("Parent scaling event received to [cluster]: " + this.getClusterId()
                    + ", [network partition]: " + scalingEvent.getNetworkPartitionId()
                    + ", [event] " + scalingEvent.getId() + ", [group instance] " + scalingEvent.getInstanceId());
        }

        this.scalingFactorBasedOnDependencies = scalingEvent.getFactor();
        VMClusterContext vmClusterContext = (VMClusterContext) clusterContext;
        String instanceId = scalingEvent.getInstanceId();

        ClusterInstanceContext clusterLevelNetworkPartitionContext =
                getClusterInstanceContext(scalingEvent.getNetworkPartitionId(), instanceId);


        //TODO get min instance count from instance context
        float requiredInstanceCount = 0 ;/* = clusterLevelNetworkPartitionContext.getMinInstanceCount() * scalingFactorBasedOnDependencies;*/
        int roundedRequiredInstanceCount = getRoundedInstanceCount(requiredInstanceCount,
                vmClusterContext.getAutoscalePolicy().getInstanceRoundingFactor());
        clusterLevelNetworkPartitionContext.setRequiredInstanceCountBasedOnDependencies(roundedRequiredInstanceCount);

        getDependentScaleCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
        getDependentScaleCheckKnowledgeSession().setGlobal("scalingFactor", scalingFactorBasedOnDependencies);
        getDependentScaleCheckKnowledgeSession().setGlobal("instanceRoundingFactor",
                vmClusterContext.getAutoscalePolicy().getInstanceRoundingFactor());

        dependentScaleCheckFactHandle = AutoscalerRuleEvaluator.evaluateScaleCheck(getScaleCheckKnowledgeSession()
                , scaleCheckFactHandle, clusterLevelNetworkPartitionContext);

    }

    public void sendClusterScalingEvent(String networkPartitionId, float factor) {

        MonitorStatusEventBuilder.handleClusterScalingEvent(this.parent, networkPartitionId, factor, this.id);
    }
}
