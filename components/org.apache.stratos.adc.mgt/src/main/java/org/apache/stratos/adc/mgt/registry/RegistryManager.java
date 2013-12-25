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
import org.apache.stratos.adc.mgt.lookup.ClusterIdToSubscription;
import org.apache.stratos.adc.mgt.lookup.SubscriptionContext;
import org.apache.stratos.adc.mgt.utils.Serializer;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);
                                                                                                                          ;
    private final static String STRATOS_MANAGER_REOSURCE = "/stratos.manager";
    private final static String CLUSTER_ID_TO_SUBSCRIPTION = "/clusterIdToSubscription";
    private final static String TENENTID_TO_SUBSCRIPTION_CONTEXT = "/tenantIdToSubscriptionContext";

    private static RegistryService registryService;
    private static volatile RegistryManager registryManager;

    public static RegistryManager getInstance() {

        registryService = DataHolder.getRegistryService();

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

    }

    private UserRegistry initRegistry (int tenantId) throws RegistryException, ADCException {

        UserRegistry tenantGovRegistry = registryService.getGovernanceSystemRegistry(tenantId);
        if (tenantGovRegistry == null) {
            String errorMsg = "Tenant " + tenantId + "'s governance registry is not initialized";
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        synchronized (RegistryManager.class) {
            // check if the resource is available, else create it
            try {
                if (!tenantGovRegistry.resourceExists(STRATOS_MANAGER_REOSURCE)) {
                    tenantGovRegistry.put(STRATOS_MANAGER_REOSURCE, tenantGovRegistry.newCollection());
                }
            } catch (RegistryException e) {
                String errorMsg = "Failed to create the registry resource " + STRATOS_MANAGER_REOSURCE;
                log.error(errorMsg, e);
                throw new ADCException(errorMsg, e);
            }
        }

        return tenantGovRegistry;
    }

    private UserRegistry initRegistry () throws RegistryException, ADCException {

        UserRegistry govRegistry = registryService.getGovernanceSystemRegistry();
        if (govRegistry == null) {
            String errorMsg = "Governance registry is not initialized";
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        synchronized (RegistryManager.class) {
            // check if the resource is available, else create it
            try {
                if (!govRegistry.resourceExists(STRATOS_MANAGER_REOSURCE)) {
                    govRegistry.put(STRATOS_MANAGER_REOSURCE, govRegistry.newCollection());
                }
            } catch (RegistryException e) {
                String errorMsg = "Failed to create the registry resource " + STRATOS_MANAGER_REOSURCE;
                log.error(errorMsg, e);
                throw new ADCException(errorMsg, e);
            }
        }

        return govRegistry;
    }

    public void persistSubscriptionContext(int tenantId, SubscriptionContext subscriptionContext)
            throws RegistryException, ADCException {

        //TODO: uncomment
        //UserRegistry tenantGovRegistry = initRegistry(tenantId);
        //temporary
        UserRegistry tenantGovRegistry = initRegistry();

        try {
            tenantGovRegistry.beginTransaction();
            Resource nodeResource = tenantGovRegistry.newResource();
            nodeResource.setContent(Serializer.serializeSubscriptionSontextToByteArray(subscriptionContext));
            //TODO: uncomment
            //tenantGovRegistry.put(STRATOS_MANAGER_REOSURCE + TENENTID_TO_SUBSCRIPTION_CONTEXT, nodeResource);
            //temporary
            tenantGovRegistry.put(STRATOS_MANAGER_REOSURCE + TENENTID_TO_SUBSCRIPTION_CONTEXT + "/" + Integer.toString(tenantId), nodeResource);
            tenantGovRegistry.commitTransaction();

        } catch (Exception e) {
            String errorMsg = "Failed to persist SubscriptionContext in registry.";
            tenantGovRegistry.rollbackTransaction();
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public Object getSubscriptionContext(int tenantId) throws ADCException, RegistryException {

        //TODO: uncomment
        //UserRegistry tenantGovRegistry = registryService.getGovernanceSystemRegistry(tenantId);
        //temprary
        UserRegistry tenantGovRegistry = registryService.getGovernanceSystemRegistry();

        if (tenantGovRegistry == null) {
            String errorMsg = "Tenant " + tenantId + "'s governance registry is not initialized";
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        try {
            //TODO: uncomment
            //Resource resource = tenantGovRegistry.get(STRATOS_MANAGER_REOSURCE + TENENTID_TO_SUBSCRIPTION_CONTEXT);
            //temporary
            Resource resource = tenantGovRegistry.get(STRATOS_MANAGER_REOSURCE + TENENTID_TO_SUBSCRIPTION_CONTEXT + "/" + Integer.toString(tenantId));
            return resource.getContent();

        } catch (ResourceNotFoundException ignore) {
            log.error("Sepcified resource not found at " + STRATOS_MANAGER_REOSURCE + TENENTID_TO_SUBSCRIPTION_CONTEXT);
            return null;

        } catch (RegistryException e) {
            String errorMsg = "Failed to retrieve SubscriptionContext from registry.";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public void persistClusterIdToSubscription (ClusterIdToSubscription clusterIdToSubscription)
            throws RegistryException, ADCException {

        UserRegistry govRegistry = initRegistry();

        try {
            govRegistry.beginTransaction();
            Resource nodeResource = govRegistry.newResource();
            nodeResource.setContent(Serializer.serializeClusterIdToSubscriptionToByteArray(clusterIdToSubscription));
            govRegistry.put(STRATOS_MANAGER_REOSURCE + CLUSTER_ID_TO_SUBSCRIPTION, nodeResource);
            govRegistry.commitTransaction();

        } catch (Exception e) {
            String errorMsg = "Failed to persist ClusterIdToSubscription in registry.";
            govRegistry.rollbackTransaction();
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public Object getClusterIdToSubscription () throws ADCException, RegistryException {

        UserRegistry govRegistry = registryService.getGovernanceSystemRegistry();
        if (govRegistry == null) {
            String errorMsg = "Governance registry is not initialized";
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        try {
            Resource resource = govRegistry.get(STRATOS_MANAGER_REOSURCE + CLUSTER_ID_TO_SUBSCRIPTION);
            return resource.getContent();

        } catch (ResourceNotFoundException ignore) {
            return null;

        } catch (RegistryException e) {
            String errorMsg = "Failed to retrieve ClusterIdToSubscription from registry.";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }
}
