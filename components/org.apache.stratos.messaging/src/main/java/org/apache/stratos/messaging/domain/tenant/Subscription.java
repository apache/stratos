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

import java.util.*;

/**
 * Tenant's service subscription.
 */
public class Subscription {
    private final String serviceName;
    private final Set<String> clusterIds;
    private final Set<String> domains;

    public Subscription(String serviceName, Set<String> clusterIds, Set<String> domains) {
        this.serviceName = serviceName;
        this.clusterIds = clusterIds;
        this.domains = (domains != null) ? domains : new HashSet<String>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public Set<String> getClusterIds() {
        return Collections.unmodifiableSet(clusterIds);
    }

    public void addDomain(String domain) {
        domains.add(domain);
    }

    public void addDomains(Set<String> domains) {
        domains.addAll(domains);
    }

    public void removeDomain(String domain) {
        domains.remove(domain);
    }

    public void removeDomains(Set<String> domains) {
        domains.removeAll(domains);
    }

    public Set<String> getDomains() {
        return Collections.unmodifiableSet(domains);
    }
}
