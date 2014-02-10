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
import org.apache.stratos.manager.subscription.CartridgeSubscription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LookupDataHolder implements Serializable {

    private static final Log log = LogFactory.getLog(LookupDataHolder.class);

    private ClusterIdToSubscription clusterIdToSubscription;
    private TenantIdToSubscriptionContext tenantIdToSubscriptionContext;
    private static volatile  LookupDataHolder lookupDataHolder;

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private LookupDataHolder () {
        clusterIdToSubscription = new ClusterIdToSubscription();
        tenantIdToSubscriptionContext = new TenantIdToSubscriptionContext();
    }

    public static LookupDataHolder getInstance () {

        if (lookupDataHolder == null) {
            synchronized (LookupDataHolder.class) {
                if (lookupDataHolder == null) {
                    lookupDataHolder = new LookupDataHolder();
                }
            }
        }
        return lookupDataHolder;
    }

    public void putSubscription (CartridgeSubscription cartridgeSubscription) {

        // add or update
        clusterIdToSubscription.addSubscription(cartridgeSubscription);

        // check if an existing SubscriptionContext is available
        SubscriptionContext existingSubscriptionCtx = tenantIdToSubscriptionContext.getSubscriptionContext(cartridgeSubscription.getSubscriber().getTenantId());
        if(existingSubscriptionCtx != null) {
            if (log.isDebugEnabled()) {
                log.debug("Existing SubscriptionContext found for tenant " + cartridgeSubscription.getSubscriber().getTenantId());
            }
            existingSubscriptionCtx.addSubscription(cartridgeSubscription);

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Existing SubscriptionContext not found for tenant " + cartridgeSubscription.getSubscriber().getTenantId());
            }
            //create a new subscription context and add the subscription
            SubscriptionContext subscriptionContext = new SubscriptionContext();
            subscriptionContext.addSubscription(cartridgeSubscription);
            tenantIdToSubscriptionContext.addSubscriptionContext(cartridgeSubscription.getSubscriber().getTenantId(), subscriptionContext);
        }

    }

    public void removeSubscription (int tenantId, String type, String subscriptionAlias, String clusterId) {

        tenantIdToSubscriptionContext.removeSubscriptionContext(tenantId, type, subscriptionAlias);
        clusterIdToSubscription.removeSubscription(clusterId, subscriptionAlias);
    }

    public Collection<CartridgeSubscription> getSubscriptions (String cartridgeType) {

        Collection<SubscriptionContext> subscriptionContexts = tenantIdToSubscriptionContext.getSubscriptionContexts();
        if (subscriptionContexts == null) {
            // no subscriptions
            return null;
        }

        Collection<CartridgeSubscription> cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
        //  iterate through the SubscriptionContexts
        for (SubscriptionContext subscriptionContext : subscriptionContexts) {
            // check if CartridgeSubscriptions exist for the given type, in each SubscriptionContext instance
            Collection<CartridgeSubscription> cartridgeSubscriptionsOfType = subscriptionContext.getSubscriptionsOfType(cartridgeType);
            if (cartridgeSubscriptionsOfType != null && !cartridgeSubscriptionsOfType.isEmpty()) {
                // collect the relevant CartridgeSubscriptions
                cartridgeSubscriptions.addAll(cartridgeSubscriptionsOfType);
            }
        }

        return cartridgeSubscriptions;
    }

    public Collection<CartridgeSubscription> getSubscriptions (int tenantId) {

        SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.getSubscriptionContext(tenantId);
        if (subscriptionContext == null) {
            // no subscriptions
            return null;
        }

        return subscriptionContext.getSubscriptions();

    }

    public Collection<CartridgeSubscription> getSubscriptionForType (int tenantId, String cartridgeType) {

         SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.getSubscriptionContext(tenantId);
         if (subscriptionContext == null) {
            // no subscriptions
            return null;
         }

         return subscriptionContext.getSubscriptionsOfType(cartridgeType);
    }

    public CartridgeSubscription getSubscriptionForAlias (int tenantId, String subscriptionAlias) {

        SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.getSubscriptionContext(tenantId);
        if (subscriptionContext == null) {
            // no subscriptions
            if (log.isDebugEnabled()) {
                log.debug("No SubscriptionContext found for tenant " + tenantId + ", subscription alias " + subscriptionAlias);
            }
            return null;
        }

        return subscriptionContext.getSubscriptionForAlias(subscriptionAlias);

    }

    public Set<CartridgeSubscription> getSubscription (String clusterId) {

        return clusterIdToSubscription.getSubscription(clusterId);

    }

    public void acquireWriteLock () {

        writeLock.lock();
    }

    public void releaseWriteLock () {

        writeLock.unlock();
    }

    public void acquireReadLock () {

        readLock.lock();
    }

    public void releaseReadLock () {

        readLock.unlock();
    }
}
