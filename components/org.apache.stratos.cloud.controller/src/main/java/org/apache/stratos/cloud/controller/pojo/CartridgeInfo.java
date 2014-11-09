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
package org.apache.stratos.cloud.controller.pojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.stratos.common.Property;

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
    
    private boolean isPublic;
    
    private String baseDir;
    
    private Property[] properties;
    
    private String defaultAutoscalingPolicy;

    private String defaultDeploymentPolicy;
    
    private LoadbalancerConfig lbConfig;

    private Persistence persistence;

    private String serviceGroup;

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
	
	public boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
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
	    this.portMappings = ArrayUtils.clone(portMappings);
    }

	public AppType[] getAppTypes() {
	    return appTypes;
    }

	public void setAppTypes(AppType[] appTypes) {
	    this.appTypes = ArrayUtils.clone(appTypes);
    }

	public Property[] getProperties() {
	    return properties;
    }

	public void setProperties(Property[] properties) {
	    this.properties = ArrayUtils.clone(properties);
    }

    public String getDefaultAutoscalingPolicy() {
        return defaultAutoscalingPolicy;
    }

    public void setDefaultAutoscalingPolicy(String defaultAutoscalingPolicy) {
        this.defaultAutoscalingPolicy = defaultAutoscalingPolicy;
    }

    public LoadbalancerConfig getLbConfig() {
        return lbConfig;
    }

    public void setLbConfig(LoadbalancerConfig lbConfig) {
        this.lbConfig = lbConfig;
    }

    public String getDefaultDeploymentPolicy() {
        return defaultDeploymentPolicy;
    }

    public void setDefaultDeploymentPolicy(String defaultDeploymentPolicy) {
        this.defaultDeploymentPolicy = defaultDeploymentPolicy;
    }

     /**
	 * @return the persistence
	 */
    public Persistence getPersistence() {
        return persistence;
    }

    /**
	 * @param persistence the persistanceMappings to set
	 */
    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }
}
