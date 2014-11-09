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

//import org.apache.stratos.autoscaler.partition.networkPartitionContext;

/**
 *
 */

/**
 * This class is used for selecting a {@link Partition} one after another and checking availability of
 * partitions of a {@link NetworkPartitionContext}
 * One after another means it completes partitions in the order defined in
 * {@link org.apache.stratos.autoscaler.policy.model.DeploymentPolicy}, and go to next  if current one
 * reached the max limit
 */
public class OneAfterAnother implements AutoscaleAlgorithm {

    private static final Log log = LogFactory.getLog(OneAfterAnother.class);

    public Partition getNextScaleUpPartition(NetworkPartitionContext networkPartitionContext, String clusterId) {

        try {
            if (log.isDebugEnabled())
                log.debug(String.format("Searching for a partition to up [network partition] %s",
                        networkPartitionContext.getId()))  ;
            int currentPartitionIndex = networkPartitionContext.getCurrentPartitionIndex();
            List<?> partitions = Arrays.asList(networkPartitionContext.getPartitions());
            int noOfPartitions = partitions.size();


            for (int i = currentPartitionIndex; i < noOfPartitions; i++) {
                if (partitions.get(currentPartitionIndex) instanceof Partition) {
                    currentPartitionIndex = networkPartitionContext.getCurrentPartitionIndex();
                    Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                    String currentPartitionId = currentPartition.getId();
                    int nonTerminatedMemberCountOfPartition = networkPartitionContext.getNonTerminatedMemberCountOfPartition(currentPartitionId);
                    if (nonTerminatedMemberCountOfPartition < currentPartition.getPartitionMax()) {
                        // current partition is free
                        if (log.isDebugEnabled())
                            log.debug(String.format("A free space found for scale up in partition %s [current] %s [max] %s",
                                    currentPartitionId, nonTerminatedMemberCountOfPartition, currentPartition.getPartitionMax()))  ;
                        return currentPartition;
                    } else {
                        // last partition is reached which is not free
                        if (currentPartitionIndex == noOfPartitions - 1) {
                            if (log.isDebugEnabled())
                                log.debug("Last partition also has no space");
                            return null;
                        }

                        networkPartitionContext.setCurrentPartitionIndex(currentPartitionIndex + 1);
                    }
                }
            }

            if (log.isDebugEnabled())
                log.debug(String.format("No free partition found at network partition %s" , networkPartitionContext));
        } catch (Exception e) {
            log.error("Error occurred while searching for next scale up partition", e);
        }
        return null;
    }

    public Partition getNextScaleDownPartition(NetworkPartitionContext networkPartitionContext, String clusterId) {

        try {

            if (log.isDebugEnabled())
                log.debug(String.format("Searching for a partition to scale down [network partition] %s",
                        networkPartitionContext.getId()))  ;
            int currentPartitionIndex = networkPartitionContext.getCurrentPartitionIndex();
            List<?> partitions = Arrays.asList(networkPartitionContext.getPartitions());

            for (int i = currentPartitionIndex; i >= 0; i--) {
                if (partitions.get(currentPartitionIndex) instanceof Partition) {
                    currentPartitionIndex = networkPartitionContext.getCurrentPartitionIndex();
                    Partition currentPartition = (Partition) partitions.get(currentPartitionIndex);
                    String currentPartitionId = currentPartition.getId();

                    // has more than minimum instances.
                    int currentlyActiveMemberCount = networkPartitionContext.getActiveMemberCount(currentPartitionId);
                    if ( currentlyActiveMemberCount > currentPartition.getPartitionMin()) {
                        // current partition is free
                        if (log.isDebugEnabled())
                            log.debug(String.format("A free space found for scale down in partition %s [current] %s [min] %s",
                                    currentPartitionId, currentlyActiveMemberCount, currentPartition.getPartitionMin()))  ;
                        return currentPartition;
                    } else {
                        if (currentPartitionIndex == 0) {
                            if (log.isDebugEnabled())
                                log.debug(String.format("Partition %s reached with no space to scale down," +
                                        "[active] %s [min] %s", currentPartitionId, currentlyActiveMemberCount,
                                        currentPartition.getPartitionMin()));
                            return null;
                        }
                        // Set next partition as current partition in Autoscaler Context
                        currentPartitionIndex = currentPartitionIndex - 1;
                        networkPartitionContext.setCurrentPartitionIndex(currentPartitionIndex);
                    }
                }

            }
            if (log.isDebugEnabled())
                log.debug("No space found in this network partition " + networkPartitionContext.getId());
        } catch (Exception e) {
            log.error("Error occurred while searching for scale down partition", e);
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
