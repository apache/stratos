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
 * The model class for Partition definition.
 */
public class Partition {

	private int partitionMax;
	private int partitionMin;
	private String id;
	private String iaas;
	private String zone;

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
     * Gets the value of the iaas property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIaas() {
        return iaas;
    }

    /**
     * Sets the value of the iaas property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIaas(String value) {
        this.iaas = value;
    }

    /**
     * Gets the value of the zone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getZone() {
        return zone;
    }

    /**
     * Sets the value of the zone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setZone(String value) {
        this.zone = value;
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

}