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
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;

import java.util.Arrays;
import java.util.List;

/**
* This class is used for selecting a {@link Partition} in round robin manner and checking availability of
 * {@link Partition}s of a {@link org.apache.stratos.autoscaler.ClusterLevelNetworkPartitionContext}
 *
*/
public class RoundRobin implements AutoscaleAlgorithm{

	private static final Log log = LogFactory.getLog(RoundRobin.class);

    public Partition getNextScaleUpPartition(NetworkPartitionContext clusterLevelNetworkPartitionContext, String clusterId){
        try{

            if (log.isDebugEnabled())
                log.debug(String.format("Searching for a partition to scale up [network partition] %s",
                        clusterLevelNetworkPartitionContext.getId()))  ;
            List<?> partitions = Arrays.asList(clusterLevelNetworkPartitionContext.getPartitions());
            int noOfPartitions = partitions.size();

            for(int i=0; i < noOfPartitions; i++)
            {
                int currentPartitionIndex = clusterLevelNetworkPartitionContext.getCurrentPartitionIndex();
                if (partitions.get(currentPartitionIndex) instanceof Partition) {
                    Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                    String currentPartitionId =  currentPartition.getId();

                    // point to next partition
                    int nextPartitionIndex = currentPartitionIndex  == noOfPartitions - 1 ? 0 : currentPartitionIndex+1;
                    clusterLevelNetworkPartitionContext.setCurrentPartitionIndex(nextPartitionIndex);
                    int nonTerminatedMemberCountOfPartition = clusterLevelNetworkPartitionContext.getNonTerminatedMemberCountOfPartition(currentPartitionId);
                    if(nonTerminatedMemberCountOfPartition < currentPartition.getPartitionMax()){
                        // current partition is free
                        if (log.isDebugEnabled())
                            log.debug(String.format("A free space found for scale up in partition %s [current] %s [max] %s",
                                    currentPartitionId, clusterLevelNetworkPartitionContext.getNonTerminatedMemberCountOfPartition(currentPartitionId),
                                                                    currentPartition.getPartitionMax()))  ;
                        return currentPartition;
                    }

                    if(log.isDebugEnabled())
                        log.debug("No free space for a new instance in partition " + currentPartition.getId());

                }
            }

            // none of the partitions were free.
            if(log.isDebugEnabled()) {
                log.debug("No free partition found at network partition " + clusterLevelNetworkPartitionContext);
    	    }
        } catch (Exception e) {
            log.error("Error occurred while searching for next scale up partition", e);
        }
    return null;
    }


	@Override
    public Partition getNextScaleDownPartition(NetworkPartitionContext clusterLevelNetworkPartitionContext, String clusterId) {
        try{
            if (log.isDebugEnabled())
                log.debug(String.format("Searching for a partition to scale up [network partition] %s",
                        clusterLevelNetworkPartitionContext.getId()))  ;
            List<?> partitions = Arrays.asList(clusterLevelNetworkPartitionContext.getPartitions());
            int noOfPartitions = partitions.size();

            for (int i = 0; i < noOfPartitions; i++) {
                int currentPartitionIndex = clusterLevelNetworkPartitionContext.getCurrentPartitionIndex();
                // point to next partition
                if (currentPartitionIndex == 0) {

                    currentPartitionIndex = noOfPartitions - 1;
                } else {

                    currentPartitionIndex = currentPartitionIndex - 1;
                }

                // Set next partition as current partition in Autoscaler Context
                clusterLevelNetworkPartitionContext.setCurrentPartitionIndex(currentPartitionIndex);

                if (partitions.get(currentPartitionIndex) instanceof Partition) {

                    Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                    String currentPartitionId = currentPartition.getId();

                    // has more than minimum instances.
                    int currentlyActiveMemberCount = clusterLevelNetworkPartitionContext.getActiveMemberCount(currentPartitionId);
                    if (currentlyActiveMemberCount > currentPartition.getPartitionMin()) {
                        // current partition is free
                        if (log.isDebugEnabled())
                            log.debug(String.format("A free space found for scale down in partition %s [current] %s [min] %s",
                                    currentPartitionId, currentlyActiveMemberCount, currentPartition.getPartitionMin()))  ;
                        return currentPartition;
                    }else {

                        if (currentPartitionIndex == 0) {
                            if (log.isDebugEnabled())
                                log.debug(String.format("Partition %s reached with no space to scale down," +
                                        "[current] %s [min] %s", currentPartitionId, currentlyActiveMemberCount,
                                        currentPartition.getPartitionMin()));
                            return null;
                        }
                    }
                }
            }

            if (log.isDebugEnabled())
                log.debug("No partition found for scale down at network partition " +
                          clusterLevelNetworkPartitionContext.getId());
            // none of the partitions were free.

        } catch (Exception e) {
            log.error("Error occurred while searching for next scale down partition", e);
        }
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
