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
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingAddedEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingRemovedEventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Domain mapping message processor chain.
 */
public class DomainMappingMessageProcessorChain extends MessageProcessorChain {

    private static final Log log = LogFactory.getLog(DomainMappingMessageProcessorChain.class);

    private DomainMappingAddedMessageProcessor domainNameAddedMessageProcessor;
    private DomainMappingRemovedMessageProcessor domainNameRemovedMessageProcessor;

    @Override
    protected void initialize() {
        domainNameAddedMessageProcessor = new DomainMappingAddedMessageProcessor();
        add(domainNameAddedMessageProcessor);

        domainNameRemovedMessageProcessor = new DomainMappingRemovedMessageProcessor();
        add(domainNameRemovedMessageProcessor);
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListener instanceof DomainMappingAddedEventListener) {
            domainNameAddedMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof DomainMappingRemovedEventListener) {
            domainNameRemovedMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
