/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.statistics.publisher;

/**
 * CartridgeStatistics is an instantaneous representaion of the cartridges current process and memory usage.
 */
public class CartridgeStatistics {

    private double memoryUsage;
    private double processorUsage;

    /**
     * Constructor
     *
     * @param memUsage the consumed memory, as a percentage of the available memory to the cartridge
     * @param procUsage the processing used, as a percentage of the processing available to the cartridge
     */
    public CartridgeStatistics(double memUsage, double procUsage) {
        memoryUsage = sanitiseUsage(memUsage);
        processorUsage = sanitiseUsage(procUsage);
    }

    /**
     * Called by contructor, utility to check input usage is a percentage.
     * throws exception if the usage is not of the correct format
     */
    private double sanitiseUsage(double usage) throws IllegalArgumentException {
        if ((usage < 0)) // || (usage > 100)) we currently get percentages over 100% for the procUsage is this fine?
        {
            throw new IllegalArgumentException("Usage statistic less than zero");
        }
        return usage;
    }

    /**
     * Called to get memory usage
     */
    public double getMemoryUsage() {
        return memoryUsage;
    }

    /**
     * Called to get processor usage
     */
    public double getProcessorUsage() {
        return processorUsage;
    }
}
