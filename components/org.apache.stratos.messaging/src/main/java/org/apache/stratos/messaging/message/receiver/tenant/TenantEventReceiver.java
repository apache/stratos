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

package org.apache.stratos.messaging.message.receiver.tenant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.event.initializer.CompleteTenantRequestEvent;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * A thread for receiving tenant information from message broker and
 * build tenant information in tenant manager.
 */
public class TenantEventReceiver extends StratosEventReceiver {
    private static final Log log = LogFactory.getLog(TenantEventReceiver.class);
    private TenantEventMessageDelegator messageDelegator;
    private TenantEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile TenantEventReceiver instance;

    private TenantEventReceiver() {
        // TODO: make pool size configurable
        this.executor = StratosThreadPool.getExecutorService("tenant-event-receiver", 35, 100);
        TenantEventMessageQueue messageQueue = new TenantEventMessageQueue();
        this.messageDelegator = new TenantEventMessageDelegator(messageQueue);
        this.messageListener = new TenantEventMessageListener(messageQueue);
        execute();
    }

    public static TenantEventReceiver getInstance () {
        if (instance == null) {
            synchronized (TenantEventReceiver.class) {
                if (instance == null) {
                    instance = new TenantEventReceiver();
                }
            }
        }

        return instance;
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }

//    public void setExecutorService(ExecutorService executor) {
//        this.executor = executor;
//    }

    private void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.TENANT_TOPIC.getTopicName(), messageListener);
            executor.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Tenant event message receiver thread started");
            }

            // Start tenant event message delegator thread
            executor.execute(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("Tenant event message delegator thread started");
            }

            initializeCompleteTenant();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Tenant receiver failed", e);
            }
        }
    }

    public void initializeCompleteTenant() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (!eventSubscriber.isSubscribed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                }

                CompleteTenantRequestEvent completeTenantRequestEvent = new CompleteTenantRequestEvent();
                String topic = MessagingUtil.getMessageTopicName(completeTenantRequestEvent);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
                eventPublisher.publish(completeTenantRequestEvent);
            }
        });
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
    }
}
