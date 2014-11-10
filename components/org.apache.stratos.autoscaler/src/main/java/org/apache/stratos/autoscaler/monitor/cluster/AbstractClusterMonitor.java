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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AbstractClusterContext;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorTerminateAllEvent;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
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

	private static final Log log = LogFactory.getLog(AbstractClusterMonitor.class);

    private String clusterId;
    private ClusterStatus status;
    private int monitoringIntervalMilliseconds;

    protected FactHandle minCheckFactHandle;
    protected FactHandle obsoleteCheckFactHandle;
    protected FactHandle scaleCheckFactHandle;
    private StatefulKnowledgeSession minCheckKnowledgeSession;
    private StatefulKnowledgeSession obsoleteCheckKnowledgeSession;
    private StatefulKnowledgeSession scaleCheckKnowledgeSession;
    private boolean isDestroyed;

    private AutoscalerRuleEvaluator autoscalerRuleEvaluator;
    protected boolean hasFaultyMember = false;
    protected boolean stop = false;

    protected AbstractClusterContext clusterContext;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    protected AbstractClusterMonitor(String clusterId, AutoscalerRuleEvaluator autoscalerRuleEvaluator,
                                     AbstractClusterContext abstractClusterContext) {

        super();
        this.clusterId = clusterId;
        this.autoscalerRuleEvaluator = autoscalerRuleEvaluator;
        this.clusterContext = abstractClusterContext;
        this.obsoleteCheckKnowledgeSession = autoscalerRuleEvaluator.getObsoleteCheckStatefulSession();
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getScaleCheckStatefulSession();
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getMinCheckStatefulSession();
        this.status = ClusterStatus.Created;
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

            /*} else if (status == ClusterStatus.Terminating) {
                // notify parent
                log.info("[Cluster] " + clusterId + " is not notifying the parent, " +
                        "since it is in Terminating State");
*/
            } else {
                MonitorStatusEventBuilder.handleClusterStatusEvent(this.parent, this.status, this.clusterId);
            }
        //}

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
    public void onParentEvent(MonitorStatusEvent statusEvent) {
        // send the ClusterTerminating event
//        if (statusEvent.getStatus() == GroupStatus.Terminating || statusEvent.getStatus() ==
//                ApplicationStatus.Terminating) {
//            StatusEventPublisher.sendClusterTerminatingEvent(appId, serviceId, clusterId);
//        }
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

    public abstract void terminateAllMembers();

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public String getServiceId(){
        return clusterContext.getServiceId();
    }
}
