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
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.application.signup.ApplicationSignUpAddedEventListener;
import org.apache.stratos.messaging.listener.application.signup.ApplicationSignUpRemovedEventListener;
import org.apache.stratos.messaging.listener.application.signup.CompleteApplicationSignUpsEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingAddedEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingRemovedEventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Application signup message processor chain.
 */
public class ApplicationSignUpMessageProcessorChain extends MessageProcessorChain {

    private static final Log log = LogFactory.getLog(ApplicationSignUpMessageProcessorChain.class);

    private CompleteApplicationSignUpsMessageProcessor completeApplicationSignUpsMessageProcessor;
    private ApplicationSignUpAddedMessageProcessor applicationSignUpAddedMessageProcessor;
    private ApplicationSignUpRemovedMessageProcessor applicationSignUpRemovedMessageProcessor;

    @Override
    protected void initialize() {
        completeApplicationSignUpsMessageProcessor = new CompleteApplicationSignUpsMessageProcessor();
        add(completeApplicationSignUpsMessageProcessor);

        applicationSignUpAddedMessageProcessor = new ApplicationSignUpAddedMessageProcessor();
        add(applicationSignUpAddedMessageProcessor);

        applicationSignUpRemovedMessageProcessor = new ApplicationSignUpRemovedMessageProcessor();
        add(applicationSignUpRemovedMessageProcessor);
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListener instanceof CompleteApplicationSignUpsEventListener) {
            completeApplicationSignUpsMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationSignUpAddedEventListener) {
            applicationSignUpAddedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationSignUpRemovedEventListener) {
            applicationSignUpRemovedMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
