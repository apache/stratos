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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.definition.PropertyBean;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.api.Registry;
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
		System.out.println("Adding meta data details");

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

			for (PropertyBean prop : cartridgeMetaData.property) {
				resource.addProperty("hostname", prop.hostname);
				resource.addProperty("username", prop.username);
				resource.addProperty("password", prop.password);
			}

			tempRegistry.put(resourcePath, resource);

			System.out.println("A resource added to: " + resourcePath);

			System.out.println(cartridgeMetaData.type);
			// registry.rateResource(resourcePath, defaultRank);

			Comment comment = new Comment();
			comment.setText("Added the " + applicationName + " " + type + " cartridge");
			// registry.addComment(resourcePath, comment);

		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			// Close the session

		}
		System.out.println("Add meta data details");
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
				System.out.println("Resource retrived");
				System.out.println("Printing retrieved resource content: " +
				                   new String((byte[]) getResource.getContent()));

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

				cartridgeMetaData.property = lst;

			}

		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();
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

}
