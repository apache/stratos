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
package org.apache.stratos.cloud.controller.messaging.receiver.cluster.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.listener.cluster.status.*;
import org.apache.stratos.messaging.message.receiver.cluster.status.ClusterStatusEventReceiver;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.concurrent.ExecutorService;

public class ClusterStatusTopicReceiver {
    private static final Log log = LogFactory.getLog(ClusterStatusTopicReceiver.class);

    private ClusterStatusEventReceiver clusterStatusEventReceiver;
    //private boolean terminated;
    //private ExecutorService executorService;

    public ClusterStatusTopicReceiver() {
        this.clusterStatusEventReceiver = ClusterStatusEventReceiver.getInstance();
        addEventListeners();
    }

//    public void execute() {
//        clusterStatusEventReceiver.setExecutorService(executorService);
//        clusterStatusEventReceiver.execute();
//        if (log.isInfoEnabled()) {
//            log.info("Cloud controller Cluster status thread started");
//        }
//
//    }

    private void addEventListeners() {
        // Listen to topology events that affect clusters
        clusterStatusEventReceiver.addEventListener(new ClusterStatusClusterResetEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleClusterReset((ClusterStatusClusterResetEvent) event);
                } catch (RegistryException e) {
                    log.error("Failed to process cluster status reset event", e);
                }
            }
        });

        clusterStatusEventReceiver.addEventListener(new ClusterStatusClusterInstanceCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                //TopologyBuilder.handleClusterInstanceCreated((ClusterStatusClusterInstanceCreatedEvent) event);
            }
        });

        clusterStatusEventReceiver.addEventListener(new ClusterStatusClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleClusterActivatedEvent((ClusterStatusClusterActivatedEvent) event);
                } catch (RegistryException e) {
                    log.error("Failed to process cluster activated event", e);
                }
            }
        });

        clusterStatusEventReceiver.addEventListener(new ClusterStatusClusterTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleClusterTerminatedEvent((ClusterStatusClusterTerminatedEvent) event);
                } catch (RegistryException e) {
                    log.error("Failed to process cluster termination event", e);
                }
            }
        });

        clusterStatusEventReceiver.addEventListener(new ClusterStatusClusterTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleClusterTerminatingEvent((ClusterStatusClusterTerminatingEvent) event);
                } catch (RegistryException e) {
                    log.error("Failed to process cluster termination event", e);
                }
            }
        });

        clusterStatusEventReceiver.addEventListener(new ClusterStatusClusterInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    TopologyBuilder.handleClusterInactivateEvent((ClusterStatusClusterInactivateEvent) event);
                } catch (RegistryException e) {
                    log.error("Failed to process cluster inactive event", e);
                }
            }
        });
    }

//    public void setTerminated(boolean terminated) {
//        this.terminated = terminated;
//    }
//
//    public ExecutorService getExecutorService() {
//        return executorService;
//    }
//
//    public void setExecutorService(ExecutorService executorService) {
//        this.executorService = executorService;
//    }
}
