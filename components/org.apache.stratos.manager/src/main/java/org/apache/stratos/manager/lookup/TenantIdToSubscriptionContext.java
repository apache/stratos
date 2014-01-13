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

package org.apache.stratos.manager.lookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TenantIdToSubscriptionContext {

    private static final Log log = LogFactory.getLog(TenantIdToSubscriptionContext.class);

    // Map: tenant Id -> SubscriptionContext
    private Map<Integer, SubscriptionContext> tenantIdToSubscriptionContext;

    public TenantIdToSubscriptionContext() {
        tenantIdToSubscriptionContext = new HashMap<Integer, SubscriptionContext>();
    }

    public Collection<SubscriptionContext> getSubscriptionContexts () {

        return tenantIdToSubscriptionContext.values();
    }

    public SubscriptionContext getSubscriptionContext (int tenantId) {

        return tenantIdToSubscriptionContext.get(tenantId);
    }

    public void addSubscriptionContext (int tenantId, SubscriptionContext subscriptionContext) {

        tenantIdToSubscriptionContext.put(tenantId, subscriptionContext);
    }

    public void removeSubscriptionContext (int tenantId, String type, String subscriptionAlias) {

        if (tenantIdToSubscriptionContext.containsKey(tenantId)) {
            SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.get(tenantId);
            subscriptionContext.deleteSubscription(type, subscriptionAlias);

            // delete the SubscriptionContext instance for the tenant if it carries no information
            if (subscriptionContext.getSubscriptionsOfType(type) == null && subscriptionContext.getSubscriptionForAlias(subscriptionAlias) == null) {
                tenantIdToSubscriptionContext.remove(tenantId);
                if (log.isDebugEnabled()) {
                    log.debug("Deleted the subscriptionContext instance for tenant " + tenantId);
                }
            }
        }
    }
}
