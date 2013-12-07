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

package org.apache.stratos.adc.mgt.subscription.tenancy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.AlreadySubscribedException;
import org.apache.stratos.adc.mgt.exception.NotSubscribedException;
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.adc.topology.mgt.serviceobjects.DomainContext;

import java.util.Properties;

public class SubscriptionMultiTenantBehaviour extends SubscriptionTenancyBehaviour {

    private static Log log = LogFactory.getLog(SubscriptionMultiTenantBehaviour.class);

    public SubscriptionMultiTenantBehaviour(CartridgeSubscription cartridgeSubscription) {
        super(cartridgeSubscription);
    }

    public void createSubscription() throws ADCException, AlreadySubscribedException {

        boolean allowMultipleSubscription = Boolean.
                valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

        if (!allowMultipleSubscription) {
            // If the cartridge is multi-tenant. We should not let users createSubscription twice.
            boolean subscribed;
            try {
                subscribed = PersistenceManager.isAlreadySubscribed(cartridgeSubscription.getType(),
                        cartridgeSubscription.getSubscriber().getTenantId());
            } catch (Exception e) {
                String msg = "Error checking whether the cartridge type " + cartridgeSubscription.getType()
                        + " is already subscribed";
                log.error(msg, e);
                throw new ADCException(msg, e);
            }

            if (subscribed) {
                String msg = "Already subscribed to " + cartridgeSubscription.getType()
                        + ". This multi-tenant cartridge will not be available to createSubscription";
                if (log.isDebugEnabled()) {
                    log.debug(msg);
                }
                throw new AlreadySubscribedException(msg, cartridgeSubscription.getType());
            }
        }

        TopologyManagementService topologyService = DataHolder.getTopologyMgtService();
        DomainContext[] domainContexts = topologyService.getDomainsAndSubdomains(cartridgeSubscription.getType(),
                cartridgeSubscription.getSubscriber().getTenantId());
        log.info("Retrieved " + domainContexts.length + " domain and corresponding subdomain pairs");

        if (domainContexts.length > 0) {
            if(domainContexts.length > 2) {
                if(log.isDebugEnabled())
                    log.debug("Too many domain sub domain pairs");
            }

            for (DomainContext domainContext : domainContexts) {
                if (domainContext.getSubDomain().equalsIgnoreCase("mgt")) {
                    cartridgeSubscription.getCluster().setMgtClusterDomain(domainContext.getDomain());
                    cartridgeSubscription.getCluster().setMgtClusterSubDomain(domainContext.getSubDomain());
                } else {
                    cartridgeSubscription.getCluster().setClusterDomain(domainContext.getDomain());
                    cartridgeSubscription.getCluster().setClusterSubDomain(domainContext.getSubDomain());
                }
            }
        } else {
            String msg = "Domain contexts not found for " + cartridgeSubscription.getType() + " and tenant id " +
                    cartridgeSubscription.getSubscriber().getTenantId();
            log.warn(msg);
            throw new ADCException(msg);
        }
    }

    public void registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException {

        //nothing to do
    }

    public void removeSubscription() throws ADCException, NotSubscribedException {

        log.info("Cartridge with alias " + cartridgeSubscription.getAlias() + ", and type " + cartridgeSubscription.getType() +
                " is a multi-tenant cartridge and therefore will not terminate all instances and " +
                "unregister services");
    }

    public PayloadArg createPayloadParameters(PayloadArg payloadArg) throws ADCException {

        //payload not used
        return null;
    }
}
