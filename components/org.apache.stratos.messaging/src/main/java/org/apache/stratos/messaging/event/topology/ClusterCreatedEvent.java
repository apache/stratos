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

package org.apache.stratos.messaging.event.topology;

import java.io.Serializable;
import java.util.Map;

/**
 * This event is fired by Cloud Controller when a cluster is created for a service.
 */
public class ClusterCreatedEvent extends TopologyEvent implements Serializable {
    private String serviceDomainName;
    private String clusterDomainName;
    private String tenantRange;
    private Map<String, String> autoScalingParams;

    public String getServiceDomainName() {
        return serviceDomainName;
    }

    public void setServiceDomainName(String serviceDomainName) {
        this.serviceDomainName = serviceDomainName;
    }

    public String getClusterDomainName() {
        return clusterDomainName;
    }

    public void setClusterDomainName(String clusterDomainName) {
        this.clusterDomainName = clusterDomainName;
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public Map<String, String> getAutoScalingParams() {
        return autoScalingParams;
    }

    public void setAutoScalingParams(Map<String, String> autoScalingParams) {
        this.autoScalingParams = autoScalingParams;
    }
}
