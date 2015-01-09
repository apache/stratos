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
import org.apache.stratos.messaging.broker.subscribe.MessageListener;
import org.apache.stratos.messaging.domain.Message;

/**
 * Application signup event message listener.
 */
class ApplicationSignUpEventMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(ApplicationSignUpEventMessageListener.class);

    private final ApplicationSignUpEventMessageQueue messageQueue;

    public ApplicationSignUpEventMessageListener(ApplicationSignUpEventMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void messageReceived(Message message) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Application signup message received: %s", message.getText()));
            }
            // Add received message to the queue
            messageQueue.add(message);

        } catch (Exception e) {
            String error = "An error occurred while receiving application signup event message";
            log.error(error, e);
        }
    }
}
