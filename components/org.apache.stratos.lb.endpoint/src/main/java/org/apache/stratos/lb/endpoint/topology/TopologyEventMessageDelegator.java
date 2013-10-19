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
package org.apache.stratos.lb.endpoint.topology;

import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.processor.ClusterCreatedEventProcessor;
import org.apache.stratos.messaging.message.processor.ClusterRemovedEventProcessor;
import org.apache.stratos.messaging.message.processor.CompleteTopologyEventProcessor;
import org.apache.stratos.messaging.message.processor.MemberActivatedEventProcessor;
import org.apache.stratos.messaging.message.processor.MemberStartedEventProcessor;
import org.apache.stratos.messaging.message.processor.MemberSuspendedEventProcessor;
import org.apache.stratos.messaging.message.processor.MemberTerminatedEventProcessor;
import org.apache.stratos.messaging.message.processor.ServiceCreatedEventProcessor;
import org.apache.stratos.messaging.message.processor.ServiceRemovedEventProcessor;
import org.apache.stratos.messaging.util.Constants;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class TopologyEventMessageDelegator implements Runnable {

    private static final Log log = LogFactory.getLog(TopologyEventMessageDelegator.class);

    @Override
    public void run() {
		log.info("Topology event message processor started");
		log.info("Waiting for the complete topology event message...");
        while (true) {
            try {
                // First take the complete topology event
                TextMessage message = TopologyEventQueue.getInstance().take();
                
                // retrieve the header
                String type = message.getStringProperty(Constants.EVENT_NAME);
                // retrieve the actual message
                String json = message.getText();
                
                CompleteTopologyEventProcessor processor = new CompleteTopologyEventProcessor();
                if(processor.process(type, json, TopologyManager.getTopology())) {
                	break;
                }

            } catch (Exception e) {
            	log.error("Failed to retrieve the full topology.", e);
            	throw new RuntimeException("Failed to retrieve the full topology.", e);
            }
        }
        
        // instantiate all the relevant processors
        ServiceCreatedEventProcessor processor1 = new ServiceCreatedEventProcessor();
        ServiceRemovedEventProcessor processor2 = new ServiceRemovedEventProcessor();
        ClusterCreatedEventProcessor processor3 = new ClusterCreatedEventProcessor();
        ClusterRemovedEventProcessor processor4 = new ClusterRemovedEventProcessor();
        MemberStartedEventProcessor processor5 = new MemberStartedEventProcessor();
        MemberActivatedEventProcessor processor6 = new MemberActivatedEventProcessor();
        MemberSuspendedEventProcessor processor7 = new MemberSuspendedEventProcessor();
        MemberTerminatedEventProcessor processor8 = new MemberTerminatedEventProcessor();
        
        // link all the relevant processors in the required order
        processor1.setNext(processor2);
        processor2.setNext(processor3);
        processor3.setNext(processor4);
        processor4.setNext(processor5);
        processor5.setNext(processor6);
        processor6.setNext(processor7);
        processor7.setNext(processor8);
        
        while (true) {
			try {
				TextMessage message = TopologyEventQueue.getInstance().take();

				// retrieve the header
				String type = message.getStringProperty(Constants.EVENT_NAME);
				// retrieve the actual message
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
            	log.error("Failed to retrieve the topology event message.", e);
            	throw new RuntimeException("Failed to retrieve the topology event message.", e);
            }
        }
    }
}
