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
package org.apache.stratos.messaging.message.processor.application.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.application.status.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Application Status processor chain is to handle the list processors to parse the application
 * status.
 */
public class AppStatusMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(AppStatusMessageProcessorChain.class);

    private AppStatusClusterCreatedMessageProcessor clusterCreatedMessageProcessor;
    private AppStatusClusterActivatedMessageProcessor clusterActivatedMessageProcessor;
    private AppStatusClusterInactivateMessageProcessor clusterInActivateMessageProcessor;
    private AppStatusClusterTerminatingMessageProcessor clusterTerminatingMessageProcessor;
    private AppStatusClusterTerminatedMessageProcessor clusterTerminatedMessageProcessor;
    private AppStatusGroupCreatedMessageProcessor groupCreatedMessageProcessor;
    private AppStatusGroupActivatedMessageProcessor groupActivatedMessageProcessor;
    private AppStatusGroupInactivatedMessageProcessor groupInActivateMessageProcessor;
    private AppStatusApplicationActivatedMessageProcessor appActivatedMessageProcessor;
    private AppStatusApplicationCreatedMessageProcessor applicationStatusAppCreatedMessageProcessor;
    private AppStatusApplicationInactivatedMessageProcessor applicationStatusAppInActivatedMessageProcessor;
    private AppStatusApplicationTerminatedMessageProcessor applicationStatusAppTerminatedMessageProcessor;
    private AppStatusApplicationTerminatingMessageProcessor applicationStatusAppTerminatingMessageProcessor;

    private AppStatusGroupTerminatedMessageProcessor groupTerminatedMessageProcessor;
    private AppStatusGroupTerminatingMessageProcessor groupTerminatingMessageProcessor;

    public void initialize() {
        // Add instance notifier event processors
        clusterCreatedMessageProcessor= new AppStatusClusterCreatedMessageProcessor();
        add(clusterCreatedMessageProcessor);

        clusterActivatedMessageProcessor = new AppStatusClusterActivatedMessageProcessor();
        add(clusterActivatedMessageProcessor);

        clusterInActivateMessageProcessor = new AppStatusClusterInactivateMessageProcessor();
        add(clusterInActivateMessageProcessor);

        clusterTerminatingMessageProcessor = new AppStatusClusterTerminatingMessageProcessor();
        add(clusterTerminatingMessageProcessor);

        clusterTerminatedMessageProcessor = new AppStatusClusterTerminatedMessageProcessor();
        add(clusterTerminatedMessageProcessor);

        groupCreatedMessageProcessor = new AppStatusGroupCreatedMessageProcessor();
        add(groupCreatedMessageProcessor);

        groupActivatedMessageProcessor = new AppStatusGroupActivatedMessageProcessor();
        add(groupActivatedMessageProcessor);

        groupInActivateMessageProcessor = new AppStatusGroupInactivatedMessageProcessor();
        add(groupInActivateMessageProcessor);

        appActivatedMessageProcessor = new AppStatusApplicationActivatedMessageProcessor();
        add(appActivatedMessageProcessor);

        applicationStatusAppCreatedMessageProcessor = new AppStatusApplicationCreatedMessageProcessor();
        this.add(applicationStatusAppCreatedMessageProcessor);

        applicationStatusAppInActivatedMessageProcessor = new AppStatusApplicationInactivatedMessageProcessor();
        this.add(applicationStatusAppInActivatedMessageProcessor);

        applicationStatusAppTerminatedMessageProcessor = new AppStatusApplicationTerminatedMessageProcessor();
        this.add(applicationStatusAppTerminatedMessageProcessor);

        applicationStatusAppTerminatingMessageProcessor = new AppStatusApplicationTerminatingMessageProcessor();
        this.add(applicationStatusAppTerminatingMessageProcessor);

        groupTerminatedMessageProcessor = new AppStatusGroupTerminatedMessageProcessor();
        this.add(groupTerminatedMessageProcessor);

        groupTerminatingMessageProcessor = new AppStatusGroupTerminatingMessageProcessor();
        this.add(groupTerminatingMessageProcessor);

        if (log.isDebugEnabled()) {
            log.debug("Instance notifier message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {
        if(eventListener instanceof AppStatusClusterCreatedEventListener) {
            clusterCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppStatusClusterActivatedEventListener) {
            clusterActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppStatusClusterInactivateEventListener) {
            clusterInActivateMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusGroupCreatedEventListener) {
            groupCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppStatusGroupActivatedEventListener) {
            groupActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusClusterTerminatedEventListener){
            clusterTerminatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusClusterTerminatingEventListener){
            clusterTerminatingMessageProcessor.addEventListener(eventListener);
        }else if (eventListener instanceof AppStatusGroupInactivateEventListener) {
            groupInActivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppStatusApplicationActivatedEventListener) {
            appActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusApplicationInactivatedEventListener){
            applicationStatusAppInActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusApplicationCreatedEventListener){
            applicationStatusAppCreatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusApplicationTerminatingEventListener){
            applicationStatusAppTerminatingMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof AppStatusApplicationTerminatedEventListener){
            applicationStatusAppTerminatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppStatusGroupTerminatingEventListener){
            groupTerminatingMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AppStatusGroupTerminatedEventListener){
            groupTerminatedMessageProcessor.addEventListener(eventListener);
        } else
        {
            throw new RuntimeException("Unknown event listener " + eventListener.toString());
        }
    }
}
