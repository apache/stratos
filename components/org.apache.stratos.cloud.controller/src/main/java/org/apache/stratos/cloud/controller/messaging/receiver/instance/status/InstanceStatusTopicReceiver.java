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
package org.apache.stratos.cloud.controller.messaging.receiver.instance.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.listener.instance.status.InstanceActivatedEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceMaintenanceListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceReadyToShutdownEventListener;
import org.apache.stratos.messaging.listener.instance.status.InstanceStartedEventListener;
import org.apache.stratos.messaging.message.receiver.instance.status.InstanceStatusEventReceiver;

import java.util.concurrent.ExecutorService;

/**
 * This will handle the instance status events
 */
public class InstanceStatusTopicReceiver {
    private static final Log log = LogFactory.getLog(InstanceStatusTopicReceiver.class);

    private InstanceStatusEventReceiver statusEventReceiver;
    private boolean terminated;
    private ExecutorService executorService;

    public InstanceStatusTopicReceiver() {
        this.statusEventReceiver = InstanceStatusEventReceiver.getInstance();
        addEventListeners();
    }

//    public void execute() {
//        statusEventReceiver.setExecutorService(executorService);
//        statusEventReceiver.execute();
//        if (log.isInfoEnabled()) {
//            log.info("Cloud controller application status thread started");
//        }
//
//        if (log.isInfoEnabled()) {
//            log.info("Cloud controller application status thread terminated");
//        }
//    }

    private void addEventListeners() {
        statusEventReceiver.addEventListener(new InstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceActivatedEvent instanceActivatedEvent = (InstanceActivatedEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Handling InstanceActivatedEvent for [member-id] %s",
                            instanceActivatedEvent.getMemberId()));
                }
                try {
                    TopologyBuilder.handleMemberActivated(instanceActivatedEvent);
                } catch (Exception e) {
                    log.error("Failed to process InstanceActivatedEvent", e);
                }
            }
        });

        statusEventReceiver.addEventListener(new InstanceStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceStartedEvent instanceStartedEvent = (InstanceStartedEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Handling InstanceStartedEvent for [member-id] %s",
                            instanceStartedEvent.getMemberId()));
                }
                try {
                    TopologyBuilder.handleMemberStarted(instanceStartedEvent);
                } catch (Exception e) {
                    log.error(String.format("Failed to process InstanceStartedEvent for [member-id] %s",
                            instanceStartedEvent.getMemberId()), e);
                }
            }
        });

        statusEventReceiver.addEventListener(new InstanceReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceReadyToShutdownEvent instanceReadyToShutdownEvent = (InstanceReadyToShutdownEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Handling InstanceReadyToShutdownEvent for [member-id] %s",
                            instanceReadyToShutdownEvent.getMemberId()));
                }
                try {
                    TopologyBuilder.handleMemberReadyToShutdown(instanceReadyToShutdownEvent);
                } catch (Exception e) {
                    log.error(String.format("Failed to process InstanceReadyToShutdownEvent for [member-id] %s",
                            instanceReadyToShutdownEvent.getMemberId()), e);
                }
            }
        });

        statusEventReceiver.addEventListener(new InstanceMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceMaintenanceModeEvent instanceMaintenanceModeEvent = (InstanceMaintenanceModeEvent) event;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Handling InstanceMaintenanceModeEvent for [member-id] %s",
                            instanceMaintenanceModeEvent.getMemberId()));
                }
                try {
                    TopologyBuilder.handleMemberMaintenance(instanceMaintenanceModeEvent);
                } catch (Exception e) {
                    log.error(String.format("Failed to process InstanceMaintenanceModeEvent for [member-id] %s",
                            instanceMaintenanceModeEvent.getMemberId()), e);
                }
            }
        });

    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
