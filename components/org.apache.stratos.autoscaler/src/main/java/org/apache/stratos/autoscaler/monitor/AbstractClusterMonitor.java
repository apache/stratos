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

import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.common.enums.ClusterType;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

/*
 * Every cluster monitor, which are monitoring a cluster, should extend this class.
 */
public abstract class AbstractClusterMonitor implements Runnable{
	
    private String clusterId;
    private String serviceId;
    private ClusterType clusterType;
	private ClusterStatus status;
	private int monitorInterval;
	
	protected FactHandle minCheckFactHandle;
	protected FactHandle scaleCheckFactHandle;
	private StatefulKnowledgeSession minCheckKnowledgeSession;
	private StatefulKnowledgeSession scaleCheckKnowledgeSession;
	private boolean isDestroyed;
	
	private AutoscalerRuleEvaluator autoscalerRuleEvaluator;
	
	protected AbstractClusterMonitor(String clusterId, String serviceId, ClusterType clusterType, 
			AutoscalerRuleEvaluator autoscalerRuleEvaluator) {
		
		super();
		this.clusterId = clusterId;
		this.serviceId = serviceId;
		this.clusterType = clusterType;
		this.autoscalerRuleEvaluator = autoscalerRuleEvaluator;
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getScaleCheckStatefulSession();
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getMinCheckStatefulSession();
	}

	protected abstract void readConfigurations();
	protected abstract void monitor();
    public abstract void destroy();
    
	public String getClusterId() {
		return clusterId;
	}
	
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
	
	public void setStatus(ClusterStatus status) {
		this.status = status;
	}

	public ClusterType getClusterType() {
		return clusterType;
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
	
	public int getMonitorInterval() {
		return monitorInterval;
	}
	
	public void setMonitorInterval(int monitorInterval) {
		this.monitorInterval = monitorInterval;
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
}
