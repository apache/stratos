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
public class AppStatusMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(AppStatusMessageProcessorChain.class);

    private AppClusterCreatedMessageProcessor clusterCreatedMessageProcessor;
    private AppClusterActivatedMessageProcessor clusterActivatedMessageProcessor;
    private AppClusterInactivateMessageProcessor clusterInActivateMessageProcessor;
    private AppClusterTerminatingMessageProcessor clusterTerminatingMessageProcessor;
    private AppClusterTerminatedMessageProcessor clusterTerminatedMessageProcessor;
    private GroupCreatedMessageProcessor groupCreatedMessageProcessor;
    private GroupActivatedMessageProcessor groupActivatedMessageProcessor;
    private GroupInactivatedMessageProcessor groupInActivateMessageProcessor;
    private ApplicationActivatedMessageProcessor appActivatedMessageProcessor;
    private ApplicationCreatedMessageProcessor applicationStatusAppCreatedMessageProcessor;
    private ApplicationInactivatedMessageProcessor applicationStatusAppInActivatedMessageProcessor;
    private ApplicationTerminatedMessageProcessor applicationStatusAppTerminatedMessageProcessor;
    private ApplicationTerminatingMessageProcessor applicationStatusAppTerminatingMessageProcessor;

    private GroupTerminatedMessageProcessor groupTerminatedMessageProcessor;
    private GroupTerminatingMessageProcessor groupTerminatingMessageProcessor;

    public void initialize() {
        // Add instance notifier event processors
        clusterCreatedMessageProcessor= new AppClusterCreatedMessageProcessor();
        add(clusterCreatedMessageProcessor);

        clusterActivatedMessageProcessor = new AppClusterActivatedMessageProcessor();
        add(clusterActivatedMessageProcessor);

        clusterInActivateMessageProcessor = new AppClusterInactivateMessageProcessor();
        add(clusterInActivateMessageProcessor);

        clusterTerminatingMessageProcessor = new AppClusterTerminatingMessageProcessor();
        add(clusterTerminatingMessageProcessor);

        clusterTerminatedMessageProcessor = new AppClusterTerminatedMessageProcessor();
        add(clusterTerminatedMessageProcessor);

        groupCreatedMessageProcessor = new GroupCreatedMessageProcessor();
        add(groupCreatedMessageProcessor);

        groupActivatedMessageProcessor = new GroupActivatedMessageProcessor();
        add(groupActivatedMessageProcessor);

        groupInActivateMessageProcessor = new GroupInactivatedMessageProcessor();
        add(groupInActivateMessageProcessor);

        appActivatedMessageProcessor = new ApplicationActivatedMessageProcessor();
        add(appActivatedMessageProcessor);

        applicationStatusAppCreatedMessageProcessor = new ApplicationCreatedMessageProcessor();
        this.add(applicationStatusAppCreatedMessageProcessor);

        applicationStatusAppInActivatedMessageProcessor = new ApplicationInactivatedMessageProcessor();
        this.add(applicationStatusAppInActivatedMessageProcessor);

        applicationStatusAppTerminatedMessageProcessor = new ApplicationTerminatedMessageProcessor();
        this.add(applicationStatusAppTerminatedMessageProcessor);

        applicationStatusAppTerminatingMessageProcessor = new ApplicationTerminatingMessageProcessor();
        this.add(applicationStatusAppTerminatingMessageProcessor);

        groupTerminatedMessageProcessor = new GroupTerminatedMessageProcessor();
        this.add(groupTerminatedMessageProcessor);

        groupTerminatingMessageProcessor = new GroupTerminatingMessageProcessor();
        this.add(groupTerminatingMessageProcessor);

        if (log.isDebugEnabled()) {
            log.debug("Instance notifier message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {
        if(eventListener instanceof AppClusterCreatedEventListener) {
            clusterCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppClusterActivatedEventListener) {
            clusterActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppClusterInactivateEventListener) {
            clusterInActivateMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof GroupCreatedEventListener) {
            groupCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupActivatedEventListener) {
            groupActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppClusterTerminatedEventListener){
            clusterTerminatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppClusterTerminatingEventListener){
            clusterTerminatingMessageProcessor.addEventListener(eventListener);
        }else if (eventListener instanceof GroupInactivateEventListener) {
            groupInActivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationActivatedEventListener) {
            appActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationInactivatedEventListener){
            applicationStatusAppInActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationCreatedEventListener){
            applicationStatusAppCreatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationTerminatingEventListener){
            applicationStatusAppTerminatingMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationTerminatedEventListener){
            applicationStatusAppTerminatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupTerminatingEventListener){
            groupTerminatingMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupTerminatedEventListener){
            groupTerminatedMessageProcessor.addEventListener(eventListener);
        } else
        {
            throw new RuntimeException("Unknown event listener " + eventListener.toString());
        }
    }
}
