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
package org.apache.stratos.messaging.message.processor.initializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.initializer.CompleteApplicationSignUpsRequestEventListener;
import org.apache.stratos.messaging.listener.initializer.CompleteApplicationsRequestEventListener;
import org.apache.stratos.messaging.listener.initializer.CompleteTenantRequestEventListener;
import org.apache.stratos.messaging.listener.initializer.CompleteTopologyRequestEventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

public class InitializerMessageProcessorChain extends MessageProcessorChain {
    private static final Log log = LogFactory.getLog(InitializerMessageProcessorChain.class);
    private CompleteTopologyRequestMessageProcessor completeTopologyRequestMessageProcessor;
    private CompleteApplicationsRequestMessageProcessor completeApplicationsRequestMessageProcessor;
    private CompleteTenantRequestMessageProcessor completeTenantRequestMessageProcessor;
    private CompleteApplicationSignUpsRequestMessageProcessor completeApplicationSignUpsRequestMessageProcessor;

    @Override
    protected void initialize() {
        completeTopologyRequestMessageProcessor = new CompleteTopologyRequestMessageProcessor();
        add(completeTopologyRequestMessageProcessor);

        completeApplicationsRequestMessageProcessor = new CompleteApplicationsRequestMessageProcessor();
        add(completeApplicationsRequestMessageProcessor);

        completeTenantRequestMessageProcessor = new CompleteTenantRequestMessageProcessor();
        add(completeTenantRequestMessageProcessor);

        completeApplicationSignUpsRequestMessageProcessor = new CompleteApplicationSignUpsRequestMessageProcessor();
        add(completeApplicationSignUpsRequestMessageProcessor);

        if (log.isDebugEnabled()) {
            log.debug("Initializer message processor chain initialized");
        }
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListener instanceof CompleteTopologyRequestEventListener) {
            completeTopologyRequestMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof CompleteApplicationsRequestEventListener) {
            completeApplicationsRequestMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof CompleteTenantRequestEventListener) {
            completeTenantRequestMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof CompleteApplicationSignUpsRequestEventListener) {
            completeApplicationSignUpsRequestMessageProcessor.addEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener");
        }
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        if (eventListener instanceof CompleteTopologyRequestEventListener) {
            completeTopologyRequestMessageProcessor.removeEventListener(eventListener);
        } else if (eventListener instanceof CompleteApplicationsRequestEventListener) {
            completeApplicationsRequestMessageProcessor.removeEventListener(eventListener);
        } else if (eventListener instanceof CompleteTenantRequestEventListener) {
            completeTenantRequestMessageProcessor.removeEventListener(eventListener);
        } else if (eventListener instanceof CompleteApplicationSignUpsRequestEventListener) {
            completeApplicationSignUpsRequestMessageProcessor.removeEventListener(eventListener);
        } else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
