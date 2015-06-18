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
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds runtime data of a network partition.
 */
public class ParentLevelNetworkPartitionContext extends NetworkPartitionContext implements Serializable {
    private static final Log log = LogFactory.getLog(ParentLevelNetworkPartitionContext.class);
    private int scaleDownRequestsCount = 0;

    private int minInstanceCount = 0, maxInstanceCount = 0;
    private int requiredInstanceCountBasedOnStats;
    private int requiredInstanceCountBasedOnDependencies;

    //active instances
    private List<InstanceContext> activeInstances;
    //pending instances
    private List<InstanceContext> pendingInstances;
    //terminating pending instances
    private List<InstanceContext> terminatingPending;
    private String partitionAlgorithm;

    //Group level partition contexts
    private List<GroupLevelPartitionContext> partitionContexts;

    //details required for partition selection algorithms
    private int currentPartitionIndex;


    public ParentLevelNetworkPartitionContext(String id, String partitionAlgo) {
        super(id);
        this.partitionAlgorithm = partitionAlgo;
        partitionContexts = new ArrayList<GroupLevelPartitionContext>();
        requiredInstanceCountBasedOnStats = minInstanceCount;
        requiredInstanceCountBasedOnDependencies = minInstanceCount;
        pendingInstances = new ArrayList<InstanceContext>();
        activeInstances = new ArrayList<InstanceContext>();
        terminatingPending = new ArrayList<InstanceContext>();

    }

    public ParentLevelNetworkPartitionContext(String id) {
        super(id);
        partitionContexts = new ArrayList<GroupLevelPartitionContext>();
        requiredInstanceCountBasedOnStats = minInstanceCount;
        requiredInstanceCountBasedOnDependencies = minInstanceCount;
        pendingInstances = new ArrayList<InstanceContext>();
        activeInstances = new ArrayList<InstanceContext>();
        terminatingPending = new ArrayList<InstanceContext>();
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
        if (!(obj instanceof ParentLevelNetworkPartitionContext)) {
            return false;
        }
        final ParentLevelNetworkPartitionContext other = (ParentLevelNetworkPartitionContext) obj;
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


    public GroupLevelPartitionContext getPartitionContextById(String partitionId) {
        for (GroupLevelPartitionContext partitionContext : partitionContexts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return partitionContext;
            }
        }
        return null;
    }

    public List<InstanceContext> getActiveInstances() {
        return activeInstances;
    }

    public void setActiveInstances(List<InstanceContext> activeInstances) {
        this.activeInstances = activeInstances;
    }

    public List<InstanceContext> getPendingInstances() {
        return pendingInstances;
    }

    public void setPendingInstances(List<InstanceContext> pendingInstances) {
        this.pendingInstances = pendingInstances;
    }

    public void addPendingInstance(InstanceContext context) {
        this.pendingInstances.add(context);
    }

    public int getPendingInstancesCount() {
        return this.pendingInstances.size();
    }

    public int getActiveInstancesCount() {
        return this.activeInstances.size();
    }

    public InstanceContext getActiveInstance(String instanceId) {
        for (InstanceContext instanceContext : activeInstances) {
            if (instanceId.equals(instanceContext.getId())) {
                return instanceContext;
            }
        }
        return null;
    }

    public InstanceContext getPendingInstance(String instanceId) {
        for (InstanceContext instanceContext : pendingInstances) {
            if (instanceId.equals(instanceContext.getId())) {
                return instanceContext;
            }
        }
        return null;
    }


    public void movePendingInstanceToActiveInstances(String instanceId) {
        if (instanceId == null) {
            return;
        }
        synchronized (pendingInstances) {
            Iterator<InstanceContext> iterator = pendingInstances.listIterator();
            while (iterator.hasNext()) {
                InstanceContext pendingInstance = iterator.next();
                if (pendingInstance == null) {
                    iterator.remove();
                    continue;
                }
                if (instanceId.equals(pendingInstance.getId())) {
                    // member is activated
                    // remove from pending list
                    iterator.remove();
                    // add to the activated list
                    this.activeInstances.add(pendingInstance);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Pending instance is removed and added to the " +
                                "activated instance list. [Instance Id] %s", instanceId));
                    }
                    break;
                }
            }
        }
    }

    public void moveActiveInstanceToTerminationPendingInstances(String instanceId) {
        if (instanceId == null) {
            return;
        }
        synchronized (activeInstances) {
            Iterator<InstanceContext> iterator = activeInstances.listIterator();
            while (iterator.hasNext()) {
                InstanceContext activeInstance = iterator.next();
                if (activeInstance == null) {
                    iterator.remove();
                    continue;
                }
                if (instanceId.equals(activeInstance.getId())) {
                    // member is activated
                    // remove from pending list
                    iterator.remove();
                    // add to the activated list
                    this.terminatingPending.add(activeInstance);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Active instance is removed and added to the " +
                                "termination pending instance list. [Instance Id] %s", instanceId));
                    }
                    break;
                }
            }
        }
    }

    public void movePendingInstanceToTerminationPendingInstances(String instanceId) {
        if (instanceId == null) {
            return;
        }
        synchronized (pendingInstances) {
            Iterator<InstanceContext> iterator = pendingInstances.listIterator();
            while (iterator.hasNext()) {
                InstanceContext pendingInstance = iterator.next();
                if (pendingInstance == null) {
                    iterator.remove();
                    continue;
                }
                if (instanceId.equals(pendingInstance.getId())) {
                    // member is activated
                    // remove from pending list
                    iterator.remove();
                    // add to the activated list
                    this.terminatingPending.add(pendingInstance);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Pending instance is removed and added to the " +
                                "termination pending instance list. [Instance Id] %s", instanceId));
                    }
                    break;
                }
            }
        }
    }

    public int getNonTerminatedInstancesCount() {
        return this.activeInstances.size() + this.pendingInstances.size();
    }

    public List<InstanceContext> getTerminatingPending() {
        return terminatingPending;
    }

    public void setTerminatingPending(List<InstanceContext> terminatingPending) {
        this.terminatingPending = terminatingPending;
    }

    public boolean removeTerminationPendingInstance(String instanceId) {
        if (id == null) {
            return false;
        }
        synchronized (pendingInstances) {
            for (Iterator<InstanceContext> iterator = pendingInstances.iterator(); iterator.hasNext(); ) {
                InstanceContext pendingInstance = iterator.next();
                if (id.equals(pendingInstance.getId())) {
                    iterator.remove();
                    return true;
                }

            }
        }
        return false;
    }


}
