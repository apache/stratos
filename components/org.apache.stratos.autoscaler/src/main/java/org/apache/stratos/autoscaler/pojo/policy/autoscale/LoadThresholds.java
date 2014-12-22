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

package org.apache.stratos.autoscaler.pojo.policy.autoscale;

import java.io.Serializable;

/**
 * The model class for LoadThresholds definition.
 */
public class LoadThresholds implements Serializable{

	private static final long serialVersionUID = -8148571245537655867L;
	private float requestsInFlightThreshold;
	private float memoryConsumptionThreshold;
	private float loadAverageThreshold;

    /**
     * Gets the value of the requestsInFlightThreshold property.
     * 
     * @return
     *     possible object is
     *     {@link RequestsInFlightThresholds }
     *     
     */
    public float getRequestsInFlightThreshold() {
        return requestsInFlightThreshold;
    }

    /**
     * Sets the value of the requestsInFlightThreshold property.
     * 
     * @param value
     *     allowed object is
     *     {@link RequestsInFlightThresholds }
     *     
     */
    public void setRequestsInFlightThreshold(float value) {
        this.requestsInFlightThreshold = value;
    }

    /**
     * Gets the value of the memoryConsumptionThreshold property.
     * 
     * @return
     *     possible object is
     *     {@link MemoryConsumptionThresholds }
     *     
     */
    public float getMemoryConsumptionThreshold() {
        return memoryConsumptionThreshold;
    }

    /**
     * Sets the value of the memoryConsumptionThreshold property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemoryConsumptionThresholds }
     *     
     */
    public void setMemoryConsumptionThreshold(float value) {
        this.memoryConsumptionThreshold = value;
    }

    /**
     * Gets the value of the loadAverageThreshold property.
     * 
     * @return
     *     possible object is
     *     {@link LoadAverageThresholds }
     *     
     */
    public float getLoadAverageThreshold() {
        return loadAverageThreshold;
    }

    /**
     * Sets the value of the loadAverageThreshold property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoadAverageThresholds }
     *     
     */
    public void setLoadAverageThreshold(float value) {
        this.loadAverageThreshold = value;
    }

}