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

package org.apache.stratos.manager.messaging.publisher.synchronizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.internal.ServiceReferenceHolder;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.*;

/**
 * Tenant event synchronizer publishes complete tenant event periodically.
 */
public class TenantEventSynchronizer implements Runnable {

	private static final Log log = LogFactory.getLog(TenantEventSynchronizer.class);

	@Override
	public void run() {
		try {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Publishing complete tenant event"));
			}
			Tenant tenant;
			List<Tenant> tenants = new ArrayList<Tenant>();
			TenantManager tenantManager = ServiceReferenceHolder.getRealmService().getTenantManager();
			org.wso2.carbon.user.api.Tenant[] carbonTenants = tenantManager.getAllTenants();
			for (org.wso2.carbon.user.api.Tenant carbonTenant : carbonTenants) {
				// Create tenant
				if (log.isDebugEnabled()) {
					log.debug(String.format("Tenant found: [tenant-id] %d [tenant-domain] %s",
					                        carbonTenant.getId(), carbonTenant.getDomain()));
				}
				tenant = new Tenant(carbonTenant.getId(), carbonTenant.getDomain());

				if (!org.apache.stratos.messaging.message.receiver.tenant.TenantManager.getInstance()
				                                                                       .tenantExists(carbonTenant.getId())) {
					// if the tenant is not already there in TenantManager,
					// trigger TenantCreatedEvent
					TenantInfoBean tenantBean = new TenantInfoBean();
					tenantBean.setTenantId(carbonTenant.getId());
					tenantBean.setTenantDomain(carbonTenant.getDomain());

					// Add tenant to Tenant Manager
					org.apache.stratos.messaging.message.receiver.tenant.TenantManager.getInstance()
					                                                                  .addTenant(tenant);
				}
				tenants.add(tenant);
			}
			CompleteTenantEvent event = new CompleteTenantEvent(tenants);
			String topic = MessagingUtil.getMessageTopicName(event);
			EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
			eventPublisher.publish(event);
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("Could not publish complete tenant event", e);
			}
		}
	}
}
