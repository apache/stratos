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
package org.apache.stratos.messaging.message.processor.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.event.application.ApplicationInstanceTerminatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.application.updater.ApplicationsUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * This processor responsible to process the application Inactivation even and update the Topology.
 */
public class ApplicationInstanceTerminatedMessageProcessor extends MessageProcessor {
    private static final Log log =
            LogFactory.getLog(ApplicationInstanceTerminatedMessageProcessor.class);


    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }


    @Override
    public boolean process(String type, String message, Object object) {
        Applications applications = (Applications) object;

        if (ApplicationInstanceTerminatedEvent.class.getName().equals(type)) {
            // Return if applications has not been initialized
            if (!applications.isInitialized())
                return false;

            // Parse complete message and build event
            ApplicationInstanceTerminatedEvent event = (ApplicationInstanceTerminatedEvent) MessagingUtil.
                    jsonToObject(message, ApplicationInstanceTerminatedEvent.class);

            ApplicationsUpdater.acquireWriteLockForApplications();

            try {
                return doProcess(event, applications);

            } finally {
                ApplicationsUpdater.releaseWriteLockForApplications();
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, applications);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess(ApplicationInstanceTerminatedEvent event, Applications applications) {

        // check if required properties are available
        if (event.getAppId() == null) {
            String errorMsg = "Application Id of application removed event is invalid";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // check if an Application with same name exists in applications
        String appId = event.getAppId();
        String instanceId = event.getInstanceId();
        if (applications.applicationExists(appId)) {
            log.warn("Application with id [ " + appId + " ] still exists in Applications, removing it");
            ApplicationInstance instance = applications.getApplication(appId).
                    getInstanceContexts(instanceId);
            if (instance == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Application [Instance] " + instanceId + " has already been removed");
                }
            } else {
                instance.setStatus(ApplicationStatus.Terminated);
                applications.getApplication(appId).removeInstance(instanceId);
            }

        }

        notifyEventListeners(event);
        return true;

    }
}
