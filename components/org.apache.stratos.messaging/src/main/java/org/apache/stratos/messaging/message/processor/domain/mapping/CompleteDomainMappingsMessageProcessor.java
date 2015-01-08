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

package org.apache.stratos.messaging.message.processor.domain.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.domain.mapping.CompleteDomainMappingsEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.domain.mapping.DomainMappingManager;
import org.apache.stratos.messaging.util.Util;

/**
 * Complete domain mappings message processor.
 */
public class CompleteDomainMappingsMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompleteDomainMappingsMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        if (CompleteDomainMappingsEvent.class.getName().equals(type)) {
            // Return if domain mappings manager has already initialized
            if(DomainMappingManager.getInstance().isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            CompleteDomainMappingsEvent event = (CompleteDomainMappingsEvent) Util.jsonToObject(message,
                    CompleteDomainMappingsEvent.class);

            try {
                DomainMappingManager.acquireWriteLock();
                DomainMappingManager.getInstance().addDomainMappings(event.getDomainMappings());
                if(log.isInfoEnabled()) {
                    log.info("Domain mappings manager initialized");
                }
                DomainMappingManager.getInstance().setInitialized(true);

                // Notify event listeners
                notifyEventListeners(event);
                return true;
            }
            finally {
                DomainMappingManager.releaseWriteLock();
            }
        }
        else {
            if(nextProcessor != null) {
                return nextProcessor.process(type, message, object);
            }
            else {
                throw new RuntimeException(String.format("Failed to process domain mapping event message using " +
                        "available message processors: [type] %s [body] %s", type, message));
            }
        }
    }
}
