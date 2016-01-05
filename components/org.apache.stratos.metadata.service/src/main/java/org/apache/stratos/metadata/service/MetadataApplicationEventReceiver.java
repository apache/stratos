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
package org.apache.stratos.metadata.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.CompleteApplicationsEvent;
import org.apache.stratos.messaging.listener.application.CompleteApplicationsEventListener;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.metadata.service.registry.MetadataApiRegistry;

import java.util.Map;

/**
 * Application receiver class for metadata service
 */
public class MetadataApplicationEventReceiver {
    private static final Log log = LogFactory.getLog(MetadataApplicationEventReceiver.class);
    private ApplicationsEventReceiver applicationsEventReceiver;

    public MetadataApplicationEventReceiver() {
        this.applicationsEventReceiver = ApplicationsEventReceiver.getInstance();
        addEventListeners();
    }

    private void addEventListeners() {
        applicationsEventReceiver.addEventListener(new CompleteApplicationsEventListener() {
            @Override
            protected void onEvent(Event event) {
                CompleteApplicationsEvent completeApplicationsEvent = (CompleteApplicationsEvent) event;
                Applications applications = completeApplicationsEvent.getApplications();
                for (Map.Entry<String, Application> entry : applications.getApplications().entrySet()) {
                    ApplicationStatus status = entry.getValue().getStatus();
                    String appId = entry.getKey();
                    if (ApplicationStatus.Active.equals(status) || ApplicationStatus.Inactive.equals(status)) {
                        MetadataApiRegistry.getApplicationIdToReadWriteLockMap().putIfAbsent(appId,
                                new ReadWriteLock(Constants.METADATA_SERVICE_THREAD_POOL_ID.concat(appId)));
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Metadata service READ WRITE locks initialized on complete applications event.");
                }
                //terminate();
            }
        });
    }

    public void terminate() {
        applicationsEventReceiver.terminate();
        if (log.isInfoEnabled()) {
            log.info("Metadata service application receiver stopped.");
        }
    }

}
