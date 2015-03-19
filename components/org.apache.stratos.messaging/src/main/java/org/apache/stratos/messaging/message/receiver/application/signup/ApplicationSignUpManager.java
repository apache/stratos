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

package org.apache.stratos.messaging.message.receiver.application.signup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Application signup manager.
 */
public class ApplicationSignUpManager {

    private static final Log log = LogFactory.getLog(ApplicationSignUpManager.class);

    // Map<ApplicationId,Map<TenantId,ApplicationSignUp>>
    private Map<String, Map<Integer, ApplicationSignUp>> applicationIdToApplicationSignUpsMap;

    private static volatile ApplicationSignUpManager instance;
    private static volatile ReadWriteLock lock = new ReadWriteLock("application-signup-manager");

    private boolean initialized;

    private ApplicationSignUpManager() {
        applicationIdToApplicationSignUpsMap = new HashMap<String, Map<Integer, ApplicationSignUp>>();
    }

    public static void acquireReadLock() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock acquired");
        }
        lock.acquireReadLock();
    }

    public static void releaseReadLock() {
        if (log.isDebugEnabled()) {
            log.debug("Read lock released");
        }
        lock.releaseReadLock();
    }

    public static void acquireWriteLock() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock acquired");
        }
        lock.acquireWriteLock();
    }

    public static void releaseWriteLock() {
        if (log.isDebugEnabled()) {
            log.debug("Write lock released");
        }
        lock.releaseWriteLock();
    }

    public static ApplicationSignUpManager getInstance() {
        if (instance == null) {
            synchronized (ApplicationSignUpManager.class) {
                if (instance == null) {
                    instance = new ApplicationSignUpManager();
                    if (log.isDebugEnabled()) {
                        log.debug("Application signup manager instance created");
                    }
                }
            }
        }
        return instance;
    }

    public void addApplicationSignUp(ApplicationSignUp applicationSignUp) {
        String applicationId = applicationSignUp.getApplicationId();
        Map<Integer, ApplicationSignUp> applicationSignUpMap = applicationIdToApplicationSignUpsMap.get(applicationId);
        if (applicationSignUpMap == null) {
            applicationSignUpMap = new HashMap<Integer, ApplicationSignUp>();
            applicationIdToApplicationSignUpsMap.put(applicationId, applicationSignUpMap);
        }

        int tenantId = applicationSignUp.getTenantId();
        if (applicationSignUpMap.get(tenantId) == null) {
            applicationSignUpMap.put(tenantId, applicationSignUp);
        }
    }

    public Collection<ApplicationSignUp> getApplicationSignUps(String applicationId) {
        Map<Integer, ApplicationSignUp> applicationSignUpMap = applicationIdToApplicationSignUpsMap.get(applicationId);
        if (applicationSignUpMap != null) {
            return applicationSignUpMap.values();
        }
        return null;
    }

    public ApplicationSignUp getApplicationSignUp(String applicationId, int tenantId) {
        Map<Integer, ApplicationSignUp> applicationSignUpMap = applicationIdToApplicationSignUpsMap.get(applicationId);
        if (applicationSignUpMap != null) {
            if (applicationSignUpMap.containsKey(tenantId)) {
                return applicationSignUpMap.get(tenantId);
            }
        }
        return null;
    }

    public void removeApplicationSignUp(String applicationId, int tenantId) {
        Map<Integer, ApplicationSignUp> applicationSignUpMap = applicationIdToApplicationSignUpsMap.get(applicationId);
        if (applicationSignUpMap != null) {
            if (applicationSignUpMap.containsKey(tenantId)) {
                applicationSignUpMap.remove(tenantId);
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
