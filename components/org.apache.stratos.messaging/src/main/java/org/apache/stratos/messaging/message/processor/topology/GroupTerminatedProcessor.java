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
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.domain.topology.GroupStatus;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.GroupTerminatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.topology.updater.TopologyUpdater;
import org.apache.stratos.messaging.util.Util;

/**
 * This processor will act upon the Group activation events
 */
public class GroupTerminatedProcessor extends MessageProcessor {
    private static final Log log = LogFactory.getLog(GroupTerminatedProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (GroupTerminatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized())
                return false;

            // Parse complete message and build event
            GroupTerminatedEvent event = (GroupTerminatedEvent) Util.
                    jsonToObject(message, GroupTerminatedEvent.class);

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

    private boolean doProcess (GroupTerminatedEvent event,Topology topology) {

        // Validate event against the existing topology
        Application application = topology.getApplication(event.getAppId());
        if (application == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Application does not exist: [service] %s",
                        event.getAppId()));
            }
            return false;
        }
        Group group = application.getGroupRecursively(event.getGroupId());

        if (group == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Group not exists in service: [AppId] %s [groupId] %s", event.getAppId(),
                        event.getGroupId()));
            }
        } else {
            // Apply changes to the topology
            if (!group.isStateTransitionValid(GroupStatus.Terminating)) {
                log.error("Invalid State Transition from " + group.getStatus() + " to " + GroupStatus.Active);
            }
            group.setStatus(GroupStatus.Terminated);
            if (log.isInfoEnabled())     {
                log.info(String.format("Group updated as activated : %s",
                        group.getUniqueIdentifier()));
            }
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;
    }
}
