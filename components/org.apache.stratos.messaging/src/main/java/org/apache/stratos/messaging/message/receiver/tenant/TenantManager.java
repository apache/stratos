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

package org.apache.stratos.messaging.message.receiver.tenant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.tenant.Tenant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  A singleton class for managing the tenant information.
 *
 *  Usage:
 *  Acquire a relevant lock and invoke a method inside a try block.
 *  Once processing is done release the lock using a finally block.
 */
public class TenantManager {
    private static final Log log = LogFactory.getLog(TenantManager.class);

    private static volatile TenantManager instance;
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private Map<Integer, Tenant> tenantIdTenantMap;
    private Map<String, Tenant> tenantDomainTenantMap;
    private boolean initialized;

    public static void acquireReadLock() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock acquired");
        }
        readLock.lock();
    }

    public static void releaseReadLock() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock released");
        }
        readLock.unlock();
    }

    public static void acquireWriteLock() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock acquired");
        }
        writeLock.lock();
    }

    public static void releaseWriteLock() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock released");
        }
        writeLock.unlock();
    }

    private TenantManager() {
        this.tenantIdTenantMap = new HashMap<Integer, Tenant>();
        this.tenantDomainTenantMap = new HashMap<String, Tenant>();
    }

    public static synchronized TenantManager getInstance() {
        if (instance == null) {
            synchronized (TenantManager.class){
                if (instance == null) {
                    instance = new TenantManager();
                    if(log.isDebugEnabled()) {
                        log.debug("TenantManager object created");
                    }
                }
            }
        }
        return instance;
    }

    public void addTenant(Tenant tenant) {
        this.tenantIdTenantMap.put(tenant.getTenantId(), tenant);
        this.tenantDomainTenantMap.put(tenant.getTenantDomain(), tenant);
    }

    public void addTenants(List<Tenant> tenants) {
        for(Tenant tenant : tenants) {
            addTenant(tenant);
        }
    }

    public Tenant getTenant(int tenantId) {
        return this.tenantIdTenantMap.get(tenantId);
    }

    public Tenant getTenant(String tenantDomain) {
        return this.tenantDomainTenantMap.get(tenantDomain);
    }

    public boolean tenantExists(int tenantId) {
        return tenantIdTenantMap.containsKey(tenantId);
    }

    public void removeTenant(int tenantId) {
        Tenant tenant = getTenant(tenantId);
        if(tenant != null) {
            tenantIdTenantMap.remove(tenant.getTenantId());
            tenantDomainTenantMap.remove(tenant.getTenantDomain());
        }
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
