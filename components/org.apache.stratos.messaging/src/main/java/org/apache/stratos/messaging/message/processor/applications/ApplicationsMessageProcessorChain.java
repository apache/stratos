/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.message.processor.applications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.applications.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Application Status processor chain is to handle the list processors to parse the application
 * status.
 */
public class ApplicationsMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(ApplicationsMessageProcessorChain.class);

    private GroupResetProcessor groupCreatedMessageProcessor;
    private GroupActivatedProcessor groupActivatedMessageProcessor;
    private GroupInActivateProcessor groupInActivateMessageProcessor;
    private GroupTerminatedProcessor groupTerminatedProcessor;
    private GroupTerminatingProcessor groupTerminatingProcessor;
    private ApplicationActivatedMessageProcessor applicationActivatedMessageProcessor;
    private ApplicationCreatedMessageProcessor applicationCreatedMessageProcessor;
    private ApplicationInactivatedMessageProcessor applicationInactivatedMessageProcessor;
    private ApplicationTerminatedMessageProcessor applicationTerminatedMessageProcessor;
    private ApplicationTerminatingMessageProcessor applicationTerminatingMessageProcessor;
    private CompleteApplicationsMessageProcessor completeApplicationsMessageProcessor;

    public void initialize() {
        // Add instance notifier event processors

        groupCreatedMessageProcessor = new GroupResetProcessor();
        add(groupCreatedMessageProcessor);

        groupActivatedMessageProcessor = new GroupActivatedProcessor();
        add(groupActivatedMessageProcessor);

        groupInActivateMessageProcessor = new GroupInActivateProcessor();
        add(groupInActivateMessageProcessor);

        groupTerminatedProcessor = new GroupTerminatedProcessor();
        add(groupTerminatedProcessor);

        groupTerminatingProcessor = new GroupTerminatingProcessor();
        add(groupTerminatingProcessor);

        applicationActivatedMessageProcessor = new ApplicationActivatedMessageProcessor();
        add(applicationActivatedMessageProcessor);

        applicationCreatedMessageProcessor = new ApplicationCreatedMessageProcessor();
        add(applicationCreatedMessageProcessor);

        applicationInactivatedMessageProcessor = new ApplicationInactivatedMessageProcessor();
        add(applicationInactivatedMessageProcessor);

        applicationTerminatingMessageProcessor = new ApplicationTerminatingMessageProcessor();
        add(applicationTerminatingMessageProcessor);

        completeApplicationsMessageProcessor = new CompleteApplicationsMessageProcessor();
        add(completeApplicationsMessageProcessor);

        applicationTerminatedMessageProcessor = new ApplicationTerminatedMessageProcessor();
        add(applicationTerminatedMessageProcessor);

        if (log.isDebugEnabled()) {
            log.debug("Instance notifier message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {

        if (eventListener instanceof GroupResetEventListener) {
            groupCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupInactivateEventListener) {
            groupInActivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupActivatedEventListener) {
            groupActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupTerminatingEventListener) {
            groupTerminatingProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupTerminatedEventListener) {
            groupTerminatedProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationCreatedEventListener) {
            applicationCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationActivatedEventListener) {
            applicationActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationInactivatedEventListener) {
            applicationInactivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationTerminatingEventListener) {
            applicationTerminatingMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationTerminatedEventListener) {
            applicationTerminatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof CompleteApplicationsEventListener) {
            completeApplicationsMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener " + eventListener.toString());
        }
    }
}
