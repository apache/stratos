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

        String memberID = System.getProperty("member.id");

        Runtime runtime = Runtime.getRuntime();

		Map<String, Integer> statsMap = new HashMap<String, Integer>();

        //statsMap.put("Available Processors", (int)runtime.availableProcessors());
        statsMap.put("total_memory", (int)(runtime.totalMemory() / MB));
        statsMap.put("max_memory", (int)(runtime.maxMemory() / MB));
        statsMap.put("used_memory", (int)((runtime.totalMemory() - runtime.freeMemory()) / MB));
        statsMap.put("free_memory", (int)(runtime.freeMemory() / MB));
        statsMap.put("load_average", (int)ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        statsMap.put("member_id", Integer.parseInt(memberID));

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