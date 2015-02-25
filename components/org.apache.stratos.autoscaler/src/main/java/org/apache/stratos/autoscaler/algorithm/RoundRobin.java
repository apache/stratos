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
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.partition.PartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.cloud.controller.stub.domain.Partition;

import java.util.Arrays;
import java.util.List;

/**
* This class is used for selecting a {@link PartitionContext} in round robin manner and checking availability of
 * {@link PartitionContext}s according to the partitions defined
 * in {@link org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy}
 *
*/
public class RoundRobin implements AutoscaleAlgorithm{

	private static final Log log = LogFactory.getLog(RoundRobin.class);

    @Override
    public PartitionContext getNextScaleUpPartitionContext(PartitionContext[] partitionContexts) {

        int selectedIndex = 0;
        int lowestInstanceCount = 0;

        for(int partitionIndex = 0; partitionIndex < partitionContexts.length - 1; partitionIndex++) {

            if(partitionContexts[partitionIndex].getNonTerminatedMemberCount() < lowestInstanceCount) {

                lowestInstanceCount = partitionContexts[partitionIndex].getNonTerminatedMemberCount();
                selectedIndex = partitionIndex;
            }
        }

        if(partitionContexts[selectedIndex].getNonTerminatedMemberCount() < partitionContexts[selectedIndex].getMax()) {

            if(log.isDebugEnabled()){
                log.debug(String.format("[round-robin algorithm] [scale-up] [partition] %s has space to create members. " +
                        "[non terminated count] %s [max] %s"
                        , partitionContexts[selectedIndex].getPartitionId(),
                        partitionContexts[selectedIndex].getNonTerminatedMemberCount(),
                        partitionContexts[selectedIndex].getMax()));
            }
            return partitionContexts[selectedIndex];
        } else {

            return null;
        }
    }

    @Override
    public PartitionContext getNextScaleDownPartitionContext(PartitionContext[] partitionContexts) {

        int selectedIndex = 0;
        int highestInstanceCount = 0;

        for(int partitionIndex = partitionContexts.length - 1; partitionIndex >= 0; partitionIndex--) {

            if(partitionContexts[partitionIndex].getNonTerminatedMemberCount() > highestInstanceCount) {

                highestInstanceCount = partitionContexts[partitionIndex].getNonTerminatedMemberCount();
                selectedIndex = partitionIndex;
            }
        }

        if(partitionContexts[selectedIndex].getNonTerminatedMemberCount() < partitionContexts[selectedIndex].getMax()) {

            if(log.isDebugEnabled()){
                log.debug(String.format("[round-robin algorithm] [scale-down] [partition] %s has has members that" +
                        " can be removed.[non terminated count] %s ", partitionContexts[selectedIndex].getPartitionId(),
                        partitionContexts[selectedIndex].getNonTerminatedMemberCount()));
            }
            return partitionContexts[selectedIndex];
        } else {

            return null;
        }
    }


}
