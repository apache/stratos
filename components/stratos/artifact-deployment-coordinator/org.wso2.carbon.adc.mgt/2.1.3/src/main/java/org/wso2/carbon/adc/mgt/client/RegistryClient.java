/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.client;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.io.IOUtils;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

public class RegistryClient {

	// url where the repository is running its services interface
	private static String backendURL = "http://localhost:9763/services/";
	private static ConfigurationContext configContext = null;

	// configuration locations used to bootstrap axis2
	private static String axis2Repo =
	                                  "/home/wso2/Desktop/HP-demo-packs-with-video/cartridge/wso2stratos-cartridge-1.0.0-SNAPSHOT/repository/conf/axis2";
	private static String axis2Conf =
	                                  "/home/wso2/Desktop/HP-demo-packs-with-video/cartridge/wso2stratos-cartridge-1.0.0-SNAPSHOT/repository/conf/axis2/axis2_client.xml";
	private static String serverURL = "https://localhost:9443/services/";

	public RegistryClient() {
		// TODO Auto-generated constructor stub
	}

	private static WSRegistryServiceClient initialize() throws Exception {
		// set these properties, this is used for authentication over https to
		// the registry
		// if you have a newer version, you can update the keystore by copying
		// it from
		// the security directory of the repository
		System.setProperty("javax.net.ssl.trustStore", "wso2carbon.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");

		configContext =
		                ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo,
		                                                                                     axis2Conf);
		return new WSRegistryServiceClient(serverURL, "admin", "admin", backendURL, configContext);
	}

	public static void addKey(String keyName, String content) throws Exception {
		Registry registry = initialize();

		// get the governance folder
		Resource governanceFolder = registry.get("/_system/governance");
		System.out.println("Folder description: " + governanceFolder.getDescription());
		Resource r1 = registry.newResource();
		String path = "/_system/governance/" + keyName;
		r1.setContent(content);
		registry.put(path, r1);

		/*
		 * List<Resource> paths = getServicePath(registry,
		 * "/_system/governance/trunk/services");
		 * 
		 * for (Resource service : paths) { // we've got all the services here
		 * 
		 * Properties props = service.getProperties(); for (Object prop :
		 * props.keySet()) { System.out.println(prop + " - " + props.get(prop));
		 * }
		 * 
		 * Association[] associations =
		 * registry.getAssociations(service.getPath(), "Documentation"); for
		 * (Association association : associations) {
		 * System.out.println(association.getAssociationType()); } }
		 */
	}

	private static List<Resource> getServicePath(Registry registry, String servicesResource)
	                                                                                        throws RegistryException {
		List<Resource> result = new ArrayList<Resource>();
		Resource resource = registry.get(servicesResource);

		if (resource instanceof Collection) {
			Object content = resource.getContent();
			for (Object path : (Object[]) content) {
				result.addAll(getServicePath(registry, (String) path));
			}
		} else if (resource instanceof Resource) {
			result.add(resource);
		}
		return result;
	}
}
