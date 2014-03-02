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
    private static final String CACHE_MANAGER_NAME = "LoadBalancerCache";

    private static CacheManager getCacheManager() {
        CacheManager cacheManager = (CacheManager) Caching.getCacheManagerFactory().getCacheManager(CACHE_MANAGER_NAME);
        if(cacheManager == null) {
            throw new RuntimeException("Could not get cache manager");
        }
        return cacheManager;
    }

    private static void startSuperTenantFlow() {
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
    }

    private static void endSuperTenantFlow() {
        PrivilegedCarbonContext.endTenantFlow();
    }

    static void putString(String cacheName, String propertyName, String value) {
        try {
            startSuperTenantFlow();
            Cache<String, String> cache = getCacheManager().getCache(cacheName);
            cache.put(propertyName, value);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cached property: [cache] %s [property] %s [value] %s", cacheName, propertyName, value));
            }
        } finally {
            endSuperTenantFlow();
        }
    }

    static String getString(String cacheName, String propertyName) {
        try {
            startSuperTenantFlow();
            Cache<String, String> cache = getCacheManager().getCache(cacheName);
            String value = cache.get(propertyName);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Read cached property: [cache] %s [property] %s [value] %s", cacheName, propertyName, value));
            }
            return value;
        } finally {
            endSuperTenantFlow();
        }
    }

    static void putInteger(String cacheName, String propertyName, int value) {
        try {
            startSuperTenantFlow();
            Cache<String, Integer> cache = getCacheManager().getCache(cacheName);
            cache.put(propertyName, value);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cached property: [cache] %s [property] %s [value] %d", cacheName, propertyName, value));
            }
        } finally {
            endSuperTenantFlow();
        }
    }

    static int getInteger(String cacheName, String propertyName) {
        try {
            startSuperTenantFlow();
            Cache<String, Integer> cache = getCacheManager().getCache(cacheName);
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
