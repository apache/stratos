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
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpAddedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpManager;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Application signup added message processor.
 */
public class ApplicationSignUpAddedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationSignUpAddedMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        if (type.equals(ApplicationSignUpAddedEvent.class.getName())) {
            ApplicationSignUpAddedEvent event = (ApplicationSignUpAddedEvent) MessagingUtil.jsonToObject(message,
                    ApplicationSignUpAddedEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to ApplicationSignUpAddedEvent");
                return false;
            }
            if (event.getClusterIds() == null) {
                log.error(String.format("Cluster ids not found in application signup added event: " +
                        "[application] %s [tenant] %d", event.getApplicationId(), event.getTenantId()));
            }

            try {
                ApplicationSignUpManager.acquireWriteLock();

                ApplicationSignUp applicationSignUp = new ApplicationSignUp();
                applicationSignUp.setApplicationId(event.getApplicationId());
                applicationSignUp.setTenantId(event.getTenantId());
                applicationSignUp.setClusterIds(event.getClusterIds().toArray(new String[event.getClusterIds().size()]));

                ApplicationSignUpManager.getInstance().addApplicationSignUp(applicationSignUp);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Application signup added: [application-id] %s [tenant-id] %s",
                            applicationSignUp.getApplicationId(), applicationSignUp.getTenantId()));
                }
            } finally {
                ApplicationSignUpManager.releaseWriteLock();
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
