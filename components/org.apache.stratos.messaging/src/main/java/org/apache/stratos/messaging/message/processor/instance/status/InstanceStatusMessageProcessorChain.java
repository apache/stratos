/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.processor.instance.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceActivatedEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceMaintenanceListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceReadyToShutdownEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceStartedEventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;
import org.apache.stratos.messaging.message.processor.instance.notifier.ArtifactUpdateMessageProcessor;
import org.apache.stratos.messaging.message.processor.instance.notifier.InstanceCleanupClusterNotifierMessageProcessor;
import org.apache.stratos.messaging.message.processor.instance.notifier.InstanceCleanupMemberNotifierMessageProcessor;

/**
 * Defines default instance notifier message processor chain.
 */
public class InstanceStatusMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(InstanceStatusMessageProcessorChain.class);

    private InstanceStatusMemberStartedMessageProcessor instanceStatusMemberStartedMessageProcessor;
    private InstanceStatusMemberActivatedMessageProcessor instanceStatusMemberActivatedMessageProcessor;
    private InstanceStatusMemberReadyToShutdownMessageProcessor instanceStatusMemberReadyToShutdownMessageProcessor;
    private InstanceStatusMemberMaintenanceMessageProcessor instanceStatusMemberMaintenanceMessageProcessor;


    public void initialize() {
        // Add instance notifier event processors
        instanceStatusMemberActivatedMessageProcessor = new InstanceStatusMemberActivatedMessageProcessor();
        add(instanceStatusMemberActivatedMessageProcessor);

        instanceStatusMemberStartedMessageProcessor = new InstanceStatusMemberStartedMessageProcessor();
        add(instanceStatusMemberStartedMessageProcessor);

        instanceStatusMemberReadyToShutdownMessageProcessor = new InstanceStatusMemberReadyToShutdownMessageProcessor();
        add(instanceStatusMemberReadyToShutdownMessageProcessor);

        instanceStatusMemberMaintenanceMessageProcessor = new InstanceStatusMemberMaintenanceMessageProcessor();
        add(instanceStatusMemberMaintenanceMessageProcessor);


        if (log.isDebugEnabled()) {
            log.debug("Instance notifier message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {
        if (eventListener instanceof InstanceStartedEventListener) {
            instanceStatusMemberStartedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof InstanceReadyToShutdownEventListener) {
            instanceStatusMemberReadyToShutdownMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof InstanceMaintenanceListener) {
            instanceStatusMemberMaintenanceMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof InstanceActivatedEventListener) {
            instanceStatusMemberActivatedMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
