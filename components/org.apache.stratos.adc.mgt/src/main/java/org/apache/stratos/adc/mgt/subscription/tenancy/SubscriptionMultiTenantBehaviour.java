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
import org.apache.stratos.adc.mgt.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.pojo.Properties;


public class SubscriptionMultiTenantBehaviour extends SubscriptionTenancyBehaviour {

    private static Log log = LogFactory.getLog(SubscriptionMultiTenantBehaviour.class);


    public void createSubscription(CartridgeSubscription cartridgeSubscription) throws ADCException, AlreadySubscribedException {

        boolean allowMultipleSubscription = Boolean.
                valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

        if (!allowMultipleSubscription) {
            // If the cartridge is multi-tenant. We should not let users createSubscription twice.
            boolean subscribed;
            try {
                /////////////////////////////////////////////////////////////////////////////////////////////////////////
                //subscribed = PersistenceManager.isAlreadySubscribed(cartridgeSubscription.getType(),
                //        cartridgeSubscription.getSubscriber().getTenantId());
                /////////////////////////////////////////////////////////////////////////////////////////////////////////
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

        //TODO: implement getting cluster Id from DB
        /*TopologyManager.acquireReadLock();

        try {
            Service service = TopologyManager.getTopology().getService(cartridgeSubscription.getType());
            if(service == null) {
                TopologyManager.releaseReadLock();
                String errorMsg = "Error in subscribing, no service found with the name " + cartridgeSubscription.getType();
                log.error(errorMsg);
                throw new ADCException(errorMsg);
            }

            //cartridgeSubscription.getCluster().setClusterDomain(service.getCluster().);
            //cartridgeSubscription.getCluster().setClusterSubDomain(domainContext.getSubDomain());

        } finally {
            TopologyManager.releaseReadLock();
        }*/
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
