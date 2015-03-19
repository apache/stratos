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

package org.apache.stratos.messaging.event.domain.mapping;

import org.apache.stratos.messaging.event.Event;

import java.io.Serializable;

/**
 * This event is fired when a domain name is added.
 */
public class DomainMappingAddedEvent extends Event implements Serializable {

    private static final long serialVersionUID = 3457484382856403382L;

    private final String applicationId;
    private final int tenantId;
    private final String serviceName;
    private final String clusterId;
    private final String domainName;
    private final String contextPath;

    public DomainMappingAddedEvent(String applicationId, int tenantId, String serviceName, String clusterId,
                                   String domainName, String contextPath) {
        this.applicationId = applicationId;
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.domainName = domainName;
        this.contextPath = contextPath;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public int getTenantId() {
        return tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String toString() {
        return String.format("[application-id] %s [tenant-id] %d [service-name] %s [cluster-id] %s [domain-name] %s " +
                        "[context-path] %s", getApplicationId(), getTenantId(), getServiceName(), getClusterId(), getDomainName(),
                getContextPath());
    }
}
