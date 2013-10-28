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
package org.apache.stratos.cloud.controller.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.messaging.event.instance.status.MemberActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.MemberStartedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

import javax.jms.TextMessage;

public class TopologyEventMessageDelegator implements Runnable {
    private static final Log log = LogFactory.getLog(TopologyEventMessageDelegator.class);

     @Override
    public void run() {
		log.info("Topology event message processor started");
		log.info("Waiting for the complete topology event message...");

        while (true) {
			try {
				TextMessage message = FasterLookUpDataHolder.getInstance().getSharedTopologyDiffQueue().take();

				// retrieve the header
				String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
				// retrieve the actual message
				String json = message.getText();
                if(MemberActivatedEvent.class.getName().equals(type)) {
                     TopologyBuilder.handleMemberStarted((MemberStartedEvent)Util.
                                                        jsonToObject(json, MemberStartedEvent.class));
                } else if(MemberActivatedEvent.class.getName().equals(type)) {
                     TopologyBuilder.handleMemberActivated((MemberActivatedEvent) Util.
                                                        jsonToObject(json, MemberActivatedEvent.class));
                }

				if (log.isDebugEnabled()) {
					log.debug(String.format("Event message received from queue: %s", type));
				}
            } catch (Exception e) {
                String error = "Failed to retrieve the topology event message.";
            	log.error(error, e);
            	throw new RuntimeException(error, e);
            }
        }
    }
}
