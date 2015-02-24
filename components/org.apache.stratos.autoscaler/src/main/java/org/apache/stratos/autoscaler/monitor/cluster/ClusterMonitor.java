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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.cluster.AbstractClusterContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterContextFactory;
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.event.publisher.InstanceNotificationPublisher;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.exception.cartridge.TerminationException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingUpBeyondMaxEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.status.processor.cluster.ClusterStatusActiveProcessor;
import org.apache.stratos.autoscaler.status.processor.cluster.ClusterStatusInactiveProcessor;
import org.apache.stratos.autoscaler.status.processor.cluster.ClusterStatusTerminatedProcessor;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.GroupStatus;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class ClusterMonitor extends Monitor implements Runnable {

    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;

    protected FactHandle minCheckFactHandle;
    protected FactHandle obsoleteCheckFactHandle;
    protected FactHandle scaleCheckFactHandle;
    protected FactHandle dependentScaleCheckFactHandle;
    protected boolean hasFaultyMember = false;
    protected boolean stop = false;
    protected AbstractClusterContext clusterContext;
    protected StatefulKnowledgeSession minCheckKnowledgeSession;
    protected StatefulKnowledgeSession obsoleteCheckKnowledgeSession;
    protected StatefulKnowledgeSession scaleCheckKnowledgeSession;
    protected StatefulKnowledgeSession dependentScaleCheckKnowledgeSession;
    protected AutoscalerRuleEvaluator autoscalerRuleEvaluator;
    protected String serviceType;
    private AtomicBoolean monitoringStarted;
    protected String clusterId;
    private Cluster cluster;
    private int monitoringIntervalMilliseconds;
    private boolean isDestroyed;
    //has scaling dependents
    private boolean hasScalingDependents;
    private boolean groupScalingEnabledSubtree;

    private static final Log log = LogFactory.getLog(ClusterMonitor.class);
    private Map<String, ClusterLevelNetworkPartitionContext> networkPartitionIdToClusterLevelNetworkPartitionCtxts;
    private boolean hasPrimary;
    private float scalingFactorBasedOnDependencies = 1.0f;


    public ClusterMonitor(Cluster cluster, boolean hasScalingDependents, boolean groupScalingEnabledSubtree) {

        scheduler = StratosThreadPool.getScheduledExecutorService(AutoscalerConstants.CLUSTER_MONITOR_SCHEDULER_ID, 50);
        int threadPoolSize = Integer.getInteger(AutoscalerConstants.CLUSTER_MONITOR_THREAD_POOL_SIZE, 50);
        executorService = StratosThreadPool.getExecutorService(
                AutoscalerConstants.CLUSTER_MONITOR_THREAD_POOL_ID, threadPoolSize);

        networkPartitionIdToClusterLevelNetworkPartitionCtxts = new HashMap<String, ClusterLevelNetworkPartitionContext>();
        readConfigurations();
        autoscalerRuleEvaluator = new AutoscalerRuleEvaluator();
        autoscalerRuleEvaluator.parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.OBSOLETE_CHECK_DROOL_FILE);
        autoscalerRuleEvaluator.parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.SCALE_CHECK_DROOL_FILE);
        autoscalerRuleEvaluator.parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.MIN_CHECK_DROOL_FILE);
        autoscalerRuleEvaluator.parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.DEPENDENT_SCALE_CHECK_DROOL_FILE);

        this.obsoleteCheckKnowledgeSession = autoscalerRuleEvaluator.getStatefulSession(
                StratosConstants.OBSOLETE_CHECK_DROOL_FILE);
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getStatefulSession(
                StratosConstants.SCALE_CHECK_DROOL_FILE);
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getStatefulSession(
                StratosConstants.MIN_CHECK_DROOL_FILE);
        this.dependentScaleCheckKnowledgeSession = autoscalerRuleEvaluator.getStatefulSession(
                StratosConstants.DEPENDENT_SCALE_CHECK_DROOL_FILE);

        this.groupScalingEnabledSubtree = groupScalingEnabledSubtree;
        this.setCluster(new Cluster(cluster));
        this.serviceType = cluster.getServiceName();
        this.clusterId = cluster.getClusterId();
        this.monitoringStarted = new AtomicBoolean(false);
        this.hasScalingDependents = hasScalingDependents;
    }

    @Override
    public MonitorType getMonitorType() {
        return MonitorType.Cluster;
    }

    public void startScheduler() {
        scheduler.scheduleAtFixedRate(this, 0, getMonitorIntervalMilliseconds(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.clusterId == null) ? 0 : this.clusterId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ClusterMonitor)) {
            return false;
        }
        final ClusterMonitor other = (ClusterMonitor) obj;
        if (this.clusterId == null) {
            if (other.clusterId != null) {
                return false;
            }
        }
        if (!this.clusterId.equals(other.clusterId)) {
            return false;
        }
        return true;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void notifyParentMonitor(ClusterStatus status, String instanceId) {

        /**
         * notifying the parent monitor about the state change
         * If the cluster in_active and if it is a in_dependent cluster,
         * then won't send the notification to parent.
         */
        ClusterInstance instance = (ClusterInstance) this.instanceIdToInstanceMap.get(instanceId);
        if(instance == null) {
            log.warn("The required cluster [instance] " + instanceId + " not found in the ClusterMonitor");
        } else {
            if(instance.getStatus() != status) {
                instance.setStatus(status);
            }
            /*if (instance.getStatus() == ClusterStatus.Inactive && !this.hasStartupDependents) {
                log.info("[Cluster] " + clusterId + "is not notifying the parent, " +
                        "since it is identified as the independent unit");
            } else {*/
            MonitorStatusEventBuilder.handleClusterStatusEvent(this.parent, status, this.clusterId, instanceId);
            //}
        }
    }

    public int getMonitorIntervalMilliseconds() {
        return monitoringIntervalMilliseconds;
    }

    public void setMonitorIntervalMilliseconds(int monitorIntervalMilliseconds) {
        this.monitoringIntervalMilliseconds = monitorIntervalMilliseconds;
    }

    public FactHandle getMinCheckFactHandle() {
        return minCheckFactHandle;
    }

    public void setMinCheckFactHandle(FactHandle minCheckFactHandle) {
        this.minCheckFactHandle = minCheckFactHandle;
    }

    public FactHandle getObsoleteCheckFactHandle() {
        return obsoleteCheckFactHandle;
    }

    public void setObsoleteCheckFactHandle(FactHandle obsoleteCheckFactHandle) {
        this.obsoleteCheckFactHandle = obsoleteCheckFactHandle;
    }

    public FactHandle getScaleCheckFactHandle() {
        return scaleCheckFactHandle;
    }

    public void setScaleCheckFactHandle(FactHandle scaleCheckFactHandle) {
        this.scaleCheckFactHandle = scaleCheckFactHandle;
    }

    public StatefulKnowledgeSession getMinCheckKnowledgeSession() {
        return minCheckKnowledgeSession;
    }

    public void setMinCheckKnowledgeSession(
            StatefulKnowledgeSession minCheckKnowledgeSession) {
        this.minCheckKnowledgeSession = minCheckKnowledgeSession;
    }

    public StatefulKnowledgeSession getObsoleteCheckKnowledgeSession() {
        return obsoleteCheckKnowledgeSession;
    }

    public void setObsoleteCheckKnowledgeSession(
            StatefulKnowledgeSession obsoleteCheckKnowledgeSession) {
        this.obsoleteCheckKnowledgeSession = obsoleteCheckKnowledgeSession;
    }

    public StatefulKnowledgeSession getScaleCheckKnowledgeSession() {
        return scaleCheckKnowledgeSession;
    }

    public void setScaleCheckKnowledgeSession(
            StatefulKnowledgeSession scaleCheckKnowledgeSession) {
        this.scaleCheckKnowledgeSession = scaleCheckKnowledgeSession;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }

    public AutoscalerRuleEvaluator getAutoscalerRuleEvaluator() {
        return autoscalerRuleEvaluator;
    }

    public void setAutoscalerRuleEvaluator(
            AutoscalerRuleEvaluator autoscalerRuleEvaluator) {
        this.autoscalerRuleEvaluator = autoscalerRuleEvaluator;
    }

    public boolean isHasFaultyMember() {
        return hasFaultyMember;
    }

    public void setHasFaultyMember(boolean hasFaultyMember) {
        this.hasFaultyMember = hasFaultyMember;
    }

    /*public void addClusterContextForInstance (String instanceId, AbstractClusterContext clusterContext) {

        if (instanceIdToClusterContextMap.get(instanceId) == null) {
            synchronized (instanceIdToClusterContextMap) {
                if (instanceIdToClusterContextMap.get(instanceId) == null) {
                    instanceIdToClusterContextMap.put(instanceId, clusterContext);
                } else {
                    log.warn("ClusterContext for already exists for cluster instance id: " + instanceId +
                            ", service type: " + serviceType + ", cluster id: " + clusterId);
                }
            }
        } else {
            log.warn("ClusterContext for already exists for cluster instance id: " + instanceId +
                    ", service type: " + serviceType + ", cluster id: " + clusterId);
        }
    }

    public AbstractClusterContext getClusterContext (String instanceId) {

        // if instanceId is null, assume that map contains only one element and return that

        return instanceIdToClusterContextMap.get(instanceId);
    }*/

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public String getServiceId() {
        return serviceType;
    }

    protected int getRoundedInstanceCount(float requiredInstances, float fraction) {

        return (requiredInstances - Math.floor(requiredInstances) > fraction) ? (int) Math.ceil(requiredInstances)
                : (int) Math.floor(requiredInstances);
    }

    public AtomicBoolean hasMonitoringStarted() {
        return monitoringStarted;
    }

    public void setMonitoringStarted(boolean monitoringStarted) {
        this.monitoringStarted.set(monitoringStarted);
    }

    public StatefulKnowledgeSession getDependentScaleCheckKnowledgeSession() {
        return dependentScaleCheckKnowledgeSession;
    }

    public void setDependentScaleCheckKnowledgeSession(StatefulKnowledgeSession dependentScaleCheckKnowledgeSession) {
        this.dependentScaleCheckKnowledgeSession = dependentScaleCheckKnowledgeSession;
    }

    public AbstractClusterContext getClusterContext() {
        return clusterContext;
    }

    public void setClusterContext(AbstractClusterContext clusterContext) {
        this.clusterContext = clusterContext;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public boolean hasScalingDependents() {
        return hasScalingDependents;
    }

    public boolean groupScalingEnabledSubtree() {

        return groupScalingEnabledSubtree;
    }
    private static void terminateMember(String memberId) {
        try {
            CloudControllerClient.getInstance().terminateInstance(memberId);

        } catch (TerminationException e) {
            log.error("Unable to terminate member: [member-id] " + memberId, e);
        }
    }

    private static void createClusterInstance(String serviceType, String clusterId, String alias,
                                              String instanceId, String partitionId, String networkPartitionId) {
        CloudControllerClient.getInstance().createClusterInstance(serviceType, clusterId, alias,
                instanceId, partitionId, networkPartitionId);
    }

    public void addClusterLevelNWPartitionContext(ClusterLevelNetworkPartitionContext clusterLevelNWPartitionCtxt) {
        networkPartitionIdToClusterLevelNetworkPartitionCtxts.put(clusterLevelNWPartitionCtxt.getId(), clusterLevelNWPartitionCtxt);
    }

    public ClusterLevelNetworkPartitionContext getClusterLevelNWPartitionContext(String nwPartitionId) {
        return networkPartitionIdToClusterLevelNetworkPartitionCtxts.get(nwPartitionId);
    }

    public void handleAverageLoadAverageEvent(
            AverageLoadAverageEvent averageLoadAverageEvent) {

        String networkPartitionId = averageLoadAverageEvent.getNetworkPartitionId();
        String clusterId = averageLoadAverageEvent.getClusterId();
        String clusterInstanceId = averageLoadAverageEvent.getClusterInstanceId();
        float value = averageLoadAverageEvent.getValue();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg load avg event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }

        ClusterInstanceContext clusterInstanceContext = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
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
                if (log.isDebugEnabled()) {
                    log.debug("Cluster monitor is running.. " + this.toString());
                }
                monitor();
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

    public synchronized void monitor() {

        try {
            for (ClusterLevelNetworkPartitionContext networkPartitionContext : getNetworkPartitionCtxts()) {

                final Collection<InstanceContext> clusterInstanceContexts = networkPartitionContext.
                        getInstanceIdToInstanceContextMap().values();

                for (final InstanceContext pInstanceContext : clusterInstanceContexts) {
                    final ClusterInstanceContext instanceContext = (ClusterInstanceContext) pInstanceContext;
                    ClusterInstance instance = (ClusterInstance) this.instanceIdToInstanceMap.
                            get(instanceContext.getId());

                    if ((instance.getStatus().getCode() <= ClusterStatus.Active.getCode()) ||
                            (instance.getStatus() == ClusterStatus.Inactive && !hasStartupDependents)
                                    && !this.hasFaultyMember) {

                        Runnable monitoringRunnable = new Runnable() {
                            @Override
                            public void run() {

                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Cluster monitor is running: [application-id] %s [cluster-id]: " +
                                            "%s", getAppId(), getClusterId()));
                                }
                                // store primary members in the cluster instance context
                                List<String> primaryMemberListInClusterInstance = new ArrayList<String>();

                                for (ClusterLevelPartitionContext partitionContext :
                                        instanceContext.getPartitionCtxts()) {

                                    // get active primary members in this cluster instance context
                                    for (MemberContext memberContext : partitionContext.getActiveMembers()) {
                                        if (isPrimaryMember(memberContext)) {
                                            primaryMemberListInClusterInstance.add(memberContext.getMemberId());
                                        }
                                    }

                                    // get pending primary members in this cluster instance context
                                    for (MemberContext memberContext : partitionContext.getPendingMembers()) {
                                        if (isPrimaryMember(memberContext)) {
                                            primaryMemberListInClusterInstance.add(memberContext.getMemberId());
                                        }
                                    }
                                }

                                getMinCheckKnowledgeSession().setGlobal("primaryMemberCount",
                                        primaryMemberListInClusterInstance.size());
                                getMinCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                                getMinCheckKnowledgeSession().setGlobal("isPrimary", hasPrimary);
                                //FIXME when parent chosen the partition
                                String paritionAlgo = instanceContext.getPartitionAlgorithm();

                                getMinCheckKnowledgeSession().setGlobal("algorithmName",
                                        paritionAlgo);

                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Running minimum check for cluster instance %s ",
                                            instanceContext.getId() + " for the cluster: " + clusterId));
                                }

                                minCheckFactHandle = AutoscalerRuleEvaluator.evaluate(getMinCheckKnowledgeSession(),
                                        minCheckFactHandle, instanceContext);


                                //checking the status of the cluster
                                boolean rifReset = instanceContext.isRifReset();
                                boolean memoryConsumptionReset = instanceContext.isMemoryConsumptionReset();
                                boolean loadAverageReset = instanceContext.isLoadAverageReset();
                                boolean averageRequestServedPerInstanceReset
                                        = instanceContext.isAverageRequestServedPerInstanceReset();

                                if (log.isDebugEnabled()) {
                                    log.debug("Execution point of scaling Rule, [Is rif Reset] : " + rifReset
                                            + " [Is memoryConsumption Reset] : " + memoryConsumptionReset
                                            + " [Is loadAverage Reset] : " + loadAverageReset);
                                }

                                if (rifReset || memoryConsumptionReset || loadAverageReset) {

                                    log.info("Executing scaling rule as statistics have been reset");
                                    ClusterContext clusterContext = (ClusterContext) ClusterMonitor.this.clusterContext;

                                    getScaleCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                                    getScaleCheckKnowledgeSession().setGlobal("rifReset", rifReset);
                                    getScaleCheckKnowledgeSession().setGlobal("mcReset", memoryConsumptionReset);
                                    getScaleCheckKnowledgeSession().setGlobal("laReset", loadAverageReset);
                                    getScaleCheckKnowledgeSession().setGlobal("isPrimary", hasPrimary);
                                    getScaleCheckKnowledgeSession().setGlobal("algorithmName", paritionAlgo);
                                    getScaleCheckKnowledgeSession().setGlobal("autoscalePolicy",
                                            clusterContext.getAutoscalePolicy());
                                    getScaleCheckKnowledgeSession().setGlobal("arspiReset",
                                            averageRequestServedPerInstanceReset);
                                    getScaleCheckKnowledgeSession().setGlobal("primaryMembers",
                                            primaryMemberListInClusterInstance);

                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("Running scale check for [cluster instance context] %s ",
                                                instanceContext.getId()));
                                        log.debug(" Primary members : " + primaryMemberListInClusterInstance);
                                    }

                                    scaleCheckFactHandle = AutoscalerRuleEvaluator.evaluate(getScaleCheckKnowledgeSession()
                                            , scaleCheckFactHandle, instanceContext);

                                    instanceContext.setRifReset(false);
                                    instanceContext.setMemoryConsumptionReset(false);
                                    instanceContext.setLoadAverageReset(false);
                                } else if (log.isDebugEnabled()) {
                                    log.debug(String.format("Scale rule will not run since the LB statistics have not " +
                                                    "received before this cycle for [cluster instance context] %s [cluster] %s",
                                            instanceContext.getId(), clusterId));
                                }

                            }
                        };
                        executorService.execute(monitoringRunnable);
                    }

                    for (final ClusterLevelPartitionContext partitionContext : instanceContext.getPartitionCtxts()) {
                        Runnable monitoringRunnable = new Runnable() {
                            @Override
                            public void run() {
                                getObsoleteCheckKnowledgeSession().setGlobal("clusterId", clusterId);
                                obsoleteCheckFactHandle = AutoscalerRuleEvaluator.evaluate(
                                        getObsoleteCheckKnowledgeSession(), obsoleteCheckFactHandle, partitionContext);
                            }
                        };
                        executorService.execute(monitoringRunnable);
                    }
                }
            }
        } catch (RejectedExecutionException ignore) {
            log.warn("Cluster monitor execution rejected: [cluster-id] " + getClusterId());
        }
    }

    private void readConfigurations() {
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        int monitorInterval = conf.getInt(AutoscalerConstants.Cluster_MONITOR_INTERVAL, 90000);
        setMonitorIntervalMilliseconds(monitorInterval);
        if (log.isDebugEnabled()) {
            log.debug("ClusterMonitor task interval set to : " + getMonitorIntervalMilliseconds());
        }
    }

    @Override
    public void destroy() {
        getMinCheckKnowledgeSession().dispose();
        getObsoleteCheckKnowledgeSession().dispose();
        getScaleCheckKnowledgeSession().dispose();
        setDestroyed(true);
        if (log.isDebugEnabled()) {
            log.debug("ClusterMonitor Drools session has been disposed. " + this.toString());
        }
    }

    @Override
    public String toString() {
        return "ClusterMonitor [clusterId=" + getClusterId() +
                ", hasPrimary=" + hasPrimary + " ]";
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
                log.info("Publishing Cluster terminating event for [application] " + appId +
                        " [cluster] " + this.getClusterId() + " [instance] " + instanceId);
            }
            ClusterStatusEventPublisher.sendClusterTerminatingEvent(getAppId(), getServiceId(), getClusterId(), instanceId);
        }
    }

    @Override
    public void onChildScalingEvent(ScalingEvent scalingEvent) {

    }

    @Override
    public void onChildScalingOverMaxEvent(ScalingUpBeyondMaxEvent scalingUpBeyondMaxEvent) {

    }

    @Override
    public void onParentScalingEvent(ScalingEvent scalingEvent) {


        log.info("Parent scaling event received to [cluster]: " + this.getClusterId()
                    + ", [network partition]: " + scalingEvent.getNetworkPartitionId()
                    + ", [event] " + scalingEvent.getId() + ", [group instance] " + scalingEvent.getInstanceId()
                    + ", [factor] " + scalingEvent.getFactor());


        this.scalingFactorBasedOnDependencies = scalingEvent.getFactor();
        ClusterContext vmClusterContext = (ClusterContext) clusterContext;
        String instanceId = scalingEvent.getInstanceId();

        ClusterInstanceContext clusterInstanceContext =
                getClusterInstanceContext(scalingEvent.getNetworkPartitionId(), instanceId);


        // store primary members in the cluster instance context
        List<String> primaryMemberListInClusterInstance = new ArrayList<String>();

        for (ClusterLevelPartitionContext partitionContext : clusterInstanceContext.getPartitionCtxts()) {

            // get active primary members in this cluster instance context
            for (MemberContext memberContext : partitionContext.getActiveMembers()) {
                if (isPrimaryMember(memberContext)) {
                    primaryMemberListInClusterInstance.add(memberContext.getMemberId());
                }
            }

            // get pending primary members in this cluster instance context
            for (MemberContext memberContext : partitionContext.getPendingMembers()) {
                if (isPrimaryMember(memberContext)) {
                    primaryMemberListInClusterInstance.add(memberContext.getMemberId());
                }
            }
        }


        //TODO get min instance count from instance context
        float requiredInstanceCount = clusterInstanceContext.getMinInstanceCount() * scalingFactorBasedOnDependencies;
        int roundedRequiredInstanceCount = getRoundedInstanceCount(requiredInstanceCount,
                vmClusterContext.getAutoscalePolicy().getInstanceRoundingFactor());
        clusterInstanceContext.setRequiredInstanceCountBasedOnDependencies(roundedRequiredInstanceCount);

        getDependentScaleCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
        getDependentScaleCheckKnowledgeSession().setGlobal("roundedRequiredInstanceCount", roundedRequiredInstanceCount);
        getDependentScaleCheckKnowledgeSession().setGlobal("algorithmName", clusterInstanceContext.getPartitionAlgorithm());
        getDependentScaleCheckKnowledgeSession().setGlobal("isPrimary", hasPrimary);
        getDependentScaleCheckKnowledgeSession().setGlobal("primaryMembers", primaryMemberListInClusterInstance);
        dependentScaleCheckFactHandle = AutoscalerRuleEvaluator.evaluate(getDependentScaleCheckKnowledgeSession()
                , dependentScaleCheckFactHandle, clusterInstanceContext);

    }

    public void sendClusterScalingEvent(String networkPartitionId, String instanceId, float factor) {

        MonitorStatusEventBuilder.handleClusterScalingEvent(this.parent, networkPartitionId, instanceId, factor, this.id);
    }

    public void sendScalingOverMaxEvent(String networkPartitionId, String instanceId) {

        MonitorStatusEventBuilder.handleScalingOverMaxEvent(this.parent, networkPartitionId, instanceId,
                this.id);
    }

    public void sendScalingDownBeyondMinEvent(String networkPartitionId, String instanceId) {

        MonitorStatusEventBuilder.handleScalingDownBeyondMinEvent(this.parent, networkPartitionId, instanceId,
                this.id);
    }

    public void handleGradientOfLoadAverageEvent(
            GradientOfLoadAverageEvent gradientOfLoadAverageEvent) {

        String networkPartitionId = gradientOfLoadAverageEvent.getNetworkPartitionId();
        String clusterId = gradientOfLoadAverageEvent.getClusterId();
        String instanceId = gradientOfLoadAverageEvent.getClusterInstanceId();
        float value = gradientOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of load avg event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, instanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setLoadAverageGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleSecondDerivativeOfLoadAverageEvent(
            SecondDerivativeOfLoadAverageEvent secondDerivativeOfLoadAverageEvent) {

        String networkPartitionId = secondDerivativeOfLoadAverageEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfLoadAverageEvent.getClusterId();
        String clusterInstanceId = secondDerivativeOfLoadAverageEvent.getClusterInstanceId();
        float value = secondDerivativeOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of load avg event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setLoadAverageSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleAverageMemoryConsumptionEvent(
            AverageMemoryConsumptionEvent averageMemoryConsumptionEvent) {

        String networkPartitionId = averageMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = averageMemoryConsumptionEvent.getClusterId();
        String clusterInstanceId = averageMemoryConsumptionEvent.getClusterInstanceId();
        float value = averageMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg Memory Consumption event: [cluster] %s [network-partition] %s "
                    + "[value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
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

    public void handleGradientOfMemoryConsumptionEvent(
            GradientOfMemoryConsumptionEvent gradientOfMemoryConsumptionEvent) {

        String networkPartitionId = gradientOfMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = gradientOfMemoryConsumptionEvent.getClusterId();
        String clusterInstanceId = gradientOfMemoryConsumptionEvent.getClusterInstanceId();
        float value = gradientOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of Memory Consumption event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setMemoryConsumptionGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleSecondDerivativeOfMemoryConsumptionEvent(
            SecondDerivativeOfMemoryConsumptionEvent secondDerivativeOfMemoryConsumptionEvent) {

        String networkPartitionId = secondDerivativeOfMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfMemoryConsumptionEvent.getClusterId();
        String clusterInstanceId = secondDerivativeOfMemoryConsumptionEvent.getClusterInstanceId();
        float value = secondDerivativeOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of Memory Consumption event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setMemoryConsumptionSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleAverageRequestsServingCapabilityEvent(
            AverageRequestsServingCapabilityEvent averageRequestsServingCapabilityEvent) {

        String clusterId = averageRequestsServingCapabilityEvent.getClusterId();
        String clusterInstanceId = averageRequestsServingCapabilityEvent.getClusterInstanceId();
        String networkPartitionId = averageRequestsServingCapabilityEvent.getNetworkPartitionId();
        Float floatValue = averageRequestsServingCapabilityEvent.getValue();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Requests Served per Instance event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, floatValue));
        }

        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setAverageRequestsServedPerInstance(floatValue);

        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }

    }

    public void handleAverageRequestsInFlightEvent(
            AverageRequestsInFlightEvent averageRequestsInFlightEvent) {

        String networkPartitionId = averageRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = averageRequestsInFlightEvent.getClusterId();
        String clusterInstanceId = averageRequestsInFlightEvent.getClusterInstanceId();
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
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setAverageRequestsInFlight(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleGradientOfRequestsInFlightEvent(
            GradientOfRequestsInFlightEvent gradientOfRequestsInFlightEvent) {

        String networkPartitionId = gradientOfRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = gradientOfRequestsInFlightEvent.getClusterId();
        String clusterInstanceId = gradientOfRequestsInFlightEvent.getClusterInstanceId();
        float value = gradientOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Gradient of Rif event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setRequestsInFlightGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleSecondDerivativeOfRequestsInFlightEvent(
            SecondDerivativeOfRequestsInFlightEvent secondDerivativeOfRequestsInFlightEvent) {

        String networkPartitionId = secondDerivativeOfRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfRequestsInFlightEvent.getClusterId();
        String clusterInstanceId = secondDerivativeOfRequestsInFlightEvent.getClusterInstanceId();
        float value = secondDerivativeOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second derivative of Rif event: [cluster] %s "
                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        if (null != clusterLevelNetworkPartitionContext) {
            clusterLevelNetworkPartitionContext.setRequestsInFlightSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleMemberAverageMemoryConsumptionEvent(
            MemberAverageMemoryConsumptionEvent memberAverageMemoryConsumptionEvent) {

        String clusterInstanceId = memberAverageMemoryConsumptionEvent.getClusterInstanceId();
        String memberId = memberAverageMemoryConsumptionEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
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

    public void handleMemberGradientOfMemoryConsumptionEvent(
            MemberGradientOfMemoryConsumptionEvent memberGradientOfMemoryConsumptionEvent) {

        String clusterInstanceId = memberGradientOfMemoryConsumptionEvent.getClusterInstanceId();
        String memberId = memberGradientOfMemoryConsumptionEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
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

    public void handleMemberSecondDerivativeOfMemoryConsumptionEvent(
            MemberSecondDerivativeOfMemoryConsumptionEvent memberSecondDerivativeOfMemoryConsumptionEvent) {

    }

    public void handleMemberAverageLoadAverageEvent(
            MemberAverageLoadAverageEvent memberAverageLoadAverageEvent) {

        String clusterInstanceId = memberAverageLoadAverageEvent.getClusterInstanceId();
        String memberId = memberAverageLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
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

    public void handleMemberGradientOfLoadAverageEvent(
            MemberGradientOfLoadAverageEvent memberGradientOfLoadAverageEvent) {

        String clusterInstanceId = memberGradientOfLoadAverageEvent.getClusterInstanceId();
        String memberId = memberGradientOfLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
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

    public void handleMemberSecondDerivativeOfLoadAverageEvent(
            MemberSecondDerivativeOfLoadAverageEvent memberSecondDerivativeOfLoadAverageEvent) {

        String clusterInstanceId = memberSecondDerivativeOfLoadAverageEvent.getClusterInstanceId();
        String memberId = memberSecondDerivativeOfLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);

        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
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

    public void handleMemberFaultEvent(MemberFaultEvent memberFaultEvent) {

        String memberId = memberFaultEvent.getMemberId();
        String clusterId = memberFaultEvent.getClusterId();
        String clusterInstanceId = memberFaultEvent.getClusterInstanceId();
        Member member = getMemberByMemberId(memberId);
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
        nwPartitionCtxt = getClusterInstanceContext(networkPartitionId, clusterInstanceId);
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
        if (log.isDebugEnabled()) {
            log.debug(String.format("Faulty member is added to obsolete list and removed from the active members list: "
                    + "[member] %s [partition] %s [cluster] %s ", memberId, partitionId, clusterId));
        }

        ServiceReferenceHolder.getInstance().getClusterStatusProcessorChain().process(
                ClusterStatusInactiveProcessor.class.getName(), clusterId, clusterInstanceId);
    }

    public void handleMemberStartedEvent(
            MemberStartedEvent memberStartedEvent) {

    }

    public void handleMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {

        String clusterId = memberActivatedEvent.getClusterId();
        String clusterInstanceId = memberActivatedEvent.getClusterInstanceId();
        String memberId = memberActivatedEvent.getMemberId();
        String networkPartitionId = memberActivatedEvent.getNetworkPartitionId();
        String partitionId = memberActivatedEvent.getPartitionId();

        ClusterInstanceContext clusterInstanceContext = getClusterInstanceContext(networkPartitionId, clusterInstanceId);
        ClusterLevelPartitionContext clusterLevelPartitionContext;
        clusterLevelPartitionContext = clusterInstanceContext.getPartitionCtxt(partitionId);
        clusterLevelPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Member stat context has been added successfully: "
                    + "[member] %s", memberId));
        }
        clusterLevelPartitionContext.movePendingMemberToActiveMembers(memberId);
        ServiceReferenceHolder.getInstance().getClusterStatusProcessorChain().process(
                ClusterStatusActiveProcessor.class.getName(), clusterId, clusterInstanceId);
    }

    public void handleMemberMaintenanceModeEvent(
            MemberMaintenanceModeEvent maintenanceModeEvent) {

        String networkPartitionId = maintenanceModeEvent.getNetworkPartitionId();
        String partitionId = maintenanceModeEvent.getPartitionId();
        String memberId = maintenanceModeEvent.getMemberId();
        String clusterInstanceId = maintenanceModeEvent.getClusterInstanceId();
        ClusterInstanceContext networkPartitionCtxt = getClusterInstanceContext(networkPartitionId,
                clusterInstanceId);
        ClusterLevelPartitionContext clusterMonitorPartitionContext = networkPartitionCtxt.
                getPartitionCtxt(partitionId);
        clusterMonitorPartitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Member has been moved as pending termination: "
                    + "[member] %s", memberId));
        }
        clusterMonitorPartitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
    }

    public void handleMemberReadyToShutdownEvent(MemberReadyToShutdownEvent memberReadyToShutdownEvent) {

        ClusterInstanceContext nwPartitionCtxt;
        String networkPartitionId = memberReadyToShutdownEvent.getNetworkPartitionId();
        String clusterInstanceId = memberReadyToShutdownEvent.getClusterInstanceId();
        nwPartitionCtxt = getClusterInstanceContext(networkPartitionId, clusterInstanceId);

        // start a new member in the same Partition
        String memberId = memberReadyToShutdownEvent.getMemberId();
        String partitionId = getPartitionOfMember(memberId);
        ClusterLevelPartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);

        try {
            String clusterId = memberReadyToShutdownEvent.getClusterId();
            //move member to pending termination list
            if (partitionCtxt.getPendingTerminationMember(memberId) != null) {
                partitionCtxt.movePendingTerminationMemberToObsoleteMembers(memberId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Member is removed from the pending termination members " +
                            "and moved to obsolete list: [member] %s " +
                            "[partition] %s [cluster] %s ", memberId, partitionId, clusterId));
                }
            } else if (partitionCtxt.getObsoleteMember(memberId) != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Member is  in obsolete list: [member] %s " +
                            "[partition] %s [cluster] %s ", memberId, partitionId, clusterId));
                }
            } //TODO else part

            //when no more members are there to terminate Invoking it monitor directly
            // to speed up the termination process
            if (partitionCtxt.getTotalMemberCount() == 0) {
                this.monitor();
            }


        } catch (Exception e) {
            String msg = "Error processing event " + e.getLocalizedMessage();
            log.error(msg, e);
        }
    }

    public void handleMemberTerminatedEvent(
            MemberTerminatedEvent memberTerminatedEvent) {

        String networkPartitionId = memberTerminatedEvent.getNetworkPartitionId();
        String memberId = memberTerminatedEvent.getMemberId();
        String clusterId = memberTerminatedEvent.getClusterId();
        String clusterInstanceId = memberTerminatedEvent.getClusterInstanceId();
        String partitionId = memberTerminatedEvent.getPartitionId();
        ClusterInstanceContext clusterLevelNetworkPartitionContext = getClusterInstanceContext(
                networkPartitionId, clusterInstanceId);
        ClusterLevelPartitionContext clusterMonitorPartitionContext =
                clusterLevelNetworkPartitionContext.getPartitionCtxt(partitionId);

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

        if (log.isDebugEnabled()) {
            log.debug(String.format("Member stat context has been removed successfully: "
                    + "[member] %s", memberId));
        }
        //Checking whether the cluster state can be changed either from in_active to created/terminating to terminated
        ServiceReferenceHolder.getInstance().getClusterStatusProcessorChain().process(
                ClusterStatusTerminatedProcessor.class.getName(), clusterId, clusterInstanceId);
    }

    public void handleClusterRemovedEvent(
            ClusterRemovedEvent clusterRemovedEvent) {

    }

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

    public void terminateAllMembers(final String instanceId, final String networkPartitionId) {
        final ClusterMonitor monitor = this;
        Thread memberTerminator = new Thread(new Runnable() {
            public void run() {

                ClusterInstanceContext instanceContext =
                        (ClusterInstanceContext) getAllNetworkPartitionCtxts().get(networkPartitionId)
                                                                    .getInstanceContext(instanceId);
                boolean allMovedToObsolete = true;
                for (ClusterLevelPartitionContext partitionContext : instanceContext.getPartitionCtxts()) {
                    if (log.isInfoEnabled()) {
                        log.info("Starting to terminate all members in cluster [" + getClusterId() + "] " +
                                "Network Partition [" + instanceContext.getNetworkPartitionId() + "], Partition [" +
                                partitionContext.getPartitionId() + "]");
                    }
                    // need to terminate active, pending and obsolete members
                    //FIXME to traverse concurrent
                    // active members
                    List<String> activeMemberIdList = new ArrayList<String>();
                    Iterator<MemberContext> iterator = partitionContext.getActiveMembers().listIterator();
                    while (iterator.hasNext()) {
                        MemberContext activeMemberCtxt = iterator.next();
                        activeMemberIdList.add(activeMemberCtxt.getMemberId());

                    }
                    for (String memberId : activeMemberIdList) {
                        log.info("Sending instance cleanup event for the active member: [member-id] " + memberId);
                        partitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
                        InstanceNotificationPublisher.getInstance().sendInstanceCleanupEventForMember(memberId);
                    }

                    Iterator<MemberContext> pendingIterator = partitionContext.getPendingMembers().listIterator();
                    List<String> pendingMemberIdList = new ArrayList<String>();
                    while (pendingIterator.hasNext()) {
                        MemberContext activeMemberCtxt = pendingIterator.next();
                        pendingMemberIdList.add(activeMemberCtxt.getMemberId());

                    }
                    for (String memberId : pendingMemberIdList) {
                        // pending members
                        if (log.isDebugEnabled()) {
                            log.debug("Moving pending member [member id] " + memberId + " to obsolete list");
                        }
                        partitionContext.movePendingMemberToObsoleteMembers(memberId);
                    }
                    if(partitionContext.getTotalMemberCount() == 0) {
                        allMovedToObsolete = allMovedToObsolete && true;
                    } else {
                        allMovedToObsolete = false;
                    }
                }

                if(allMovedToObsolete) {
                    monitor.monitor();
                }
            }
        }, "Member Terminator - [cluster id] " + getClusterId());

        memberTerminator.start();
    }

    public Map<String, ClusterLevelNetworkPartitionContext> getAllNetworkPartitionCtxts() {
        return ((ClusterContext) this.clusterContext).getNetworkPartitionCtxts();
    }

    public ClusterInstanceContext getClusterInstanceContext(String networkPartitionId, String instanceId) {Map<String, ClusterLevelNetworkPartitionContext> clusterLevelNetworkPartitionContextMap =
                ((ClusterContext) this.clusterContext).getNetworkPartitionCtxts();
        if(StringUtils.isBlank(networkPartitionId)) {
            throw new RuntimeException("Network partition id is null");
        }
        ClusterLevelNetworkPartitionContext networkPartitionContext =
                clusterLevelNetworkPartitionContextMap.get(networkPartitionId);
        if(networkPartitionContext == null) {
            throw new RuntimeException("Network partition context not found: [network-partition-id] " +
            networkPartitionId);
        }
        ClusterInstanceContext instanceContext = (ClusterInstanceContext) networkPartitionContext.
                                                        getInstanceContext(instanceId);
        return instanceContext;
    }

    public Collection<ClusterLevelNetworkPartitionContext> getNetworkPartitionCtxts() {
        return ((ClusterContext) this.clusterContext).getNetworkPartitionCtxts().values();
    }

    public void createClusterInstances(List<String> parentInstanceIds, Cluster cluster)
            throws PolicyValidationException, PartitionValidationException {
        for (String parentInstanceId : parentInstanceIds) {
            createInstance(parentInstanceId, cluster);
        }
    }

    public boolean createInstanceOnDemand(String instanceId) {
        Cluster cluster = TopologyManager.getTopology().getService(this.serviceType).
                getCluster(this.clusterId);
        try {
            return createInstance(instanceId, cluster);
            //TODO exception
        } catch (PolicyValidationException e) {
            log.error("Error while creating the cluster instance", e);
        } catch (PartitionValidationException e) {
            log.error("Error while creating the cluster instance", e);

        }
        return false;

    }

    private boolean createInstance(String parentInstanceId, Cluster cluster)
            throws PolicyValidationException, PartitionValidationException {
        Instance parentMonitorInstance = this.parent.getInstance(parentInstanceId);
        String partitionId = null;
        if (parentMonitorInstance instanceof GroupInstance) {
            partitionId = parentMonitorInstance.getPartitionId();
        }
        if (parentMonitorInstance != null) {

            ClusterInstance clusterInstance = cluster.getInstanceContexts(parentInstanceId);
            if (clusterInstance != null) {

                // Cluster instance is already there. No need to create one.
                ClusterContext clusterContext = (ClusterContext) this.getClusterContext();
                if (clusterContext == null) {
                    clusterContext = ClusterContextFactory.getVMClusterContext(clusterInstance.getInstanceId(), cluster,
                            hasScalingDependents());
                    this.setClusterContext(clusterContext);
                }

                // create VMClusterContext and then add all the instanceContexts
                clusterContext.addInstanceContext(parentInstanceId, cluster, hasScalingDependents(),
                        groupScalingEnabledSubtree());
                if (this.getInstance(clusterInstance.getInstanceId()) == null) {
                    this.addInstance(clusterInstance);
                }
                // Checking the current status of the cluster instance
                boolean stateChanged =
                        ServiceReferenceHolder.getInstance().getClusterStatusProcessorChain()
                                .process("", cluster.getClusterId(), clusterInstance.getInstanceId());
                if (!stateChanged && clusterInstance.getStatus() != ClusterStatus.Created) {
                    this.notifyParentMonitor(clusterInstance.getStatus(),
                            clusterInstance.getInstanceId());
                }
                if (this.hasMonitoringStarted().compareAndSet(false, true)) {
                    this.startScheduler();
                    log.info(String.format("Monitoring task for cluster monitor started: [cluster-id] %s",
                            cluster.getClusterId()));
                }
            } else {
                createClusterInstance(cluster.getServiceName(), cluster.getClusterId(), null, parentInstanceId, partitionId,
                        parentMonitorInstance.getNetworkPartitionId());
                log.debug(String.format("Cluster instance created: [application-id] %s [service-name] %s " +
                        "[cluster-id] %s", appId, cluster.getServiceName(), cluster.getClusterId()));
            }
            return true;

        } else {
            return false;

        }

    }

    /**
     * Move all the members of the cluster instance to termiantion pending
     *
     * @param instanceId
     */
    public void moveMembersFromActiveToPendingTermination(String instanceId) {

        //TODO take read lock for network partition context
        //FIXME to iterate properly
        for (ClusterLevelNetworkPartitionContext networkPartitionContext :
                ((ClusterContext) this.clusterContext).getNetworkPartitionCtxts().values()) {
            ClusterInstanceContext clusterInstanceContext =
                    (ClusterInstanceContext) networkPartitionContext.getInstanceContext(instanceId);
            if (clusterInstanceContext != null) {
                for (ClusterLevelPartitionContext partitionContext : clusterInstanceContext.getPartitionCtxts()) {
                    List<String> members = new ArrayList<String>();

                    Iterator<MemberContext> iterator = partitionContext.getActiveMembers().listIterator();
                    while (iterator.hasNext()) {
                        MemberContext activeMember = iterator.next();
                        members.add(activeMember.getMemberId());
                    }

                    for (String memberId : members) {
                        partitionContext.moveActiveMemberToTerminationPendingMembers(
                                memberId);
                    }
                    List<String> pendingMembers = new ArrayList<String>();

                    Iterator<MemberContext> pendingIterator = partitionContext.getPendingMembers().listIterator();
                    while (pendingIterator.hasNext()) {
                        MemberContext activeMember = pendingIterator.next();
                        pendingMembers.add(activeMember.getMemberId());
                    }
                    for (String memberId : members) {
                        // pending members
                        if (log.isDebugEnabled()) {
                            log.debug("Moving pending member [member id] " + memberId + " the obsolete list");
                        }
                        partitionContext.movePendingMemberToObsoleteMembers(memberId);
                    }
                }
            }
        }
    }
}
