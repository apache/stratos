/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.context.partition.network;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;
import org.apache.stratos.cloud.controller.domain.xsd.Partition;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds runtime data of a network partition.
 *
 */
public class GroupLevelNetworkPartitionContext extends NetworkPartitionContext implements Serializable {
    private static final Log log = LogFactory.getLog(GroupLevelNetworkPartitionContext.class);
    private final String id;
    private int scaleDownRequestsCount = 0;
    private float averageRequestsServedPerInstance;

    private int minInstanceCount = 0, maxInstanceCount = 0;
    private int requiredInstanceCountBasedOnStats;
    private int requiredInstanceCountBasedOnDependencies;

    private final String partitionAlgorithm;

    private final Partition[] partitions;

    //details required for partition selection algorithms
    private int currentPartitionIndex;

    //partitions of this network partition
    private final Map<String, GroupLevelPartitionContext> partitionCtxts;

    public GroupLevelNetworkPartitionContext(String id, String partitionAlgo, Partition[] partitions) {
        this.id = id;
        this.partitionAlgorithm = partitionAlgo;
        if (partitions == null) {
            this.partitions = new Partition[0];
        } else {
            this.partitions = Arrays.copyOf(partitions, partitions.length);
        }
        partitionCtxts = new HashMap<String, GroupLevelPartitionContext>();
        for (Partition partition : partitions) {
            minInstanceCount += partition.getPartitionMin();
            maxInstanceCount += partition.getPartitionMax();
        }
        requiredInstanceCountBasedOnStats = minInstanceCount;
        requiredInstanceCountBasedOnDependencies = minInstanceCount;

    }

    public int getMinInstanceCount() {
        return minInstanceCount;
    }

    public void setMinInstanceCount(int minInstanceCount) {
        this.minInstanceCount = minInstanceCount;
    }

    public int getMaxInstanceCount() {
        return maxInstanceCount;
    }

    public void setMaxInstanceCount(int maxInstanceCount) {
        this.maxInstanceCount = maxInstanceCount;
    }

    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;

    }

    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GroupLevelNetworkPartitionContext)) {
            return false;
        }
        final GroupLevelNetworkPartitionContext other = (GroupLevelNetworkPartitionContext) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NetworkPartitionContext [id=" + id + "partitionAlgorithm=" + partitionAlgorithm + ", minInstanceCount=" +
                minInstanceCount + ", maxInstanceCount=" + maxInstanceCount + "]";
    }

    public int getCurrentPartitionIndex() {
        return currentPartitionIndex;
    }

    public void setCurrentPartitionIndex(int currentPartitionIndex) {
        this.currentPartitionIndex = currentPartitionIndex;
    }

    public String getId() {
        return id;
    }

    public Map<String, GroupLevelPartitionContext> getPartitionCtxts() {
        return partitionCtxts;
    }

    public GroupLevelPartitionContext getPartitionCtxt(String partitionId) {
        return partitionCtxts.get(partitionId);
    }

    public void addPartitionContext(GroupLevelPartitionContext partitionContext) {
        partitionCtxts.put(partitionContext.getPartitionId(), partitionContext);
    }

    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
    }

    public Partition[] getPartitions() {
        return partitions;
    }

    public int getNonTerminatedMemberCountOfPartition(String partitionId) {
        if (partitionCtxts.containsKey(partitionId)) {
            return getPartitionCtxt(partitionId).getNonTerminatedInstanceCount();
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {
        if (partitionCtxts.containsKey(currentPartitionId)) {
            return getPartitionCtxt(currentPartitionId).getActiveInstanceCount();
        }
        return 0;
    }

    public int getScaleDownRequestsCount() {
        return scaleDownRequestsCount;
    }

    public void resetScaleDownRequestsCount() {
        this.scaleDownRequestsCount = 0;
    }

    public void increaseScaleDownRequestsCount() {
        this.scaleDownRequestsCount += 1;
    }

    public float getRequiredInstanceCountBasedOnStats() {
        return requiredInstanceCountBasedOnStats;
    }

    public void setRequiredInstanceCountBasedOnStats(int requiredInstanceCountBasedOnStats) {
        this.requiredInstanceCountBasedOnStats = requiredInstanceCountBasedOnStats;
    }

    public int getRequiredInstanceCountBasedOnDependencies() {
        return requiredInstanceCountBasedOnDependencies;
    }

    public void setRequiredInstanceCountBasedOnDependencies(int requiredInstanceCountBasedOnDependencies) {
        this.requiredInstanceCountBasedOnDependencies = requiredInstanceCountBasedOnDependencies;
    }



}