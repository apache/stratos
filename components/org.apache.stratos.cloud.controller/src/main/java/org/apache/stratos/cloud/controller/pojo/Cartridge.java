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

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    
    private String defaultAutoscalingPolicy;
    
    private LoadbalancerConfig lbConfig;
    
    private List<PortMapping> portMappings = new ArrayList<PortMapping>();
    
    private Persistence persistence;
    
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
    
    /**
     * Key - partition id
     * Value - Corresponding IaasProvider.
     */
    private Map<String, IaasProvider> partitionToIaasProvider = new ConcurrentHashMap<String, IaasProvider>();
    
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
    
    public void addIaasProvider(String partitionId, IaasProvider iaasProvider) {
        partitionToIaasProvider.put(partitionId, iaasProvider);
    }
    
    public void addIaasProviders(Map<String, IaasProvider> map) {
        for (Iterator<String> iterator = map.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            IaasProvider value = map.get(key);
            
            partitionToIaasProvider.put(key, value);
        }
    }
    
    public IaasProvider getIaasProviderOfPartition(String partitionId) {
        return partitionToIaasProvider.get(partitionId);
    }
    
    public void addProperty(String key, String val) {
        if (key != null && val != null) {
            properties.put(key, val);
        }
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

    public Map<String, IaasProvider> getPartitionToIaasProvider() {
        return partitionToIaasProvider;
    }

    public void setPartitionToIaasProvider(Map<String, IaasProvider> partitionToIaasProvider) {
        this.partitionToIaasProvider = partitionToIaasProvider;
    }

    public LoadbalancerConfig getLbConfig() {
        return lbConfig;
    }

    public void setLbConfig(LoadbalancerConfig lbConfig) {
        this.lbConfig = lbConfig;
    }

    public String getDefaultAutoscalingPolicy() {
        return defaultAutoscalingPolicy;
    }

    public void setDefaultAutoscalingPolicy(String defaultAutoscalingPolicy) {
        this.defaultAutoscalingPolicy = defaultAutoscalingPolicy;
    }

    /**
	 * @return the persistence
	 */
    public Persistence getPersistence() {
        return persistence;
    }

    /**
	 * @param persistence the peristanceMappings to set
	 */
    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }
}
