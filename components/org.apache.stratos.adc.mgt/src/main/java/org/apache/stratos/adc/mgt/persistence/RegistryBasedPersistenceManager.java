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
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
import org.apache.stratos.adc.mgt.lookup.ClusterIdToSubscription;
import org.apache.stratos.adc.mgt.lookup.SubscriptionContext;
import org.apache.stratos.adc.mgt.registry.RegistryManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.Deserializer;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.Collection;

public class RegistryBasedPersistenceManager extends PersistenceManager {

    private static final Log log = LogFactory.getLog(RegistryBasedPersistenceManager.class);

    @Override
    public void persistCartridgeSubscription(CartridgeSubscription cartridgeSubscription) throws PersistenceManagerException {

        SubscriptionContext subscriptionContext = new SubscriptionContext();
        subscriptionContext.addSubscription(cartridgeSubscription);

        try {
            RegistryManager.getInstance().persistSubscriptionContext(cartridgeSubscription.getSubscriber().getTenantId(), subscriptionContext);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);
        }
    }

    @Override
    public void removeCartridgeSubscription(int tenantId, String alias) throws PersistenceManagerException {
        //TODO
    }

    @Override
    public CartridgeSubscription getCartridgeSubscription(int tenantId, String alias) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().getSubscriptionContext(tenantId);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
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
    public Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().getSubscriptionContext(tenantId);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
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
            return subscriptionContext.getSubscriptions();
        }

        return null;
    }

    @Override
    public CartridgeSubscription getCartridgeSubscription(String clusterDomain) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().getClusterIdToSubscription();

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
        }

        Object clusterIdToSubscriptionObj;

        try {
            clusterIdToSubscriptionObj = Deserializer.deserializeFromByteArray((byte[]) byteObj);

        } catch (Exception e) {
            throw new PersistenceManagerException(e);
        }

        ClusterIdToSubscription clusterIdToSubscription;
        if (clusterIdToSubscriptionObj instanceof ClusterIdToSubscription) {
            clusterIdToSubscription = (ClusterIdToSubscription) clusterIdToSubscriptionObj;
            return clusterIdToSubscription.getSubscription(clusterDomain);
        }

        return null;
    }

    @Override
    public Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId, String cartridgeType) throws PersistenceManagerException {

        Object byteObj;

        try {
            byteObj = RegistryManager.getInstance().getSubscriptionContext(tenantId);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);
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
            return subscriptionContext.getSubscriptionsOfType(cartridgeType);
        }

        return null;
    }
}
