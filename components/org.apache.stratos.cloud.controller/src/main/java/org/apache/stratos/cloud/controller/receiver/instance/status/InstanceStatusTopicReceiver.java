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
package org.apache.stratos.cloud.controller.receiver.instance.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
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

/**
 * This will handle the instance status events
 */
public class InstanceStatusTopicReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(InstanceStatusTopicReceiver.class);

    private InstanceStatusEventReceiver statusEventReceiver;
    private boolean terminated;

    public InstanceStatusTopicReceiver() {
        this.statusEventReceiver = new InstanceStatusEventReceiver();
        addEventListeners();
    }


    @Override
    public void run() {
        Thread thread = new Thread(statusEventReceiver);
        thread.start();
        if (log.isInfoEnabled()) {
            log.info("Cloud controller application status thread started");
        }
        ///* Keep the thread live until terminated
        while (!terminated) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Cloud controller application status thread terminated");
        }
    }

    private void addEventListeners() {
        statusEventReceiver.addEventListener(new InstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleMemberActivated((InstanceActivatedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new InstanceStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleMemberStarted((InstanceStartedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new InstanceReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleMemberReadyToShutdown((InstanceReadyToShutdownEvent) event);
                } catch (Exception e) {
                    String error = "Failed to retrieve the instance status event message";
                    log.error(error, e);
                }
            }
        });

        statusEventReceiver.addEventListener(new InstanceMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleMemberMaintenance((InstanceMaintenanceModeEvent) event);
                } catch (Exception e) {
                String error = "Failed to retrieve the instance status event message";
                log.error(error, e);
                }
            }
        });


    }

}
