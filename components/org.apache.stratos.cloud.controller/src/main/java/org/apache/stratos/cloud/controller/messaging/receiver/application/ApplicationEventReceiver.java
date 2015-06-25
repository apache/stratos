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
package org.apache.stratos.cloud.controller.messaging.receiver.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.ApplicationDeletedEvent;
import org.apache.stratos.messaging.event.application.ApplicationInstanceTerminatedEvent;
import org.apache.stratos.messaging.listener.application.ApplicationDeletedEventListener;
import org.apache.stratos.messaging.listener.application.ApplicationInstanceTerminatedEventListener;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;

import java.util.concurrent.ExecutorService;

/**
 * This is to receive the application topic messages.
 */
public class ApplicationEventReceiver {
    private static final Log log = LogFactory.getLog(ApplicationEventReceiver.class);
    private ApplicationsEventReceiver applicationsEventReceiver;
    private ExecutorService executorService;

    public ApplicationEventReceiver() {
        this.applicationsEventReceiver = new ApplicationsEventReceiver();
        addEventListeners();

    }

    public void execute() {

        if (log.isInfoEnabled()) {
            log.info("Cloud controller application event receiver thread started");
        }
        applicationsEventReceiver.setExecutorService(executorService);
        applicationsEventReceiver.execute();
    }

    private void addEventListeners() {

        applicationsEventReceiver.addEventListener(new ApplicationInstanceTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                // Remove the application related data
                ApplicationInstanceTerminatedEvent instanceTerminatedEvent =
                        (ApplicationInstanceTerminatedEvent) event;
                log.info("Application instance terminated event received: [application-id] " +
                        instanceTerminatedEvent.getAppId());
                String appId = instanceTerminatedEvent.getAppId();
                Application application = ApplicationManager.getApplications().
                        getApplication(instanceTerminatedEvent.getAppId());

                if(application.getInstanceContextCount() == 0) {
                    TopologyBuilder.handleApplicationClustersRemoved(appId,
                            application.getClusterDataRecursively());
                }
            }
        });
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
