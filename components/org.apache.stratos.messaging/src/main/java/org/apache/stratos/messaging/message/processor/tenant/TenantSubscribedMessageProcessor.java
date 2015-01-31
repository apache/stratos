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

package org.apache.stratos.messaging.message.processor.tenant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.tenant.Subscription;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Tenant subscribed message processor for updating a tenant in tenant manager and
 * triggering tenant subscribed event listeners.
 */
public class TenantSubscribedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(TenantSubscribedMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        if (TenantSubscribedEvent.class.getName().equals(type)) {
            // Return if tenant manager has not initialized
            if(!TenantManager.getInstance().isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            TenantSubscribedEvent event = (TenantSubscribedEvent) MessagingUtil.jsonToObject(message, TenantSubscribedEvent.class);

            try {
                TenantManager.acquireWriteLock();
                Tenant tenant = TenantManager.getInstance().getTenant(event.getTenantId());
                if(tenant == null) {
                    if(log.isWarnEnabled()) {
                        log.warn(String.format("Tenant not found: [tenant-id] %d", event.getTenantId()));
                    }
                    return false;
                }
                Subscription subscription = new Subscription(event.getServiceName(), event.getClusterIds());
                tenant.addSubscription(subscription);
                if(log.isInfoEnabled()) {
                    log.info(String.format("Tenant subscribed to service: [tenant-id] %d [tenant-domain] %s [service] %s",
                             tenant.getTenantId(), tenant.getTenantDomain(), event.getServiceName()));
                }

                // Notify event listeners
                notifyEventListeners(event);
                return true;
            }
            finally {
                TenantManager.releaseWriteLock();
            }
        }
        else {
            if(nextProcessor != null) {
                return nextProcessor.process(type, message, object);
            }
            else {
                throw new RuntimeException(String.format("Failed to process tenant message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }
}
