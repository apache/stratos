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
package org.apache.stratos.metadataservice.registry;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.definition.*;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;


/**
 * Carbon registry implementation
 */

public class CarbonRegistry extends AbstractAdmin implements DataStore {

    private static Log log = LogFactory.getLog(CarbonRegistry.class);
    @Context
    HttpServletRequest httpServletRequest;

    private static final String mainResource = "/startos/";


    public CarbonRegistry() {

    }


    /**
     * Add the meta data to governance registry
     *
     * @param applicationName Application Name
     * @param cartridgeType Cartridge Type
     * @param cartridgeMetaData Cartridge Meta Data
     * @throws Exception
     */
    @Override
    public void addCartridgeMetaDataDetails(String applicationName, String cartridgeType,
                                            CartridgeMetaData cartridgeMetaData) throws Exception {
        log.debug("Adding meta data details");

        Registry tempRegistry = getGovernanceUserRegistry();


        Resource resource = tempRegistry.newResource();

        String type = cartridgeMetaData.type;

        resource.setContent("Application description :: " + type);

        String resourcePath = mainResource + applicationName + "/" + cartridgeType;

        resource.addProperty("Application Name", cartridgeMetaData.applicationName);
        resource.addProperty("Display Name", cartridgeMetaData.displayName);
        resource.addProperty("Description", cartridgeMetaData.description);
        resource.addProperty("Cartidge Type", cartridgeMetaData.type);
        resource.addProperty("provider", cartridgeMetaData.provider);
        resource.addProperty("Version", cartridgeMetaData.version);
        resource.addProperty("Host", cartridgeMetaData.host);
        resource.addProperty("Properties", cartridgeMetaData.properties);

        tempRegistry.put(resourcePath, resource);

        if (log.isDebugEnabled()) {
            log.debug("A resource added to: " + resourcePath);
        }


    }

    /**
     * Get the meta data from the registry
     *
     * @param applicationName name of the application
     * @param cartridgeType cartridge type
     * @return
     * @throws Exception
     */
    @Override
    public String getCartridgeMetaDataDetails(String applicationName, String cartridgeType)
            throws Exception {
        Registry registry = getGovernanceUserRegistry();
        CartridgeMetaData cartridgeMetaData = new CartridgeMetaData();


        String resourcePath = mainResource + applicationName + "/" + cartridgeType;
        if (registry.resourceExists(resourcePath)) {

            Resource getResource = registry.get(resourcePath);

            cartridgeMetaData.type = getResource.getProperty("Cartidge Type");
            cartridgeMetaData.applicationName = getResource.getProperty("Application Name");
            cartridgeMetaData.description = getResource.getProperty("Description");
            cartridgeMetaData.displayName = getResource.getProperty("Display Name");
            cartridgeMetaData.host = getResource.getProperty("host");
            cartridgeMetaData.provider = getResource.getProperty("provider");
            cartridgeMetaData.version = getResource.getProperty("Version");
            cartridgeMetaData.properties = getResource.getProperty("Properties");


        }


        return cartridgeMetaData.toString();
    }


    /**
     * Remove the meta data from the registry
     *
     * @param applicationName name of the application
     * @param cartridgeType cartridge type
     * @return
     * @throws Exception
     */
    @Override
    public boolean removeCartridgeMetaDataDetails(String applicationName, String cartridgeType)
            throws Exception {
        Registry registry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + cartridgeType;

        if (registry != null) {
            registry.delete(resourcePath);
            return true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Unable to delete the meta data since the Registry is NULL");
            }
            return false;
        }
    }

    /**
     * Get Properties of clustor
     * @param applicationName
     * @param clusterId
     * @return
     * @throws RegistryException
     */
    @Override
    public List<NewProperty> getPropertiesOfCluster(String applicationName, String clusterId) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + clusterId;
        if (!tempRegistry.resourceExists(resourcePath)) {
            return null;
            //throw new RegistryException("Cluster does not exist at " + resourcePath);
        }
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
     * @param applicationId
     * @param clusterId
     * @param property
     * @throws RegistryException
     */
    @Override
    public void addPropertyToCluster(String applicationId, String clusterId, NewProperty property) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationId + "/" + clusterId;
        Resource regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);

        regResource.setProperty(property.getKey(), Arrays.asList(property.getValues()));
        tempRegistry.put(resourcePath, regResource);
        log.info(String.format("Property %s is added to cluster %s of application %s", property.getKey(), clusterId, applicationId));

    }

    /**
     * Add properties to clustor
     * @param applicationName
     * @param clusterId
     * @param properties
     * @throws RegistryException
     */
    @Override
    public void addPropertiesToCluster(String applicationName, String clusterId, NewProperty[] properties) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + clusterId;
        Resource regResource;
        regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);

        for (NewProperty property : properties) {
            regResource.setProperty(property.getKey(), (Arrays.asList(property.getValues())));

        }
        tempRegistry.put(resourcePath, regResource);
        log.info(String.format("Properties  are added to cluster %s of application %s", clusterId, applicationName));
    }

    /**
     * Create or get resource for application
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
