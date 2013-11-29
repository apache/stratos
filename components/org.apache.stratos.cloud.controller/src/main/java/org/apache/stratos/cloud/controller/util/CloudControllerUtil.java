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
package org.apache.stratos.cloud.controller.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.pojo.AppType;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.pojo.Property;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class CloudControllerUtil {
	private static final Log log = LogFactory.getLog(CloudControllerUtil.class);

    public static CartridgeInfo toCartridgeInfo(Cartridge cartridge) {

		CartridgeInfo carInfo = new CartridgeInfo();
		carInfo.setType(cartridge.getType());
		carInfo.setDisplayName(cartridge.getDisplayName());
		carInfo.setDescription(cartridge.getDescription());
		carInfo.setHostName(cartridge.getHostName());
		carInfo.setDeploymentDirs(cartridge.getDeploymentDirs());
		carInfo.setProvider(cartridge.getProvider());
		carInfo.setVersion(cartridge.getVersion());
		carInfo.setMultiTenant(cartridge.isMultiTenant());
		carInfo.setBaseDir(cartridge.getBaseDir());
		carInfo.setPortMappings(cartridge.getPortMappings()
		                                 .toArray(new PortMapping[cartridge.getPortMappings()
		                                                                   .size()]));
		carInfo.setAppTypes(cartridge.getAppTypeMappings()
                                .toArray(new AppType[cartridge.getAppTypeMappings()
                                                                  .size()]));
		
		List<Property> propList = new ArrayList<Property>();
		
		for (Iterator<?> iterator = cartridge.getProperties().entrySet().iterator(); iterator.hasNext();) {
	        @SuppressWarnings("unchecked")
            Map.Entry<String, String> entry = (Entry<String, String>) iterator.next();
	        
	        Property prop = new Property(entry.getKey(), entry.getValue());
	        propList.add(prop);
        }
		Property[] props = new Property[propList.size()];
		
		carInfo.setProperties(propList.toArray(props));

		return carInfo;
	}
	
	public static List<Object> getKeysFromValue(Map<?, ?> hm, Object value) {
		List<Object> list = new ArrayList<Object>();
		for (Object o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				list.add(o);
			}
		}
		return list;
	}
	
	public static void sleep(long time){
    	try {
    		Thread.sleep(time);
    	} catch (InterruptedException ignore) {}
    	
    }
	
	/**
	 * Converts org.apache.stratos.messaging.util.Properties to java.util.Properties
	 * @param properties org.apache.stratos.messaging.util.Properties
	 * @return java.util.Properties
	 */
    public static Properties toJavaUtilProperties(
        org.apache.stratos.cloud.controller.pojo.Properties properties) {
        Properties javaProps = new Properties();

        if (properties != null && properties.getProperties() != null) {

            for (org.apache.stratos.cloud.controller.pojo.Property property : properties.getProperties()) {
                javaProps.put(property.getName(), property.getValue());
            }

        }

        return javaProps;
    }
	
	public static void handleException(String msg, Exception e){
		log.error(msg, e);
		throw new CloudControllerException(msg, e);
	}
	
	public static void handleException(String msg){
		log.error(msg);
		throw new CloudControllerException(msg);
	}
}
