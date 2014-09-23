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

package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.ApplicationCreatedEvent;
import org.apache.stratos.messaging.event.topology.ApplicationRemovedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.Util;

public class ApplicationRemovedMessageProcessor extends MessageProcessor {
	
	private static final Log log = LogFactory.getLog(ApplicationCreatedMessageProcessor.class);

	private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
    	
    	if (log.isDebugEnabled()) {
    		log.debug("ApplicationRemovedMessageProcessor processing " + object);
    	}
    	
    	Topology topology = (Topology) object;
    	
    	if (ApplicationRemovedEvent.class.getName().equals(type)) {
	    	if (!topology.isInitialized()) {
	    		if (log.isDebugEnabled()) {
	        		log.debug("ApplicationRemovedMessageProcessor topology not initialized ... " + object);
	        	}
	            return false;
	        }
	
	        ApplicationRemovedEvent appRemovedEvent = (ApplicationRemovedEvent) Util.jsonToObject(message, ApplicationRemovedEvent.class);
	        if (appRemovedEvent == null) {
	            log.error("Unable to convert the JSON message to ApplicationCreatedEvent");
	            return false;
	        }
	        
	     // check if required properties are available
	        if (appRemovedEvent.getApplicationId() == null) {
	            String errorMsg = "Application Id of application removed event is invalid";
	            log.error(errorMsg);
	            throw new RuntimeException(errorMsg);
	        }
	        
	        if (appRemovedEvent.getTenantDomain()== null) {
	            String errorMsg = "Application tenant domain of application removed event is invalid";
	            log.error(errorMsg);
	            throw new RuntimeException(errorMsg);
	        }
	        
	        // check if an Application with same name exists in topology
	        String appId = appRemovedEvent.getApplicationId();
            if (topology.applicationExists(appId)) {
                log.warn("Application with id [ " + appId + " ] still exists in Topology, removing it");
                topology.removeApplication(appId);
            } 
	        
            if (log.isDebugEnabled()) {
        		log.debug("ApplicationRemovedMessageProcessor notifying listener " + object);
        	}
            
	        notifyEventListeners(appRemovedEvent);
	        return true;
        
	    } else {
	        if (nextProcessor != null) {
	            // ask the next processor to take care of the message.
	            return nextProcessor.process(type, message, topology);
	        } else {
	            throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
	        }
	    }
        
    }
}
