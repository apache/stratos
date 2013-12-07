/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.domain.tenant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Tenant definition.
 */
public class Tenant implements Serializable{
    private static final long serialVersionUID = 2154359124188618021L;

    private int tenantId;
    private String tenantDomain;
    // Map<ServiceName, Subscribed>
    private Map<String, Boolean> serviceNameMap;

    public Tenant(int tenantId, String tenantDomain) {
        this.tenantId = tenantId;
        this.tenantDomain = tenantDomain;
        this.serviceNameMap = new HashMap<String, Boolean>();
    }

    public int getTenantId() {
        return tenantId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public boolean isServiceSubscribed(String serviceName) {
        return serviceNameMap.containsKey(serviceName);
    }

    public void addServiceSubscription(String serviceName) {
        serviceNameMap.put(serviceName, true);
    }

    public void removeServiceSubscription(String serviceName) {
        serviceNameMap.remove(serviceName);
    }
}
