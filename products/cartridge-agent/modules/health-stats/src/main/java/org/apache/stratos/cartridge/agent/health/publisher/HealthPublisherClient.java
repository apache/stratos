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

package org.apache.stratos.cartridge.agent.health.publisher;

import java.io.*;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Runtime;
import java.lang.System;
import java.util.HashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;

public class HealthPublisherClient {

    private static final int MB = 1024 * 1024;

    public Object getHealthStats() {

        Runtime runtime = Runtime.getRuntime();

        Map<String, Double> statsMap = new HashMap<String, Double>();

        double totalMemory = (double)(runtime.totalMemory() / MB);
        double usedMemory = (double)((totalMemory - (runtime.freeMemory() / MB) ));
        double memoryConsumption = (usedMemory / totalMemory) * 100;

        statsMap.put("memory_consumption", memoryConsumption);
        statsMap.put("load_average", (double)ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

        Object statObj = (Object)statsMap;

        return statObj;
    }

    public void run() {
        try {
            HealthPublisher publisher = new HealthPublisher();

            while (true) {
                Object healthStatObj = getHealthStats();
                publisher.update(healthStatObj);

                Thread.sleep(10000);
            }
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}