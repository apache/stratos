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

import java.util.*;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.cluster.VMClusterContext;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.exception.cartridge.TerminationException;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.status.processor.StatusChecker;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class VMClusterMonitor extends AbstractClusterMonitor {

    private static final Log log = LogFactory.getLog(VMClusterMonitor.class);
    private Map<String, ClusterLevelNetworkPartitionContext> networkPartitionIdToClusterLevelNetworkPartitionCtxts;
    private boolean hasPrimary;
    private float scalingFactorBasedOnDependencies = 1.0f;

    protected VMClusterMonitor(String serviceType, String clusterId) {
        super(serviceType, clusterId, new AutoscalerRuleEvaluator(
                StratosConstants.VM_MIN_CHECK_DROOL_FILE,
                StratosConstants.VM_OBSOLETE_CHECK_DROOL_FILE,
                StratosConstants.VM_SCALE_CHECK_DROOL_FILE));
        this.networkPartitionIdToClusterLevelNetworkPartitionCtxts = new HashMap<String, ClusterLevelNetworkPartitionContext>();

        readConfigurations();
    }

    public void addClusterLevelNWPartitionContext (ClusterLevelNetworkPartitionContext clusterLevelNWPartitionCtxt) {
        networkPartitionIdToClusterLevelNetworkPartitionCtxts.put(clusterLevelNWPartitionCtxt.getId(), clusterLevelNWPartitionCtxt);
    }

    public ClusterLevelNetworkPartitionContext getClusterLevelNWPartitionContext (String nwPartitionId) {
        return networkPartitionIdToClusterLevelNetworkPartitionCtxts.get(nwPartitionId);
    }

    @Override
    public void handleAverageLoadAverageEvent(
            AverageLoadAverageEvent averageLoadAverageEvent) {

        String networkPartitionId = averageLoadAverageEvent.getNetworkPartitionId();
        String clusterId = averageLoadAverageEvent.getClusterId();
        String instanceId = averageLoadAverageEvent.getInstanceId();
        float value = averageLoadAverageEvent.getValue();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg load avg event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }

        ClusterInstanceContext clusterInstanceContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterInstanceContext) {
            clusterInstanceContext.setAverageLoadAverage(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
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
        Properties props = AutoscalerUtil.toCommonProperties(memberContext.getProperties());
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

                    for (ClusterInstanceContext instanceContext : networkPartitionContext.
                            getClusterInstanceContextMap().values()) {

                        // store primary members in the cluster instance context
                        List<String> primaryMemberListInClusterInstance = new ArrayList<String>();

                        //FIXME to check the status of the instance
                        if (true) {

                            for (ClusterLevelPartitionContext partitionContext : instanceContext.getPartitionCtxts()) {
                                // get active primary members in this partition context
                                for (MemberContext memberContext : partitionContext.getActiveMembers()) {
                                    if (isPrimaryMember(memberContext)) {
                                        primaryMemberListInClusterInstance.add(memberContext.getMemberId());
                                    }
                                }

                                // get pending primary members in this partition context
                                for (MemberContext memberContext : partitionContext.getPendingMembers()) {
                                    if (isPrimaryMember(memberContext)) {
                                        primaryMemberListInClusterInstance.add(memberContext.getMemberId());
                                    }
                                }
                            }

                            getMinCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                            getMinCheckKnowledgeSession().setGlobal("isPrimary", hasPrimary);
                            getMinCheckKnowledgeSession().setGlobal("instanceId", instanceContext.getId());

                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Running minimum check for cluster instance %s ",
                                        instanceContext.getId()));
                            }

                            minCheckFactHandle = AutoscalerRuleEvaluator.
                                    evaluateMinCheck(getMinCheckKnowledgeSession()
                                            , minCheckFactHandle, instanceContext);

                            obsoleteCheckFactHandle = AutoscalerRuleEvaluator.
                                    evaluateObsoleteCheck(getObsoleteCheckKnowledgeSession(),
                                            obsoleteCheckFactHandle, instanceContext);

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

                                getScaleCheckKnowledgeSession().setGlobal("instance", instanceContext);
                                getScaleCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                                getScaleCheckKnowledgeSession().setGlobal("autoscalePolicy", vmClusterContext.getAutoscalePolicy());
                                getScaleCheckKnowledgeSession().setGlobal("rifReset", rifReset);
                                getScaleCheckKnowledgeSession().setGlobal("mcReset", memoryConsumptionReset);
                                getScaleCheckKnowledgeSession().setGlobal("laReset", loadAverageReset);
//                                    getScaleCheckKnowledgeSession().setGlobal("lbRef", lbReferenceType);
                                getScaleCheckKnowledgeSession().setGlobal("isPrimary", false);
                                getScaleCheckKnowledgeSession().setGlobal("primaryMembers", primaryMemberListInClusterInstance);

                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Running scale check for network partition %s ", networkPartitionContext.getId()));
                                    log.debug(" Primary members : " + primaryMemberListInClusterInstance);
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
//                ", lbReferenceType=" + lbReferenceType +
                ", hasPrimary=" + hasPrimary + " ]";
    }

