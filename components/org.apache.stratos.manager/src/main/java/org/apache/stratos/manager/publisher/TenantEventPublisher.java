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

package org.apache.stratos.manager.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.listeners.TenantMgtListener;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.event.tenant.TenantCreatedEvent;
import org.apache.stratos.messaging.event.tenant.TenantRemovedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUpdatedEvent;
import org.apache.stratos.messaging.util.Constants;

/**
 * Tenant event publisher to publish tenant events to the message broker by
 * listening to the tenant manager.
 */
public class TenantEventPublisher implements TenantMgtListener {

    private static final Log log = LogFactory.getLog(TenantEventPublisher.class);
    private static final int EXEC_ORDER = 1;


    @Override
        public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
            try {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Publishing tenant created event: [tenant-id] %d [tenant-domain] %s", tenantInfo.getTenantId(), tenantInfo.getTenantDomain()));
                }
                Tenant tenant = new Tenant(tenantInfo.getTenantId(), tenantInfo.getTenantDomain());
                TenantCreatedEvent event = new TenantCreatedEvent(tenant);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.TENANT_TOPIC);
                eventPublisher.publish(event);
            }
            catch (Exception e) {
                log.error("Could not publish tenant created event", e);
            }
        }

        @Override
        public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
            try {
                if(log.isInfoEnabled()) {
                    log.info(String.format("Publishing tenant updated event: [tenant-id] %d [tenant-domain] %s", tenantInfo.getTenantId(), tenantInfo.getTenantDomain()));
                }
                TenantUpdatedEvent event = new TenantUpdatedEvent(tenantInfo.getTenantId(), tenantInfo.getTenantDomain());
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.TENANT_TOPIC);
                eventPublisher.publish(event);
            }
            catch (Exception e) {
                log.error("Could not publish tenant updated event");
            }
        }

        @Override
        public void onTenantDelete(int tenantId) {
            try {
                if(log.isInfoEnabled()) {
                    log.info(String.format("Publishing tenant removed event: [tenant-id] %d", tenantId));
                }
                TenantRemovedEvent event = new TenantRemovedEvent(tenantId);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.TENANT_TOPIC);
                eventPublisher.publish(event);
            }
            catch (Exception e) {
                log.error("Could not publish tenant removed event");
            }
        }

        @Override
        public void onTenantRename(int tenantId, String oldDomainName, String newDomainName) throws StratosException {
        }

        @Override
        public void onTenantInitialActivation(int tenantId) throws StratosException {
        }

        @Override
        public void onTenantActivation(int tenantId) throws StratosException {
        }

        @Override
        public void onTenantDeactivation(int tenantId) throws StratosException {
        }

        @Override
        public void onSubscriptionPlanChange(int tenantId, String oldPlan, String newPlan) throws StratosException {
        }

        @Override
        public int getListenerOrder() {
            return EXEC_ORDER;
        }
}
