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

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LookupDataHolder {

    private static final Log log = LogFactory.getLog(LookupDataHolder.class);

    private Map<Integer, SubscriptionAliasToCartridgeSubscriptionMap> tenantIdToCartridgeSubscriptionCache;
    private Map<String, CartridgeSubscription> clusterItToCartridgeSubscrptionMatch;
    //private static LookupDataHolder lookupDataHolder;

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public LookupDataHolder () {
        tenantIdToCartridgeSubscriptionCache = new HashMap<Integer, SubscriptionAliasToCartridgeSubscriptionMap>();
        clusterItToCartridgeSubscrptionMatch = new HashMap<String, CartridgeSubscription>();
    }

    /*public static LookupDataHolder getInstance ()  {

        if (lookupDataHolder == null) {
            synchronized(LookupDataHolder.class) {
                if (lookupDataHolder == null)  {
                    lookupDataHolder = new LookupDataHolder();
                }
            }
        }
        return lookupDataHolder;
    }*/

    public void addCartridgeSubscription (int tenantId, String subscriptionAlias, CartridgeSubscription
            cartridgeSubscription) {
        addSubscription(tenantId, subscriptionAlias, cartridgeSubscription);
    }

    private void addSubscription (int tenantId, String subscriptionAlias, CartridgeSubscription cartridgeSubscription) {

        writeLock.lock();

        try {
            SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap = tenantIdToCartridgeSubscriptionCache.
                    get(tenantId);

            if(aliasToSubscriptionMap != null) {
                aliasToSubscriptionMap.addSubscription(subscriptionAlias, cartridgeSubscription);

            } else {
                aliasToSubscriptionMap = new SubscriptionAliasToCartridgeSubscriptionMap();
                aliasToSubscriptionMap.addSubscription(subscriptionAlias, cartridgeSubscription);
            }

            if(clusterItToCartridgeSubscrptionMatch.put(cartridgeSubscription.getClusterDomain(), cartridgeSubscription)
                    != null) {
                log.info("Overwrote the CartridgeSubscription value for cluster Id " +
                        cartridgeSubscription.getClusterDomain());
            }

        } finally {
            writeLock.unlock();
        }
    }

    public void removeCartridgeSubscription (int tenantId, String subscriptionAlias, String clusterId) {
        removeSubscription(tenantId, subscriptionAlias, clusterId);
    }

    private void removeSubscription (int tenantId, String subscriptionAlias, String clusterId) {

        writeLock.lock();

        try {
            SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap = tenantIdToCartridgeSubscriptionCache.
                    get(tenantId);

            if(aliasToSubscriptionMap != null) {
                aliasToSubscriptionMap.removeSubscription(subscriptionAlias);
                if(aliasToSubscriptionMap.isEmpty()){
                    tenantIdToCartridgeSubscriptionCache.remove(tenantId);
                }

            } else {
                log.info("No SubscriptionAliasToCartridgeSubscriptionMap entry found for tenant Id " + tenantId);
            }

            if(clusterItToCartridgeSubscrptionMatch.remove(clusterId) != null) {
                log.info("No CartridgeSubscription entry found for cluster Id " + clusterId);
            }

        } finally {
            writeLock.unlock();
        }
    }

    public CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias) {
        return getSubscription(tenantId, subscriptionAlias);
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) {
        return getSubscriptions(tenantId);
    }

    private Collection<CartridgeSubscription> getSubscriptions (int tenantId) {

        readLock.lock();

        try {
            SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap = tenantIdToCartridgeSubscriptionCache.
                    get(tenantId);

            if(aliasToSubscriptionMap != null) {
                aliasToSubscriptionMap.getCartridgeSubscriptions();
            }

        } finally {
            readLock.unlock();
        }

        return null;
    }

    public CartridgeSubscription getCartridgeSubscription (String clusterId) {
        return getCartridgeSubscription(clusterId);
    }

    private CartridgeSubscription getSubscription (String clusterId) {

        readLock.lock();
        try {
            return clusterItToCartridgeSubscrptionMatch.get(clusterId);

        } finally {
            readLock.unlock();
        }
    }

    private CartridgeSubscription getSubscription(int tenantId, String subscriptionAlias) {

        readLock.lock();

        try {
            SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap = tenantIdToCartridgeSubscriptionCache.
                    get(tenantId);

            if(aliasToSubscriptionMap != null) {
                CartridgeSubscription cartridgeSubscription = aliasToSubscriptionMap.
                        getCartridgeSubscription(subscriptionAlias);

                if(cartridgeSubscription != null){
                    return cartridgeSubscription;

                } else {
                    log.info("No CartridgeSubscription entry found for subscription alias "
                            + subscriptionAlias);
                    //if(log.isDebugEnabled()) {
                    //    log.debug("No entry found for subscription alias " + subscriptionAlias);
                    //}
                }
            } else {
                log.info("No SubscriptionAliasToCartridgeSubscriptionMap entry found for tenant id " + tenantId);
                //if(log.isDebugEnabled()) {
                //    log.debug("No entry found for tenant id " + tenantId);
                //}
            }

            return null;

        } finally {
             readLock.unlock();
        }
    }

    private class SubscriptionAliasToCartridgeSubscriptionMap {

        private Map<String, CartridgeSubscription> subscriptionAliasToCartridgeSubscriptionMap;

        public SubscriptionAliasToCartridgeSubscriptionMap () {
            subscriptionAliasToCartridgeSubscriptionMap = new HashMap<String, CartridgeSubscription>();
        }

        public Map<String, CartridgeSubscription> getSubscriptionAliasToCartridgeSubscriptionMap() {
            return subscriptionAliasToCartridgeSubscriptionMap;
        }

        public void addSubscription(String subscriptionAlias, CartridgeSubscription cartridgeSubscription) {

            if(subscriptionAliasToCartridgeSubscriptionMap.put(subscriptionAlias, cartridgeSubscription) != null) {
                log.info("Overwrote the previous CartridgeSubscription value for subscription alias" + subscriptionAlias);
            }
        }

        public boolean isEmpty () {
            return subscriptionAliasToCartridgeSubscriptionMap.isEmpty();
        }

        public void removeSubscription (String subscriptionAlias) {

            if(subscriptionAliasToCartridgeSubscriptionMap.remove(subscriptionAlias) == null) {
                log.info("No CartridgeSubscription entry found for subscription alias " + subscriptionAlias);
            }
        }

        public CartridgeSubscription getCartridgeSubscription (String cartridgeSubscriptionAlias) {
            return subscriptionAliasToCartridgeSubscriptionMap.get(cartridgeSubscriptionAlias);
        }

        public Collection<CartridgeSubscription> getCartridgeSubscriptions () {
            return subscriptionAliasToCartridgeSubscriptionMap.values();
        }
    }
}
