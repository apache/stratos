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

import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.stratos.common.Properties;

/**
 * Holds runtime data of a Cluster.
 * @author nirmal
 *
 */
public class ClusterContext implements Serializable{

    private static final long serialVersionUID = 4830667953031229223L;
    // cluster id
    private String clusterId;
    // cartridge type
    private String cartridgeType;
    // payload as a String
    private String payload;
    private String hostName;
    private boolean isLbCluster;
    private boolean isVolumeRequired;
    private Volume[] volumes;
    // timeout in milliseconds - this would be the per member time that CC waits before forcefully terminate instances on an unregistration.
    private long timeoutInMillis;
    private Properties properties;

    public ClusterContext(String clusterId, String cartridgeType, String payload, String hostName, 
    		boolean isLbCluster, Properties properties) {
        this.clusterId = clusterId;
        this.cartridgeType = cartridgeType;
        this.payload = payload;
        this.setHostName(hostName);
        this.isLbCluster = isLbCluster;
        this.setProperties(properties);
    }
    
    public String getClusterId() {
        return clusterId;
    }
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
    public String getCartridgeType() {
        return cartridgeType;
    }
    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }
    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

	public boolean isLbCluster() {
		return isLbCluster;
	}

	public void setLbCluster(boolean isLbCluster) {
		this.isLbCluster = isLbCluster;
	}
	
	public boolean isVolumeRequired() {
		return isVolumeRequired;
	}

	public void setVolumeRequired(boolean isVolumeRequired) {
		this.isVolumeRequired = isVolumeRequired;
	}

	public long getTimeoutInMillis() {
		return timeoutInMillis;
	}

	public void setTimeoutInMillis(long timeoutInMillis) {
		this.timeoutInMillis = timeoutInMillis;
	}

	public Volume[] getVolumes() {
		return volumes;
	}

	public void setVolumes(Volume[] volumes) {
		this.volumes = ArrayUtils.clone(volumes);
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
	/*public void addProperty(String key, int value) {
		this.properties.put(key, value);
	}
	
	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}*/
}
