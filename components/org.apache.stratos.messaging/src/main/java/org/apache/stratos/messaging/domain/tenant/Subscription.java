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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Tenant's service subscription.
 */
public class Subscription {
    private static final Log log = LogFactory.getLog(Subscription.class);

    private final String serviceName;
    private final Set<String> clusterIds;
    private final Map<String, SubscriptionDomain> subscriptionDomainMap;

    public Subscription(String serviceName, Set<String> clusterIds) {
        this.serviceName = serviceName;
        this.clusterIds = clusterIds;
        this.subscriptionDomainMap = new HashMap<String, SubscriptionDomain>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public Set<String> getClusterIds() {
        return Collections.unmodifiableSet(clusterIds);
    }

    public void addSubscriptionDomain(SubscriptionDomain subscriptionDomain) {
        subscriptionDomainMap.put(subscriptionDomain.getDomainName(), subscriptionDomain);
    }

    public void addSubscriptionDomain(String domainName, String applicationAlias) {
        addSubscriptionDomain(new SubscriptionDomain(domainName, applicationAlias));
    }

    public void removeSubscriptionDomain(String domainName) {
        if(subscriptionDomainExists(domainName)) {
            subscriptionDomainMap.remove(domainName);
        }
        else {
            if(log.isWarnEnabled()) {
                log.warn("Subscription domain does not exist: " + domainName);
            }
        }
    }

    public boolean subscriptionDomainExists(String domainName) {
        return subscriptionDomainMap.containsKey(domainName);
    }

    public Collection<SubscriptionDomain> getSubscriptionDomains() {
        return Collections.unmodifiableCollection(subscriptionDomainMap.values());
    }
}
