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
import org.wso2.carbon.registry.core.Comment;
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
	private static final String mainResource = "/startos/";
	private static final int defaultRank = 3;
	private RegistryService registryService;

	public CarbonRegistry() {

	}

	/*
	 * Add the meta data to governance registry
	 * 
	 * @see org.apache.stratos.metadataservice.registry.DataStore#
	 * addCartridgeMetaDataDetails(java.lang.String, java.lang.String,
	 * org.apache.stratos.metadataservice.definition.CartridgeMetaData)
	 */
	@Override
	public String addCartridgeMetaDataDetails(String applicationName, String cartridgeType,
	                                          CartridgeMetaData cartridgeMetaData) throws Exception {
		log.debug("Adding meta data details");
		
		Registry tempRegistry = getGovernanceUserRegistry();
		try {

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
			resource.addProperty("host", cartridgeMetaData.host);

			for (PropertyBean prop : cartridgeMetaData.properties) {
				resource.addProperty("hostname", prop.hostname);
				resource.addProperty("username", prop.username);
				resource.addProperty("password", prop.password);
			}

			tempRegistry.put(resourcePath, resource);

			if(log.isDebugEnabled()){
				log.debug("A resource added to: " + resourcePath);
			}
		
			Comment comment = new Comment();
			comment.setText("Added the " + applicationName + " " + type + " cartridge");
			// registry.addComment(resourcePath, comment);

		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("addCartridgeMetaDataDetails", e);
			}
		} finally {
			// Close the session

		}
        
		return "success";
	}

	/*
	 * Get the meta data from the registry
	 * 
	 * @see org.apache.stratos.metadataservice.registry.DataStore#
	 * getCartridgeMetaDataDetails(java.lang.String, java.lang.String)
	 */
	@Override
	public String getCartridgeMetaDataDetails(String applicationName, String cartridgeType)
	                                                                                       throws Exception {
		Registry registry = getGovernanceUserRegistry();
		CartridgeMetaData cartridgeMetaData = new CartridgeMetaData();
		try {

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

				List<PropertyBean> lst = new ArrayList<PropertyBean>();
				PropertyBean prop = new PropertyBean();
				prop.hostname = getResource.getProperty("hostname");
				prop.username = getResource.getProperty("username");
				prop.password = getResource.getProperty("password");
				lst.add(prop);

				cartridgeMetaData.properties = lst;

			}

		} catch (Exception e) {

			if (log.isErrorEnabled()) {
				log.error("getCartridgeMetaDataDetails", e);
			}
		} finally {
			// Close the session

		}
		return cartridgeMetaData.toString();
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
		registry.delete(resourcePath);
		return false;
	}



    public List<NewProperty> getPropertiesOfCluster(String applicationName, String clusterId) throws Exception {
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
        tempRegistry.put(regResource.getPath(), regResource);

    }

    @Override
    public void addPropertiesToCluster(String applicationName, String clusterId, NewProperty[] properties) throws Exception {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + clusterId;
        Resource regResource;
        regResource = createOrGetResourceforCluster(tempRegistry, resourcePath);
        try {
            for(NewProperty property : properties){
                regResource.setProperty(property.getKey(), (Arrays.asList(property.getValues())));

            }
            tempRegistry.put(resourcePath, regResource);
            if(log.isDebugEnabled()){
                log.debug("A resource added to: " + resourcePath);
            }

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("addCartridgeMetaDataDetails", e);
            }
        } finally {
            // Close the session

        }
    }

    public void addPropertiesToApplication(String applicationId, NewProperty[] properties) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationId;
        Resource regResource = createOrGetResourceforApplication(tempRegistry, resourcePath);

        for(NewProperty property : properties){
            regResource.setProperty(property.getKey(), (Arrays.asList(property.getValues())));

        }
        tempRegistry.put(resourcePath, regResource);
    }

    public void addPropertyToApplication(String applicationId, NewProperty property) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationId;
        Resource regResource = createOrGetResourceforApplication(tempRegistry, resourcePath);
        regResource.setProperty(property.getKey(), (Arrays.asList(property.getValues())));
    }

    public List<NewProperty> getPropertiesOfApplication(String applicationId) throws RegistryException {
        Registry tempRegistry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationId;
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
        if(newProperties.size() == 0){
            return null;
        }
        return newProperties;
    }


    private Resource createOrGetResourceforApplication(Registry tempRegistry, String resourcePath) throws RegistryException {
        Resource regResource;
        if(tempRegistry.resourceExists(resourcePath)) {
            regResource = tempRegistry.get(resourcePath);
        }else{
            regResource = tempRegistry.newCollection();
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
        }
        return regResource;
    }

}
