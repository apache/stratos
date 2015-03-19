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

package org.apache.stratos.messaging.message.processor.application.signup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.messaging.event.application.signup.CompleteApplicationSignUpsEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpManager;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Complete application signups message processor.
 */
public class CompleteApplicationSignUpsMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompleteApplicationSignUpsMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        if (type.equals(CompleteApplicationSignUpsEvent.class.getName())) {
            CompleteApplicationSignUpsEvent event = (CompleteApplicationSignUpsEvent) MessagingUtil.jsonToObject(message,
                    CompleteApplicationSignUpsEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to CompleteApplicationSignUpsEvent");
                return false;
            }

            if (!ApplicationSignUpManager.getInstance().isInitialized()) {
                try {
                    ApplicationSignUpManager.acquireWriteLock();
                    for (ApplicationSignUp applicationSignUp : event.getApplicationSignUps()) {
                        ApplicationSignUpManager.getInstance().addApplicationSignUp(applicationSignUp);
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Application signup added: [application-id] %s [tenant-id] %s",
                                    applicationSignUp.getApplicationId(), applicationSignUp.getTenantId()));
                        }
                    }
                    ApplicationSignUpManager.getInstance().setInitialized(true);
                } finally {
                    ApplicationSignUpManager.releaseWriteLock();
                }
            }

            notifyEventListeners(event);
            return true;
        } else if (nextProcessor != null) {
            // ask the next processor to take care of the message.
            return nextProcessor.process(type, message, object);
        } else {
            throw new RuntimeException(String.format("Failed to process message using available message processors: " +
                    "[type] %s [body] %s", type, message));
        }
    }
}
