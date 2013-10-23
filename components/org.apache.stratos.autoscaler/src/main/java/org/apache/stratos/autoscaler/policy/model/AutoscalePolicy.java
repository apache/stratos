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

package org.apache.stratos.autoscaler.policy.model;

/**
 * The model class for Autoscale-policy definition.
 */
public class AutoscalePolicy {

	private LoadThresholds loadThresholds;
	private HAPolicy haPolicy;
	private String id;
	private String displayName;
	private String description;

    /**
     * Gets the value of the loadThresholds property.
     * 
     * @return
     *     possible object is
     *     {@link LoadThresholds }
     *     
     */
    public LoadThresholds getLoadThresholds() {
        return loadThresholds;
    }

    /**
     * Sets the value of the loadThresholds property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoadThresholds }
     *     
     */
    public void setLoadThresholds(LoadThresholds value) {
        this.loadThresholds = value;
    }

    /**
     * Gets the value of the haPolicy property.
     * 
     * @return
     *     possible object is
     *     {@link HAPolicy }
     *     
     */
    public HAPolicy getHAPolicy() {
        return haPolicy;
    }

    /**
     * Sets the value of the haPolicy property.
     * 
     * @param value
     *     allowed object is
     *     {@link HAPolicy }
     *     
     */
    public void setHAPolicy(HAPolicy value) {
        this.haPolicy = value;
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
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the displayName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
	public String getDisplayName() {
		return displayName;
	}

	 /**
     * Sets the value of the displayName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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

}
