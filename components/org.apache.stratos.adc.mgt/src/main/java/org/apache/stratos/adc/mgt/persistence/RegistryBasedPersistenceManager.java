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
import org.apache.stratos.adc.mgt.lookup.ClusterIdToCartridgeSubscriptionMap;
import org.apache.stratos.adc.mgt.lookup.SubscriptionAliasToCartridgeSubscriptionMap;
import org.apache.stratos.adc.mgt.registry.RegistryManager;
import org.apache.stratos.adc.mgt.utils.Deserializer;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

public class RegistryBasedPersistenceManager extends PersistenceManager {

    private static final Log log = LogFactory.getLog(RegistryBasedPersistenceManager.class);

    @Override
    public void persistCartridgeSubscriptions(int tenantId, SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap) throws PersistenceManagerException {

        try {
            RegistryManager.getInstance().persistAliastoSubscriptionMap(tenantId, aliasToSubscriptionMap);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);
        }
    }

    @Override
    public SubscriptionAliasToCartridgeSubscriptionMap retrieveCartridgeSubscriptions(int tenantId) throws PersistenceManagerException {

        Object aliasToSubscriptionMapObj;

        try {
            aliasToSubscriptionMapObj = RegistryManager.getInstance().getAliastoSubscriptionMap(tenantId);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);
        }

        if (aliasToSubscriptionMapObj != null) {
            try {
                Object dataObj = Deserializer
                        .deserializeFromByteArray((byte[]) aliasToSubscriptionMapObj);
                if(dataObj instanceof SubscriptionAliasToCartridgeSubscriptionMap) {
                    return (SubscriptionAliasToCartridgeSubscriptionMap) dataObj;
                } else {
                    return null;
                }

            } catch (Exception e) {
                String errorMsg = "Unable to retrieve data from Registry. Hence, any historical data will not get reflected.";
                log.warn(errorMsg, e);
            }
        }

        return null;
    }

    @Override
    public void persistCartridgeSubscriptions(String clusterId, ClusterIdToCartridgeSubscriptionMap clusterIdToSubscriptionMap) throws PersistenceManagerException {

        try {
            RegistryManager.getInstance().persistClusterIdToSubscriptionMap(clusterIdToSubscriptionMap);

        } catch (RegistryException e) {
            throw new PersistenceManagerException(e);

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);
        }
    }

    @Override
    public ClusterIdToCartridgeSubscriptionMap retrieveCartridgeSubscriptions(String clusterId) throws PersistenceManagerException {

        Object clusterIdToSubscriptionMapObj;

        try {
            clusterIdToSubscriptionMapObj = RegistryManager.getInstance().getClusterIdtoSubscriptionMap();

        } catch (ADCException e) {
            throw new PersistenceManagerException(e);
        }

        if (clusterIdToSubscriptionMapObj != null) {
            try {
                Object dataObj = Deserializer
                        .deserializeFromByteArray((byte[]) clusterIdToSubscriptionMapObj);
                if(dataObj instanceof ClusterIdToCartridgeSubscriptionMap) {
                    return (ClusterIdToCartridgeSubscriptionMap) dataObj;
                } else {
                    return null;
                }

            } catch (Exception e) {
                String errorMsg = "Unable to retrieve data from Registry. Hence, any historical data will not get reflected.";
                log.warn(errorMsg, e);
            }
        }

        return null;
    }
}
