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

package org.apache.stratos.adc.mgt.subscription.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class CartridgeInstanceCache {

    private static final Log log = LogFactory.getLog(CartridgeInstanceCache.class);

    private Map<CartridgeInstanceCacheKey, CartridgeSubscription> cartridgeInstanceKeyToCartridgeInstance;
    private final Map<Integer, List<CartridgeSubscription>> tenantIdToCartridgeInstance;

    private static CartridgeInstanceCache cartridgeInstanceCache;

    private CartridgeInstanceCache() {
        cartridgeInstanceKeyToCartridgeInstance = new ConcurrentHashMap<CartridgeInstanceCacheKey,
                CartridgeSubscription>();
        tenantIdToCartridgeInstance = new ConcurrentHashMap<Integer, List<CartridgeSubscription>>();
    }

    public static CartridgeInstanceCache getCartridgeInstanceCache()  {

        if (cartridgeInstanceCache == null) {
            synchronized(CartridgeInstanceCache.class) {
                if (cartridgeInstanceCache == null)  {
                    cartridgeInstanceCache = new CartridgeInstanceCache();
                }
            }
        }
        return cartridgeInstanceCache;
    }

    public void addCartridgeInstances (int tenantId, List<CartridgeSubscription> cartridgeSubscriptions) {

        //tenantIdToCartridgeInstance.putSubscription(tenantId, cartridgeSubscriptions);
        for(CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            addCartridgeInstance(new CartridgeInstanceCacheKey(tenantId, cartridgeSubscription.getAlias()),
                    cartridgeSubscription);
        }
    }

    public void addCartridgeInstance(CartridgeInstanceCacheKey cartridgeInstanceCacheKey,
                                     CartridgeSubscription cartridgeSubscription) {

        cartridgeInstanceKeyToCartridgeInstance.put(cartridgeInstanceCacheKey, cartridgeSubscription);

        List<CartridgeSubscription> cartridgeSubscriptions = tenantIdToCartridgeInstance.get(cartridgeInstanceCacheKey.
                getTenantId());
        if(cartridgeSubscriptions != null) {
            cartridgeSubscriptions.add(cartridgeSubscription);
        }
        else {
            cartridgeSubscriptions = new Vector<CartridgeSubscription>();
            cartridgeSubscriptions.add(cartridgeSubscription);
            tenantIdToCartridgeInstance.put(cartridgeInstanceCacheKey.getTenantId(), cartridgeSubscriptions);
        }

        log.info("Added tenant " + cartridgeInstanceCacheKey.getTenantId() + "'s cartridge subscription with alias " +
                cartridgeInstanceCacheKey.getCartridgeInstanceAlias() + " to the cache");
    }

    public CartridgeSubscription getCartridgeInstance (CartridgeInstanceCacheKey cartridgeInstanceCacheKey) {

        return cartridgeInstanceKeyToCartridgeInstance.get(cartridgeInstanceCacheKey);
    }

    public List<CartridgeSubscription> getCartridgeInstances (int tenantId) {

        return tenantIdToCartridgeInstance.get(tenantId);
    }

    public boolean alreadyExists (CartridgeInstanceCacheKey cartridgeInstanceCacheKey) {

        return cartridgeInstanceKeyToCartridgeInstance.containsKey(cartridgeInstanceCacheKey);
    }

    public void removeCartridgeInstances (int tenantId) {

        List<CartridgeSubscription> cartridgeSubscriptions = tenantIdToCartridgeInstance.get(tenantId);
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            removeCartridgeInstance(new CartridgeInstanceCacheKey(tenantId, cartridgeSubscription.getAlias()));
        }
    }

    public void removeCartridgeInstance (CartridgeInstanceCacheKey cartridgeInstanceCacheKey) {

        if (cartridgeInstanceKeyToCartridgeInstance.remove(cartridgeInstanceCacheKey) != null &&
        tenantIdToCartridgeInstance.remove(cartridgeInstanceCacheKey.getTenantId()) != null) {

            log.info("Removed tenant " + cartridgeInstanceCacheKey.getTenantId() + "'s cartridge subscription with alias " +
                    cartridgeInstanceCacheKey.getCartridgeInstanceAlias() + " from the cache");
        }
    }
}
