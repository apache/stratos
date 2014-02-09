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

package org.apache.stratos.manager.subscription.tenancy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.publisher.InstanceNotificationPublisher;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.utils.CartridgeConstants;


public class SubscriptionMultiTenantBehaviour extends SubscriptionTenancyBehaviour {

    private static Log log = LogFactory.getLog(SubscriptionMultiTenantBehaviour.class);


    public void createSubscription(CartridgeSubscription cartridgeSubscription) throws ADCException, AlreadySubscribedException {

        boolean allowMultipleSubscription = Boolean.
                valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

        if (!allowMultipleSubscription) {
            // If the cartridge is multi-tenant. We should not let users createSubscription twice.
            boolean subscribed;

            try {
                subscribed = hasAlreadySubscribed(cartridgeSubscription.getSubscriber().getTenantId(), cartridgeSubscription.getType());

            } catch (Exception e) {
                String msg = "Error checking whether the cartridge type " + cartridgeSubscription.getType() +
                        " is already subscribed";
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

        // get the cluster domain and host name from deployed Service
        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        Service deployedService;
        try {
            deployedService = dataInsertionAndRetrievalManager.getService(cartridgeSubscription.getType());

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in checking if Service is available is PersistenceManager";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }

        if (deployedService == null) {
            String errorMsg = "There is no deployed Service for type " + cartridgeSubscription.getType();
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        //set the cluster and hostname
        cartridgeSubscription.setClusterDomain(deployedService.getClusterId());
        cartridgeSubscription.setHostName(deployedService.getHostName());

        if (cartridgeSubscription.getRepository() != null) {

            // publish the ArtifactUpdated event
            log.info(" Multitenant --> Publishing Artifact update event -- ");
            log.info(" Values :  cluster id - " + cartridgeSubscription.getClusterDomain() + "  tenant - " +
                    cartridgeSubscription.getSubscriber().getTenantId());
            InstanceNotificationPublisher publisher = new InstanceNotificationPublisher();
            publisher.sendArtifactUpdateEvent(cartridgeSubscription.getRepository(),
                    cartridgeSubscription.getClusterDomain(), // clusterId
                    String.valueOf(cartridgeSubscription.getSubscriber().getTenantId()));

        } else {
            if(log.isDebugEnabled()) {
                log.debug("No repository found for subscription with alias: " + cartridgeSubscription.getAlias() + ", type: " + cartridgeSubscription.getType()+
                        ". Not sending the Artifact Updated event");
            }
        }
    }

    public void registerSubscription(CartridgeSubscription cartridgeSubscription, Properties properties)
            throws ADCException, UnregisteredCartridgeException {

        //nothing to do
    }

    public void removeSubscription(CartridgeSubscription cartridgeSubscription) throws ADCException, NotSubscribedException {

        log.info("Cartridge with alias " + cartridgeSubscription.getAlias() + ", and type " + cartridgeSubscription.getType() +
                " is a multi-tenant cartridge and therefore will not terminate all instances and " +
                "unregister services");
    }

    private static boolean hasAlreadySubscribed(int tenantId, String cartridgeType) {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
        return ( dataInsertionAndRetrievalManager.getCartridgeSubscriptions(tenantId, cartridgeType) == null ||
                 dataInsertionAndRetrievalManager.getCartridgeSubscriptions(tenantId, cartridgeType).isEmpty() ) ? false : true;
    }
}
