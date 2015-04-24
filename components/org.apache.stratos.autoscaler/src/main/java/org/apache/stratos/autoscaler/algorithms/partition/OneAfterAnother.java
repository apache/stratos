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

package org.apache.stratos.autoscaler.algorithms.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.PartitionAlgorithm;
import org.apache.stratos.autoscaler.context.partition.PartitionContext;

/**
 * This class is used for selecting a {@link PartitionContext} using one after another algorithm
 * One after another means it completes partitions in the order defined in
 * {@link org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy}, and go to next if current one
 * reached the max limit
 */
public class OneAfterAnother implements PartitionAlgorithm {

    private static final Log log = LogFactory.getLog(OneAfterAnother.class);

    @Override
    public PartitionContext getNextScaleUpPartitionContext(PartitionContext[] partitionContexts) {

        for (PartitionContext partitionContext : partitionContexts) {

            if (partitionContext.getNonTerminatedMemberCount() < partitionContext.getMax() && !partitionContext.isObsoletePartition()) {

                if (log.isDebugEnabled()) {
                    log.debug(String.format("[one-after-another algorithm] [scale-up] [partition] %s has space to create " +
                                    "members. [non terminated count] %s [max] %s", partitionContext.getPartitionId(),
                            partitionContext.getNonTerminatedMemberCount(), partitionContext.getMax()));
                }
                return partitionContext;
            }
        }
        return null;
    }

    @Override
    public PartitionContext getNextScaleDownPartitionContext(PartitionContext[] partitionContexts) {

        for (int partitionIndex = partitionContexts.length - 1; partitionIndex >= 0; partitionIndex--) {

            if (partitionContexts[partitionIndex].getNonTerminatedMemberCount() > 0
                    && !partitionContexts[partitionIndex].isObsoletePartition()) {

                if (log.isDebugEnabled()) {
                    log.debug(String.format("[one-after-another algorithm] [scale-down] [partition] %s has members that" +
                                    " can be removed. [non terminated count] %s",
                            partitionContexts[partitionIndex].getPartitionId(),
                            partitionContexts[partitionIndex].getNonTerminatedMemberCount()));
                }
                return partitionContexts[partitionIndex];
            }
        }
        return null;
    }


}
