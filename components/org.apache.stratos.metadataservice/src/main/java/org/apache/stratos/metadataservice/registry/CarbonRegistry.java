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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.definition.*;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.service.RegistryService;

public class CarbonRegistry extends AbstractAdmin implements DataStore {

	private static Log log = LogFactory.getLog(CarbonRegistry.class);
	@Context
	HttpServletRequest httpServletRequest;

	private static ConfigurationContext configContext = null;

	private static String defaultAxis2Repo = "repository/deployment/client";
	private static String defaultAxis2Conf = "repository/conf/axis2/axis2_client.xml";

	private static final String defaultUsername = "admin@org.com";
	private static final String defaultPassword = "admin123";
	private static final String serverURL = "https://localhost:9445/services/";
	private static final String mainResource = "/stratos/";
	private static final int defaultRank = 3;
	private RegistryService registryService;

	public CarbonRegistry() {

	}

    /*
     *
     * Remove the meta data from the registry
     *
     * @see org.apache.stratos.metadataservice.registry.DataStore#
     * removeCartridgeMetaDataDetails(java.lang.String, java.lang.String)
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



    public List<NewProperty> getPropertiesOfCluster(String applicationName, String clusterId) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + clusterId;
        if(!tempRegistry.resourceExists(resourcePath)){
            return null;
            //throw new RegistryException("Cluster does not exist at " + resourcePath);
        }
        Resource regResource = tempRegistry.get(resourcePath);

        ArrayList<NewProperty> newProperties = new ArrayList<NewProperty>();

        Properties props = regResource.getProperties();
        Enumeration<?> x = props.propertyNames();
        while(x.hasMoreElements())
        {
            String key = (String) x.nextElement();
            List<String>  values = regResource.getPropertyValues(key);
            NewProperty property = new NewProperty();
            property.setKey(key);
            String[] valueArr = new String[values.size()];
            property.setValues(values.toArray(valueArr));

            newProperties.add(property);
        }

        return newProperties;
    }

    public void addPropertyToCluster(String applicationId, String clusterId, NewProperty property) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationId + "/" + clusterId;
        Resource regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);

        regResource.setProperty(property.getKey(), Arrays.asList(property.getValues()));
        tempRegistry.put(resourcePath, regResource);
        log.info(String.format("Property %s is added to cluster %s of application %s", property.getKey(), clusterId, applicationId));

    }

    @Override
    public void addPropertiesToCluster(String applicationName, String clusterId, NewProperty[] properties) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + clusterId;
        Resource regResource;
        regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);

        for(NewProperty property : properties){
            regResource.setProperty(property.getKey(), (Arrays.asList(property.getValues())));

        }
        tempRegistry.put(resourcePath, regResource);
        log.info(String.format("Properties  are added to cluster %s of application %s", clusterId, applicationName));
    }


    private Resource createOrGetResourceforApplication(Registry tempRegistry, String resourcePath) throws RegistryException {
        Resource regResource;
        if(tempRegistry.resourceExists(resourcePath)) {
            regResource = tempRegistry.get(resourcePath);
        }else{
            regResource = tempRegistry.newCollection();
            if(log.isDebugEnabled()){
                log.debug("Registry resource is create at path " + regResource.getPath() + " for application");
            }
        }
        return regResource;
    }

    private Resource createOrGetResourceforCluster(Registry tempRegistry, String resourcePath) throws RegistryException {

        int index = resourcePath.lastIndexOf('/');
        String applicationResourcePath = resourcePath.substring(0,index);
        createOrGetResourceforApplication(tempRegistry, applicationResourcePath);
        Resource regResource;
        if(tempRegistry.resourceExists(resourcePath)) {
            regResource = tempRegistry.get(resourcePath);
        }else{
            regResource = tempRegistry.newResource();
            if(log.isDebugEnabled()){
                log.debug("Registry resource is create at path for cluster" + regResource.getPath() + " for cluster");
            }
        }
        return regResource;
    }

}
