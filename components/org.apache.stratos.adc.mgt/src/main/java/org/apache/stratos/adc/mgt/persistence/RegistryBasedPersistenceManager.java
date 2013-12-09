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

import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;

import java.util.List;

public class RegistryBasedPersistenceManager extends PersistenceManager {

    @Override
    public void persistCartridgeSubscription(CartridgeSubscription cartridgeSubscription) throws PersistenceManagerException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeCartridgeSubscription(int tenantId, String alias) throws PersistenceManagerException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CartridgeSubscription getCartridgeSubscription(int tenantId, String alias) throws PersistenceManagerException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<CartridgeSubscription> getCartridgeSubscriptions(int tenantId) throws PersistenceManagerException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CartridgeSubscription getCartridgeSubscription(String clusterDomain) throws PersistenceManagerException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
