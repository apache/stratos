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

import org.apache.stratos.messaging.event.Event;

import java.io.Serializable;
import java.util.*;

/**
 * This event is fired when domains are added to a tenant subscription.
 */
public class SubscriptionDomainAddedEvent extends Event implements Serializable {
    private static final long serialVersionUID = 3457484382856403382L;

    private final int tenantId;
    private final String serviceName;
    private final Set<String> clusterIds;
    private final String domainName;
    private final String applicationContext;

    public SubscriptionDomainAddedEvent(int tenantId, String serviceName, Set<String> clusterIds, String domainName,
                                        String applicationContext) {
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.clusterIds = clusterIds;
        this.domainName = domainName;
        this.applicationContext = applicationContext;
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

    public String getDomainName() {
        return domainName;
    }

    public String getApplicationContext() {
        return applicationContext;
    }
}
