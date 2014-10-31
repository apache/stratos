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
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.listener.cluster.status.*;
import org.apache.stratos.messaging.message.receiver.applications.ApplicationsEventReceiver;

public class ApplicationStatusTopicReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(ApplicationStatusTopicReceiver.class);

    private ApplicationsEventReceiver statusEventReceiver;
    private boolean terminated;

    public ApplicationStatusTopicReceiver() {
        this.statusEventReceiver = new ApplicationsEventReceiver();
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
        statusEventReceiver.addEventListener(new ClusterStatusClusterResetEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterCreated((ClusterStatusClusterResettedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new ClusterStatusClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterActivatedEvent((ClusterStatusClusterActivatedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new ClusterStatusClusterTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterTerminatedEvent((ClusterStatusClusterTerminatedEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new ClusterStatusClusterTerminatingEventListener(){
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterTerminatingEvent((ClusterStatusClusterTerminatingEvent) event);
            }
        });

        statusEventReceiver.addEventListener(new ClusterStatusClusterInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                TopologyBuilder.handleClusterInActivateEvent((ClusterStatusClusterInactivateEvent) event);
            }
        });
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }
}
