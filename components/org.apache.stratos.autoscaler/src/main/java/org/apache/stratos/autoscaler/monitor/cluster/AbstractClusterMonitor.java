/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.context.cluster.AbstractClusterContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.common.Properties;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.event.topology.*;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Every cluster monitor, which are monitoring a cluster, should extend this class.
 */
public abstract class AbstractClusterMonitor extends Monitor implements Runnable {

    private static final Log log = LogFactory.getLog(AbstractClusterMonitor.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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

    protected AbstractClusterMonitor(Cluster cluster) {

        super();
        this.setCluster(new Cluster(cluster));
        this.serviceType = cluster.getServiceName();
        this.clusterId = cluster.getClusterId();
        this.monitoringStarted = new AtomicBoolean(false);
    }

    protected abstract void readConfigurations();

    public void startScheduler() {
        scheduler.scheduleAtFixedRate(this, 0, getMonitorIntervalMilliseconds(), TimeUnit.MILLISECONDS);
    }

    protected void stopScheduler() {
        scheduler.shutdownNow();
    }

    protected abstract void monitor();

    public abstract void destroy();

    //handle health events
    public abstract void handleAverageLoadAverageEvent(
            AverageLoadAverageEvent averageLoadAverageEvent);

    public abstract void handleGradientOfLoadAverageEvent(
            GradientOfLoadAverageEvent gradientOfLoadAverageEvent);

    public abstract void handleSecondDerivativeOfLoadAverageEvent(
            SecondDerivativeOfLoadAverageEvent secondDerivativeOfLoadAverageEvent);

    public abstract void handleAverageMemoryConsumptionEvent(
            AverageMemoryConsumptionEvent averageMemoryConsumptionEvent);

    public abstract void handleGradientOfMemoryConsumptionEvent(
            GradientOfMemoryConsumptionEvent gradientOfMemoryConsumptionEvent);

    public abstract void handleSecondDerivativeOfMemoryConsumptionEvent(
            SecondDerivativeOfMemoryConsumptionEvent secondDerivativeOfMemoryConsumptionEvent);

    public abstract void handleAverageRequestsInFlightEvent(
            AverageRequestsInFlightEvent averageRequestsInFlightEvent);

    public abstract void handleGradientOfRequestsInFlightEvent(
            GradientOfRequestsInFlightEvent gradientOfRequestsInFlightEvent);

    public abstract void handleSecondDerivativeOfRequestsInFlightEvent(
            SecondDerivativeOfRequestsInFlightEvent secondDerivativeOfRequestsInFlightEvent);

    public abstract void handleMemberAverageMemoryConsumptionEvent(
            MemberAverageMemoryConsumptionEvent memberAverageMemoryConsumptionEvent);

    public abstract void handleMemberGradientOfMemoryConsumptionEvent(
            MemberGradientOfMemoryConsumptionEvent memberGradientOfMemoryConsumptionEvent);

    public abstract void handleMemberSecondDerivativeOfMemoryConsumptionEvent(
            MemberSecondDerivativeOfMemoryConsumptionEvent memberSecondDerivativeOfMemoryConsumptionEvent);


    public abstract void handleMemberAverageLoadAverageEvent(
            MemberAverageLoadAverageEvent memberAverageLoadAverageEvent);

    public abstract void handleMemberGradientOfLoadAverageEvent(
            MemberGradientOfLoadAverageEvent memberGradientOfLoadAverageEvent);

    public abstract void handleMemberSecondDerivativeOfLoadAverageEvent(
            MemberSecondDerivativeOfLoadAverageEvent memberSecondDerivativeOfLoadAverageEvent);

    public abstract void handleMemberFaultEvent(MemberFaultEvent memberFaultEvent);

    //handle topology events
    public abstract void handleMemberStartedEvent(MemberStartedEvent memberStartedEvent);

    public abstract void handleMemberActivatedEvent(MemberActivatedEvent memberActivatedEvent);

    public abstract void handleMemberMaintenanceModeEvent(
            MemberMaintenanceModeEvent maintenanceModeEvent);

    public abstract void handleMemberReadyToShutdownEvent(
            MemberReadyToShutdownEvent memberReadyToShutdownEvent);

    public abstract void handleMemberTerminatedEvent(MemberTerminatedEvent memberTerminatedEvent);

    public abstract void handleClusterRemovedEvent(ClusterRemovedEvent clusterRemovedEvent);

    public abstract void handleDynamicUpdates(Properties properties) throws InvalidArgumentException;

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
        if (!(obj instanceof AbstractClusterMonitor)) {
            return false;
        }
        final AbstractClusterMonitor other = (AbstractClusterMonitor) obj;
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

    @Override
    public void onParentStatusEvent(MonitorStatusEvent statusEvent) {
        if (statusEvent.getStatus() == GroupStatus.Terminating ||
                statusEvent.getStatus() == ApplicationStatus.Terminating) {
            ClusterStatusEventPublisher.sendClusterTerminatingEvent(appId, this.getServiceId(),
                    clusterId, statusEvent.getInstanceId());
        } else if (statusEvent.getStatus() == ClusterStatus.Created ||
                statusEvent.getStatus() == GroupStatus.Created) {
            Application application = ApplicationHolder.getApplications().getApplication(this.appId);
            Group group = application.getGroupRecursively(statusEvent.getId());
            //TODO*****starting a new instance of this monitor
            //createGroupInstance(group, statusEvent.getInstanceId());
        }
        // send the ClusterTerminating event
//        if (statusEvent.getStatus() == GroupStatus.Terminating || statusEvent.getStatus() ==
//                ApplicationStatus.Terminating) {
//
//        }
    }

    @Override
    public void onChildStatusEvent(MonitorStatusEvent statusEvent) {

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

    public abstract void terminateAllMembers(String instanceId, String networkPartitionId);

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


}
