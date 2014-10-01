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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.common.enums.ClusterType;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 *
 */
   abstract public class VMClusterMonitor extends AbstractClusterMonitor{

	private static final Log log = LogFactory.getLog(VMClusterMonitor.class);
	// Map<NetworkpartitionId, Network Partition Context>
	protected Map<String, NetworkPartitionContext> networkPartitionCtxts;
	protected DeploymentPolicy deploymentPolicy;
	protected AutoscalePolicy autoscalePolicy;
	
    protected VMClusterMonitor(String clusterId, String serviceId, ClusterType clusterType, 
    		AutoscalerRuleEvaluator autoscalerRuleEvaluator, 
    		DeploymentPolicy deploymentPolicy, AutoscalePolicy autoscalePolicy, 
    		Map<String, NetworkPartitionContext> networkPartitionCtxts) {
    	super(clusterId, serviceId, clusterType, autoscalerRuleEvaluator);
    	this.deploymentPolicy = deploymentPolicy;
    	this.autoscalePolicy = autoscalePolicy;
    	this.networkPartitionCtxts = networkPartitionCtxts;
    }

   	public NetworkPartitionContext getNetworkPartitionCtxt(Member member) {
   		log.info("***** getNetworkPartitionCtxt " + member.getNetworkPartitionId());
		String networkPartitionId = member.getNetworkPartitionId();
    	if(networkPartitionCtxts.containsKey(networkPartitionId)) {
    		log.info("returnnig network partition context " + networkPartitionCtxts.get(networkPartitionId));
    		return networkPartitionCtxts.get(networkPartitionId);
    	}
    	log.info("returning null getNetworkPartitionCtxt");
   	    return null;
   	}
   	
    public String getPartitionOfMember(String memberId){
        for(Service service: TopologyManager.getTopology().getServices()){
            for(Cluster cluster: service.getClusters()){
                if(cluster.memberExists(memberId)){
                    return cluster.getMember(memberId).getPartitionId();
                }
            }
        }
        return null;
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
}