//    public String getLbReferenceType() {
//        return lbReferenceType;
//    }
//
//    public void setLbReferenceType(String lbReferenceType) {
//        this.lbReferenceType = lbReferenceType;
//    }

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
    @Override
    public void handleGradientOfLoadAverageEvent(
            GradientOfLoadAverageEvent gradientOfLoadAverageEvent) {

        String networkPartitionId = gradientOfLoadAverageEvent.getNetworkPartitionId();
        String clusterId = gradientOfLoadAverageEvent.getClusterId();
        String instanceId = gradientOfLoadAverageEvent.getInstanceId();
        float value = gradientOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of load avg event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setLoadAverageGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfLoadAverageEvent(
            SecondDerivativeOfLoadAverageEvent secondDerivativeOfLoadAverageEvent) {

        String networkPartitionId = secondDerivativeOfLoadAverageEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfLoadAverageEvent.getClusterId();
        String instanceId = secondDerivativeOfLoadAverageEvent.getInstanceId();
        float value = secondDerivativeOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of load avg event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                                                                        networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setLoadAverageSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleAverageMemoryConsumptionEvent(
            AverageMemoryConsumptionEvent averageMemoryConsumptionEvent) {

        String networkPartitionId = averageMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = averageMemoryConsumptionEvent.getClusterId();
        String instanceId = averageMemoryConsumptionEvent.getInstanceId();
        float value = averageMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg Memory Consumption event: [cluster] %s [network-partition] %s "
                    + "[value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setAverageMemoryConsumption(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Network partition context is not available for :"
                                + " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleGradientOfMemoryConsumptionEvent(
            GradientOfMemoryConsumptionEvent gradientOfMemoryConsumptionEvent) {

        String networkPartitionId = gradientOfMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = gradientOfMemoryConsumptionEvent.getClusterId();
        String instanceId = gradientOfMemoryConsumptionEvent.getInstanceId();
        float value = gradientOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of Memory Consumption event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setMemoryConsumptionGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfMemoryConsumptionEvent(
            SecondDerivativeOfMemoryConsumptionEvent secondDerivativeOfMemoryConsumptionEvent) {

        String networkPartitionId = secondDerivativeOfMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfMemoryConsumptionEvent.getClusterId();
        String instanceId = secondDerivativeOfMemoryConsumptionEvent.getInstanceId();
        float value = secondDerivativeOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of Memory Consumption event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setMemoryConsumptionSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleAverageRequestsServingCapabilityEvent(AverageRequestsServingCapabilityEvent averageRequestsServingCapabilityEvent) {

        String clusterId = averageRequestsServingCapabilityEvent.getClusterId();
        String instanceId = averageRequestsServingCapabilityEvent.getInstanceId();
        String networkPartitionId = averageRequestsServingCapabilityEvent.getNetworkPartitionId();
        Float floatValue = averageRequestsServingCapabilityEvent.getValue();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Requests Served per Instance event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, floatValue));
        }

        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if(null != clusterLevelNetworkPartitionContext){
            clusterLevelNetworkPartitionContext.setAverageRequestsServedPerInstance(floatValue);

        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }

    }

    @Override
    public void handleAverageRequestsInFlightEvent(
            AverageRequestsInFlightEvent averageRequestsInFlightEvent) {

        String networkPartitionId = averageRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = averageRequestsInFlightEvent.getClusterId();
        String instanceId = averageRequestsInFlightEvent.getInstanceId();
        Float servedCount = averageRequestsInFlightEvent.getServedCount();
        Float activeInstances = averageRequestsInFlightEvent.getActiveInstances();
        Float requestsServedPerInstance = servedCount / activeInstances;
        if (requestsServedPerInstance.isInfinite()) {
            requestsServedPerInstance = 0f;
        }
        float value = averageRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Rif event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setAverageRequestsInFlight(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleGradientOfRequestsInFlightEvent(
            GradientOfRequestsInFlightEvent gradientOfRequestsInFlightEvent) {

        String networkPartitionId = gradientOfRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = gradientOfRequestsInFlightEvent.getClusterId();
        String instanceId = gradientOfRequestsInFlightEvent.getInstanceId();
        float value = gradientOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Gradient of Rif event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setRequestsInFlightGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfRequestsInFlightEvent(
            SecondDerivativeOfRequestsInFlightEvent secondDerivativeOfRequestsInFlightEvent) {

        String networkPartitionId = secondDerivativeOfRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfRequestsInFlightEvent.getClusterId();
        String instanceId = secondDerivativeOfRequestsInFlightEvent.getInstanceId();
        float value = secondDerivativeOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second derivative of Rif event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setRequestsInFlightSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleMemberAverageMemoryConsumptionEvent(
            MemberAverageMemoryConsumptionEvent memberAverageMemoryConsumptionEvent) {

        String instanceId = memberAverageMemoryConsumptionEvent.getInstanceId();
        String memberId = memberAverageMemoryConsumptionEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(
                                                                member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberAverageMemoryConsumptionEvent.getValue();
        memberStatsContext.setAverageMemoryConsumption(value);
    }

    @Override
    public void handleMemberGradientOfMemoryConsumptionEvent(
            MemberGradientOfMemoryConsumptionEvent memberGradientOfMemoryConsumptionEvent) {

        String instanceId = memberGradientOfMemoryConsumptionEvent.getInstanceId();
        String memberId = memberGradientOfMemoryConsumptionEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(
                                                                            member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberGradientOfMemoryConsumptionEvent.getValue();
        memberStatsContext.setGradientOfMemoryConsumption(value);
    }

    @Override
    public void handleMemberSecondDerivativeOfMemoryConsumptionEvent(
            MemberSecondDerivativeOfMemoryConsumptionEvent memberSecondDerivativeOfMemoryConsumptionEvent) {

    }

    @Override
    public void handleMemberAverageLoadAverageEvent(
            MemberAverageLoadAverageEvent memberAverageLoadAverageEvent) {

        String instanceId = memberAverageLoadAverageEvent.getInstanceId();
        String memberId = memberAverageLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(
                                                                            member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberAverageLoadAverageEvent.getValue();
        memberStatsContext.setAverageLoadAverage(value);
    }

    @Override
    public void handleMemberGradientOfLoadAverageEvent(
            MemberGradientOfLoadAverageEvent memberGradientOfLoadAverageEvent) {

        String instanceId = memberGradientOfLoadAverageEvent.getInstanceId();
        String memberId = memberGradientOfLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(
                                                                            member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberGradientOfLoadAverageEvent.getValue();
        memberStatsContext.setGradientOfLoadAverage(value);
    }

    @Override
    public void handleMemberSecondDerivativeOfLoadAverageEvent(
            MemberSecondDerivativeOfLoadAverageEvent memberSecondDerivativeOfLoadAverageEvent) {

        String instanceId = memberSecondDerivativeOfLoadAverageEvent.getInstanceId();
        String memberId = memberSecondDerivativeOfLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);

        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(
                                                                            member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberSecondDerivativeOfLoadAverageEvent.getValue();
        memberStatsContext.setSecondDerivativeOfLoadAverage(value);
    }

    @Override
    public void handleMemberFaultEvent(MemberFaultEvent memberFaultEvent) {

        String memberId = memberFaultEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String instanceId = memberFaultEvent.getInstanceId();
        String networkPartitionId = memberFaultEvent.getNetworkPartitionId();
        if (null == member) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member not found in the Topology: [member] %s", memberId));
            }
            return;
        }
        if (!member.isActive()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member activated event has not received for the member %s. "
                        + "Therefore ignoring" + " the member fault health stat", memberId));
            }
            return;
        }

        ClusterInstanceContext nwPartitionCtxt;
        nwPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        String partitionId = getPartitionOfMember(memberId);
        ClusterLevelPartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);
        if (!partitionCtxt.activeMemberExist(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Could not find the active member in partition context, "
                        + "[member] %s ", memberId));
            }
            return;
        }

        // move member to obsolete list
        synchronized (this) {
            partitionCtxt.moveMemberToObsoleteList(memberId);
        }
        if (log.isInfoEnabled()) {
            String clusterId = memberFaultEvent.getClusterId();
            log.info(String.format("Faulty member is added to obsolete list and removed from the active members list: "
                    + "[member] %s [partition] %s [cluster] %s ", memberId, partitionId, clusterId));
        }

        StatusChecker.getInstance().onMemberFaultEvent(memberFaultEvent.getClusterId(),
                partitionId, instanceId);
    }

    @Override
    public void handleMemberStartedEvent(
            MemberStartedEvent memberStartedEvent) {

    }

    @Override
    public void handleMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {

        String instanceId = memberActivatedEvent.getInstanceId();
        String networkPartitionId = memberActivatedEvent.getNetworkPartitionId();
        String partitionId = memberActivatedEvent.getPartitionId();
        String memberId = memberActivatedEvent.getMemberId();
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext clusterLevelPartitionContext;
        clusterLevelPartitionContext = networkPartitionCtxt.getPartitionCtxt(partitionId);
        clusterLevelPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isInfoEnabled()) {
            log.info(String.format("Member stat context has been added successfully: "
                    + "[member] %s", memberId));
        }
        clusterLevelPartitionContext.movePendingMemberToActiveMembers(memberId);
        StatusChecker.getInstance().onMemberStatusChange(memberActivatedEvent.getClusterId());
    }

    @Override
    public void handleMemberMaintenanceModeEvent(
            MemberMaintenanceModeEvent maintenanceModeEvent) {

        String networkPartitionId = maintenanceModeEvent.getNetworkPartitionId();
        String partitionId = maintenanceModeEvent.getPartitionId();
        String memberId = maintenanceModeEvent.getMemberId();
        String instanceId = maintenanceModeEvent.getInstanceId();
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext clusterMonitorPartitionContext = networkPartitionCtxt.
                                        getPartitionCtxt(partitionId);
        clusterMonitorPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Member has been moved as pending termination: "
                    + "[member] %s", memberId));
        }
        clusterMonitorPartitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
    }

    @Override
    public void handleMemberReadyToShutdownEvent(MemberReadyToShutdownEvent memberReadyToShutdownEvent) {

        ClusterInstanceContext nwPartitionCtxt;
        String networkPartitionId = memberReadyToShutdownEvent.getNetworkPartitionId();
        String instanceId = memberReadyToShutdownEvent.getInstanceId();
        nwPartitionCtxt = getClusterInstanceContext(instanceId,
                networkPartitionId);

        // start a new member in the same Partition
        String memberId = memberReadyToShutdownEvent.getMemberId();
        String partitionId = getPartitionOfMember(memberId);
        ClusterLevelPartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);
        // terminate the shutdown ready member
        //CloudControllerClient ccClient = CloudControllerClient.getInstance();

        try {
            //NetworkPartitionContext nwPartitionCtxt;
            //String networkPartitionId = memberReadyToShutdownEvent.getNetworkPartitionId();
            //nwPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);

            // start a new member in the same Partition
            //String memberId = memberReadyToShutdownEvent.getMemberId();
            String clusterId = memberReadyToShutdownEvent.getClusterId();
            //String partitionId = getPartitionOfMember(memberId);
            //PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);

            //move member to pending termination list
            if (partitionCtxt.getPendingTerminationMember(memberId) != null) {
                partitionCtxt.movePendingTerminationMemberToObsoleteMembers(memberId);
            } else if (partitionCtxt.getPendingTerminationMember(memberId) != null) {
                // add member to obsolete list since the member is shutdown ready member
                partitionCtxt.movePendingTerminationMemberToObsoleteMembers(memberId);
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("Member is terminated and removed from the active members list: [member] %s [partition] %s [cluster] %s ",
                        memberId, partitionId, clusterId));
            }
        } catch (Exception e) {
            String msg = "Error processing event " + e.getLocalizedMessage();
            log.error(msg, e);
        }
    }


    @Override
    public void handleMemberTerminatedEvent(
            MemberTerminatedEvent memberTerminatedEvent) {

        String networkPartitionId = memberTerminatedEvent.getNetworkPartitionId();
        String memberId = memberTerminatedEvent.getMemberId();
        String clusterId = memberTerminatedEvent.getClusterId();
        String instanceId = memberTerminatedEvent.getInstanceId();
        String partitionId = memberTerminatedEvent.getPartitionId();
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(instanceId,
                networkPartitionId);
        ClusterLevelPartitionContext clusterMonitorPartitionContext =
                clusterLevelNetworkPartitionContext.getPartitionCtxt(partitionId);
        clusterMonitorPartitionContext.removeMemberStatsContext(memberId);

        if (clusterMonitorPartitionContext.removeTerminationPendingMember(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member is removed from termination pending members list: "
                        + "[member] %s", memberId));
            }
        } else if (clusterMonitorPartitionContext.removePendingMember(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member is removed from pending members list: "
                        + "[member] %s", memberId));
            }
        } else if (clusterMonitorPartitionContext.removeActiveMemberById(memberId)) {
            log.warn(String.format("Member is in the wrong list and it is removed from "
                    + "active members list: %s", memberId));
        } else if (clusterMonitorPartitionContext.removeObsoleteMember(memberId)) {
            log.warn(String.format("Obsolete member has either been terminated or its obsolete time out has expired and"
                    + " it is removed from obsolete members list: %s", memberId));
        } else {
            log.warn(String.format("Member is not available in any of the list active, "
                    + "pending and termination pending: %s", memberId));
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Member stat context has been removed successfully: "
                    + "[member] %s", memberId));
        }
        //Checking whether the cluster state can be changed either from in_active to created/terminating to terminated
        StatusChecker.getInstance().onMemberTermination(clusterId, instanceId);
    }

    @Override
    public void handleClusterRemovedEvent(
            ClusterRemovedEvent clusterRemovedEvent) {

    }

    @Override
    public void handleDynamicUpdates(Properties properties) throws InvalidArgumentException {

    }

    private String getNetworkPartitionIdByMemberId(String memberId) {
        for (Service service : TopologyManager.getTopology().getServices()) {
            for (Cluster cluster : service.getClusters()) {
                if (cluster.memberExists(memberId)) {
                    return cluster.getMember(memberId).getNetworkPartitionId();
                }
            }
        }
        return null;
    }

    private Member getMemberByMemberId(String memberId) {
        try {
            TopologyManager.acquireReadLock();
            for (Service service : TopologyManager.getTopology().getServices()) {
                for (Cluster cluster : service.getClusters()) {
                    if (cluster.memberExists(memberId)) {
                        return cluster.getMember(memberId);
                    }
                }
            }
            return null;
        } finally {
            TopologyManager.releaseReadLock();
        }
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

    @Override
    public void terminateAllMembers() {

        Thread memberTerminator = new Thread(new Runnable() {
            public void run() {

                for (ClusterLevelNetworkPartitionContext networkPartitionContext : getAllNetworkPartitionCtxts().values()) {
                    for(ClusterInstanceContext instanceContext : networkPartitionContext.getClusterInstanceContextMap().values()) {
                        for (ClusterLevelPartitionContext partitionContext : instanceContext.getPartitionCtxts()) {
                            //if (log.isDebugEnabled()) {
                            log.info("Starting to terminate all members in cluster [" + getClusterId() + "] Network Partition [ " +
                                    networkPartitionContext.getId() + " ], Partition [ " +
                                    partitionContext.getPartitionId() + " ]");
                            // }
                            // need to terminate active, pending and obsolete members

                            // active members
                            for (MemberContext activeMemberCtxt : partitionContext.getActiveMembers()) {
                                log.info("Terminating active member [member id] " + activeMemberCtxt.getMemberId());
                                terminateMember(activeMemberCtxt.getMemberId());
                            }

                            // pending members
                            for (MemberContext pendingMemberCtxt : partitionContext.getPendingMembers()) {
                                log.info("Terminating pending member [member id] " + pendingMemberCtxt.getMemberId());
                                terminateMember(pendingMemberCtxt.getMemberId());
                            }

                            // obsolete members
                            for (String obsoleteMemberId : partitionContext.getObsoletedMembers().keySet()) {
                                log.info("Terminating obsolete member [member id] " + obsoleteMemberId);
                                terminateMember(obsoleteMemberId);
                            }

//                terminateAllFactHandle = AutoscalerRuleEvaluator.evaluateTerminateAll
//                        (terminateAllKnowledgeSession, terminateAllFactHandle, partitionContext);
                        }
                    }

                }
            }
        }, "Member Terminator - [cluster id] " + getClusterId());

        memberTerminator.start();
    }

    public Map<String, ClusterLevelNetworkPartitionContext> getAllNetworkPartitionCtxts() {
        return ((VMClusterContext)this.clusterContext).getNetworkPartitionCtxts();
    }

    public ClusterInstanceContext getClusterInstanceContext(String networkPartitionId, String instanceId) {
        Map<String, ClusterLevelNetworkPartitionContext> clusterLevelNetworkPartitionContextMap =
                ((VMClusterContext)this.clusterContext).getNetworkPartitionCtxts();
        ClusterLevelNetworkPartitionContext networkPartitionContext =
                clusterLevelNetworkPartitionContextMap.get(networkPartitionId);
        return networkPartitionContext.getClusterInstanceContextMap().get(instanceId);
    }

    private static void terminateMember(String memberId) {
        try {
            CloudControllerClient.getInstance().terminate(memberId);

        } catch (TerminationException e) {
            log.error("Unable to terminate member [member id ] " + memberId, e);
        }
    }

    public Collection<ClusterLevelNetworkPartitionContext> getNetworkPartitionCtxts() {
        return ((VMClusterContext)this.clusterContext).getNetworkPartitionCtxts().values();
    }


}
