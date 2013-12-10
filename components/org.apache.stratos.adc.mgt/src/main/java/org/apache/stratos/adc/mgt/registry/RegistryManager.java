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

package org.apache.stratos.adc.mgt.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.lookup.ClusterIdToCartridgeSubscriptionMap;
import org.apache.stratos.adc.mgt.lookup.SubscriptionAliasToCartridgeSubscriptionMap;
import org.apache.stratos.adc.mgt.utils.Serializer;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;

public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);
    private final static String STRATOS_MANAGER_REOSURCE = "/stratos.manager";
    private static Registry registryService;
    private static RegistryManager registryManager;

    public static RegistryManager getInstance() {

        registryService = DataHolder.getRegistry();

        if (registryManager == null) {
            synchronized (RegistryManager.class) {
                if (registryService == null) {
                    return registryManager;
                }
                registryManager = new RegistryManager();
            }
        }
        return registryManager;
    }

    private RegistryManager() {
        try {
            if (!registryService.resourceExists(STRATOS_MANAGER_REOSURCE)) {
                registryService.put(STRATOS_MANAGER_REOSURCE, registryService.newCollection());
            }
        } catch (RegistryException e) {
            String errorMsg = "Failed to create the registry resource " + STRATOS_MANAGER_REOSURCE;
            log.error(errorMsg, e);;
        }
    }

    public void persistAliastoSubscriptionMap (int tenantId,
                                               SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap)
            throws RegistryException, ADCException {

        try {
            registryService.beginTransaction();
            Resource nodeResource = registryService.newResource();
            nodeResource.setContent(Serializer.serializeAliasToSubscriptionMapToByteArray(aliasToSubscriptionMap));
            registryService.put(STRATOS_MANAGER_REOSURCE + "/subscription/tenant" + Integer.toString(tenantId),
                    nodeResource);
            registryService.commitTransaction();

        } catch (Exception e) {
            String errorMsg = "Failed to persist SubscriptionAliasToCartridgeSubscriptionMap in registry.";
            registryService.rollbackTransaction();
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public Object getAliastoSubscriptionMap (int tenantId) throws ADCException {

        try {
            Resource resource = registryService.get(STRATOS_MANAGER_REOSURCE + "/subscription/tenant" +
                    Integer.toString(tenantId));
            return resource.getContent();

        } catch (ResourceNotFoundException ignore) {
            return null;

        } catch (RegistryException e) {
            String errorMsg = "Failed to retrieve SubscriptionAliasToCartridgeSubscriptionMap from registry.";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public void persistClusterIdToSubscriptionMap (ClusterIdToCartridgeSubscriptionMap clusterIdToSubscriptionMap)
            throws RegistryException, ADCException {

        try {
            registryService.beginTransaction();
            Resource nodeResource = registryService.newResource();
            nodeResource.setContent(Serializer.serializeClusterIdToSubscriptionMapToByteArray(clusterIdToSubscriptionMap));
            registryService.put(STRATOS_MANAGER_REOSURCE + "/subscription/cluster",
                    nodeResource);
            registryService.commitTransaction();

        } catch (Exception e) {
            String errorMsg = "Failed to persist ClusterIdToCartridgeSubscriptionMap in registry.";
            registryService.rollbackTransaction();
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public Object getClusterIdtoSubscriptionMap () throws ADCException {

        try {
            Resource resource = registryService.get(STRATOS_MANAGER_REOSURCE + "/subscription/cluster");
            return resource.getContent();

        } catch (ResourceNotFoundException ignore) {
            return null;

        } catch (RegistryException e) {
            String errorMsg = "Failed to retrieve ClusterIdToCartridgeSubscriptionMap from registry.";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }
}
