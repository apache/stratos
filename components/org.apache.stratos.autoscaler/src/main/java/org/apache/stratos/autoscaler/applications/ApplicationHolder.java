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

package org.apache.stratos.autoscaler.applications;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;


public class ApplicationHolder {

    private static final Log log = LogFactory.getLog(ApplicationHolder.class);

    private static volatile Applications applications;

    private static ReadWriteLock lock = new ReadWriteLock("application-holder");

    private ApplicationHolder () {
    }

    public static void acquireReadLock() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock acquired");
        }
        lock.acquireReadLock();
    }

    public static void releaseReadLock() {
        if(log.isDebugEnabled()) {
            log.debug("Read lock released");
        }
        lock.releaseReadLock();
    }

    public static void acquireWriteLock() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock acquired");
        }
        lock.acquireWriteLock();
    }

    public static void releaseWriteLock() {
        if(log.isDebugEnabled()) {
            log.debug("Write lock released");
        }
        lock.releaseWriteLock();
    }

    public static Applications getApplications () {
        if (applications == null) {
            synchronized (ApplicationHolder.class) {
                if (applications == null) {
                    // retrieve from registry
                    if (log.isDebugEnabled()) {
                        log.debug("Trying to retrieve applications from registry...");
                    }
                    applications = AutoscalerUtil.getApplications();
                    if (applications == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("No applications found in registry");
                        }
                        // create a new Applications object
                        applications = new Applications();
                    }
                }
            }
        }
        return applications;
    }

    public static void persistApplication (Application application) {
        synchronized (ApplicationHolder.class) {
            getApplications().addApplication(application);
            AutoscalerUtil.persistApplication(application);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Applications updated: %s", toJson(applications)));
            }
        }
    }

    public static void removeApplication (String applicationId) {
        synchronized (ApplicationHolder.class) {
            getApplications().removeApplication(applicationId);
            AutoscalerUtil.removeApplication(applicationId);
            if (log.isDebugEnabled()) {
                log.debug("Application [ " + applicationId + " ] removed from application holder");
            }
        }
    }

    private static String toJson(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }
}
