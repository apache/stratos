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

import java.util.*;

/**
 * This will keep track of network partition level information.
 */
public abstract class NetworkPartitionContext {
    private static final Log log = LogFactory.getLog(NetworkPartitionContext.class);
    //id of the network partition context
    private final String id;
    //group instances kept inside a partition
    private Map<String, InstanceContext> instanceIdToInstanceContextMap;
    //active instances
    private List<InstanceContext> activeInstances;
    //pending instances
    private List<InstanceContext> pendingInstances;
    //terminating pending instances
    private List<InstanceContext> terminatingPending;
    private int pendingMembersFailureCount = 0;

    protected NetworkPartitionContext(String id) {
        this.id = id;
        instanceIdToInstanceContextMap = new HashMap<String, InstanceContext>();
        pendingInstances = new ArrayList<InstanceContext>();
        activeInstances = new ArrayList<InstanceContext>();
        terminatingPending = new ArrayList<InstanceContext>();

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
                    pendingMembersFailureCount = 0;
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
