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

package org.apache.stratos.cloud.controller.deployment.partition;

import org.apache.stratos.common.Properties;

import java.io.Serializable;

/**
 * The model class for Partition definition.
 */
public class Partition implements Serializable{

    private static final long serialVersionUID = 3725971214092010720L;
    private int partitionMax;
	private int partitionMin;
	/**
	 * provider should match with an IaasProvider type.
	 */
    private String provider;
    private Properties properties = new Properties();
    private String id;
    private String description;
    private boolean isPublic;


    /**
     * Gets the value of the partitionMax property.
     * 
     */
    public int getPartitionMax() {
        return partitionMax;
    }

    /**
     * Sets the value of the partitionMax property.
     * 
     */
    public void setPartitionMax(int value) {
        this.partitionMax = value;
    }

    /**
     * Gets the value of the partitionMin property.
     * 
     */
    public int getPartitionMin() {
        return partitionMin;
    }

    /**
     * Sets the value of the partitionMin property.
     * 
     */
    public void setPartitionMin(int value) {
        this.partitionMin = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
	public String getId() {
		return id;
	}

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
	public String getDescription() {
		return description;
	}
	
    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
     * Sets the value of the isPublic property.
     * 
     * @param value
     *     allowed object is boolean
     *     
     */
	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	
	/**
     * Gets the value of the isPublic property.
     *          
     */
	public boolean getIsPublic() {
		return isPublic;
	}
		
	public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getProvider() {
        return provider;
    }

	public void setProvider(String provider) {
        this.provider = provider;
    }
	
	@Override
	public String toString() {
		return "Partition [id=" + id + ", description=" + description + ", isPublic=" + isPublic 
				+ ", provider=" + provider + ", partitionMin=" + partitionMin + ", partitionMax="
				+ partitionMax + ", properties=" + properties + "]";
	}
    
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Partition) {
            return this.id.equals(((Partition) obj).getId());
        }
        return false;
        
    }
    
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

}
