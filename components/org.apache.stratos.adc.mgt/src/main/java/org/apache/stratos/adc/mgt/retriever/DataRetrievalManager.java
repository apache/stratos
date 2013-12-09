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

package org.apache.stratos.adc.mgt.retriever;

import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
import org.apache.stratos.adc.mgt.lookup.LookupDataHolder;
import org.apache.stratos.adc.mgt.persistence.PersistenceManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataRetrievalManager {

    private static final Log log = LogFactory.getLog(DataRetrievalManager.class);

    private PersistenceManager persistenceManager;
    private LookupDataHolder lookupDataHolder;
    private ExecutorService cartridgeSubscriptionUpdateThreadPool = null;

    public DataRetrievalManager (PersistenceManager persistenceManager, LookupDataHolder lookupDataHolder) {
        this.persistenceManager = persistenceManager;
        this.lookupDataHolder = lookupDataHolder;
        cartridgeSubscriptionUpdateThreadPool = Executors.newCachedThreadPool();
    }

    public CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias)
            throws PersistenceManagerException {

        CartridgeSubscription cartridgeSubscription = null;

        if(lookupDataHolder != null) {
            //look in the local cache
            cartridgeSubscription = lookupDataHolder.getCartridgeSubscription(tenantId, subscriptionAlias);
        }
        //if not found in the local cache, look in the Persistence Manager
        if (cartridgeSubscription == null) {
            persistenceManager.getCartridgeSubscription(tenantId, subscriptionAlias);
        } else {
            if(log.isDebugEnabled()) {
                log.debug("Cartridge subscription entry for tenant Id " + tenantId + ", subscription alias " +
                        subscriptionAlias + " found in the local cache");
            }
        }

        return cartridgeSubscription;
    }

    public CartridgeSubscription getCartridgeSubscription (String clusterId)
            throws PersistenceManagerException {

        CartridgeSubscription cartridgeSubscription = null;

        if(lookupDataHolder != null) {
            //look in the local cache
            cartridgeSubscription = lookupDataHolder.getCartridgeSubscription(clusterId);
        }
        //if not found in the local cache, look in the Persistence Manager
        if (cartridgeSubscription == null) {
            persistenceManager.getCartridgeSubscription(clusterId);

        } else {
            if(log.isDebugEnabled()) {
                log.debug("Cartridge subscription entry for cluster domain " + clusterId +
                        " found in the local cache");
            }
        }

        return cartridgeSubscription;
    }

    public List<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) throws PersistenceManagerException {

        Collection<CartridgeSubscription> cartridgeSubscriptionCollection = null;

        if(lookupDataHolder != null) {
            //look in the local cache
            cartridgeSubscriptionCollection = lookupDataHolder.getCartridgeSubscriptions(tenantId);
        }
        //if not found in the local cache, look in the Persistence Manager
        if (cartridgeSubscriptionCollection == null) {
            persistenceManager.getCartridgeSubscriptions(tenantId);

        } else {
            if(log.isDebugEnabled()) {
                log.debug("Cartridge subscription entries for tenant " + tenantId +
                        " found in the local cache");
            }
        }

        List<CartridgeSubscription> cartridgeSubscriptionList = new ArrayList<CartridgeSubscription>();
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptionCollection) {
            cartridgeSubscriptionList.add(cartridgeSubscription);
        }

        return cartridgeSubscriptionList;
    }

    public void putCartridgeSubscription (CartridgeSubscription cartridgeSubscription) {

        cartridgeSubscriptionUpdateThreadPool.submit(new CartridgeSubscriptionUpdater(cartridgeSubscription,
                lookupDataHolder, persistenceManager));
    }

    private class CartridgeSubscriptionUpdater implements Runnable {

        CartridgeSubscription cartridgeSubscription;
        LookupDataHolder lookupDataHolder;
        PersistenceManager persistenceManager;

        public CartridgeSubscriptionUpdater (CartridgeSubscription cartridgeSubscription, LookupDataHolder
                lookupDataHolder, PersistenceManager persistenceManager) {

            this.cartridgeSubscription = cartridgeSubscription;
            this.lookupDataHolder = lookupDataHolder;
            this.persistenceManager = persistenceManager;
        }

        public void run() {

            if(lookupDataHolder != null) {
                lookupDataHolder.addCartridgeSubscription(cartridgeSubscription.getSubscriber().getTenantId(),
                        cartridgeSubscription.getAlias(), cartridgeSubscription);
            }
            try {
                persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in persisting Cartridge Subscription instance";
                log.error(errorMsg, e);
            }
        }
    }

}
