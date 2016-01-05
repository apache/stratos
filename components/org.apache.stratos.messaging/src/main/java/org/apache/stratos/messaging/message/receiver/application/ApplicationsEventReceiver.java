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
package org.apache.stratos.messaging.message.receiver.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.event.initializer.CompleteApplicationsRequestEvent;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

public class ApplicationsEventReceiver extends StratosEventReceiver{
    private static final Log log = LogFactory.getLog(ApplicationsEventReceiver.class);

    private ApplicationsEventMessageDelegator messageDelegator;
    private ApplicationsEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile ApplicationsEventReceiver instance;

    private ApplicationsEventReceiver() {
        ApplicationsEventMessageQueue messageQueue = new ApplicationsEventMessageQueue();
        this.messageDelegator = new ApplicationsEventMessageDelegator(messageQueue);
        this.messageListener = new ApplicationsEventMessageListener(messageQueue);
        execute();
    }

    public static ApplicationsEventReceiver getInstance () {
        if (instance == null) {
            synchronized (ApplicationsEventReceiver.class) {
                if (instance == null) {
                    instance = new ApplicationsEventReceiver();
                }
            }
        }

        return instance;
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }

    public void removeEventListener(EventListener eventListener) {
        messageDelegator.removeEventListener(eventListener);
    }

    private void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.APPLICATION_TOPIC.getTopicName(),
                    messageListener);
            executor.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Application status event message receiver thread started");
            }

            // Start Application status event message delegator thread
            executor.execute(messageDelegator);

            if (log.isDebugEnabled()) {
                log.debug("Application status event message delegator thread started");
            }
            initializeCompleteApplicationsModel();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Application status failed", e);
            }
        }
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
    }

    public void initializeCompleteApplicationsModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (!eventSubscriber.isSubscribed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                }

                CompleteApplicationsRequestEvent completeApplicationsRequestEvent
                        = new CompleteApplicationsRequestEvent();
                String topic = MessagingUtil.getMessageTopicName(completeApplicationsRequestEvent);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
                eventPublisher.publish(completeApplicationsRequestEvent);
            }
        });
    }
}
