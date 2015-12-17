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

package org.apache.stratos.messaging.message.receiver.instance.notifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * A thread for receiving instance notifier information from message broker.
 */
public class InstanceNotifierEventReceiver extends StratosEventReceiver {
    private static final Log log = LogFactory.getLog(InstanceNotifierEventReceiver.class);
    private final InstanceNotifierEventMessageDelegator messageDelegator;
    private EventSubscriber eventSubscriber;
    private InstanceNotifierEventMessageListener messageListener;
    private static volatile InstanceNotifierEventReceiver instance;
    //private boolean terminated;

    private InstanceNotifierEventReceiver() {
        // TODO: make pool size configurable
        this.executorService = StratosThreadPool.getExecutorService("topology-event-receiver", 100);
        InstanceNotifierEventMessageQueue messageQueue = new InstanceNotifierEventMessageQueue();
        this.messageDelegator = new InstanceNotifierEventMessageDelegator(messageQueue);
        this.messageListener = new InstanceNotifierEventMessageListener(messageQueue);
        execute();
    }

    public static InstanceNotifierEventReceiver getInstance () {
        if (instance == null) {
            synchronized (InstanceNotifierEventReceiver.class) {
                if (instance == null) {
                    instance = new InstanceNotifierEventReceiver();
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
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.INSTANCE_NOTIFIER_TOPIC.getTopicName(),
                    messageListener);
            executorService.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Instance Notifier event message receiver thread started");
            }

            // Start topology event message delegator thread
            executorService.execute(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("Instance Notifier  event message delegator thread started");
            }

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Instance Notifier receiver failed", e);
            }
        }
    }

    public boolean isSubscribed() {
        return ((eventSubscriber != null) && (eventSubscriber.isSubscribed()));
    }

    public synchronized void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
        //terminated = true;
        log.info("InstanceNotifierEventReceiver terminated");
    }
}
