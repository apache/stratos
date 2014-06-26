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
import org.apache.stratos.messaging.domain.topology.ConfigCompositeApplication;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.CompositeApplicationCreatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.Util;
import org.apache.stratos.messaging.domain.topology.CompositeApplication;
import org.apache.stratos.messaging.domain.topology.util.CompositeApplicationBuilder;

//Grouping
public class CompositeApplicationCreatedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompositeApplicationCreatedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;
        
        if (log.isDebugEnabled()) {
        	log.debug("processing application event of type " + type + 
        			" / topology:" +  topology + " msg: " + message);
        }

        if (CompositeApplicationCreatedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
            if (!topology.isInitialized()) {
                
            	if (log.isDebugEnabled()) {
                	log.debug("topology is not initialized .... need to add check ... Grouping");
                }
            	
            	//return false;
            }

            // Parse complete message and build event
            CompositeApplicationCreatedEvent event = 
            		(CompositeApplicationCreatedEvent) Util.jsonToObject(message, CompositeApplicationCreatedEvent.class);
            
            if (log.isDebugEnabled()) {
            	log.debug("processing application created event with application id: " + event.getApplicationAlias());
            }

         // Validate event against the existing topology
            if (topology.compositeApplicationExists(event.getApplicationAlias())) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("CompositeApplication already created: [com app] %s", event.getApplicationAlias()));
                }
            } else {
            	
            	ConfigCompositeApplication configApp = event.getCompositeApplication();
             	String alias = "compositeApplicationAlias"; 
                if (log.isInfoEnabled()) {
            		log.info("adding ConfigCompositeApplication with alias " + alias + " to topology");
                }
                topology.addConfigCompositeApplication(alias, configApp);
            }
            
            // Notify event listeners
            notifyEventListeners(event);
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
