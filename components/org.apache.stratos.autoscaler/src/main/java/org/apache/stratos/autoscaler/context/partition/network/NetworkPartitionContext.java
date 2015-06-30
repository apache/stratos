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
import java.util.concurrent.ConcurrentHashMap;

/**
 * This will keep track of network partition level information.
 */
public abstract class NetworkPartitionContext {
    private static final Log log = LogFactory.getLog(NetworkPartitionContext.class);
    //id of the network partition context
    protected final String id;
    //group instances kept inside a partition
    protected Map<String, InstanceContext> instanceIdToInstanceContextMap;
    private int pendingMembersFailureCount = 0;

    protected NetworkPartitionContext(String id) {
        this.id = id;
        instanceIdToInstanceContextMap = new ConcurrentHashMap<String, InstanceContext>();

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
}
