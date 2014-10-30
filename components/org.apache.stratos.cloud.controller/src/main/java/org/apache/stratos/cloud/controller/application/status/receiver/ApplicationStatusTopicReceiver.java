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
package org.apache.stratos.cloud.controller.application.status.receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.status.*;
import org.apache.stratos.messaging.listener.application.status.*;
import org.apache.stratos.messaging.message.receiver.application.status.ApplicationStatusEventReceiver;

public class ApplicationStatusTopicReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(ApplicationStatusTopicReceiver.class);

    private ApplicationStatusEventReceiver statusEventReceiver;
    private boolean terminated;

    public ApplicationStatusTopicReceiver() {
        this.statusEventReceiver = new ApplicationStatusEventReceiver();
        addEventListeners();
    }

    public void run() {
        //FIXME this activated before autoscaler deployer activated.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ignore) {
        }
        Thread thread = new Thread(statusEventReceiver);
        thread.start();
        if (log.isInfoEnabled()) {
            log.info("Cloud controller application status thread started");
        }

        // Keep the thread live until terminated
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
        // Listen to topology events that affect clusters
        statusEventReceiver.addEventListener(new AppStatusClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterActivatedEvent((AppStatusClusterActivatedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new AppStatusClusterTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterTerminatedEvent((AppStatusClusterTerminatedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new AppStatusClusterTerminatingEventListener(){
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterTerminatingEvent((AppStatusClusterTerminatingEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new AppStatusClusterInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterInActivateEvent((AppStatusClusterInactivateEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new AppStatusGroupActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupActivatedEvent((AppStatusGroupActivatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusGroupTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupTerminatedEvent((AppStatusGroupTerminatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusGroupTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupTerminatingEvent((AppStatusGroupTerminatingEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusGroupInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupInActiveEvent((AppStatusGroupInactivateEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusApplicationActivatedEventListener() {

            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationActivatedEvent((AppStatusApplicationActivatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusApplicationInactivatedEventListener() {

            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationInActivatedEvent((AppStatusApplicationInactivatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusApplicationCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationCreatedEvent((AppStatusApplicationCreatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusApplicationTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationTerminatingEvent((AppStatusApplicationTerminatingEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new AppStatusApplicationTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationTerminatedEvent((AppStatusApplicationTerminatedEvent) event);

            }
        });


    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }
}
