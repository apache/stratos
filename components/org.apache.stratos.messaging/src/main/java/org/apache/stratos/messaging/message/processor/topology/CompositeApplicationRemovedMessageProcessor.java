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
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.CompositeApplicationCreatedEvent;
import org.apache.stratos.messaging.event.topology.CompositeApplicationRemovedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.Util;
import org.apache.stratos.messaging.domain.topology.CompositeApplication;
import org.apache.stratos.messaging.domain.topology.util.CompositeApplicationBuilder;

//Grouping
public class CompositeApplicationRemovedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompositeApplicationRemovedMessageProcessor.class);
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

        if (CompositeApplicationRemovedEvent.class.getName().equals(type)) {
            // Return if topology has not been initialized
        	
            if (!topology.isInitialized()) {

            	if (log.isDebugEnabled()) {
                	log.debug("topology is not initialized .... need to add check ... Grouping");
                }
                //return false;
            }

            // Parse complete message and build event
            CompositeApplicationRemovedEvent event = 
            		(CompositeApplicationRemovedEvent) Util.jsonToObject(message, CompositeApplicationRemovedEvent.class);
            
            if (log.isDebugEnabled()) {
            	log.debug("processing application removed event with application id: " + event.getApplicationAlias());
            }

         // Validate event against the existing topology
            if (topology.configCompositeApplicationExists(event.getApplicationAlias())) {
            	topology.removeAllConfigCompositeApplication();
                if (log.isDebugEnabled()) {
                    log.debug("CompositeApplication exists, removing all ConfigCompositeApplication" + event.getApplicationAlias()) ;
                }
            } else {
                topology.removeAllConfigCompositeApplication();
            	if (log.isWarnEnabled()) {
                    log.warn("ConfigCompositeApplication " + event.getApplicationAlias() + " does not exist, removing all ") ;
                }
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
