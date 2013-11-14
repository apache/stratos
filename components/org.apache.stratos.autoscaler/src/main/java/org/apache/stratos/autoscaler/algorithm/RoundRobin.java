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
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.Partition;

/**
* Select partition in round robin manner and return
*/
public class RoundRobin implements AutoscaleAlgorithm{

    public Partition getNextScaleUpPartition(String clusterId){
    	
        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);    	            	       
        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();
    	//Find relevant policyId using topology
    	String policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getAutoscalePolicyName();
    	int noOfPartitions = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().size();
    	for(int i=0; i < noOfPartitions; i++)
    	{
    	        
    	        int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
    	        Partition currentPartition = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions()
    	                .get(currentPartitionIndex); 
    	        String currentPartitionId =  currentPartition.getId();
    	        
    	        // point to next partition
    	        currentPartitionIndex = currentPartitionIndex + 1 == noOfPartitions ? 0 : currentPartitionIndex+1;

    	        //Set next partition as current partition in Autoscaler Context
    	        AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(currentPartitionIndex);
    	        
    	        
    	        if(clusterContext.getPartitionCount(currentPartitionId) < currentPartition.getPartitionMembersMax()){
    	        	// current partition is free
    	        	AutoscalerContext.getInstance().getClusterContext(clusterId).addPartitionCount(currentPartitionId, 1);
	                return currentPartition;
	            }    	            	      
    	        
    	}
    	
    	// coming here means non of the partitions has space for another instance to be created. All partitions are full.
        return null;
    }


    public Partition getNextScaleDownPartition(String clusterId){

    	 String policyId;
         int previousPartitionIndex;
         ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
         int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();

         String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();

         //Find relevant policyId using topology
         policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getAutoscalePolicyName();


         int noOfPartitions = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().size();
         
         for(int i=0; i<noOfPartitions;i++)
         {
         	if (currentPartitionIndex == 0) {

         		previousPartitionIndex = noOfPartitions - 1;
            }else {

            	previousPartitionIndex = currentPartitionIndex - 1;
            }

             //Set next partition as current partition in Autoscaler Context
             AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(previousPartitionIndex);

             //Find next partition
             Partition previousPartition = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions()
                     .get(previousPartitionIndex);
             String previousPartitionId = previousPartition.getId();
             if(clusterContext.partitionCountExists(previousPartitionId)
                     && (clusterContext.getPartitionCount(previousPartitionId) > previousPartition.getPartitionMembersMin())){
            	 return previousPartition;
             }
         }
         
         return null;
    }


    public Partition getScaleDownPartition(String clusterId){
    	
        Partition partition = PolicyManager.getInstance().getPolicy("economyPolicy").getHAPolicy().getPartitions()
                            .get(0);

        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
        int partitionMemberCount = clusterContext.getPartitionCount(partition.getId());

        if(partitionMemberCount >= partition.getPartitionMembersMin())       {

            clusterContext.increaseMemberCountInPartition(partition.getId(), partitionMemberCount - 1);
        } else{
            partition = null;
        }
        return partition;
    }


    @Override
    public boolean scaleUpPartitionAvailable(String clusterId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean scaleDownPartitionAvailable(String clusterId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }



    public Partition getScaleUpPartition(String clusterId){
        Partition partition = PolicyManager.getInstance().getPolicy("economyPolicy").getHAPolicy().getPartitions()
                            .get(0);

        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
        int partitionMemberCount = clusterContext.getPartitionCount(partition.getId());

        if(partitionMemberCount <= partition.getPartitionMembersMax())       {

            clusterContext.increaseMemberCountInPartition(partition.getId(), partitionMemberCount + 1);
        } else{
            partition = null;
        }

        return partition;
    }
}
