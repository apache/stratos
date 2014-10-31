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
package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ApplicationInactivatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.Util;

/**
 * This processor responsible to process the application Inactivation even and update the Topology.
 */
public class ApplicationInactivatedMessageProcessor extends MessageProcessor {
    private static final Log log =
            LogFactory.getLog(ApplicationInactivatedMessageProcessor.class);


    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }


    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (ApplicationInactivatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized())
                return false;

            // Parse complete message and build event
            ApplicationInactivatedEvent event = (ApplicationInactivatedEvent) Util.
                    jsonToObject(message, ApplicationInactivatedEvent.class);

            TopologyUpdater.acquireWriteLockForApplication(event.getAppId());

            try {
                return doProcess(event, topology);

            } finally {
                TopologyUpdater.releaseWriteLockForApplication(event.getAppId());
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess (ApplicationInactivatedEvent event, Topology topology) {

        // Validate event against the existing topology
        Application application = topology.getApplication(event.getAppId());
        if (application == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Application does not exist: [service] %s",
                        event.getAppId()));
            }
            return false;
        } else {
            // Apply changes to the topology
            if (!application.isStateTransitionValid(ApplicationStatus.Inactive)) {
                log.error("Invalid State transfer from [ " + application.getStatus() +
                        " ] to [ " + ApplicationStatus.Inactive + " ]");
            }
            application.setStatus(ApplicationStatus.Inactive);

        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;

    }
}
