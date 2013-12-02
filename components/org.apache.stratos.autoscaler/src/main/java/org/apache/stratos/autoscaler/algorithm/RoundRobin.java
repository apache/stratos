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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterContext;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

import edu.emory.mathcs.backport.java.util.Arrays;

import java.util.List;

/**
* This class is used for selecting a {@link Partition} in round robin manner and checking availability of
 * {@link Partition}s of a {@link PartitionGroup}
 *
*/
public class RoundRobin implements AutoscaleAlgorithm{
	
	private static final Log log = LogFactory.getLog(RoundRobin.class);
    
    public Partition getNextScaleUpPartition(PartitionGroup partitionGroup, String clusterId){


        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
    	List<?> partitions = Arrays.asList(partitionGroup.getPartitions());
    	int noOfPartitions = partitions.size();

    	for(int i=0; i < noOfPartitions; i++)
    	{
    	    int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
    	    if (partitions.get(currentPartitionIndex) instanceof Partition) {
    		    Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
    	        String currentPartitionId =  currentPartition.getId();
    	        
    	        // point to next partition
    	        int nextPartitionIndex = currentPartitionIndex  == noOfPartitions - 1 ? 0 : currentPartitionIndex+1;
    	        clusterContext.setCurrentPartitionIndex(nextPartitionIndex);
    	        
    	        // current partition has no partitionid-instanceid info in cluster context
	        	if(!clusterContext.partitionCountExists(currentPartitionId))    	        		
	        		AutoscalerContext.getInstance().getClusterContext(clusterId).addPartitionCount(currentPartitionId, 0);
	        	
    	        if(clusterContext.getMemberCount(currentPartitionId) < currentPartition.getPartitionMax()){
    	        	// current partition is free    	        	
    	        	clusterContext.increaseMemberCountInPartitionBy(currentPartitionId, 1);
    	        	if(log.isDebugEnabled())
    	        		log.debug("Free space found in partition " + currentPartition.getId());
	                return currentPartition;
	            }

    	        if(log.isDebugEnabled())
    	        	log.debug("No free space for a new instance in partition " + currentPartition.getId());

    	    }
    	}
    	
    	// none of the partitions were free.
    	if(log.isDebugEnabled()) {
    		log.debug("No free partition found at partition group " + partitionGroup);
    	}
        return null;
    }


	@Override
    public Partition getNextScaleDownPartition(PartitionGroup partitionGroup, String clusterId) {

        ClusterContext clusterContext =
                                        AutoscalerContext.getInstance()
                                                         .getClusterContext(clusterId);

        List<?> partitions = Arrays.asList(partitionGroup.getPartitions());
        int noOfPartitions = partitions.size();

        for (int i = 0; i < noOfPartitions; i++) {
            int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
            // point to next partition
            if (currentPartitionIndex == 0) {

                currentPartitionIndex = noOfPartitions - 1;
            } else {

                currentPartitionIndex = currentPartitionIndex - 1;
            }

            // Set next partition as current partition in Autoscaler Context
            clusterContext.setCurrentPartitionIndex(currentPartitionIndex);

            if (partitions.get(currentPartitionIndex) instanceof Partition) {

                Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                String currentPartitionId = currentPartition.getId();

                if (!clusterContext.partitionCountExists(currentPartitionId))
                    AutoscalerContext.getInstance().getClusterContext(clusterId)
                                     .addPartitionCount(currentPartitionId, 0);
                // has more than minimum instances.
                if (clusterContext.getMemberCount(currentPartitionId) > currentPartition.getPartitionMin()) {
                    // current partition is free
                    clusterContext.decreaseMemberCountInPartitionBy(currentPartitionId, 1);
                    if (log.isDebugEnabled()) {
                        log.debug("Returning partition for scaling down " +
                                  currentPartition.getId());
                    }
                    return currentPartition;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Found no members to scale down at partition" +
                              currentPartition.getId());
                }
            }
        }

        if (log.isDebugEnabled())
            log.debug("No partition found for scale down at partition group " +
                      partitionGroup.getId());
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
