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
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.signup.DomainMapping;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.domain.mapping.DomainMappingManager;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Url mapping added message processor.
 */
public class DomainMappingAddedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(DomainMappingAddedMessageProcessor.class);

    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        if (DomainMappingAddedEvent.class.getName().equals(type)) {

            // Return if domain mapping manager has not initialized
            if (!DomainMappingManager.getInstance().isInitialized()) {
                return false;
            }

            // Parse complete message and build event
            DomainMappingAddedEvent event = (DomainMappingAddedEvent) MessagingUtil.jsonToObject(message, DomainMappingAddedEvent.class);

            int tenantId = event.getTenantId();
            String applicationId = event.getApplicationId();

            try {
                TenantManager.acquireReadLock();
                Tenant tenant = TenantManager.getInstance().getTenant(tenantId);
                if (tenant == null) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Tenant not found: [tenant-id] %d", tenantId));
                    }
                    return false;
                }

                ApplicationManager.acquireReadLockForApplication(applicationId);
                Application application = ApplicationManager.getApplications().getApplication(applicationId);
                if (application == null) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Application not found: [application-id] %d", applicationId));
                    }
                    return false;
                }

                try {
                    DomainMappingManager.acquireWriteLock();

                    DomainMapping domainMapping = new DomainMapping();
                    domainMapping.setTenantId(event.getTenantId());
                    domainMapping.setApplicationId(event.getApplicationId());
                    domainMapping.setServiceName(event.getServiceName());
                    domainMapping.setClusterId(event.getClusterId());
                    domainMapping.setDomainName(event.getDomainName());
                    domainMapping.setContextPath(event.getContextPath());

                    DomainMappingManager.getInstance().addDomainMapping(domainMapping);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Domain mapping added: [application-id] %s [cluster-id] %s " +
                                "[domain-name]", event.getApplicationId(), event.getClusterId(), event.getDomainName()));
                    }

                    // Notify event listeners
                    notifyEventListeners(event);
                    return true;
                } catch (Exception e) {
                    String error = String.format("Could not add domain mapping: [application-id] %s [cluster-id] %s " +
                            "[domain-name]", event.getApplicationId(), event.getClusterId(), event.getDomainName());
                    log.error(error, e);
                    return false;
                } finally {
                    DomainMappingManager.releaseWriteLock();
                }
            } finally {
                ApplicationManager.releaseReadLockForApplication(applicationId);
                TenantManager.releaseReadLock();
            }
        } else {
            if (nextProcessor != null) {
                return nextProcessor.process(type, message, object);
            } else {
                throw new RuntimeException(String.format("Failed to process domain mapping using available " +
                        "message processors: [type] %s [body] %s", type, message));
            }
        }
    }
}
