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
package org.apache.stratos.messaging.message.receiver.initializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.concurrent.ExecutorService;

public class InitializerEventReceiver extends StratosEventReceiver {
    private static final Log log = LogFactory.getLog(InitializerEventReceiver.class);

    private InitializerEventMessageDelegator messageDelegator;
    private InitializerEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile InitializerEventReceiver instance;
    //private ExecutorService executorService;

    private InitializerEventReceiver() {
        // TODO: make pool size configurable
        this.executorService = StratosThreadPool.getExecutorService("initializer-event-receiver", 100);
        InitializerEventMessageQueue initializerEventMessageQueue = new InitializerEventMessageQueue();
        this.messageDelegator = new InitializerEventMessageDelegator(initializerEventMessageQueue);
        this.messageListener = new InitializerEventMessageListener(initializerEventMessageQueue);
        execute();
    }

    public static InitializerEventReceiver getInstance () {
        if (instance == null) {
            synchronized (InitializerEventReceiver.class) {
                if (instance == null) {
                    instance = new InitializerEventReceiver();
                }
            }
        }

        return instance;
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }

    private void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.INITIALIZER_TOPIC.getTopicName(),
                    messageListener);
            executorService.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Initializer event message delegator thread started");
            }
            // Start initializer event message delegator thread
            executorService.execute(messageDelegator);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Initializer receiver failed", e);
            }
        }
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
    }

//    public ExecutorService getExecutorService() {
//        return executorService;
//    }
//
//    public void setExecutorService(ExecutorService executorService) {
//        this.executorService = executorService;
//    }
}
