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
import org.apache.stratos.messaging.domain.Message;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;
import org.apache.stratos.messaging.message.processor.application.signup.ApplicationSignUpMessageProcessorChain;

/**
 * Application signup event message delegator.
 */
class ApplicationSignUpEventMessageDelegator implements Runnable {

    private static final Log log = LogFactory.getLog(ApplicationSignUpEventMessageDelegator.class);

    private MessageProcessorChain processorChain;
    private ApplicationSignUpEventMessageQueue messageQueue;
    private boolean terminated;

    public ApplicationSignUpEventMessageDelegator(ApplicationSignUpEventMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
        this.processorChain = new ApplicationSignUpMessageProcessorChain();
    }

    public void addEventListener(EventListener eventListener) {
        processorChain.addEventListener(eventListener);
    }

    @Override
    public void run() {
        try {
            if (log.isInfoEnabled()) {
                log.info("Application signup event message delegator started");
            }

            while (!terminated) {
                try {
                    Message message = messageQueue.take();
                    String type = message.getEventClassName();

                    // Retrieve the actual message
                    String json = message.getText();

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Application signup event message received from queue: [event-class-name] %s " +
                                "[message-queue] %s", type, messageQueue.getClass()));
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Delegating application signup event message: %s", type));
                    }
                    processorChain.process(type, json, ApplicationSignUpManager.getInstance());
                } catch (InterruptedException e) {
                    log.info("Application signup event message delegator is shutting down...");
                    return;
                } catch (Exception e) {
                    log.error("Failed to retrieve application signup event message", e);
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Application signup event message delegator failed", e);
            }
        }
    }

    /**
     * Terminate application signup event message delegator thread.
     */
    public void terminate() {
        terminated = true;
    }
}
