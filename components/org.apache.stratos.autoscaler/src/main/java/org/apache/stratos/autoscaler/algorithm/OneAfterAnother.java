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
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Completes partitions in the order defined in autoscaler policy, go to next if current one reached the max limit
 */
public class OneAfterAnother implements AutoscaleAlgorithm {
	
	private static final Log log = LogFactory.getLog(OneAfterAnother.class);

    public Partition getNextScaleUpPartition(PartitionGroup partitionGrp, String clusterId) {
    	
    	ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
    	int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
    	List<?> partitions = Arrays.asList(partitionGrp.getPartitions());
    	int noOfPartitions = partitions.size();    	
    	
    	for(int i=currentPartitionIndex; i< noOfPartitions; i++)
    	{
            if (partitions.get(currentPartitionIndex) instanceof Partition) {
                currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
                Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                String currentPartitionId = currentPartition.getId();

                if (clusterContext.partitionCountExists(currentPartitionId)) {
                    clusterContext.addPartitionCount(currentPartitionId, 0);
                }

                if (clusterContext.getMemberCount(currentPartitionId) < currentPartition.getPartitionMax()) {
                    // current partition is free
                    clusterContext.increaseMemberCountInPartitionBy(currentPartitionId, 1);
                    if (log.isDebugEnabled())
                        log.debug("Free space found in partition " + currentPartition.getId());

                    return currentPartition;
                } else {
                    // last partition is reached which is not free
                    if (currentPartitionIndex == noOfPartitions - 1) {
                        if (log.isDebugEnabled())
                            log.debug("Last partition also has no space");
                        return null;
                    }

                    clusterContext.setCurrentPartitionIndex(currentPartitionIndex + 1);
                }
            }
    	}
    	
    	if(log.isDebugEnabled())
    		log.debug("No free partition found at partition group" + partitionGrp);
    	
    	return null;
    }

    public Partition getNextScaleDownPartition(PartitionGroup partitionGrp, String clusterId) {

    	ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
    	int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
    	List<?> partitions = Arrays.asList(partitionGrp.getPartitions());
    	
    	for(int i = currentPartitionIndex; i >= 0; i--)
    	{
            if (partitions.get(currentPartitionIndex) instanceof Partition) {
                currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
                Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                String currentPartitionId = currentPartition.getId();

                // has more than minimum instances.
                if (clusterContext.getMemberCount(currentPartitionId) > currentPartition.getPartitionMin()) {
                    // current partition is free
                    clusterContext.decreaseMemberCountInPartitionBy(currentPartitionId, 1);
                    if (log.isDebugEnabled())
                        log.debug("A free space found for scale down in partition" +
                                  currentPartition.getId());
                    return currentPartition;
                } else {
                    if (currentPartitionIndex == 0) {
                        if (log.isDebugEnabled())
                            log.debug("First partition reached with no space to scale down");
                        return null;
                    }
                    // Set next partition as current partition in Autoscaler Context
                    currentPartitionIndex = currentPartitionIndex - 1;
                    clusterContext.setCurrentPartitionIndex(currentPartitionIndex);
                }
            }
	        
    	}
    	if(log.isDebugEnabled())
    		log.debug("No space found in this partition group " + partitionGrp.getId());
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
