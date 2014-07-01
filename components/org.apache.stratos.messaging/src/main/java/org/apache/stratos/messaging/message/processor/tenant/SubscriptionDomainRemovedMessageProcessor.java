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
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainRemovedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.util.Util;

/**
 * Subscription domain removed message processor for removing domains from tenant subscriptions.
 */
public class SubscriptionDomainRemovedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(SubscriptionDomainRemovedMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        if (SubscriptionDomainRemovedEvent.class.getName().equals(type)) {
            // Return if tenant manager has not initialized
            if (!TenantManager.getInstance().isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            SubscriptionDomainRemovedEvent event = (SubscriptionDomainRemovedEvent) Util.jsonToObject(message, SubscriptionDomainRemovedEvent.class);

            try {
                TenantManager.acquireWriteLock();
                Tenant tenant = TenantManager.getInstance().getTenant(event.getTenantId());
                if (tenant == null) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Tenant not found: [tenant-id] %d", event.getTenantId()));
                    }
                    return false;
                }
                Subscription subscription = tenant.getSubscription(event.getServiceName());
                if (subscription == null) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Subscription not found: [tenant-id] %d", event.getTenantId()));
                    }
                    return false;
                }
                subscription.removeSubscriptionDomain(event.getDomainName());
                if (log.isInfoEnabled()) {
                    log.info(String.format("Domain removed from tenant subscription: [tenant-id] %d [tenant-domain] %s " +
                                    "[service] %s [domain-name] %s", tenant.getTenantId(), tenant.getTenantDomain(),
                            event.getServiceName(), event.getDomainName()
                    ));
                }

                // Notify event listeners
                notifyEventListeners(event);
                return true;
            } finally {
                TenantManager.releaseWriteLock();
            }
        } else {
            if (nextProcessor != null) {
                return nextProcessor.process(type, message, object);
            } else {
                throw new RuntimeException(String.format("Failed to process tenant message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }
}
