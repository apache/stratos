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
package org.apache.stratos.manager.messaging.receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.components.ApplicationSignUpHandler;
import org.apache.stratos.manager.messaging.publisher.ApplicationSignUpEventPublisher;
import org.apache.stratos.manager.messaging.publisher.synchronizer.TenantEventSynchronizer;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.initializer.CompleteApplicationSignUpsRequestEventListener;
import org.apache.stratos.messaging.listener.initializer.CompleteTenantRequestEventListener;
import org.apache.stratos.messaging.message.receiver.initializer.InitializerEventReceiver;

import java.util.concurrent.ExecutorService;

public class StratosManagerInitializerTopicReceiver {
    private static final Log log = LogFactory.getLog(StratosManagerInitializerTopicReceiver.class);
    private InitializerEventReceiver initializerEventReceiver;
    //private ExecutorService executorService;
    private ApplicationSignUpHandler applicationSignUpHandler;

    public StratosManagerInitializerTopicReceiver() {
        this.initializerEventReceiver = InitializerEventReceiver.getInstance();
        applicationSignUpHandler = new ApplicationSignUpHandler();
        addEventListeners();
    }

//    public void execute() {
//        initializerEventReceiver.setExecutorService(executorService);
//        initializerEventReceiver.execute();
//        if (log.isInfoEnabled()) {
//            log.info("Stratos manager initializer topic receiver started");
//        }
//    }

    private void addEventListeners() {
        initializerEventReceiver.addEventListener(new CompleteTenantRequestEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (log.isDebugEnabled()) {
                    log.debug("Handling CompleteTenantRequestEvent");
                }
                try {
                    TenantEventSynchronizer.sendCompleteTenantEvent();
                } catch (Exception e) {
                    log.error("Failed to process CompleteTenantRequestEvent", e);
                }
            }
        });

        initializerEventReceiver.addEventListener(new CompleteApplicationSignUpsRequestEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (log.isDebugEnabled()) {
                    log.debug("Handling CompleteApplicationSignUpsRequestEvent");
                }
                try {
                    ApplicationSignUpEventPublisher
                            .publishCompleteApplicationSignUpsEvent(applicationSignUpHandler.getApplicationSignUps());
                } catch (Exception e) {
                    log.error("Failed to process CompleteApplicationSignUpsRequestEvent", e);
                }
            }
        });
    }

//    public ExecutorService getExecutorService() {
//        return executorService;
//    }
//
//    public void setExecutorService(ExecutorService executorService) {
//        this.executorService = executorService;
//    }
}
