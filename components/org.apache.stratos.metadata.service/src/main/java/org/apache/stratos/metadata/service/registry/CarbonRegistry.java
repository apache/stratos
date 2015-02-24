/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadata.service.registry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadata.service.definition.NewProperty;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.*;


/**
 * Carbon registry implementation
 */

public class CarbonRegistry implements DataStore {

    private static final String mainResource = "metadata/";
    private static Log log = LogFactory.getLog(CarbonRegistry.class);
    @Context
    HttpServletRequest httpServletRequest;

    public CarbonRegistry() {
    }


    /**
     * Get Properties of clustor
     *
     * @param applicationName
     * @param clusterId
     * @return
     * @throws RegistryException
     */
    public List<NewProperty> getPropertiesOfCluster(String applicationName, String clusterId) throws RegistryException {
        Registry tempRegistry = getRegistry();
        String resourcePath = mainResource + applicationName + "/" + clusterId;


        if (!tempRegistry.resourceExists(resourcePath)) {
            return null;
            //throw new RegistryException("Cluster does not exist at " + resourcePath);
        }

        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        Resource regResource = tempRegistry.get(resourcePath);

        ArrayList<NewProperty> newProperties = new ArrayList<NewProperty>();

        Properties props = regResource.getProperties();
        Enumeration<?> x = props.propertyNames();
        while (x.hasMoreElements()) {
            String key = (String) x.nextElement();
            List<String> values = regResource.getPropertyValues(key);
            NewProperty property = new NewProperty();
            property.setKey(key);
            String[] valueArr = new String[values.size()];
            property.setValues(values.toArray(valueArr));

            newProperties.add(property);
        }

        return newProperties;
    }

    /**
     * Add property to cluster
     *
     * @param applicationId
     * @param clusterId
     * @param property
     * @throws RegistryException
     */
    public void addPropertyToCluster(String applicationId, String clusterId, NewProperty property) throws RegistryException {
        Registry registry = getRegistry();
        String resourcePath = mainResource + applicationId + "/" + clusterId;

        try {
            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            registry.beginTransaction();
            Resource nodeResource = null;
            if (registry.resourceExists(resourcePath)) {
                nodeResource = registry.get(resourcePath);
            } else {
                nodeResource = registry.newResource();
                if (log.isDebugEnabled()) {
                    log.debug("Registry resource is create at path for cluster" + nodeResource.getPath() + " for cluster");
                }
            }

            nodeResource.setProperty(property.getKey(), Arrays.asList(property.getValues()));
            registry.put(resourcePath, nodeResource);

            registry.commitTransaction();

            log.info(String.format("Registry property is persisted: [resource-path] %s [Property Name] %s [Property Values] %s",
                    resourcePath, property.getKey(), Arrays.asList(property.getValues())));

        } catch (Exception e) {
            String msg = "Failed to persist properties in registry: " + resourcePath;
            registry.rollbackTransaction();
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }

    }

    private UserRegistry getRegistry() throws RegistryException {
        return org.apache.stratos.common.internal.ServiceReferenceHolder.getInstance().getRegistryService().getGovernanceSystemRegistry();
    }

    /**
     * Delete the resource identified by the applicationId, if exist.
     *
     * @param applicationId ID of the application.
     * @return True if resource exist and able to delete, else false.
     * @throws RegistryException
     */
    public boolean deleteApplication(String applicationId) throws RegistryException {
        if (StringUtils.isBlank(applicationId)) {
            throw new IllegalArgumentException("Application ID can not be null");
        }

        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        Registry registry = null;
        String resourcePath = mainResource + applicationId;
        try {
            registry = getRegistry();
            registry.beginTransaction();
            if (registry.resourceExists(resourcePath)) {
                registry.delete(resourcePath);
                registry.commitTransaction();
                log.info(String.format("Application removed from registry %s", applicationId));
                return true;
            }
        } catch (RegistryException e) {
            registry.rollbackTransaction();
            log.error("Could not remove registry resource: [resource-path] " + resourcePath);
        }
        return false;
    }

    /**
     * Add properties to cluster
     *
     * @param applicationName
     * @param clusterId
     * @param properties
     * @throws RegistryException
     */
    public void addPropertiesToCluster(String applicationName, String clusterId, NewProperty[] properties) throws RegistryException {
        for (NewProperty property : properties) {
            this.addPropertyToCluster(applicationName, clusterId, property);

        }
    }

    /**
     * Create or get resource for application
     *
     * @param tempRegistry
     * @param resourcePath
     * @return
     * @throws RegistryException
     */
    private Resource createOrGetResourceforApplication(Registry tempRegistry, String resourcePath) throws RegistryException {
        Resource regResource;
        if (tempRegistry.resourceExists(resourcePath)) {
            regResource = tempRegistry.get(resourcePath);
        } else {
            regResource = tempRegistry.newCollection();
            if (log.isDebugEnabled()) {
                log.debug("Registry resource is create at path " + regResource.getPath() + " for application");
            }
        }
        return regResource;
    }

    /**
     * Create and get resources for Clustor
     *
     * @param tempRegistry
     * @param resourcePath
     * @return
     * @throws RegistryException
     */
    private Resource createOrGetResourceforCluster(Registry tempRegistry, String resourcePath) throws RegistryException {

        int index = resourcePath.lastIndexOf('/');
        String applicationResourcePath = resourcePath.substring(0, index);
        createOrGetResourceforApplication(tempRegistry, applicationResourcePath);
        Resource regResource;
        if (tempRegistry.resourceExists(resourcePath)) {
            regResource = tempRegistry.get(resourcePath);
        } else {
            regResource = tempRegistry.newResource();
            if (log.isDebugEnabled()) {
                log.debug("Registry resource is create at path for cluster" + regResource.getPath() + " for cluster");
            }
        }
        return regResource;
    }

}
