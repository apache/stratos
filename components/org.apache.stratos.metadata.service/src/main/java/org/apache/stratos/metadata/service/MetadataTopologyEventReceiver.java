/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadata.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.ApplicationClustersCreatedEvent;
import org.apache.stratos.messaging.event.topology.ApplicationClustersRemovedEvent;
import org.apache.stratos.messaging.listener.topology.ApplicationClustersCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ApplicationClustersRemovedEventListener;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.metadata.service.registry.MetadataApiRegistry;

import java.util.concurrent.ExecutorService;

/**
 * Topology receiver class for metadata service
 */
public class MetadataTopologyEventReceiver {
    private static final Log log = LogFactory.getLog(MetadataTopologyEventReceiver.class);
    private TopologyEventReceiver topologyEventReceiver;
    //private ExecutorService executorService;

    public MetadataTopologyEventReceiver() {
        this.topologyEventReceiver = TopologyEventReceiver.getInstance();
//        //executor = StratosThreadPool.getExecutorService(Constants
//                .METADATA_SERVICE_THREAD_POOL_ID, 20);
        addEventListeners();
    }

    private void addEventListeners() {
        topologyEventReceiver.addEventListener(new ApplicationClustersCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ApplicationClustersCreatedEvent appClustersCreatedEvent = (ApplicationClustersCreatedEvent) event;
                String applicationId = appClustersCreatedEvent.getAppId();
                MetadataApiRegistry.getApplicationIdToReadWriteLockMap().put(applicationId,
                        new ReadWriteLock(Constants.METADATA_SERVICE_THREAD_POOL_ID.concat(applicationId)));
            }
        });

        topologyEventReceiver.addEventListener(new ApplicationClustersRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ApplicationClustersRemovedEvent appClustersRemovedEvent = (ApplicationClustersRemovedEvent) event;
                String applicationId = appClustersRemovedEvent.getAppId();
                MetadataApiRegistry.getApplicationIdToReadWriteLockMap().remove(applicationId);
            }
        });
    }

//    public void execute() {
//        topologyEventReceiver.setExecutorService(getExecutorService());
//        topologyEventReceiver.execute();
//
//        if (log.isInfoEnabled()) {
//            log.info("Metadata service topology receiver started.");
//        }
//    }
//
//    public void terminate() {
//        topologyEventReceiver.terminate();
//        if (log.isInfoEnabled()) {
//            log.info("Metadata service topology receiver stopped.");
//        }
//    }

//    public ExecutorService getExecutorService() {
//        return executorService;
//    }
}
