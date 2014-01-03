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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
import org.apache.stratos.adc.mgt.lookup.LookupDataHolder;
import org.apache.stratos.adc.mgt.persistence.PersistenceManager;
import org.apache.stratos.adc.mgt.persistence.RegistryBasedPersistenceManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;

import java.util.Collection;

public class DataInsertionAndRetrievalManager {

    private static final Log log = LogFactory.getLog(DataInsertionAndRetrievalManager.class);

    // TODO: use a global object
    private static PersistenceManager persistenceManager = new RegistryBasedPersistenceManager();

    public void cacheAndPersistSubcription (CartridgeSubscription cartridgeSubscription) throws PersistenceManagerException {

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // store in LookupDataHolder
            LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);

            try {
                // store in Persistence Manager
                persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in persisting CartridgeSubscription in Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

        } finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void removeSubscription (int tenantId, String subscriptionAlias) throws PersistenceManagerException {

        CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);

        String cartridgeType = cartridgeSubscription.getType();
        String clusterId = cartridgeSubscription.getClusterDomain();

        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // remove from cache
            LookupDataHolder.getInstance().removeSubscription(tenantId, cartridgeType, subscriptionAlias, clusterId);

            // remove from persistence manager
            try {
                persistenceManager.removeCartridgeSubscription(tenantId, cartridgeType, subscriptionAlias);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in removing CartridgeSubscription from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

        } finally {
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cachePersistedSubscriptions () throws PersistenceManagerException {

        Collection<CartridgeSubscription> cartridgeSubscriptions;

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            try {
                cartridgeSubscriptions = persistenceManager.getCartridgeSubscriptions();

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in retrieving CartridgeSubscriptions from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

            if(cartridgeSubscriptions == null || cartridgeSubscriptions.isEmpty()) {
                if(log.isDebugEnabled()) {
                    log.debug("No CartridgeSubscriptions found to add to the cache");
                }
                return;
            }
            cacheSubscriptions(cartridgeSubscriptions);

        } finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cachePersistedSubscriptions (int tenantId) throws PersistenceManagerException {

        Collection<CartridgeSubscription> cartridgeSubscriptions;

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            try {
                cartridgeSubscriptions = persistenceManager.getCartridgeSubscriptions(tenantId);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in retrieving CartridgeSubscriptions from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

            if(cartridgeSubscriptions == null || cartridgeSubscriptions.isEmpty()) {
                if(log.isDebugEnabled()) {
                    log.debug("No CartridgeSubscriptions found to add to the cache");
                }
                return;
            }
            cacheSubscriptions(cartridgeSubscriptions);

        } finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    private void cacheSubscriptions (Collection<CartridgeSubscription> cartridgeSubscriptions) {

        // cache all
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);
            if (log.isDebugEnabled()) {
                log.debug("Updated the in memory cache with the CartridgeSubscription: " + cartridgeSubscription.toString());
            }
        }
    }

    /*public void persistAll (int tenantId) {

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
    }*/

    public CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            CartridgeSubscription cartridgeSubscription = LookupDataHolder.getInstance().getSubscriptionForAlias(tenantId, subscriptionAlias);

            /*if (cartridgeSubscription == null) {
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
                // LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);
            }*/

            return cartridgeSubscription;

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public CartridgeSubscription getCartridgeSubscription (String clusterId) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            CartridgeSubscription cartridgeSubscription = LookupDataHolder.getInstance().getSubscription(clusterId);
            /*if (cartridgeSubscription == null) {
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
                // LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);
            }*/

            return cartridgeSubscription;

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            Collection<CartridgeSubscription> cartridgeSubscriptions = LookupDataHolder.getInstance().getSubscriptions(tenantId);
            /*if (cartridgeSubscriptions == null) {
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
                //Iterator<CartridgeSubscription> iterator = cartridgeSubscriptions.iterator();
                //while (iterator.hasNext()) {
                //    LookupDataHolder.getInstance().putSubscription(iterator.next());
                //}
            }*/

            return cartridgeSubscriptions;

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId, String cartridgeType) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {

            Collection<CartridgeSubscription> cartridgeSubscriptions = LookupDataHolder.getInstance().getSubscriptionForType(tenantId, cartridgeType);
            /*if (cartridgeSubscriptions == null) {
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
                // Iterator<CartridgeSubscription> iterator = cartridgeSubscriptions.iterator();
                // while (iterator.hasNext()) {
                //    LookupDataHolder.getInstance().putSubscription(iterator.next());
                //}
            }*/

            return cartridgeSubscriptions;

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }
}
