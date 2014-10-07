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
import org.apache.stratos.messaging.domain.topology.Application;
import org.apache.stratos.messaging.domain.topology.Status;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ApplicationActivatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Util;

/**
 * This processor responsible to process the application activation even and update the Topology.
 */
public class ApplicationActivatedMessageProcessor extends MessageProcessor {
    private static final Log log =
            LogFactory.getLog(ApplicationActivatedMessageProcessor.class);


    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }


    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (ApplicationActivatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized())
                return false;

            // Parse complete message and build event
            ApplicationActivatedEvent event = (ApplicationActivatedEvent) Util.
                    jsonToObject(message, ApplicationActivatedEvent.class);

            TopologyManager.acquireReadLockForApplications();
            TopologyManager.acquireWriteLockForApplication(event.getAppId());

            try {
                return doProcess(event, topology);

            } finally {
                TopologyManager.releaseWriteLockForApplication(event.getAppId());
                TopologyManager.releaseReadLockForApplications();
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

    private boolean doProcess (ApplicationActivatedEvent event, Topology topology) {

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
            application.setStatus(Status.Activated);
            if (log.isInfoEnabled()) {
                log.info(String.format("Application updated as activated : %s",
                        application.toString()));
            }
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;

    }
}
