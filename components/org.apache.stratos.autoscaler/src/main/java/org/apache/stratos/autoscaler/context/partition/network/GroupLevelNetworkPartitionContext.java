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
import org.apache.stratos.autoscaler.context.group.GroupInstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds runtime data of a network partition.
 */
public class GroupLevelNetworkPartitionContext extends NetworkPartitionContext implements Serializable {
    private static final Log log = LogFactory.getLog(GroupLevelNetworkPartitionContext.class);
    private int scaleDownRequestsCount = 0;
    private float averageRequestsServedPerInstance;

    private int minInstanceCount = 0, maxInstanceCount = 0;
    private int requiredInstanceCountBasedOnStats;
    private int requiredInstanceCountBasedOnDependencies;

    private String partitionAlgorithm;

    //Group level partition contexts
    private List<GroupLevelPartitionContext> partitionContexts;

    //details required for partition selection algorithms
    private int currentPartitionIndex;



    public GroupLevelNetworkPartitionContext(String id, String partitionAlgo) {
        super(id);
        this.partitionAlgorithm = partitionAlgo;
        partitionContexts = new ArrayList<GroupLevelPartitionContext>();
        requiredInstanceCountBasedOnStats = minInstanceCount;
        requiredInstanceCountBasedOnDependencies = minInstanceCount;


    }

    public GroupLevelNetworkPartitionContext(String id) {
        super(id);
        partitionContexts = new ArrayList<GroupLevelPartitionContext>();
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
        result = prime * result + ((super.getId() == null) ? 0 : super.getId().hashCode());
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
        if (super.getId() == null) {
            if (super.getId() != null) {
                return false;
            }
        } else if (!super.getId().equals(super.getId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NetworkPartitionContext [id=" + super.getId() + "partitionAlgorithm=" + partitionAlgorithm + ", minInstanceCount=" +
                minInstanceCount + ", maxInstanceCount=" + maxInstanceCount + "]";
    }

    public int getCurrentPartitionIndex() {
        return currentPartitionIndex;
    }

    public void setCurrentPartitionIndex(int currentPartitionIndex) {
        this.currentPartitionIndex = currentPartitionIndex;
    }

    public String getId() {
        return super.getId();
    }


    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
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

    public List<GroupLevelPartitionContext> getPartitionCtxts() {

        return partitionContexts;
    }

    public GroupLevelPartitionContext getPartitionCtxt(String partitionId) {

        for (GroupLevelPartitionContext partitionContext : partitionContexts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return partitionContext;
            }
        }
        return null;
    }

    public void addPartitionContext(GroupLevelPartitionContext partitionContext) {
        partitionContexts.add(partitionContext);
    }

    public int getNonTerminatedMemberCountOfPartition(String partitionId) {

        for (GroupLevelPartitionContext partitionContext : partitionContexts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return partitionContext.getNonTerminatedInstanceCount();
            }
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {

        for (GroupLevelPartitionContext partitionContext : partitionContexts) {
            if (partitionContext.getPartitionId().equals(currentPartitionId)) {
                return partitionContext.getActiveInstanceCount();
            }
        }
        return 0;
    }

    public GroupLevelPartitionContext getPartitionContextById(String partitionId) {
        for (GroupLevelPartitionContext partitionContext : partitionContexts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return partitionContext;
            }
        }
        return null;
    }




}
