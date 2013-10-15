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

package org.apache.stratos.adc.mgt.instance.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.instance.CartridgeInstance;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class CartridgeInstanceCache {

    private static final Log log = LogFactory.getLog(CartridgeInstanceCache.class);

    private Map<CartridgeInstanceCacheKey, CartridgeInstance> cartridgeInstanceKeyToCartridgeInstance;
    private final Map<Integer, List<CartridgeInstance>> tenantIdToCartridgeInstance;

    private static CartridgeInstanceCache cartridgeInstanceCache;

    private CartridgeInstanceCache() {
        cartridgeInstanceKeyToCartridgeInstance = new ConcurrentHashMap<CartridgeInstanceCacheKey,
                CartridgeInstance>();
        tenantIdToCartridgeInstance = new ConcurrentHashMap<Integer, List<CartridgeInstance>>();
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

    public void addCartridgeInstances (int tenantId, List<CartridgeInstance> cartridgeInstances) {

        //tenantIdToCartridgeInstance.put(tenantId, cartridgeInstances);
        for(CartridgeInstance cartridgeInstance : cartridgeInstances) {
            addCartridgeInstance(new CartridgeInstanceCacheKey(tenantId, cartridgeInstance.getAlias()),
                    cartridgeInstance);
        }
    }

    public void addCartridgeInstance(CartridgeInstanceCacheKey cartridgeInstanceCacheKey,
                                     CartridgeInstance cartridgeInstance) {

        cartridgeInstanceKeyToCartridgeInstance.put(cartridgeInstanceCacheKey, cartridgeInstance);

        List<CartridgeInstance> cartridgeInstances = tenantIdToCartridgeInstance.get(cartridgeInstanceCacheKey.
                getTenantId());
        if(cartridgeInstances != null) {
            cartridgeInstances.add(cartridgeInstance);
        }
        else {
            cartridgeInstances = new Vector<CartridgeInstance>();
            cartridgeInstances.add(cartridgeInstance);
            tenantIdToCartridgeInstance.put(cartridgeInstanceCacheKey.getTenantId(), cartridgeInstances);
        }

        log.info("Added tenant " + cartridgeInstanceCacheKey.getTenantId() + "'s cartridge instance with alias " +
                cartridgeInstanceCacheKey.getCartridgeInstanceAlias() + " to the cache");
    }

    public CartridgeInstance getCartridgeInstance (CartridgeInstanceCacheKey cartridgeInstanceCacheKey) {

        return cartridgeInstanceKeyToCartridgeInstance.get(cartridgeInstanceCacheKey);
    }

    public List<CartridgeInstance> getCartridgeInstances (int tenantId) {

        return tenantIdToCartridgeInstance.get(tenantId);
    }

    public boolean alreadyExists (CartridgeInstanceCacheKey cartridgeInstanceCacheKey) {

        return cartridgeInstanceKeyToCartridgeInstance.containsKey(cartridgeInstanceCacheKey);
    }

    public void removeCartridgeInstances (int tenantId) {

        List<CartridgeInstance> cartridgeInstances = tenantIdToCartridgeInstance.get(tenantId);
        for (CartridgeInstance cartridgeInstance : cartridgeInstances) {
            removeCartridgeInstance(new CartridgeInstanceCacheKey(tenantId, cartridgeInstance.getAlias()));
        }
    }

    public void removeCartridgeInstance (CartridgeInstanceCacheKey cartridgeInstanceCacheKey) {

        if (cartridgeInstanceKeyToCartridgeInstance.remove(cartridgeInstanceCacheKey) != null &&
        tenantIdToCartridgeInstance.remove(cartridgeInstanceCacheKey.getTenantId()) != null) {

            log.info("Removed tenant " + cartridgeInstanceCacheKey.getTenantId() + "'s cartridge instance with alias " +
                    cartridgeInstanceCacheKey.getCartridgeInstanceAlias() + " from the cache");
        }
    }
}
