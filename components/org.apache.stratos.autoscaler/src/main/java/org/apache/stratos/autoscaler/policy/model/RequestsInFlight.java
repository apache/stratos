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
 * The model class for RequestsInFlight definition.
 */
public class RequestsInFlight {

    private float average;
    private float secondDerivative;
    private float gradient;
    private float scaleDownMarginOfGradient;
    private float scaleDownMarginOfSecondDerivative;

    /**
     * Gets the value of the average property.   
     */
    public float getAverage() {
        return average;
    }

    /**
     * Sets the value of the average property.   
     */
    public void setAverage(float value) {
        this.average = value;
    }

    /**
     * Gets the value of the second-derivative property. 
     */
    public float getSecondDerivative() {
        return secondDerivative;
    }

    /**
     * Sets the value of the second-derivative property.   
     */
    public void setSecondDerivative(float value) {
        this.secondDerivative = value;
    }

    /**
     * Gets the value of the gradient property. 
     */
    public float getGradient() {
        return gradient;
    }

    /**
     * Sets the value of the gradient property.
     *     
     */
    public void setGradient(float value) {
        this.gradient = value;
    }

    /**
     * Gets the value of the scaleDownSlowerMarginOfGradient property.
     */
    public float getScaleDownMarginOfGradient() {
        return scaleDownMarginOfGradient;
    }

    /**
     * Sets the value of the scaleDownSlowerMarginOfGradient property.
     *
     */
    public void setScaleDownMarginOfGradient(float scaleDownMarginOfGradient) {
        this.scaleDownMarginOfGradient = scaleDownMarginOfGradient;
    }

    /**
     * Gets the value of the scaleDownSlowerMarginOfSecondDerivative property.
     */
    public float getScaleDownMarginOfSecondDerivative() {
        return scaleDownMarginOfSecondDerivative;
    }

    /**
     * Sets the value of the scaleDownSlowerMarginOfSecondDerivative property.
     *
     */
    public void setScaleDownMarginOfSecondDerivative(float scaleDownMarginOfSecondDerivative) {
        this.scaleDownMarginOfSecondDerivative = scaleDownMarginOfSecondDerivative;
    }

}