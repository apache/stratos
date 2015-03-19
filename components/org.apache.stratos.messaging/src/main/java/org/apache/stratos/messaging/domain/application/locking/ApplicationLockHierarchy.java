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

package org.apache.stratos.messaging.domain.application.locking;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class ApplicationLockHierarchy {

    private static final Log log = LogFactory.getLog(ApplicationLockHierarchy.class);

    // lock for Applications
    private final ApplicationLock applicationLock;

    // key = Application.id
    private final Map<String, ApplicationLock> appIdToApplicationLockMap;

    private static volatile ApplicationLockHierarchy applicationLockHierarchy;

    private ApplicationLockHierarchy() {
        this.applicationLock = new ApplicationLock();
        this.appIdToApplicationLockMap = new HashMap<String, ApplicationLock>();
    }

    public static ApplicationLockHierarchy getInstance() {

        if (applicationLockHierarchy == null) {
            synchronized (ApplicationLockHierarchy.class) {
                if (applicationLockHierarchy == null) {
                    applicationLockHierarchy = new ApplicationLockHierarchy();
                }
            }
        }

        return applicationLockHierarchy;
    }

    public ApplicationLock getLockForApplication(String appId) {
        ApplicationLock applicationLock = appIdToApplicationLockMap.get(appId);
        if (applicationLock == null) {
            synchronized (ApplicationLockHierarchy.class) {
                if (applicationLock == null) {
                    applicationLock = new ApplicationLock();
                    appIdToApplicationLockMap.put(appId, applicationLock);
                }
            }
        }
        return applicationLock;
    }

    public void removeLockForApplication(String appId) {
        if (appIdToApplicationLockMap.remove(appId) != null) {
            log.info("Removed lock for application: [application-id] " + appId);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Lock already removed for application: [application-id] " + appId);
            }
        }
    }

    public ApplicationLock getApplicationLock() {
        return applicationLock;
    }
}
