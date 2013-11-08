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

package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.EventListener;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Defines default topology message processor chain.
 */
public class TopologyEventProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(TopologyEventProcessorChain.class);

    private ServiceCreatedEventProcessor serviceCreatedEventProcessor;
    private ServiceRemovedEventProcessor serviceRemovedEventProcessor;
    private ClusterCreatedEventProcessor clusterCreatedEventProcessor;
    private ClusterRemovedEventProcessor clusterRemovedEventProcessor;
    private InstanceSpawnedEventProcessor instanceSpawnedEventProcessor;
    private MemberStartedEventProcessor memberStartedEventProcessor;
    private MemberActivatedEventProcessor memberActivatedEventProcessor;
    private MemberSuspendedEventProcessor memberSuspendedEventProcessor;
    private MemberTerminatedEventProcessor memberTerminatedEventProcessor;

    public void initialize() {
        // Add topology event processors
        serviceCreatedEventProcessor = new ServiceCreatedEventProcessor();
        add(serviceCreatedEventProcessor);

        serviceRemovedEventProcessor = new ServiceRemovedEventProcessor();
        add(serviceRemovedEventProcessor);

        clusterCreatedEventProcessor = new ClusterCreatedEventProcessor();
        add(clusterCreatedEventProcessor);

        clusterRemovedEventProcessor = new ClusterRemovedEventProcessor();
        add(clusterRemovedEventProcessor);

        instanceSpawnedEventProcessor = new InstanceSpawnedEventProcessor();
        add(instanceSpawnedEventProcessor);

        memberStartedEventProcessor = new MemberStartedEventProcessor();
        add(memberStartedEventProcessor);

        memberActivatedEventProcessor = new MemberActivatedEventProcessor();
        add(memberActivatedEventProcessor);

        memberSuspendedEventProcessor = new MemberSuspendedEventProcessor();
        add(memberSuspendedEventProcessor);

        memberTerminatedEventProcessor = new MemberTerminatedEventProcessor();
        add(memberTerminatedEventProcessor);

        if(log.isDebugEnabled()) {
            log.debug("Topology message processor chain initialized");
        }
    }

    public void addEventListener(EventListener eventListener) {
        if(eventListener instanceof ClusterCreatedEventListener) {
            clusterCreatedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof ClusterRemovedEventListener) {
            clusterRemovedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof InstanceSpawnedEventListener) {
            instanceSpawnedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof MemberActivatedEventListener) {
            memberActivatedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof MemberStartedEventListener) {
            memberStartedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof MemberSuspendedEventListener) {
            memberSuspendedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof MemberTerminatedEventListener) {
            memberTerminatedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof ServiceCreatedEventListener) {
            serviceCreatedEventProcessor.addEventListener(eventListener);
        }
        else if(eventListener instanceof ServiceRemovedEventListener) {
            serviceRemovedEventProcessor.addEventListener(eventListener);
        }
        else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
