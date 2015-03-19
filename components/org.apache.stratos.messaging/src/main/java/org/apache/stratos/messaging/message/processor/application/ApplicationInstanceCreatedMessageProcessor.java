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
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.event.application.ApplicationInstanceCreatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.application.updater.ApplicationsUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

public class ApplicationInstanceCreatedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationInstanceCreatedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        Applications applications = (Applications) object;

        if (ApplicationInstanceCreatedEvent.class.getName().equals(type)) {

            if (!applications.isInitialized()) {
                return false;
            }


            ApplicationInstanceCreatedEvent event = (ApplicationInstanceCreatedEvent) MessagingUtil.jsonToObject(message,
                    ApplicationInstanceCreatedEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to ApplicationInstanceCreatedEvent");
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

    private boolean doProcess(ApplicationInstanceCreatedEvent event, Applications applications) {

        // check if required properties are available
        if (event.getApplicationInstance() == null) {
            String errorMsg = "Application instance object of application instance created event is invalid";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);

        }

        ApplicationInstance applicationInstance = event.getApplicationInstance();

        if (applicationInstance.getInstanceId() == null || applicationInstance.getInstanceId().isEmpty()) {
            String errorMsg = "App instance id of application instance created event is invalid: [ "
                    + applicationInstance.getInstanceId() + " ]";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // check if an Application instance with same name exists in applications instance
        if (null != applications.getApplication(event.getApplicationId()).
                getInstanceByNetworkPartitionId(applicationInstance.getNetworkPartitionId())) {

            log.warn("Application instance with id [ " + applicationInstance.getInstanceId() + " ] already exists");

        } else {
            // add application instance to Application Topology
            applications.getApplication(event.getApplicationId()).addInstance(applicationInstance.getInstanceId(), applicationInstance);
        }

        notifyEventListeners(event);
        return true;
    }
}
