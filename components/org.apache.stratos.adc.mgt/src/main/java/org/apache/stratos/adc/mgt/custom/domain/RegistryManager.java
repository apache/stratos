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

package org.apache.stratos.adc.mgt.custom.domain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.DomainMappingExistsException;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

public class RegistryManager {
	private static Log log = LogFactory.getLog(RegistryManager.class);
	private static Registry registry = DataHolder.getRegistry();

	public RegistryManager() {
		try {
			if (!registry.resourceExists(CartridgeConstants.DomainMappingInfo.HOSTINFO)) {
				registry.put(CartridgeConstants.DomainMappingInfo.HOSTINFO,
				                    registry.newCollection());
			}
		} catch (RegistryException e) {
			String msg =
			             "Error while accessing registry or initializing domain mapping registry path\n";
			log.error(msg + e.getMessage());
		}
	}

	/**
    *
    */
    public void addDomainMappingToRegistry(String hostName, String actualHost)
            throws ADCException, RegistryException, DomainMappingExistsException {
        try {
            registry.beginTransaction();
            Resource hostResource = registry.newResource();
            hostResource.addProperty(CartridgeConstants.DomainMappingInfo.ACTUAL_HOST, actualHost);
            if (!registry.resourceExists(CartridgeConstants.DomainMappingInfo.HOSTINFO +
                                                hostName)) {
                registry.put(CartridgeConstants.DomainMappingInfo.HOSTINFO + hostName,
                                    hostResource);
            } else {
                registry.rollbackTransaction();
                String msg = "Requested domain is already taken!";
                log.error(msg);
                throw new DomainMappingExistsException(msg, hostName);
            }
            registry.commitTransaction();
        } catch (RegistryException e) {
            registry.rollbackTransaction();
            throw e; 
        }
    }


    /**
        *
        */
   	public void removeDomainMappingFromRegistry(String actualHost) throws Exception {
   		try {
               registry.beginTransaction();
                String hostResourcePath = CartridgeConstants.DomainMappingInfo.HOSTINFO;
                if (registry.resourceExists(hostResourcePath)) {
                    Resource hostResource = registry.get(hostResourcePath);
                    Collection hostInfoCollection;
                    if(hostResource instanceof Collection){
                        hostInfoCollection = (Collection) hostResource;
                    } else {
                        throw new Exception("Resource is not a collection " + hostResourcePath );
                    }
                    String[] paths = hostInfoCollection.getChildren();
                    for (String path: paths){
                        Resource domainMapping = registry.get(path);
                        String actualHostInRegistry = domainMapping.getProperty(CartridgeConstants.DomainMappingInfo.ACTUAL_HOST);
                        if(actualHostInRegistry != null && actualHost.equalsIgnoreCase(actualHostInRegistry)){
                            registry.delete(path);
                        }
                    }
                }
                registry.commitTransaction();
   		} catch (RegistryException e) {
   			registry.rollbackTransaction();
   			log.error("Unable to remove the mapping", e);
   			throw e;
   		}
   	}

}
