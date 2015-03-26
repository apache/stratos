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
package org.apache.stratos.cloud.controller.domain;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.*;
import org.apache.stratos.common.Properties;

import java.io.Serializable;
import java.util.*;

/**
 * Holds information regarding a Cartridge.
 */
public class Cartridge implements Serializable{

	private transient static final Log log = LogFactory.getLog(Cartridge.class);
    private static final long serialVersionUID = 6637409027085059072L;

	private String type;
    private String hostName;
    private String provider;
	private String category;
    private String displayName;
    private String description;
    private String baseDir;
    private String version;
    private boolean multiTenant;
    private String tenantPartitions;
    private PortMapping[] portMappings;
    private Persistence persistence;
    private AppType[] appTypeMappings;
    private String loadBalancingIPType;
	private String[] metadataKeys;

    private boolean isPublic;

    /**
     * Property of this Cartridge.
     */
    private org.apache.stratos.common.Properties properties;

    private String[] deploymentDirs;
    private String[] exportingProperties;

    private IaasConfig[] iaasConfigs;

    public Cartridge(){
    	init();
    }
    
    public Cartridge(String type, String host, String provider, String version, boolean multiTenant) {
        this.type = type;
        this.hostName = host;
        this.provider = provider;
        this.version = version;
        this.multiTenant = multiTenant;
        init();
    }
    
    private void init() {
        tenantPartitions = "*";
        properties = new Properties();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public void addProperty(String key, String val) {
        if (key != null && val != null) {
            Property property = new Property();
            property.setName(key);
            property.setValue(val);
            properties.addProperty(property);
        }
    }

    public org.apache.stratos.common.Properties getProperties() {
        return properties;
    }

    public void setProperties(org.apache.stratos.common.Properties properties) {
        this.properties = properties;
    }

	public boolean equals(Object obj) {
		if (obj instanceof Cartridge) {
			return this.type.equals(((Cartridge)obj).getType());
		}
		return false;
	}
    
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(type).
            toHashCode();
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
	
	public void reset(){
	}

	public String[] getDeploymentDirs() {
	    return deploymentDirs;
    }

	public void setDeploymentDirs(String[] deploymentDirs) {
	    this.deploymentDirs = deploymentDirs;
    }

    public void addDeploymentDir(String dir) {
        List<String> deploymentDirList = null;
        if (dir != null) {
            deploymentDirList = Arrays.asList(dir);
        }
        deploymentDirList.add(dir);
        deploymentDirs = deploymentDirList.toArray(new String[deploymentDirList.size()]);
    }

    public void addPortMapping(PortMapping mapping) {
        List<PortMapping> portMappingList = Arrays.asList(mapping);
        portMappingList.add(mapping);
        portMappingList.toArray(this.portMappings);
    }

    public void addAppType(AppType type) {
        List<AppType> appTypeList = Arrays.asList(type);
        appTypeList.add(type);
        appTypeList.toArray(this.appTypeMappings);
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

	public AppType[] getAppTypeMappings() {
    	return appTypeMappings;
    }

	public void setAppTypeMappings(AppType[] appTypeMappings) {
    	this.appTypeMappings = appTypeMappings;
    }

    /**
	 * @return the persistence
	 */
    public Persistence getPersistence() {
        return persistence;
    }

    /**
	 * @param persistence the persistenceMappings to set
	 */
    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public String[] getExportingProperties() {
        return exportingProperties;
    }

    public void setExportingProperties(String[] exportingProperties) {
        this.exportingProperties = exportingProperties;
    }

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

    public String getTenantPartitions() {
        return tenantPartitions;
    }

    public void setTenantPartitions(String tenantPartitions) {
        this.tenantPartitions = tenantPartitions;
    }

	public String[] getMetadataKeys() {
		return metadataKeys;
	}

	public void setMetadataKeys(String[] metadataKeys) {
		this.metadataKeys = metadataKeys;
	}

    public String getLoadBalancingIPType() {
        return loadBalancingIPType;
    }

    public void setLoadBalancingIPType(String loadBalancingIPType) {
        this.loadBalancingIPType = loadBalancingIPType;
    }

    public IaasConfig[] getIaasConfigs() {
        return iaasConfigs;
    }

    public void setIaasConfigs(IaasConfig[] iaasConfigs) {
        this.iaasConfigs = ArrayUtils.clone(iaasConfigs);
    }

    public boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    public String toString() {
        return "Cartridge [type=" + type
                + ", hostName=" + hostName
                + ", provider=" + provider
                + ", version=" + version
                + ", multiTenant=" + multiTenant
                + ", properties=" + properties
                + "]";
    }
}
