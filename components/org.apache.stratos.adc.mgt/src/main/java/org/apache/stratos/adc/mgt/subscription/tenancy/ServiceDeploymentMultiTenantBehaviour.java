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

package org.apache.stratos.adc.mgt.subscription.tenancy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.AlreadySubscribedException;
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.cloud.controller.pojo.Properties;

public class ServiceDeploymentMultiTenantBehaviour extends
		SubscriptionMultiTenantBehaviour {

	private static Log log = LogFactory.getLog(ServiceDeploymentMultiTenantBehaviour.class);
	
	public ServiceDeploymentMultiTenantBehaviour(
			CartridgeSubscription cartridgeSubscription) {
		super(cartridgeSubscription);
	}
	
	@Override
	public void createSubscription() throws ADCException,
			AlreadySubscribedException {
	
		log.info(" --- in Service Deployment Multitenant Behaviour create subscription ---- ");
		
		boolean allowMultipleSubscription = Boolean.
        valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

		if (!allowMultipleSubscription) {
			// If the cartridge is multi-tenant. We should not let users
			// createSubscription twice.
			boolean subscribed;
			try {
				subscribed = PersistenceManager.isAlreadySubscribed(
						cartridgeSubscription.getType(), cartridgeSubscription
								.getSubscriber().getTenantId());
			} catch (Exception e) {
				String msg = "Error checking whether the cartridge type "
						+ cartridgeSubscription.getType()
						+ " is already subscribed";
				log.error(msg, e);
				throw new ADCException(msg, e);
			}

			if (subscribed) {
				String msg = "Already subscribed to "
						+ cartridgeSubscription.getType()
						+ ". This multi-tenant cartridge will not be available to createSubscription";
				if (log.isDebugEnabled()) {
					log.debug(msg);
				}
				throw new AlreadySubscribedException(msg,
						cartridgeSubscription.getType());
			}
		}
	
				/*if (domainContext.getSubDomain().equalsIgnoreCase("mgt")) {
					cartridgeSubscription.getCluster().setMgtClusterDomain(
							domainContext.getDomain());
					cartridgeSubscription.getCluster().setMgtClusterSubDomain(
							domainContext.getSubDomain());
				} else {
					cartridgeSubscription.getCluster().setClusterDomain(
							domainContext.getDomain());
					cartridgeSubscription.getCluster().setClusterSubDomain(
							domainContext.getSubDomain());
				}*/
				
	}
	
	
	@Override
	public void registerSubscription(Properties properties)
			throws ADCException, UnregisteredCartridgeException {
		
		// register subscription to start up the cartridge instances
		ApplicationManagementUtil.registerService(cartridgeSubscription.getType(),
                cartridgeSubscription.getCluster().getClusterDomain(),
                cartridgeSubscription.getCluster().getClusterSubDomain(),
                cartridgeSubscription.getPayload().createPayload(),
                cartridgeSubscription.getPayload().getPayloadArg().getTenantRange(),
                cartridgeSubscription.getCluster().getHostName(),
                cartridgeSubscription.getAutoscalingPolicyName(),
                cartridgeSubscription.getDeploymentPolicyName(),
                properties);
		
        cartridgeSubscription.getPayload().delete();
	}
	
	@Override
	public PayloadArg createPayloadParameters(PayloadArg payloadArg)
			throws ADCException {
				
		return payloadArg;
	}

}
