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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Holds information regarding a Cartridge.
 */
public class Cartridge implements Serializable{

    private static final long serialVersionUID = 6637409027085059072L;

	private String type;
    
    private String hostName;
    
    private String provider;
    
    private String displayName;
    
    private String description;
    
    private String baseDir;
    
    private String version;
    
    private boolean multiTenant;
    
    private List<PortMapping> portMappings = new ArrayList<PortMapping>();
    
    private List<AppType> appTypeMappings = new ArrayList<AppType>();
    
    /**
     * Property map of this Cartridge.
     */
    private Map<String, String> properties = new HashMap<String, String>();
    
    /**
     * A Cartridge can have 1..n {@link IaasProvider}s
     */
    private List<IaasProvider> iaases = new ArrayList<IaasProvider>();
    
    private List<String> deploymentDirs = new ArrayList<String>();
    
    private IaasProvider lastlyUsedIaas;
    
    public Cartridge(){}
    
    public Cartridge(String type, String host, String provider, String version, boolean multiTenant) {
        this.type = type;
        this.hostName = host;
        this.provider = provider;
        this.version = version;
        this.multiTenant = multiTenant;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
    
    public String getProperty(String key) {
        return properties.get(key);
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public void addIaasProvider(IaasProvider iaas) {
        for (IaasProvider anIaas : iaases) {
            if(anIaas.equals(iaas)){
                int idx = iaases.indexOf(anIaas);
                iaases.remove(idx);
                iaases.add(idx, iaas);
                return;
            }
        }
        this.iaases.add(iaas);
    }
    
    public IaasProvider getIaasProvider(String iaasType){
    	for (IaasProvider iaas : iaases) {
	        if(iaas.getType().equals(iaasType)){
	        	return iaas;
	        }
        }
    	
    	return null;
    }

    public List<IaasProvider> getIaases() {
        return iaases;
    }

    public void setIaases(List<IaasProvider> iaases) {
        this.iaases = iaases;
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

    public IaasProvider getLastlyUsedIaas() {
        return lastlyUsedIaas;
    }

    public void setLastlyUsedIaas(IaasProvider lastlyUsedIaas) {
        this.lastlyUsedIaas = lastlyUsedIaas;
    }

//    public boolean isJcloudsObjectsBuilt() {
//        return isJcloudsObjectsBuilt;
//    }
//
//    public void setJcloudsObjectsBuilt(boolean isJcloudsObjectsBuilt) {
//        this.isJcloudsObjectsBuilt = isJcloudsObjectsBuilt;
//    }

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
//		lastlyUsedIaas = null;
	}

	public List<String> getDeploymentDirs() {
	    return deploymentDirs;
    }

	public void setDeploymentDirs(List<String> deploymentDirs) {
	    this.deploymentDirs = deploymentDirs;
    }
	
	public void addDeploymentDir(String dir){
		deploymentDirs.add(dir);
	}
	
	public void addPortMapping(PortMapping mapping){
		portMappings.add(mapping);
	}
	
	public void addAppType(AppType type){
		appTypeMappings.add(type);
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

	public List<PortMapping> getPortMappings() {
	    return portMappings;
    }

	public void setPortMappings(List<PortMapping> portMappings) {
	    this.portMappings = portMappings;
    }

	public List<AppType> getAppTypeMappings() {
    	return appTypeMappings;
    }

	public void setAppTypeMappings(List<AppType> appTypeMappings) {
    	this.appTypeMappings = appTypeMappings;
    }
    
}
