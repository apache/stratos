/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.message.processor.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.event.application.CompleteApplicationsEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.application.updater.ApplicationsUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.Collection;

public class CompleteApplicationsMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompleteApplicationsMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Applications applications = (Applications) object;

        if (CompleteApplicationsEvent.class.getName().equals(type)) {
            // Parse complete message and build event
            CompleteApplicationsEvent event = (CompleteApplicationsEvent) MessagingUtil.
                    jsonToObject(message, CompleteApplicationsEvent.class);

            if (!applications.isInitialized()) {
                ApplicationsUpdater.acquireWriteLockForApplications();
                try {
                    doProcess(event, applications);
                } finally {
                    ApplicationsUpdater.releaseWriteLockForApplications();
                }
            }

            // Notify event listeners
            notifyEventListeners(event);
            return true;

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, applications);
            }
            return false;
        }
    }

    private void doProcess(CompleteApplicationsEvent event, Applications applications) {
        // add existing Applications to Topology
        Collection<Application> applicationsList = event.getApplications().getApplications().values();
        if (applicationsList != null && !applicationsList.isEmpty()) {
            for (Application application : applicationsList) {
                applications.addApplication(application);
                if (log.isDebugEnabled()) {
                    log.debug("Application with id [ " + application.getUniqueIdentifier() + " ] added to Applications");
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No Application information found in Complete Applications event");
            }
        }

        // Set topology initialized
        applications.setInitialized(true);
    }
}
