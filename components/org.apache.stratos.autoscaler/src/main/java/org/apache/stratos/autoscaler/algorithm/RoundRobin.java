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

import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterContext;
import org.apache.stratos.autoscaler.policy.model.Partition;
import org.apache.stratos.autoscaler.policy.model.PartitionGroup;

import java.util.List;

/**
* Select partition in round robin manner and return
*/
public class RoundRobin implements AutoscaleAlgorithm{
    
    public Partition getNextScaleUpPartition(PartitionGroup partitionGrp, String clusterId){
    	
    	ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
    	int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
    	List<Partition> partitions = partitionGrp.getPartitions();
    	int noOfPartitions = partitions.size();
    	
    	for(int i=0; i < noOfPartitions; i++)
    	{
    		    Partition currentPartition = partitions.get(currentPartitionIndex);
    	        String currentPartitionId =  currentPartition.getId();
    	        
    	        // point to next partition
    	        int nextPartitionIndex = currentPartitionIndex + 1 == noOfPartitions ? 0 : currentPartitionIndex+1;
    	        AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(nextPartitionIndex);
    	        
    	        // current partition has no partitionid-instanceid info in cluster context
	        	if(!clusterContext.partitionCountExists(currentPartitionId))    	        		
	        		AutoscalerContext.getInstance().getClusterContext(clusterId).addPartitionCount(currentPartitionId, 0);
	        	
    	        if(clusterContext.getMemberCount(currentPartitionId) < currentPartition.getPartitionMembersMax()){
    	        	// current partition is free    	        	
    	        	clusterContext.increaseMemberCountInPartitionBy(currentPartitionId, 1);
	                return currentPartition;
	            }   	            	      
    	        
    	}
    	
    	// none of the partitions were free.
        return null;
    }


	@Override
	public Partition getNextScaleDownPartition(PartitionGroup partitionGrp , String clusterId) {
		
		ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
    	int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
    	List<Partition> partitions = partitionGrp.getPartitions();
    	int noOfPartitions = partitions.size();
    	
    	for(int i=0; i < noOfPartitions; i++)
    	{
    		    Partition currentPartition = partitions.get(currentPartitionIndex);
    	        String currentPartitionId =  currentPartition.getId();
    	        
    	        // point to next partition
    	        if (currentPartitionIndex == 0) {

    	        	currentPartitionIndex = noOfPartitions - 1;
                }else {

                	currentPartitionIndex = currentPartitionIndex - 1;
                }

    	        //Set next partition as current partition in Autoscaler Context
    	        clusterContext.setCurrentPartitionIndex(currentPartitionIndex);
    	            	
    	        // has more than minimum instances.
    	        if(clusterContext.getMemberCount(currentPartitionId) > currentPartition.getPartitionMembersMin()){
    	        	// current partition is free    	        	
    	        	clusterContext.decreaseMemberCountInPartitionBy(currentPartitionId, 1);
	                return currentPartition;
	            }   	            	      
    	        
    	}
    	
    	// none of the partitions were free.
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
}
