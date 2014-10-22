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
import org.apache.stratos.messaging.listener.topology.ClusterActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.GroupActivatedEventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Application Status processor chain is to handle the list processors to parse the application
 * status.
 */
public class ApplicationStatusMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(ApplicationStatusMessageProcessorChain.class);

    private ApplicationStatusClusterActivatedMessageProcessor clusterActivatedMessageProcessor;
    private ApplicationStatusClusterInActivateMessageProcessor clusterInActivateMessageProcessor;
    private ApplicationStatusGroupActivatedMessageProcessor groupActivatedMessageProcessor;
    private ApplicationStatusGroupInActivateMessageProcessor groupInActivateMessageProcessor;
    private ApplicationStatusAppActivatedMessageProcessor appActivatedMessageProcessor;
    private ApplicationStatusAppCreatedMessageProcessor applicationStatusAppCreatedMessageProcessor;
    private ApplicationStatusAppInActivatedMessageProcessor applicationStatusAppInActivatedMessageProcessor;
    private ApplicationStatusAppTerminatedMessageProcessor applicationStatusAppTerminatedMessageProcessor;
    private ApplicationStatusAppTerminatingMessageProcessor applicationStatusAppTerminatingMessageProcessor;

    public void initialize() {
        // Add instance notifier event processors
        clusterActivatedMessageProcessor = new ApplicationStatusClusterActivatedMessageProcessor();
        add(clusterActivatedMessageProcessor);

        clusterInActivateMessageProcessor = new ApplicationStatusClusterInActivateMessageProcessor();
        add(clusterInActivateMessageProcessor);

        groupActivatedMessageProcessor = new ApplicationStatusGroupActivatedMessageProcessor();
        add(groupActivatedMessageProcessor);

        groupInActivateMessageProcessor = new ApplicationStatusGroupInActivateMessageProcessor();
        add(groupInActivateMessageProcessor);

        appActivatedMessageProcessor = new ApplicationStatusAppActivatedMessageProcessor();
        add(appActivatedMessageProcessor);

        applicationStatusAppCreatedMessageProcessor = new ApplicationStatusAppCreatedMessageProcessor();
        this.add(applicationStatusAppCreatedMessageProcessor);

        applicationStatusAppInActivatedMessageProcessor = new ApplicationStatusAppInActivatedMessageProcessor();
        this.add(applicationStatusAppInActivatedMessageProcessor);

        applicationStatusAppTerminatedMessageProcessor = new ApplicationStatusAppTerminatedMessageProcessor();
        this.add(applicationStatusAppTerminatedMessageProcessor);

        applicationStatusAppTerminatingMessageProcessor = new ApplicationStatusAppTerminatingMessageProcessor();
        this.add(applicationStatusAppTerminatingMessageProcessor);


        if (log.isDebugEnabled()) {
            log.debug("Instance notifier message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {
        if (eventListener instanceof ClusterActivatedEventListener) {
            clusterActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterInActivateEventListener) {
            clusterInActivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupActivatedEventListener) {
            groupActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GroupInActivateEventListener) {
            groupInActivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ApplicationActivatedEventListener) {
            appActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationInActivatedEventListener){
            applicationStatusAppInActivatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationCreatedEventListener){
            applicationStatusAppCreatedMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationTerminatingEventListener){
            applicationStatusAppTerminatingMessageProcessor.addEventListener(eventListener);
        } else if(eventListener instanceof ApplicationTerminatedEventListener){
            applicationStatusAppTerminatedMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
