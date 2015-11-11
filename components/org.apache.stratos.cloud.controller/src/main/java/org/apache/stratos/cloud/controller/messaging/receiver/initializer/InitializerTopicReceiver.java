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
package org.apache.stratos.cloud.controller.messaging.receiver.initializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.messaging.publisher.TopologyEventPublisher;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyHolder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.initializer.CompleteTopologyRequestEventListener;
import org.apache.stratos.messaging.message.receiver.initializer.InitializerEventReceiver;

import java.util.concurrent.ExecutorService;

public class InitializerTopicReceiver {
    private static final Log log = LogFactory.getLog(InitializerTopicReceiver.class);
    private InitializerEventReceiver initializerEventReceiver;
    private ExecutorService executorService;

    public InitializerTopicReceiver() {
        this.initializerEventReceiver = new InitializerEventReceiver();
        addEventListeners();
    }

    public void execute() {
        initializerEventReceiver.setExecutorService(executorService);
        initializerEventReceiver.execute();
        if (log.isInfoEnabled()) {
            log.info("Cloud controller initializer topic receiver started");
        }
    }

    private void addEventListeners() {
        initializerEventReceiver.addEventListener(new CompleteTopologyRequestEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (log.isDebugEnabled()) {
                    log.debug("Handling CompleteTopologyRequestEvent");
                }
                try {
                    TopologyEventPublisher.sendCompleteTopologyEvent(TopologyHolder.getTopology());
                } catch (Exception e) {
                    log.error("Failed to process CompleteTopologyRequestEvent", e);
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
