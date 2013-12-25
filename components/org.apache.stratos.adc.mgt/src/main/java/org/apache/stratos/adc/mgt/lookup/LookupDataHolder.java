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

package org.apache.stratos.adc.mgt.lookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;

import java.io.Serializable;
import java.util.Collection;
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

    public void put (CartridgeSubscription cartridgeSubscription) {

        writeLock.lock();

        try {
            if (clusterIdToSubscription.getSubscription(cartridgeSubscription.getClusterDomain()) != null) {
                if(log.isDebugEnabled()) {
                    log.debug("Overwriting the existing CartridgeSubscription for tenant " + cartridgeSubscription.getSubscriber().getTenantId() +
                            ", alias " + cartridgeSubscription.getAlias());
                }
            }
            // add or update
            clusterIdToSubscription.addSubscription(cartridgeSubscription);

            // check if an existing SubscriptionContext is available
            SubscriptionContext existingSubscriptionCtx = tenantIdToSubscriptionContext.getSubscriptionContext(cartridgeSubscription.getSubscriber().getTenantId());
            if(existingSubscriptionCtx != null) {
                existingSubscriptionCtx.addSubscription(cartridgeSubscription);

            } else {
                //create a new subscription context and add the subscription
                SubscriptionContext subscriptionContext = new SubscriptionContext();
                subscriptionContext.addSubscription(cartridgeSubscription);
                tenantIdToSubscriptionContext.addSubscriptionContext(cartridgeSubscription.getSubscriber().getTenantId(), subscriptionContext);
            }

        } finally {
            writeLock.unlock();
        }
    }

    public Collection<CartridgeSubscription> getSubscriptions (int tenantId) {

        readLock.lock();

        try {
            SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.getSubscriptionContext(tenantId);
            if (subscriptionContext == null) {
                // no subscriptions
                return null;
            }

            return subscriptionContext.getSubscriptions();

        } finally {
            readLock.unlock();
        }
    }

    public Collection<CartridgeSubscription> getSubscriptionForType (int tenantId, String cartridgeType) {

        readLock.lock();

        try {
            SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.getSubscriptionContext(tenantId);
            if (subscriptionContext == null) {
                // no subscriptions
                return null;
            }

            return subscriptionContext.getSubscriptionsOfType(cartridgeType);

        } finally {
            readLock.unlock();
        }
    }

    public CartridgeSubscription getSubscriptionForAlias (int tenantId, String subscriptionAlias) {

        readLock.lock();

        try {
            SubscriptionContext subscriptionContext = tenantIdToSubscriptionContext.getSubscriptionContext(tenantId);
            if (subscriptionContext == null) {
                // no subscriptions
                return null;
            }

            return subscriptionContext.getSubscriptionForAlias(subscriptionAlias);

        } finally {
            readLock.unlock();
        }
    }

    public CartridgeSubscription getSubscription (String clusterId) {

        readLock.lock();

        try {
            return clusterIdToSubscription.getSubscription(clusterId);

        } finally {
            readLock.unlock();
        }
    }
}
