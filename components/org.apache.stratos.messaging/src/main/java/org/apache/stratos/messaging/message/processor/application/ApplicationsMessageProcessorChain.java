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
package org.apache.stratos.messaging.message.processor.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.application.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Application Status processor chain is to handle the list processors to parse the application
 * status.
 */
public class ApplicationsMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(ApplicationsMessageProcessorChain.class);

    private GroupInstanceCreatedProcessor groupCreatedMessageProcessor;
    private GroupInstanceActivatedProcessor groupActivatedMessageProcessor;
    private GroupInstanceInactivateProcessor groupInactivateMessageProcessor;
    private GroupInstanceTerminatedProcessor groupTerminatedProcessor;
    private GroupInstanceTerminatingProcessor groupTerminatingProcessor;
    private ApplicationInstanceCreatedMessageProcessor applicationInstanceCreatedMessageProcessor;
    private ApplicationInstanceActivatedMessageProcessor applicationActivatedMessageProcessor;
    private ApplicationCreatedMessageProcessor applicationCreatedMessageProcessor;
    private ApplicationDeletedMessageProcessor applicationDeletedMessageProcessor;
    private ApplicationInstanceInactivatedMessageProcessor applicationInactivatedMessageProcessor;
    private ApplicationInstanceTerminatedMessageProcessor applicationTerminatedMessageProcessor;
    private ApplicationInstanceTerminatingMessageProcessor applicationTerminatingMessageProcessor;
    private CompleteApplicationsMessageProcessor completeApplicationsMessageProcessor;

    public void initialize() {
        // Add instance notifier event processors

        groupCreatedMessageProcessor = new GroupInstanceCreatedProcessor();
        add(groupCreatedMessageProcessor);

        groupActivatedMessageProcessor = new GroupInstanceActivatedProcessor();
        add(groupActivatedMessageProcessor);

        groupInactivateMessageProcessor = new GroupInstanceInactivateProcessor();
        add(groupInactivateMessageProcessor);

        groupTerminatedProcessor = new GroupInstanceTerminatedProcessor();
        add(groupTerminatedProcessor);

        groupTerminatingProcessor = new GroupInstanceTerminatingProcessor();
        add(groupTerminatingProcessor);

        applicationInstanceCreatedMessageProcessor = new ApplicationInstanceCreatedMessageProcessor();
        add(applicationInstanceCreatedMessageProcessor);

        applicationActivatedMessageProcessor = new ApplicationInstanceActivatedMessageProcessor();
        add(applicationActivatedMessageProcessor);

        applicationCreatedMessageProcessor = new ApplicationCreatedMessageProcessor();
        add(applicationCreatedMessageProcessor);

        applicationDeletedMessageProcessor = new ApplicationDeletedMessageProcessor();
        add(applicationDeletedMessageProcessor);

        applicationInactivatedMessageProcessor = new ApplicationInstanceInactivatedMessageProcessor();
        add(applicationInactivatedMessageProcessor);

        applicationTerminatingMessageProcessor = new ApplicationInstanceTerminatingMessageProcessor();
        add(applicationTerminatingMessageProcessor);

        completeApplicationsMessageProcessor = new CompleteApplicationsMessageProcessor();
        add(completeApplicationsMessageProcessor);

        applicationTerminatedMessageProcessor = new ApplicationInstanceTerminatedMessageProcessor();
        add(applicationTerminatedMessageProcessor);

        if (log.isDebugEnabled()) {
            log.debug("Instance notifier message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {

        if (eventListener instanceof GroupInstanceCreatedEventListener) {
            groupCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupInstanceInactivateEventListener) {
            groupInactivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupInstanceActivatedEventListener) {
            groupActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupInstanceTerminatingEventListener) {
            groupTerminatingProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupInstanceTerminatedEventListener) {
            groupTerminatedProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationInstanceCreatedEventListener) {
            applicationInstanceCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationCreatedEventListener) {
            applicationCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationDeletedEventListener) {
            applicationDeletedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationInstanceActivatedEventListener) {
            applicationActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationInstanceInactivatedEventListener) {
            applicationInactivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationInstanceTerminatingEventListener) {
            applicationTerminatingMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationInstanceTerminatedEventListener) {
            applicationTerminatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof CompleteApplicationsEventListener) {
            completeApplicationsMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener " + eventListener.toString());
        }
    }
}
