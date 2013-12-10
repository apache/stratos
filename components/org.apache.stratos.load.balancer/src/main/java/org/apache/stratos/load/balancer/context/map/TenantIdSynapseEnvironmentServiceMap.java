/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.load.balancer.context.map;

import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant id, synapse environment service map for accessing synapse environment services using
 * tenant id:
 * Map[TenantId, SynapseEnvironmentService]
 */
public class TenantIdSynapseEnvironmentServiceMap {

    private ConcurrentHashMap<Integer, SynapseEnvironmentService> concurrentHashMap;

    public TenantIdSynapseEnvironmentServiceMap() {
        concurrentHashMap = new ConcurrentHashMap<Integer, SynapseEnvironmentService>();
    }

    public SynapseEnvironmentService getSynapseEnvironmentService(int tenantId) {
        return concurrentHashMap.get(tenantId);
    }

    public boolean containsSynapseEnvironmentService(int tenantId) {
        return concurrentHashMap.containsKey(tenantId);
    }

    public void addSynapseEnvironmentService(int tenantId, SynapseEnvironmentService synapseEnvironmentService) {
        concurrentHashMap.put(tenantId, synapseEnvironmentService);
    }

    public void removeSynapseEnvironmentService(int tenantId) {
        concurrentHashMap.remove(tenantId);
    }

    public Map<Integer, SynapseEnvironmentService> getTenantIdSynapseEnvironmentServiceMap() {
        return concurrentHashMap;
    }

    public void clear() {
        concurrentHashMap.clear();
    }
}
