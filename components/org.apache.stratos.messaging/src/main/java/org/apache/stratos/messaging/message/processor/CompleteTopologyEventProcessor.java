/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.message.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.util.Util;

public class CompleteTopologyEventProcessor implements MessageProcessor {

	private static final Log log = LogFactory.getLog(CompleteTopologyEventProcessor.class);
	private MessageProcessor nextMsgProcessor;

	@Override
	public void setNext(MessageProcessor nextProcessor) {
		nextMsgProcessor = nextProcessor;
	}

	@Override
	public boolean process(String type, String message, Topology topology) {
		try {
			if (CompleteTopologyEvent.class.getName().equals(type)) {
				// Parse complete message and build event
				CompleteTopologyEvent event =
				                              (CompleteTopologyEvent) Util.jsonToObject(message,
				                                                                        CompleteTopologyEvent.class);
				topology.addServices(event.getTopology().getServices());
				log.info("Topology initialized.");

				return true;
			} else {
				if (nextMsgProcessor != null) {
					// ask the next processor to take care of the message.
					return nextMsgProcessor.process(type, message, topology);
				}
			}
		} catch (Exception e) {
			if (nextMsgProcessor != null) {
				// ask the next processor to take care of the message.
				return nextMsgProcessor.process(type, message, topology);
			} else {
				throw new RuntimeException(
				                           String.format("Failed to process the message: %s of type %s using any of the available processors.",
				                                         message, type));
			}
		}
		
		return false;
	}

}
