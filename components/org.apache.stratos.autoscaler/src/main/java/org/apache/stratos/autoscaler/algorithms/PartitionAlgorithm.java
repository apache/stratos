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

package org.apache.stratos.autoscaler.algorithms;

import org.apache.stratos.autoscaler.context.partition.PartitionContext;


/**
 * This interface is should be implemented by all the algorithms that are there to select partitions context
 */
public interface PartitionAlgorithm {

    /**
     * Returns a {@link PartitionContext} to scale up from the given list
     *
     * @param partitionContexts is the array of partition contexts which will be select the partition context from
     * @return {@link PartitionContext} to scale up
     */
    public PartitionContext getNextScaleUpPartitionContext(PartitionContext[] partitionContexts);

    /**
     * Returns a {@link PartitionContext} to scale down from the given list
     *
     * @param partitionContexts is the array of partition contexts which will be select the partition context from
     * @return {@link PartitionContext} to scale down
     */
    public PartitionContext getNextScaleDownPartitionContext(PartitionContext[] partitionContexts);
}
