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

package org.apache.stratos.load.balancer.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;


/**
 * Load balancer cache manages the load balancer contexts in a clustered environment.
 */
class LoadBalancerCache {
    private static final Log log = LogFactory.getLog(LoadBalancerCache.class);
    private static volatile LoadBalancerCache instance;

    private CacheManager cacheManager;

    private LoadBalancerCache() {
        try {
            startSuperTenantFlow();
            cacheManager = (CacheManager) Caching.getCacheManagerFactory().getCacheManager("LoadBalancerCache");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            throw new RuntimeException(e);
        } finally {
            endSuperTenantFlow();
        }
    }

    public static LoadBalancerCache getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerCache.class) {
                if (instance == null) {
                    instance = new LoadBalancerCache();
                }
            }
        }
        return instance;
    }

    private void startSuperTenantFlow() {
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
    }

    private void endSuperTenantFlow() {
        PrivilegedCarbonContext.endTenantFlow();
    }

    void putString(String cacheName, String propertyName, String value) {
        try {
            startSuperTenantFlow();
            Cache<String, String> cache = cacheManager.getCache(cacheName);
            cache.put(propertyName, value);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cached property: [cache] %s [property] %s [value] %s", cacheName, propertyName, value));
            }
        } finally {
            endSuperTenantFlow();
        }
    }

    String getString(String cacheName, String propertyName) {
        try {
            startSuperTenantFlow();
            Cache<String, String> cache = cacheManager.getCache(cacheName);
            String value = cache.get(propertyName);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Read cached property: [cache] %s [property] %s [value] %s", cacheName, propertyName, value));
            }
            return value;
        } finally {
            endSuperTenantFlow();
        }
    }

    void putInteger(String cacheName, String propertyName, int value) {
        try {
            startSuperTenantFlow();
            Cache<String, Integer> cache = cacheManager.getCache(cacheName);
            cache.put(propertyName, value);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cached property: [cache] %s [property] %s [value] %d", cacheName, propertyName, value));
            }
        } finally {
            endSuperTenantFlow();
        }
    }

    int getInteger(String cacheName, String propertyName) {
        try {
            startSuperTenantFlow();
            Cache<String, Integer> cache = cacheManager.getCache(cacheName);
            int value = cache.get(propertyName);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Read cached property: [cache] %s [property] %s [value] %d", cacheName, propertyName, value));
            }
            return value;
        } finally {
            endSuperTenantFlow();
        }
    }
}
