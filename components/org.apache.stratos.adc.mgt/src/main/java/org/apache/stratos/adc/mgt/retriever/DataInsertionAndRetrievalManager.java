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
import org.apache.stratos.adc.mgt.persistence.RegistryBasedPersistenceManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataInsertionAndRetrievalManager {

    private static final Log log = LogFactory.getLog(DataInsertionAndRetrievalManager.class);

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    // TODO: use a global object
    PersistenceManager persistenceManager = new RegistryBasedPersistenceManager();

    public void putCartridgeSubscription (CartridgeSubscription cartridgeSubscription) {

        writeLock.lock();

        try {
            // store in LookupDataHolder
            LookupDataHolder.getInstance().put(cartridgeSubscription);

            try {
                // store in Persistence Manager
                persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

            } catch (PersistenceManagerException e) {
                log.error("Error in persisting CartridgeSubscription in Persistence Manager", e);
            }

        } finally {
            writeLock.unlock();
        }
    }

    public void persistAll (int tenantId) {

        Collection<CartridgeSubscription> cartridgeSubscriptions = LookupDataHolder.getInstance().getSubscriptions(tenantId);

        writeLock.lock();

        try {
            for(CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
                try {
                    // store in Persistence Manager
                    persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

                } catch (PersistenceManagerException e) {
                    log.error("Error in persisting CartridgeSubscription in Persistence Manager", e);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias) {

        CartridgeSubscription cartridgeSubscription = LookupDataHolder.getInstance().getSubscriptionForAlias(tenantId, subscriptionAlias);
        if (cartridgeSubscription == null) {
            // not available in the cache, look in the registry
            if (log.isDebugEnabled()) {
                log.debug("CartridgeSubscription for tenant " + tenantId + ", alias " + subscriptionAlias + " not available in memory");
            }

            try {
                cartridgeSubscription = persistenceManager.getCartridgeSubscription(tenantId, subscriptionAlias);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in accessing Persistence Manager";
                log.error(errorMsg, e);
                return null;
            }

            // add to the LookupDataHolder
            LookupDataHolder.getInstance().put(cartridgeSubscription);
        }

        return cartridgeSubscription;
    }

    public CartridgeSubscription getCartridgeSubscription (String clusterId) {

        CartridgeSubscription cartridgeSubscription = LookupDataHolder.getInstance().getSubscription(clusterId);
        if (cartridgeSubscription == null) {
            // not available in the cache, look in the registry
            if (log.isDebugEnabled()) {
                log.debug("CartridgeSubscription for cluster " + clusterId + " not available in memory");
            }

            try {
                cartridgeSubscription = persistenceManager.getCartridgeSubscription(clusterId);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in accessing Persistence Manager";
                log.error(errorMsg, e);
                return null;
            }

            // add to the LookupDataHolder
            LookupDataHolder.getInstance().put(cartridgeSubscription);
        }

        return cartridgeSubscription;
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) {

        Collection<CartridgeSubscription> cartridgeSubscriptions = LookupDataHolder.getInstance().getSubscriptions(tenantId);
        if (cartridgeSubscriptions == null) {
            // not available in the cache, look in the registry
            if (log.isDebugEnabled()) {
                log.debug("CartridgeSubscriptions for tenant " + tenantId + " not available in memory");
            }

            try {
                cartridgeSubscriptions = persistenceManager.getCartridgeSubscriptions(tenantId);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in accessing Persistence Manager";
                log.error(errorMsg, e);
                return null;
            }

            // add to the LookupDataHolder
            Iterator<CartridgeSubscription> iterator = cartridgeSubscriptions.iterator();
            while (iterator.hasNext()) {
                LookupDataHolder.getInstance().put(iterator.next());
            }
        }

        return cartridgeSubscriptions;
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId, String cartridgeType) {

        Collection<CartridgeSubscription> cartridgeSubscriptions = LookupDataHolder.getInstance().getSubscriptionForType(tenantId, cartridgeType);
        if (cartridgeSubscriptions == null) {
            // not available in the cache, look in the registry
            if (log.isDebugEnabled()) {
                log.debug("CartridgeSubscriptions for tenant " + tenantId + ", type " + cartridgeType + " not available in memory");
            }

            try {
                cartridgeSubscriptions = persistenceManager.getCartridgeSubscriptions(tenantId, cartridgeType);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in accessing Persistence Manager";
                log.error(errorMsg, e);
                return null;
            }

            // add to the LookupDataHolder
            Iterator<CartridgeSubscription> iterator = cartridgeSubscriptions.iterator();
            while (iterator.hasNext()) {
                LookupDataHolder.getInstance().put(iterator.next());
            }
        }

        return cartridgeSubscriptions;
    }
}
