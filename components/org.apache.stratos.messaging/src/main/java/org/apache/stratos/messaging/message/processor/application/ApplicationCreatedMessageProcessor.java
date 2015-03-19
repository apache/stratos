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
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.event.application.ApplicationCreatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.application.updater.ApplicationsUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

public class ApplicationCreatedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationCreatedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        Applications applications = (Applications) object;

        if (ApplicationCreatedEvent.class.getName().equals(type)) {
            if (!applications.isInitialized()) {
                return false;
            }

            ApplicationCreatedEvent event = (ApplicationCreatedEvent) MessagingUtil.jsonToObject(message, ApplicationCreatedEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to ApplicationCreatedEvent");
                return false;
            }

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

    private boolean doProcess(ApplicationCreatedEvent event, Applications applications) {

        // check if required properties are available
        if (event.getApplication() == null) {
            String errorMsg = "Application object of application created event is invalid";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (event.getApplication().getUniqueIdentifier() == null || event.getApplication().getUniqueIdentifier().isEmpty()) {
            String errorMsg = "App id of application created event is invalid: [ " + event.getApplication().getUniqueIdentifier() + " ]";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // check if an Application with same name exists in applications
        if (applications.applicationExists(event.getApplication().getUniqueIdentifier())) {
            if (log.isDebugEnabled()) {
                log.debug("Application with id [ " + event.getApplication().getUniqueIdentifier() + " ] already exists");
            }
        } else {
            // add application and the clusters to Topology
            applications.addApplication(event.getApplication());
            if (log.isInfoEnabled()) {
                log.info("Application with id [ " + event.getApplication().getUniqueIdentifier() + " ] created");
            }
        }

        notifyEventListeners(event);
        return true;
    }
}
