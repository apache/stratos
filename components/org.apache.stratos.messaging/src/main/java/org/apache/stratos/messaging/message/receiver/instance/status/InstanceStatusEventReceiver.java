/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.receiver.instance.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.concurrent.ExecutorService;

/**
 * A thread for receiving instance notifier information from message broker.
 */
public class InstanceStatusEventReceiver {
    private static final Log log = LogFactory.getLog(InstanceStatusEventReceiver.class);
    private final InstanceStatusEventMessageDelegator messageDelegator;
    private final InstanceStatusEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private boolean terminated;
    private ExecutorService executorService;

    public InstanceStatusEventReceiver() {
        InstanceStatusEventMessageQueue messageQueue = new InstanceStatusEventMessageQueue();
        this.messageDelegator = new InstanceStatusEventMessageDelegator(messageQueue);
        this.messageListener = new InstanceStatusEventMessageListener(messageQueue);
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }


    public void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.INSTANCE_STATUS_TOPIC.getTopicName(), messageListener);
            executorService.submit(eventSubscriber);
            if (log.isDebugEnabled()) {
                log.debug("InstanceNotifier event message receiver thread started");
            }

            // Start instance notifier event message delegate thread
            executorService.submit(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("InstanceNotifier event message delegator thread started");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("InstanceNotifier receiver failed", e);
            }
        }
    }

    public boolean isSubscribed() {
        return ((eventSubscriber != null) && (eventSubscriber.isSubscribed()));
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
        terminated = true;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
