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

package org.apache.stratos.adc.mgt.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
import org.apache.stratos.adc.mgt.lookup.ClusterIdToSubscription;
import org.apache.stratos.adc.mgt.lookup.SubscriptionContext;
import org.apache.stratos.adc.mgt.registry.RegistryManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.Deserializer;
import org.apache.stratos.adc.mgt.utils.Serializer;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegistryBasedPersistenceManager extends PersistenceManager {

    private static final Log log = LogFactory.getLog(RegistryBasedPersistenceManager.class);
    // Registry paths
    private static final String STRATOS_MANAGER_REOSURCE = "/stratos.manager";
    private static final String CLUSTER_ID_TO_SUBSCRIPTION = "/clusterIdToSubscription";
    private static final String TENANT_ID_TO_SUBSCRIPTION_CONTEXT = "/tenantIdToSubscriptionContext";

    @Override
    public void persistCartridgeSubscription (CartridgeSubscription cartridgeSubscription) throws PersistenceManagerException {

        SubscriptionContext subscriptionContext = new SubscriptionContext();
        subscriptionContext.addSubscription(cartridgeSubscription);

        //TODO: need to synchronize?
        // persist in the path TENANT_ID_TO_SUBSCRIPTION_CONTEXT
        try {
            RegistryManager.getInstance().persist(STRATOS_MANAGER_REOSURCE + TENANT_ID_TO_SUBSCRIPTION_CONTEXT + "/" +
                    Integer.toString(cartridgeSubscription.getSubscriber().getTenantId()), Serializer.serializeSubscriptionSontextToByteArray(subscriptionContext));

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);

        } catch (IOException e) {
            throw new PersistenceManagerException(e);
        }

        // persist in the path CLUSTER_ID_TO_SUBSCRIPTION
        try {
            RegistryManager.getInstance().persist(STRATOS_MANAGER_REOSURCE + CLUSTER_ID_TO_SUBSCRIPTION + "/" +
                    cartridgeSubscription.getClusterDomain(), Serializer.serializeSubscriptionSontextToByteArray(subscriptionContext));

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);

        } catch (IOException e) {
            throw new PersistenceManagerException(e);
        }
    }

    @Override
    public void removeCartridgeSubscription (int tenantId, String alias) throws PersistenceManagerException {
        //TODO
    }

    @Override
    public CartridgeSubscription getCartridgeSubscription (int tenantId, String alias) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().retrieve(STRATOS_MANAGER_REOSURCE + TENANT_ID_TO_SUBSCRIPTION_CONTEXT + "/" +
                Integer.toString(tenantId));

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
        }

        if (byteObj == null) {
            return null;
        }

        Object subscriptionContextObj;

        try {
            subscriptionContextObj = Deserializer.deserializeFromByteArray((byte[]) byteObj);

        } catch (Exception e) {
            throw new PersistenceManagerException(e);
        }

        SubscriptionContext subscriptionContext;
        if (subscriptionContextObj instanceof SubscriptionContext) {
            subscriptionContext = (SubscriptionContext) subscriptionContextObj;
            return subscriptionContext.getSubscriptionForAlias(alias);
        }

        return null;
    }

    @Override
    public List<CartridgeSubscription> getCartridgeSubscriptions () throws PersistenceManagerException {

        Object resourceObj;

        try {
            resourceObj = RegistryManager.getInstance().retrieve(STRATOS_MANAGER_REOSURCE + TENANT_ID_TO_SUBSCRIPTION_CONTEXT);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
        }

        if ((resourceObj == null) || !(resourceObj instanceof String[])) {
            return null;
        }

        // get the paths for all SubscriptionContext instnaces
        String[] subscriptionCtxtResourcePaths = (String[]) resourceObj;

        List<CartridgeSubscription> cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
        //for each path, get the SubscriptionContext instance
        for (String subscriptionCtxResourcePath : subscriptionCtxtResourcePaths) {

            Object serializedSubscriptionCtxObj = null;
            try {
                serializedSubscriptionCtxObj = RegistryManager.getInstance().retrieve(subscriptionCtxResourcePath);

            } catch (RegistryException e) {
                // issue might be at only this path, therefore log and continue
                log.error("Error while retrieving Resource at " + subscriptionCtxResourcePath, e);
                continue;
            }

            //De-serialize
            Object subscriptionCtxObj = null;
            try {
                subscriptionCtxObj = Deserializer.deserializeFromByteArray((byte[]) serializedSubscriptionCtxObj);

            } catch (Exception e) {
                // issue might be de-serializing only this object, therefore log and continue
                log.error("Error while de-serializing the object retrieved from "  + subscriptionCtxResourcePath, e);
                continue;
            }

            if (subscriptionCtxObj != null && subscriptionCtxObj instanceof SubscriptionContext) {
                SubscriptionContext subscriptionContext = (SubscriptionContext) subscriptionCtxObj;
                cartridgeSubscriptions.addAll(subscriptionContext.getSubscriptions());
            }
        }

        return cartridgeSubscriptions;
    }

    @Override
    public List<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().retrieve(STRATOS_MANAGER_REOSURCE + TENANT_ID_TO_SUBSCRIPTION_CONTEXT + "/" + Integer.toString(tenantId));

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
        }

        Object subscriptionContextObj;

        try {
            subscriptionContextObj = Deserializer.deserializeFromByteArray((byte[]) byteObj);

        } catch (Exception e) {
            throw new PersistenceManagerException(e);
        }

        List<CartridgeSubscription> cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
        if (subscriptionContextObj instanceof SubscriptionContext) {
            //get all Subscriptions for this tenant
            cartridgeSubscriptions.addAll(((SubscriptionContext) subscriptionContextObj).getSubscriptions());
        }

        return cartridgeSubscriptions;
    }

    @Override
    public CartridgeSubscription getCartridgeSubscription (String clusterDomain) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().retrieve(STRATOS_MANAGER_REOSURCE + CLUSTER_ID_TO_SUBSCRIPTION + "/" + clusterDomain);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
        }

        Object clusterIdToSubscriptionObj;

        try {
            clusterIdToSubscriptionObj = Deserializer.deserializeFromByteArray((byte[]) byteObj);

        } catch (Exception e) {
            throw new PersistenceManagerException(e);
        }

        if (clusterIdToSubscriptionObj instanceof ClusterIdToSubscription) {
            ((ClusterIdToSubscription) clusterIdToSubscriptionObj).getSubscription(clusterDomain);
        }

        return null;
    }

    @Override
    public List<CartridgeSubscription> getCartridgeSubscriptions (int tenantId, String cartridgeType) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().retrieve(STRATOS_MANAGER_REOSURCE + TENANT_ID_TO_SUBSCRIPTION_CONTEXT + "/" + Integer.toString(tenantId));

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
        }

        Object subscriptionContextObj;

        try {
            subscriptionContextObj = Deserializer.deserializeFromByteArray((byte[]) byteObj);

        } catch (Exception e) {
            throw new PersistenceManagerException(e);
        }

        List<CartridgeSubscription> cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
        if (subscriptionContextObj instanceof SubscriptionContext) {
            //get all Subscriptions for this tenant and the type
            cartridgeSubscriptions.addAll(((SubscriptionContext) subscriptionContextObj).getSubscriptionsOfType(cartridgeType));
        }

        return cartridgeSubscriptions;
    }
}
