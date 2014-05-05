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

package org.apache.stratos.messaging.event.tenant;

import java.io.Serializable;
import java.util.*;

/**
 * This event is fired when a tenant is subscribed to a service.
 */
public class TenantSubscribedEvent extends TenantEvent implements Serializable {
    private static final long serialVersionUID = -4023221432696893312L;

    private final int tenantId;
    private final String serviceName;
    private final Set<String> clusterIds;

    public TenantSubscribedEvent(int tenantId, String serviceName, Set<String> clusterIds) {
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.clusterIds = clusterIds;
    }

    public int getTenantId() {
        return tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Set<String> getClusterIds() {
        return Collections.unmodifiableSet(clusterIds);
    }
}
