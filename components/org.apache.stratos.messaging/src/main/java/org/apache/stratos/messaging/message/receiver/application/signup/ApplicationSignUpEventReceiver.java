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

package org.apache.stratos.messaging.message.receiver.application.signup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.event.initializer.CompleteApplicationSignUpsRequestEvent;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Application signup event receiver.
 */
public class ApplicationSignUpEventReceiver extends StratosEventReceiver {

    private static final Log log = LogFactory.getLog(ApplicationSignUpEventReceiver.class);

    private ApplicationSignUpEventMessageDelegator messageDelegator;
    private ApplicationSignUpEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile ApplicationSignUpEventReceiver instance;

    private ApplicationSignUpEventReceiver() {
        ApplicationSignUpEventMessageQueue messageQueue = new ApplicationSignUpEventMessageQueue();
        this.messageDelegator = new ApplicationSignUpEventMessageDelegator(messageQueue);
        this.messageListener = new ApplicationSignUpEventMessageListener(messageQueue);
        execute();
    }

    public static ApplicationSignUpEventReceiver getInstance () {
        if (instance == null) {
            synchronized (ApplicationSignUpEventReceiver.class) {
                if (instance == null) {
                    instance = new ApplicationSignUpEventReceiver();
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
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.APPLICATION_SIGNUP_TOPIC.getTopicName(),
                    messageListener);
            // subscriber.setMessageListener(messageListener);
            executor.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Application signup event message receiver thread started");
            }

            // Start topology event message delegator thread
            executor.execute(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("Application signup event message delegator thread started");
            }

            initializeCompleteApplicationSignUps();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Application signup receiver failed", e);
            }
        }
    }

    public void initializeCompleteApplicationSignUps() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (!eventSubscriber.isSubscribed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                }

                CompleteApplicationSignUpsRequestEvent completeApplicationSignUpsRequestEvent
                        = new CompleteApplicationSignUpsRequestEvent();
                String topic = MessagingUtil.getMessageTopicName(completeApplicationSignUpsRequestEvent);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
                eventPublisher.publish(completeApplicationSignUpsRequestEvent);
            }
        });
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
    }
}
