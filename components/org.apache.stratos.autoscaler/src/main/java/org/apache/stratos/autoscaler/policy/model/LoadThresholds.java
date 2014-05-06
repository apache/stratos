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

import java.io.Serializable;

/**
 * The model class for LoadThresholds definition.
 */
public class LoadThresholds implements Serializable{

	private static final long serialVersionUID = -8148571245537655867L;
	private RequestsInFlightThresholds requestsInFlight;
	private MemoryConsumptionThresholds memoryConsumption;
	private LoadAverageThresholds loadAverage;

    /**
     * Gets the value of the requestsInFlight property.
     * 
     * @return
     *     possible object is
     *     {@link RequestsInFlightThresholds }
     *     
     */
    public RequestsInFlightThresholds getRequestsInFlight() {
        return requestsInFlight;
    }

    /**
     * Sets the value of the requestsInFlight property.
     * 
     * @param value
     *     allowed object is
     *     {@link RequestsInFlightThresholds }
     *     
     */
    public void setRequestsInFlight(RequestsInFlightThresholds value) {
        this.requestsInFlight = value;
    }

    /**
     * Gets the value of the memoryConsumption property.
     * 
     * @return
     *     possible object is
     *     {@link MemoryConsumptionThresholds }
     *     
     */
    public MemoryConsumptionThresholds getMemoryConsumption() {
        return memoryConsumption;
    }

    /**
     * Sets the value of the memoryConsumption property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemoryConsumptionThresholds }
     *     
     */
    public void setMemoryConsumption(MemoryConsumptionThresholds value) {
        this.memoryConsumption = value;
    }

    /**
     * Gets the value of the loadAverage property.
     * 
     * @return
     *     possible object is
     *     {@link LoadAverageThresholds }
     *     
     */
    public LoadAverageThresholds getLoadAverage() {
        return loadAverage;
    }

    /**
     * Sets the value of the loadAverage property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoadAverageThresholds }
     *     
     */
    public void setLoadAverage(LoadAverageThresholds value) {
        this.loadAverage = value;
    }

}