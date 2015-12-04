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

package org.apache.stratos.messaging.message.receiver.health.stat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * A thread for receiving health stat information from message broker
 */
public class HealthStatEventReceiver extends StratosEventReceiver {
    private static final Log log = LogFactory.getLog(HealthStatEventReceiver.class);

    private final HealthStatEventMessageDelegator messageDelegator;
    private final HealthStatEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile HealthStatEventReceiver instance;

    private HealthStatEventReceiver() {
        // TODO: make pool size configurable
        this.executor = StratosThreadPool.getExecutorService("healthstat-event-receiver", 35, 100);
        HealthStatEventMessageQueue messageQueue = new HealthStatEventMessageQueue();
        this.messageDelegator = new HealthStatEventMessageDelegator(messageQueue);
        this.messageListener = new HealthStatEventMessageListener(messageQueue);
        execute();
    }

    public static HealthStatEventReceiver getInstance () {
        if (instance == null) {
            synchronized (HealthStatEventReceiver.class) {
                if (instance == null) {
                    instance = new HealthStatEventReceiver();
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
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.HEALTH_STAT_TOPIC.getTopicName(), messageListener);

            executor.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Health stats event message delegator thread started");
            }
            // Start health stat event message delegator thread
            executor.execute(messageDelegator);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Health stats receiver failed", e);
            }
        }
    }

//    public void terminate() {
//        eventSubscriber.terminate();
//        messageDelegator.terminate();
//        terminated = true;
//    }
//
//    public ExecutorService getExecutorService() {
//        return executor;
//    }
//
//    public void setExecutorService(ExecutorService executor) {
//        this.executor = executor;
//    }
}
