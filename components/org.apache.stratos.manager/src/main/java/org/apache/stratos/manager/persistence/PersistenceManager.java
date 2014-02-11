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

package org.apache.stratos.manager.persistence;

import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.subscription.CartridgeSubscription;

import java.util.Collection;

public abstract class PersistenceManager {

    public abstract void persistCartridgeSubscription (CartridgeSubscription cartridgeSubscription)
            throws PersistenceManagerException;

    public abstract void removeCartridgeSubscription (int tenantId, String type, String alias)
            throws PersistenceManagerException;

    public abstract Collection<CartridgeSubscription> getCartridgeSubscriptions()
            throws PersistenceManagerException;

    public abstract Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId)
            throws PersistenceManagerException;

    public abstract void persistService (Service service) throws PersistenceManagerException;

    public abstract Service getService (String cartridgeType) throws PersistenceManagerException;

    public abstract void removeService (String cartridgeType) throws PersistenceManagerException;

}
