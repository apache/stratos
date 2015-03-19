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
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpRemovedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpManager;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Application signup removed message processor.
 */
public class ApplicationSignUpRemovedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationSignUpRemovedMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        if (type.equals(ApplicationSignUpRemovedEvent.class.getName())) {
            ApplicationSignUpRemovedEvent event = (ApplicationSignUpRemovedEvent) MessagingUtil.jsonToObject(message,
                    ApplicationSignUpRemovedEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to ApplicationSignUpRemovedEvent");
                return false;
            }

            try {
                ApplicationSignUpManager.acquireWriteLock();
                String applicationId = event.getApplicationId();
                int tenantId = event.getTenantId();

                ApplicationSignUp applicationSignUp = ApplicationSignUpManager.getInstance().getApplicationSignUp(
                        applicationId, tenantId);
                if (applicationSignUp == null) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Application signup not found: [application-id] %s [tenant-id] %d",
                                applicationId, tenantId));
                    }
                    return false;
                }

                ApplicationSignUpManager.getInstance().removeApplicationSignUp(applicationId, tenantId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Application signup removed: [application-id] %s [tenant-id] %d",
                            applicationSignUp.getApplicationId(), applicationSignUp.getTenantId()));
                }
            } finally {
                ApplicationSignUpManager.releaseWriteLock();
            }

            notifyEventListeners(event);
            return true;
        } else if (nextProcessor != null) {
            return nextProcessor.process(type, message, object);
        } else {
            throw new RuntimeException(String.format("Failed to process message using available message processors: " +
                    "[type] %s [body] %s", type, message));
        }
    }
}
