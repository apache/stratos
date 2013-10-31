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
package org.apache.stratos.messaging.message.receiver.topology;

import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.processor.topology.*;
import org.apache.stratos.messaging.util.Constants;


/**
 * Implements the default event processor chain for updating
 * the topology data structure in topology manager.
 */
public class TopologyEventMessageDelegator implements Runnable {

    private static final Log log = LogFactory.getLog(TopologyEventMessageDelegator.class);

    @Override
    public void run() {
        try {
            if(log.isInfoEnabled()) {
                log.info("Topology event message delegator started");
                log.info("Waiting for the complete topology event message...");
            }
            while (true) {
                try {
                    // First take the complete topology event
                    TextMessage message = TopologyEventQueue.getInstance().take();

                    // retrieve the header
                    String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
                    // retrieve the actual message
                    String json = message.getText();

                    CompleteTopologyEventProcessor processor = new CompleteTopologyEventProcessor();
                    if (processor.process(type, json, TopologyManager.getTopology())) {
                        break;
                    }

                } catch (Exception e) {
                    if(log.isErrorEnabled()) {
                        log.error("Failed to retrieve the full topology.", e);
                    }
                    throw new RuntimeException("Failed to retrieve the complete topology", e);
                }
            }

            // Instantiate all topology event processors
            ServiceCreatedEventProcessor processor1 = new ServiceCreatedEventProcessor();
            ServiceRemovedEventProcessor processor2 = new ServiceRemovedEventProcessor();
            ClusterCreatedEventProcessor processor3 = new ClusterCreatedEventProcessor();
            ClusterRemovedEventProcessor processor4 = new ClusterRemovedEventProcessor();
            InstanceSpawnedEventProcessor processor5 = new InstanceSpawnedEventProcessor();
            MemberStartedEventProcessor processor6 = new MemberStartedEventProcessor();
            MemberActivatedEventProcessor processor7 = new MemberActivatedEventProcessor();
            MemberSuspendedEventProcessor processor8 = new MemberSuspendedEventProcessor();
            MemberTerminatedEventProcessor processor9 = new MemberTerminatedEventProcessor();

            // Link above processors in the required order
            processor1.setNext(processor2);
            processor2.setNext(processor3);
            processor3.setNext(processor4);
            processor4.setNext(processor5);
            processor5.setNext(processor6);
            processor6.setNext(processor7);
            processor7.setNext(processor8);
            processor8.setNext(processor9);

            while (true) {
                try {
                    TextMessage message = TopologyEventQueue.getInstance().take();

                    // Retrieve the header
                    String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
                    // Retrieve the actual message
                    String json = message.getText();

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Event message received from queue: %s", type));
                    }

                    try {
                        TopologyManager.acquireWriteLock();
                        processor1.process(type, json, TopologyManager.getTopology());
                    } finally {
                        TopologyManager.releaseWriteLock();
                    }

                } catch (Exception e) {
                    String error = "Failed to retrieve topology event message";
                    log.error(error, e);
                    throw new RuntimeException(error, e);
                }
            }
        }
        catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Topology event message delegator failed", e);
            }
        }
    }
}
