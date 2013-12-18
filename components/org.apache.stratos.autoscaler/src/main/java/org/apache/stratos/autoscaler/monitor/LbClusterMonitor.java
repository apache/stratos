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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 *
 */
public class LbClusterMonitor extends AbstractMonitor{

    private static final Log log = LogFactory.getLog(LbClusterMonitor.class);
    private String clusterId;
    private String serviceId;

    private Map<String, NetworkPartitionContext> networkPartitionCtxts;

    private StatefulKnowledgeSession minCheckKnowledgeSession;
    private StatefulKnowledgeSession scaleCheckKnowledgeSession;
    private boolean isDestroyed;

    private DeploymentPolicy deploymentPolicy;
    private AutoscalePolicy autoscalePolicy;

        // Key- MemberId Value- partitionId
    private Map<String, String> memberPartitionMap;

    private FactHandle minCheckFactHandle;
    private FactHandle scaleCheckFactHandle;

    private AutoscalerRuleEvaluator autoscalerRuleEvaluator;

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

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
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

    @Override
    public void run() {

        while (!isDestroyed()) {
            if (log.isDebugEnabled()) {
                log.debug("Cluster monitor is running.. "+this.toString());
            }
            try {
                monitor();
            } catch (Exception e) {
                log.error("Cluster monitor: Monitor failed. "+this.toString(), e);
            }
            try {
                // TODO make this configurable
                Thread.sleep(30000);
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

    
    public void destroy() {
        minCheckKnowledgeSession.dispose();
        scaleCheckKnowledgeSession.dispose();
        setDestroyed(true);
        if(log.isDebugEnabled()) {
            log.debug("Cluster Monitor Drools session has been disposed. "+this.toString());
        }
    }

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

    public String getPartitonOfMember(String memberId){
   		return this.memberPartitionMap.get(memberId);
   	}

   	public boolean memberExist(String memberId){
   		return this.memberPartitionMap.containsKey(memberId);
   	}

    @Override
    public String toString() {
        return "LbClusterMonitor [clusterId=" + clusterId + ", serviceId=" + serviceId + "]";
    }
    
    @Override
	public NetworkPartitionContext findNetworkPartition(String memberId) {
		 for(Service service: TopologyManager.getTopology().getServices()){
	            for(Cluster cluster: service.getClusters()){
	                NetworkPartitionContext netCtx = AutoscalerContext.getInstance().getLBMonitor(cluster.getClusterId())
	                        .getNetworkPartitionCtxt(cluster.getMember(memberId).getNetworkPartitionId());
	                if(null != netCtx)
	                	return netCtx;
	            }
	      }
	      return null;
	}
}
