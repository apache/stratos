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

            if(partitionContexts[partitionIndex].getActiveInstanceCount() < lowestInstanceCount) {

                lowestInstanceCount = partitionContexts[partitionIndex].getActiveInstanceCount();
                selectedIndex = partitionIndex;
            }
        }

        if(partitionContexts[selectedIndex].getActiveInstanceCount() < partitionContexts[selectedIndex].getMax()) {

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

            if(partitionContexts[partitionIndex].getActiveInstanceCount() > highestInstanceCount) {

                highestInstanceCount = partitionContexts[partitionIndex].getActiveInstanceCount();
                selectedIndex = partitionIndex;
            }
        }

        if(partitionContexts[selectedIndex].getActiveInstanceCount() < partitionContexts[selectedIndex].getMax()) {

            return partitionContexts[selectedIndex];
        } else {

            return null;
        }
    }


}
