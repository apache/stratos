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
        statusEventReceiver.addEventListener(new ClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterActivatedEvent((ClusterActivatedEvent) event);
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

        statusEventReceiver.addEventListener(new ClusterInActivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterInActivateEvent((ClusterInActivateEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new GroupActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupActivatedEvent((GroupActivatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new GroupTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupTerminatedEvent((GroupInTerminatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new GroupTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleGroupTerminatingEvent((GroupInTerminatingEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new ApplicationActivatedEventListener() {

            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationActivatedEvent((ApplicationActivatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new ApplicationInActivatedEventListener() {

            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationInActivatedEvent((ApplicationInactivatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new ApplicationCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationCreatedEvent((ApplicationCreatedEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new ApplicationTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationTerminatingEvent((ApplicationTerminatingEvent) event);

            }
        });

        statusEventReceiver.addEventListener(new ApplicationTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleApplicationTerminatedEvent((ApplicationTerminatedEvent) event);

            }
        });


    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }
}
