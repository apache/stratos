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

package org.apache.stratos.manager.dto;


import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.cloud.controller.stub.pojo.PortMapping;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

@XmlRootElement
public class Cartridge implements Comparable<Cartridge> {

    private String displayName;
    private String description;
    private boolean isPublic;
	private String cartridgeAlias;
	private String cartridgeType;
	private int activeInstances;
	private String status;
	//private String ip;
	private String provider;
	private String version;
	private boolean multiTenant;
    private boolean isLoadBalancer;
	private String hostName;
	//private String policy;
	//private String policyDescription;
	private String repoURL;
	private String mappedDomain;

    //DB specific
    private String dbHost;
    private String dbUserName;
    private String password;

    //LB cluster id
    private String lbClusterId;
    
    private String clusterId;

	private String[] accessURLs;
	private PortMapping[] portMappings;

    private String defaultAutoscalingPolicy;

    private String defaultDeploymentPolicy;

    private boolean isPersistance;

    private Persistence persistence;
    
    private String serviceGroup;

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
	
	public boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public String getCartridgeAlias() {
		return cartridgeAlias;
	}

	public void setCartridgeAlias(String cartridgeAlias) {
		this.cartridgeAlias = cartridgeAlias;
	}

	public String getCartridgeType() {
		return cartridgeType;
	}

	public void setCartridgeType(String cartridgeType) {
		this.cartridgeType = cartridgeType;
	}

	public int getActiveInstances() {
		return activeInstances;
	}

	public void setActiveInstances(int activeInstances) {
		this.activeInstances = activeInstances;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/*public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}*/

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public String getHostName() {
    	return hostName;
    }

	public void setHostName(String hostName) {
    	this.hostName = hostName;
    }

	/*public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getPolicyDescription() {
		return policyDescription;
	}

	public void setPolicyDescription(String policyDescription) {
		this.policyDescription = policyDescription;
	}*/

	public String getRepoURL() {
    	return repoURL;
    }

	public void setRepoURL(String repoURL) {
    	this.repoURL = repoURL;
    }

	public String getDbUserName() {
    	return dbUserName;
    }

	public String[] getAccessURLs() {
		return accessURLs;
	}

	public void setAccessURLs(String[] accessURLs) {
        if(accessURLs == null) {
            this.accessURLs = new String[0];
        } else {
            this.accessURLs = Arrays.copyOf(accessURLs, accessURLs.length);
        }
	}

	public void setDbUserName(String dbUserName) {
    	this.dbUserName = dbUserName;
    }

	public String getMappedDomain() {
		return mappedDomain;
	}

	public void setMappedDomain(String mappedDomain) {
		this.mappedDomain = mappedDomain;
	}

	@Override
	public int compareTo(Cartridge o) {
		int compare = 0;
		if (cartridgeAlias != null && o.cartridgeAlias != null) {
			compare = cartridgeAlias.compareTo(o.cartridgeAlias);
		}
		if (compare == 0 && cartridgeType != null && o.cartridgeType != null) {
			compare = cartridgeType.compareTo(o.cartridgeType);
		}
		return compare;
	}

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

	public PortMapping[] getPortMappings() {
		return portMappings;
	}

	public void setPortMappings(PortMapping[] portMappings) {
        if(portMappings == null) {
            this.portMappings = new PortMapping[0];
        } else {
            this.portMappings = Arrays.copyOf(portMappings, portMappings.length);
        }
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }

    public boolean isLoadBalancer() {
        return isLoadBalancer;
    }

    public void setLoadBalancer(boolean loadBalancer) {
        this.isLoadBalancer = loadBalancer ;
    }

    public String getDefaultAutoscalingPolicy() {
        return defaultAutoscalingPolicy;
    }

    public void setDefaultAutoscalingPolicy(String defaultAutoscalingPolicy) {
        this.defaultAutoscalingPolicy = defaultAutoscalingPolicy;
    }

    public String getDefaultDeploymentPolicy() {
        return defaultDeploymentPolicy;
    }

    public void setDefaultDeploymentPolicy(String defaultDeploymentPolicy) {
        this.defaultDeploymentPolicy = defaultDeploymentPolicy;
}

    public boolean isPersistance() {
        return isPersistance;
    }

    public void setPersistance(boolean persistance) {
        isPersistance = persistance;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

}
