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

import org.apache.stratos.adc.mgt.deploy.service.Service;
import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;

import java.util.Collection;

public abstract class PersistenceManager {

    public abstract void persistCartridgeSubscription (CartridgeSubscription cartridgeSubscription)
            throws PersistenceManagerException;

    public abstract void removeCartridgeSubscription (int tenantId, String type, String alias)
            throws PersistenceManagerException;

    //public abstract CartridgeSubscription getCartridgeSubscription(int tenantId, String alias)
    //        throws PersistenceManagerException;

    public abstract Collection<CartridgeSubscription> getCartridgeSubscriptions()
            throws PersistenceManagerException;

    public abstract Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId)
            throws PersistenceManagerException;

    public abstract void persistService (Service service) throws PersistenceManagerException;

    public abstract Service getService (String cartridgeType) throws PersistenceManagerException;

    public abstract void removeService (String cartridgeType) throws PersistenceManagerException;

    //public abstract Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId)
    //        throws PersistenceManagerException;

    //public abstract CartridgeSubscription getCartridgeSubscription (String clusterDomain)
    //        throws PersistenceManagerException;

    //public abstract Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId, String cartridgeType)
    //        throws PersistenceManagerException;

    /*public abstract Repository getRepository (int tenantId, String alias)
            throws PersistenceManagerException;

    public abstract Repository getRepository (String clusterDomain)
            throws PersistenceManagerException;

    public abstract DataCartridge getDataCartridgeSubscriptionInfo (int tenantId, String alias)
            throws PersistenceManagerException;

    public abstract boolean isAliasTaken (int tenantId, String alias)
            throws PersistenceManagerException;

    public abstract boolean hasSubscribed (int tenantId, String cartridgeType)
            throws PersistenceManagerException;

    public abstract void removeDomainMapping (int tenantId, String cartridgeAlias)
            throws PersistenceManagerException;

    public abstract void updateDomianMapping (int tenantId, String cartridgeAlias, String newDomain)
            throws PersistenceManagerException;

    public abstract String getMappedDomain (int tenantId, String cartridgeAlias)
            throws PersistenceManagerException;

    public abstract Cluster getCluster (int tenantId, String cartridgeAlias)
            throws PersistenceManagerException;

    public abstract void updateSubscriptionStatus (int tenantId, String cartridgeAlias, String newStatus)
            throws PersistenceManagerException;

    public abstract void updateServiceStatus (int tenantId, String cartridgeAlias, String newStatus)
            throws PersistenceManagerException;   */
}
