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
package org.apache.stratos.autoscaler.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.grouping.topic.StatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.messaging.domain.topology.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.GroupStatus;
import org.apache.stratos.messaging.event.health.stat.AverageLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.AverageMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.AverageRequestsInFlightEvent;
import org.apache.stratos.messaging.event.health.stat.GradientOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.GradientOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.GradientOfRequestsInFlightEvent;
import org.apache.stratos.messaging.event.health.stat.MemberAverageLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.MemberAverageMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.MemberFaultEvent;
import org.apache.stratos.messaging.event.health.stat.MemberGradientOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.MemberGradientOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.MemberSecondDerivativeOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.MemberSecondDerivativeOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfRequestsInFlightEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

/*
 * Every cluster monitor, which are monitoring a cluster, should extend this class.
 */
public abstract class AbstractClusterMonitor extends Monitor implements Runnable {

    private String clusterId;
    private String serviceId;
    protected ClusterStatus status;
    private int monitoringIntervalMilliseconds;

    protected FactHandle minCheckFactHandle;
    protected FactHandle scaleCheckFactHandle;
    private StatefulKnowledgeSession minCheckKnowledgeSession;
    private StatefulKnowledgeSession scaleCheckKnowledgeSession;
    private boolean isDestroyed;

    private AutoscalerRuleEvaluator autoscalerRuleEvaluator;
    protected boolean hasFaultyMember = false;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    protected AbstractClusterMonitor(String clusterId, String serviceId,
                                     AutoscalerRuleEvaluator autoscalerRuleEvaluator) {

        super();
        this.clusterId = clusterId;
        this.serviceId = serviceId;
        this.autoscalerRuleEvaluator = autoscalerRuleEvaluator;
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getScaleCheckStatefulSession();
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getMinCheckStatefulSession();
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

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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
    public void onParentEvent(MonitorStatusEvent statusEvent) {
        // send the ClusterTerminating event
        if (statusEvent.getStatus() == GroupStatus.Terminating || statusEvent.getStatus() ==
                ApplicationStatus.Terminating) {
            StatusEventPublisher.sendClusterTerminatingEvent(appId, serviceId, clusterId);
        }
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

    public void setHasFaultyMember(boolean hasFaultyMember) {
        this.hasFaultyMember = hasFaultyMember;
    }

    public boolean isHasFaultyMember() {
        return hasFaultyMember;
    }
}
