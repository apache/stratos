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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration related to Topology synchronization
 *
 */
public class TopologyConfig implements Serializable{
	
	private static final long serialVersionUID = 4435173744617096911L;
	
	// default implementation is WSO2MBTopologyPublisher
	private String className = "org.apache.stratos.cloud.controller.topic.WSO2MBTopologyPublisher";
	
	/**
     * Key - Value pair.
     */
    private Map<String, String> properties = new HashMap<String, String>();
    
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        
        if(properties.containsKey(key)){
            return properties.get(key);
        }
        
        return null;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

	public String getClassName() {
		// try to get the class name from a property
		String temp = getProperty(CloudControllerConstants.CLASS_NAME_ELEMENT);
		return temp == null ? className : temp;
	}

	public void setClassName(String className) {
		this.className = className;
	}

}
