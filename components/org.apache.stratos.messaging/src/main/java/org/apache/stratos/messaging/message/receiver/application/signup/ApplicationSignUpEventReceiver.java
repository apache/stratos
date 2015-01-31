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
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.concurrent.ExecutorService;

/**
 * Application signup event receiver.
 */
public class ApplicationSignUpEventReceiver {

    private static final Log log = LogFactory.getLog(ApplicationSignUpEventReceiver.class);

    private ApplicationSignUpEventMessageDelegator messageDelegator;
    private ApplicationSignUpEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private ExecutorService executorService;

    public ApplicationSignUpEventReceiver() {
        ApplicationSignUpEventMessageQueue messageQueue = new ApplicationSignUpEventMessageQueue();
        this.messageDelegator = new ApplicationSignUpEventMessageDelegator(messageQueue);
        this.messageListener = new ApplicationSignUpEventMessageListener(messageQueue);
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }


    public void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.APPLICATION_SIGNUP_TOPIC.getTopicName(), messageListener);
            // subscriber.setMessageListener(messageListener);
            executorService.execute(eventSubscriber);


            if (log.isDebugEnabled()) {
                log.debug("Application signup event message receiver thread started");
            }

            // Start topology event message delegator thread
            executorService.execute(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("Application signup event message delegator thread started");
            }


        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Application signup receiver failed", e);
            }
        }
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
