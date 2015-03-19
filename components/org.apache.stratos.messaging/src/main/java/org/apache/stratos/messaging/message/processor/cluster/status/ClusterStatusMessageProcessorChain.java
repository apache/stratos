/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.message.processor.cluster.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.cluster.status.ClusterStatusClusterCreatedEvent;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.cluster.status.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * This is to keep track of the processors for the cluster status topic.
 */
public class ClusterStatusMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(ClusterStatusMessageProcessorChain.class);

    private ClusterStatusClusterCreatedMessageProcessor clusterCreatedMessageProcessor;
    private ClusterStatusClusterActivatedMessageProcessor clusterActivatedMessageProcessor;
    private ClusterStatusClusterResetMessageProcessor clusterResetMessageProcessor;
    private ClusterStatusClusterInactivateMessageProcessor clusterInactivateMessageProcessor;
    private ClusterStatusClusterTerminatedMessageProcessor clusterTerminatedMessageProcessor;
    private ClusterStatusClusterTerminatingMessageProcessor clusterTerminatingMessageProcessor;
    private ClusterStatusClusterInstanceCreatedMessageProcessor clusterInstanceCreatedMessageProcessor;

    @Override
    protected void initialize() {
        clusterCreatedMessageProcessor = new ClusterStatusClusterCreatedMessageProcessor();
        add(clusterCreatedMessageProcessor);

        clusterResetMessageProcessor = new ClusterStatusClusterResetMessageProcessor();
        add(clusterResetMessageProcessor);

        clusterActivatedMessageProcessor = new ClusterStatusClusterActivatedMessageProcessor();
        add(clusterActivatedMessageProcessor);

        clusterInactivateMessageProcessor = new ClusterStatusClusterInactivateMessageProcessor();
        add(clusterInactivateMessageProcessor);

        clusterTerminatedMessageProcessor = new ClusterStatusClusterTerminatedMessageProcessor();
        add(clusterTerminatedMessageProcessor);

        clusterTerminatingMessageProcessor = new ClusterStatusClusterTerminatingMessageProcessor();
        add(clusterTerminatingMessageProcessor);

        clusterInstanceCreatedMessageProcessor = new ClusterStatusClusterInstanceCreatedMessageProcessor();
        add(clusterInstanceCreatedMessageProcessor);

        if (log.isDebugEnabled()) {
            log.debug("Cluster status  message processor chain initialized");
        }
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListener instanceof ClusterStatusClusterCreatedEventListener) {
            clusterCreatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterStatusClusterResetEventListener) {
            clusterResetMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterStatusClusterInactivateEventListener) {
            clusterInactivateMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterStatusClusterActivatedEventListener) {
            clusterActivatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterStatusClusterTerminatingEventListener) {
            clusterTerminatingMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterStatusClusterTerminatedEventListener) {
            clusterTerminatedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof ClusterStatusClusterInstanceCreatedEventListener) {
            clusterInstanceCreatedMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener " + eventListener.toString());
        }

    }
}
