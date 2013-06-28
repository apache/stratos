/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds useful information for externals, regarding a Cartridge.
 */
public class CartridgeInfo {

    private String type;
    
    private String hostName;
    
    private String displayName;
    
    private String description;
    
    private String[] deploymentDirs;
    
    private PortMapping[] portMappings;
    
    private AppType[] appTypes;
    
    private String provider;
    
    private String version;
    
    private boolean multiTenant;
    
    private String baseDir;
    
    private Property[] properties;
    
    public CartridgeInfo(){
    	
    }
    
    public CartridgeInfo(String type, String host, String desc, List<String> deploymentDirs, String provider) {
        this.type = type;
        this.hostName = host;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	public String getHostName() {
	    return hostName;
    }

	public void setHostName(String hostName) {
	    this.hostName = hostName;
    }

	public String[] getDeploymentDirs() {
	    return deploymentDirs;
    }

	public void setDeploymentDirs(List<String> deploymentDirsList) {
		if(deploymentDirsList == null){
			deploymentDirsList = new ArrayList<String>();
		}
	    this.deploymentDirs = new String[deploymentDirsList.size()];
	    
	    deploymentDirsList.toArray(deploymentDirs);
	    
    }
	
    public String getProvider() {
	    return provider;
    }

	public void setProvider(String provider) {
	    this.provider = provider;
    }

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isMultiTenant() {
		return multiTenant;
	}

	public void setMultiTenant(boolean multiTenant) {
		this.multiTenant = multiTenant;
	}

	public String getBaseDir() {
	    return baseDir;
    }

	public void setBaseDir(String baseDir) {
	    this.baseDir = baseDir;
    }

	public PortMapping[] getPortMappings() {
	    return portMappings;
    }

	public void setPortMappings(PortMapping[] portMappings) {
	    this.portMappings = portMappings;
    }

	public AppType[] getAppTypes() {
	    return appTypes;
    }

	public void setAppTypes(AppType[] appTypes) {
	    this.appTypes = appTypes;
    }

	public Property[] getProperties() {
	    return properties;
    }

	public void setProperties(Property[] properties) {
	    this.properties = properties;
    }
}
