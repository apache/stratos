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

package org.apache.stratos.manager.retriever;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.composite.application.beans.ApplicationDefinition;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.lookup.LookupDataHolder;
import org.apache.stratos.manager.persistence.PersistenceManager;
import org.apache.stratos.manager.persistence.RegistryBasedPersistenceManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.ApplicationSubscription;
import org.apache.stratos.manager.subscription.GroupSubscription;

import java.util.Collection;
import java.util.Set;

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
                // remove from the in memory model since persisting failed
                LookupDataHolder.getInstance().removeSubscription(cartridgeSubscription.getSubscriber().getTenantId(), cartridgeSubscription.getType(),
                        cartridgeSubscription.getAlias(), cartridgeSubscription.getClusterDomain(),
                        cartridgeSubscription.getRepository() != null ? cartridgeSubscription.getRepository().getUrl() : null);

                throw e;
            }

        } finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cacheAndUpdateSubscription(CartridgeSubscription cartridgeSubscription) throws PersistenceManagerException {

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // store in LookupDataHolder
            LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);

            try {
                // store in Persistence Manager
                persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in updating cartridge subscription in persistence manager";
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

        if (cartridgeSubscription == null) {
            if (log.isDebugEnabled()) {
                log.debug("No CartridgeSubscription found for tenant " + tenantId + ", subscription alias " + subscriptionAlias);
            }
            return;
        }

        String cartridgeType = cartridgeSubscription.getType();
        String clusterId = cartridgeSubscription.getClusterDomain();

        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // remove from persistence manager
            try {
                persistenceManager.removeCartridgeSubscription(tenantId, cartridgeType, subscriptionAlias);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error in removing CartridgeSubscription from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

            // remove from cache
            LookupDataHolder.getInstance().removeSubscription(tenantId, cartridgeType, subscriptionAlias, clusterId,
                    cartridgeSubscription.getRepository() != null ? cartridgeSubscription.getRepository().getUrl() : null);

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

    public void cacheSubscriptionsWithoutPersisting (Collection<CartridgeSubscription> cartridgeSubscriptions) {

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            cacheSubscriptions(cartridgeSubscriptions);

        } finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void removeSubscriptionFromCache (int tenantId, String subscriptionAlias) {

        LookupDataHolder.getInstance().acquireWriteLock();

        CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);
        if (cartridgeSubscription == null) {
            if (log.isDebugEnabled()) {
                log.debug("No CartridgeSubscription found for tenant " + tenantId + ", subscription alias " + subscriptionAlias);
            }
            return;
        }

        String cartridgeType = cartridgeSubscription.getType();
        String clusterId = cartridgeSubscription.getClusterDomain();

        try {
            // remove from cache
            LookupDataHolder.getInstance().removeSubscription(tenantId, cartridgeType, subscriptionAlias, clusterId,
                    cartridgeSubscription.getRepository() != null ? cartridgeSubscription.getRepository().getUrl() : null);

        } finally {
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

    public void persistService (Service service) throws PersistenceManagerException {

        persistenceManager.persistService(service);
    }

    public Collection<Service> getServices() throws PersistenceManagerException {

        return persistenceManager.getServices();
    }

    public Service getService (String cartridgeType) throws PersistenceManagerException {

        return persistenceManager.getService(cartridgeType);
    }

    public void removeService (String cartridgeType) throws PersistenceManagerException {

        persistenceManager.removeService(cartridgeType);
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions(String cartridgeType) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptions(cartridgeType);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionForAlias(tenantId, subscriptionAlias);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Set<CartridgeSubscription> getCartridgeSubscriptionForCluster (String clusterId) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscription(clusterId);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Set<CartridgeSubscription> getCartridgeSubscriptionForRepository (String repoUrl) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionsForRepoUrl(repoUrl);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptions(tenantId);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId, String cartridgeType) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionForType(tenantId, cartridgeType);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    //Don't use this method unless absolutely necessary, use getCartridgeSubscription (int tenantId, String subscriptionAlias)
    public CartridgeSubscription getCartridgeSubscriptionForAlias (String subscriptionAlias) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionForAlias(subscriptionAlias);

        } finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public void peristServiceGroupDefinition (ServiceGroupDefinition serviceGroupDefinition) throws PersistenceManagerException {

        persistenceManager.persistServiceGroupDefinition(serviceGroupDefinition);
    }

    public ServiceGroupDefinition getServiceGroupDefinition (String serviceGroupDefinitionName) throws PersistenceManagerException {

        return persistenceManager.getServiceGroupDefinition(serviceGroupDefinitionName);
    }

    public void removeServiceGroupDefinition (String serviceGroupName) throws PersistenceManagerException {

        persistenceManager.removeServiceGroupDefinition(serviceGroupName);
    }

    public void persistGroupSubscription (GroupSubscription groupSubscription) throws PersistenceManagerException {

        persistenceManager.persistGroupSubscription(groupSubscription);
    }

    public GroupSubscription getGroupSubscription (int tenantId, String groupName, String groupAlias) throws PersistenceManagerException {

        return persistenceManager.getGroupSubscription(tenantId, groupName, groupAlias);
    }

    public void removeGroupSubscription (int tenantId, String groupName, String groupAlias) throws PersistenceManagerException {

        persistenceManager.removeGroupSubscription(tenantId, groupName, groupAlias);
    }

    public void persistApplicationSubscription (ApplicationSubscription compositeAppSubscription) throws PersistenceManagerException {

        persistenceManager.persistCompositeAppSubscription(compositeAppSubscription);
    }

    public ApplicationSubscription getApplicationSubscription (int tenantId, String appId) throws PersistenceManagerException {

        return persistenceManager.getCompositeAppSubscription(tenantId, appId);
    }

    public void removeApplicationSubscription (int tenantId, String appId) throws PersistenceManagerException {

        persistenceManager.removeCompositeAppSubscription(tenantId, appId);
    }
}
