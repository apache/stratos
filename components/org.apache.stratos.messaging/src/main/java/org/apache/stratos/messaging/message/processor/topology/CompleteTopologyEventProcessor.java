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
package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.Util;

public class CompleteTopologyEventProcessor extends TopologyEventProcessor {

    private static final Log log = LogFactory.getLog(CompleteTopologyEventProcessor.class);
    private MessageProcessor nextMsgProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        nextMsgProcessor = nextProcessor;
    }

    @Override
    public boolean processEvent(String type, String message, Topology topology) {
        if (CompleteTopologyEvent.class.getName().equals(type)) {
            // Parse complete message and build event
            CompleteTopologyEvent event = (CompleteTopologyEvent) Util.jsonToObject(message, CompleteTopologyEvent.class);
            topology.addServices(event.getTopology().getServices());
            if (log.isInfoEnabled()) {
                log.info("Topology initialized");
            }

            notifyEventListeners(event);
            return true;
        } else {
            if (nextMsgProcessor != null) {
                // ask the next processor to take care of the message.
                return nextMsgProcessor.process(type, message, topology);
            }
            return false;
        }
    }
}
