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
import org.apache.stratos.messaging.domain.Message;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;
import org.apache.stratos.messaging.message.processor.application.ApplicationsMessageProcessorChain;

public class ApplicationsEventMessageDelegator implements Runnable {
    private static final Log log = LogFactory.getLog(ApplicationsEventMessageDelegator.class);

    private ApplicationsEventMessageQueue messageQueue;
    private MessageProcessorChain processorChain;
    private boolean terminated;

    public ApplicationsEventMessageDelegator(ApplicationsEventMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
        this.processorChain = new ApplicationsMessageProcessorChain();
    }

    public void addEventListener(EventListener eventListener) {
        processorChain.addEventListener(eventListener);
    }

    @Override
    public void run() {
        try {
            if (log.isInfoEnabled()) {
                log.info("Application status event message delegator started");
            }

            while (!terminated) {
                try {
                    Message message = messageQueue.take();
                    String type = message.getEventClassName();

                    // Skip application signup events
                    if (!type.startsWith("org.apache.stratos.messaging.event.application.signup")) {

                        // Retrieve the actual message
                        String json = message.getText();

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Application status event message received from queue: %s", type));
                        }

                        // Delegate message to message processor chain
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Delegating application status event message: %s", type));
                        }
                        processorChain.process(type, json, ApplicationManager.getApplications());
                    }
                } catch (InterruptedException ignore) {
                    log.info("Shutting down application event message delegator...");
                    terminate();
                } catch (Exception e) {
                    log.error("Failed to retrieve application status event message", e);
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Application status event message delegator failed", e);
            }
        }
    }

    /**
     * Terminate topology event message delegator thread.
     */
    public void terminate() {
        terminated = true;
    }


    private EventMessage jsonToEventMessage(String json) {

        EventMessage event = new EventMessage();
        String message;

        //split the message to 3 parts using ':' first is class name, second contains the text 'message' and the third contains
        //message
        String[] MessageParts = json.split(":", 3);

        String eventType = MessageParts[0].trim();
        eventType = eventType.substring(eventType.indexOf("\"") + 1, eventType.lastIndexOf("\""));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Extracted [event type] %s", eventType));
        }

        event.setEventName(eventType);
        String messageTag = MessageParts[1];
        messageTag = messageTag.substring(messageTag.indexOf("\"") + 1, messageTag.lastIndexOf("\""));

        if ("message".equals(messageTag)) {
            message = MessageParts[2].trim();
            //Remove trailing bracket twice to get the message
            message = message.substring(0, message.lastIndexOf("}")).trim();
            message = message.substring(0, message.lastIndexOf("}")).trim();
            if (message.indexOf('{') == 0 && message.indexOf('}') == message.length() - 1) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("[Extracted message] %s ", message));
                }
                event.setMessage(message);
                return event;
            }
        }
        return null;
    }

    private class EventMessage {
        private String eventName;
        private String message;

        private String getEventName() {
            return eventName;
        }

        private void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
