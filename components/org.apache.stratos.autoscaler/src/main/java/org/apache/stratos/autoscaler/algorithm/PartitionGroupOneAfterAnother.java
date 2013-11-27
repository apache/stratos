/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.stratos.autoscaler.algorithm;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterContext;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.messaging.domain.policy.Partition;
import org.apache.stratos.messaging.domain.policy.PartitionGroup;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;

/**
 * Completes partitions in the order defined in autoscaler policy, go to next if current one reached the max limit
 */
public class PartitionGroupOneAfterAnother implements AutoscaleAlgorithm {

	private static final Log log = LogFactory.getLog(PartitionGroupOneAfterAnother.class);
	
    public Partition getNextScaleUpPartition(String clusterId) {
    	
    	ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);    	            	       
        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();
        
    	//Find relevant policyId using topology
    	String policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getDeploymentPolicyName();
    	    	
    	List<PartitionGroup> partitionGroups = PolicyManager.getInstance().getDeploymentPolicy(policyId).getPartitionGroups();  	    	    
    	int currentPartitionGroupIndex = clusterContext.getCurrentPartitionGroupIndex();  
    	
    	for(int i= currentPartitionGroupIndex; i< partitionGroups.size(); i++)
    	{
    		currentPartitionGroupIndex = clusterContext.getCurrentPartitionGroupIndex();   		
	        PartitionGroup currentPartitionGroup = partitionGroups.get(currentPartitionGroupIndex);
	        String alogirthm = currentPartitionGroup.getPartitionAlgo();
	        
	        if(log.isDebugEnabled())
	        	log.debug("Trying current partition group " + currentPartitionGroup.getId());
	        // search withing the partition group
	        Partition partition = AutoscalerRuleEvaluator.getInstance().getAutoscaleAlgorithm(alogirthm).getNextScaleUpPartition(currentPartitionGroup, clusterId);
	        
	        if(partition != null){
	        	if(log.isDebugEnabled())
	        		log.debug("No partition found in partition group" +currentPartitionGroup.getId());
	        	return partition;
	        }else{
	        	clusterContext.setCurrentPartitionIndex(0);
	        	//last partition group has reached
	        	if(currentPartitionGroupIndex == partitionGroups.size() - 1){
	        		if(log.isDebugEnabled())
	        			log.debug("First partition group has reached wihtout space ");
	        		return null;
	        	}
	        	// current partition group is filled	        	
	        	clusterContext.setCurrentPartitionGroupIndex(currentPartitionGroupIndex + 1);	        	
	        }
	       
    	}
    	
    	return null;
    }

    public Partition getNextScaleDownPartition(String clusterId) {
    	ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);    	            	       
        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();
        
    	//Find relevant policyId using topology
    	String policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getDeploymentPolicyName();
    	int currentPartitionGroupIndex = clusterContext.getCurrentPartitionGroupIndex();
    	
    	List<PartitionGroup> partitionGroups = PolicyManager.getInstance().getDeploymentPolicy(policyId).getPartitionGroups();  	    	    
    	
    	for(int i = currentPartitionGroupIndex; i >= 0; i--)
    	{
    		currentPartitionGroupIndex = clusterContext.getCurrentPartitionGroupIndex();    		
	        PartitionGroup currentPartitionGroup = partitionGroups.get(currentPartitionGroupIndex);
	        String alogirthm = currentPartitionGroup.getPartitionAlgo();
	        if(log.isDebugEnabled())
	        	log.debug("Trying scale down in partition group " + currentPartitionGroup.getId());
	        // search within the partition group
	        Partition partition = AutoscalerRuleEvaluator.getInstance().getAutoscaleAlgorithm(alogirthm).getNextScaleDownPartition(currentPartitionGroup, clusterId);
	        
	        if(partition != null){
	        	if(log.isDebugEnabled())
	        		log.debug("No free partition in partition group" + currentPartitionGroup.getId());
	        	return partition;
	        }else{
	        	clusterContext.setCurrentPartitionIndex(0);
	        	//first partition group has reached. None of the partitions group has less than minimum instance count.  
	        	if(currentPartitionGroupIndex == 0)
	        		return null;
	        	
	        	// current partition group has no extra instances      		        	
	        	clusterContext.setCurrentPartitionGroupIndex(currentPartitionGroupIndex - 1);	        	
	        }
	       
    	}
    	// none of the partitions groups are free.
    	return null;
    }
    
    @Override
    public boolean scaleUpPartitionAvailable(String clusterId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean scaleDownPartitionAvailable(String clusterId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

	@Override
	public Partition getNextScaleUpPartition(PartitionGroup partition,
			String clusterId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Partition getNextScaleDownPartition(PartitionGroup partition,
			String clusterId) {
		// TODO Auto-generated method stub
		return null;
	}
}
