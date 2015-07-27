/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.context.partition.network;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This will keep track of network partition level information.
 */
public class NetworkPartitionContext {
    private static final Log log = LogFactory.getLog(NetworkPartitionContext.class);
    //id of the network partition context
    private String id;
    //group instances kept inside a partition
    private Map<String, InstanceContext> instanceIdToInstanceContextMap;
    //network partition level min and max
    private int minInstanceCount = 0, maxInstanceCount = 0;
    //active instances
    private List<InstanceContext> activeInstances;
    //pending instances
    private List<InstanceContext> pendingInstances;
    //terminating pending instances
    private List<InstanceContext> terminatingPending;
    //partition algorithm
    private String partitionAlgorithm;
    //partition min
    private int min;

    //Group level partition contexts
    private List<GroupLevelPartitionContext> partitionContexts;

    public NetworkPartitionContext(String id, String partitionAlgo) {
        this.id = id;
        this.partitionAlgorithm = partitionAlgo;
        partitionContexts = new ArrayList<GroupLevelPartitionContext>();
        pendingInstances = new ArrayList<InstanceContext>();
        activeInstances = new ArrayList<InstanceContext>();
        terminatingPending = new ArrayList<InstanceContext>();

    }

    public NetworkPartitionContext(String id) {
        this.id = id;
        partitionContexts = new ArrayList<GroupLevelPartitionContext>();
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
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;

    }

    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NetworkPartitionContext [id=" + id + "partitionAlgorithm=" + partitionAlgorithm + ", minInstanceCount=" +
                minInstanceCount + ", maxInstanceCount=" + maxInstanceCount + "]";
    }


    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
    }

    public List<InstanceContext> getInstanceIdToInstanceContextMap(String parentInstanceId) {
        List<InstanceContext> instanceContexts = new ArrayList<InstanceContext>();
        for(InstanceContext instanceContext : instanceIdToInstanceContextMap.values()) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }
        return instanceContexts;
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

    public List<InstanceContext> getActiveInstances(String parentInstanceId) {
        List<InstanceContext> instanceContexts = new ArrayList<InstanceContext>();
        for(InstanceContext instanceContext : activeInstances) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }
        return instanceContexts;
    }

    public List<InstanceContext> getPendingInstances(String parentInstanceId) {
        List<InstanceContext> instanceContexts = new ArrayList<InstanceContext>();
        for(InstanceContext instanceContext : pendingInstances) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }
        return instanceContexts;
    }

    public void addPendingInstance(InstanceContext context) {
        this.pendingInstances.add(context);
    }

    public int getPendingInstancesCount() {
        return this.pendingInstances.size();
    }

    public int getPendingInstancesCount(String parentInstanceId) {
        List<InstanceContext> instanceContexts = new ArrayList<InstanceContext>();
        for(InstanceContext instanceContext : pendingInstances) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }
        return instanceContexts.size();
    }


    public int getActiveInstancesCount() {
        return this.activeInstances.size();
    }

    public int getActiveInstancesCount(String parentInstanceId) {
        List<InstanceContext> instanceContexts = new ArrayList<InstanceContext>();
        for(InstanceContext instanceContext : activeInstances) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }
        return instanceContexts.size();
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

    public int getNonTerminatedInstancesCount(String parentInstanceId) {
        List<InstanceContext> instanceContexts = new ArrayList<InstanceContext>();

        for(InstanceContext instanceContext : activeInstances) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }

        for(InstanceContext instanceContext : pendingInstances) {
            if(instanceContext.getParentInstanceId().equals(parentInstanceId)) {
                instanceContexts.add(instanceContext);
            }
        }
        return instanceContexts.size();
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

    public String getId() {
        return id;
    }

    public Map<String, InstanceContext> getInstanceIdToInstanceContextMap() {
        return instanceIdToInstanceContextMap;
    }

    public void setInstanceIdToInstanceContextMap(Map<String, InstanceContext> instanceIdToInstanceContextMap) {
        this.instanceIdToInstanceContextMap = instanceIdToInstanceContextMap;
    }

    public void addInstanceContext(InstanceContext context) {
        this.instanceIdToInstanceContextMap.put(context.getId(), context);

    }

    public InstanceContext getInstanceContext(String instanceId) {
        return this.instanceIdToInstanceContextMap.get(instanceId);
    }

    public void removeInstanceContext(String instanceId) {
        this.instanceIdToInstanceContextMap.remove(instanceId);
    }

    public boolean containsInstanceContext(String instanceId) {
        return this.instanceIdToInstanceContextMap.containsKey(instanceId);
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }
}
